package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.requests.data.model.PriceListSummary
import com.fayroz.requests.data.model.SupplierEntity
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SupplierPriceListsScreen(
    repository: FayrozRepository,
    supplierId: Long,
    onBack: () -> Unit,
    onOpenPriceList: (Long) -> Unit,
) {
    val supplier by produceState<SupplierEntity?>(initialValue = null, supplierId) {
        value = repository.getSupplier(supplierId)
    }
    val priceLists by repository.priceLists(supplierId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<PriceListSummary?>(null) }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ScreenHeader(
                supplier?.name ?: "قوائم الأسعار",
                "كل ليستة تُحفظ بتاريخها ولا تستبدل الليستات القديمة",
            )
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "رجوع") }
        }

        supplier?.let {
            Card {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("الخصم الافتراضي", style = MaterialTheme.typography.bodySmall)
                        Text("${it.defaultDiscountPercent}%", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Column {
                        Text(if (it.approved) "مورد معتمد" else "غير معتمد", style = MaterialTheme.typography.labelLarge)
                        if (it.specialty.isNotBlank()) Text(it.specialty, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        PrimaryAction("قائمة أسعار جديدة", Icons.Outlined.PostAdd) { showAdd = true }

        if (priceLists.isEmpty()) {
            EmptyState(Icons.Outlined.ReceiptLong, "لا توجد قوائم أسعار", "أنشئ أول ليستة للمورد ثم أضف أسعار الأصناف إليها.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(priceLists, key = { it.id }) { list ->
                    Card(onClick = { onOpenPriceList(list.id) }) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(list.name, style = MaterialTheme.typography.titleMedium)
                                Text(formatDate(list.effectiveDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (list.sourceReference.isNotBlank()) Text("المرجع: ${list.sourceReference}", style = MaterialTheme.typography.bodySmall)
                                Text("${list.entryCount} صنف", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            }
                            Column {
                                IconButton(onClick = { onOpenPriceList(list.id) }) { Icon(Icons.Outlined.EditNote, "فتح") }
                                IconButton(onClick = { deleteTarget = list }) { Icon(Icons.Outlined.DeleteOutline, "حذف") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var reference by remember { mutableStateOf("") }
        var dateText by remember { mutableStateOf(formatDate(System.currentTimeMillis())) }
        val parsedDate = remember(dateText) { parseDate(dateText) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("قائمة أسعار جديدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("اسم الليستة *") }, singleLine = true, placeholder = { Text("مثال: ليستة سبتمبر 2026") })
                    OutlinedTextField(reference, { reference = it }, label = { Text("مرجع / رقم الليستة") }, singleLine = true)
                    OutlinedTextField(
                        dateText,
                        { dateText = it },
                        label = { Text("تاريخ الليستة *") },
                        placeholder = { Text("dd/MM/yyyy") },
                        singleLine = true,
                        isError = parsedDate == null,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch { repository.addPriceList(supplierId, name, effectiveDate = parsedDate ?: return@launch, sourceReference = reference) }
                    showAdd = false
                }, enabled = name.isNotBlank() && parsedDate != null) { Text("إنشاء") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("إلغاء") } },
        )
    }

    deleteTarget?.let { list ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف ${list.name}؟") },
            text = { Text("سيتم حذف أسعار هذه الليستة. القوائم والتواريخ الأخرى ستظل محفوظة.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deletePriceList(list.id) }
                    deleteTarget = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("إلغاء") } },
        )
    }
}

private fun formatDate(timestamp: Long): String = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(timestamp))

private fun parseDate(value: String): Long? = runCatching {
    java.time.LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()
