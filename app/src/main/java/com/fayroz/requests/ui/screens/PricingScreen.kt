package com.fayroz.requests.ui.screens

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fayroz.requests.data.model.*
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                it.craftsmanName.contains(query, true)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FayrozDetailHeader(
            title = "تسعير الكشوف",
            subtitle = "لكل محل نسخة تسعير مستقلة",
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
                                Text("${sheet.lineCount} بند", style = MaterialTheme.typography.bodySmall)
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
            else "${copies.size} نسخة تسعير",
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
                                Text(
                                    "${copy.pricedCount} / ${copy.lineCount} بند متسعّر",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (copy.pricedCount > 0) {
                                    Text(
                                        "الإجمالي: ${money(copy.total)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
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
        CreatePricingCopyDialog(
            onDismiss = { showCreate = false },
            onCreate = { place, notes ->
                scope.launch {
                    val id = repository.createPricingCopy(sheetId, place, notes)
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
private fun CreatePricingCopyDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
) {
    var place by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نسخة تسعير جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم المحل / المورد *") },
                    placeholder = { Text("مثال: محل النور") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ملاحظات (اختياري)") },
                    maxLines = 2,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(place.trim(), notes.trim()) },
                enabled = place.isNotBlank(),
            ) { Text("إنشاء النسخة") }
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
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<PricingCopyDetail?>(null) }
    val lines = remember { mutableStateListOf<EditableCopyLine>() }
    var loading by remember { mutableStateOf(true) }
    var brandTargetIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(copyId) {
        loading = true
        detail = repository.loadPricingCopyDetail(copyId)
        lines.clear()
        detail?.lines?.forEach { line ->
            lines += EditableCopyLine(
                detail = line,
                brand = line.brand,
                priceText = line.unitPrice?.let(::formatPlainNumber).orEmpty(),
            )
        }
        loading = false
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

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowForward, "رجوع")
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(data.copy.placeName, style = MaterialTheme.typography.titleLarge)
                Text(
                    "كشف ${data.sheet.sheetNumber} • ${data.project.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("الإجمالي", style = MaterialTheme.typography.labelSmall)
                Text(money(total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            }
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
                        scope.launch {
                            repository.updatePricingCopyLine(
                                copyId = copyId,
                                requestLineId = current.detail.requestLineId,
                                brand = current.brand,
                                unitPrice = text.toDoubleOrNull(),
                            )
                        }
                    },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
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
                    scope.launch {
                        repository.updatePricingCopyLine(
                            copyId = copyId,
                            requestLineId = current.detail.requestLineId,
                            brand = brand,
                            unitPrice = current.priceText.toDoubleOrNull(),
                        )
                    }
                    brandTargetIndex = null
                },
                onAdd = { brand ->
                    if (categoryId != null) {
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
                    if (line.detail.usage.isNotBlank()) {
                        Text(
                            line.detail.usage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                OutlinedTextField(
                    value = "${formatQuantity(line.detail.quantity)} ${line.detail.unit}",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(0.42f),
                    label = { Text("الكمية") },
                    singleLine = true,
                )

                OutlinedButton(
                    onClick = onBrandClick,
                    modifier = Modifier.weight(0.58f).height(56.dp),
                ) {
                    Icon(Icons.Outlined.LocalOffer, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        line.brand.ifBlank { "اختيار الماركة" },
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
                }
            }

            OutlinedTextField(
                value = line.priceText,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("\\d*(\\.\\d*)?"))) {
                        onPriceChange(value)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("السعر") },
                placeholder = { Text("0.00") },
                suffix = { Text("ج / ${line.detail.unit}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
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
        title = { Text("ماركة — $title") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (brands.isEmpty()) {
                    Text("مفيش ماركات مسجلة للصنف ده.")
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
                        label = { Text("إضافة ماركة للمكتبة") },
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
    var data by remember { mutableStateOf<PricingCopiesComparison?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(sheetId) {
        loading = true
        data = repository.loadPricingCopiesComparison(sheetId)
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val comparison = data
    if (comparison == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            FayrozDetailHeader("مقارنة التسعيرات", "تعذر تحميل الكشف", onBack)
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
                Text("إجمالي النسخ", style = MaterialTheme.typography.titleMedium)
            }

            items(comparison.copies, key = { it.copy.id }) { copy ->
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(copy.copy.placeName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${copy.pricedCount}/${copy.lines.size} بند",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(money(copy.total), style = MaterialTheme.typography.titleMedium)
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
                                            line.brand.ifBlank { "بدون ماركة" },
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
