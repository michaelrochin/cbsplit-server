const http = require('http');
const url = require('url');
const fs = require('fs');
const path = require('path');

const port = process.env.PORT || 8080;

// Simple in-memory storage for demo
let sessions = new Map();
let analytics = new Map();

const server = http.createServer((req, res) => {
    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;
    const method = req.method;

    // CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (method === 'OPTIONS') {
        res.writeHead(200);
        res.end();
        return;
    }

    // Health check
    if (pathname === '/health') {
        res.setHeader('Content-Type', 'application/json');
        res.writeHead(200);
        res.end(JSON.stringify({
            status: 'healthy',
            service: 'cbsplit',
            version: '1.0.0',
            timestamp: new Date().toISOString()
        }));
        return;
    }

    // Tracking script
    if (pathname === '/cbsplit-tracking.js') {
        res.setHeader('Content-Type', 'application/javascript');
        res.writeHead(200);
        res.end(`
// CBSplit Universal Tracking Script
(function() {
    window.CBSplit = window.CBSplit || {
        apiUrl: '${req.headers.host ? 'https://' + req.headers.host : 'http://localhost:8080'}/api/cbsplit',
        
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
            }).catch(e => console.warn('CBSplit tracking error:', e));
            
            this.sessionId = sessionId;
            this.funnelId = funnelId;
            this.variant = variant;
            
            console.log('CBSplit session started:', sessionId, 'variant:', variant);
            return sessionId;
        },
        
        trackConversion: function(type, value, metadata) {
            if (!this.sessionId) return;
            
            fetch(this.apiUrl + '/conversion', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    sessionId: this.sessionId,
                    conversionType: type,
                    value: value || 0,
                    metadata: metadata || {},
                    timestamp: Date.now()
                })
            }).catch(e => console.warn('CBSplit conversion error:', e));
            
            console.log('CBSplit conversion tracked:', type, value);
        },
        
        trackInteraction: function(action, element, value, metadata) {
            if (!this.sessionId) return;
            
            fetch(this.apiUrl + '/interaction', {
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
            }).catch(e => console.warn('CBSplit interaction error:', e));
        }
    };
    
    console.log('CBSplit tracking loaded');
})();
        `);
        return;
    }

    // API Routes
    if (pathname.startsWith('/api/cbsplit/')) {
        if (method === 'POST') {
            let body = '';
            req.on('data', chunk => {
                body += chunk.toString();
            });
            req.on('end', () => {
                handleApiPost(pathname, body, res);
            });
        } else if (method === 'GET') {
            handleApiGet(pathname, parsedUrl.query, res);
        }
        return;
    }

    // Webhook Routes
    if (pathname.startsWith('/webhook/')) {
        if (method === 'POST') {
            let body = '';
            req.on('data', chunk => {
                body += chunk.toString();
            });
            req.on('end', () => {
                handleWebhook(pathname, body, res);
            });
        }
        return;
    }

    // Default response
    res.setHeader('Content-Type', 'application/json');
    res.writeHead(404);
    res.end(JSON.stringify({
        error: 'Not found',
        availableEndpoints: [
            '/health',
            '/cbsplit-tracking.js',
            '/api/cbsplit/start-session',
            '/api/cbsplit/conversion',
            '/api/cbsplit/analytics/{testId}',
            '/webhook/clickbank',
            '/webhook/clickfunnels'
        ]
    }));
});

function handleApiPost(pathname, body, res) {
    res.setHeader('Content-Type', 'application/json');
    
    try {
        const data = JSON.parse(body);
        
        if (pathname === '/api/cbsplit/start-session') {
            const { sessionId, funnelId, variant, entryUrl } = data;
            sessions.set(sessionId, {
                sessionId,
                funnelId,
                variant,
                entryUrl,
                startTime: Date.now(),
                conversions: [],
                interactions: []
            });
            
            res.writeHead(200);
            res.end(JSON.stringify({ success: true, sessionId }));
            return;
        }
        
        if (pathname === '/api/cbsplit/conversion') {
            const { sessionId, conversionType, value, metadata } = data;
            const session = sessions.get(sessionId);
            if (session) {
                session.conversions.push({
                    type: conversionType,
                    value: value || 0,
                    metadata: metadata || {},
                    timestamp: Date.now()
                });
                
                // Update analytics
                const testId = session.funnelId;
                if (!analytics.has(testId)) {
                    analytics.set(testId, { variants: new Map() });
                }
                const testData = analytics.get(testId);
                if (!testData.variants.has(session.variant)) {
                    testData.variants.set(session.variant, {
                        sessions: 0,
                        conversions: 0,
                        revenue: 0
                    });
                }
                const variantData = testData.variants.get(session.variant);
                variantData.conversions++;
                variantData.revenue += (value || 0);
            }
            
            res.writeHead(200);
            res.end(JSON.stringify({ success: true }));
            return;
        }
        
        if (pathname === '/api/cbsplit/interaction') {
            const { sessionId, action, element, value, metadata } = data;
            const session = sessions.get(sessionId);
            if (session) {
                session.interactions.push({
                    action,
                    element,
                    value,
                    metadata: metadata || {},
                    timestamp: Date.now()
                });
            }
            
            res.writeHead(200);
            res.end(JSON.stringify({ success: true }));
            return;
        }
        
    } catch (e) {
        res.writeHead(400);
        res.end(JSON.stringify({ error: 'Invalid JSON' }));
        return;
    }
    
    res.writeHead(404);
    res.end(JSON.stringify({ error: 'API endpoint not found' }));
}

function handleApiGet(pathname, query, res) {
    res.setHeader('Content-Type', 'application/json');
    
    // Analytics endpoint
    const analyticsMatch = pathname.match(/^\/api\/cbsplit\/analytics\/(.+)$/);
    if (analyticsMatch) {
        const testId = analyticsMatch[1];
        const testData = analytics.get(testId);
        
        if (!testData) {
            res.writeHead(404);
            res.end(JSON.stringify({ error: 'Test not found' }));
            return;
        }
        
        const variants = [];
        let totalSessions = 0;
        
        for (const [variant, data] of testData.variants) {
            totalSessions += data.sessions;
            variants.push({
                variant,
                sessions: data.sessions,
                conversions: data.conversions,
                conversionRate: data.sessions > 0 ? data.conversions / data.sessions : 0,
                revenue: data.revenue,
                revenuePerVisitor: data.sessions > 0 ? data.revenue / data.sessions : 0
            });
        }
        
        // Simple winner analysis
        let winner = variants.reduce((best, current) => 
            current.revenuePerVisitor > best.revenuePerVisitor ? current : best
        , variants[0] || { variant: 'A', revenuePerVisitor: 0 });
        
        const response = {
            testId,
            variants,
            winnerAnalysis: {
                winningVariant: winner.variant,
                confidence: totalSessions > 100 ? 95 : totalSessions > 50 ? 80 : 60,
                expectedLift: winner.revenuePerVisitor > 0 ? 15 : 0,
                recommendation: totalSessions > 100 ? 'Sufficient data for decision' : 'Continue testing'
            },
            totalSessions
        };
        
        res.writeHead(200);
        res.end(JSON.stringify(response));
        return;
    }
    
    res.writeHead(404);
    res.end(JSON.stringify({ error: 'Endpoint not found' }));
}

function handleWebhook(pathname, body, res) {
    console.log('Webhook received:', pathname, body.substring(0, 100) + '...');
    
    // Simple webhook acknowledgment
    res.setHeader('Content-Type', 'application/json');
    res.writeHead(200);
    res.end(JSON.stringify({ received: true, timestamp: new Date().toISOString() }));
}

server.listen(port, () => {
    console.log(`🚀 CBSplit Server running on port ${port}`);
    console.log(`📊 Health check: http://localhost:${port}/health`);
    console.log(`📜 Tracking script: http://localhost:${port}/cbsplit-tracking.js`);
    console.log(`🔗 API endpoints: http://localhost:${port}/api/cbsplit/`);
});

// Count sessions for analytics
setInterval(() => {
    for (const [testId, testData] of analytics) {
        for (const [variant, data] of testData.variants) {
            // Count active sessions for this variant
            const activeSessions = Array.from(sessions.values())
                .filter(s => s.funnelId === testId && s.variant === variant);
            data.sessions = activeSessions.length;
        }
    }
}, 10000); // Update every 10 seconds