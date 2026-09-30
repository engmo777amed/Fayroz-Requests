package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.requests.data.model.RequestSheetSummary
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.launch

private enum class SheetPricingFilter(val label: String) {
    ALL("كل الحالات"),
    UNPRICED("غير مسعّر"),
    STARTED("به تسعير"),
    COMPLETE("تسعير كامل"),
}

@Composable
fun RequestSheetsScreen(
    repository: FayrozRepository,
    onNewSheet: () -> Unit,
    onEditSheet: (Long) -> Unit,
    onPriceSheet: (Long) -> Unit,
) {
    val sheets by repository.sheetSummaries.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<RequestSheetSummary?>(null) }
    var projectFilterId by remember { mutableStateOf<Long?>(null) }
    var pricingFilter by remember { mutableStateOf(SheetPricingFilter.ALL) }
    var recentDays by remember { mutableStateOf<Int?>(null) }
    var projectMenu by remember { mutableStateOf(false) }

    val projects = remember(sheets) {
        sheets.distinctBy { it.projectId }
            .map { it.projectId to it.projectName }
            .sortedBy { it.second }
    }
    val selectedProjectName = projects.firstOrNull { it.first == projectFilterId }?.second ?: "كل المشروعات"
    val filtered = remember(sheets, query, projectFilterId, pricingFilter, recentDays) {
        val now = System.currentTimeMillis()
        val cutoff = recentDays?.let { now - it * 24L * 60L * 60L * 1000L }
        sheets.filter { sheet ->
            val searchMatches = query.isBlank() ||
                sheet.projectName.contains(query, true) ||
                sheet.sheetNumber.contains(query, true) ||
                sheet.trade.contains(query, true) ||
                sheet.craftsmanName.contains(query, true) ||
                sheet.workLocation.contains(query, true)
            val projectMatches = projectFilterId == null || sheet.projectId == projectFilterId
            val pricingMatches = when (pricingFilter) {
                SheetPricingFilter.ALL -> true
                SheetPricingFilter.UNPRICED -> !sheet.hasPricing
                SheetPricingFilter.STARTED -> sheet.hasPricing
                SheetPricingFilter.COMPLETE -> sheet.hasCompletePricing
            }
            val dateMatches = cutoff == null || sheet.sheetDate >= cutoff
            searchMatches && projectMatches && pricingMatches && dateMatches
        }
    }

    Column(
        Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("كشوف الطلبات", "المشروع ← الكشف ← البنود والكميات")
        PrimaryAction("كشف طلبات جديد", Icons.Outlined.AddCircle, onNewSheet)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("بحث في الكشوف") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
        )

        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { projectMenu = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Business, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(selectedProjectName, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Outlined.KeyboardArrowDown, null)
            }
            DropdownMenu(
                expanded = projectMenu,
                onDismissRequest = { projectMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("كل المشروعات") },
                    onClick = {
                        projectFilterId = null
                        projectMenu = false
                    },
                )
                projects.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            projectFilterId = id
                            projectMenu = false
                        },
                    )
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(SheetPricingFilter.entries) { filter ->
                FilterChip(
                    selected = pricingFilter == filter,
                    onClick = { pricingFilter = filter },
                    label = { Text(filter.label) },
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = recentDays == null,
                    onClick = { recentDays = null },
                    label = { Text("كل التواريخ") },
                )
            }
            item {
                FilterChip(
                    selected = recentDays == 7,
                    onClick = { recentDays = 7 },
                    label = { Text("آخر 7 أيام") },
                )
            }
            item {
                FilterChip(
                    selected = recentDays == 30,
                    onClick = { recentDays = 30 },
                    label = { Text("آخر 30 يوم") },
                )
            }
        }

        Text(
            "النتائج: ${filtered.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (filtered.isEmpty()) {
            EmptyState(
                Icons.Outlined.ReceiptLong,
                "لا توجد كشوف مطابقة",
                "غيّر البحث أو الفلاتر، أو أنشئ كشف طلبات جديد.",
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(bottom = 18.dp),
            ) {
                items(filtered, key = { it.id }) { sheet ->
                    Card(onClick = { onEditSheet(sheet.id) }) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "كشف ${sheet.sheetNumber} • ${sheet.projectName}",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    val info = buildList {
                                        if (sheet.workLocation.isNotBlank()) add("المكان: ${sheet.workLocation}")
                                        if (sheet.craftsmanName.isNotBlank()) add("الصنايعي: ${sheet.craftsmanName}")
                                        add("${sheet.lineCount} بند")
                                    }
                                    Text(
                                        info.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                                SuggestionChip(
                                    onClick = { onPriceSheet(sheet.id) },
                                    label = {
                                        Text(
                                            when {
                                                sheet.hasCompletePricing -> "تسعير كامل"
                                                sheet.hasPricing -> "غير مكتمل"
                                                else -> "غير مسعّر"
                                            }
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            if (sheet.hasCompletePricing) Icons.Outlined.CheckCircle
                                            else Icons.Outlined.PriceCheck,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = { onPriceSheet(sheet.id) },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(horizontal = 9.dp),
                                ) {
                                    Icon(Icons.Outlined.PriceCheck, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("تسعير الكشف")
                                }

                                OutlinedButton(
                                    onClick = { onEditSheet(sheet.id) },
                                    modifier = Modifier.weight(0.68f).height(40.dp),
                                    contentPadding = PaddingValues(horizontal = 7.dp),
                                ) {
                                    Icon(Icons.Outlined.EditNote, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تعديل")
                                }

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val newId = repository.duplicateSheet(sheet.id)
                                            onEditSheet(newId)
                                        }
                                    },
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, "نسخ الكشف")
                                }
                                IconButton(
                                    onClick = { deleteTarget = sheet },
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    Icon(Icons.Outlined.DeleteOutline, "حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { sheet ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف كشف ${sheet.sheetNumber}؟") },
            text = { Text("سيتم حذف الكشف وبنوده ونسخ تسعيره فقط، ولن تُحذف الأصناف من دليل الأصناف.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteSheet(sheet.id) }
                    deleteTarget = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("إلغاء") } },
        )
    }
}
