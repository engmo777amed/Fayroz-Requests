package com.fayroz.requests.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fayroz.requests.data.importer.*
import com.fayroz.requests.data.model.ItemEntity
import com.fayroz.requests.data.model.PriceListEntity
import com.fayroz.requests.data.model.SupplierEntity
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

enum class ImportStep { PICK_FILE, MAP_COLUMNS, REVIEW, DONE }

private data class ImportResolution(
    val itemId: Long? = null,
    val itemName: String? = null,
    val createNew: Boolean = false,
    val skip: Boolean = false,
) {
    val resolved: Boolean get() = itemId != null || createNew || skip
}

@Composable
fun PriceListImportScreen(
    repository: FayrozRepository,
    priceListId: Long,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allItems by repository.items.collectAsState(initial = emptyList())
    val priceList by produceState<PriceListEntity?>(initialValue = null, priceListId) {
        value = repository.getPriceList(priceListId)
    }
    val supplier by produceState<SupplierEntity?>(initialValue = null, priceList?.supplierId) {
        value = priceList?.let { repository.getSupplier(it.supplierId) }
    }

    var step by remember { mutableStateOf(ImportStep.PICK_FILE) }
    var table by remember { mutableStateOf<TabularPriceData?>(null) }
    var mapping by remember { mutableStateOf<ImportColumnMapping?>(null) }
    var candidates by remember { mutableStateOf<List<PriceImportCandidate>>(emptyList()) }
    var resolutions by remember { mutableStateOf<Map<Int, ImportResolution>>(emptyMap()) }
    var summary by remember { mutableStateOf<PriceImportSummary?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var itemPickerRow by remember { mutableStateOf<Int?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        loading = true
        error = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val fileName = resolveFileName(context, uri)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        PriceListFileParser.parse(fileName, input)
                    } ?: error("تعذر فتح الملف المختار.")
                }
            }.onSuccess { parsed ->
                table = parsed
                mapping = PriceListImportEngine.detectMapping(parsed.headers)
                step = ImportStep.MAP_COLUMNS
            }.onFailure { ex ->
                error = ex.message ?: "تعذر قراءة الملف."
            }
            loading = false
        }
    }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            ScreenHeader(
                title = "استيراد قائمة أسعار",
                subtitle = supplier?.let { "${it.name} • ${priceList?.name.orEmpty()}" },
            )
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "رجوع") }
        }

        ImportProgress(step)

        error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                }
            }
        }

        when (step) {
            ImportStep.PICK_FILE -> PickFileStep(
                loading = loading,
                onPick = { picker.launch(arrayOf("text/csv", "text/plain", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) },
            )

            ImportStep.MAP_COLUMNS -> {
                val data = table
                val current = mapping
                if (data != null && current != null) {
                    ColumnMappingStep(
                        data = data,
                        mapping = current,
                        onMappingChange = { mapping = it },
                        loading = loading,
                        onBack = { step = ImportStep.PICK_FILE },
                        onContinue = {
                            loading = true
                            error = null
                            scope.launch {
                                runCatching { repository.buildPriceImportCandidates(data, current) }
                                    .onSuccess { built ->
                                        candidates = built
                                        resolutions = built.associate { candidate ->
                                            candidate.sourceRowNumber to when (candidate.matchKind) {
                                                ImportMatchKind.EXACT_CODE, ImportMatchKind.EXACT_NAME -> ImportResolution(
                                                    itemId = candidate.matchedItemId,
                                                    itemName = candidate.matchedItemName,
                                                )
                                                ImportMatchKind.INVALID -> ImportResolution(skip = true)
                                                else -> ImportResolution()
                                            }
                                        }
                                        step = ImportStep.REVIEW
                                    }
                                    .onFailure { ex -> error = ex.message ?: "تعذر تجهيز المطابقة." }
                                loading = false
                            }
                        },
                    )
                }
            }

            ImportStep.REVIEW -> ImportReviewStep(
                candidates = candidates,
                resolutions = resolutions,
                loading = loading,
                onAcceptSuggestion = { candidate ->
                    candidate.suggestedItemId?.let { id ->
                        resolutions = resolutions + (candidate.sourceRowNumber to ImportResolution(id, candidate.suggestedItemName))
                    }
                },
                onPickExisting = { candidate -> itemPickerRow = candidate.sourceRowNumber },
                onCreateNew = { candidate ->
                    resolutions = resolutions + (candidate.sourceRowNumber to ImportResolution(createNew = true, itemName = candidate.itemName))
                },
                onSkip = { candidate ->
                    resolutions = resolutions + (candidate.sourceRowNumber to ImportResolution(skip = true))
                },
                onReset = { candidate ->
                    resolutions = resolutions + (candidate.sourceRowNumber to ImportResolution())
                },
                onBack = { step = ImportStep.MAP_COLUMNS },
                onImport = {
                    val unresolved = candidates.filter { it.matchKind != ImportMatchKind.INVALID }
                        .count { resolutions[it.sourceRowNumber]?.resolved != true }
                    if (unresolved > 0) {
                        error = "راجع الصفوف غير المحسومة أولًا: $unresolved صف."
                    } else {
                        val resolvedRows = candidates.mapNotNull { candidate ->
                            val resolution = resolutions[candidate.sourceRowNumber] ?: return@mapNotNull null
                            if (candidate.matchKind == ImportMatchKind.INVALID || resolution.skip) return@mapNotNull null
                            ResolvedPriceImportRow(
                                sourceRowNumber = candidate.sourceRowNumber,
                                itemName = candidate.itemName,
                                itemCode = candidate.itemCode,
                                unit = candidate.unit,
                                brand = candidate.brand,
                                specification = candidate.specification,
                                listPrice = candidate.listPrice,
                                discountPercent = candidate.discountPercent,
                                existingItemId = resolution.itemId,
                                createNewItem = resolution.createNew,
                            )
                        }
                        val skipped = candidates.size - resolvedRows.size
                        loading = true
                        error = null
                        scope.launch {
                            runCatching {
                                repository.importPriceListRows(
                                    priceListId = priceListId,
                                    rows = resolvedRows,
                                    skippedRows = skipped,
                                    sourceReference = table?.sourceFileName.orEmpty(),
                                )
                            }
                                .onSuccess {
                                    summary = it
                                    step = ImportStep.DONE
                                }
                                .onFailure { ex -> error = ex.message ?: "فشل حفظ القائمة المستوردة." }
                            loading = false
                        }
                    }
                },
            )

            ImportStep.DONE -> ImportDoneStep(summary = summary, onDone = onDone)
        }
    }

    itemPickerRow?.let { rowNumber ->
        ImportItemPickerDialog(
            items = allItems,
            onDismiss = { itemPickerRow = null },
            onSelect = { item ->
                resolutions = resolutions + (rowNumber to ImportResolution(itemId = item.id, itemName = item.name))
                itemPickerRow = null
            },
        )
    }
}

@Composable
private fun ImportProgress(step: ImportStep) {
    val labels = listOf("الملف", "الأعمدة", "المراجعة", "تم")
    val active = step.ordinal
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label ->
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(label) },
                leadingIcon = if (index <= active) {{ Icon(Icons.Outlined.CheckCircle, null, Modifier.size(17.dp)) }} else null,
                modifier = Modifier.weight(1f),
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = if (index == active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    disabledLabelColor = if (index == active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconContentColor = if (index <= active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun PickFileStep(loading: Boolean, onPick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.UploadFile, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
            Text("اختر ليستة المورد", style = MaterialTheme.typography.titleLarge)
            Text(
                "يدعم XLSX وCSV. أول صف في الملف يُعتبر أسماء الأعمدة، وبعد الاختيار ستراجع ربط الأعمدة ومطابقة الأصناف قبل الحفظ.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onPick, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.FolderOpen, null)
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "جارٍ قراءة الملف…" else "اختيار ملف")
            }
            Text("ملفات XLS القديمة: احفظها XLSX أو CSV أولًا.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ColumnMappingStep(
    data: TabularPriceData,
    mapping: ImportColumnMapping,
    onMappingChange: (ImportColumnMapping) -> Unit,
    loading: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val headers = data.headers
    val valid = mapping.itemNameColumn in headers.indices && mapping.listPriceColumn in headers.indices && mapping.itemNameColumn != mapping.listPriceColumn
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(data.sourceFileName, style = MaterialTheme.typography.titleMedium)
                    Text("${data.rows.size} صف • ${headers.size} عمود • صف العناوين ${data.headerRowNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("راجع الربط التلقائي قبل المتابعة.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { ColumnPicker("اسم الصنف *", headers, mapping.itemNameColumn, allowNone = false) { onMappingChange(mapping.copy(itemNameColumn = it ?: mapping.itemNameColumn)) } }
        item { ColumnPicker("سعر الليستة *", headers, mapping.listPriceColumn, allowNone = false) { onMappingChange(mapping.copy(listPriceColumn = it ?: mapping.listPriceColumn)) } }
        item { ColumnPicker("كود الصنف", headers, mapping.codeColumn) { onMappingChange(mapping.copy(codeColumn = it)) } }
        item { ColumnPicker("الوحدة", headers, mapping.unitColumn) { onMappingChange(mapping.copy(unitColumn = it)) } }
        item { ColumnPicker("الخصم %", headers, mapping.discountColumn) { onMappingChange(mapping.copy(discountColumn = it)) } }
        item { ColumnPicker("الماركة", headers, mapping.brandColumn) { onMappingChange(mapping.copy(brandColumn = it)) } }
        item { ColumnPicker("المواصفة", headers, mapping.specificationColumn) { onMappingChange(mapping.copy(specificationColumn = it)) } }
        item {
            if (!valid) Text("اسم الصنف والسعر يجب أن يكونا عمودين مختلفين.", color = MaterialTheme.colorScheme.error)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("رجوع") }
                Button(onClick = onContinue, enabled = valid && !loading, modifier = Modifier.weight(2f)) {
                    Text(if (loading) "جارٍ المطابقة…" else "مطابقة الأصناف")
                }
            }
        }
    }
}

@Composable
private fun ColumnPicker(
    label: String,
    headers: List<String>,
    selected: Int?,
    allowNone: Boolean = true,
    onSelected: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.let { headers.getOrNull(it) } ?: "— غير مستخدم —", modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowNone) DropdownMenuItem(text = { Text("— غير مستخدم —") }, onClick = { onSelected(null); expanded = false })
            headers.forEachIndexed { index, header ->
                DropdownMenuItem(
                    text = { Text(header) },
                    leadingIcon = if (selected == index) {{ Icon(Icons.Outlined.Check, null) }} else null,
                    onClick = { onSelected(index); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ImportReviewStep(
    candidates: List<PriceImportCandidate>,
    resolutions: Map<Int, ImportResolution>,
    loading: Boolean,
    onAcceptSuggestion: (PriceImportCandidate) -> Unit,
    onPickExisting: (PriceImportCandidate) -> Unit,
    onCreateNew: (PriceImportCandidate) -> Unit,
    onSkip: (PriceImportCandidate) -> Unit,
    onReset: (PriceImportCandidate) -> Unit,
    onBack: () -> Unit,
    onImport: () -> Unit,
) {
    val invalid = candidates.count { it.matchKind == ImportMatchKind.INVALID }
    val unresolved = candidates.filter { it.matchKind != ImportMatchKind.INVALID }
        .count { resolutions[it.sourceRowNumber]?.resolved != true }
    val skipped = candidates.count { candidate ->
        candidate.matchKind == ImportMatchKind.INVALID || resolutions[candidate.sourceRowNumber]?.skip == true
    }
    val ready = candidates.count { candidate ->
        candidate.matchKind != ImportMatchKind.INVALID &&
            resolutions[candidate.sourceRowNumber]?.resolved == true &&
            resolutions[candidate.sourceRowNumber]?.skip != true
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                ReviewCount("جاهز", ready.toString())
                ReviewCount("يحتاج مراجعة", unresolved.toString(), highlight = unresolved > 0)
                ReviewCount("متخطى/غير صالح", skipped.toString())
            }
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(candidates, key = { it.sourceRowNumber }) { candidate ->
                val resolution = resolutions[candidate.sourceRowNumber] ?: ImportResolution()
                ImportCandidateCard(
                    candidate = candidate,
                    resolution = resolution,
                    onAcceptSuggestion = { onAcceptSuggestion(candidate) },
                    onPickExisting = { onPickExisting(candidate) },
                    onCreateNew = { onCreateNew(candidate) },
                    onSkip = { onSkip(candidate) },
                    onReset = { onReset(candidate) },
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, enabled = !loading, modifier = Modifier.weight(1f)) { Text("الأعمدة") }
            Button(onClick = onImport, enabled = unresolved == 0 && !loading, modifier = Modifier.weight(2f)) {
                Icon(Icons.Outlined.FileDownloadDone, null)
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "جارٍ الحفظ…" else "استيراد $ready سعر")
            }
        }
    }
}

@Composable
private fun ReviewCount(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ImportCandidateCard(
    candidate: PriceImportCandidate,
    resolution: ImportResolution,
    onAcceptSuggestion: () -> Unit,
    onPickExisting: () -> Unit,
    onCreateNew: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
) {
    val resolvedName = when {
        resolution.skip -> "سيتم تخطي الصف"
        resolution.createNew -> "سيتم إنشاء صنف جديد"
        resolution.itemName != null -> "مرتبط بـ ${resolution.itemName}"
        else -> null
    }
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(candidate.itemName.ifBlank { "صف ${candidate.sourceRowNumber}" }, style = MaterialTheme.typography.titleMedium)
                    val meta = listOf(candidate.itemCode, candidate.unit).filter { it.isNotBlank() }.joinToString(" • ")
                    if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(importMoney(candidate.listPrice), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            }

            if (candidate.discountPercent != null) {
                Text("خصم الملف: ${formatImportDiscount(candidate.discountPercent)}", style = MaterialTheme.typography.bodySmall)
            }

            when (candidate.matchKind) {
                ImportMatchKind.EXACT_CODE -> MatchLabel(Icons.Outlined.Link, "مطابق بالكود: ${candidate.matchedItemName}")
                ImportMatchKind.EXACT_NAME -> MatchLabel(Icons.Outlined.Link, "مطابق بالاسم: ${candidate.matchedItemName}")
                ImportMatchKind.SUGGESTED -> if (!resolution.resolved) {
                    MatchLabel(Icons.Outlined.AutoAwesome, "اقتراح: ${candidate.suggestedItemName} (${(candidate.confidence * 100).toInt()}%)")
                }
                ImportMatchKind.UNMATCHED -> if (!resolution.resolved) MatchLabel(Icons.Outlined.HelpOutline, "لم يتم العثور على تطابق واضح")
                ImportMatchKind.INVALID -> MatchLabel(Icons.Outlined.ErrorOutline, candidate.validationMessage, error = true)
            }

            if (resolvedName != null && candidate.matchKind !in setOf(ImportMatchKind.EXACT_CODE, ImportMatchKind.EXACT_NAME)) {
                MatchLabel(if (resolution.skip) Icons.Outlined.SkipNext else Icons.Outlined.CheckCircle, resolvedName)
            }

            if (candidate.matchKind != ImportMatchKind.INVALID && candidate.matchKind !in setOf(ImportMatchKind.EXACT_CODE, ImportMatchKind.EXACT_NAME)) {
                if (!resolution.resolved) {
                    if (candidate.matchKind == ImportMatchKind.SUGGESTED) {
                        TextButton(onClick = onAcceptSuggestion) { Text("اعتماد الاقتراح") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = onPickExisting, modifier = Modifier.weight(1f)) { Text("اختيار موجود") }
                        OutlinedButton(onClick = onCreateNew, modifier = Modifier.weight(1f)) { Text("صنف جديد") }
                        IconButton(onClick = onSkip) { Icon(Icons.Outlined.SkipNext, "تخطي") }
                    }
                } else {
                    TextButton(onClick = onReset) { Text("تغيير القرار") }
                }
            }
        }
    }
}

@Composable
private fun MatchLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, error: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ImportDoneStep(summary: PriceImportSummary?, onDone: () -> Unit) {
    val result = summary ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.TaskAlt, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
            Text("تم استيراد القائمة", style = MaterialTheme.typography.headlineSmall)
            Text("${result.importedRows} سعر تم حفظه", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                ReviewCount("أسعار جديدة", result.insertedPrices.toString())
                ReviewCount("تم تحديثها", result.updatedPrices.toString())
                ReviewCount("أصناف جديدة", result.createdItems.toString())
                ReviewCount("متخطى", result.skippedRows.toString())
            }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("العودة لقائمة الأسعار") }
        }
    }
}

@Composable
private fun ImportItemPickerDialog(
    items: List<ItemEntity>,
    onDismiss: () -> Unit,
    onSelect: (ItemEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) {
        if (query.isBlank()) items else items.filter { it.name.contains(query, true) || it.code.contains(query, true) }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ربط بصنف موجود", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("بحث بالاسم أو الكود") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                )
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(filtered, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = { Text("${item.code} • ${item.defaultUnit}") },
                            modifier = Modifier.clickable { onSelect(item) },
                        )
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("إلغاء") }
            }
        }
    }
}

private fun resolveFileName(context: android.content.Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index) ?: "price-list.xlsx"
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "price-list.xlsx"
}

private val importDecimal = DecimalFormat("#,##0.00")
private fun importMoney(value: Double): String = "${importDecimal.format(value)} ج"
private fun formatImportDiscount(value: Double): String = if (value % 1.0 == 0.0) "${value.toInt()}%" else "${importDecimal.format(value)}%"
