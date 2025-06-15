# CBSplit - Enterprise A/Z Split Testing & Funnel Analytics Platform

CBSplit is a comprehensive conversion rate optimization (CRO) platform that tracks users across complex multi-step funnels, providing accurate revenue attribution and statistical analysis. Built to compete with tools like Optimizely and VWO, but specifically designed for digital marketers using ClickFunnels, Kajabi, Teachable, and ClickBank.

## 🚀 Features

- **A-Z Testing**: Test up to 26 variants simultaneously with statistical significance
- **Cross-Domain Tracking**: Follow users across ClickFunnels, Kajabi, Teachable, and custom domains
- **Revenue Attribution**: Track revenue back to original test variants with multiple attribution models
- **Funnel Analytics**: Detailed step-by-step funnel analysis with drop-off identification
- **Webhook Integration**: Automatic conversion tracking from ClickBank, ClickFunnels, Kajabi, etc.
- **Real-Time Analytics**: Live dashboard with confidence intervals and winner analysis
- **Statistical Analysis**: Proper significance testing, sample size calculations, and lift analysis

## 📋 Table of Contents

1. [Quick Start](#quick-start)
2. [Installation](#installation)
3. [Basic Usage](#basic-usage)
4. [Platform Integrations](#platform-integrations)
5. [API Reference](#api-reference)
6. [Analytics & Reporting](#analytics--reporting)
7. [Advanced Features](#advanced-features)
8. [Troubleshooting](#troubleshooting)

## 🏃‍♂️ Quick Start

### 1. Deploy Your CBSplit Server

```bash
# Clone and deploy
git clone https://github.com/michaelrochin/cbsplit-server.git
cd cbsplit-server

# Run locally
npm start

# Or deploy to cloud
railway up  # Railway
heroku create your-app && git push heroku main  # Heroku
```

### 2. Add Tracking to Your Landing Page

```html
<!DOCTYPE html>
<html>
<head>
    <title>Landing Page Test</title>
</head>
<body>
    <!-- Variant A Content -->
    <div class="variant-a">
        <h1>Original Headline</h1>
        <p>Original description</p>
        <button onclick="goToSales()">Get Started</button>
    </div>
    
    <!-- Variant B Content -->
    <div class="variant-b" style="display: none;">
        <h1>🚀 New Compelling Headline!</h1>
        <p>⚡ Improved description with urgency</p>
        <button onclick="goToSales()">Join Now - Limited Time!</button>
    </div>
    
    <!-- CBSplit Tracking -->
    <script src="https://your-cbsplit-server.com/cbsplit-tracking.js"></script>
    <script>
        // Initialize test
        const variant = Math.random() < 0.5 ? 'A' : 'B';
        const sessionId = CBSplit.startSession('homepage_test', variant);
        
        // Show variant content
        if (variant === 'B') {
            document.querySelector('.variant-a').style.display = 'none';
            document.querySelector('.variant-b').style.display = 'block';
        }
        
        // Track button clicks
        function goToSales() {
            CBSplit.trackInteraction('button_click', 'cta_button');
            window.location.href = 'https://your-sales-page.com';
        }
    </script>
</body>
</html>
```

### 3. View Results

```javascript
// Get test results
fetch('https://your-cbsplit-server.com/api/cbsplit/analytics/homepage_test')
  .then(response => response.json())
  .then(data => {
    console.log('Winner:', data.winnerAnalysis.winningVariant);
    console.log('Confidence:', data.winnerAnalysis.confidence + '%');
    console.log('Revenue Lift:', data.winnerAnalysis.expectedLift + '%');
  });
```

## 🛠 Installation

### Prerequisites

- Java 11+ or Docker
- Git

### Local Development

```bash
# Clone repository
git clone https://github.com/michaelrochin/cbsplit-server.git
cd cbsplit-server

# Run with Node.js (Recommended)
npm start

# Or run directly
node server.js

# Verify installation
curl http://localhost:8080/health
# Response: {"status":"healthy","service":"cbsplit"}
```

### Cloud Deployment

#### Railway (Recommended)
```bash
npm install -g @railway/cli
railway login
railway init
railway up
```

#### Heroku
```bash
heroku create your-cbsplit-app
git push heroku main
```

#### DigitalOcean
```bash
doctl apps create --spec .do/app.yaml
```

## 🎯 Basic Usage

### Creating Your First A/B Test

#### 1. Set Up Test Structure

```kotlin
// Server-side: Create funnel flow
val funnelSteps = listOf(
    FunnelStep("step1", "Landing Page", "landing", "https://yoursite.com", "A", "test1", 1),
    FunnelStep("step2", "Sales Page", "sales", "https://sales.yoursite.com", "A", "test1", 2),
    FunnelStep("step3", "Checkout", "checkout", "https://checkout.com", "A", "test1", 3)
)

CBSplitFunnelTracking.createFunnelFlow(
    funnelId = "homepage_test",
    funnelName = "Homepage Headline Test",
    testId = "test1",
    baseSteps = funnelSteps
)
```

#### 2. Add Tracking Script

```html
<!-- Include CBSplit tracking -->
<script src="https://your-server.com/cbsplit-tracking.js"></script>
<script>
// Test configuration
const TEST_CONFIG = {
    testId: 'homepage_test',
    variants: ['A', 'B'],
    trafficSplit: [50, 50] // 50/50 split
};

// Assign variant
function assignVariant() {
    const hash = btoa(navigator.userAgent + Date.now()).slice(0, 10);
    return Math.random() < 0.5 ? 'A' : 'B';
}

// Initialize tracking
const variant = assignVariant();
const sessionId = CBSplit.startSession(TEST_CONFIG.testId, variant);

// Show variant content
document.querySelector('.variant-' + variant.toLowerCase()).style.display = 'block';
document.querySelector('.variant-' + (variant === 'A' ? 'b' : 'a')).style.display = 'none';
</script>
```

#### 3. Track Conversions

```html
<!-- Email signup form -->
<form onsubmit="trackEmailSignup(event)">
    <input type="email" name="email" required>
    <button type="submit">Get Free Guide</button>
</form>

<!-- Purchase button -->
<button onclick="trackPurchase(197.00)">Buy Now - $197</button>

<script>
function trackEmailSignup(event) {
    const email = event.target.email.value;
    CBSplit.trackConversion('lead', 0, {
        email: email,
        source: 'homepage'
    });
}

function trackPurchase(amount) {
    CBSplit.trackConversion('purchase', amount, {
        product: 'course_1',
        orderId: 'order_' + Date.now()
    });
}
</script>
```

### Viewing Results

#### Web Dashboard
Visit your CBSplit UI at: `https://your-server.com/`

#### API Access
```bash
# Get comprehensive analytics
curl https://your-server.com/api/cbsplit/analytics/homepage_test

# Get funnel analytics
curl https://your-server.com/api/cbsplit/funnel/homepage_test/analytics

# Response includes:
{
  "winnerAnalysis": {
    "winningVariant": "B",
    "confidence": 96.5,
    "expectedLift": 23.4,
    "recommendation": "Declare winner - statistically significant"
  },
  "variants": [
    {
      "variant": "A",
      "sessions": 1000,
      "conversions": 80,
      "conversionRate": 0.08,
      "revenue": 15840.0
    },
    {
      "variant": "B", 
      "sessions": 980,
      "conversions": 115,
      "conversionRate": 0.117,
      "revenue": 22755.0
    }
  ]
}
```

## 🔗 Platform Integrations

### ClickFunnels Integration

#### 1. Landing Page Setup
```html
<!-- Add to ClickFunnels Page Header -->
<script src="https://your-server.com/cbsplit-tracking.js"></script>
<script>
window.CFSplitTest = {
    init: function() {
        // Get session from URL or create new
        this.sessionId = this.getSessionFromUrl() || CBSplit.startSession('cf_test', this.assignVariant());
        this.trackPageView();
        this.setupFormTracking();
        this.injectTrackingLinks();
    },
    
    getSessionFromUrl: function() {
        const params = new URLSearchParams(window.location.search);
        return params.get('cb_session');
    },
    
    assignVariant: function() {
        return Math.random() < 0.5 ? 'A' : 'B';
    },
    
    trackPageView: function() {
        CBSplit.trackStep(this.getCurrentStep());
    },
    
    getCurrentStep: function() {
        const url = window.location.pathname;
        if (url.includes('optin')) return 1;
        if (url.includes('sales')) return 2;
        if (url.includes('order')) return 3;
        return 1;
    },
    
    setupFormTracking: function() {
        document.addEventListener('submit', (e) => {
            if (e.target.querySelector('input[type="email"]')) {
                CBSplit.trackConversion('lead', 0);
            }
        });
    },
    
    injectTrackingLinks: function() {
        document.querySelectorAll('a[href]').forEach(link => {
            const href = link.getAttribute('href');
            if (href && !href.includes('cb_session=')) {
                const separator = href.includes('?') ? '&' : '?';
                link.setAttribute('href', href + separator + 'cb_session=' + this.sessionId);
            }
        });
    }
};

// Auto-initialize
CFSplitTest.init();
</script>
```

#### 2. Webhook Setup
1. Go to ClickFunnels → Settings → Webhooks
2. Add URL: `https://your-server.com/webhook/clickfunnels`
3. Select events: Contact created, Order created

### Kajabi Integration

#### 1. Landing Page Tracking
```html
<!-- Add to Kajabi → Settings → Code Snippets → Head -->
<script src="https://your-server.com/cbsplit-tracking.js"></script>
<script>
window.KajabiSplitTest = {
    testId: 'kajabi_course_test',
    
    init: function() {
        this.sessionId = CBSplit.startSession(this.testId, this.assignVariant());
        this.showVariantContent();
        this.trackKajabiForms();
    },
    
    assignVariant: function() {
        // Consistent assignment based on user IP/session
        const hash = this.simpleHash(navigator.userAgent);
        return hash % 2 === 0 ? 'A' : 'B';
    },
    
    simpleHash: function(str) {
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            const char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash; // Convert to 32bit integer
        }
        return Math.abs(hash);
    },
    
    showVariantContent: function() {
        const variant = this.variant || 'A';
        document.querySelectorAll('.variant-' + (variant === 'A' ? 'b' : 'a')).forEach(el => {
            el.style.display = 'none';
        });
    },
    
    trackKajabiForms: function() {
        document.addEventListener('submit', (e) => {
            if (e.target.classList.contains('kajabi-form')) {
                const email = e.target.querySelector('input[type="email"]')?.value;
                CBSplit.trackConversion('lead', 0, { email: email });
            }
        });
    }
};

KajabiSplitTest.init();
</script>

<!-- Variant Content -->
<div class="variant-a">
    <h1>Master Web Development</h1>
    <p>Comprehensive course for beginners</p>
</div>

<div class="variant-b">
    <h1>🚀 Become a Web Dev Pro in 30 Days!</h1>
    <p>⚡ Fast-track bootcamp with job guarantee!</p>
</div>
```

#### 2. Webhook Configuration
1. Kajabi → Settings → Integrations → Webhooks
2. URL: `https://your-server.com/webhook/kajabi`
3. Events: `offer.purchased`, `form.submitted`

### Teachable Integration

#### 1. Course Page Tracking
```html
<!-- Add to Teachable → Site → Code Snippets → Head -->
<script src="https://your-server.com/cbsplit-tracking.js"></script>
<script>
window.TeachableSplitTest = {
    init: function() {
        this.sessionId = CBSplit.startSession('teachable_test', this.assignVariant());
        this.trackCoursePage();
        this.setupEnrollmentTracking();
        this.showVariantPricing();
    },
    
    trackCoursePage: function() {
        const pageType = this.getPageType();
        CBSplit.trackStep(pageType === 'course_sales' ? 1 : 2);
    },
    
    getPageType: function() {
        if (window.location.pathname.includes('/courses/') && 
            document.querySelector('.course-card')) return 'course_sales';
        if (document.querySelector('.lecture-content')) return 'course_content';
        return 'unknown';
    },
    
    setupEnrollmentTracking: function() {
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('enroll-button') ||
                e.target.textContent.toLowerCase().includes('enroll')) {
                
                CBSplit.trackConversion('enrollment', this.getCoursePrice());
            }
        });
    },
    
    getCoursePrice: function() {
        const priceEl = document.querySelector('.course-price, .price');
        return priceEl ? parseFloat(priceEl.textContent.replace(/[^0-9.]/g, '')) : 0;
    },
    
    showVariantPricing: function() {
        if (this.variant === 'B') {
            // Show discounted pricing
            document.querySelectorAll('.course-price').forEach(el => {
                const originalPrice = parseFloat(el.textContent.replace(/[^0-9.]/g, ''));
                const discountedPrice = originalPrice * 0.8; // 20% discount
                el.innerHTML = `<span style="text-decoration: line-through;">$${originalPrice}</span> 
                               <span style="color: red; font-weight: bold;">$${discountedPrice}</span>`;
            });
        }
    }
};

TeachableSplitTest.init();
</script>
```

### ClickBank Integration

CBSplit automatically processes ClickBank INS (Instant Notification Service) webhooks.

#### Setup:
1. ClickBank Vendor Account → Account Settings → My Site → Advanced Tools
2. Instant Notification URL: `https://your-server.com/webhook/clickbank`
3. Secret Key: Set in your CBSplit server environment

## 📊 Analytics & Reporting

### Statistical Significance

CBSplit uses proper statistical testing:

```javascript
// Check if test is ready for decision
fetch('/api/cbsplit/analytics/your_test')
  .then(r => r.json())
  .then(data => {
    const analysis = data.winnerAnalysis;
    
    if (analysis.confidence >= 95) {
        console.log(`🎉 Winner: ${analysis.winningVariant}`);
        console.log(`Confidence: ${analysis.confidence}%`);
        console.log(`Expected Lift: ${analysis.expectedLift}%`);
    } else if (analysis.confidence >= 90) {
        console.log(`⚠️ Likely winner: ${analysis.winningVariant} (${analysis.confidence}%)`);
        console.log(`Recommendation: ${analysis.recommendation}`);
    } else {
        console.log(`📊 Continue testing - insufficient data`);
    }
  });
```

### Revenue Attribution Models

CBSplit supports multiple attribution models:

```kotlin
// First-touch attribution (default)
CBSplitRevenueAttribution.attributeRevenue(
    sessionId = sessionId,
    orderId = orderId,
    revenue = 197.0,
    attributionModelType = "first_touch"
)

// Last-touch attribution
CBSplitRevenueAttribution.attributeRevenue(
    attributionModelType = "last_touch"
)

// Linear attribution (equal credit to all touchpoints)
CBSplitRevenueAttribution.attributeRevenue(
    attributionModelType = "linear"
)

// Time-decay attribution (more recent touchpoints get more credit)
CBSplitRevenueAttribution.attributeRevenue(
    attributionModelType = "time_decay"
)
```

### Funnel Analysis

```javascript
// Get detailed funnel breakdown
fetch('/api/cbsplit/funnel/your_funnel/analytics?variant=A')
  .then(r => r.json())
  .then(data => {
    console.log('Completion Rate:', data.completionRate);
    console.log('Drop-off Analysis:', data.dropoffAnalysis);
    
    // Example output:
    // Step 1 (Landing): 100% (1000 visitors)
    // Step 2 (Sales): 65% (650 visitors) - 35% drop-off
    // Step 3 (Checkout): 45% (450 visitors) - 20% drop-off  
    // Step 4 (Purchase): 8% (80 visitors) - 37% drop-off
  });
```

### Segment Analysis

```javascript
// Analyze performance by traffic source
fetch('/api/cbsplit/analytics/your_test')
  .then(r => r.json())
  .then(data => {
    data.segmentAnalysis.forEach(segment => {
      if (segment.segmentName === 'traffic_source') {
        console.log(`${segment.segmentValue}: ${segment.variants[0].conversionRate}% conversion`);
      }
    });
    
    // Example output:
    // Facebook: 8.5% conversion rate
    // Google: 12.3% conversion rate  
    // Email: 15.7% conversion rate
    // Direct: 6.2% conversion rate
  });
```

## 🔧 Advanced Features

### Custom Event Tracking

```javascript
// Track custom events
CBSplit.trackInteraction('video_play', 'product_demo', null, {
    videoLength: '5:30',
    videoPosition: '2:15'
});

CBSplit.trackInteraction('scroll_depth', 'page', '75%', {
    timeOnPage: 120 // seconds
});

CBSplit.trackInteraction('button_click', 'pricing_table', 'plan_premium', {
    planPrice: '$97/month'
});
```

### Multi-Touch Customer Journeys

```kotlin
// Track complex customer journey
CBSplitRevenueAttribution.trackTouchpoint(
    customerId = "customer_123",
    sessionId = sessionId,
    source = "facebook",
    medium = "cpc", 
    campaign = "summer_sale",
    variant = "B",
    testId = "homepage_test",
    page = "landing_page",
    action = "page_view"
)

// Later, when they purchase
CBSplitRevenueAttribution.attributeRevenue(
    sessionId = sessionId,
    orderId = "order_456",
    revenue = 297.0,
    customerId = "customer_123"
)

// Get customer lifetime value
val customerAnalytics = CBSplitRevenueAttribution.getCustomerLtvAnalytics("customer_123")
println("Customer LTV: ${customerAnalytics?.get("totalRevenue")}")
```

### A/Z Testing (More Than 2 Variants)

```javascript
// Test multiple variants
const variants = ['A', 'B', 'C', 'D', 'E'];
const weights = [20, 20, 20, 20, 20]; // Equal distribution

function assignVariant() {
    const random = Math.random() * 100;
    let cumulative = 0;
    
    for (let i = 0; i < variants.length; i++) {
        cumulative += weights[i];
        if (random <= cumulative) {
            return variants[i];
        }
    }
    return 'A'; // Fallback
}

const variant = assignVariant();
CBSplit.startSession('multivariant_test', variant);

// Show appropriate content
document.querySelector('.variant-' + variant.toLowerCase()).style.display = 'block';
```

### Geo-Based Testing

```javascript
// Different variants by country
async function getCountryBasedVariant() {
    try {
        const response = await fetch('https://ipapi.co/json/');
        const data = await response.json();
        const country = data.country_code;
        
        // US/UK/CA get variant B, others get variant A
        if (['US', 'UK', 'CA'].includes(country)) {
            return 'B';
        } else {
            return 'A';
        }
    } catch (e) {
        return 'A'; // Fallback
    }
}

getCountryBasedVariant().then(variant => {
    CBSplit.startSession('geo_test', variant);
});
```

## 🔍 API Reference

### Session Management

#### Start Session
```http
POST /api/cbsplit/start-session
Content-Type: application/json

{
  "sessionId": "session_123",
  "funnelId": "my_funnel",
  "variant": "A", 
  "entryUrl": "https://example.com"
}
```

#### Track Funnel Step
```http
POST /api/cbsplit/funnel/step
Content-Type: application/json

{
  "sessionId": "session_123",
  "stepPosition": 2,
  "timestamp": 1640995200000,
  "previousStepExitAction": "next"
}
```

#### Track Conversion
```http
POST /api/cbsplit/funnel/conversion
Content-Type: application/json

{
  "sessionId": "session_123",
  "conversionType": "purchase",
  "value": 197.0,
  "currency": "USD",
  "metadata": {
    "orderId": "order_456",
    "product": "course_1"
  }
}
```

### Analytics

#### Get Test Analytics
```http
GET /api/cbsplit/analytics/{testId}

Response:
{
  "testId": "homepage_test",
  "variants": [...],
  "winnerAnalysis": {...},
  "statisticalSignificance": {...},
  "revenueMetrics": {...}
}
```

#### Get Funnel Analytics
```http
GET /api/cbsplit/funnel/{funnelId}/analytics?variant=A

Response:
{
  "funnelId": "my_funnel",
  "variant": "A",
  "totalSessions": 1000,
  "completionRate": 0.08,
  "dropoffAnalysis": {...},
  "averageTimePerStep": {...}
}
```

### Webhooks

All webhook endpoints automatically process and attribute conversions:

- `POST /webhook/clickbank` - ClickBank INS
- `POST /webhook/clickfunnels` - ClickFunnels events
- `POST /webhook/kajabi` - Kajabi purchases/enrollments
- `POST /webhook/teachable` - Teachable enrollments
- `POST /webhook/{platform}` - Generic webhook handler

## 🐛 Troubleshooting

### Common Issues

#### 1. Tracking Not Working
```javascript
// Debug tracking
console.log('CBSplit loaded:', typeof CBSplit !== 'undefined');
console.log('Session ID:', CBSplit.sessionId);

// Check network requests in browser dev tools
// Look for requests to /api/cbsplit/
```

#### 2. Cross-Domain Issues
```javascript
// Verify tracking parameters are passed
const urlParams = new URLSearchParams(window.location.search);
console.log('CB Session:', urlParams.get('cb_session'));
console.log('CB Variant:', urlParams.get('cb_variant'));

// If missing, check link injection:
document.querySelectorAll('a[href]').forEach(link => {
    console.log('Link:', link.href);
    // Should contain cb_session= parameter
});
```

#### 3. Webhook Not Receiving Data
```bash
# Check webhook endpoint
curl -X POST https://your-server.com/webhook/clickbank \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "transactionType=SALE&receipt=12345&totalAccountAmount=197.00"

# Check server logs
docker logs cbsplit-server
# Or
heroku logs --tail
```

#### 4. No Statistical Significance
- **Check sample size**: Need minimum ~100 conversions per variant
- **Check test duration**: Run for at least 1-2 business cycles
- **Check traffic quality**: Ensure consistent traffic sources

```javascript
// Check current sample size
fetch('/api/cbsplit/analytics/your_test')
  .then(r => r.json())
  .then(data => {
    const winner = data.winnerAnalysis;
    console.log('Current sample:', winner.currentSampleSize);
    console.log('Required sample:', winner.requiredSampleSize);
    console.log('Days to significance:', winner.daysToSignificance);
  });
```

### Performance Optimization

#### 1. Reduce Server Load
```javascript
// Batch tracking requests
const trackingQueue = [];
setInterval(() => {
    if (trackingQueue.length > 0) {
        fetch('/api/cbsplit/batch-track', {
            method: 'POST',
            body: JSON.stringify(trackingQueue)
        });
        trackingQueue.length = 0;
    }
}, 5000); // Send every 5 seconds
```

#### 2. Cache Analytics Results
```kotlin
// Server-side caching (implement in your deployment)
val cache = mutableMapOf<String, Pair<Long, AdvancedTestMetrics>>()
val CACHE_TTL = 5 * 60 * 1000L // 5 minutes

fun getCachedAnalytics(testId: String): AdvancedTestMetrics? {
    val cached = cache[testId]
    if (cached != null && System.currentTimeMillis() - cached.first < CACHE_TTL) {
        return cached.second
    }
    return null
}
```

## 🎯 Best Practices

### 1. Test Planning
- **Single variable per test**: Only change one element at a time
- **Significant differences**: Make meaningful changes (not just color tweaks)
- **Clear hypothesis**: Know what you're testing and why
- **Sufficient traffic**: Ensure you have enough visitors for statistical power

### 2. Implementation
- **Consistent assignment**: Use hash-based assignment for returning visitors
- **Quality assurance**: Test both variants thoroughly before launching
- **Tracking verification**: Verify all conversion events are firing correctly
- **Cross-device tracking**: Consider user journey across devices

### 3. Analysis
- **Wait for significance**: Don't call winners too early (minimum 95% confidence)
- **Consider practical significance**: 1% lift might be statistically significant but not worth implementing
- **Segment analysis**: Look at performance across different user segments
- **External factors**: Account for seasonality, promotions, etc.

### 4. Scaling
- **Progressive rollout**: Start with 10% traffic, then scale up
- **Monitor performance**: Watch for technical issues during rollout
- **Document results**: Keep detailed records of all tests and results
- **Share learnings**: Document insights for future tests

## 📞 Support

For issues, questions, or feature requests:

1. **Check the troubleshooting section** above
2. **Review server logs** for error messages
3. **Test with minimal examples** to isolate issues
4. **Check API endpoints** with curl/Postman

## 📜 License

MIT License - feel free to use for commercial projects.

---

**CBSplit** - Enterprise-grade split testing for digital marketers. Track conversions across any funnel with statistical precision.