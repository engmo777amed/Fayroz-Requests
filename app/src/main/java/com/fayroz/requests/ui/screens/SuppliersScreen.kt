package com.fayroz.requests.ui.screens

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
import com.fayroz.requests.data.model.SupplierEntity
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.launch

@Composable
fun SuppliersScreen(
    repository: FayrozRepository,
    onOpenSupplier: (Long) -> Unit,
) {
    val suppliers by repository.suppliers.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SupplierEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<SupplierEntity?>(null) }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("الموردون", "المورد المعتمد + ليستات الأسعار + الخصم الافتراضي")
        PrimaryAction("إضافة مورد", Icons.Outlined.PersonAdd) { showAdd = true }

        if (suppliers.isEmpty()) {
            EmptyState(Icons.Outlined.Storefront, "لا يوجد موردون بعد", "أضف المورد ونسبة الخصم الافتراضية، ثم أنشئ قوائم الأسعار الخاصة به.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(suppliers, key = { it.id }) { supplier ->
                    Card(onClick = { onOpenSupplier(supplier.id) }) {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(supplier.name, style = MaterialTheme.typography.titleMedium)
                                        if (supplier.approved) {
                                            SuggestionChip(onClick = {}, label = { Text("معتمد") })
                                        }
                                    }
                                    if (supplier.specialty.isNotBlank()) Text(supplier.specialty, style = MaterialTheme.typography.bodySmall)
                                    Text("الخصم الافتراضي: ${formatPercent(supplier.defaultDiscountPercent)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                                    if (supplier.phone.isNotBlank()) Text(supplier.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column {
                                    IconButton(onClick = { onOpenSupplier(supplier.id) }) { Icon(Icons.Outlined.PriceChange, "قوائم الأسعار") }
                                    Row {
                                        IconButton(onClick = { editing = supplier }) { Icon(Icons.Outlined.Edit, "تعديل") }
                                        IconButton(onClick = { deleteTarget = supplier }) { Icon(Icons.Outlined.DeleteOutline, "حذف") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        SupplierDialog(
            title = "إضافة مورد",
            onDismiss = { showAdd = false },
            onSave = { data ->
                scope.launch {
                    repository.addSupplier(
                        name = data.name,
                        phone = data.phone,
                        address = data.address,
                        specialty = data.specialty,
                        defaultDiscountPercent = data.discount,
                        approved = data.approved,
                        notes = data.notes,
                    )
                }
                showAdd = false
            },
        )
    }

    editing?.let { supplier ->
        SupplierDialog(
            title = "تعديل المورد",
            initial = SupplierFormData(
                name = supplier.name,
                phone = supplier.phone,
                address = supplier.address,
                specialty = supplier.specialty,
                discount = supplier.defaultDiscountPercent,
                approved = supplier.approved,
                notes = supplier.notes,
            ),
            onDismiss = { editing = null },
            onSave = { data ->
                scope.launch {
                    repository.updateSupplier(
                        supplier.copy(
                            name = data.name.trim(),
                            phone = data.phone.trim(),
                            address = data.address.trim(),
                            specialty = data.specialty.trim(),
                            defaultDiscountPercent = data.discount,
                            approved = data.approved,
                            notes = data.notes.trim(),
                        )
                    )
                }
                editing = null
            },
        )
    }

    deleteTarget?.let { supplier ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف المورد؟") },
            text = { Text("سيتم حذف المورد وقوائم أسعاره وسجل الأسعار المرتبط به.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteSupplier(supplier) }
                    deleteTarget = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("إلغاء") } },
        )
    }
}

private data class SupplierFormData(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val specialty: String = "",
    val discount: Double = 0.0,
    val approved: Boolean = true,
    val notes: String = "",
)

@Composable
private fun SupplierDialog(
    title: String,
    initial: SupplierFormData = SupplierFormData(),
    onDismiss: () -> Unit,
    onSave: (SupplierFormData) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var phone by remember { mutableStateOf(initial.phone) }
    var address by remember { mutableStateOf(initial.address) }
    var specialty by remember { mutableStateOf(initial.specialty) }
    var discount by remember { mutableStateOf(if (initial.discount == 0.0) "" else initial.discount.toString()) }
    var approved by remember { mutableStateOf(initial.approved) }
    var notes by remember { mutableStateOf(initial.notes) }
    val discountValue = discount.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("اسم المورد *") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("الهاتف") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(specialty, { specialty = it }, label = { Text("التخصص") }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("العنوان") }, singleLine = true)
                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("الخصم الافتراضي %") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("يُطبق تلقائيًا ما لم يوجد خصم خاص للصنف") },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("مورد معتمد")
                    Switch(checked = approved, onCheckedChange = { approved = it })
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(SupplierFormData(name, phone, address, specialty, discountValue, approved, notes))
                },
                enabled = name.isNotBlank() && discountValue in 0.0..100.0,
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

private fun formatPercent(value: Double): String = if (value % 1.0 == 0.0) "${value.toInt()}%" else "${"%.2f".format(value)}%"
