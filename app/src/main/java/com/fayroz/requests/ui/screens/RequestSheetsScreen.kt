package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PriceCheck
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.requests.data.model.RequestSheetSummary
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    val filtered = remember(sheets, query) {
        if (query.isBlank()) sheets else sheets.filter {
            it.projectName.contains(query, true) || it.sheetNumber.contains(query, true) ||
                it.trade.contains(query, true) || it.craftsmanName.contains(query, true) ||
                it.workLocation.contains(query, true)
        }
    }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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

        if (filtered.isEmpty()) {
            EmptyState(Icons.Outlined.ReceiptLong, "لا توجد كشوف بعد", "اختار الأصناف وحدد الكمية، ومكان الاستخدام اختياري.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(filtered, key = { it.id }) { sheet ->
                    Card(onClick = { onEditSheet(sheet.id) }) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("كشف ${sheet.sheetNumber} • ${sheet.projectName}", style = MaterialTheme.typography.titleSmall)
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
                                IconButton(
                                    onClick = { deleteTarget = sheet },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(Icons.Outlined.DeleteOutline, "حذف", modifier = Modifier.size(19.dp))
                                }
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = { onPriceSheet(sheet.id) },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                ) {
                                    Icon(Icons.Outlined.PriceCheck, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("تسعير الكشف")
                                }

                                OutlinedButton(
                                    onClick = { onEditSheet(sheet.id) },
                                    modifier = Modifier.weight(0.72f).height(40.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                ) {
                                    Icon(Icons.Outlined.EditNote, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تعديل")
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
            text = { Text("سيتم حذف الكشف وبنوده فقط، ولن تُحذف الأصناف من قاعدة الأصناف.") },
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

private fun formatDate(timestamp: Long): String = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(timestamp))
