package functions.cbsplit

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.*

@Serializable
data class WebhookPayload(
    val event: String,
    val timestamp: Long,
    val data: JsonObject,
    val source: String,
    val signature: String? = null
)

@Serializable
data class ConversionWebhook(
    val sessionId: String,
    val variant: String,
    val testId: String,
    val orderId: String,
    val revenue: Double,
    val currency: String = "USD",
    val customerEmail: String? = null,
    val customerId: String? = null,
    val productId: String? = null,
    val affiliateId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ClickBankWebhook(
    val transactionType: String,
    val receipt: String,
    val totalAccountAmount: Double,
    val paymentMethod: String,
    val customerDisplayName: String,
    val customerEmail: String,
    val lineItems: List<ClickBankLineItem>,
    val affiliate: ClickBankAffiliate? = null,
    val trackingCodes: Map<String, String> = emptyMap()
)

@Serializable
data class ClickBankLineItem(
    val itemNo: String,
    val productTitle: String,
    val quantity: Int,
    val accountAmount: Double
)

@Serializable
data class ClickBankAffiliate(
    val id: String,
    val nickname: String
)

@Serializable
data class ClickFunnelsWebhook(
    val event: String,
    val contact: ClickFunnelsContact,
    val order: ClickFunnelsOrder? = null,
    val funnel: ClickFunnelsFunnel,
    val page: ClickFunnelsPage
)

@Serializable
data class ClickFunnelsContact(
    val id: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null
)

@Serializable
data class ClickFunnelsOrder(
    val id: String,
    val totalAmount: Double,
    val currency: String,
    val status: String,
    val products: List<ClickFunnelsProduct>
)

@Serializable
data class ClickFunnelsProduct(
    val id: String,
    val name: String,
    val price: Double,
    val quantity: Int
)

@Serializable
data class ClickFunnelsFunnel(
    val id: String,
    val name: String
)

@Serializable
data class ClickFunnelsPage(
    val id: String,
    val name: String,
    val url: String
)

object CBSplitWebhooks {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val webhookSecrets = mutableMapOf<String, String>()
    private val processingQueue = mutableListOf<WebhookPayload>()
    private val isProcessing = mutableMapOf<String, Boolean>()
    
    /**
     * Registers a webhook secret for signature verification
     */
    fun registerWebhookSecret(source: String, secret: String) {
        webhookSecrets[source] = secret
    }
    
    /**
     * Processes ClickBank INS (Instant Notification Service) webhook
     */
    suspend fun processClickBankWebhook(payload: String, signature: String?): Boolean {
        return try {
            // Verify signature if provided
            if (signature != null && !verifyClickBankSignature(payload, signature)) {
                return false
            }
            
            val webhookData = parseClickBankPayload(payload)
            val trackingParams = extractTrackingFromClickBank(webhookData)
            
            if (trackingParams != null) {
                val conversion = ConversionWebhook(
                    sessionId = trackingParams.sessionId,
                    variant = trackingParams.variant,
                    testId = trackingParams.testId,
                    orderId = webhookData.receipt,
                    revenue = webhookData.totalAccountAmount,
                    currency = "USD",
                    customerEmail = webhookData.customerEmail,
                    customerId = webhookData.customerDisplayName,
                    productId = webhookData.lineItems.firstOrNull()?.itemNo,
                    affiliateId = webhookData.affiliate?.id
                )
                
                processConversion(conversion)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Processes ClickFunnels webhook
     */
    suspend fun processClickFunnelsWebhook(payload: String, signature: String?): Boolean {
        return try {
            if (signature != null && !verifyClickFunnelsSignature(payload, signature)) {
                return false
            }
            
            val webhookData = json.decodeFromString<ClickFunnelsWebhook>(payload)
            
            when (webhookData.event) {
                "contact.created" -> {
                    // Lead conversion
                    val trackingParams = extractTrackingFromUrl(webhookData.page.url)
                    if (trackingParams != null) {
                        val conversion = ConversionWebhook(
                            sessionId = trackingParams.sessionId,
                            variant = trackingParams.variant,
                            testId = trackingParams.testId,
                            orderId = "lead_${webhookData.contact.id}",
                            revenue = 0.0,
                            customerEmail = webhookData.contact.email,
                            customerId = webhookData.contact.id
                        )
                        processConversion(conversion)
                    }
                }
                
                "order.created" -> {
                    // Purchase conversion
                    webhookData.order?.let { order ->
                        val trackingParams = extractTrackingFromUrl(webhookData.page.url)
                        if (trackingParams != null) {
                            val conversion = ConversionWebhook(
                                sessionId = trackingParams.sessionId,
                                variant = trackingParams.variant,
                                testId = trackingParams.testId,
                                orderId = order.id,
                                revenue = order.totalAmount,
                                currency = order.currency,
                                customerEmail = webhookData.contact.email,
                                customerId = webhookData.contact.id,
                                productId = order.products.firstOrNull()?.id
                            )
                            processConversion(conversion)
                        }
                    }
                }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Processes generic webhook payload
     */
    suspend fun processGenericWebhook(
        source: String,
        payload: String,
        signature: String? = null
    ): Boolean {
        return try {
            val webhookPayload = WebhookPayload(
                event = "generic",
                timestamp = System.currentTimeMillis(),
                data = json.parseToJsonElement(payload).jsonObject,
                source = source,
                signature = signature
            )
            
            // Add to processing queue
            processingQueue.add(webhookPayload)
            processWebhookQueue()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Processes conversion data and updates analytics
     */
    private suspend fun processConversion(conversion: ConversionWebhook) {
        // Update CBSplit analytics
        val analytics = CBSplitAnalytics.getInstance(null) // Context not needed for server-side
        
        analytics.trackConversion(
            testId = conversion.testId,
            variant = conversion.variant,
            eventType = if (conversion.revenue > 0) "purchase" else "lead",
            value = conversion.revenue,
            userId = conversion.customerId ?: conversion.customerEmail ?: "unknown"
        )
        
        // Update session bridge
        CBSplitSessionBridge.addTouchpoint(
            sessionId = conversion.sessionId,
            source = "webhook",
            medium = "conversion",
            campaign = conversion.testId,
            page = "checkout",
            action = if (conversion.revenue > 0) "purchase" else "lead"
        )
        
        // Trigger revenue attribution
        CBSplitRevenueAttribution.attributeRevenue(
            sessionId = conversion.sessionId,
            orderId = conversion.orderId,
            revenue = conversion.revenue,
            currency = conversion.currency,
            timestamp = conversion.timestamp
        )
        
        // Send to external analytics if configured
        broadcastConversion(conversion)
    }
    
    /**
     * Verifies ClickBank signature
     */
    private fun verifyClickBankSignature(payload: String, signature: String): Boolean {
        val secret = webhookSecrets["clickbank"] ?: return false
        
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
            mac.init(secretKey)
            
            val hash = mac.doFinal(payload.toByteArray())
            val computedSignature = Base64.getEncoder().encodeToString(hash)
            
            computedSignature == signature
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Verifies ClickFunnels signature
     */
    private fun verifyClickFunnelsSignature(payload: String, signature: String): Boolean {
        val secret = webhookSecrets["clickfunnels"] ?: return false
        
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
            mac.init(secretKey)
            
            val hash = mac.doFinal(payload.toByteArray())
            val computedSignature = "sha256=" + hash.joinToString("") { "%02x".format(it) }
            
            computedSignature == signature
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Parses ClickBank form-encoded payload
     */
    private fun parseClickBankPayload(payload: String): ClickBankWebhook {
        val params = payload.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                java.net.URLDecoder.decode(parts[0], "UTF-8") to java.net.URLDecoder.decode(parts[1], "UTF-8")
            } else {
                parts[0] to ""
            }
        }
        
        return ClickBankWebhook(
            transactionType = params["transactionType"] ?: "",
            receipt = params["receipt"] ?: "",
            totalAccountAmount = params["totalAccountAmount"]?.toDoubleOrNull() ?: 0.0,
            paymentMethod = params["paymentMethod"] ?: "",
            customerDisplayName = params["customerDisplayName"] ?: "",
            customerEmail = params["customerEmail"] ?: "",
            lineItems = parseClickBankLineItems(params),
            affiliate = parseClickBankAffiliate(params),
            trackingCodes = params.filterKeys { it.startsWith("tracking") }
        )
    }
    
    /**
     * Parses ClickBank line items from parameters
     */
    private fun parseClickBankLineItems(params: Map<String, String>): List<ClickBankLineItem> {
        val lineItems = mutableListOf<ClickBankLineItem>()
        var index = 1
        
        while (params.containsKey("lineItemNo$index")) {
            lineItems.add(
                ClickBankLineItem(
                    itemNo = params["lineItemNo$index"] ?: "",
                    productTitle = params["lineItemTitle$index"] ?: "",
                    quantity = params["lineItemQuantity$index"]?.toIntOrNull() ?: 1,
                    accountAmount = params["lineItemAccountAmount$index"]?.toDoubleOrNull() ?: 0.0
                )
            )
            index++
        }
        
        return lineItems
    }
    
    /**
     * Parses ClickBank affiliate from parameters
     */
    private fun parseClickBankAffiliate(params: Map<String, String>): ClickBankAffiliate? {
        val affiliateId = params["affiliate"] ?: return null
        return ClickBankAffiliate(
            id = affiliateId,
            nickname = params["affiliateNickname"] ?: affiliateId
        )
    }
    
    /**
     * Extracts CBSplit tracking parameters from ClickBank webhook
     */
    private fun extractTrackingFromClickBank(webhook: ClickBankWebhook): CBSplitUrlInjector.TrackingParams? {
        return webhook.trackingCodes.let { codes ->
            val sessionId = codes["cb_session"] ?: return null
            val variant = codes["cb_variant"] ?: return null
            val testId = codes["cb_test"] ?: return null
            
            CBSplitUrlInjector.TrackingParams(
                sessionId = sessionId,
                variant = variant,
                testId = testId,
                timestamp = codes["cb_ts"]?.toLongOrNull() ?: System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Extracts tracking parameters from URL
     */
    private fun extractTrackingFromUrl(url: String): CBSplitUrlInjector.TrackingParams? {
        return CBSplitUrlInjector.extractTrackingParams(url)
    }
    
    /**
     * Processes webhook queue asynchronously
     */
    private suspend fun processWebhookQueue() {
        if (isProcessing["queue"] == true) return
        
        isProcessing["queue"] = true
        
        try {
            withContext(Dispatchers.IO) {
                while (processingQueue.isNotEmpty()) {
                    val webhook = processingQueue.removeAt(0)
                    processWebhookPayload(webhook)
                    delay(100) // Prevent overwhelming the system
                }
            }
        } finally {
            isProcessing["queue"] = false
        }
    }
    
    /**
     * Processes individual webhook payload
     */
    private suspend fun processWebhookPayload(webhook: WebhookPayload) {
        try {
            when (webhook.source) {
                "kajabi" -> processKajabiWebhook(webhook)
                "leadpages" -> processLeadPagesWebhook(webhook)
                "unbounce" -> processUnbounceWebhook(webhook)
                "shopify" -> processShopifyWebhook(webhook)
                else -> processUnknownWebhook(webhook)
            }
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }
    
    /**
     * Processes Kajabi webhook
     */
    private suspend fun processKajabiWebhook(webhook: WebhookPayload) {
        val data = webhook.data
        
        when (webhook.event) {
            "form.submitted" -> {
                // Handle lead conversion
                val email = data["email"]?.toString()
                if (email != null) {
                    // Extract tracking from form data or referrer
                    // Process as lead conversion
                }
            }
            "offer.purchased" -> {
                // Handle purchase conversion
                val amount = data["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
                // Process as purchase conversion
            }
        }
    }
    
    /**
     * Processes LeadPages webhook
     */
    private suspend fun processLeadPagesWebhook(webhook: WebhookPayload) {
        // Similar processing for LeadPages events
    }
    
    /**
     * Processes Unbounce webhook
     */
    private suspend fun processUnbounceWebhook(webhook: WebhookPayload) {
        // Similar processing for Unbounce events
    }
    
    /**
     * Processes Shopify webhook
     */
    private suspend fun processShopifyWebhook(webhook: WebhookPayload) {
        // Similar processing for Shopify events
    }
    
    /**
     * Processes unknown webhook
     */
    private suspend fun processUnknownWebhook(webhook: WebhookPayload) {
        // Generic processing for unknown webhook sources
    }
    
    /**
     * Broadcasts conversion to external systems
     */
    private suspend fun broadcastConversion(conversion: ConversionWebhook) {
        // Send to CBSplit broadcast system
        CBSplitBroadcast.broadcastMessage(
            channel = "cbsplit_conversions",
            message = mapOf(
                "type" to "conversion",
                "sessionId" to conversion.sessionId,
                "variant" to conversion.variant,
                "revenue" to conversion.revenue,
                "timestamp" to conversion.timestamp
            )
        )
        
        // Send to external analytics platforms
        sendToGoogleAnalytics(conversion)
        sendToFacebookPixel(conversion)
    }
    
    /**
     * Sends conversion to Google Analytics
     */
    private suspend fun sendToGoogleAnalytics(conversion: ConversionWebhook) {
        // Implementation for GA4 Measurement Protocol
    }
    
    /**
     * Sends conversion to Facebook Pixel
     */
    private suspend fun sendToFacebookPixel(conversion: ConversionWebhook) {
        // Implementation for Facebook Conversions API
    }
    
    /**
     * Creates webhook endpoint handlers
     */
    fun createWebhookEndpoints(): Map<String, suspend (String, String?) -> Boolean> {
        return mapOf(
            "/webhook/clickbank" to { payload, signature -> processClickBankWebhook(payload, signature) },
            "/webhook/clickfunnels" to { payload, signature -> processClickFunnelsWebhook(payload, signature) },
            "/webhook/kajabi" to { payload, signature -> processGenericWebhook("kajabi", payload, signature) },
            "/webhook/leadpages" to { payload, signature -> processGenericWebhook("leadpages", payload, signature) },
            "/webhook/unbounce" to { payload, signature -> processGenericWebhook("unbounce", payload, signature) },
            "/webhook/shopify" to { payload, signature -> processGenericWebhook("shopify", payload, signature) }
        )
    }
}