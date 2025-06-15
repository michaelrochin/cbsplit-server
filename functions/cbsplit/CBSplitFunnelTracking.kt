package functions.cbsplit

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class FunnelStep(
    val stepId: String,
    val stepName: String,
    val stepType: String, // "landing", "optin", "sales", "checkout", "upsell", "thankyou"
    val url: String,
    val variant: String,
    val testId: String,
    val position: Int, // Step order in funnel (1, 2, 3, etc.)
    val isRequired: Boolean = true,
    val conversionGoal: String? = null
)

@Serializable
data class FunnelFlow(
    val funnelId: String,
    val funnelName: String,
    val testId: String,
    val steps: List<FunnelStep>,
    val variants: Map<String, List<FunnelStep>>, // Different step sequences per variant
    val conversionGoals: List<String> = listOf("lead", "sale", "upsell")
)

@Serializable
data class UserFunnelSession(
    val sessionId: String,
    val funnelId: String,
    val variant: String,
    val currentStep: Int,
    val stepsCompleted: MutableList<FunnelStepVisit> = mutableListOf(),
    val conversions: MutableList<FunnelConversion> = mutableListOf(),
    val startTimestamp: Long = System.currentTimeMillis(),
    val lastActivityTimestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val exitStep: Int? = null,
    val exitReason: String? = null
)

@Serializable
data class FunnelStepVisit(
    val stepId: String,
    val stepPosition: Int,
    val variant: String,
    val timestamp: Long,
    val timeOnStep: Long = 0L,
    val interactions: MutableList<StepInteraction> = mutableListOf(),
    val exitTimestamp: Long? = null,
    val exitAction: String? = null // "next", "back", "exit", "conversion"
)

@Serializable
data class StepInteraction(
    val action: String, // "click", "scroll", "form_start", "form_submit", "video_play", "video_complete"
    val element: String,
    val timestamp: Long,
    val value: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class FunnelConversion(
    val conversionId: String,
    val type: String, // "lead", "sale", "upsell", "custom"
    val value: Double,
    val currency: String = "USD",
    val stepPosition: Int,
    val timestamp: Long,
    val metadata: Map<String, Any> = emptyMap()
)

@Serializable
data class FunnelAnalytics(
    val funnelId: String,
    val testId: String,
    val variant: String,
    val totalSessions: Int,
    val completionRate: Double,
    val dropoffAnalysis: Map<Int, Double>, // Step position -> drop-off rate
    val conversionRates: Map<String, Double>, // Conversion type -> rate
    val averageTimePerStep: Map<Int, Long>,
    val revenuePerVisitor: Double,
    val topExitPoints: List<ExitPoint>
)

@Serializable
data class ExitPoint(
    val stepPosition: Int,
    val exitCount: Int,
    val exitRate: Double,
    val topExitReasons: List<String>
)

object CBSplitFunnelTracking {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val funnelFlows = ConcurrentHashMap<String, FunnelFlow>()
    private val activeSessions = ConcurrentHashMap<String, UserFunnelSession>()
    private val completedSessions = mutableListOf<UserFunnelSession>()
    
    /**
     * Creates a new funnel flow with steps and variants
     */
    fun createFunnelFlow(
        funnelId: String,
        funnelName: String,
        testId: String,
        baseSteps: List<FunnelStep>,
        variantSteps: Map<String, List<FunnelStep>> = emptyMap()
    ): FunnelFlow {
        val funnelFlow = FunnelFlow(
            funnelId = funnelId,
            funnelName = funnelName,
            testId = testId,
            steps = baseSteps,
            variants = variantSteps,
            conversionGoals = listOf("lead", "sale", "upsell")
        )
        
        funnelFlows[funnelId] = funnelFlow
        return funnelFlow
    }
    
    /**
     * Starts a new funnel session for a user
     */
    fun startFunnelSession(
        sessionId: String,
        funnelId: String,
        variant: String,
        entryUrl: String? = null
    ): UserFunnelSession? {
        val funnelFlow = funnelFlows[funnelId] ?: return null
        
        // Determine starting step based on entry URL or default to step 1
        val startingStep = if (entryUrl != null) {
            findStepByUrl(funnelFlow, variant, entryUrl)?.position ?: 1
        } else {
            1
        }
        
        val funnelSession = UserFunnelSession(
            sessionId = sessionId,
            funnelId = funnelId,
            variant = variant,
            currentStep = startingStep
        )
        
        activeSessions[sessionId] = funnelSession
        
        // Track initial step visit
        trackStepVisit(sessionId, startingStep, System.currentTimeMillis())
        
        return funnelSession
    }
    
    /**
     * Tracks a user visit to a funnel step
     */
    fun trackStepVisit(
        sessionId: String,
        stepPosition: Int,
        timestamp: Long = System.currentTimeMillis(),
        previousStepExitAction: String? = null
    ): Boolean {
        val session = activeSessions[sessionId] ?: return false
        val funnelFlow = funnelFlows[session.funnelId] ?: return false
        
        // Close previous step if exists
        if (session.stepsCompleted.isNotEmpty()) {
            val previousStep = session.stepsCompleted.last()
            if (previousStep.exitTimestamp == null) {
                previousStep.exitTimestamp = timestamp
                previousStep.exitAction = previousStepExitAction ?: "next"
                val timeOnStep = timestamp - previousStep.timestamp
                session.stepsCompleted[session.stepsCompleted.size - 1] = 
                    previousStep.copy(timeOnStep = timeOnStep)
            }
        }
        
        // Find step info
        val stepInfo = findStepByPosition(funnelFlow, session.variant, stepPosition)
        
        val stepVisit = FunnelStepVisit(
            stepId = stepInfo?.stepId ?: "step_$stepPosition",
            stepPosition = stepPosition,
            variant = session.variant,
            timestamp = timestamp
        )
        
        session.stepsCompleted.add(stepVisit)
        session.currentStep = stepPosition
        session.lastActivityTimestamp = timestamp
        
        // Update session bridge with touchpoint
        CBSplitSessionBridge.addTouchpoint(
            sessionId = sessionId,
            source = "funnel",
            medium = "step_$stepPosition",
            campaign = session.funnelId,
            page = stepInfo?.stepName ?: "step_$stepPosition",
            action = "page_view"
        )
        
        return true
    }
    
    /**
     * Tracks user interaction within a funnel step
     */
    fun trackStepInteraction(
        sessionId: String,
        action: String,
        element: String,
        value: String? = null,
        metadata: Map<String, String> = emptyMap(),
        timestamp: Long = System.currentTimeMillis()
    ): Boolean {
        val session = activeSessions[sessionId] ?: return false
        
        if (session.stepsCompleted.isEmpty()) return false
        
        val currentStepIndex = session.stepsCompleted.size - 1
        val currentStep = session.stepsCompleted[currentStepIndex]
        
        val interaction = StepInteraction(
            action = action,
            element = element,
            timestamp = timestamp,
            value = value,
            metadata = metadata
        )
        
        currentStep.interactions.add(interaction)
        session.lastActivityTimestamp = timestamp
        
        // Track high-value interactions
        if (action in listOf("form_submit", "video_complete", "button_click")) {
            CBSplitSessionBridge.addTouchpoint(
                sessionId = sessionId,
                source = "funnel",
                medium = "interaction",
                campaign = session.funnelId,
                page = "step_${session.currentStep}",
                action = action
            )
        }
        
        return true
    }
    
    /**
     * Records a conversion at a funnel step
     */
    fun trackFunnelConversion(
        sessionId: String,
        conversionType: String,
        value: Double,
        currency: String = "USD",
        stepPosition: Int? = null,
        metadata: Map<String, Any> = emptyMap(),
        timestamp: Long = System.currentTimeMillis()
    ): String? {
        val session = activeSessions[sessionId] ?: return null
        val currentStep = stepPosition ?: session.currentStep
        
        val conversionId = generateConversionId(sessionId, conversionType, timestamp)
        
        val conversion = FunnelConversion(
            conversionId = conversionId,
            type = conversionType,
            value = value,
            currency = currency,
            stepPosition = currentStep,
            timestamp = timestamp,
            metadata = metadata
        )
        
        session.conversions.add(conversion)
        session.lastActivityTimestamp = timestamp
        
        // Track conversion in main analytics
        val analytics = CBSplitAnalytics.getInstance(null)
        analytics.trackConversion(
            testId = session.funnelId,
            variant = session.variant,
            eventType = conversionType,
            value = value,
            userId = sessionId
        )
        
        // Track revenue attribution
        if (value > 0) {
            CBSplitRevenueAttribution.attributeRevenue(
                sessionId = sessionId,
                orderId = conversionId,
                revenue = value,
                currency = currency,
                customerId = sessionId
            )
        }
        
        return conversionId
    }
    
    /**
     * Ends a funnel session
     */
    fun endFunnelSession(
        sessionId: String,
        exitStep: Int? = null,
        exitReason: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): UserFunnelSession? {
        val session = activeSessions.remove(sessionId) ?: return null
        
        // Close current step
        if (session.stepsCompleted.isNotEmpty()) {
            val lastStep = session.stepsCompleted.last()
            if (lastStep.exitTimestamp == null) {
                lastStep.exitTimestamp = timestamp
                lastStep.exitAction = exitReason ?: "exit"
                val timeOnStep = timestamp - lastStep.timestamp
                session.stepsCompleted[session.stepsCompleted.size - 1] = 
                    lastStep.copy(timeOnStep = timeOnStep)
            }
        }
        
        session.isActive = false
        session.exitStep = exitStep ?: session.currentStep
        session.exitReason = exitReason
        session.lastActivityTimestamp = timestamp
        
        completedSessions.add(session)
        
        return session
    }
    
    /**
     * Gets funnel analytics for a specific test and variant
     */
    fun getFunnelAnalytics(
        funnelId: String,
        variant: String? = null,
        timeRange: Pair<Long, Long>? = null
    ): FunnelAnalytics? {
        val funnelFlow = funnelFlows[funnelId] ?: return null
        
        val sessions = (activeSessions.values + completedSessions).filter { session ->
            session.funnelId == funnelId &&
            (variant == null || session.variant == variant) &&
            (timeRange == null || session.startTimestamp in timeRange.first..timeRange.second)
        }
        
        if (sessions.isEmpty()) return null
        
        val totalSessions = sessions.size
        val completedSessions = sessions.filter { !it.isActive }
        val completionRate = completedSessions.size.toDouble() / totalSessions
        
        // Calculate drop-off analysis
        val maxSteps = funnelFlow.steps.size
        val dropoffAnalysis = (1..maxSteps).associate { step ->
            val visitedStep = sessions.count { session ->
                session.stepsCompleted.any { it.stepPosition >= step }
            }
            val dropoffRate = 1.0 - (visitedStep.toDouble() / totalSessions)
            step to dropoffRate
        }
        
        // Calculate conversion rates
        val conversionRates = sessions.flatMap { it.conversions }
            .groupBy { it.type }
            .mapValues { (_, conversions) ->
                conversions.size.toDouble() / totalSessions
            }
        
        // Calculate average time per step
        val averageTimePerStep = (1..maxSteps).associate { step ->
            val stepVisits = sessions.flatMap { session ->
                session.stepsCompleted.filter { it.stepPosition == step && it.timeOnStep > 0 }
            }
            val avgTime = if (stepVisits.isNotEmpty()) {
                stepVisits.map { it.timeOnStep }.average().toLong()
            } else 0L
            step to avgTime
        }
        
        // Calculate revenue per visitor
        val totalRevenue = sessions.sumOf { session ->
            session.conversions.sumOf { it.value }
        }
        val revenuePerVisitor = totalRevenue / totalSessions
        
        // Find top exit points
        val exitPoints = completedSessions.groupBy { it.exitStep ?: it.currentStep }
            .map { (step, sessions) ->
                val exitReasons = sessions.mapNotNull { it.exitReason }.groupBy { it }
                    .map { (reason, list) -> reason to list.size }
                    .sortedByDescending { it.second }
                    .take(3)
                    .map { it.first }
                
                ExitPoint(
                    stepPosition = step,
                    exitCount = sessions.size,
                    exitRate = sessions.size.toDouble() / totalSessions,
                    topExitReasons = exitReasons
                )
            }
            .sortedByDescending { it.exitCount }
            .take(5)
        
        return FunnelAnalytics(
            funnelId = funnelId,
            testId = funnelFlow.testId,
            variant = variant ?: "all",
            totalSessions = totalSessions,
            completionRate = completionRate,
            dropoffAnalysis = dropoffAnalysis,
            conversionRates = conversionRates,
            averageTimePerStep = averageTimePerStep,
            revenuePerVisitor = revenuePerVisitor,
            topExitPoints = exitPoints
        )
    }
    
    /**
     * Gets variant comparison analytics
     */
    fun getVariantComparison(funnelId: String): Map<String, FunnelAnalytics> {
        val funnelFlow = funnelFlows[funnelId] ?: return emptyMap()
        
        val variants = (activeSessions.values + completedSessions)
            .filter { it.funnelId == funnelId }
            .map { it.variant }
            .distinct()
        
        return variants.associateWith { variant ->
            getFunnelAnalytics(funnelId, variant) ?: FunnelAnalytics(
                funnelId = funnelId,
                testId = funnelFlow.testId,
                variant = variant,
                totalSessions = 0,
                completionRate = 0.0,
                dropoffAnalysis = emptyMap(),
                conversionRates = emptyMap(),
                averageTimePerStep = emptyMap(),
                revenuePerVisitor = 0.0,
                topExitPoints = emptyList()
            )
        }
    }
    
    /**
     * Generates JavaScript tracking code for funnel steps
     */
    fun generateFunnelTrackingScript(
        sessionId: String,
        funnelId: String,
        stepPosition: Int
    ): String {
        return """
            <script>
            window.CBSplitFunnel = window.CBSplitFunnel || {
                sessionId: '$sessionId',
                funnelId: '$funnelId',
                currentStep: $stepPosition,
                
                // Track step visit
                trackStepVisit: function(step, previousAction) {
                    fetch('/api/cbsplit/funnel/step', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            sessionId: this.sessionId,
                            stepPosition: step || this.currentStep,
                            previousStepExitAction: previousAction,
                            timestamp: Date.now()
                        })
                    });
                },
                
                // Track interaction
                trackInteraction: function(action, element, value, metadata) {
                    fetch('/api/cbsplit/funnel/interaction', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            sessionId: this.sessionId,
                            action: action,
                            element: element,
                            value: value,
                            metadata: metadata || {},
                            timestamp: Date.now()
                        })
                    });
                },
                
                // Track conversion
                trackConversion: function(type, value, currency, metadata) {
                    fetch('/api/cbsplit/funnel/conversion', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            sessionId: this.sessionId,
                            conversionType: type,
                            value: value || 0,
                            currency: currency || 'USD',
                            metadata: metadata || {},
                            timestamp: Date.now()
                        })
                    });
                },
                
                // Auto-track common interactions
                autoTrack: function() {
                    // Track form submissions
                    document.addEventListener('submit', function(e) {
                        CBSplitFunnel.trackInteraction('form_submit', e.target.tagName, null, {
                            formId: e.target.id || 'unknown',
                            formClass: e.target.className || 'unknown'
                        });
                    });
                    
                    // Track button clicks
                    document.addEventListener('click', function(e) {
                        if (e.target.tagName === 'BUTTON' || e.target.type === 'submit') {
                            CBSplitFunnel.trackInteraction('button_click', e.target.textContent || 'button', null, {
                                buttonId: e.target.id || 'unknown',
                                buttonClass: e.target.className || 'unknown'
                            });
                        }
                    });
                    
                    // Track video events
                    document.querySelectorAll('video').forEach(function(video) {
                        video.addEventListener('play', function() {
                            CBSplitFunnel.trackInteraction('video_play', 'video', null, {
                                videoSrc: video.src || 'unknown'
                            });
                        });
                        
                        video.addEventListener('ended', function() {
                            CBSplitFunnel.trackInteraction('video_complete', 'video', null, {
                                videoSrc: video.src || 'unknown'
                            });
                        });
                    });
                    
                    // Track scroll depth
                    var maxScroll = 0;
                    window.addEventListener('scroll', function() {
                        var scrollPercent = Math.round((window.scrollY / (document.body.scrollHeight - window.innerHeight)) * 100);
                        if (scrollPercent > maxScroll && scrollPercent % 25 === 0) {
                            maxScroll = scrollPercent;
                            CBSplitFunnel.trackInteraction('scroll', 'page', scrollPercent.toString(), {
                                scrollDepth: scrollPercent + '%'
                            });
                        }
                    });
                }
            };
            
            // Auto-initialize tracking
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', function() {
                    CBSplitFunnel.trackStepVisit();
                    CBSplitFunnel.autoTrack();
                });
            } else {
                CBSplitFunnel.trackStepVisit();
                CBSplitFunnel.autoTrack();
            }
            </script>
        """.trimIndent()
    }
    
    /**
     * Helper functions
     */
    private fun findStepByPosition(flow: FunnelFlow, variant: String, position: Int): FunnelStep? {
        val steps = flow.variants[variant] ?: flow.steps
        return steps.find { it.position == position }
    }
    
    private fun findStepByUrl(flow: FunnelFlow, variant: String, url: String): FunnelStep? {
        val steps = flow.variants[variant] ?: flow.steps
        return steps.find { step ->
            url.contains(step.url) || step.url.contains(url)
        }
    }
    
    private fun generateConversionId(sessionId: String, type: String, timestamp: Long): String {
        return "${type}_${sessionId}_${timestamp}"
    }
    
    /**
     * Cleanup old sessions
     */
    fun cleanupOldSessions(daysToKeep: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        
        // Remove old completed sessions
        completedSessions.removeAll { it.lastActivityTimestamp < cutoffTime }
        
        // End inactive sessions
        val inactiveSessions = activeSessions.filter { (_, session) ->
            session.lastActivityTimestamp < cutoffTime
        }
        
        inactiveSessions.forEach { (sessionId, _) ->
            endFunnelSession(sessionId, exitReason = "timeout")
        }
    }
}