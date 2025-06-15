package functions.cbsplit

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.*
import java.security.MessageDigest

@Serializable
data class SessionData(
    val sessionId: String,
    val variant: String,
    val testId: String,
    val originalSource: String,
    val timestamp: Long,
    val touchpoints: MutableList<TouchpointData> = mutableListOf(),
    val attributionWindow: Long = 30 * 24 * 60 * 60 * 1000L // 30 days
)

@Serializable
data class TouchpointData(
    val timestamp: Long,
    val source: String,
    val medium: String,
    val campaign: String,
    val page: String,
    val action: String
)

object CBSplitSessionBridge {
    private val activeSessions = mutableMapOf<String, SessionData>()
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Creates a new session with variant assignment
     */
    fun createSession(
        testId: String,
        variant: String,
        originalSource: String,
        utmParams: Map<String, String> = emptyMap()
    ): String {
        val sessionId = generateSessionId(testId, variant)
        val sessionData = SessionData(
            sessionId = sessionId,
            variant = variant,
            testId = testId,
            originalSource = originalSource,
            timestamp = System.currentTimeMillis()
        )
        
        // Add initial touchpoint
        sessionData.touchpoints.add(
            TouchpointData(
                timestamp = System.currentTimeMillis(),
                source = utmParams["utm_source"] ?: "direct",
                medium = utmParams["utm_medium"] ?: "none",
                campaign = utmParams["utm_campaign"] ?: "none",
                page = "landing_page",
                action = "page_view"
            )
        )
        
        activeSessions[sessionId] = sessionData
        return sessionId
    }
    
    /**
     * Bridges session data across domains using URL parameters
     */
    fun bridgeSession(baseUrl: String, sessionId: String): String {
        val sessionData = activeSessions[sessionId] ?: return baseUrl
        
        val params = mutableListOf<String>()
        params.add("cb_session=$sessionId")
        params.add("cb_variant=${sessionData.variant}")
        params.add("cb_test=${sessionData.testId}")
        params.add("cb_timestamp=${System.currentTimeMillis()}")
        
        val separator = if (baseUrl.contains("?")) "&" else "?"
        return "$baseUrl$separator${params.joinToString("&")}"
    }
    
    /**
     * Restores session from URL parameters (for receiving domain)
     */
    fun restoreSession(urlParams: Map<String, String>): SessionData? {
        val sessionId = urlParams["cb_session"] ?: return null
        val variant = urlParams["cb_variant"] ?: return null
        val testId = urlParams["cb_test"] ?: return null
        
        // Check if session exists in memory
        activeSessions[sessionId]?.let { return it }
        
        // Reconstruct session from URL params
        val sessionData = SessionData(
            sessionId = sessionId,
            variant = variant,
            testId = testId,
            originalSource = "restored",
            timestamp = urlParams["cb_timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
        )
        
        activeSessions[sessionId] = sessionData
        return sessionData
    }
    
    /**
     * Adds a touchpoint to existing session
     */
    fun addTouchpoint(
        sessionId: String,
        source: String,
        medium: String,
        campaign: String,
        page: String,
        action: String
    ) {
        activeSessions[sessionId]?.let { session ->
            session.touchpoints.add(
                TouchpointData(
                    timestamp = System.currentTimeMillis(),
                    source = source,
                    medium = medium,
                    campaign = campaign,
                    page = page,
                    action = action
                )
            )
        }
    }
    
    /**
     * Extends session attribution window
     */
    fun extendSession(sessionId: String, extendDays: Int) {
        activeSessions[sessionId]?.let { session ->
            val extensionMs = extendDays * 24 * 60 * 60 * 1000L
            // Update attribution window
            activeSessions[sessionId] = session.copy(
                attributionWindow = session.attributionWindow + extensionMs
            )
        }
    }
    
    /**
     * Checks if session is still valid (within attribution window)
     */
    fun isSessionValid(sessionId: String): Boolean {
        val session = activeSessions[sessionId] ?: return false
        val elapsed = System.currentTimeMillis() - session.timestamp
        return elapsed <= session.attributionWindow
    }
    
    /**
     * Gets session data for attribution
     */
    fun getSession(sessionId: String): SessionData? {
        return if (isSessionValid(sessionId)) {
            activeSessions[sessionId]
        } else {
            // Clean up expired session
            activeSessions.remove(sessionId)
            null
        }
    }
    
    /**
     * Serializes session data for storage/transmission
     */
    fun serializeSession(sessionData: SessionData): String {
        return json.encodeToString(sessionData)
    }
    
    /**
     * Deserializes session data from storage
     */
    fun deserializeSession(jsonString: String): SessionData? {
        return try {
            json.decodeFromString<SessionData>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generates unique session ID using hash of test + variant + timestamp
     */
    private fun generateSessionId(testId: String, variant: String): String {
        val input = "$testId-$variant-${System.currentTimeMillis()}-${UUID.randomUUID()}"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }
    
    /**
     * JavaScript bridge code generator for client-side integration
     */
    fun generateJavaScriptBridge(sessionId: String): String {
        return """
            <script>
            window.CBSplitSession = {
                sessionId: '$sessionId',
                
                // Inject tracking parameters into all links
                injectTrackingParams: function() {
                    document.addEventListener('DOMContentLoaded', function() {
                        var links = document.querySelectorAll('a[href]');
                        links.forEach(function(link) {
                            var href = link.getAttribute('href');
                            if (href && !href.startsWith('#') && !href.startsWith('mailto:')) {
                                var separator = href.includes('?') ? '&' : '?';
                                link.setAttribute('href', href + separator + 'cb_session=$sessionId');
                            }
                        });
                    });
                },
                
                // Track page view
                trackPageView: function(page) {
                    fetch('/api/cbsplit/touchpoint', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            sessionId: this.sessionId,
                            action: 'page_view',
                            page: page,
                            timestamp: Date.now()
                        })
                    });
                },
                
                // Track conversion
                trackConversion: function(type, value) {
                    fetch('/api/cbsplit/conversion', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            sessionId: this.sessionId,
                            type: type,
                            value: value,
                            timestamp: Date.now()
                        })
                    });
                }
            };
            
            // Auto-inject tracking params
            CBSplitSession.injectTrackingParams();
            </script>
        """.trimIndent()
    }
    
    /**
     * Cleanup expired sessions (call periodically)
     */
    fun cleanupExpiredSessions() {
        val now = System.currentTimeMillis()
        val expiredSessions = activeSessions.filter { (_, session) ->
            (now - session.timestamp) > session.attributionWindow
        }
        
        expiredSessions.forEach { (sessionId, _) ->
            activeSessions.remove(sessionId)
        }
    }
    
    /**
     * Get all sessions for a specific test
     */
    fun getSessionsForTest(testId: String): List<SessionData> {
        return activeSessions.values.filter { it.testId == testId && isSessionValid(it.sessionId) }
    }
    
    /**
     * Get session analytics
     */
    fun getSessionAnalytics(testId: String): Map<String, Any> {
        val sessions = getSessionsForTest(testId)
        val variantBreakdown = sessions.groupBy { it.variant }
        
        return mapOf(
            "totalSessions" to sessions.size,
            "variantBreakdown" to variantBreakdown.mapValues { (_, sessions) ->
                mapOf(
                    "count" to sessions.size,
                    "avgTouchpoints" to sessions.map { it.touchpoints.size }.average(),
                    "sources" to sessions.map { it.originalSource }.distinct()
                )
            },
            "topSources" to sessions.groupBy { it.originalSource }.mapValues { it.value.size },
            "touchpointAnalysis" to sessions.flatMap { it.touchpoints }
                .groupBy { it.action }
                .mapValues { it.value.size }
        )
    }
}