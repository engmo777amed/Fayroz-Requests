package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.launch

@Composable
fun ItemsScreen(repository: FayrozRepository, onOpenHistory: (Long) -> Unit = {}) {
    val allItems by repository.items.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(allItems, query) {
        if (query.isBlank()) allItems else allItems.filter { it.name.contains(query, true) || it.code.contains(query, true) }
    }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("قاعدة الأصناف", "الصنف يُسجل مرة واحدة ويُستخدم في كل الكشوف والأسعار")
        PrimaryAction("إضافة صنف", Icons.Outlined.AddBox) { showAdd = true }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("بحث بالاسم أو الكود") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
        )

        if (filtered.isEmpty()) {
            EmptyState(Icons.Outlined.Inventory2, "لا توجد أصناف", if (query.isBlank()) "أضف صنفًا أو ابدأ من كشف جديد وسيُضاف الصنف تلقائيًا." else "لا توجد نتائج مطابقة للبحث.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { item ->
                    Card(onClick = { onOpenHistory(item.id) }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text("${item.code}  •  ${item.defaultUnit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (item.brand.isNotBlank()) Text("الماركة: ${item.brand}", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Outlined.History, "تاريخ السعر", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var unit by remember { mutableStateOf("") }
        var brand by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("إضافة صنف") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("اسم الصنف *") }, singleLine = true)
                    OutlinedTextField(unit, { unit = it }, label = { Text("الوحدة *") }, singleLine = true)
                    OutlinedTextField(brand, { brand = it }, label = { Text("الماركة") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { repository.addItem(name, unit, brand = brand) }
                        showAdd = false
                    },
                    enabled = name.isNotBlank() && unit.isNotBlank(),
                ) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("إلغاء") } },
        )
    }
}
