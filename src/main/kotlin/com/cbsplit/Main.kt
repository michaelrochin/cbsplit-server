package com.cbsplit

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.compression.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import functions.cbsplit.*

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    
    println("Starting CBSplit Server on port $port...")
    
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        configureSerialization()
        configureCORS()
        configureCompression()
        configureRouting()
        
        println("CBSplit Server started successfully!")
        println("API Endpoints available at http://localhost:$port/api/cbsplit/")
        println("Webhook endpoints available at http://localhost:$port/webhook/")
    }.start(wait = true)
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        anyHost() // In production, specify your domains
    }
}

fun Application.configureCompression() {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024)
        }
    }
}

fun Application.configureRouting() {
    routing {
        
        // Health check
        get("/health") {
            call.respond(mapOf("status" to "healthy", "service" to "cbsplit"))
        }
        
        // CBSplit API routes
        route("/api/cbsplit") {
            
            // Start new session
            post("/start-session") {
                try {
                    val request = call.receive<Map<String, Any>>()
                    val sessionId = request["sessionId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing sessionId")
                    val funnelId = request["funnelId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing funnelId")
                    val variant = request["variant"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing variant")
                    val entryUrl = request["entryUrl"] as? String
                    
                    val session = CBSplitFunnelTracking.startFunnelSession(sessionId, funnelId, variant, entryUrl)
                    
                    if (session != null) {
                        call.respond(mapOf("success" to true, "sessionId" to sessionId))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create session"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Track funnel step
            post("/funnel/step") {
                try {
                    val request = call.receive<Map<String, Any>>()
                    val sessionId = request["sessionId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing sessionId")
                    val stepPosition = (request["stepPosition"] as? Number)?.toInt() ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing stepPosition")
                    val timestamp = (request["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val previousAction = request["previousStepExitAction"] as? String
                    
                    val success = CBSplitFunnelTracking.trackStepVisit(sessionId, stepPosition, timestamp, previousAction)
                    
                    call.respond(mapOf("success" to success))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Track interaction
            post("/funnel/interaction") {
                try {
                    val request = call.receive<Map<String, Any>>()
                    val sessionId = request["sessionId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing sessionId")
                    val action = request["action"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing action")
                    val element = request["element"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing element")
                    val value = request["value"] as? String
                    val metadata = request["metadata"] as? Map<String, String> ?: emptyMap()
                    val timestamp = (request["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    
                    val success = CBSplitFunnelTracking.trackStepInteraction(sessionId, action, element, value, metadata, timestamp)
                    
                    call.respond(mapOf("success" to success))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Track conversion
            post("/funnel/conversion") {
                try {
                    val request = call.receive<Map<String, Any>>()
                    val sessionId = request["sessionId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing sessionId")
                    val conversionType = request["conversionType"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing conversionType")
                    val value = (request["value"] as? Number)?.toDouble() ?: 0.0
                    val currency = request["currency"] as? String ?: "USD"
                    val metadata = request["metadata"] as? Map<String, Any> ?: emptyMap()
                    val timestamp = (request["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    
                    val conversionId = CBSplitFunnelTracking.trackFunnelConversion(sessionId, conversionType, value, currency, null, metadata, timestamp)
                    
                    if (conversionId != null) {
                        call.respond(mapOf("success" to true, "conversionId" to conversionId))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to track conversion"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Track touchpoint
            post("/touchpoint") {
                try {
                    val request = call.receive<Map<String, Any>>()
                    val sessionId = request["sessionId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing sessionId")
                    val action = request["action"] as? String ?: "page_view"
                    val page = request["page"] as? String ?: "unknown"
                    val source = request["source"] as? String ?: "direct"
                    val medium = request["medium"] as? String ?: "none"
                    val campaign = request["campaign"] as? String ?: "none"
                    
                    CBSplitSessionBridge.addTouchpoint(sessionId, source, medium, campaign, page, action)
                    
                    call.respond(mapOf("success" to true))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Get analytics
            get("/analytics/{testId}") {
                try {
                    val testId = call.parameters["testId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing testId")
                    
                    val analytics = CBSplitAdvancedAnalytics.generateAdvancedTestMetrics(testId)
                    
                    if (analytics != null) {
                        call.respond(analytics)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Test not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Get funnel analytics
            get("/funnel/{funnelId}/analytics") {
                try {
                    val funnelId = call.parameters["funnelId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing funnelId")
                    val variant = call.request.queryParameters["variant"]
                    
                    val analytics = CBSplitFunnelTracking.getFunnelAnalytics(funnelId, variant)
                    
                    if (analytics != null) {
                        call.respond(analytics)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Funnel not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
        }
        
        // Webhook routes
        route("/webhook") {
            
            // ClickBank webhook
            post("/clickbank") {
                try {
                    val payload = call.receiveText()
                    val signature = call.request.headers["X-ClickBank-Signature"]
                    
                    val success = CBSplitWebhooks.processClickBankWebhook(payload, signature)
                    
                    if (success) {
                        call.respond(HttpStatusCode.OK, "Webhook processed")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Failed to process webhook")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // ClickFunnels webhook
            post("/clickfunnels") {
                try {
                    val payload = call.receiveText()
                    val signature = call.request.headers["X-ClickFunnels-Signature"]
                    
                    val success = CBSplitWebhooks.processClickFunnelsWebhook(payload, signature)
                    
                    if (success) {
                        call.respond(HttpStatusCode.OK, "Webhook processed")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Failed to process webhook")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Generic webhook handler for Kajabi, Teachable, etc.
            post("/{platform}") {
                try {
                    val platform = call.parameters["platform"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing platform")
                    val payload = call.receiveText()
                    val signature = call.request.headers["X-Signature"] ?: call.request.headers["X-${platform.capitalize()}-Signature"]
                    
                    val success = CBSplitWebhooks.processGenericWebhook(platform, payload, signature)
                    
                    if (success) {
                        call.respond(HttpStatusCode.OK, "Webhook processed")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Failed to process webhook")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
        }
        
        // Serve static files (JS tracking scripts)
        get("/cbsplit-tracking.js") {
            val script = generateTrackingScript()
            call.respondText(script, ContentType.Application.JavaScript)
        }
        
        get("/cbsplit-auto-track.js") {
            val script = generateAutoTrackingScript()
            call.respondText(script, ContentType.Application.JavaScript)
        }
    }
}

fun generateTrackingScript(): String {
    return """
        // CBSplit Universal Tracking Script
        (function() {
            window.CBSplit = window.CBSplit || {
                apiUrl: '${System.getenv("CBSPLIT_API_URL") ?: "http://localhost:8080"}/api/cbsplit',
                
                startSession: function(funnelId, variant, entryUrl) {
                    const sessionId = 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
                    
                    fetch(this.apiUrl + '/start-session', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            sessionId: sessionId,
                            funnelId: funnelId,
                            variant: variant,
                            entryUrl: entryUrl || window.location.href
                        })
                    });
                    
                    // Store session info
                    this.sessionId = sessionId;
                    this.funnelId = funnelId;
                    this.variant = variant;
                    
                    return sessionId;
                },
                
                trackStep: function(stepPosition) {
                    if (!this.sessionId) return;
                    
                    fetch(this.apiUrl + '/funnel/step', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            sessionId: this.sessionId,
                            stepPosition: stepPosition,
                            timestamp: Date.now()
                        })
                    });
                },
                
                trackConversion: function(type, value, metadata) {
                    if (!this.sessionId) return;
                    
                    fetch(this.apiUrl + '/funnel/conversion', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            sessionId: this.sessionId,
                            conversionType: type,
                            value: value || 0,
                            metadata: metadata || {},
                            timestamp: Date.now()
                        })
                    });
                },
                
                trackInteraction: function(action, element, value, metadata) {
                    if (!this.sessionId) return;
                    
                    fetch(this.apiUrl + '/funnel/interaction', {
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
                }
            };
        })();
    """.trimIndent()
}

fun generateAutoTrackingScript(): String {
    return """
        // CBSplit Auto-Tracking Script
        (function() {
            // Auto-track form submissions
            document.addEventListener('submit', function(e) {
                if (window.CBSplit && window.CBSplit.sessionId) {
                    const form = e.target;
                    const formType = form.querySelector('input[type="email"]') ? 'lead' : 'form';
                    window.CBSplit.trackInteraction('form_submit', formType);
                }
            });
            
            // Auto-track button clicks
            document.addEventListener('click', function(e) {
                if (window.CBSplit && window.CBSplit.sessionId) {
                    if (e.target.tagName === 'BUTTON' || e.target.type === 'submit') {
                        window.CBSplit.trackInteraction('button_click', e.target.textContent || 'button');
                    }
                }
            });
            
            // Auto-inject tracking params into links
            function injectTrackingParams() {
                if (!window.CBSplit || !window.CBSplit.sessionId) return;
                
                document.querySelectorAll('a[href]').forEach(function(link) {
                    const href = link.getAttribute('href');
                    if (href && !href.startsWith('#') && !href.includes('cb_session=')) {
                        const separator = href.includes('?') ? '&' : '?';
                        const params = 'cb_session=' + window.CBSplit.sessionId + '&cb_variant=' + (window.CBSplit.variant || 'A');
                        link.setAttribute('href', href + separator + params);
                    }
                });
            }
            
            // Run injection on page load and periodically
            document.addEventListener('DOMContentLoaded', injectTrackingParams);
            setInterval(injectTrackingParams, 2000);
        })();
    """.trimIndent()
}