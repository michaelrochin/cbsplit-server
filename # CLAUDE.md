# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Important
- ALL instructions within this document MUST BE FOLLOWED, these are not optional unless explicitly stated.
- DO NOT edit more code than you have to.
- DO NOT WASTE TOKENS, be succinct and concise.

## CBSplit System Guidelines
This document provides specific guidance for creating and maintaining conversion rate optimization functionality in this comprehensive marketing platform.

### CBSplit System Overview

CBSplit is a comprehensive conversion rate optimization platform and lightweight CMS designed for internet marketers. It provides ISOLATED CRO units including split testing, page management, shopping cart integrations, and marketing automation which can be configured by users and adjusted through various optimization parameters.

### Feature Creation Priority Rules
- IMMEDIATE EXECUTION: Launch ultra-fast feature creation immediately upon request
- NO CLARIFICATION: Skip asking what type of feature unless absolutely critical
- ASSUME DEFAULTS: Multi-variant features unless explicitly specified as single variant
- ULTRA-FAST BY DEFAULT: Always use 9-parallel-Task method for efficiency

### When to Create CBSplit Features
- IMPERATIVE: When user requests feature creation, IMMEDIATELY launch ultra-fast creation without hesitation.
- Valid requests include "create split test", "build landing page", "setup integration", "add pixel manager", "create funnel", "setup redirect", or similar direct phrasing.
- Do NOT assume a feature should be created based on analytics commands or other requests.
- Skip clarification questions and proceed directly to ultra-fast implementation.

### Ultra-Fast CBSplit Feature Creation Workflow
**IMMEDIATE EXECUTION:** Upon ANY feature creation request, instantly launch all 9 Tasks in parallel:
1. **Container**: Create FeatureNameContainer.kt
2. **Broadcast**: Create FeatureNameBroadcast.kt  
3. **Integration**: Create FeatureNameIntegration.kt
4. **Analytics**: Create FeatureNameAnalytics.kt
5. **FeatureMap**: Update featureNameMap.kt
6. **LoadFeatures**: Update loadFeatures.kt
7. **IntegrationMap**: Update featureIntegrationMap.kt
8. **Execute**: Update executeFeatureCommand.kt
9. **Remaining**: Update featureStateResetData.kt, getConversionKeyTermUrl.kt, getCROSystemMessage.kt

### Token Optimization during CBSplit Feature Design
- Strip out all comments (including block comments, inline comments, and KDoc/Javadoc) when reading code files
- Filter out all logging statements and debug information when analyzing code
- In particularly large files, ignore formatting whitespace when analyzing structure

### CBSplit Feature Design Guidelines
- Create features in the `functions/cbsplit` directory
- Multi-variant features (default): Base on `CBSplitMultiContainer.kt` (no gradient background)
- Single-variant features (rare): Base on `CBSplitSingleContainer.kt` (gradient background)
- **CRITICAL**: Make MINIMAL CHANGES to template structure and indentation patterns
- **CRITICAL**: Preserve existing patterns, function signatures, and component structures
- **CRITICAL**: DO NOT rewrite or restructure code unnecessarily
- Visualization: Show conversion metrics, A-Z testing data (up to 26 variants), CMS dashboard elements, integration status, and revenue optimization data
- Fixed measure policy: `true` for numerical data ('01', '26', '15.3%', '$249', 'A1'), `false` for text data ('variant', 'clickbank', 'autopilot', 'mobile')
- Support major integrations: ClickBank, Sticky.io, InfusionSoft, DigiStore24, BuyGoods, ClickFunnels, UltraCart, Konnektive, LimeLight CRM, HasOffers
- Do not add parameters to functions
- Do not modify aspects of other features

### Core CBSplit CRO Features to Support
- **A-Z Split Testing**: Up to 26 concurrent variations with 100% accuracy
- **CMS Dashboard**: Page creation, layouts, snippets, assets with version control
- **Shopping Cart Integrations**: ClickBank API, Sticky.io, InfusionSoft, etc.
- **Pixel Management**: Facebook Pixel, Google Tag Manager, affiliate pixel deployment
- **GEO Redirection**: Foreign traffic handling and revenue optimization
- **SEO Tools**: Meta management, sitemap generation, URL slugs
- **User Management**: Admin privilege levels, action logging
- **API Backend**: Read/write conversion datapoints, webhooks, INS relays
- **Autopilot Testing**: Auto-complete tests and deploy winning variations
- **List Building**: Third-party mailer integrations (Maropost, InfusionSoft)
- **Mobile Optimization**: Separate mobile layouts and responsive design
- **Fraud Protection**: Traffic filtering, banned affiliates, verification
- **Revenue Tracking**: Conversion recording, campaign parameter tracking
- **Redirect Management**: 301 redirects, migration assistance
- **Order Forms**: Custom checkout integration
- **Quizzes**: Interactive conversion elements

### CBSplit Admin Module Structure
- **Content**: Assets, Layouts, Pages, Menus, Snippets, Tests, Redirects, Quizzes, Order Forms
- **ClickBank**: Accounts, Products API integration
- **Sticky.io**: Campaigns, Funnels, Products, Bundles
- **Addons**: API Access, Webhooks, Contact Forms, UltraCart, Fraud Filters, xVerify, Maropost, InfusionSoft, LimeLight, Konnektive
- **Facebook**: Pixel management
- **Logs**: Action logging, notifications
- **Admin**: User management, options, help

### Permitted Reference Files for CBSplit Integration and Design
DO NOT READ ANY OTHER FILES & TIGHTLY FOLLOW THE CBSPLIT API ARCHITECTURE:
- `adjustCurrentFeature()` in `adjustCurrentFeature.kt`
- `LoadFeatures()` in `loadFeatures.kt`
- `featureStateResetData` list in `featureStateResetData.kt`
- `featureNameMap()` in `featureNameMap.kt`
- `getConversionKeyTermUrl()` in `getConversionKeyTermUrl.kt`
- `getCROSystemMessage()` in `getCROSystemMessage.kt`

### Additional Conditionally Permitted Reference Files for CBSplit Integration and Design
IMPORTANT ONLY READ ONE SET, DO NOT READ BOTH SETS:
- Single variant files: (`CBSplitSingleContainer.kt`, `CBSplitSingleBroadcast.kt`, `CBSplitSingleIntegration.kt`)
- Multi variant files: (`CBSplitMultiContainer.kt`, `CBSplitMultiBroadcast.kt`, `CBSplitMultiIntegration.kt`)

### DO NOT Read the following files under any circumstances UNLESS EXPLICITLY asked to do so:
- `singleVariantBroadcast.kt`
- `multipleVariantBroadcast.kt`
- `legacyIntegrations.kt`
- `wordpressMigration.kt`

### Ultra-Fast Execution Rules
**IMPERATIVE: ALL CBSplit feature creation uses ultra-fast method by default**

#### Critical Rules:
- IMMEDIATE START: No analysis, questions, or explanations before launching tasks
- Launch ALL 9 tasks in single message
- Each task handles ONLY specified files
- Use Multi templates for Multiple Broadcast, Single for Single Broadcast
- Task 9 combines 3 small config files to prevent over-splitting
- **EFFICIENCY OPTIMIZATION**: Use MultiEdit tool when making multiple edits to the same file within Tasks
- **ACCURACY PRIORITY**: Ensure 100% data accuracy for split testing and conversion tracking
- **INTEGRATION FOCUS**: Prioritize ClickBank and major shopping cart compatibility
- **MARKETING FOCUS**: Optimize for VSL testing, landing page optimization, and funnel revenue maximization
- **LIGHTWEIGHT PERFORMANCE**: Maintain fast loading times as WordPress alternative