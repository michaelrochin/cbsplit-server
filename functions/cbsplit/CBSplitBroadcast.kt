package functions.cbsplit

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * CBSplit Broadcast Handler for CRO Platform
 * Handles A-Z testing data broadcasting, conversion metrics transmission,
 * integration status updates, and revenue optimization data
 */
class CBSplitBroadcast {
    
    private val broadcastChannels = ConcurrentHashMap<String, BroadcastChannel<CBSplitMessage>>()
    private val messageCounter = AtomicLong(0)
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        const val CHANNEL_CAPACITY = 64
        const val DEFAULT_CHANNEL = "cbsplit_main"
        const val TESTING_CHANNEL = "cbsplit_testing"
        const val METRICS_CHANNEL = "cbsplit_metrics"
        const val STATUS_CHANNEL = "cbsplit_status"
        const val OPTIMIZATION_CHANNEL = "cbsplit_optimization"
    }
    
    /**
     * Data classes for different message types
     */
    @Serializable
    sealed class CBSplitMessage {
        abstract val messageId: Long
        abstract val timestamp: Long
        abstract val channelId: String
        
        @Serializable
        data class TestingData(
            override val messageId: Long,
            override val timestamp: Long,
            override val channelId: String,
            val testId: String,
            val variantId: String,
            val testType: String, // A/B, A/Z, Multivariate
            val userSegment: String,
            val conversionGoal: String,
            val metrics: Map<String, Double>,
            val confidence: Double,
            val sampleSize: Int,
            val status: TestStatus
        ) : CBSplitMessage()
        
        @Serializable
        data class ConversionMetrics(
            override val messageId: Long,
            override val timestamp: Long,
            override val channelId: String,
            val sessionId: String,
            val userId: String?,
            val eventType: String,
            val conversionValue: Double,
            val currency: String,
            val funnelStep: String,
            val testVariant: String?,
            val conversionPath: List<String>,
            val attribution: Map<String, String>
        ) : CBSplitMessage()
        
        @Serializable
        data class IntegrationStatus(
            override val messageId: Long,
            override val timestamp: Long,
            override val channelId: String,
            val integrationId: String,
            val platform: String,
            val status: IntegrationStatusType,
            val healthCheck: Map<String, Boolean>,
            val lastSync: Long,
            val errorCount: Int,
            val latency: Long
        ) : CBSplitMessage()
        
        @Serializable
        data class RevenueOptimization(
            override val messageId: Long,
            override val timestamp: Long,
            override val channelId: String,
            val optimizationId: String,
            val strategy: String,
            val currentRevenue: Double,
            val projectedRevenue: Double,
            val uplift: Double,
            val riskScore: Double,
            val recommendations: List<OptimizationRecommendation>,
            val automationSettings: AutomationConfig
        ) : CBSplitMessage()
    }
    
    @Serializable
    enum class TestStatus {
        DRAFT, RUNNING, PAUSED, COMPLETED, ARCHIVED
    }
    
    @Serializable
    enum class IntegrationStatusType {
        CONNECTED, DISCONNECTED, ERROR, SYNCING, MAINTENANCE
    }
    
    @Serializable
    data class OptimizationRecommendation(
        val type: String,
        val description: String,
        val impact: Double,
        val effort: String,
        val priority: Int
    )
    
    @Serializable
    data class AutomationConfig(
        val enabled: Boolean,
        val triggerThreshold: Double,
        val maxChanges: Int,
        val rollbackOnFailure: Boolean
    )
    
    /**
     * Initialize broadcast channels
     */
    init {
        initializeChannels()
    }
    
    private fun initializeChannels() {
        val channels = listOf(
            DEFAULT_CHANNEL,
            TESTING_CHANNEL,
            METRICS_CHANNEL,
            STATUS_CHANNEL,
            OPTIMIZATION_CHANNEL
        )
        
        channels.forEach { channelId ->
            broadcastChannels[channelId] = BroadcastChannel(CHANNEL_CAPACITY)
        }
    }
    
    /**
     * Broadcast testing data (A-Z testing results, variants, etc.)
     */
    suspend fun broadcastTestingData(
        testId: String,
        variantId: String,
        testType: String,
        userSegment: String,
        conversionGoal: String,
        metrics: Map<String, Double>,
        confidence: Double,
        sampleSize: Int,
        status: TestStatus,
        channelId: String = TESTING_CHANNEL
    ) {
        val message = CBSplitMessage.TestingData(
            messageId = messageCounter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            channelId = channelId,
            testId = testId,
            variantId = variantId,
            testType = testType,
            userSegment = userSegment,
            conversionGoal = conversionGoal,
            metrics = metrics,
            confidence = confidence,
            sampleSize = sampleSize,
            status = status
        )
        
        broadcastMessage(message, channelId)
    }
    
    /**
     * Broadcast conversion metrics
     */
    suspend fun broadcastConversionMetrics(
        sessionId: String,
        userId: String?,
        eventType: String,
        conversionValue: Double,
        currency: String,
        funnelStep: String,
        testVariant: String?,
        conversionPath: List<String>,
        attribution: Map<String, String>,
        channelId: String = METRICS_CHANNEL
    ) {
        val message = CBSplitMessage.ConversionMetrics(
            messageId = messageCounter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            channelId = channelId,
            sessionId = sessionId,
            userId = userId,
            eventType = eventType,
            conversionValue = conversionValue,
            currency = currency,
            funnelStep = funnelStep,
            testVariant = testVariant,
            conversionPath = conversionPath,
            attribution = attribution
        )
        
        broadcastMessage(message, channelId)
    }
    
    /**
     * Broadcast integration status updates
     */
    suspend fun broadcastIntegrationStatus(
        integrationId: String,
        platform: String,
        status: IntegrationStatusType,
        healthCheck: Map<String, Boolean>,
        lastSync: Long,
        errorCount: Int,
        latency: Long,
        channelId: String = STATUS_CHANNEL
    ) {
        val message = CBSplitMessage.IntegrationStatus(
            messageId = messageCounter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            channelId = channelId,
            integrationId = integrationId,
            platform = platform,
            status = status,
            healthCheck = healthCheck,
            lastSync = lastSync,
            errorCount = errorCount,
            latency = latency
        )
        
        broadcastMessage(message, channelId)
    }
    
    /**
     * Broadcast revenue optimization data
     */
    suspend fun broadcastRevenueOptimization(
        optimizationId: String,
        strategy: String,
        currentRevenue: Double,
        projectedRevenue: Double,
        uplift: Double,
        riskScore: Double,
        recommendations: List<OptimizationRecommendation>,
        automationSettings: AutomationConfig,
        channelId: String = OPTIMIZATION_CHANNEL
    ) {
        val message = CBSplitMessage.RevenueOptimization(
            messageId = messageCounter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            channelId = channelId,
            optimizationId = optimizationId,
            strategy = strategy,
            currentRevenue = currentRevenue,
            projectedRevenue = projectedRevenue,
            uplift = uplift,
            riskScore = riskScore,
            recommendations = recommendations,
            automationSettings = automationSettings
        )
        
        broadcastMessage(message, channelId)
    }
    
    /**
     * Generic broadcast message function
     */
    private suspend fun broadcastMessage(message: CBSplitMessage, channelId: String) {
        val channel = broadcastChannels[channelId]
        if (channel != null && !channel.isClosedForSend) {
            try {
                channel.send(message)
            } catch (e: Exception) {
                println("Failed to broadcast message to channel $channelId: ${e.message}")
            }
        } else {
            println("Channel $channelId is not available or closed")
        }
    }
    
    /**
     * Subscribe to a specific channel
     */
    fun subscribe(channelId: String = DEFAULT_CHANNEL): Flow<CBSplitMessage> {
        val channel = broadcastChannels[channelId]
        return if (channel != null) {
            channel.asFlow()
        } else {
            throw IllegalArgumentException("Channel $channelId does not exist")
        }
    }
    
    /**
     * Subscribe to multiple channels
     */
    fun subscribeToMultiple(channelIds: List<String>): Flow<CBSplitMessage> {
        // Implementation would merge multiple channel flows
        // For simplicity, returning the first valid channel
        val validChannels = channelIds.mapNotNull { broadcastChannels[it] }
        return if (validChannels.isNotEmpty()) {
            validChannels.first().asFlow()
        } else {
            throw IllegalArgumentException("No valid channels found in: $channelIds")
        }
    }
    
    /**
     * Get channel statistics
     */
    fun getChannelStats(channelId: String): Map<String, Any> {
        val channel = broadcastChannels[channelId]
        return if (channel != null) {
            mapOf(
                "channelId" to channelId,
                "capacity" to CHANNEL_CAPACITY,
                "isClosedForSend" to channel.isClosedForSend,
                "isClosedForReceive" to channel.isClosedForReceive
            )
        } else {
            emptyMap()
        }
    }
    
    /**
     * Close specific channel
     */
    fun closeChannel(channelId: String) {
        broadcastChannels[channelId]?.close()
        broadcastChannels.remove(channelId)
    }
    
    /**
     * Close all channels
     */
    fun closeAllChannels() {
        broadcastChannels.values.forEach { it.close() }
        broadcastChannels.clear()
    }
    
    /**
     * Utility functions for message serialization
     */
    fun serializeMessage(message: CBSplitMessage): String {
        return json.encodeToString(CBSplitMessage.serializer(), message)
    }
    
    fun deserializeMessage(jsonString: String): CBSplitMessage {
        return json.decodeFromString(CBSplitMessage.serializer(), jsonString)
    }
    
    /**
     * Batch broadcast multiple messages
     */
    suspend fun batchBroadcast(messages: List<CBSplitMessage>) {
        messages.forEach { message ->
            broadcastMessage(message, message.channelId)
        }
    }
    
    /**
     * Health check for broadcast system
     */
    fun healthCheck(): Map<String, Any> {
        val channelHealth = broadcastChannels.mapValues { (_, channel) ->
            mapOf(
                "isOpen" to !channel.isClosedForSend,
                "canReceive" to !channel.isClosedForReceive
            )
        }
        
        return mapOf(
            "totalChannels" to broadcastChannels.size,
            "messagesProcessed" to messageCounter.get(),
            "channels" to channelHealth,
            "systemTime" to System.currentTimeMillis()
        )
    }
}