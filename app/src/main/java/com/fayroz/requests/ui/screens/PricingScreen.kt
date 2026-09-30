package com.fayroz.requests.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fayroz.requests.data.model.*
import com.fayroz.requests.data.repository.FayrozRepository
import com.fayroz.requests.export.FayrozReports
import com.fayroz.requests.export.ShareFiles
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun PricingScreen(
    repository: FayrozRepository,
    onPriceSheet: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val sheets by repository.sheetSummaries.collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    val filtered = remember(sheets, query) {
        if (query.isBlank()) sheets else sheets.filter {
            it.projectName.contains(query, true) ||
                it.sheetNumber.contains(query, true) ||
                it.craftsmanName.contains(query, true) ||
                it.workLocation.contains(query, true)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FayrozDetailHeader(
            title = "تسعير الكشوف",
            subtitle = "لكل محل أو مورد نسخة تسعير مستقلة",
            onBack = onBack,
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("بحث عن كشف") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
        )

        if (filtered.isEmpty()) {
            EmptyState(
                Icons.Outlined.CompareArrows,
                "لا توجد كشوف للتسعير",
                "أنشئ كشف طلبات أولًا، ثم اعمل نسخة تسعير لكل محل أو مورد.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { sheet ->
                    Card(onClick = { onPriceSheet(sheet.id) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("كشف ${sheet.sheetNumber}", style = MaterialTheme.typography.titleMedium)
                                Text(sheet.projectName, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "${sheet.lineCount} بند • ${sheet.pricingCopyCount} نسخة تسعير",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            FilledTonalIconButton(onClick = { onPriceSheet(sheet.id) }) {
                                Icon(Icons.Outlined.ContentCopy, "نسخ التسعير")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SheetPricingScreen(
    repository: FayrozRepository,
    sheetId: Long,
    onBack: () -> Unit,
    onOpenCopy: (Long) -> Unit,
    onCompare: () -> Unit,
) {
    val copies by repository.pricingCopies(sheetId).collectAsState(initial = emptyList())
    val suppliers by repository.suppliers.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var sheet by remember { mutableStateOf<RequestSheetEntity?>(null) }
    var project by remember { mutableStateOf<ProjectEntity?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<PricingCopySummary?>(null) }

    LaunchedEffect(sheetId) {
        sheet = repository.getSheet(sheetId)
        project = sheet?.let { repository.getProject(it.projectId) }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FayrozDetailHeader(
            title = sheet?.let { "تسعير كشف ${it.sheetNumber}" } ?: "نسخ التسعير",
            subtitle = project?.name ?: "",
            onBack = onBack,
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { showCreate = true },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.NoteAdd, null)
                Spacer(Modifier.width(6.dp))
                Text("نسخة تسعير")
            }

            OutlinedButton(
                onClick = onCompare,
                enabled = copies.size >= 2,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.CompareArrows, null)
                Spacer(Modifier.width(6.dp))
                Text("مقارنة")
            }
        }

        Text(
            if (copies.isEmpty()) "اعمل نسخة لكل محل أو مورد."
            else "${copies.size} نسخة • ${copies.count { it.complete }} مكتملة",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (copies.isEmpty()) {
            EmptyState(
                Icons.Outlined.ContentCopy,
                "لسه مفيش نسخ تسعير",
                "الكشف يفضل واحد، وكل محل ياخد نسخة تسعير مستقلة.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(copies, key = { it.copyId }) { copy ->
                    Card(onClick = { onOpenCopy(copy.copyId) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(copy.placeName, style = MaterialTheme.typography.titleMedium)
                                if (!copy.supplierName.isNullOrBlank()) {
                                    Text(
                                        "مرتبط بالمورد: ${copy.supplierName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Text(
                                    "${copy.pricedCount} / ${copy.lineCount} بند",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    SuggestionChip(
                                        onClick = { onOpenCopy(copy.copyId) },
                                        label = { Text(if (copy.complete) "مكتمل" else "غير مكتمل") },
                                        icon = {
                                            Icon(
                                                if (copy.complete) Icons.Outlined.CheckCircle
                                                else Icons.Outlined.WarningAmber,
                                                null,
                                                modifier = Modifier.size(15.dp),
                                            )
                                        },
                                    )
                                    if (copy.quoteNumber.isNotBlank()) {
                                        AssistChip(
                                            onClick = { onOpenCopy(copy.copyId) },
                                            label = { Text("عرض ${copy.quoteNumber}") },
                                        )
                                    }
                                }
                                Text(
                                    if (copy.complete) "الإجمالي: ${money(copy.total)}"
                                    else "الإجمالي الجزئي: ${money(copy.total)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (copy.complete) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { deleteTarget = copy }) {
                                Icon(Icons.Outlined.DeleteOutline, "حذف النسخة")
                            }
                            Icon(Icons.Outlined.ChevronLeft, null)
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        PricingCopyDetailsDialog(
            title = "نسخة تسعير جديدة",
            suppliers = suppliers,
            initialPlace = "",
            initialSupplierId = null,
            initialQuoteNumber = "",
            initialQuoteDate = System.currentTimeMillis(),
            initialNotes = "",
            onDismiss = { showCreate = false },
            onSave = { place, supplierId, createSupplier, quoteNumber, quoteDate, notes ->
                scope.launch {
                    val id = repository.createPricingCopy(
                        sheetId = sheetId,
                        placeName = place,
                        supplierId = supplierId,
                        createSupplier = createSupplier,
                        quoteNumber = quoteNumber,
                        quoteDate = quoteDate,
                        notes = notes,
                    )
                    showCreate = false
                    onOpenCopy(id)
                }
            },
        )
    }

    deleteTarget?.let { copy ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف نسخة التسعير؟") },
            text = { Text("سيتم حذف أسعار وماركات «${copy.placeName}» فقط، والكشف الأصلي لن يتأثر.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deletePricingCopy(copy.copyId) }
                    deleteTarget = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("إلغاء") } },
        )
    }
}

@Composable
private fun PricingCopyDetailsDialog(
    title: String,
    suppliers: List<SupplierEntity>,
    initialPlace: String,
    initialSupplierId: Long?,
    initialQuoteNumber: String,
    initialQuoteDate: Long,
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String, Long?, Boolean, String, Long, String) -> Unit,
) {
    val context = LocalContext.current
    var place by remember { mutableStateOf(initialPlace) }
    var supplierId by remember { mutableStateOf(initialSupplierId) }
    var quoteNumber by remember { mutableStateOf(initialQuoteNumber) }
    var quoteDate by remember { mutableLongStateOf(initialQuoteDate) }
    var notes by remember { mutableStateOf(initialNotes) }
    var supplierMenu by remember { mutableStateOf(false) }
    var createSupplier by remember { mutableStateOf(false) }
    val selectedSupplier = suppliers.firstOrNull { it.id == supplierId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = place,
                        onValueChange = {
                            place = it
                            if (selectedSupplier?.name != it) supplierId = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("اسم المحل / المورد *") },
                        singleLine = true,
                    )
                }

                item {
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { supplierMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Storefront, null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                selectedSupplier?.name ?: "ربط بمورد مسجل (اختياري)",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                            Icon(Icons.Outlined.KeyboardArrowDown, null)
                        }
                        DropdownMenu(
                            expanded = supplierMenu,
                            onDismissRequest = { supplierMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون ربط — محل مؤقت") },
                                onClick = {
                                    supplierId = null
                                    createSupplier = false
                                    supplierMenu = false
                                },
                            )
                            suppliers.forEach { supplier ->
                                DropdownMenuItem(
                                    text = { Text(supplier.name) },
                                    onClick = {
                                        supplierId = supplier.id
                                        place = supplier.name
                                        createSupplier = false
                                        supplierMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                if (supplierId == null && place.isNotBlank()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = createSupplier,
                                onCheckedChange = { createSupplier = it },
                            )
                            Text("حفظ «$place» كمورد جديد وربط الأسعار بتاريخ المورد")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = quoteNumber,
                        onValueChange = { quoteNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("رقم عرض السعر (اختياري)") },
                        singleLine = true,
                    )
                }

                item {
                    OutlinedButton(
                        onClick = {
                            showDatePicker(context, quoteDate) { quoteDate = it }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, null)
                        Spacer(Modifier.width(6.dp))
                        Text("تاريخ العرض: ${formatDate(quoteDate)}")
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("ملاحظات (اختياري)") },
                        minLines = 2,
                        maxLines = 3,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        place.trim(),
                        supplierId,
                        createSupplier,
                        quoteNumber.trim(),
                        quoteDate,
                        notes.trim(),
                    )
                },
                enabled = place.isNotBlank(),
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

private data class EditableCopyLine(
    val detail: PricingCopyLineDetail,
    val brand: String,
    val priceText: String,
)

@Composable
fun PricingCopyEditorScreen(
    repository: FayrozRepository,
    copyId: Long,
    onBack: () -> Unit,
) {
    val brandLibrary by repository.categoryBrands.collectAsState(initial = emptyList())
    val suppliers by repository.suppliers.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var detail by remember { mutableStateOf<PricingCopyDetail?>(null) }
    val lines = remember { mutableStateListOf<EditableCopyLine>() }
    var loading by remember { mutableStateOf(true) }
    var brandTargetIndex by remember { mutableStateOf<Int?>(null) }
    var editDetails by remember { mutableStateOf(false) }
    var saveState by remember { mutableStateOf("تم الحفظ تلقائيًا ✓") }

    fun applyLoaded(data: PricingCopyDetail?) {
        detail = data
        lines.clear()
        data?.lines?.forEach { line ->
            lines += EditableCopyLine(
                detail = line,
                brand = line.brand,
                priceText = line.unitPrice?.let(::formatPlainNumber).orEmpty(),
            )
        }
    }

    LaunchedEffect(copyId) {
        loading = true
        applyLoaded(repository.loadPricingCopyDetail(copyId))
        loading = false
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val fresh = repository.loadPricingCopyDetail(copyId) ?: error("نسخة التسعير غير موجودة")
                    context.contentResolver.openOutputStream(uri)?.use {
                        FayrozReports.writePricingCopyPdf(it, fresh)
                    } ?: error("تعذر فتح الملف")
                }.onSuccess {
                    Toast.makeText(context, "تم تصدير PDF ✓", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "تعذر تصدير PDF", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val data = detail
    if (data == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            FayrozDetailHeader("نسخة التسعير", "تعذر تحميل النسخة", onBack)
        }
        return
    }

    val total = lines.sumOf { line ->
        (line.priceText.toDoubleOrNull() ?: 0.0) * line.detail.quantity
    }
    val pricedCount = lines.count { it.priceText.toDoubleOrNull() != null }
    val complete = lines.isNotEmpty() && pricedCount == lines.size
    val linkedSupplier = suppliers.firstOrNull { it.id == data.copy.supplierId }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowForward, "رجوع")
            }
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(data.copy.placeName, style = MaterialTheme.typography.titleLarge)
                Text(
                    "كشف ${data.sheet.sheetNumber} • ${data.project.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val meta = buildList {
                    linkedSupplier?.let { add("مورد: ${it.name}") }
                    if (data.copy.quoteNumber.isNotBlank()) add("عرض ${data.copy.quoteNumber}")
                    add(formatDate(data.copy.quoteDate))
                }
                Text(
                    meta.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { editDetails = true }) {
                Icon(Icons.Outlined.Edit, "تعديل بيانات العرض")
            }
            IconButton(
                onClick = { pdfLauncher.launch("FAYROZ-Quote-${data.copy.placeName}.pdf") },
            ) {
                Icon(Icons.Outlined.PictureAsPdf, "PDF")
            }
            IconButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            val fresh = repository.loadPricingCopyDetail(copyId) ?: error("نسخة التسعير غير موجودة")
                            ShareFiles.sharePdf(
                                context,
                                "FAYROZ-Quote-${fresh.copy.placeName}.pdf",
                            ) {
                                FayrozReports.writePricingCopyPdf(it, fresh)
                            }
                        }.onFailure {
                            Toast.makeText(context, "تعذر مشاركة التسعير.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
            ) {
                Icon(Icons.Outlined.Share, "مشاركة")
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.CloudDone,
                    null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    saveState,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (complete) "إجمالي العرض" else "إجمالي جزئي",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        money(total),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (complete) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!complete) {
            Text(
                "التسعير غير مكتمل: ${pricedCount}/${lines.size} بند. لن يُعتبر هذا الإجمالي أقل عرض كامل في المقارنة.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            itemsIndexed(lines, key = { _, line -> line.detail.requestLineId }) { index, line ->
                PricingCopyLineCard(
                    line = line,
                    onBrandClick = { brandTargetIndex = index },
                    onPriceChange = { text ->
                        val current = lines[index]
                        lines[index] = current.copy(priceText = text)
                        saveState = "جارٍ الحفظ..."
                        scope.launch {
                            repository.updatePricingCopyLine(
                                copyId = copyId,
                                requestLineId = current.detail.requestLineId,
                                brand = current.brand,
                                unitPrice = text.toDoubleOrNull(),
                            )
                            saveState = "تم الحفظ تلقائيًا ✓"
                        }
                    },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (editDetails) {
        PricingCopyDetailsDialog(
            title = "تعديل بيانات عرض السعر",
            suppliers = suppliers,
            initialPlace = data.copy.placeName,
            initialSupplierId = data.copy.supplierId,
            initialQuoteNumber = data.copy.quoteNumber,
            initialQuoteDate = data.copy.quoteDate,
            initialNotes = data.copy.notes,
            onDismiss = { editDetails = false },
            onSave = { place, supplierId, createSupplier, quoteNumber, quoteDate, notes ->
                scope.launch {
                    saveState = "جارٍ الحفظ..."
                    repository.updatePricingCopyMetadata(
                        copyId = copyId,
                        placeName = place,
                        supplierId = supplierId,
                        createSupplier = createSupplier,
                        quoteNumber = quoteNumber,
                        quoteDate = quoteDate,
                        notes = notes,
                    )
                    applyLoaded(repository.loadPricingCopyDetail(copyId))
                    saveState = "تم الحفظ تلقائيًا ✓"
                    editDetails = false
                }
            },
        )
    }

    brandTargetIndex?.let { index ->
        val line = lines.getOrNull(index)
        if (line != null) {
            val categoryId = line.detail.categoryId
            val brands = brandLibrary
                .filter { categoryId != null && it.categoryId == categoryId }
                .map { it.name }
                .distinct()
            BrandLibraryDialog(
                title = line.detail.itemName,
                selected = line.brand,
                brands = brands,
                allowAdd = categoryId != null,
                onDismiss = { brandTargetIndex = null },
                onSelect = { brand ->
                    val current = lines[index]
                    lines[index] = current.copy(brand = brand)
                    saveState = "جارٍ الحفظ..."
                    scope.launch {
                        repository.updatePricingCopyLine(
                            copyId = copyId,
                            requestLineId = current.detail.requestLineId,
                            brand = brand,
                            unitPrice = current.priceText.toDoubleOrNull(),
                        )
                        saveState = "تم الحفظ تلقائيًا ✓"
                    }
                    brandTargetIndex = null
                },
                onAdd = { brand ->
                    if (categoryId != null) {
                        saveState = "جارٍ الحفظ..."
                        scope.launch {
                            repository.addCategoryBrand(categoryId, brand)
                            val current = lines[index]
                            lines[index] = current.copy(brand = brand)
                            repository.updatePricingCopyLine(
                                copyId = copyId,
                                requestLineId = current.detail.requestLineId,
                                brand = brand,
                                unitPrice = current.priceText.toDoubleOrNull(),
                            )
                            saveState = "تم الحفظ تلقائيًا ✓"
                        }
                    }
                    brandTargetIndex = null
                },
            )
        }
    }
}

@Composable
private fun PricingCopyLineCard(
    line: EditableCopyLine,
    onBrandClick: () -> Unit,
    onPriceChange: (String) -> Unit,
) {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(line.detail.itemName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${formatQuantity(line.detail.quantity)} ${line.detail.unit}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                val lineTotal = (line.priceText.toDoubleOrNull() ?: 0.0) * line.detail.quantity
                if (line.priceText.isNotBlank()) {
                    Text(
                        money(lineTotal),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onBrandClick,
                    modifier = Modifier.weight(0.48f).height(56.dp),
                ) {
                    Icon(Icons.Outlined.LocalOffer, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        line.brand.ifBlank { "الماركة / الشركة" },
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
                }

                OutlinedTextField(
                    value = line.priceText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("\\d*(\\.\\d*)?"))) {
                            onPriceChange(value)
                        }
                    },
                    modifier = Modifier.weight(0.52f),
                    label = { Text("السعر") },
                    placeholder = { Text("0.00") },
                    suffix = { Text("ج") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun BrandLibraryDialog(
    title: String,
    selected: String,
    brands: List<String>,
    allowAdd: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onAdd: (String) -> Unit,
) {
    var newBrand by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ماركة / شركة — $title") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (brands.isEmpty()) {
                    Text("مفيش ماركات مسجلة للقسم ده.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(brands) { brand ->
                            ListItem(
                                headlineContent = { Text(brand) },
                                leadingContent = {
                                    RadioButton(
                                        selected = selected.equals(brand, ignoreCase = true),
                                        onClick = { onSelect(brand) },
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                if (allowAdd) {
                    HorizontalDivider()
                    OutlinedTextField(
                        value = newBrand,
                        onValueChange = { newBrand = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("إضافة ماركة / شركة للمكتبة") },
                        singleLine = true,
                    )
                    Button(
                        onClick = { onAdd(newBrand.trim()) },
                        enabled = newBrand.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("إضافة واختيار")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}

@Composable
fun PricingCopiesComparisonScreen(
    repository: FayrozRepository,
    sheetId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<PricingCopiesComparison?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(sheetId, reloadKey) {
        loading = true
        loadError = null
        runCatching {
            repository.loadPricingCopiesComparison(sheetId)
        }.onSuccess { loaded ->
            data = loaded
        }.onFailure { error ->
            data = null
            loadError = error.message ?: error::class.simpleName ?: "خطأ غير معروف"
        }
        loading = false
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val comparison = data
        if (uri != null && comparison != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    FayrozReports.writeComparisonPdf(it, comparison)
                } ?: error("تعذر فتح الملف")
            }.onSuccess {
                Toast.makeText(context, "تم تصدير PDF ✓", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "تعذر تصدير PDF", Toast.LENGTH_LONG).show()
            }
        }
    }

    val xlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        val comparison = data
        if (uri != null && comparison != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    FayrozReports.writeComparisonXlsx(it, comparison)
                } ?: error("تعذر فتح الملف")
            }.onSuccess {
                Toast.makeText(context, "تم تصدير Excel ✓", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "تعذر تصدير Excel", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    loadError?.let { message ->
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FayrozDetailHeader("مقارنة التسعيرات", "تعذر تحميل المقارنة", onBack)
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "حصل خطأ أثناء قراءة بيانات المقارنة، وتم منع إغلاق البرنامج.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { reloadKey++ }) {
                        Icon(Icons.Outlined.Refresh, null)
                        Spacer(Modifier.width(6.dp))
                        Text("إعادة المحاولة")
                    }
                }
            }
        }
        return
    }

    val comparison = data
    if (comparison == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            FayrozDetailHeader("مقارنة التسعيرات", "الكشف غير موجود أو لا يمكن قراءته", onBack)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FayrozDetailHeader(
                title = "مقارنة تسعيرات كشف ${comparison.sheet.sheetNumber}",
                subtitle = comparison.project.name,
                onBack = onBack,
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        pdfLauncher.launch("FAYROZ-Compare-${comparison.sheet.sheetNumber}.pdf")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.PictureAsPdf, null)
                    Spacer(Modifier.width(5.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = {
                        xlsxLauncher.launch("FAYROZ-Compare-${comparison.sheet.sheetNumber}.xlsx")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.TableView, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Excel")
                }
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                ShareFiles.sharePdf(
                                    context,
                                    "FAYROZ-Compare-${comparison.sheet.sheetNumber}.pdf",
                                ) {
                                    FayrozReports.writeComparisonPdf(it, comparison)
                                }
                            }.onFailure {
                                Toast.makeText(context, "تعذر مشاركة المقارنة.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Share, null)
                    Spacer(Modifier.width(5.dp))
                    Text("مشاركة")
                }
            }
        }

        if (comparison.copies.size < 2) {
            item {
                Card {
                    Text(
                        "اعمل نسختين تسعير على الأقل علشان تظهر المقارنة.",
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                    )
                }
            }
        } else {
            item {
                Card {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text("ملخص المقارنة", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "العروض الكاملة: ${comparison.completeCopies.size}/${comparison.copies.size}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        comparison.lowestCompleteTotal?.let {
                            Text("أقل عرض كامل: ${money(it)}", color = MaterialTheme.colorScheme.secondary)
                        } ?: Text(
                            "لا يوجد عرض مكتمل حتى الآن.",
                            color = MaterialTheme.colorScheme.error,
                        )
                        comparison.completeRangeSaving?.takeIf { it > 0.0 }?.let {
                            Text("الفرق بين أعلى وأقل عرض كامل: ${money(it)}")
                        }
                        comparison.bestMixTotal?.let {
                            Text(
                                "أقل تجميعة بند-بند: ${money(it)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "العرض غير المكتمل لا يدخل في مقارنة أقل إجمالي كامل.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Text("إجمالي النسخ", style = MaterialTheme.typography.titleMedium) }

            items(comparison.copies, key = { it.copy.id }) { copy ->
                val isLowestComplete = copy.complete &&
                    comparison.lowestCompleteTotal != null &&
                    copy.total == comparison.lowestCompleteTotal
                Card(
                    colors = if (isLowestComplete) {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        )
                    } else CardDefaults.cardColors()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(copy.copy.placeName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${copy.pricedCount}/${copy.lines.size} بند • الأقل في ${comparison.cheapestLineCount(copy.copy.id)} بند",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (isLowestComplete) {
                                Text(
                                    "أقل عرض كامل",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (copy.complete) money(copy.total) else "غير مكتمل",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (!copy.complete) {
                                Text(
                                    "جزئي: ${money(copy.total)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            val baseLines = comparison.copies.firstOrNull()?.lines.orEmpty()
            item {
                Spacer(Modifier.height(4.dp))
                Text("مقارنة البنود", style = MaterialTheme.typography.titleMedium)
            }

            items(baseLines, key = { it.requestLineId }) { baseLine ->
                val offers = comparison.copies.mapNotNull { copy ->
                    copy.lines.firstOrNull { it.requestLineId == baseLine.requestLineId }?.let { line ->
                        Triple(copy, line, line.unitPrice)
                    }
                }
                val lowest = offers.mapNotNull { it.third }.minOrNull()

                Card {
                    Column(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(baseLine.itemName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${formatQuantity(baseLine.quantity)} ${baseLine.unit}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        offers.forEach { (copy, line, price) ->
                            val isLowest = price != null && lowest != null && price == lowest
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (isLowest) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(copy.copy.placeName, style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            line.brand.ifBlank { "بدون ماركة / شركة" },
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            price?.let(::money) ?: "غير مسعّر",
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                        price?.let {
                                            Text(
                                                money(it * line.quantity),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(18.dp)) }
    }
}

private fun showDatePicker(
    context: Context,
    current: Long,
    onSelected: (Long) -> Unit,
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = current }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onSelected(selected.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
    ).show()
}

private val pricingMoneyFormat = DecimalFormat("#,##0.00")

private fun money(value: Double): String = "${pricingMoneyFormat.format(value)} ج"

private fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else pricingMoneyFormat.format(value)

private fun formatPlainNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

private fun formatDate(timestamp: Long): String =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))
