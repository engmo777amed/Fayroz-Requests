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
                it.trade.contains(query, true) || it.craftsmanName.contains(query, true)
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { sheet ->
                    Card(onClick = { onEditSheet(sheet.id) }) {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text("كشف ${sheet.sheetNumber}", style = MaterialTheme.typography.titleMedium)
                                    Text(sheet.projectName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Row {
                                    IconButton(onClick = { onPriceSheet(sheet.id) }) { Icon(Icons.Outlined.PriceCheck, "تسعير") }
                                    IconButton(onClick = { onEditSheet(sheet.id) }) { Icon(Icons.Outlined.EditNote, "تعديل") }
                                    IconButton(onClick = { deleteTarget = sheet }) { Icon(Icons.Outlined.DeleteOutline, "حذف") }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sheet.trade.ifBlank { "بدون تخصص" }, style = MaterialTheme.typography.bodySmall)
                                Text("${sheet.lineCount} بند", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            }
                            if (sheet.craftsmanName.isNotBlank()) Text("الصنايعي: ${sheet.craftsmanName}", style = MaterialTheme.typography.bodySmall)
                            Text(formatDate(sheet.sheetDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
