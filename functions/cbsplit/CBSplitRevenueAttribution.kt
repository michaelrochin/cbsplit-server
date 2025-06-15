package functions.cbsplit

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class AttributionModel(
    val type: String, // "first_touch", "last_touch", "linear", "time_decay", "position_based"
    val lookbackDays: Int = 30,
    val weights: Map<String, Double> = emptyMap()
)

@Serializable
data class RevenueAttribution(
    val sessionId: String,
    val orderId: String,
    val revenue: Double,
    val currency: String,
    val timestamp: Long,
    val attributionModel: String,
    val touchpointContributions: List<TouchpointContribution>,
    val testId: String,
    val variant: String,
    val customerLtv: Double = 0.0
)

@Serializable
data class TouchpointContribution(
    val touchpointId: String,
    val source: String,
    val medium: String,
    val campaign: String,
    val variant: String,
    val contribution: Double, // Percentage of revenue attributed to this touchpoint
    val timestamp: Long,
    val position: Int // Position in customer journey (1 = first, etc.)
)

@Serializable
data class CustomerJourney(
    val customerId: String,
    val sessionId: String,
    val touchpoints: MutableList<CustomerTouchpoint> = mutableListOf(),
    val conversions: MutableList<CustomerConversion> = mutableListOf(),
    val totalRevenue: Double = 0.0,
    val firstTouchTimestamp: Long = 0L,
    val lastTouchTimestamp: Long = 0L
)

@Serializable
data class CustomerTouchpoint(
    val id: String,
    val timestamp: Long,
    val source: String,
    val medium: String,
    val campaign: String,
    val variant: String,
    val testId: String,
    val page: String,
    val action: String,
    val value: Double = 0.0
)

@Serializable
data class CustomerConversion(
    val orderId: String,
    val timestamp: Long,
    val revenue: Double,
    val currency: String,
    val type: String, // "lead", "trial", "purchase", "upsell"
    val attributedTouchpoints: List<String> = emptyList()
)

object CBSplitRevenueAttribution {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val customerJourneys = ConcurrentHashMap<String, CustomerJourney>()
    private val attributionModels = mutableMapOf<String, AttributionModel>()
    private val revenueAttributions = mutableListOf<RevenueAttribution>()
    
    init {
        // Initialize default attribution models
        setupDefaultAttributionModels()
    }
    
    /**
     * Sets up default attribution models
     */
    private fun setupDefaultAttributionModels() {
        attributionModels["first_touch"] = AttributionModel(
            type = "first_touch",
            lookbackDays = 30,
            weights = mapOf("first" to 1.0)
        )
        
        attributionModels["last_touch"] = AttributionModel(
            type = "last_touch",
            lookbackDays = 30,
            weights = mapOf("last" to 1.0)
        )
        
        attributionModels["linear"] = AttributionModel(
            type = "linear",
            lookbackDays = 30
        )
        
        attributionModels["time_decay"] = AttributionModel(
            type = "time_decay",
            lookbackDays = 30,
            weights = mapOf("decay_rate" to 0.7) // 7-day half-life
        )
        
        attributionModels["position_based"] = AttributionModel(
            type = "position_based",
            lookbackDays = 30,
            weights = mapOf(
                "first" to 0.4,
                "last" to 0.4,
                "middle" to 0.2
            )
        )
    }
    
    /**
     * Attributes revenue to touchpoints based on customer journey
     */
    suspend fun attributeRevenue(
        sessionId: String,
        orderId: String,
        revenue: Double,
        currency: String = "USD",
        customerId: String? = null,
        attributionModelType: String = "first_touch",
        timestamp: Long = System.currentTimeMillis()
    ): RevenueAttribution? {
        
        val customerKey = customerId ?: sessionId
        val journey = customerJourneys[customerKey] ?: return null
        val model = attributionModels[attributionModelType] ?: attributionModels["first_touch"]!!
        
        // Add conversion to journey
        val conversion = CustomerConversion(
            orderId = orderId,
            timestamp = timestamp,
            revenue = revenue,
            currency = currency,
            type = if (revenue > 0) "purchase" else "lead"
        )
        journey.conversions.add(conversion)
        journey.totalRevenue += revenue
        
        // Calculate touchpoint contributions based on attribution model
        val contributions = calculateTouchpointContributions(journey, conversion, model)
        
        // Get test info from session
        val sessionData = CBSplitSessionBridge.getSession(sessionId)
        val testId = sessionData?.testId ?: "unknown"
        val variant = sessionData?.variant ?: "unknown"
        
        val attribution = RevenueAttribution(
            sessionId = sessionId,
            orderId = orderId,
            revenue = revenue,
            currency = currency,
            timestamp = timestamp,
            attributionModel = attributionModelType,
            touchpointContributions = contributions,
            testId = testId,
            variant = variant,
            customerLtv = journey.totalRevenue
        )
        
        revenueAttributions.add(attribution)
        
        // Update analytics with attributed revenue
        updateVariantAttribution(testId, variant, contributions, revenue)
        
        return attribution
    }
    
    /**
     * Tracks a customer touchpoint
     */
    fun trackTouchpoint(
        customerId: String,
        sessionId: String,
        source: String,
        medium: String,
        campaign: String,
        variant: String,
        testId: String,
        page: String,
        action: String,
        value: Double = 0.0,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val touchpointId = generateTouchpointId(customerId, timestamp)
        
        val touchpoint = CustomerTouchpoint(
            id = touchpointId,
            timestamp = timestamp,
            source = source,
            medium = medium,
            campaign = campaign,
            variant = variant,
            testId = testId,
            page = page,
            action = action,
            value = value
        )
        
        val journey = customerJourneys.getOrPut(customerId) {
            CustomerJourney(
                customerId = customerId,
                sessionId = sessionId,
                firstTouchTimestamp = timestamp,
                lastTouchTimestamp = timestamp
            )
        }
        
        journey.touchpoints.add(touchpoint)
        journey.lastTouchTimestamp = timestamp
        
        // Update first touch if this is earlier
        if (timestamp < journey.firstTouchTimestamp) {
            journey.firstTouchTimestamp = timestamp
        }
    }
    
    /**
     * Calculates touchpoint contributions based on attribution model
     */
    private fun calculateTouchpointContributions(
        journey: CustomerJourney,
        conversion: CustomerConversion,
        model: AttributionModel
    ): List<TouchpointContribution> {
        
        val eligibleTouchpoints = journey.touchpoints.filter { touchpoint ->
            val daysDiff = (conversion.timestamp - touchpoint.timestamp) / (24 * 60 * 60 * 1000)
            daysDiff <= model.lookbackDays
        }.sortedBy { it.timestamp }
        
        if (eligibleTouchpoints.isEmpty()) return emptyList()
        
        return when (model.type) {
            "first_touch" -> calculateFirstTouchAttribution(eligibleTouchpoints, conversion)
            "last_touch" -> calculateLastTouchAttribution(eligibleTouchpoints, conversion)
            "linear" -> calculateLinearAttribution(eligibleTouchpoints, conversion)
            "time_decay" -> calculateTimeDecayAttribution(eligibleTouchpoints, conversion, model)
            "position_based" -> calculatePositionBasedAttribution(eligibleTouchpoints, conversion, model)
            else -> calculateFirstTouchAttribution(eligibleTouchpoints, conversion)
        }
    }
    
    /**
     * First-touch attribution: 100% credit to first touchpoint
     */
    private fun calculateFirstTouchAttribution(
        touchpoints: List<CustomerTouchpoint>,
        conversion: CustomerConversion
    ): List<TouchpointContribution> {
        val firstTouchpoint = touchpoints.first()
        
        return listOf(
            TouchpointContribution(
                touchpointId = firstTouchpoint.id,
                source = firstTouchpoint.source,
                medium = firstTouchpoint.medium,
                campaign = firstTouchpoint.campaign,
                variant = firstTouchpoint.variant,
                contribution = 1.0,
                timestamp = firstTouchpoint.timestamp,
                position = 1
            )
        )
    }
    
    /**
     * Last-touch attribution: 100% credit to last touchpoint
     */
    private fun calculateLastTouchAttribution(
        touchpoints: List<CustomerTouchpoint>,
        conversion: CustomerConversion
    ): List<TouchpointContribution> {
        val lastTouchpoint = touchpoints.last()
        
        return listOf(
            TouchpointContribution(
                touchpointId = lastTouchpoint.id,
                source = lastTouchpoint.source,
                medium = lastTouchpoint.medium,
                campaign = lastTouchpoint.campaign,
                variant = lastTouchpoint.variant,
                contribution = 1.0,
                timestamp = lastTouchpoint.timestamp,
                position = touchpoints.size
            )
        )
    }
    
    /**
     * Linear attribution: Equal credit to all touchpoints
     */
    private fun calculateLinearAttribution(
        touchpoints: List<CustomerTouchpoint>,
        conversion: CustomerConversion
    ): List<TouchpointContribution> {
        val contribution = 1.0 / touchpoints.size
        
        return touchpoints.mapIndexed { index, touchpoint ->
            TouchpointContribution(
                touchpointId = touchpoint.id,
                source = touchpoint.source,
                medium = touchpoint.medium,
                campaign = touchpoint.campaign,
                variant = touchpoint.variant,
                contribution = contribution,
                timestamp = touchpoint.timestamp,
                position = index + 1
            )
        }
    }
    
    /**
     * Time-decay attribution: More recent touchpoints get more credit
     */
    private fun calculateTimeDecayAttribution(
        touchpoints: List<CustomerTouchpoint>,
        conversion: CustomerConversion,
        model: AttributionModel
    ): List<TouchpointContribution> {
        val decayRate = model.weights["decay_rate"] ?: 0.7
        val halfLifeDays = 7.0
        
        val weights = touchpoints.map { touchpoint ->
            val daysSinceTouch = (conversion.timestamp - touchpoint.timestamp) / (24 * 60 * 60 * 1000.0)
            Math.pow(decayRate, daysSinceTouch / halfLifeDays)
        }
        
        val totalWeight = weights.sum()
        
        return touchpoints.mapIndexed { index, touchpoint ->
            TouchpointContribution(
                touchpointId = touchpoint.id,
                source = touchpoint.source,
                medium = touchpoint.medium,
                campaign = touchpoint.campaign,
                variant = touchpoint.variant,
                contribution = weights[index] / totalWeight,
                timestamp = touchpoint.timestamp,
                position = index + 1
            )
        }
    }
    
    /**
     * Position-based attribution: 40% first, 40% last, 20% middle
     */
    private fun calculatePositionBasedAttribution(
        touchpoints: List<CustomerTouchpoint>,
        conversion: CustomerConversion,
        model: AttributionModel
    ): List<TouchpointContribution> {
        val firstWeight = model.weights["first"] ?: 0.4
        val lastWeight = model.weights["last"] ?: 0.4
        val middleWeight = model.weights["middle"] ?: 0.2
        
        return when (touchpoints.size) {
            1 -> {
                listOf(
                    TouchpointContribution(
                        touchpointId = touchpoints[0].id,
                        source = touchpoints[0].source,
                        medium = touchpoints[0].medium,
                        campaign = touchpoints[0].campaign,
                        variant = touchpoints[0].variant,
                        contribution = 1.0,
                        timestamp = touchpoints[0].timestamp,
                        position = 1
                    )
                )
            }
            2 -> {
                listOf(
                    TouchpointContribution(
                        touchpointId = touchpoints[0].id,
                        source = touchpoints[0].source,
                        medium = touchpoints[0].medium,
                        campaign = touchpoints[0].campaign,
                        variant = touchpoints[0].variant,
                        contribution = firstWeight + middleWeight / 2,
                        timestamp = touchpoints[0].timestamp,
                        position = 1
                    ),
                    TouchpointContribution(
                        touchpointId = touchpoints[1].id,
                        source = touchpoints[1].source,
                        medium = touchpoints[1].medium,
                        campaign = touchpoints[1].campaign,
                        variant = touchpoints[1].variant,
                        contribution = lastWeight + middleWeight / 2,
                        timestamp = touchpoints[1].timestamp,
                        position = 2
                    )
                )
            }
            else -> {
                val middleContribution = middleWeight / (touchpoints.size - 2)
                
                touchpoints.mapIndexed { index, touchpoint ->
                    val contribution = when (index) {
                        0 -> firstWeight
                        touchpoints.size - 1 -> lastWeight
                        else -> middleContribution
                    }
                    
                    TouchpointContribution(
                        touchpointId = touchpoint.id,
                        source = touchpoint.source,
                        medium = touchpoint.medium,
                        campaign = touchpoint.campaign,
                        variant = touchpoint.variant,
                        contribution = contribution,
                        timestamp = touchpoint.timestamp,
                        position = index + 1
                    )
                }
            }
        }
    }
    
    /**
     * Updates variant attribution analytics
     */
    private suspend fun updateVariantAttribution(
        testId: String,
        variant: String,
        contributions: List<TouchpointContribution>,
        revenue: Double
    ) {
        val variantContribution = contributions
            .filter { it.variant == variant }
            .sumOf { it.contribution }
        
        val attributedRevenue = revenue * variantContribution
        
        // Update CBSplit analytics
        val analytics = CBSplitAnalytics.getInstance(null)
        analytics.trackRevenue(testId, variant, attributedRevenue, "USD", "attributed_${System.currentTimeMillis()}")
    }
    
    /**
     * Gets attribution analytics for a test
     */
    fun getAttributionAnalytics(testId: String): Map<String, Any> {
        val testAttributions = revenueAttributions.filter { it.testId == testId }
        
        val variantBreakdown = testAttributions.groupBy { it.variant }
            .mapValues { (variant, attributions) ->
                mapOf(
                    "totalRevenue" to attributions.sumOf { it.revenue },
                    "conversions" to attributions.size,
                    "avgOrderValue" to if (attributions.isNotEmpty()) attributions.sumOf { it.revenue } / attributions.size else 0.0,
                    "touchpointBreakdown" to attributions.flatMap { it.touchpointContributions }
                        .groupBy { "${it.source}/${it.medium}" }
                        .mapValues { (_, contributions) ->
                            mapOf(
                                "totalContribution" to contributions.sumOf { it.contribution },
                                "touchpoints" to contributions.size
                            )
                        }
                )
            }
        
        return mapOf(
            "testId" to testId,
            "totalRevenue" to testAttributions.sumOf { it.revenue },
            "totalConversions" to testAttributions.size,
            "variantBreakdown" to variantBreakdown,
            "attributionModel" to testAttributions.firstOrNull()?.attributionModel ?: "unknown"
        )
    }
    
    /**
     * Gets customer lifetime value analytics
     */
    fun getCustomerLtvAnalytics(customerId: String): Map<String, Any>? {
        val journey = customerJourneys[customerId] ?: return null
        
        return mapOf(
            "customerId" to customerId,
            "totalRevenue" to journey.totalRevenue,
            "totalTouchpoints" to journey.touchpoints.size,
            "totalConversions" to journey.conversions.size,
            "firstTouchDate" to journey.firstTouchTimestamp,
            "lastTouchDate" to journey.lastTouchTimestamp,
            "averageOrderValue" to if (journey.conversions.isNotEmpty()) {
                journey.conversions.sumOf { it.revenue } / journey.conversions.size
            } else 0.0,
            "touchpointSources" to journey.touchpoints.groupBy { it.source }.mapValues { it.value.size },
            "conversionTimeline" to journey.conversions.sortedBy { it.timestamp }
        )
    }
    
    /**
     * Generates touchpoint ID
     */
    private fun generateTouchpointId(customerId: String, timestamp: Long): String {
        return "${customerId}_${timestamp}_${(0..999).random()}"
    }
    
    /**
     * Cleanup old attribution data
     */
    fun cleanupOldAttributions(daysToKeep: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        
        // Remove old revenue attributions
        revenueAttributions.removeAll { it.timestamp < cutoffTime }
        
        // Remove old customer journeys
        customerJourneys.entries.removeAll { (_, journey) ->
            journey.lastTouchTimestamp < cutoffTime
        }
    }
    
    /**
     * Exports attribution data for analysis
     */
    fun exportAttributionData(testId: String? = null): String {
        val dataToExport = if (testId != null) {
            revenueAttributions.filter { it.testId == testId }
        } else {
            revenueAttributions
        }
        
        return json.encodeToString(dataToExport)
    }
}