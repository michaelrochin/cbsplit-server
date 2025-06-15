package functions.cbsplit

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * CBSplit Integration Handler
 * Supports major CRO platform integrations including shopping carts, pixels, webhooks, APIs, and email marketing
 */
@Serializable
data class IntegrationConfig(
    val platform: String,
    val apiKey: String? = null,
    val apiSecret: String? = null,
    val endpoint: String? = null,
    val webhookUrl: String? = null,
    val pixelId: String? = null,
    val enabled: Boolean = true,
    val settings: Map<String, String> = emptyMap()
)

@Serializable
data class CBSplitRequest(
    val sessionId: String,
    val variant: String,
    val userId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val integrations: List<String> = emptyList()
)

@Serializable
data class CBSplitResponse(
    val success: Boolean,
    val variant: String,
    val sessionId: String,
    val integrationResults: Map<String, IntegrationResult> = emptyMap(),
    val error: String? = null
)

@Serializable
data class IntegrationResult(
    val platform: String,
    val success: Boolean,
    val message: String? = null,
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class WebhookPayload(
    val event: String,
    val sessionId: String,
    val variant: String,
    val timestamp: Long,
    val data: Map<String, Any> = emptyMap()
)

class CBSplitIntegration {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    private val integrationConfigs = ConcurrentHashMap<String, IntegrationConfig>()
    private val activeVariants = ConcurrentHashMap<String, String>()
    
    companion object {
        // Shopping Cart Platforms
        const val CLICKBANK = "clickbank"
        const val STICKY_IO = "sticky_io"
        const val INFUSIONSOFT = "infusionsoft"
        const val SHOPIFY = "shopify"
        const val WOOCOMMERCE = "woocommerce"
        const val STRIPE = "stripe"
        const val PAYPAL = "paypal"
        const val GUMROAD = "gumroad"
        
        // Pixel Management Platforms
        const val FACEBOOK_PIXEL = "facebook_pixel"
        const val GOOGLE_ANALYTICS = "google_analytics"
        const val GOOGLE_TAG_MANAGER = "google_tag_manager"
        const val HOTJAR = "hotjar"
        const val MIXPANEL = "mixpanel"
        const val SEGMENT = "segment"
        const val AMPLITUDE = "amplitude"
        
        // Email Marketing Platforms
        const val MAILCHIMP = "mailchimp"
        const val KLAVIYO = "klaviyo"
        const val SENDGRID = "sendgrid"
        const val AWEBER = "aweber"
        const val CONVERTKIT = "convertkit"
        const val ACTIVECAMPAIGN = "activecampaign"
        
        // CRO and Testing Platforms
        const val OPTIMIZELY = "optimizely"
        const val VWO = "vwo"
        const val GOOGLE_OPTIMIZE = "google_optimize"
        const val UNBOUNCE = "unbounce"
        const val LEADPAGES = "leadpages"
    }
    
    /**
     * Initialize integration configurations
     */
    fun initializeIntegrations(configs: List<IntegrationConfig>) {
        configs.forEach { config ->
            integrationConfigs[config.platform] = config
        }
    }
    
    /**
     * Main entry point for CBSplit variant assignment and integration processing
     */
    suspend fun processVariantAssignment(request: CBSplitRequest): CBSplitResponse {
        return try {
            // Assign variant using multi-variant logic
            val assignedVariant = assignVariant(request)
            
            // Store variant assignment
            activeVariants[request.sessionId] = assignedVariant
            
            // Process integrations if specified
            val integrationResults = if (request.integrations.isNotEmpty()) {
                processIntegrations(request, assignedVariant)
            } else {
                emptyMap()
            }
            
            CBSplitResponse(
                success = true,
                variant = assignedVariant,
                sessionId = request.sessionId,
                integrationResults = integrationResults
            )
        } catch (exception: Exception) {
            CBSplitResponse(
                success = false,
                variant = "control",
                sessionId = request.sessionId,
                error = exception.message
            )
        }
    }
    
    /**
     * Multi-variant assignment logic
     */
    private fun assignVariant(request: CBSplitRequest): String {
        // Hash-based variant assignment for consistent user experience
        val hash = (request.sessionId + request.userId.orEmpty()).hashCode()
        val variants = listOf("control", "variant_a", "variant_b", "variant_c")
        val index = kotlin.math.abs(hash) % variants.size
        return variants[index]
    }
    
    /**
     * Process multiple integrations concurrently
     */
    private suspend fun processIntegrations(
        request: CBSplitRequest,
        variant: String
    ): Map<String, IntegrationResult> {
        val results = ConcurrentHashMap<String, IntegrationResult>()
        
        coroutineScope {
            request.integrations.map { platform ->
                async {
                    val config = integrationConfigs[platform]
                    if (config?.enabled == true) {
                        val result = processIntegration(platform, request, variant, config)
                        results[platform] = result
                    }
                }
            }.awaitAll()
        }
        
        return results
    }
    
    /**
     * Process individual integration based on platform
     */
    private suspend fun processIntegration(
        platform: String,
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        return try {
            when (platform) {
                // Shopping Cart Integrations
                CLICKBANK -> processClickBankIntegration(request, variant, config)
                STICKY_IO -> processStickyIoIntegration(request, variant, config)
                INFUSIONSOFT -> processInfusionSoftIntegration(request, variant, config)
                SHOPIFY -> processShopifyIntegration(request, variant, config)
                WOOCOMMERCE -> processWooCommerceIntegration(request, variant, config)
                STRIPE -> processStripeIntegration(request, variant, config)
                PAYPAL -> processPayPalIntegration(request, variant, config)
                GUMROAD -> processGumroadIntegration(request, variant, config)
                
                // Pixel Management Integrations
                FACEBOOK_PIXEL -> processFacebookPixelIntegration(request, variant, config)
                GOOGLE_ANALYTICS -> processGoogleAnalyticsIntegration(request, variant, config)
                GOOGLE_TAG_MANAGER -> processGoogleTagManagerIntegration(request, variant, config)
                HOTJAR -> processHotjarIntegration(request, variant, config)
                MIXPANEL -> processMixpanelIntegration(request, variant, config)
                SEGMENT -> processSegmentIntegration(request, variant, config)
                AMPLITUDE -> processAmplitudeIntegration(request, variant, config)
                
                // Email Marketing Integrations
                MAILCHIMP -> processMailchimpIntegration(request, variant, config)
                KLAVIYO -> processKlaviyoIntegration(request, variant, config)
                SENDGRID -> processSendGridIntegration(request, variant, config)
                AWEBER -> processAWeberIntegration(request, variant, config)
                CONVERTKIT -> processConvertKitIntegration(request, variant, config)
                ACTIVECAMPAIGN -> processActiveCampaignIntegration(request, variant, config)
                
                // CRO and Testing Platform Integrations
                OPTIMIZELY -> processOptimizelyIntegration(request, variant, config)
                VWO -> processVWOIntegration(request, variant, config)
                GOOGLE_OPTIMIZE -> processGoogleOptimizeIntegration(request, variant, config)
                UNBOUNCE -> processUnbounceIntegration(request, variant, config)
                LEADPAGES -> processLeadPagesIntegration(request, variant, config)
                
                else -> IntegrationResult(
                    platform = platform,
                    success = false,
                    message = "Unsupported platform: $platform"
                )
            }
        } catch (exception: Exception) {
            IntegrationResult(
                platform = platform,
                success = false,
                message = "Integration error: ${exception.message}"
            )
        }
    }
    
    // Shopping Cart Integration Methods
    private suspend fun processClickBankIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "session_id" to request.sessionId,
            "variant" to variant,
            "user_id" to request.userId.orEmpty(),
            "event" to "variant_assigned"
        )
        
        return sendWebhook(config.webhookUrl ?: config.endpoint ?: "", payload, CLICKBANK)
    }
    
    private suspend fun processStickyIoIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "Authorization" to "Bearer ${config.apiKey}",
            "Content-Type" to "application/json"
        )
        
        val payload = mapOf(
            "session_id" to request.sessionId,
            "variant" to variant,
            "properties" to mapOf(
                "split_test_variant" to variant,
                "user_id" to request.userId.orEmpty()
            )
        )
        
        return sendApiRequest(config.endpoint ?: "", payload, headers, STICKY_IO)
    }
    
    private suspend fun processInfusionSoftIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "X-Keap-API-Key" to config.apiKey.orEmpty(),
            "Content-Type" to "application/json"
        )
        
        val payload = mapOf(
            "contact_id" to request.userId.orEmpty(),
            "tag_name" to "CBSplit_$variant",
            "session_data" to mapOf(
                "session_id" to request.sessionId,
                "variant" to variant
            )
        )
        
        return sendApiRequest(config.endpoint ?: "", payload, headers, INFUSIONSOFT)
    }
    
    private suspend fun processShopifyIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "X-Shopify-Access-Token" to config.apiKey.orEmpty(),
            "Content-Type" to "application/json"
        )
        
        val payload = mapOf(
            "customer" to mapOf(
                "id" to request.userId.orEmpty(),
                "tags" to "CBSplit_$variant",
                "note" to "Session: ${request.sessionId}, Variant: $variant"
            )
        )
        
        return sendApiRequest(config.endpoint ?: "", payload, headers, SHOPIFY)
    }
    
    private suspend fun processWooCommerceIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "session_id" to request.sessionId,
            "variant" to variant,
            "user_id" to request.userId.orEmpty(),
            "action" to "cbsplit_variant_assigned"
        )
        
        return sendWebhook(config.webhookUrl ?: "", payload, WOOCOMMERCE)
    }
    
    private suspend fun processStripeIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "Authorization" to "Bearer ${config.apiSecret}",
            "Content-Type" to "application/x-www-form-urlencoded"
        )
        
        val payload = mapOf(
            "metadata[cbsplit_session]" to request.sessionId,
            "metadata[cbsplit_variant]" to variant,
            "metadata[user_id]" to request.userId.orEmpty()
        )
        
        return sendApiRequest(config.endpoint ?: "", payload, headers, STRIPE)
    }
    
    private suspend fun processPayPalIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "session_id" to request.sessionId,
            "variant" to variant,
            "user_id" to request.userId.orEmpty(),
            "custom_fields" to mapOf(
                "cbsplit_variant" to variant
            )
        )
        
        return sendWebhook(config.webhookUrl ?: "", payload, PAYPAL)
    }
    
    private suspend fun processGumroadIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "session_id" to request.sessionId,
            "variant" to variant,
            "user_id" to request.userId.orEmpty(),
            "event_type" to "variant_assignment"
        )
        
        return sendWebhook(config.webhookUrl ?: "", payload, GUMROAD)
    }
    
    // Pixel Management Integration Methods
    private suspend fun processFacebookPixelIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "Content-Type" to "application/json"
        )
        
        val payload = mapOf(
            "data" to listOf(
                mapOf(
                    "event_name" to "CBSplitVariantAssigned",
                    "event_time" to (System.currentTimeMillis() / 1000),
                    "user_data" to mapOf(
                        "external_id" to request.userId.orEmpty()
                    ),
                    "custom_data" to mapOf(
                        "variant" to variant,
                        "session_id" to request.sessionId
                    )
                )
            ),
            "access_token" to config.apiKey.orEmpty()
        )
        
        val endpoint = "https://graph.facebook.com/v18.0/${config.pixelId}/events"
        return sendApiRequest(endpoint, payload, headers, FACEBOOK_PIXEL)
    }
    
    private suspend fun processGoogleAnalyticsIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "client_id" to request.sessionId,
            "events" to listOf(
                mapOf(
                    "name" to "cbsplit_variant_assigned",
                    "parameters" to mapOf(
                        "variant" to variant,
                        "session_id" to request.sessionId,
                        "user_id" to request.userId.orEmpty()
                    )
                )
            )
        )
        
        val endpoint = "https://www.google-analytics.com/mp/collect?measurement_id=${config.settings["measurement_id"]}&api_secret=${config.apiSecret}"
        return sendApiRequest(endpoint, payload, emptyMap(), GOOGLE_ANALYTICS)
    }
    
    private suspend fun processGoogleTagManagerIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        // GTM integration typically happens client-side, but we can send server-side events
        val payload = mapOf(
            "event" to "cbsplit_variant_assigned",
            "cbsplit_variant" to variant,
            "cbsplit_session_id" to request.sessionId,
            "user_id" to request.userId.orEmpty()
        )
        
        return sendWebhook(config.webhookUrl ?: "", payload, GOOGLE_TAG_MANAGER)
    }
    
    private suspend fun processHotjarIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "session_id" to request.sessionId,
            "variant" to variant,
            "user_id" to request.userId.orEmpty(),
            "event" to "cbsplit_variant_assigned"
        )
        
        return sendWebhook(config.webhookUrl ?: "", payload, HOTJAR)
    }
    
    private suspend fun processMixpanelIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "Authorization" to "Basic ${config.apiSecret}",
            "Content-Type" to "application/json"
        )
        
        val payload = mapOf(
            "event" to "CBSplit Variant Assigned",
            "properties" to mapOf(
                "distinct_id" to request.userId.orEmpty(),
                "variant" to variant,
                "session_id" to request.sessionId,
                "token" to config.apiKey.orEmpty()
            )
        )
        
        return sendApiRequest("https://api.mixpanel.com/track", payload, headers, MIXPANEL)
    }
    
    private suspend fun processSegmentIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "Authorization" to "Basic ${config.apiKey}",
            "Content-Type" to "application/json"
        )
        
        val payload = mapOf(
            "userId" to request.userId.orEmpty(),
            "event" to "CBSplit Variant Assigned",
            "properties" to mapOf(
                "variant" to variant,
                "session_id" to request.sessionId
            )
        )
        
        return sendApiRequest("https://api.segment.io/v1/track", payload, headers, SEGMENT)
    }
    
    private suspend fun processAmplitudeIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "api_key" to config.apiKey.orEmpty(),
            "events" to listOf(
                mapOf(
                    "user_id" to request.userId.orEmpty(),
                    "event_type" to "CBSplit Variant Assigned",
                    "event_properties" to mapOf(
                        "variant" to variant,
                        "session_id" to request.sessionId
                    )
                )
            )
        )
        
        return sendApiRequest("https://api2.amplitude.com/2/httpapi", payload, emptyMap(), AMPLITUDE)
    }
    
    // Email Marketing Integration Methods
    private suspend fun processMailchimpIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "Authorization" to "Bearer ${config.apiKey}",
            "Content-Type" to "application/json"
        )
        
        val payload = mapOf(
            "email_address" to request.metadata["email"].orEmpty(),
            "status" to "subscribed",
            "merge_fields" to mapOf(
                "CBSPLIT" to variant,
                "SESSION" to request.sessionId
            ),
            "tags" to listOf("CBSplit_$variant")
        )
        
        return sendApiRequest(config.endpoint ?: "", payload, headers, MAILCHIMP)
    }
    
    private suspend fun processKlaviyoIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "Authorization" to "Klaviyo-API-Key ${config.apiKey}",
            "Content-Type" to "application/json"
        )
        
        val payload = mapOf(
            "data" to mapOf(
                "type" to "event",
                "attributes" to mapOf(
                    "profile" to mapOf(
                        "email" to request.metadata["email"].orEmpty()
                    ),
                    "metric" to mapOf(
                        "name" to "CBSplit Variant Assigned"
                    ),
                    "properties" to mapOf(
                        "variant" to variant,
                        "session_id" to request.sessionId
                    )
                )
            )
        )
        
        return sendApiRequest("https://a.klaviyo.com/api/events/", payload, headers, KLAVIYO)
    }
    
    private suspend fun processSendGridIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "Authorization" to "Bearer ${config.apiKey}",
            "Content-Type" to "application/json"
        )
        
        val payload = mapOf(
            "contacts" to listOf(
                mapOf(
                    "email" to request.metadata["email"].orEmpty(),
                    "custom_fields" to mapOf(
                        "cbsplit_variant" to variant,
                        "cbsplit_session" to request.sessionId
                    )
                )
            )
        )
        
        return sendApiRequest("https://api.sendgrid.com/v3/marketing/contacts", payload, headers, SENDGRID)
    }
    
    private suspend fun processAWeberIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "email" to request.metadata["email"].orEmpty(),
            "tags" to mapOf(
                "cbsplit_variant" to variant
            ),
            "custom_fields" to mapOf(
                "session_id" to request.sessionId
            )
        )
        
        return sendWebhook(config.webhookUrl ?: "", payload, AWEBER)
    }
    
    private suspend fun processConvertKitIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "api_key" to config.apiKey.orEmpty(),
            "email" to request.metadata["email"].orEmpty(),
            "fields" to mapOf(
                "cbsplit_variant" to variant,
                "cbsplit_session" to request.sessionId
            ),
            "tags" to listOf("CBSplit_$variant")
        )
        
        return sendApiRequest(config.endpoint ?: "", payload, emptyMap(), CONVERTKIT)
    }
    
    private suspend fun processActiveCampaignIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "Api-Token" to config.apiKey.orEmpty(),
            "Content-Type" to "application/json"
        )
        
        val payload = mapOf(
            "contact" to mapOf(
                "email" to request.metadata["email"].orEmpty(),
                "fieldValues" to listOf(
                    mapOf(
                        "field" to "cbsplit_variant",
                        "value" to variant
                    ),
                    mapOf(
                        "field" to "cbsplit_session",
                        "value" to request.sessionId
                    )
                )
            )
        )
        
        return sendApiRequest(config.endpoint ?: "", payload, headers, ACTIVECAMPAIGN)
    }
    
    // CRO and Testing Platform Integration Methods
    private suspend fun processOptimizelyIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val headers = mapOf(
            "Authorization" to "Bearer ${config.apiKey}",
            "Content-Type" to "application/json"
        )
        
        val payload = mapOf(
            "project_id" to config.settings["project_id"].orEmpty(),
            "visitors" to listOf(
                mapOf(
                    "visitor_id" to request.sessionId,
                    "attributes" to listOf(
                        mapOf(
                            "entity_id" to "cbsplit_variant",
                            "key" to "cbsplit_variant",
                            "value" to variant
                        )
                    )
                )
            )
        )
        
        return sendApiRequest("https://logx.optimizely.com/v1/events", payload, headers, OPTIMIZELY)
    }
    
    private suspend fun processVWOIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "session_id" to request.sessionId,
            "variant" to variant,
            "user_id" to request.userId.orEmpty(),
            "event" to "cbsplit_variant_assigned"
        )
        
        return sendWebhook(config.webhookUrl ?: "", payload, VWO)
    }
    
    private suspend fun processGoogleOptimizeIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        // Google Optimize integration typically happens client-side
        val payload = mapOf(
            "session_id" to request.sessionId,
            "variant" to variant,
            "experiment_id" to config.settings["experiment_id"].orEmpty()
        )
        
        return sendWebhook(config.webhookUrl ?: "", payload, GOOGLE_OPTIMIZE)
    }
    
    private suspend fun processUnbounceIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "session_id" to request.sessionId,
            "variant" to variant,
            "page_id" to config.settings["page_id"].orEmpty(),
            "event" to "cbsplit_variant_assigned"
        )
        
        return sendWebhook(config.webhookUrl ?: "", payload, UNBOUNCE)
    }
    
    private suspend fun processLeadPagesIntegration(
        request: CBSplitRequest,
        variant: String,
        config: IntegrationConfig
    ): IntegrationResult {
        val payload = mapOf(
            "session_id" to request.sessionId,
            "variant" to variant,
            "lead_page_id" to config.settings["lead_page_id"].orEmpty(),
            "event" to "cbsplit_variant_assigned"
        )
        
        return sendWebhook(config.webhookUrl ?: "", payload, LEADPAGES)
    }
    
    /**
     * Generic webhook sender
     */
    private suspend fun sendWebhook(
        url: String,
        payload: Map<String, Any>,
        platform: String
    ): IntegrationResult {
        if (url.isEmpty()) {
            return IntegrationResult(
                platform = platform,
                success = false,
                message = "Webhook URL not configured"
            )
        }
        
        return try {
            val json = Json.encodeToString(payload)
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            IntegrationResult(
                platform = platform,
                success = response.statusCode() in 200..299,
                message = if (response.statusCode() in 200..299) "Webhook sent successfully" else "Webhook failed with status ${response.statusCode()}",
                data = mapOf("status_code" to response.statusCode().toString())
            )
        } catch (exception: Exception) {
            IntegrationResult(
                platform = platform,
                success = false,
                message = "Webhook error: ${exception.message}"
            )
        }
    }
    
    /**
     * Generic API request sender
     */
    private suspend fun sendApiRequest(
        url: String,
        payload: Map<String, Any>,
        headers: Map<String, String>,
        platform: String
    ): IntegrationResult {
        if (url.isEmpty()) {
            return IntegrationResult(
                platform = platform,
                success = false,
                message = "API endpoint not configured"
            )
        }
        
        return try {
            val json = Json.encodeToString(payload)
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(json))
            
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            
            val request = requestBuilder.build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            IntegrationResult(
                platform = platform,
                success = response.statusCode() in 200..299,
                message = if (response.statusCode() in 200..299) "API request successful" else "API request failed with status ${response.statusCode()}",
                data = mapOf(
                    "status_code" to response.statusCode().toString(),
                    "response_body" to response.body().take(200)
                )
            )
        } catch (exception: Exception) {
            IntegrationResult(
                platform = platform,
                success = false,
                message = "API request error: ${exception.message}"
            )
        }
    }
    
    /**
     * Send webhook notifications for completed transactions or events
     */
    suspend fun sendWebhookNotification(
        sessionId: String,
        event: String,
        data: Map<String, Any> = emptyMap()
    ): Boolean {
        val variant = activeVariants[sessionId] ?: "unknown"
        
        val payload = WebhookPayload(
            event = event,
            sessionId = sessionId,
            variant = variant,
            timestamp = System.currentTimeMillis(),
            data = data
        )
        
        // Send to all configured webhook endpoints
        val webhookConfigs = integrationConfigs.values.filter { 
            it.enabled && !it.webhookUrl.isNullOrEmpty() 
        }
        
        return try {
            coroutineScope {
                webhookConfigs.map { config ->
                    async {
                        sendWebhook(config.webhookUrl!!, payload.toMap(), config.platform)
                    }
                }.awaitAll()
            }
            true
        } catch (exception: Exception) {
            false
        }
    }
    
    /**
     * Get current variant for a session
     */
    fun getVariantForSession(sessionId: String): String? {
        return activeVariants[sessionId]
    }
    
    /**
     * Update integration configuration
     */
    fun updateIntegrationConfig(platform: String, config: IntegrationConfig) {
        integrationConfigs[platform] = config
    }
    
    /**
     * Get integration configuration
     */
    fun getIntegrationConfig(platform: String): IntegrationConfig? {
        return integrationConfigs[platform]
    }
    
    /**
     * Remove integration configuration
     */
    fun removeIntegrationConfig(platform: String) {
        integrationConfigs.remove(platform)
    }
    
    /**
     * Get all active integrations
     */
    fun getActiveIntegrations(): List<String> {
        return integrationConfigs.values.filter { it.enabled }.map { it.platform }
    }
}

/**
 * Extension function to convert WebhookPayload to Map
 */
private fun WebhookPayload.toMap(): Map<String, Any> {
    return mapOf(
        "event" to event,
        "session_id" to sessionId,
        "variant" to variant,
        "timestamp" to timestamp,
        "data" to data
    )
}