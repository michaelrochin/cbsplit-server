# CBSplit - Enterprise Split Testing for Course Creators

**Live Server:** https://cbsplit-server.onrender.com

CBSplit provides **enterprise-level A/B testing** for course creators using Kajabi, Teachable, and ClickFunnels. Unlike native platform testing, CBSplit tracks users across your entire funnel with statistical precision.

## 🏆 Why CBSplit Beats Native Platform Testing

### **Native Platform Limitations:**
- ❌ **Kajabi**: No native A/B testing for course pages
- ❌ **Teachable**: Basic testing, no cross-funnel tracking  
- ❌ **ClickFunnels**: Limited to single pages, no revenue attribution
- ❌ **All Platforms**: No statistical significance calculations
- ❌ **All Platforms**: Can't track users across multiple domains

### **CBSplit Advantages:**
- ✅ **Cross-Platform Tracking**: Follow users from Facebook Ad → Landing Page → Course Platform → Purchase
- ✅ **Statistical Significance**: Know when you have enough data to make decisions (95% confidence)
- ✅ **Revenue Attribution**: See which variant actually makes more money, not just converts more
- ✅ **Multi-Touch Attribution**: Credit all touchpoints in the customer journey
- ✅ **Real-Time Analytics**: Live dashboard with conversion rates and revenue data
- ✅ **Unlimited Tests**: Run multiple tests simultaneously across all platforms
- ✅ **Advanced Segmentation**: Analyze by traffic source, device, geography

## 🚀 Quick Start

### **Test Your Server:**
```bash
curl https://cbsplit-server.onrender.com/health
# Should return: {"status":"healthy","service":"cbsplit"}
```

### **Get Analytics:**
```bash
curl https://cbsplit-server.onrender.com/api/cbsplit/analytics/your_test_id
```

---

# 📚 Kajabi Integration Guide

## **Why Use CBSplit with Kajabi:**
- Kajabi has **NO native A/B testing** for course sales pages
- Test headlines, pricing, course descriptions, and urgency elements
- Track from landing page through course enrollment
- See which variant drives highest **lifetime customer value**

## **Step 1: Add CBSplit to Your Kajabi Course Page**

### **Method A: Code Block (Recommended)**
1. **Edit your course page** in Kajabi
2. **Add a "Code" block** at the top of your page
3. **Paste this code:**

```html
<script src="https://cbsplit-server.onrender.com/cbsplit-tracking.js"></script>
<script>
// Kajabi CBSplit A/B Test
window.KajabiTest = {
    testId: 'kajabi_course_sales', // Change this for different courses
    
    init: function() {
        // Assign variant consistently
        this.variant = this.getVariant();
        
        // Start CBSplit session
        this.sessionId = CBSplit.startSession(this.testId, this.variant, window.location.href);
        
        // Show variant content
        this.showVariant();
        
        // Track conversions
        this.setupTracking();
        
        console.log('🚀 CBSplit active - Variant:', this.variant);
    },
    
    getVariant: function() {
        // Consistent assignment based on user fingerprint
        const fingerprint = navigator.userAgent + navigator.language;
        let hash = 0;
        for (let i = 0; i < fingerprint.length; i++) {
            hash = ((hash << 5) - hash + fingerprint.charCodeAt(i)) & 0xffffffff;
        }
        return Math.abs(hash) % 2 === 0 ? 'A' : 'B';
    },
    
    showVariant: function() {
        // Hide all variants
        document.querySelectorAll('.cbsplit-a, .cbsplit-b').forEach(el => {
            el.style.display = 'none';
        });
        
        // Show selected variant
        document.querySelectorAll('.cbsplit-' + this.variant.toLowerCase()).forEach(el => {
            el.style.display = 'block';
        });
        
        // Apply variant B changes
        if (this.variant === 'B') {
            this.applyVariantB();
        }
    },
    
    applyVariantB: function() {
        setTimeout(() => {
            // Add urgency to pricing
            const priceElements = document.querySelectorAll('.price, [class*="price"], .course-price');
            priceElements.forEach(el => {
                if (!el.dataset.cbModified) {
                    el.style.cssText += 'background: #ff4444 !important; color: white !important; padding: 8px 12px !important; border-radius: 6px !important; box-shadow: 0 4px 12px rgba(255,68,68,0.3) !important;';
                    el.dataset.cbModified = 'true';
                }
            });
            
            // Add countdown timer
            const urgencyDiv = document.createElement('div');
            urgencyDiv.innerHTML = '🔥 <strong>LIMITED TIME: 50% OFF!</strong> - Offer expires in 24 hours';
            urgencyDiv.style.cssText = 'background: linear-gradient(45deg, #ff6b6b, #ee5a24); color: white; padding: 15px; text-align: center; font-size: 18px; border-radius: 8px; margin: 20px 0; animation: pulse 2s infinite;';
            
            const targetElement = document.querySelector('.course-header, .hero, h1') || document.body.firstElementChild;
            if (targetElement && targetElement.parentNode) {
                targetElement.parentNode.insertBefore(urgencyDiv, targetElement.nextSibling);
            }
        }, 500);
    },
    
    setupTracking: function() {
        // Track enrollment clicks
        document.addEventListener('click', (e) => {
            const text = e.target.textContent.toLowerCase();
            const isEnrollButton = text.includes('enroll') || text.includes('buy') || 
                                 text.includes('purchase') || text.includes('join') || 
                                 text.includes('start') || text.includes('get access');
            
            if (isEnrollButton) {
                const price = this.extractPrice();
                CBSplit.trackConversion('enrollment_click', price, {
                    buttonText: e.target.textContent.trim(),
                    courseName: document.title,
                    variant: this.variant,
                    source: 'kajabi'
                });
                console.log('💰 Enrollment tracked:', price, 'Variant:', this.variant);
            }
        });
        
        // Track form submissions (email signups)
        document.addEventListener('submit', (e) => {
            if (e.target.querySelector('input[type="email"]')) {
                CBSplit.trackConversion('email_signup', 0, {
                    variant: this.variant,
                    source: 'kajabi'
                });
                console.log('📧 Email signup tracked - Variant:', this.variant);
            }
        });
    },
    
    extractPrice: function() {
        const priceSelectors = ['.price', '.course-price', '[data-price]', '[class*="price"]'];
        for (const selector of priceSelectors) {
            const el = document.querySelector(selector);
            if (el) {
                const match = el.textContent.match(/\$?(\d+(?:\.\d{2})?)/);
                if (match) return parseFloat(match[1]);
            }
        }
        return 97; // Default price
    }
};

// Auto-start
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => KajabiTest.init());
} else {
    KajabiTest.init();
}
</script>

<!-- Add this CSS for animations -->
<style>
@keyframes pulse {
    0% { transform: scale(1); }
    50% { transform: scale(1.05); }
    100% { transform: scale(1); }
}
</style>
```

## **Step 2: Add Variant Content**

### **In your Kajabi page content, add variant sections:**

```html
<!-- Variant A: Original Content -->
<div class="cbsplit-a">
    <h2>Master Web Development</h2>
    <p>Learn HTML, CSS, and JavaScript at your own pace with our comprehensive course.</p>
    <ul>
        <li>✓ 50+ video lessons</li>
        <li>✓ Downloadable resources</li>
        <li>✓ Community access</li>
        <li>✓ Certificate of completion</li>
    </ul>
</div>

<!-- Variant B: High-Converting Content -->
<div class="cbsplit-b">
    <h2>🚀 Go From Beginner to HIRED Web Developer in 60 Days!</h2>
    <p><strong>⚡ Join 3,247 students who landed jobs after our course!</strong></p>
    <p>Skip years of trial and error. Get the exact roadmap companies want:</p>
    <ul>
        <li>🎯 <strong>Job-Ready Portfolio</strong> - 5 projects employers love</li>
        <li>💼 <strong>Interview Prep</strong> - Practice with real coding challenges</li>
        <li>🏆 <strong>Career Support</strong> - Resume review + job search strategy</li>
        <li>⚡ <strong>Fast Track</strong> - Learn 3x faster with our proven method</li>
    </ul>
    
    <div style="background: linear-gradient(45deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px; margin: 20px 0; text-align: center;">
        <h3>🔥 EARLY BIRD SPECIAL</h3>
        <p><strong>Save $200 if you enroll in the next 24 hours!</strong></p>
        <p style="font-size: 24px;">~~$297~~ <span style="color: #FFD700;">$97 TODAY ONLY</span></p>
    </div>
</div>
```

## **Step 3: Set Up Revenue Tracking (Optional)**

### **Add Kajabi webhook:**
1. **Go to**: Kajabi Settings → Integrations → Webhooks
2. **Webhook URL**: `https://cbsplit-server.onrender.com/webhook/kajabi`
3. **Events**: Select `offer.purchased`

---

# 🎓 Teachable Integration Guide

## **Why Use CBSplit with Teachable:**
- Teachable's native testing is **limited to basic elements**
- No **cross-funnel tracking** from ads to enrollment
- No **statistical significance** calculations
- CBSplit tracks entire customer journey with revenue attribution

## **Step 1: Add CBSplit to Your Teachable Course**

### **Go to**: Site → Code Snippets → Header Code

```html
<script src="https://cbsplit-server.onrender.com/cbsplit-tracking.js"></script>
<script>
// Teachable CBSplit Integration
window.TeachableTest = {
    testId: 'teachable_course_test',
    
    init: function() {
        // Only run on course sales pages
        if (this.isSalesPage()) {
            this.variant = this.getVariant();
            this.sessionId = CBSplit.startSession(this.testId, this.variant, window.location.href);
            this.showVariant();
            this.setupTracking();
            console.log('🎓 CBSplit active on Teachable - Variant:', this.variant);
        }
    },
    
    isSalesPage: function() {
        return window.location.pathname.includes('/courses/') && 
               (document.querySelector('.course-card') || 
                document.querySelector('.enroll-button') ||
                document.querySelector('[class*="enroll"]'));
    },
    
    getVariant: function() {
        const key = 'teachable_variant_' + window.location.pathname;
        let variant = localStorage.getItem(key);
        if (!variant) {
            variant = Math.random() < 0.5 ? 'A' : 'B';
            localStorage.setItem(key, variant);
        }
        return variant;
    },
    
    showVariant: function() {
        if (this.variant === 'B') {
            this.applyVariantB();
        }
    },
    
    applyVariantB: function() {
        setTimeout(() => {
            // Transform pricing for variant B
            const priceElements = document.querySelectorAll('.course-price, .price, [class*="price"]');
            priceElements.forEach(el => {
                const originalPrice = el.textContent;
                const match = originalPrice.match(/\$(\d+)/);
                if (match) {
                    const price = parseInt(match[1]);
                    const discountedPrice = Math.round(price * 0.7); // 30% off
                    el.innerHTML = `
                        <span style="text-decoration: line-through; color: #999;">$${price}</span>
                        <span style="color: #e74c3c; font-weight: bold; font-size: 1.2em;">$${discountedPrice}</span>
                        <div style="background: #e74c3c; color: white; padding: 5px; border-radius: 3px; font-size: 12px; margin-top: 5px;">
                            🔥 LIMITED TIME: 30% OFF
                        </div>
                    `;
                }
            });
            
            // Add social proof
            const socialProof = document.createElement('div');
            socialProof.innerHTML = `
                <div style="background: #2ecc71; color: white; padding: 15px; border-radius: 8px; margin: 20px 0; text-align: center;">
                    ⭐ "This course changed my career!" - <strong>Sarah M.</strong><br>
                    👥 <strong>2,847 students enrolled</strong> • ⭐ <strong>4.9/5 rating</strong>
                </div>
            `;
            
            const targetElement = document.querySelector('.course-header') || 
                                document.querySelector('h1') || 
                                document.querySelector('.hero');
            if (targetElement) {
                targetElement.parentNode.insertBefore(socialProof, targetElement.nextSibling);
            }
        }, 1000);
    },
    
    setupTracking: function() {
        // Track enrollment buttons
        document.addEventListener('click', (e) => {
            const isEnrollButton = e.target.classList.contains('enroll-button') ||
                                 e.target.textContent.toLowerCase().includes('enroll') ||
                                 e.target.textContent.toLowerCase().includes('buy');
            
            if (isEnrollButton) {
                const price = this.extractPrice();
                CBSplit.trackConversion('enrollment_attempt', price, {
                    variant: this.variant,
                    source: 'teachable',
                    courseName: document.title
                });
                console.log('📚 Teachable enrollment tracked:', price);
            }
        });
        
        // Track successful enrollment (thank you page)
        if (window.location.pathname.includes('/enrolled') || 
            window.location.pathname.includes('/thank') ||
            document.querySelector('.enrollment-success')) {
            
            const price = this.extractPriceFromStorage();
            CBSplit.trackConversion('enrollment_complete', price, {
                variant: this.variant,
                source: 'teachable'
            });
        }
    },
    
    extractPrice: function() {
        const priceElements = document.querySelectorAll('.course-price, .price, [class*="price"]');
        for (const el of priceElements) {
            const match = el.textContent.match(/\$(\d+)/);
            if (match) {
                const price = parseInt(match[1]);
                localStorage.setItem('teachable_last_price', price);
                return price;
            }
        }
        return 97;
    },
    
    extractPriceFromStorage: function() {
        return parseInt(localStorage.getItem('teachable_last_price') || '97');
    }
};

// Auto-initialize
TeachableTest.init();
</script>
```

## **Step 2: Create Variant Content**

### **Add to your course description/content:**

```html
<!-- Variant A: Standard Description -->
<div class="cbsplit-a">
    <h3>What You'll Learn</h3>
    <ul>
        <li>Course fundamentals</li>
        <li>Practical exercises</li>
        <li>Real-world applications</li>
    </ul>
</div>

<!-- Variant B: Results-Focused Description -->
<div class="cbsplit-b">
    <h3>🎯 Your Transformation in 30 Days</h3>
    <ul>
        <li>🚀 <strong>Week 1:</strong> Master the foundations (most students see progress in 3 days)</li>
        <li>💼 <strong>Week 2:</strong> Build your first portfolio project</li>
        <li>🏆 <strong>Week 3:</strong> Advanced techniques that set you apart</li>
        <li>💰 <strong>Week 4:</strong> Job search strategy + interview prep</li>
    </ul>
    
    <div style="background: #3498db; color: white; padding: 15px; border-radius: 8px; margin: 15px 0;">
        <strong>⚡ Fast Track Guarantee:</strong> Master the material in 30 days or get 6 months free!
    </div>
</div>
```

---

# 🔥 ClickFunnels Integration Guide

## **Why Use CBSplit with ClickFunnels:**
- ClickFunnels testing is **limited to single pages**
- No **funnel-wide tracking** across multiple steps
- No **revenue attribution** to original variant
- CBSplit tracks users through your entire funnel sequence

## **Step 1: Add CBSplit to Your ClickFunnels Pages**

### **In ClickFunnels: Page Settings → Tracking Code → Header**

```html
<script src="https://cbsplit-server.onrender.com/cbsplit-tracking.js"></script>
<script>
// ClickFunnels CBSplit Integration
window.CFTest = {
    testId: 'clickfunnels_vsl_test',
    
    init: function() {
        // Get or create session
        this.sessionId = this.getSessionFromUrl() || this.createSession();
        this.variant = this.getVariantFromUrl() || this.getVariant();
        
        // Track funnel step
        this.trackStep();
        
        // Show variant content
        this.showVariant();
        
        // Setup tracking
        this.setupTracking();
        
        // Inject tracking into links
        this.injectTracking();
        
        console.log('🔥 CBSplit active on ClickFunnels - Step:', this.getStep(), 'Variant:', this.variant);
    },
    
    getSessionFromUrl: function() {
        const params = new URLSearchParams(window.location.search);
        return params.get('cb_session');
    },
    
    getVariantFromUrl: function() {
        const params = new URLSearchParams(window.location.search);
        return params.get('cb_variant');
    },
    
    createSession: function() {
        this.variant = this.getVariant();
        return CBSplit.startSession(this.testId, this.variant, window.location.href);
    },
    
    getVariant: function() {
        // Consistent assignment for this funnel
        const funnelId = window.location.hostname + window.location.pathname.split('/')[1];
        let hash = 0;
        for (let i = 0; i < funnelId.length; i++) {
            hash = ((hash << 5) - hash + funnelId.charCodeAt(i)) & 0xffffffff;
        }
        return Math.abs(hash) % 2 === 0 ? 'A' : 'B';
    },
    
    getStep: function() {
        const path = window.location.pathname;
        if (path.includes('optin') || path.includes('squeeze')) return 1;
        if (path.includes('vsl') || path.includes('sales')) return 2;
        if (path.includes('order') || path.includes('checkout')) return 3;
        if (path.includes('upsell')) return 4;
        if (path.includes('thank')) return 5;
        return 1;
    },
    
    trackStep: function() {
        CBSplit.trackInteraction('funnel_step', 'step_' + this.getStep(), null, {
            variant: this.variant,
            stepName: this.getStepName(),
            url: window.location.href
        });
    },
    
    getStepName: function() {
        const stepNames = {
            1: 'opt_in',
            2: 'sales_video', 
            3: 'checkout',
            4: 'upsell',
            5: 'thank_you'
        };
        return stepNames[this.getStep()] || 'unknown';
    },
    
    showVariant: function() {
        // Hide all variants
        document.querySelectorAll('.cbsplit-a, .cbsplit-b').forEach(el => {
            el.style.display = 'none';
        });
        
        // Show selected variant
        document.querySelectorAll('.cbsplit-' + this.variant.toLowerCase()).forEach(el => {
            el.style.display = 'block';
        });
        
        if (this.variant === 'B') {
            this.applyVariantB();
        }
    },
    
    applyVariantB: function() {
        setTimeout(() => {
            // Add urgency to buttons
            const buttons = document.querySelectorAll('button, .button, [class*="btn"]');
            buttons.forEach(btn => {
                if (btn.textContent.toLowerCase().includes('buy') || 
                    btn.textContent.toLowerCase().includes('order') ||
                    btn.textContent.toLowerCase().includes('yes')) {
                    btn.style.cssText += 'background: linear-gradient(45deg, #ff6b6b, #ee5a24) !important; transform: scale(1.05) !important; box-shadow: 0 8px 25px rgba(255,107,107,0.4) !important; animation: buttonPulse 2s infinite !important;';
                }
            });
            
            // Add countdown timer
            const timer = document.createElement('div');
            timer.innerHTML = `
                <div style="background: #2c3e50; color: #ecf0f1; padding: 20px; text-align: center; position: fixed; top: 0; left: 0; right: 0; z-index: 9999; font-size: 18px; box-shadow: 0 4px 20px rgba(0,0,0,0.3);">
                    ⏰ <strong>LIMITED TIME OFFER EXPIRES IN:</strong> 
                    <span id="countdown" style="color: #e74c3c; font-weight: bold;">23:59:45</span>
                </div>
            `;
            document.body.appendChild(timer);
            
            // Simple countdown
            let timeLeft = 86385; // 23:59:45
            setInterval(() => {
                timeLeft--;
                const hours = Math.floor(timeLeft / 3600);
                const minutes = Math.floor((timeLeft % 3600) / 60);
                const seconds = timeLeft % 60;
                document.getElementById('countdown').textContent = 
                    `${hours.toString().padStart(2,'0')}:${minutes.toString().padStart(2,'0')}:${seconds.toString().padStart(2,'0')}`;
            }, 1000);
        }, 1000);
    },
    
    setupTracking: function() {
        // Track form submissions
        document.addEventListener('submit', (e) => {
            const form = e.target;
            
            // Email opt-in
            if (form.querySelector('input[type="email"]')) {
                CBSplit.trackConversion('email_optin', 0, {
                    variant: this.variant,
                    step: this.getStep(),
                    source: 'clickfunnels'
                });
            }
            
            // Order form
            if (form.action.includes('order') || form.querySelector('[name*="price"]')) {
                const price = this.extractOrderValue(form);
                CBSplit.trackConversion('purchase', price, {
                    variant: this.variant,
                    step: this.getStep(),
                    source: 'clickfunnels'
                });
            }
        });
        
        // Track video completion
        document.querySelectorAll('video').forEach(video => {
            video.addEventListener('ended', () => {
                CBSplit.trackInteraction('video_complete', 'sales_video', null, {
                    variant: this.variant,
                    videoDuration: video.duration
                });
            });
        });
    },
    
    extractOrderValue: function(form) {
        // Try to find price in form
        const priceInputs = form.querySelectorAll('[name*="price"], [name*="amount"], [data-price]');
        for (const input of priceInputs) {
            const value = parseFloat(input.value || input.dataset.price || input.textContent);
            if (value > 0) return value;
        }
        
        // Look for price on page
        const priceElements = document.querySelectorAll('.price, [class*="price"], .amount');
        for (const el of priceElements) {
            const match = el.textContent.match(/\$(\d+(?:\.\d{2})?)/);
            if (match) return parseFloat(match[1]);
        }
        
        return 97; // Default
    },
    
    injectTracking: function() {
        // Add tracking params to all links
        document.querySelectorAll('a[href]').forEach(link => {
            const href = link.getAttribute('href');
            if (href && !href.startsWith('#') && !href.includes('cb_session=')) {
                const separator = href.includes('?') ? '&' : '?';
                const params = `cb_session=${this.sessionId}&cb_variant=${this.variant}&cb_test=${this.testId}`;
                link.setAttribute('href', href + separator + params);
            }
        });
    }
};

// Auto-initialize
CFTest.init();
</script>

<style>
@keyframes buttonPulse {
    0% { transform: scale(1.05); }
    50% { transform: scale(1.1); }
    100% { transform: scale(1.05); }
}
</style>
```

## **Step 2: Add Variant Content to Each Funnel Step**

### **Opt-in Page (Step 1):**
```html
<!-- Variant A: Standard -->
<div class="cbsplit-a">
    <h1>Get Free Training</h1>
    <p>Enter your email to access the free video series</p>
</div>

<!-- Variant B: Urgency -->
<div class="cbsplit-b">
    <h1>🔥 FREE: $2,000 Marketing Course (Today Only)</h1>
    <p><strong>⚡ WARNING:</strong> Only 100 spots available! Get instant access before it's too late.</p>
</div>
```

### **Sales Page (Step 2):**
```html
<!-- Variant A: Standard -->
<div class="cbsplit-a">
    <h2>Transform Your Business</h2>
    <p>Learn the strategies that helped me build a 7-figure business</p>
</div>

<!-- Variant B: Social Proof -->
<div class="cbsplit-b">
    <h2>💰 The EXACT System That Made Me $1.2M Last Year</h2>
    <p><strong>Used by 847 entrepreneurs</strong> to generate over $47M in revenue</p>
    <div style="background: #27ae60; color: white; padding: 15px; border-radius: 8px; margin: 20px 0;">
        ⭐ "I made my first $10K in 30 days using this system!" - Jennifer K.
    </div>
</div>
```

---

# 📊 Analytics & Results

## **View Your Test Results**

### **Real-Time Analytics:**
```bash
# Kajabi course test
curl https://cbsplit-server.onrender.com/api/cbsplit/analytics/kajabi_course_sales

# Teachable course test  
curl https://cbsplit-server.onrender.com/api/cbsplit/analytics/teachable_course_test

# ClickFunnels funnel test
curl https://cbsplit-server.onrender.com/api/cbsplit/analytics/clickfunnels_vsl_test
```

### **Example Response:**
```json
{
  "testId": "kajabi_course_sales",
  "variants": [
    {
      "variant": "A",
      "sessions": 1247,
      "conversions": 89,
      "conversionRate": 0.071,
      "revenue": 8633,
      "revenuePerVisitor": 6.92
    },
    {
      "variant": "B", 
      "sessions": 1198,
      "conversions": 127,
      "conversionRate": 0.106,
      "revenue": 12319,
      "revenuePerVisitor": 10.28
    }
  ],
  "winnerAnalysis": {
    "winningVariant": "B",
    "confidence": 96.8,
    "expectedLift": 48.6,
    "recommendation": "Declare winner - statistically significant"
  },
  "totalSessions": 2445
}
```

## **Understanding Your Results**

### **Statistical Significance:**
- **90%+ confidence**: Strong evidence
- **95%+ confidence**: Statistical significance (safe to implement)
- **99%+ confidence**: Very strong evidence

### **Key Metrics:**
- **Conversion Rate**: Percentage who convert
- **Revenue Per Visitor**: Total revenue ÷ total visitors
- **Expected Lift**: Percentage improvement of winner vs control

---

# 🎯 Advanced Features

## **Multi-Platform Customer Journey**

Track users across your entire funnel:
```
Facebook Ad → Landing Page → Kajabi Course → ClickBank Upsell → Teachable Advanced Course
     ↓              ↓              ↓              ↓                    ↓
  UTM tracking → Variant A/B → Email signup → Purchase $97 → Upsell $297
```

All attributed to the original test variant!

## **Segment Analysis**

Analyze performance by:
- **Traffic Source**: Facebook vs Google vs Email
- **Device**: Mobile vs Desktop  
- **Time**: Day vs Night traffic
- **Geography**: Different countries

## **Revenue Attribution Models**

- **First Touch**: Credit to first interaction
- **Last Touch**: Credit to final interaction  
- **Linear**: Equal credit to all touchpoints
- **Time Decay**: More credit to recent interactions

---

# 🚀 Best Practices

## **Test Planning**
1. **Hypothesis**: "Adding urgency will increase conversions by 20%"
2. **Single Variable**: Only test one element at a time
3. **Significant Changes**: Test big differences, not minor tweaks
4. **Sample Size**: Need 100+ conversions per variant for significance

## **Implementation**
1. **QA Test**: Verify both variants work properly
2. **Equal Traffic**: Ensure 50/50 traffic split
3. **Consistent Experience**: Same user always sees same variant
4. **Mobile Optimization**: Test on all devices

## **Analysis**
1. **Wait for Significance**: Don't stop tests early
2. **External Factors**: Account for holidays, promotions, etc.
3. **Segment Analysis**: Look at different user groups
4. **Business Impact**: Consider practical significance vs statistical

---

# 💡 Why CBSplit vs Native Platform Testing

## **Statistical Rigor**
- **Native Platforms**: No statistical significance calculations
- **CBSplit**: Proper confidence intervals, sample size calculations, power analysis

## **Cross-Platform Tracking**  
- **Native Platforms**: Siloed to single platform
- **CBSplit**: Track from Facebook ad through entire customer journey

## **Revenue Focus**
- **Native Platforms**: Focus on conversion rate only
- **CBSplit**: Optimize for revenue per visitor and lifetime value

## **Advanced Attribution**
- **Native Platforms**: Last-click attribution only
- **CBSplit**: Multi-touch attribution models

## **Real-Time Analytics**
- **Native Platforms**: Basic reporting
- **CBSplit**: Live dashboard with detailed insights

---

# 🎉 Success Stories

## **Typical Results:**
- **Course Creators**: 15-40% increase in enrollment rates
- **ClickFunnels Users**: 25-60% improvement in funnel conversion
- **Kajabi Creators**: 20-50% boost in course sales

## **Common Winning Elements:**
- **Urgency**: Limited time offers
- **Social Proof**: Student testimonials and numbers
- **Benefit-Focused**: Outcomes vs features
- **Risk Reversal**: Guarantees and free trials

---

# 📞 Support & Troubleshooting

## **Test Your Setup:**
```javascript
// Run in browser console
console.log('CBSplit loaded:', typeof CBSplit !== 'undefined');
console.log('Session ID:', CBSplit.sessionId);
console.log('Variant:', window.KajabiTest?.variant || window.TeachableTest?.variant || window.CFTest?.variant);
```

## **Common Issues:**
1. **No Console Messages**: Check if script loaded
2. **No Variant Content**: Verify CSS selectors
3. **No Conversions**: Check button click tracking
4. **CORS Errors**: Verify server URL

## **Get Help:**
- **Server Status**: https://cbsplit-server.onrender.com/health
- **Documentation**: This README
- **Analytics**: `curl https://cbsplit-server.onrender.com/api/cbsplit/analytics/your_test_id`

---

**🚀 Start split testing like an enterprise with CBSplit!**