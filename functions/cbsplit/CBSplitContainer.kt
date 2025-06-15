package functions.cbsplit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.*

/**
 * CBSplit Container - Comprehensive CRO Platform Tool
 * 
 * A comprehensive conversion rate optimization platform with:
 * - A-Z split testing (up to 26 variants)
 * - CMS dashboard functionality
 * - Shopping cart integrations
 * - Pixel management capabilities
 * - Conversion tracking
 */
@Composable
fun CBSplitContainer(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var cbSplitState by remember { mutableStateOf(CBSplitState()) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        CBSplitHeader()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab Navigation
        CBSplitTabRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content based on selected tab
        when (selectedTab) {
            0 -> SplitTestingPanel(
                state = cbSplitState,
                onStateChange = { cbSplitState = it }
            )
            1 -> CMSDashboardPanel(
                state = cbSplitState,
                onStateChange = { cbSplitState = it }
            )
            2 -> ShoppingCartPanel(
                state = cbSplitState,
                onStateChange = { cbSplitState = it }
            )
            3 -> PixelManagementPanel(
                state = cbSplitState,
                onStateChange = { cbSplitState = it }
            )
            4 -> ConversionTrackingPanel(
                state = cbSplitState,
                onStateChange = { cbSplitState = it }
            )
        }
    }
}

@Composable
private fun CBSplitHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "CBSplit CRO Platform",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Comprehensive Conversion Rate Optimization Tool",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CBSplitTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "Split Testing",
        "CMS Dashboard", 
        "Shopping Cart",
        "Pixel Management",
        "Conversion Tracking"
    )
    
    TabRow(
        selectedTabIndex = selectedTab,
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) }
            )
        }
    }
}

@Composable
private fun SplitTestingPanel(
    state: CBSplitState,
    onStateChange: (CBSplitState) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SplitTestConfigCard(
                state = state,
                onStateChange = onStateChange
            )
        }
        
        item {
            VariantManagementCard(
                variants = state.variants,
                onVariantsChange = { variants ->
                    onStateChange(state.copy(variants = variants))
                }
            )
        }
        
        item {
            TestResultsCard(results = state.testResults)
        }
    }
}

@Composable
private fun SplitTestConfigCard(
    state: CBSplitState,
    onStateChange: (CBSplitState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Split Test Configuration",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = state.testName,
                onValueChange = { onStateChange(state.copy(testName = it)) },
                label = { Text("Test Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = state.testDescription,
                onValueChange = { onStateChange(state.copy(testDescription = it)) },
                label = { Text("Test Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { 
                        onStateChange(state.copy(isTestActive = !state.isTestActive))
                    }
                ) {
                    Text(if (state.isTestActive) "Stop Test" else "Start Test")
                }
                
                OutlinedButton(
                    onClick = { 
                        onStateChange(CBSplitState())
                    }
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun VariantManagementCard(
    variants: List<TestVariant>,
    onVariantsChange: (List<TestVariant>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Test Variants (${variants.size}/26)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        if (variants.size < 26) {
                            val newVariant = TestVariant(
                                id = "variant_${('A' + variants.size).toChar()}",
                                name = "Variant ${('A' + variants.size).toChar()}",
                                trafficAllocation = 100f / (variants.size + 1)
                            )
                            onVariantsChange(variants + newVariant)
                        }
                    },
                    enabled = variants.size < 26
                ) {
                    Text("Add Variant")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            variants.forEachIndexed { index, variant ->
                VariantConfigRow(
                    variant = variant,
                    onVariantChange = { updatedVariant ->
                        val updatedVariants = variants.toMutableList()
                        updatedVariants[index] = updatedVariant
                        onVariantsChange(updatedVariants)
                    },
                    onDelete = {
                        onVariantsChange(variants.filterIndexed { i, _ -> i != index })
                    }
                )
                
                if (index < variants.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun VariantConfigRow(
    variant: TestVariant,
    onVariantChange: (TestVariant) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = variant.name,
                    fontWeight = FontWeight.Medium
                )
                
                IconButton(onClick = onDelete) {
                    Text("✕", color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = variant.content,
                onValueChange = { onVariantChange(variant.copy(content = it)) },
                label = { Text("Content/URL") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Traffic Allocation: ${variant.trafficAllocation.toInt()}%")
            Slider(
                value = variant.trafficAllocation,
                onValueChange = { onVariantChange(variant.copy(trafficAllocation = it)) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TestResultsCard(results: TestResults) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Test Results",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultStatCard(
                    title = "Total Visitors",
                    value = results.totalVisitors.toString()
                )
                
                ResultStatCard(
                    title = "Conversions", 
                    value = results.totalConversions.toString()
                )
                
                ResultStatCard(
                    title = "Conversion Rate",
                    value = "${(results.conversionRate * 100).toInt()}%"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Statistical Significance: ${if (results.isSignificant) "Yes" else "No"}",
                fontWeight = FontWeight.Medium,
                color = if (results.isSignificant) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultStatCard(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CMSDashboardPanel(
    state: CBSplitState,
    onStateChange: (CBSplitState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ContentManagementCard(
            content = state.cmsContent,
            onContentChange = { content ->
                onStateChange(state.copy(cmsContent = content))
            }
        )
        
        PageTemplateCard(
            templates = state.pageTemplates,
            onTemplatesChange = { templates ->
                onStateChange(state.copy(pageTemplates = templates))
            }
        )
        
        MediaLibraryCard(
            mediaFiles = state.mediaFiles,
            onMediaFilesChange = { files ->
                onStateChange(state.copy(mediaFiles = files))
            }
        )
    }
}

@Composable
private fun ContentManagementCard(
    content: List<CMSContent>,
    onContentChange: (List<CMSContent>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Content Management",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        val newContent = CMSContent(
                            id = UUID.randomUUID().toString(),
                            title = "New Content",
                            type = "page",
                            content = ""
                        )
                        onContentChange(content + newContent)
                    }
                ) {
                    Text("Add Content")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content.forEach { item ->
                ContentItemRow(
                    content = item,
                    onContentChange = { updatedItem ->
                        val updatedContent = content.map { 
                            if (it.id == updatedItem.id) updatedItem else it 
                        }
                        onContentChange(updatedContent)
                    },
                    onDelete = {
                        onContentChange(content.filter { it.id != item.id })
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ContentItemRow(
    content: CMSContent,
    onContentChange: (CMSContent) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = content.title,
                    onValueChange = { onContentChange(content.copy(title = it)) },
                    label = { Text("Title") },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onDelete) {
                    Text("✕", color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = content.content,
                onValueChange = { onContentChange(content.copy(content = it)) },
                label = { Text("Content") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }
}

@Composable
private fun PageTemplateCard(
    templates: List<PageTemplate>,
    onTemplatesChange: (List<PageTemplate>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page Templates",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        val newTemplate = PageTemplate(
                            id = UUID.randomUUID().toString(),
                            name = "New Template",
                            htmlContent = "",
                            cssContent = ""
                        )
                        onTemplatesChange(templates + newTemplate)
                    }
                ) {
                    Text("Add Template")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            templates.forEach { template ->
                TemplateItemRow(
                    template = template,
                    onTemplateChange = { updatedTemplate ->
                        val updatedTemplates = templates.map { 
                            if (it.id == updatedTemplate.id) updatedTemplate else it 
                        }
                        onTemplatesChange(updatedTemplates)
                    },
                    onDelete = {
                        onTemplatesChange(templates.filter { it.id != template.id })
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TemplateItemRow(
    template: PageTemplate,
    onTemplateChange: (PageTemplate) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = template.name,
                    onValueChange = { onTemplateChange(template.copy(name = it)) },
                    label = { Text("Template Name") },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onDelete) {
                    Text("✕", color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = template.htmlContent,
                onValueChange = { onTemplateChange(template.copy(htmlContent = it)) },
                label = { Text("HTML Content") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = template.cssContent,
                onValueChange = { onTemplateChange(template.copy(cssContent = it)) },
                label = { Text("CSS Content") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }
}

@Composable
private fun MediaLibraryCard(
    mediaFiles: List<MediaFile>,
    onMediaFilesChange: (List<MediaFile>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Media Library",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        val newFile = MediaFile(
                            id = UUID.randomUUID().toString(),
                            name = "new-file.jpg",
                            url = "",
                            type = "image"
                        )
                        onMediaFilesChange(mediaFiles + newFile)
                    }
                ) {
                    Text("Upload Media")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            mediaFiles.forEach { file ->
                MediaFileRow(
                    file = file,
                    onFileChange = { updatedFile ->
                        val updatedFiles = mediaFiles.map { 
                            if (it.id == updatedFile.id) updatedFile else it 
                        }
                        onMediaFilesChange(updatedFiles)
                    },
                    onDelete = {
                        onMediaFilesChange(mediaFiles.filter { it.id != file.id })
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MediaFileRow(
    file: MediaFile,
    onFileChange: (MediaFile) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = file.type,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (file.url.isNotEmpty()) {
                    Text(
                        text = file.url,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Text("✕", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ShoppingCartPanel(
    state: CBSplitState,
    onStateChange: (CBSplitState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CartIntegrationCard(
            integrations = state.cartIntegrations,
            onIntegrationsChange = { integrations ->
                onStateChange(state.copy(cartIntegrations = integrations))
            }
        )
        
        CheckoutOptimizationCard(
            optimizations = state.checkoutOptimizations,
            onOptimizationsChange = { optimizations ->
                onStateChange(state.copy(checkoutOptimizations = optimizations))
            }
        )
        
        AbandonedCartCard(
            abandonedCarts = state.abandonedCarts,
            onAbandonedCartsChange = { carts ->
                onStateChange(state.copy(abandonedCarts = carts))
            }
        )
    }
}

@Composable
private fun CartIntegrationCard(
    integrations: List<CartIntegration>,
    onIntegrationsChange: (List<CartIntegration>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shopping Cart Integrations",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        val newIntegration = CartIntegration(
                            id = UUID.randomUUID().toString(),
                            platform = "Shopify",
                            apiKey = "",
                            webhookUrl = "",
                            isActive = false
                        )
                        onIntegrationsChange(integrations + newIntegration)
                    }
                ) {
                    Text("Add Integration")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            integrations.forEach { integration ->
                CartIntegrationRow(
                    integration = integration,
                    onIntegrationChange = { updatedIntegration ->
                        val updatedIntegrations = integrations.map { 
                            if (it.id == updatedIntegration.id) updatedIntegration else it 
                        }
                        onIntegrationsChange(updatedIntegrations)
                    },
                    onDelete = {
                        onIntegrationsChange(integrations.filter { it.id != integration.id })
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CartIntegrationRow(
    integration: CartIntegration,
    onIntegrationChange: (CartIntegration) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = integration.platform,
                    fontWeight = FontWeight.Medium
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = integration.isActive,
                        onCheckedChange = { 
                            onIntegrationChange(integration.copy(isActive = it))
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(onClick = onDelete) {
                        Text("✕", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = integration.apiKey,
                onValueChange = { onIntegrationChange(integration.copy(apiKey = it)) },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = integration.webhookUrl,
                onValueChange = { onIntegrationChange(integration.copy(webhookUrl = it)) },
                label = { Text("Webhook URL") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CheckoutOptimizationCard(
    optimizations: List<CheckoutOptimization>,
    onOptimizationsChange: (List<CheckoutOptimization>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Checkout Optimizations",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        val newOptimization = CheckoutOptimization(
                            id = UUID.randomUUID().toString(),
                            name = "New Optimization",
                            type = "form_fields",
                            configuration = "",
                            isEnabled = false
                        )
                        onOptimizationsChange(optimizations + newOptimization)
                    }
                ) {
                    Text("Add Optimization")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            optimizations.forEach { optimization ->
                CheckoutOptimizationRow(
                    optimization = optimization,
                    onOptimizationChange = { updatedOptimization ->
                        val updatedOptimizations = optimizations.map { 
                            if (it.id == updatedOptimization.id) updatedOptimization else it 
                        }
                        onOptimizationsChange(updatedOptimizations)
                    },
                    onDelete = {
                        onOptimizationsChange(optimizations.filter { it.id != optimization.id })
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CheckoutOptimizationRow(
    optimization: CheckoutOptimization,
    onOptimizationChange: (CheckoutOptimization) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = optimization.name,
                    onValueChange = { onOptimizationChange(optimization.copy(name = it)) },
                    label = { Text("Optimization Name") },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Switch(
                    checked = optimization.isEnabled,
                    onCheckedChange = { 
                        onOptimizationChange(optimization.copy(isEnabled = it))
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onDelete) {
                    Text("✕", color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = optimization.configuration,
                onValueChange = { onOptimizationChange(optimization.copy(configuration = it)) },
                label = { Text("Configuration") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }
}

@Composable
private fun AbandonedCartCard(
    abandonedCarts: List<AbandonedCart>,
    onAbandonedCartsChange: (List<AbandonedCart>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Abandoned Cart Recovery",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (abandonedCarts.isEmpty()) {
                Text(
                    text = "No abandoned carts found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                abandonedCarts.forEach { cart ->
                    AbandonedCartRow(cart = cart)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun AbandonedCartRow(cart: AbandonedCart) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = cart.customerEmail,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$${cart.cartValue}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Items: ${cart.itemCount}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Abandoned: ${cart.abandonedAt}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PixelManagementPanel(
    state: CBSplitState,
    onStateChange: (CBSplitState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TrackingPixelsCard(
            pixels = state.trackingPixels,
            onPixelsChange = { pixels ->
                onStateChange(state.copy(trackingPixels = pixels))
            }
        )
        
        PixelFireLogCard(
            fireLog = state.pixelFireLog,
            onFireLogChange = { log ->
                onStateChange(state.copy(pixelFireLog = log))
            }
        )
        
        CustomEventsCard(
            customEvents = state.customEvents,
            onCustomEventsChange = { events ->
                onStateChange(state.copy(customEvents = events))
            }
        )
    }
}

@Composable
private fun TrackingPixelsCard(
    pixels: List<TrackingPixel>,
    onPixelsChange: (List<TrackingPixel>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tracking Pixels",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        val newPixel = TrackingPixel(
                            id = UUID.randomUUID().toString(),
                            name = "New Pixel",
                            platform = "Facebook",
                            pixelId = "",
                            isActive = false
                        )
                        onPixelsChange(pixels + newPixel)
                    }
                ) {
                    Text("Add Pixel")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            pixels.forEach { pixel ->
                TrackingPixelRow(
                    pixel = pixel,
                    onPixelChange = { updatedPixel ->
                        val updatedPixels = pixels.map { 
                            if (it.id == updatedPixel.id) updatedPixel else it 
                        }
                        onPixelsChange(updatedPixels)
                    },
                    onDelete = {
                        onPixelsChange(pixels.filter { it.id != pixel.id })
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TrackingPixelRow(
    pixel: TrackingPixel,
    onPixelChange: (TrackingPixel) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = pixel.name,
                    onValueChange = { onPixelChange(pixel.copy(name = it)) },
                    label = { Text("Pixel Name") },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Switch(
                    checked = pixel.isActive,
                    onCheckedChange = { 
                        onPixelChange(pixel.copy(isActive = it))
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onDelete) {
                    Text("✕", color = MaterialTheme.colorScheme.error)
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = pixel.platform,
                    onValueChange = { onPixelChange(pixel.copy(platform = it)) },
                    label = { Text("Platform") },
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = pixel.pixelId,
                    onValueChange = { onPixelChange(pixel.copy(pixelId = it)) },
                    label = { Text("Pixel ID") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PixelFireLogCard(
    fireLog: List<PixelFireEvent>,
    onFireLogChange: (List<PixelFireEvent>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pixel Fire Log",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedButton(
                    onClick = { onFireLogChange(emptyList()) }
                ) {
                    Text("Clear Log")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (fireLog.isEmpty()) {
                Text(
                    text = "No pixel events recorded",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                fireLog.takeLast(10).forEach { event ->
                    PixelFireEventRow(event = event)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun PixelFireEventRow(event: PixelFireEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.pixelName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = event.eventType,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = event.timestamp,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomEventsCard(
    customEvents: List<CustomEvent>,
    onCustomEventsChange: (List<CustomEvent>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Events",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        val newEvent = CustomEvent(
                            id = UUID.randomUUID().toString(),
                            name = "custom_event",
                            description = "",
                            triggerCondition = "",
                            isEnabled = false
                        )
                        onCustomEventsChange(customEvents + newEvent)
                    }
                ) {
                    Text("Add Event")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            customEvents.forEach { event ->
                CustomEventRow(
                    event = event,
                    onEventChange = { updatedEvent ->
                        val updatedEvents = customEvents.map { 
                            if (it.id == updatedEvent.id) updatedEvent else it 
                        }
                        onCustomEventsChange(updatedEvents)
                    },
                    onDelete = {
                        onCustomEventsChange(customEvents.filter { it.id != event.id })
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CustomEventRow(
    event: CustomEvent,
    onEventChange: (CustomEvent) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = event.name,
                    onValueChange = { onEventChange(event.copy(name = it)) },
                    label = { Text("Event Name") },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Switch(
                    checked = event.isEnabled,
                    onCheckedChange = { 
                        onEventChange(event.copy(isEnabled = it))
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onDelete) {
                    Text("✕", color = MaterialTheme.colorScheme.error)
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = event.description,
                onValueChange = { onEventChange(event.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = event.triggerCondition,
                onValueChange = { onEventChange(event.copy(triggerCondition = it)) },
                label = { Text("Trigger Condition") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConversionTrackingPanel(
    state: CBSplitState,
    onStateChange: (CBSplitState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConversionGoalsCard(
            goals = state.conversionGoals,
            onGoalsChange = { goals ->
                onStateChange(state.copy(conversionGoals = goals))
            }
        )
        
        ConversionFunnelCard(
            funnel = state.conversionFunnel,
            onFunnelChange = { funnel ->
                onStateChange(state.copy(conversionFunnel = funnel))
            }
        )
        
        ConversionAnalyticsCard(
            analytics = state.conversionAnalytics
        )
    }
}

@Composable
private fun ConversionGoalsCard(
    goals: List<ConversionGoal>,
    onGoalsChange: (List<ConversionGoal>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conversion Goals",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        val newGoal = ConversionGoal(
                            id = UUID.randomUUID().toString(),
                            name = "New Goal",
                            type = "page_visit",
                            value = 0.0,
                            isActive = false
                        )
                        onGoalsChange(goals + newGoal)
                    }
                ) {
                    Text("Add Goal")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            goals.forEach { goal ->
                ConversionGoalRow(
                    goal = goal,
                    onGoalChange = { updatedGoal ->
                        val updatedGoals = goals.map { 
                            if (it.id == updatedGoal.id) updatedGoal else it 
                        }
                        onGoalsChange(updatedGoals)
                    },
                    onDelete = {
                        onGoalsChange(goals.filter { it.id != goal.id })
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ConversionGoalRow(
    goal: ConversionGoal,
    onGoalChange: (ConversionGoal) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = goal.name,
                    onValueChange = { onGoalChange(goal.copy(name = it)) },
                    label = { Text("Goal Name") },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Switch(
                    checked = goal.isActive,
                    onCheckedChange = { 
                        onGoalChange(goal.copy(isActive = it))
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onDelete) {
                    Text("✕", color = MaterialTheme.colorScheme.error)
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = goal.type,
                    onValueChange = { onGoalChange(goal.copy(type = it)) },
                    label = { Text("Type") },
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = goal.value.toString(),
                    onValueChange = { 
                        val value = it.toDoubleOrNull() ?: 0.0
                        onGoalChange(goal.copy(value = value))
                    },
                    label = { Text("Value") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConversionFunnelCard(
    funnel: ConversionFunnel,
    onFunnelChange: (ConversionFunnel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Conversion Funnel",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            funnel.steps.forEachIndexed { index, step ->
                FunnelStepRow(
                    step = step,
                    stepNumber = index + 1,
                    onStepChange = { updatedStep ->
                        val updatedSteps = funnel.steps.toMutableList()
                        updatedSteps[index] = updatedStep
                        onFunnelChange(funnel.copy(steps = updatedSteps))
                    }
                )
                
                if (index < funnel.steps.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val newStep = FunnelStep(
                        id = UUID.randomUUID().toString(),
                        name = "Step ${funnel.steps.size + 1}",
                        url = "",
                        visitors = 0,
                        conversions = 0
                    )
                    onFunnelChange(funnel.copy(steps = funnel.steps + newStep))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Funnel Step")
            }
        }
    }
}

@Composable
private fun FunnelStepRow(
    step: FunnelStep,
    stepNumber: Int,
    onStepChange: (FunnelStep) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Step $stepNumber",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = step.name,
                onValueChange = { onStepChange(step.copy(name = it)) },
                label = { Text("Step Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = step.url,
                onValueChange = { onStepChange(step.copy(url = it)) },
                label = { Text("URL Pattern") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = step.visitors.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Visitors",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = step.conversions.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Conversions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val conversionRate = if (step.visitors > 0) {
                        (step.conversions.toFloat() / step.visitors.toFloat() * 100).toInt()
                    } else 0
                    
                    Text(
                        text = "$conversionRate%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Rate",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversionAnalyticsCard(analytics: ConversionAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Conversion Analytics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnalyticsStatCard(
                    title = "Total Revenue",
                    value = "$${analytics.totalRevenue}",
                    color = MaterialTheme.colorScheme.primary
                )
                
                AnalyticsStatCard(
                    title = "Avg Order Value",
                    value = "$${analytics.averageOrderValue}",
                    color = MaterialTheme.colorScheme.secondary
                )
                
                AnalyticsStatCard(
                    title = "ROI",
                    value = "${analytics.roi}%",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnalyticsStatCard(
                    title = "Lifetime Value",
                    value = "$${analytics.customerLifetimeValue}",
                    color = MaterialTheme.colorScheme.primary
                )
                
                AnalyticsStatCard(
                    title = "Churn Rate",
                    value = "${analytics.churnRate}%",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(
    title: String,
    value: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Data Classes
data class CBSplitState(
    val testName: String = "",
    val testDescription: String = "",
    val isTestActive: Boolean = false,
    val variants: List<TestVariant> = listOf(
        TestVariant(id = "variant_A", name = "Variant A", trafficAllocation = 50f),
        TestVariant(id = "variant_B", name = "Variant B", trafficAllocation = 50f)
    ),
    val testResults: TestResults = TestResults(),
    val cmsContent: List<CMSContent> = emptyList(),
    val pageTemplates: List<PageTemplate> = emptyList(),
    val mediaFiles: List<MediaFile> = emptyList(),
    val cartIntegrations: List<CartIntegration> = emptyList(),
    val checkoutOptimizations: List<CheckoutOptimization> = emptyList(),
    val abandonedCarts: List<AbandonedCart> = emptyList(),
    val trackingPixels: List<TrackingPixel> = emptyList(),
    val pixelFireLog: List<PixelFireEvent> = emptyList(),
    val customEvents: List<CustomEvent> = emptyList(),
    val conversionGoals: List<ConversionGoal> = emptyList(),
    val conversionFunnel: ConversionFunnel = ConversionFunnel(),
    val conversionAnalytics: ConversionAnalytics = ConversionAnalytics()
)

data class TestVariant(
    val id: String,
    val name: String,
    val content: String = "",
    val trafficAllocation: Float
)

data class TestResults(
    val totalVisitors: Int = 0,
    val totalConversions: Int = 0,
    val conversionRate: Float = 0f,
    val isSignificant: Boolean = false
)

data class CMSContent(
    val id: String,
    val title: String,
    val type: String,
    val content: String,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class PageTemplate(
    val id: String,
    val name: String,
    val htmlContent: String,
    val cssContent: String,
    val createdAt: String = ""
)

data class MediaFile(
    val id: String,
    val name: String,
    val url: String,
    val type: String,
    val size: Long = 0L,
    val uploadedAt: String = ""
)

data class CartIntegration(
    val id: String,
    val platform: String,
    val apiKey: String,
    val webhookUrl: String,
    val isActive: Boolean
)

data class CheckoutOptimization(
    val id: String,
    val name: String,
    val type: String,
    val configuration: String,
    val isEnabled: Boolean
)

data class AbandonedCart(
    val id: String,
    val customerEmail: String,
    val cartValue: Double,
    val itemCount: Int,
    val abandonedAt: String
)

data class TrackingPixel(
    val id: String,
    val name: String,
    val platform: String,
    val pixelId: String,
    val isActive: Boolean
)

data class PixelFireEvent(
    val pixelName: String,
    val eventType: String,
    val timestamp: String
)

data class CustomEvent(
    val id: String,
    val name: String,
    val description: String,
    val triggerCondition: String,
    val isEnabled: Boolean
)

data class ConversionGoal(
    val id: String,
    val name: String,
    val type: String,
    val value: Double,
    val isActive: Boolean
)

data class ConversionFunnel(
    val steps: List<FunnelStep> = emptyList()
)

data class FunnelStep(
    val id: String,
    val name: String,
    val url: String,
    val visitors: Int,
    val conversions: Int
)

data class ConversionAnalytics(
    val totalRevenue: Double = 0.0,
    val averageOrderValue: Double = 0.0,
    val customerLifetimeValue: Double = 0.0,
    val roi: Double = 0.0,
    val churnRate: Double = 0.0
)