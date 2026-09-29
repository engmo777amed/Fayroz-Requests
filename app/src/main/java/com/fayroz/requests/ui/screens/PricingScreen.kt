package com.fayroz.requests.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fayroz.requests.data.model.*
import com.fayroz.requests.data.repository.FayrozRepository
import com.fayroz.requests.domain.PriceFreshnessEngine
import com.fayroz.requests.export.PricingExport
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PricingScreen(
    repository: FayrozRepository,
    onPriceSheet: (Long) -> Unit,
) {
    val sheets by repository.sheetSummaries.collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    val filtered = remember(sheets, query) {
        if (query.isBlank()) sheets else sheets.filter {
            it.projectName.contains(query, true) || it.sheetNumber.contains(query, true) ||
                it.trade.contains(query, true) || it.craftsmanName.contains(query, true)
        }
    }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("تسعير الكشوف", "اختر كشفًا وقارن آخر أسعار الموردين لكل بند")
        Card {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("قاعدة الخصم", style = MaterialTheme.typography.titleMedium)
                Text("خصم الصنف ← خصم المجموعة ← خصم المورد العام", style = MaterialTheme.typography.bodyMedium)
                Text("سعر الليستة والخصم والصافي محفوظون تاريخيًا.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("بحث عن كشف") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
        )

        if (filtered.isEmpty()) {
            EmptyState(Icons.Outlined.CompareArrows, "لا توجد كشوف للتسعير", "أنشئ كشف طلبات أولًا، ثم ارجع هنا لتسعيره من أكثر من مورد.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(filtered, key = { it.id }) { sheet ->
                    Card(onClick = { onPriceSheet(sheet.id) }) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("كشف ${sheet.sheetNumber}", style = MaterialTheme.typography.titleMedium)
                                Text(sheet.projectName, color = MaterialTheme.colorScheme.primary)
                                Text("${sheet.lineCount} بند • ${sheet.trade.ifBlank { "بدون تخصص" }}", style = MaterialTheme.typography.bodySmall)
                            }
                            FilledTonalIconButton(onClick = { onPriceSheet(sheet.id) }) {
                                Icon(Icons.Outlined.PriceCheck, "تسعير")
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
) {
    var comparison by remember { mutableStateOf<SheetPricingComparison?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var directQuoteLine by remember { mutableStateOf<PricingLineComparison?>(null) }
    val context = LocalContext.current
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val data = comparison
        if (uri != null && data != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { PricingExport.writePdf(it, data) }
                    ?: error("تعذر فتح الملف")
            }.onSuccess {
                Toast.makeText(context, "تم تصدير PDF", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "تعذر تصدير PDF", Toast.LENGTH_LONG).show()
            }
        }
    }
    val xlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        val data = comparison
        if (uri != null && data != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { PricingExport.writeXlsx(it, data) }
                    ?: error("تعذر فتح الملف")
            }.onSuccess {
                Toast.makeText(context, "تم تصدير Excel", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "تعذر تصدير Excel", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(sheetId, refreshKey) {
        loading = true
        comparison = repository.loadPricingComparison(sheetId)
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val data = comparison
    if (data == null) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "رجوع") }
            EmptyState(Icons.Outlined.ErrorOutline, "الكشف غير موجود", "تعذر فتح بيانات التسعير لهذا الكشف.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScreenHeader("تسعير كشف ${data.sheet.sheetNumber}", data.project.name)
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "رجوع") }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { pdfLauncher.launch(exportFileName(data, "pdf")) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.PictureAsPdf, null)
                    Spacer(Modifier.width(6.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = { xlsxLauncher.launch(exportFileName(data, "xlsx")) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.TableView, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Excel")
                }
            }
        }

        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("تم تسعير", style = MaterialTheme.typography.bodySmall)
                            Text("${data.pricedLineCount} / ${data.totalLineCount} بند", style = MaterialTheme.typography.titleLarge)
                        }
                        data.bestMixTotal?.let {
                            Column {
                                Text("أقل تجميعة", style = MaterialTheme.typography.bodySmall)
                                Text(money(it), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                    if (data.pricedLineCount < data.totalLineCount) {
                        Text("بعض البنود لا يوجد لها سعر بعد. أضف ليستة مورد أو عرضًا مباشرًا من داخل البند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (data.supplierTotals.isNotEmpty()) {
            item { Text("إجمالي الموردين", style = MaterialTheme.typography.titleMedium) }
            items(data.supplierTotals, key = { it.supplierId }) { total ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(total.supplierName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (total.complete) "مسعّر كل البنود" else "${total.coveredLines}/${total.totalLines} بند",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (total.complete) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(money(total.totalNet), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }

        item { Text("تفاصيل البنود", style = MaterialTheme.typography.titleMedium) }

        items(data.lines, key = { it.lineId }) { line ->
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(line.itemName, style = MaterialTheme.typography.titleMedium)
                            Text("${formatQuantity(line.quantity)} ${line.unit}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            if (line.usage.isNotBlank()) Text("الاستخدام: ${line.usage}", style = MaterialTheme.typography.bodySmall)
                            if (line.lineDescription.isNotBlank()) Text(line.lineDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FilledTonalIconButton(onClick = { directQuoteLine = line }) {
                            Icon(Icons.Outlined.AddCard, "عرض مباشر")
                        }
                    }

                    if (line.offers.isEmpty()) {
                        Text("لا يوجد سعر لهذا الصنف حتى الآن.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { directQuoteLine = line }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("إضافة عرض سعر مباشر")
                        }
                    } else {
                        line.offers.forEachIndexed { index, offer ->
                            val lowest = index == 0
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = if (lowest) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(offer.supplierName, style = MaterialTheme.typography.titleSmall)
                                            if (lowest) SuggestionChip(onClick = {}, label = { Text("أقل صافي") })
                                        }
                                        Text(money(offer.totalNet), style = MaterialTheme.typography.titleMedium)
                                    }
                                    Text(
                                        "ليستة ${money(offer.listPrice)}  •  خصم ${formatDiscount(offer.discountPercent)}  •  صافي الوحدة ${money(offer.netUnitPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    val freshness = PriceFreshnessEngine.evaluate(offer.priceDate)
                                    Text(
                                        "${sourceLabel(offer.source)} • ${formatDate(offer.priceDate)} • ${freshness.statusLabel} (${freshness.label})",
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

    directQuoteLine?.let { line ->
        DirectQuoteDialog(
            repository = repository,
            line = line,
            onDismiss = { directQuoteLine = null },
            onSaved = {
                directQuoteLine = null
                refreshKey++
            },
        )
    }
}

@Composable
private fun DirectQuoteDialog(
    repository: FayrozRepository,
    line: PricingLineComparison,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val suppliers by repository.suppliers.collectAsState(initial = emptyList())
    val approvedSuppliers = remember(suppliers) { suppliers.filter { it.approved } }
    val scope = rememberCoroutineScope()
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var showSupplierPicker by remember { mutableStateOf(false) }
    var price by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }
    var rememberDiscount by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSupplier?.id) {
        val supplier = selectedSupplier ?: return@LaunchedEffect
        discount = repository.suggestedDiscountPercent(supplier.id, line.itemId).toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("عرض مباشر — ${line.itemName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showSupplierPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Storefront, null)
                    Spacer(Modifier.width(8.dp))
                    Text(selectedSupplier?.name ?: "اختيار المورد")
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("السعر قبل الخصم *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("الخصم %") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("احفظ الخصم كخصم خاص للصنف", modifier = Modifier.weight(1f))
                    Checkbox(checked = rememberDiscount, onCheckedChange = { rememberDiscount = it })
                }
                val p = price.toDoubleOrNull() ?: 0.0
                val d = discount.toDoubleOrNull() ?: 0.0
                val net = p * (1.0 - d.coerceIn(0.0, 100.0) / 100.0)
                Text("صافي الوحدة: ${money(net)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Text("إجمالي البند: ${money(net * line.quantity)}", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات / مرجع العرض") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val supplier = selectedSupplier ?: return@Button
                    saving = true
                    scope.launch {
                        repository.addDirectQuote(
                            supplierId = supplier.id,
                            itemId = line.itemId,
                            quotedPrice = price.toDoubleOrNull() ?: 0.0,
                            discountPercent = discount.toDoubleOrNull(),
                            rememberAsItemDiscount = rememberDiscount,
                            notes = notes,
                        )
                        saving = false
                        onSaved()
                    }
                },
                enabled = selectedSupplier != null && (price.toDoubleOrNull() ?: -1.0) >= 0.0 && (discount.toDoubleOrNull() ?: -1.0) in 0.0..100.0 && !saving,
            ) { Text(if (saving) "جارٍ الحفظ" else "حفظ العرض") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )

    if (showSupplierPicker) {
        SupplierPickerDialog(
            suppliers = approvedSuppliers,
            onDismiss = { showSupplierPicker = false },
            onSelect = {
                selectedSupplier = it
                showSupplierPicker = false
            },
        )
    }
}

@Composable
private fun SupplierPickerDialog(
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onSelect: (SupplierEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(suppliers, query) {
        if (query.isBlank()) suppliers else suppliers.filter { it.name.contains(query, true) || it.specialty.contains(query, true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("اختيار المورد", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    query,
                    { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("بحث") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                )
                if (filtered.isEmpty()) {
                    Text("لا يوجد موردون معتمدون.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(filtered, key = { it.id }) { supplier ->
                            ListItem(
                                headlineContent = { Text(supplier.name) },
                                supportingContent = { Text("خصم افتراضي ${formatDiscount(supplier.defaultDiscountPercent)}${if (supplier.specialty.isBlank()) "" else " • ${supplier.specialty}"}") },
                                modifier = Modifier.clickable { onSelect(supplier) },
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("إلغاء") }
            }
        }
    }
}

private fun exportFileName(data: SheetPricingComparison, extension: String): String {
    val safeProject = data.project.name.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").trim('_').take(35)
    return "FAYROZ_${safeProject.ifBlank { "Project" }}_Sheet_${data.sheet.sheetNumber}.$extension"
}

private val pricingMoneyFormat = DecimalFormat("#,##0.00")
private fun money(value: Double): String = "${pricingMoneyFormat.format(value)} ج"
private fun formatDiscount(value: Double): String = if (value % 1.0 == 0.0) "${value.toInt()}%" else "${pricingMoneyFormat.format(value)}%"
private fun formatQuantity(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else pricingMoneyFormat.format(value)
private fun sourceLabel(source: PriceSource): String = when (source) {
    PriceSource.PRICE_LIST -> "ليستة مورد"
    PriceSource.DIRECT_QUOTE -> "عرض مباشر"
    PriceSource.MANUAL -> "سعر يدوي"
}
private fun formatDate(timestamp: Long): String = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(timestamp))
