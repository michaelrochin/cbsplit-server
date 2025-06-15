package functions.cbsplit

import java.net.URL
import java.net.URLEncoder
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class TrackingParams(
    val sessionId: String,
    val variant: String,
    val testId: String,
    val timestamp: Long,
    val source: String? = null,
    val medium: String? = null,
    val campaign: String? = null,
    val content: String? = null,
    val term: String? = null
)

object CBSplitUrlInjector {
    
    /**
     * Injects CBSplit tracking parameters into any URL
     */
    fun injectTrackingParams(
        originalUrl: String,
        trackingParams: TrackingParams,
        preserveExisting: Boolean = true
    ): String {
        return try {
            val url = URL(originalUrl)
            val existingParams = parseQueryParams(url.query)
            
            val cbParams = mutableMapOf<String, String>()
            cbParams["cb_session"] = trackingParams.sessionId
            cbParams["cb_variant"] = trackingParams.variant
            cbParams["cb_test"] = trackingParams.testId
            cbParams["cb_ts"] = trackingParams.timestamp.toString()
            
            // Add UTM parameters if provided
            trackingParams.source?.let { cbParams["cb_source"] = it }
            trackingParams.medium?.let { cbParams["cb_medium"] = it }
            trackingParams.campaign?.let { cbParams["cb_campaign"] = it }
            trackingParams.content?.let { cbParams["cb_content"] = it }
            trackingParams.term?.let { cbParams["cb_term"] = it }
            
            val finalParams = if (preserveExisting) {
                existingParams + cbParams
            } else {
                cbParams
            }
            
            buildUrl(url, finalParams)
        } catch (e: Exception) {
            // If URL parsing fails, append params to original URL
            val separator = if (originalUrl.contains("?")) "&" else "?"
            val params = buildParamString(trackingParams)
            "$originalUrl$separator$params"
        }
    }
    
    /**
     * Extracts CBSplit tracking parameters from URL
     */
    fun extractTrackingParams(url: String): TrackingParams? {
        return try {
            val urlObj = URL(url)
            val params = parseQueryParams(urlObj.query)
            
            val sessionId = params["cb_session"] ?: return null
            val variant = params["cb_variant"] ?: return null
            val testId = params["cb_test"] ?: return null
            val timestamp = params["cb_ts"]?.toLongOrNull() ?: System.currentTimeMillis()
            
            TrackingParams(
                sessionId = sessionId,
                variant = variant,
                testId = testId,
                timestamp = timestamp,
                source = params["cb_source"],
                medium = params["cb_medium"],
                campaign = params["cb_campaign"],
                content = params["cb_content"],
                term = params["cb_term"]
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generates JavaScript code for automatic URL injection
     */
    fun generateAutoInjectionScript(trackingParams: TrackingParams): String {
        val paramsJson = Json.encodeToString(trackingParams)
        
        return """
            <script>
            (function() {
                var cbParams = $paramsJson;
                
                // Function to inject tracking params into URL
                function injectCBParams(url) {
                    if (!url || url.startsWith('#') || url.startsWith('mailto:') || url.startsWith('tel:')) {
                        return url;
                    }
                    
                    var separator = url.includes('?') ? '&' : '?';
                    var params = [];
                    
                    params.push('cb_session=' + encodeURIComponent(cbParams.sessionId));
                    params.push('cb_variant=' + encodeURIComponent(cbParams.variant));
                    params.push('cb_test=' + encodeURIComponent(cbParams.testId));
                    params.push('cb_ts=' + Date.now());
                    
                    if (cbParams.source) params.push('cb_source=' + encodeURIComponent(cbParams.source));
                    if (cbParams.medium) params.push('cb_medium=' + encodeURIComponent(cbParams.medium));
                    if (cbParams.campaign) params.push('cb_campaign=' + encodeURIComponent(cbParams.campaign));
                    if (cbParams.content) params.push('cb_content=' + encodeURIComponent(cbParams.content));
                    if (cbParams.term) params.push('cb_term=' + encodeURIComponent(cbParams.term));
                    
                    return url + separator + params.join('&');
                }
                
                // Inject params into all existing links
                function injectIntoExistingLinks() {
                    var links = document.querySelectorAll('a[href]');
                    for (var i = 0; i < links.length; i++) {
                        var link = links[i];
                        var href = link.getAttribute('href');
                        if (href && !href.includes('cb_session=')) {
                            link.setAttribute('href', injectCBParams(href));
                            link.setAttribute('data-cb-injected', 'true');
                        }
                    }
                }
                
                // Inject params into form actions
                function injectIntoForms() {
                    var forms = document.querySelectorAll('form[action]');
                    for (var i = 0; i < forms.length; i++) {
                        var form = forms[i];
                        var action = form.getAttribute('action');
                        if (action && !action.includes('cb_session=')) {
                            form.setAttribute('action', injectCBParams(action));
                            
                            // Also add as hidden fields
                            var hiddenFields = [
                                { name: 'cb_session', value: cbParams.sessionId },
                                { name: 'cb_variant', value: cbParams.variant },
                                { name: 'cb_test', value: cbParams.testId },
                                { name: 'cb_ts', value: Date.now().toString() }
                            ];
                            
                            hiddenFields.forEach(function(field) {
                                if (!form.querySelector('input[name="' + field.name + '"]')) {
                                    var input = document.createElement('input');
                                    input.type = 'hidden';
                                    input.name = field.name;
                                    input.value = field.value;
                                    form.appendChild(input);
                                }
                            });
                        }
                    }
                }
                
                // Monitor for dynamically added links
                function observeNewLinks() {
                    if (typeof MutationObserver !== 'undefined') {
                        var observer = new MutationObserver(function(mutations) {
                            mutations.forEach(function(mutation) {
                                if (mutation.type === 'childList') {
                                    mutation.addedNodes.forEach(function(node) {
                                        if (node.nodeType === 1) { // Element node
                                            // Check if the node itself is a link
                                            if (node.tagName === 'A' && node.href && !node.getAttribute('data-cb-injected')) {
                                                node.href = injectCBParams(node.href);
                                                node.setAttribute('data-cb-injected', 'true');
                                            }
                                            
                                            // Check for links within the node
                                            var links = node.querySelectorAll ? node.querySelectorAll('a[href]') : [];
                                            for (var i = 0; i < links.length; i++) {
                                                var link = links[i];
                                                if (!link.getAttribute('data-cb-injected')) {
                                                    link.href = injectCBParams(link.href);
                                                    link.setAttribute('data-cb-injected', 'true');
                                                }
                                            }
                                        }
                                    });
                                }
                            });
                        });
                        
                        observer.observe(document.body, {
                            childList: true,
                            subtree: true
                        });
                    }
                }
                
                // Override window.open to inject params
                var originalOpen = window.open;
                window.open = function(url, name, specs) {
                    if (url) {
                        url = injectCBParams(url);
                    }
                    return originalOpen.call(this, url, name, specs);
                };
                
                // Override location.href setter
                var originalSetHref = Object.getOwnPropertyDescriptor(Location.prototype, 'href').set;
                Object.defineProperty(Location.prototype, 'href', {
                    set: function(url) {
                        return originalSetHref.call(this, injectCBParams(url));
                    },
                    get: Object.getOwnPropertyDescriptor(Location.prototype, 'href').get
                });
                
                // Initialize when DOM is ready
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', function() {
                        injectIntoExistingLinks();
                        injectIntoForms();
                        observeNewLinks();
                    });
                } else {
                    injectIntoExistingLinks();
                    injectIntoForms();
                    observeNewLinks();
                }
                
                // Expose utility functions globally
                window.CBSplitUrlInjector = {
                    injectParams: injectCBParams,
                    getParams: function() { return cbParams; },
                    reinject: function() {
                        injectIntoExistingLinks();
                        injectIntoForms();
                    }
                };
            })();
            </script>
        """.trimIndent()
    }
    
    /**
     * Generates ClickFunnels-specific integration code
     */
    fun generateClickFunnelsIntegration(trackingParams: TrackingParams): String {
        return """
            <script>
            // ClickFunnels specific integration
            (function() {
                var cbParams = ${Json.encodeToString(trackingParams)};
                
                // Override ClickFunnels form submissions
                document.addEventListener('submit', function(e) {
                    var form = e.target;
                    if (form.tagName === 'FORM') {
                        // Add CB params as hidden fields
                        Object.keys(cbParams).forEach(function(key) {
                            var fieldName = 'cb_' + key.replace(/([A-Z])/g, '_$1').toLowerCase();
                            var existingField = form.querySelector('input[name="' + fieldName + '"]');
                            if (!existingField && cbParams[key]) {
                                var input = document.createElement('input');
                                input.type = 'hidden';
                                input.name = fieldName;
                                input.value = cbParams[key];
                                form.appendChild(input);
                            }
                        });
                    }
                });
                
                // Track ClickFunnels page progression
                if (typeof CF !== 'undefined') {
                    var originalPageChange = CF.pageChange || function() {};
                    CF.pageChange = function(data) {
                        // Send page change event to CBSplit
                        fetch('/api/cbsplit/touchpoint', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({
                                sessionId: cbParams.sessionId,
                                action: 'page_change',
                                page: data.page || 'unknown',
                                funnel: data.funnel || 'unknown',
                                timestamp: Date.now()
                            })
                        }).catch(function() {}); // Silent fail
                        
                        return originalPageChange.call(this, data);
                    };
                }
            })();
            </script>
        """.trimIndent()
    }
    
    /**
     * Parses query string into key-value pairs
     */
    private fun parseQueryParams(query: String?): Map<String, String> {
        if (query.isNullOrBlank()) return emptyMap()
        
        return query.split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    parts[0] to java.net.URLDecoder.decode(parts[1], "UTF-8")
                } else null
            }
            .toMap()
    }
    
    /**
     * Builds URL with parameters
     */
    private fun buildUrl(url: URL, params: Map<String, String>): String {
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }
        
        return if (queryString.isNotEmpty()) {
            "${url.protocol}://${url.host}${if (url.port != -1) ":${url.port}" else ""}${url.path}?$queryString"
        } else {
            "${url.protocol}://${url.host}${if (url.port != -1) ":${url.port}" else ""}${url.path}"
        }
    }
    
    /**
     * Builds parameter string from tracking params
     */
    private fun buildParamString(trackingParams: TrackingParams): String {
        val params = mutableListOf<String>()
        
        params.add("cb_session=${URLEncoder.encode(trackingParams.sessionId, "UTF-8")}")
        params.add("cb_variant=${URLEncoder.encode(trackingParams.variant, "UTF-8")}")
        params.add("cb_test=${URLEncoder.encode(trackingParams.testId, "UTF-8")}")
        params.add("cb_ts=${trackingParams.timestamp}")
        
        trackingParams.source?.let { params.add("cb_source=${URLEncoder.encode(it, "UTF-8")}") }
        trackingParams.medium?.let { params.add("cb_medium=${URLEncoder.encode(it, "UTF-8")}") }
        trackingParams.campaign?.let { params.add("cb_campaign=${URLEncoder.encode(it, "UTF-8")}") }
        trackingParams.content?.let { params.add("cb_content=${URLEncoder.encode(it, "UTF-8")}") }
        trackingParams.term?.let { params.add("cb_term=${URLEncoder.encode(it, "UTF-8")}") }
        
        return params.joinToString("&")
    }
    
    /**
     * Creates tracking parameters from session data
     */
    fun createTrackingParams(
        sessionId: String,
        variant: String,
        testId: String,
        utmParams: Map<String, String> = emptyMap()
    ): TrackingParams {
        return TrackingParams(
            sessionId = sessionId,
            variant = variant,
            testId = testId,
            timestamp = System.currentTimeMillis(),
            source = utmParams["utm_source"],
            medium = utmParams["utm_medium"],
            campaign = utmParams["utm_campaign"],
            content = utmParams["utm_content"],
            term = utmParams["utm_term"]
        )
    }
    
    /**
     * Validates if URL contains CBSplit tracking parameters
     */
    fun hasTrackingParams(url: String): Boolean {
        return url.contains("cb_session=") && url.contains("cb_variant=")
    }
    
    /**
     * Removes CBSplit tracking parameters from URL
     */
    fun removeTrackingParams(url: String): String {
        return try {
            val urlObj = URL(url)
            val params = parseQueryParams(urlObj.query)
            val cleanParams = params.filterKeys { !it.startsWith("cb_") }
            
            buildUrl(urlObj, cleanParams)
        } catch (e: Exception) {
            url
        }
    }
}