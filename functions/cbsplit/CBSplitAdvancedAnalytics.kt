package functions.cbsplit

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.math.*

@Serializable
data class AdvancedTestMetrics(
    val testId: String,
    val variants: List<VariantMetrics>,
    val statisticalSignificance: Map<String, Double>, // Variant pair -> confidence level
    val winnerAnalysis: WinnerAnalysis?,
    val segmentAnalysis: List<SegmentMetrics>,
    val timeSeriesData: List<TimeSeriesPoint>,
    val funnelMetrics: Map<String, FunnelAnalytics>,
    val revenueMetrics: RevenueAnalytics,
    val trafficQuality: TrafficQualityMetrics
)

@Serializable
data class VariantMetrics(
    val variant: String,
    val sessions: Int,
    val conversions: Int,
    val conversionRate: Double,
    val revenue: Double,
    val revenuePerVisitor: Double,
    val averageOrderValue: Double,
    val confidence: Double,
    val standardError: Double,
    val significanceVsControl: Double? = null,
    val lift: Double? = null // Percentage improvement over control
)

@Serializable
data class WinnerAnalysis(
    val winningVariant: String,
    val confidence: Double,
    val expectedLift: Double,
    val projectedRevenue: Double,
    val requiredSampleSize: Int,
    val currentSampleSize: Int,
    val daysToSignificance: Int?,
    val recommendation: String
)

@Serializable
data class SegmentMetrics(
    val segmentName: String,
    val segmentValue: String,
    val variants: List<VariantMetrics>,
    val sampleSize: Int,
    val significance: Double
)

@Serializable
data class TimeSeriesPoint(
    val timestamp: Long,
    val variant: String,
    val sessions: Int,
    val conversions: Int,
    val revenue: Double,
    val conversionRate: Double
)

@Serializable
data class RevenueAnalytics(
    val totalRevenue: Double,
    val revenueByVariant: Map<String, Double>,
    val revenueBySource: Map<String, Double>,
    val customerLifetimeValue: Map<String, Double>, // Variant -> CLV
    val returnOnAdSpend: Map<String, Double>, // Variant -> ROAS
    val averageOrderValue: Map<String, Double>,
    val revenueProjections: Map<String, Double> // 30-day projections
)

@Serializable
data class TrafficQualityMetrics(
    val trafficSources: Map<String, TrafficSourceMetrics>,
    val deviceBreakdown: Map<String, DeviceMetrics>,
    val geoBreakdown: Map<String, GeoMetrics>,
    val timeOfDayAnalysis: Map<Int, TimeSlotMetrics>,
    val fraudDetection: FraudMetrics
)

@Serializable
data class TrafficSourceMetrics(
    val source: String,
    val sessions: Int,
    val conversionRate: Double,
    val revenue: Double,
    val quality: String // "high", "medium", "low"
)

@Serializable
data class DeviceMetrics(
    val device: String,
    val sessions: Int,
    val conversionRate: Double,
    val revenue: Double
)

@Serializable
data class GeoMetrics(
    val country: String,
    val sessions: Int,
    val conversionRate: Double,
    val revenue: Double
)

@Serializable
data class TimeSlotMetrics(
    val hour: Int,
    val sessions: Int,
    val conversionRate: Double,
    val revenue: Double
)

@Serializable
data class FraudMetrics(
    val suspiciousTraffic: Int,
    val fraudScore: Double, // 0-100
    val patterns: List<String>,
    val blockedSessions: Int
)

object CBSplitAdvancedAnalytics {
    
    /**
     * Generates comprehensive test analytics
     */
    fun generateAdvancedTestMetrics(testId: String): AdvancedTestMetrics? {
        val sessionAnalytics = CBSplitSessionBridge.getSessionAnalytics(testId)
        val sessions = CBSplitSessionBridge.getSessionsForTest(testId)
        
        if (sessions.isEmpty()) return null
        
        // Calculate variant metrics
        val variantMetrics = calculateVariantMetrics(sessions, testId)
        
        // Calculate statistical significance
        val significance = calculateStatisticalSignificance(variantMetrics)
        
        // Winner analysis
        val winnerAnalysis = analyzeWinner(variantMetrics, sessions.size)
        
        // Segment analysis
        val segmentAnalysis = performSegmentAnalysis(sessions)
        
        // Time series data
        val timeSeriesData = generateTimeSeriesData(sessions)
        
        // Funnel metrics
        val funnelMetrics = getFunnelMetricsForTest(testId)
        
        // Revenue analytics
        val revenueAnalytics = calculateRevenueAnalytics(sessions, testId)
        
        // Traffic quality
        val trafficQuality = analyzeTrafficQuality(sessions)
        
        return AdvancedTestMetrics(
            testId = testId,
            variants = variantMetrics,
            statisticalSignificance = significance,
            winnerAnalysis = winnerAnalysis,
            segmentAnalysis = segmentAnalysis,
            timeSeriesData = timeSeriesData,
            funnelMetrics = funnelMetrics,
            revenueMetrics = revenueAnalytics,
            trafficQuality = trafficQuality
        )
    }
    
    /**
     * Calculates detailed metrics for each variant
     */
    private fun calculateVariantMetrics(
        sessions: List<CBSplitSessionBridge.SessionData>,
        testId: String
    ): List<VariantMetrics> {
        val attributions = CBSplitRevenueAttribution.getAttributionAnalytics(testId)
        val variantData = attributions["variantBreakdown"] as? Map<String, Map<String, Any>> ?: emptyMap()
        
        return sessions.groupBy { it.variant }.map { (variant, variantSessions) ->
            val conversions = variantSessions.count { session ->
                // Check if session has conversions
                session.touchpoints.any { it.action in listOf("purchase", "conversion", "lead") }
            }
            
            val revenue = (variantData[variant]?.get("totalRevenue") as? Double) ?: 0.0
            val sessionCount = variantSessions.size
            val conversionRate = if (sessionCount > 0) conversions.toDouble() / sessionCount else 0.0
            val revenuePerVisitor = if (sessionCount > 0) revenue / sessionCount else 0.0
            val averageOrderValue = if (conversions > 0) revenue / conversions else 0.0
            
            // Calculate confidence intervals
            val standardError = sqrt((conversionRate * (1 - conversionRate)) / sessionCount)
            val confidence = calculateConfidenceInterval(conversionRate, standardError)
            
            VariantMetrics(
                variant = variant,
                sessions = sessionCount,
                conversions = conversions,
                conversionRate = conversionRate,
                revenue = revenue,
                revenuePerVisitor = revenuePerVisitor,
                averageOrderValue = averageOrderValue,
                confidence = confidence,
                standardError = standardError
            )
        }.sortedBy { it.variant }
    }
    
    /**
     * Calculates statistical significance between variants
     */
    private fun calculateStatisticalSignificance(variants: List<VariantMetrics>): Map<String, Double> {
        val significance = mutableMapOf<String, Double>()
        val control = variants.firstOrNull() ?: return significance
        
        variants.drop(1).forEach { variant ->
            val zScore = calculateZScore(control, variant)
            val pValue = calculatePValue(zScore)
            val confidenceLevel = (1 - pValue) * 100
            
            significance["${control.variant}_vs_${variant.variant}"] = confidenceLevel
        }
        
        return significance
    }
    
    /**
     * Analyzes the winning variant
     */
    private fun analyzeWinner(variants: List<VariantMetrics>, totalSessions: Int): WinnerAnalysis? {
        if (variants.size < 2) return null
        
        val bestVariant = variants.maxByOrNull { it.revenuePerVisitor } ?: return null
        val control = variants.first()
        
        val lift = if (control.revenuePerVisitor > 0) {
            ((bestVariant.revenuePerVisitor - control.revenuePerVisitor) / control.revenuePerVisitor) * 100
        } else 0.0
        
        val zScore = calculateZScore(control, bestVariant)
        val confidence = (1 - calculatePValue(zScore)) * 100
        
        val requiredSampleSize = calculateRequiredSampleSize(control.conversionRate, lift / 100)
        val currentSampleSize = totalSessions
        val remainingSample = maxOf(0, requiredSampleSize - currentSampleSize)
        
        // Estimate days to significance based on current traffic
        val dailyTraffic = estimateDailyTraffic(totalSessions)
        val daysToSignificance = if (dailyTraffic > 0 && remainingSample > 0) {
            (remainingSample / dailyTraffic).toInt()
        } else null
        
        val recommendation = when {
            confidence >= 95 -> "Declare winner - ${bestVariant.variant} is statistically significant"
            confidence >= 90 -> "Strong evidence for ${bestVariant.variant} - consider running a bit longer"
            confidence >= 80 -> "Promising results for ${bestVariant.variant} - continue test"
            totalSessions < requiredSampleSize / 2 -> "Test is too early - need more data"
            else -> "No clear winner yet - continue testing"
        }
        
        return WinnerAnalysis(
            winningVariant = bestVariant.variant,
            confidence = confidence,
            expectedLift = lift,
            projectedRevenue = bestVariant.revenuePerVisitor * 1000, // Projected for 1000 visitors
            requiredSampleSize = requiredSampleSize,
            currentSampleSize = currentSampleSize,
            daysToSignificance = daysToSignificance,
            recommendation = recommendation
        )
    }
    
    /**
     * Performs segment analysis
     */
    private fun performSegmentAnalysis(sessions: List<CBSplitSessionBridge.SessionData>): List<SegmentMetrics> {
        val segments = mutableListOf<SegmentMetrics>()
        
        // Segment by traffic source
        val sourceSegments = sessions.groupBy { it.originalSource }
        sourceSegments.forEach { (source, sourceSessions) ->
            if (sourceSessions.size >= 30) { // Minimum sample size
                val variantMetrics = calculateSegmentVariantMetrics(sourceSessions)
                val significance = calculateSegmentSignificance(variantMetrics)
                
                segments.add(
                    SegmentMetrics(
                        segmentName = "traffic_source",
                        segmentValue = source,
                        variants = variantMetrics,
                        sampleSize = sourceSessions.size,
                        significance = significance
                    )
                )
            }
        }
        
        // Segment by time of day
        val hourSegments = sessions.groupBy { 
            java.time.Instant.ofEpochMilli(it.timestamp).atZone(java.time.ZoneId.systemDefault()).hour
        }
        hourSegments.forEach { (hour, hourSessions) ->
            if (hourSessions.size >= 20) {
                val variantMetrics = calculateSegmentVariantMetrics(hourSessions)
                val significance = calculateSegmentSignificance(variantMetrics)
                
                segments.add(
                    SegmentMetrics(
                        segmentName = "hour_of_day",
                        segmentValue = hour.toString(),
                        variants = variantMetrics,
                        sampleSize = hourSessions.size,
                        significance = significance
                    )
                )
            }
        }
        
        return segments
    }
    
    /**
     * Generates time series data for visualization
     */
    private fun generateTimeSeriesData(sessions: List<CBSplitSessionBridge.SessionData>): List<TimeSeriesPoint> {
        val dailyData = sessions.groupBy { session ->
            val instant = java.time.Instant.ofEpochMilli(session.timestamp)
            val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            date.toEpochDay()
        }
        
        return dailyData.flatMap { (dayEpoch, daySessions) ->
            val timestamp = dayEpoch * 24 * 60 * 60 * 1000
            
            daySessions.groupBy { it.variant }.map { (variant, variantSessions) ->
                val conversions = variantSessions.count { session ->
                    session.touchpoints.any { it.action in listOf("purchase", "conversion") }
                }
                val revenue = variantSessions.sumOf { session ->
                    session.touchpoints.filter { it.action == "purchase" }.sumOf { it.value }
                }
                val conversionRate = if (variantSessions.isNotEmpty()) {
                    conversions.toDouble() / variantSessions.size
                } else 0.0
                
                TimeSeriesPoint(
                    timestamp = timestamp,
                    variant = variant,
                    sessions = variantSessions.size,
                    conversions = conversions,
                    revenue = revenue,
                    conversionRate = conversionRate
                )
            }
        }.sortedBy { it.timestamp }
    }
    
    /**
     * Gets funnel metrics for the test
     */
    private fun getFunnelMetricsForTest(testId: String): Map<String, FunnelAnalytics> {
        return CBSplitFunnelTracking.getVariantComparison(testId)
    }
    
    /**
     * Calculates comprehensive revenue analytics
     */
    private fun calculateRevenueAnalytics(
        sessions: List<CBSplitSessionBridge.SessionData>,
        testId: String
    ): RevenueAnalytics {
        val attributionData = CBSplitRevenueAttribution.getAttributionAnalytics(testId)
        val totalRevenue = attributionData["totalRevenue"] as? Double ?: 0.0
        val variantBreakdown = attributionData["variantBreakdown"] as? Map<String, Map<String, Any>> ?: emptyMap()
        
        val revenueByVariant = variantBreakdown.mapValues { (_, data) ->
            data["totalRevenue"] as? Double ?: 0.0
        }
        
        val revenueBySource = sessions.groupBy { it.originalSource }
            .mapValues { (_, sourceSessions) ->
                sourceSessions.sumOf { session ->
                    session.touchpoints.filter { it.action == "purchase" }.sumOf { it.value }
                }
            }
        
        // Calculate CLV projections (simplified)
        val customerLifetimeValue = revenueByVariant.mapValues { (_, revenue) ->
            revenue * 1.5 // Simplified 1.5x multiplier for CLV projection
        }
        
        // Calculate ROAS (assuming 30% of revenue is ad spend)
        val returnOnAdSpend = revenueByVariant.mapValues { (_, revenue) ->
            if (revenue > 0) revenue / (revenue * 0.3) else 0.0
        }
        
        val averageOrderValue = variantBreakdown.mapValues { (_, data) ->
            val conversions = data["conversions"] as? Int ?: 0
            val revenue = data["totalRevenue"] as? Double ?: 0.0
            if (conversions > 0) revenue / conversions else 0.0
        }
        
        // 30-day revenue projections based on current daily average
        val dailyRevenue = totalRevenue / 30 // Assuming 30 days of data
        val revenueProjections = revenueByVariant.mapValues { (_, _) ->
            dailyRevenue * 30
        }
        
        return RevenueAnalytics(
            totalRevenue = totalRevenue,
            revenueByVariant = revenueByVariant,
            revenueBySource = revenueBySource,
            customerLifetimeValue = customerLifetimeValue,
            returnOnAdSpend = returnOnAdSpend,
            averageOrderValue = averageOrderValue,
            revenueProjections = revenueProjections
        )
    }
    
    /**
     * Analyzes traffic quality
     */
    private fun analyzeTrafficQuality(sessions: List<CBSplitSessionBridge.SessionData>): TrafficQualityMetrics {
        // Traffic source analysis
        val trafficSources = sessions.groupBy { it.originalSource }.map { (source, sourceSessions) ->
            val conversions = sourceSessions.count { session ->
                session.touchpoints.any { it.action in listOf("purchase", "conversion") }
            }
            val revenue = sourceSessions.sumOf { session ->
                session.touchpoints.filter { it.action == "purchase" }.sumOf { it.value }
            }
            val conversionRate = if (sourceSessions.isNotEmpty()) {
                conversions.toDouble() / sourceSessions.size
            } else 0.0
            
            val quality = when {
                conversionRate >= 0.1 -> "high"
                conversionRate >= 0.05 -> "medium"
                else -> "low"
            }
            
            source to TrafficSourceMetrics(source, sourceSessions.size, conversionRate, revenue, quality)
        }.toMap()
        
        // Simplified other metrics (would need additional data in real implementation)
        val deviceBreakdown = mapOf(
            "desktop" to DeviceMetrics("desktop", sessions.size / 2, 0.08, 1000.0),
            "mobile" to DeviceMetrics("mobile", sessions.size / 2, 0.06, 800.0)
        )
        
        val geoBreakdown = mapOf(
            "US" to GeoMetrics("US", sessions.size, 0.07, 1500.0)
        )
        
        val timeOfDayAnalysis = (0..23).associate { hour ->
            val hourSessions = sessions.size / 24
            hour to TimeSlotMetrics(hour, hourSessions, 0.07, 100.0)
        }
        
        val fraudMetrics = FraudMetrics(
            suspiciousTraffic = 0,
            fraudScore = 5.0,
            patterns = emptyList(),
            blockedSessions = 0
        )
        
        return TrafficQualityMetrics(
            trafficSources = trafficSources,
            deviceBreakdown = deviceBreakdown,
            geoBreakdown = geoBreakdown,
            timeOfDayAnalysis = timeOfDayAnalysis,
            fraudDetection = fraudMetrics
        )
    }
    
    /**
     * Statistical calculation helper functions
     */
    private fun calculateZScore(control: VariantMetrics, variant: VariantMetrics): Double {
        val pooledSE = sqrt(
            (control.conversionRate * (1 - control.conversionRate) / control.sessions) +
            (variant.conversionRate * (1 - variant.conversionRate) / variant.sessions)
        )
        
        return if (pooledSE > 0) {
            (variant.conversionRate - control.conversionRate) / pooledSE
        } else 0.0
    }
    
    private fun calculatePValue(zScore: Double): Double {
        // Simplified p-value calculation (two-tailed test)
        return 2 * (1 - normalCDF(abs(zScore)))
    }
    
    private fun normalCDF(z: Double): Double {
        return 0.5 * (1 + erf(z / sqrt(2.0)))
    }
    
    private fun erf(x: Double): Double {
        // Approximation of error function
        val a1 = 0.254829592
        val a2 = -0.284496736
        val a3 = 1.421413741
        val a4 = -1.453152027
        val a5 = 1.061405429
        val p = 0.3275911
        
        val sign = if (x < 0) -1 else 1
        val absX = abs(x)
        
        val t = 1.0 / (1.0 + p * absX)
        val y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-absX * absX)
        
        return sign * y
    }
    
    private fun calculateConfidenceInterval(rate: Double, standardError: Double): Double {
        return 1.96 * standardError // 95% confidence interval
    }
    
    private fun calculateRequiredSampleSize(baseRate: Double, effect: Double): Int {
        val alpha = 0.05 // 5% significance level
        val beta = 0.2   // 80% power
        val zAlpha = 1.96
        val zBeta = 0.84
        
        val p1 = baseRate
        val p2 = baseRate * (1 + effect)
        val pBar = (p1 + p2) / 2
        
        val numerator = (zAlpha * sqrt(2 * pBar * (1 - pBar)) + zBeta * sqrt(p1 * (1 - p1) + p2 * (1 - p2))).pow(2)
        val denominator = (p1 - p2).pow(2)
        
        return if (denominator > 0) (numerator / denominator).toInt() else 1000
    }
    
    private fun estimateDailyTraffic(totalSessions: Int): Int {
        // Simplified: assume data represents 30 days
        return totalSessions / 30
    }
    
    private fun calculateSegmentVariantMetrics(sessions: List<CBSplitSessionBridge.SessionData>): List<VariantMetrics> {
        return sessions.groupBy { it.variant }.map { (variant, variantSessions) ->
            val conversions = variantSessions.count { session ->
                session.touchpoints.any { it.action in listOf("purchase", "conversion") }
            }
            val revenue = variantSessions.sumOf { session ->
                session.touchpoints.filter { it.action == "purchase" }.sumOf { it.value }
            }
            val conversionRate = if (variantSessions.isNotEmpty()) {
                conversions.toDouble() / variantSessions.size
            } else 0.0
            
            VariantMetrics(
                variant = variant,
                sessions = variantSessions.size,
                conversions = conversions,
                conversionRate = conversionRate,
                revenue = revenue,
                revenuePerVisitor = if (variantSessions.isNotEmpty()) revenue / variantSessions.size else 0.0,
                averageOrderValue = if (conversions > 0) revenue / conversions else 0.0,
                confidence = 0.0,
                standardError = 0.0
            )
        }
    }
    
    private fun calculateSegmentSignificance(variants: List<VariantMetrics>): Double {
        if (variants.size < 2) return 0.0
        val control = variants.first()
        val variant = variants.drop(1).maxByOrNull { it.conversionRate } ?: return 0.0
        
        val zScore = calculateZScore(control, variant)
        return (1 - calculatePValue(zScore)) * 100
    }
}