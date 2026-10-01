package com.fayroz.requests.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fayroz.requests.data.model.ItemEntity
import com.fayroz.requests.data.model.PriceListEntity
import com.fayroz.requests.data.model.PriceListEntryDetail
import com.fayroz.requests.data.model.SupplierEntity
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun PriceListEntriesScreen(
    repository: FayrozRepository,
    priceListId: Long,
    onBack: () -> Unit,
    onImport: () -> Unit,
) {
    val priceList by produceState<PriceListEntity?>(initialValue = null, priceListId) {
        value = repository.getPriceList(priceListId)
    }
    val supplier by produceState<SupplierEntity?>(initialValue = null, priceList?.supplierId) {
        value = priceList?.let { repository.getSupplier(it.supplierId) }
    }
    val entries by repository.priceListEntries(priceListId).collectAsState(initial = emptyList())
    val allItems by repository.items.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<PriceListEntryDetail?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<PriceListEntryDetail?>(null) }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ScreenHeader(
                priceList?.name ?: "بنود قائمة الأسعار",
                supplier?.let { "${it.name} • الخصم الافتراضي ${formatDiscount(it.defaultDiscountPercent)}" },
            )
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "رجوع") }
        }

        PrimaryAction("إضافة سعر صنف", Icons.Outlined.AddShoppingCart) { showAdd = true }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(Icons.Outlined.UploadFile, null)
            Spacer(Modifier.width(8.dp))
            Text("استيراد Excel / CSV")
        }

        if (entries.isEmpty()) {
            EmptyState(Icons.Outlined.PriceChange, "الليستة فارغة", "أضف سعر الصنف وسيتم حساب الخصم والسعر الصافي تلقائيًا.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(entries, key = { it.priceId }) { entry ->
                    Card(onClick = { editing = entry }) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.itemName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        buildString {
                                            append(entry.itemCode).append(" • ").append(entry.unit)
                                            if (entry.brand.isNotBlank()) append(" • ").append(entry.brand)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row {
                                    IconButton(onClick = { editing = entry }) { Icon(Icons.Outlined.Edit, "تعديل") }
                                    IconButton(onClick = { deleteTarget = entry }) { Icon(Icons.Outlined.DeleteOutline, "حذف") }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                PriceValue("سعر الليستة", money(entry.listPrice))
                                PriceValue("الخصم", formatDiscount(entry.appliedDiscountPercent))
                                PriceValue("الصافي", money(entry.netPrice), highlight = true)
                            }
                            if (entry.notes.isNotBlank()) Text(entry.notes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        PriceEntryDialog(
            repository = repository,
            priceListId = priceListId,
            allItems = allItems,
            initialEntry = null,
            onDismiss = { showAdd = false },
            onSaved = { showAdd = false },
        )
    }

    editing?.let { entry ->
        PriceEntryDialog(
            repository = repository,
            priceListId = priceListId,
            allItems = allItems,
            initialEntry = entry,
            onDismiss = { editing = null },
            onSaved = { editing = null },
        )
    }

    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف سعر ${entry.itemName}؟") },
            text = { Text("سيتم حذف هذا السعر من الليستة الحالية فقط.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deletePrice(entry.priceId) }
                    deleteTarget = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("إلغاء") } },
        )
    }
}

@Composable
private fun PriceValue(label: String, value: String, highlight: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = if (highlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PriceEntryDialog(
    repository: FayrozRepository,
    priceListId: Long,
    allItems: List<ItemEntity>,
    initialEntry: PriceListEntryDetail?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedItem by remember(initialEntry?.priceId) { mutableStateOf<ItemEntity?>(null) }
    var listPrice by remember(initialEntry) { mutableStateOf(initialEntry?.listPrice?.toString() ?: "") }
    var discount by remember(initialEntry) { mutableStateOf(initialEntry?.appliedDiscountPercent?.toString() ?: "") }
    var brand by remember(initialEntry) { mutableStateOf(initialEntry?.brand ?: "") }
    var priceUnit by remember(initialEntry) { mutableStateOf(initialEntry?.unit ?: "") }
    var rememberDiscount by remember { mutableStateOf(false) }
    var notes by remember(initialEntry) { mutableStateOf(initialEntry?.notes ?: "") }
    var showItemPicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(initialEntry?.itemId, allItems.size) {
        if (initialEntry != null && selectedItem == null) {
            selectedItem = allItems.firstOrNull { it.id == initialEntry.itemId }
        }
        if (priceUnit.isBlank()) {
            selectedItem?.let { priceUnit = it.defaultUnit }
        }
    }

    LaunchedEffect(selectedItem?.id, initialEntry?.priceId) {
        val item = selectedItem ?: return@LaunchedEffect
        if (initialEntry == null) {
            if (priceUnit.isBlank()) priceUnit = item.defaultUnit
            discount = repository.suggestedDiscountPercent(
                supplierId = repository.getPriceList(priceListId)?.supplierId ?: return@LaunchedEffect,
                itemId = item.id,
            ).toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEntry == null) "إضافة سعر" else "تعديل السعر") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { if (initialEntry == null) showItemPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Inventory2, null)
                    Spacer(Modifier.width(8.dp))
                    Text(selectedItem?.name ?: "اختيار الصنف")
                }
                OutlinedTextField(
                    value = listPrice,
                    onValueChange = { listPrice = it },
                    label = { Text("سعر الليستة *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("الماركة") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = priceUnit,
                        onValueChange = { priceUnit = it },
                        modifier = Modifier.weight(0.72f),
                        label = { Text("وحدة السعر") },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("الخصم %") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("يظهر تلقائيًا من اتفاق المورد ويمكن تغييره هنا") },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("حفظ الخصم للصنف")
                        Text("يُستخدم تلقائيًا في الأسعار التالية", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Checkbox(checked = rememberDiscount, onCheckedChange = { rememberDiscount = it })
                }
                val lp = listPrice.toDoubleOrNull() ?: 0.0
                val dp = discount.toDoubleOrNull() ?: 0.0
                Text("الصافي: ${money(lp * (1.0 - dp.coerceIn(0.0, 100.0) / 100.0))}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = selectedItem ?: return@Button
                    saving = true
                    scope.launch {
                        repository.upsertPriceListEntry(
                            priceListId = priceListId,
                            itemId = item.id,
                            listPrice = listPrice.toDoubleOrNull() ?: 0.0,
                            discountPercent = discount.toDoubleOrNull(),
                            rememberAsItemDiscount = rememberDiscount,
                            brand = brand,
                            priceUnit = priceUnit.ifBlank { item.defaultUnit },
                            notes = notes,
                        )
                        saving = false
                        onSaved()
                    }
                },
                enabled = selectedItem != null &&
                    priceUnit.isNotBlank() &&
                    (listPrice.toDoubleOrNull() ?: -1.0) >= 0.0 &&
                    (discount.toDoubleOrNull() ?: -1.0) in 0.0..100.0 &&
                    !saving,
            ) { Text(if (saving) "جارٍ الحفظ" else "حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )

    if (showItemPicker) {
        ItemPickerDialog(
            items = allItems,
            onDismiss = { showItemPicker = false },
            onSelect = {
                selectedItem = it
                showItemPicker = false
            },
        )
    }
}

@Composable
private fun ItemPickerDialog(
    items: List<ItemEntity>,
    onDismiss: () -> Unit,
    onSelect: (ItemEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) {
        if (query.isBlank()) items else items.filter {
            it.name.contains(query, true) ||
                it.marketName.contains(query, true) ||
                it.code.contains(query, true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("اختيار الصنف", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("بحث") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                )
                LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.marketName.ifBlank { item.name }) },
                            supportingContent = {
                                val technical = if (item.marketName.isNotBlank() && item.marketName != item.name) " • ${item.name}" else ""
                                Text("${item.code} • ${item.defaultUnit}$technical")
                            },
                            modifier = Modifier.clickable { onSelect(item) },
                        )
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("إلغاء") }
            }
        }
    }
}

private val decimalFormat = DecimalFormat("#,##0.00")
private fun money(value: Double): String = "${decimalFormat.format(value)} ج"
private fun formatDiscount(value: Double): String = if (value % 1.0 == 0.0) "${value.toInt()}%" else "${decimalFormat.format(value)}%"
