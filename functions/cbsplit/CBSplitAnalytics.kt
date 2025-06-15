package com.cbsplit.analytics

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * CBSplit Analytics Component
 * Handles comprehensive analytics tracking for A-Z testing, revenue optimization,
 * CMS dashboard usage, integration performance, and conversion metrics
 */
class CBSplitAnalytics private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "CBSplitAnalytics"
        private const val PREFS_NAME = "cbsplit_analytics_prefs"
        private const val SESSION_TIMEOUT = 30 * 60 * 1000L // 30 minutes
        
        @Volatile
        private var INSTANCE: CBSplitAnalytics? = null
        
        fun getInstance(context: Context): CBSplitAnalytics {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CBSplitAnalytics(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    private var currentSessionId: String = generateSessionId()
    private var sessionStartTime: Long = System.currentTimeMillis()
    
    // Analytics Data Classes
    data class ConversionMetric(
        val testId: String,
        val variant: String,
        val conversionType: String,
        val value: Double,
        val timestamp: Long,
        val sessionId: String,
        val userId: String? = null
    )
    
    data class RevenueData(
        val testId: String,
        val variant: String,
        val revenue: Double,
        val currency: String,
        val transactionId: String,
        val timestamp: Long,
        val campaignParams: Map<String, String> = emptyMap()
    )
    
    data class CmsUsageMetric(
        val action: String,
        val page: String,
        val duration: Long,
        val timestamp: Long,
        val userId: String,
        val features: List<String> = emptyList()
    )
    
    data class IntegrationPerformance(
        val integrationName: String,
        val operationType: String,
        val responseTime: Long,
        val success: Boolean,
        val errorCode: String? = null,
        val timestamp: Long
    )
    
    data class PixelEvent(
        val pixelId: String,
        val eventType: String,
        val parameters: Map<String, Any>,
        val timestamp: Long,
        val testId: String? = null,
        val variant: String? = null
    )
    
    data class CampaignParams(
        val source: String? = null,
        val medium: String? = null,
        val campaign: String? = null,
        val term: String? = null,
        val content: String? = null,
        val gclid: String? = null,
        val fbclid: String? = null
    )
    
    // Conversion Metrics Tracking
    fun trackConversion(
        testId: String,
        variant: String,
        conversionType: String,
        value: Double = 0.0,
        userId: String? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val metric = ConversionMetric(
                    testId = testId,
                    variant = variant,
                    conversionType = conversionType,
                    value = value,
                    timestamp = System.currentTimeMillis(),
                    sessionId = currentSessionId,
                    userId = userId
                )
                
                logAnalyticsEvent("conversion_tracked", metric.toJson())
                persistConversionMetric(metric)
                
                // Calculate and update conversion rates
                updateConversionRates(testId, variant, conversionType)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking conversion: ${e.message}", e)
            }
        }
    }
    
    // Revenue Optimization Tracking
    fun trackRevenue(
        testId: String,
        variant: String,
        revenue: Double,
        currency: String = "USD",
        transactionId: String,
        campaignParams: CampaignParams? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val revenueData = RevenueData(
                    testId = testId,
                    variant = variant,
                    revenue = revenue,
                    currency = currency,
                    transactionId = transactionId,
                    timestamp = System.currentTimeMillis(),
                    campaignParams = campaignParams?.toMap() ?: emptyMap()
                )
                
                logAnalyticsEvent("revenue_tracked", revenueData.toJson())
                persistRevenueData(revenueData)
                
                // Update revenue optimization metrics
                updateRevenueMetrics(testId, variant, revenue)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking revenue: ${e.message}", e)
            }
        }
    }
    
    // CMS Dashboard Usage Tracking
    fun trackCmsUsage(
        action: String,
        page: String,
        startTime: Long,
        userId: String,
        features: List<String> = emptyList()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val duration = System.currentTimeMillis() - startTime
                val usageMetric = CmsUsageMetric(
                    action = action,
                    page = page,
                    duration = duration,
                    timestamp = System.currentTimeMillis(),
                    userId = userId,
                    features = features
                )
                
                logAnalyticsEvent("cms_usage_tracked", usageMetric.toJson())
                persistCmsUsageMetric(usageMetric)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking CMS usage: ${e.message}", e)
            }
        }
    }
    
    // Integration Performance Tracking
    fun trackIntegrationPerformance(
        integrationName: String,
        operationType: String,
        startTime: Long,
        success: Boolean,
        errorCode: String? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val responseTime = System.currentTimeMillis() - startTime
                val performance = IntegrationPerformance(
                    integrationName = integrationName,
                    operationType = operationType,
                    responseTime = responseTime,
                    success = success,
                    errorCode = errorCode,
                    timestamp = System.currentTimeMillis()
                )
                
                logAnalyticsEvent("integration_performance_tracked", performance.toJson())
                persistIntegrationPerformance(performance)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking integration performance: ${e.message}", e)
            }
        }
    }
    
    // Pixel Firing Analytics
    fun firePixel(
        pixelId: String,
        eventType: String,
        parameters: Map<String, Any>,
        testId: String? = null,
        variant: String? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pixelEvent = PixelEvent(
                    pixelId = pixelId,
                    eventType = eventType,
                    parameters = parameters,
                    timestamp = System.currentTimeMillis(),
                    testId = testId,
                    variant = variant
                )
                
                logAnalyticsEvent("pixel_fired", pixelEvent.toJson())
                persistPixelEvent(pixelEvent)
                
                // Send pixel to external systems
                sendPixelToExternalSystems(pixelEvent)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error firing pixel: ${e.message}", e)
            }
        }
    }
    
    // Conversion Rate Calculations
    fun calculateConversionRate(testId: String, variant: String, conversionType: String): Double {
        return try {
            val totalSessions = getTotalSessions(testId, variant)
            val conversions = getConversions(testId, variant, conversionType)
            
            if (totalSessions > 0) {
                (conversions.toDouble() / totalSessions.toDouble()) * 100.0
            } else {
                0.0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating conversion rate: ${e.message}", e)
            0.0
        }
    }
    
    // Campaign Parameter Tracking
    fun trackCampaignParameters(campaignParams: CampaignParams) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val params = campaignParams.toMap()
                logAnalyticsEvent("campaign_parameters_tracked", JSONObject(params))
                persistCampaignParameters(params)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking campaign parameters: ${e.message}", e)
            }
        }
    }
    
    // Session Management
    fun startSession(userId: String? = null) {
        try {
            currentSessionId = generateSessionId()
            sessionStartTime = System.currentTimeMillis()
            
            val sessionData = mapOf(
                "sessionId" to currentSessionId,
                "startTime" to sessionStartTime,
                "userId" to userId
            )
            
            logAnalyticsEvent("session_started", JSONObject(sessionData))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting session: ${e.message}", e)
        }
    }
    
    fun endSession() {
        try {
            val sessionDuration = System.currentTimeMillis() - sessionStartTime
            val sessionData = mapOf(
                "sessionId" to currentSessionId,
                "duration" to sessionDuration,
                "endTime" to System.currentTimeMillis()
            )
            
            logAnalyticsEvent("session_ended", JSONObject(sessionData))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error ending session: ${e.message}", e)
        }
    }
    
    // Utility Methods
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    }
    
    private fun logAnalyticsEvent(eventName: String, data: JSONObject) {
        Log.d(TAG, "Analytics Event: $eventName - Data: $data")
        // Send to analytics service (Firebase, Mixpanel, etc.)
    }
    
    private fun persistConversionMetric(metric: ConversionMetric) {
        // Persist to local database or send to server
        val key = "conversion_${metric.testId}_${metric.variant}_${metric.timestamp}"
        prefs.edit().putString(key, metric.toJson().toString()).apply()
    }
    
    private fun persistRevenueData(revenueData: RevenueData) {
        val key = "revenue_${revenueData.testId}_${revenueData.variant}_${revenueData.timestamp}"
        prefs.edit().putString(key, revenueData.toJson().toString()).apply()
    }
    
    private fun persistCmsUsageMetric(usageMetric: CmsUsageMetric) {
        val key = "cms_usage_${usageMetric.userId}_${usageMetric.timestamp}"
        prefs.edit().putString(key, usageMetric.toJson().toString()).apply()
    }
    
    private fun persistIntegrationPerformance(performance: IntegrationPerformance) {
        val key = "integration_${performance.integrationName}_${performance.timestamp}"
        prefs.edit().putString(key, performance.toJson().toString()).apply()
    }
    
    private fun persistPixelEvent(pixelEvent: PixelEvent) {
        val key = "pixel_${pixelEvent.pixelId}_${pixelEvent.timestamp}"
        prefs.edit().putString(key, pixelEvent.toJson().toString()).apply()
    }
    
    private fun persistCampaignParameters(params: Map<String, String?>) {
        val key = "campaign_params_${System.currentTimeMillis()}"
        prefs.edit().putString(key, JSONObject(params).toString()).apply()
    }
    
    private fun updateConversionRates(testId: String, variant: String, conversionType: String) {
        val conversionRate = calculateConversionRate(testId, variant, conversionType)
        val key = "conversion_rate_${testId}_${variant}_${conversionType}"
        prefs.edit().putFloat(key, conversionRate.toFloat()).apply()
    }
    
    private fun updateRevenueMetrics(testId: String, variant: String, revenue: Double) {
        val currentRevenue = prefs.getFloat("total_revenue_${testId}_${variant}", 0f)
        val newRevenue = currentRevenue + revenue.toFloat()
        prefs.edit().putFloat("total_revenue_${testId}_${variant}", newRevenue).apply()
    }
    
    private fun getTotalSessions(testId: String, variant: String): Int {
        return prefs.getInt("total_sessions_${testId}_${variant}", 0)
    }
    
    private fun getConversions(testId: String, variant: String, conversionType: String): Int {
        return prefs.getInt("conversions_${testId}_${variant}_${conversionType}", 0)
    }
    
    private fun sendPixelToExternalSystems(pixelEvent: PixelEvent) {
        // Implementation for sending pixels to external analytics systems
        // Facebook Pixel, Google Analytics, etc.
    }
    
    // Extension functions for JSON serialization
    private fun ConversionMetric.toJson(): JSONObject {
        return JSONObject().apply {
            put("testId", testId)
            put("variant", variant)
            put("conversionType", conversionType)
            put("value", value)
            put("timestamp", timestamp)
            put("sessionId", sessionId)
            put("userId", userId)
        }
    }
    
    private fun RevenueData.toJson(): JSONObject {
        return JSONObject().apply {
            put("testId", testId)
            put("variant", variant)
            put("revenue", revenue)
            put("currency", currency)
            put("transactionId", transactionId)
            put("timestamp", timestamp)
            put("campaignParams", JSONObject(campaignParams))
        }
    }
    
    private fun CmsUsageMetric.toJson(): JSONObject {
        return JSONObject().apply {
            put("action", action)
            put("page", page)
            put("duration", duration)
            put("timestamp", timestamp)
            put("userId", userId)
            put("features", features.joinToString(","))
        }
    }
    
    private fun IntegrationPerformance.toJson(): JSONObject {
        return JSONObject().apply {
            put("integrationName", integrationName)
            put("operationType", operationType)
            put("responseTime", responseTime)
            put("success", success)
            put("errorCode", errorCode)
            put("timestamp", timestamp)
        }
    }
    
    private fun PixelEvent.toJson(): JSONObject {
        return JSONObject().apply {
            put("pixelId", pixelId)
            put("eventType", eventType)
            put("parameters", JSONObject(parameters))
            put("timestamp", timestamp)
            put("testId", testId)
            put("variant", variant)
        }
    }
    
    private fun CampaignParams.toMap(): Map<String, String?> {
        return mapOf(
            "source" to source,
            "medium" to medium,
            "campaign" to campaign,
            "term" to term,
            "content" to content,
            "gclid" to gclid,
            "fbclid" to fbclid
        )
    }
}