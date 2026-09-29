package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.requests.data.model.ItemEntity
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.launch

@Composable
fun ItemsScreen(repository: FayrozRepository, onOpenHistory: (Long) -> Unit = {}) {
    val allItems by repository.items.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItemEntity?>(null) }
    var deletingItem by remember { mutableStateOf<ItemEntity?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(allItems, query) {
        if (query.isBlank()) allItems
        else allItems.filter {
            it.name.contains(query, true) ||
                it.code.contains(query, true) ||
                it.brand.contains(query, true) ||
                it.specification.contains(query, true)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            "دليل الأصناف",
            "كل صنف يُسجل مرة واحدة ويمكن تعديله واستخدامه في الكشوف والأسعار",
        )

        PrimaryAction("إضافة صنف", Icons.Outlined.AddBox) { showAdd = true }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("بحث بالاسم أو الكود أو الماركة") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
        )

        if (filtered.isEmpty()) {
            EmptyState(
                Icons.Outlined.Inventory2,
                "لا توجد أصناف",
                if (query.isBlank()) {
                    "أضف أول صنف إلى دليل الأصناف، أو أضفه من داخل كشف جديد."
                } else {
                    "لا توجد نتائج مطابقة للبحث."
                },
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { item ->
                    Card {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${item.code}  •  ${item.defaultUnit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (item.brand.isNotBlank()) {
                                        Text(
                                            "الماركة: ${item.brand}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    if (item.specification.isNotBlank()) {
                                        Text(
                                            item.specification,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                IconButton(onClick = { editingItem = item }) {
                                    Icon(Icons.Outlined.Edit, "تعديل الصنف")
                                }
                                IconButton(onClick = { onOpenHistory(item.id) }) {
                                    Icon(
                                        Icons.Outlined.History,
                                        "تاريخ السعر",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(onClick = { deletingItem = item }) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        "حذف الصنف",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ItemEditorDialog(
            title = "إضافة صنف",
            item = null,
            onDismiss = { showAdd = false },
            onSave = { name, unit, brand, specification ->
                scope.launch {
                    runCatching {
                        repository.addItem(
                            name = name,
                            unit = unit,
                            brand = brand,
                            specification = specification,
                        )
                    }.onSuccess {
                        showAdd = false
                    }.onFailure {
                        message = "تعذر إضافة الصنف: ${it.message ?: "خطأ غير معروف"}"
                    }
                }
            },
        )
    }

    editingItem?.let { item ->
        ItemEditorDialog(
            title = "تعديل الصنف",
            item = item,
            onDismiss = { editingItem = null },
            onSave = { name, unit, brand, specification ->
                scope.launch {
                    val error = repository.updateItemDetails(
                        itemId = item.id,
                        name = name,
                        unit = unit,
                        brand = brand,
                        specification = specification,
                    )
                    if (error == null) {
                        editingItem = null
                    } else {
                        message = error
                    }
                }
            },
        )
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            icon = { Icon(Icons.Outlined.DeleteOutline, null) },
            title = { Text("حذف الصنف") },
            text = {
                Text(
                    "هل تريد حذف «${item.name}» من دليل الأصناف؟\n\n" +
                        "إذا كان الصنف مستخدمًا في كشف أو مرتبطًا بأسعار محفوظة فلن يتم حذفه.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val error = repository.deleteItem(item.id)
                            deletingItem = null
                            message = error ?: "تم حذف الصنف من دليل الأصناف."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) { Text("إلغاء") }
            },
        )
    }

    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("دليل الأصناف") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text("حسنًا") }
            },
        )
    }
}

@Composable
private fun ItemEditorDialog(
    title: String,
    item: ItemEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, unit: String, brand: String, specification: String) -> Unit,
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var unit by remember(item?.id) { mutableStateOf(item?.defaultUnit.orEmpty()) }
    var brand by remember(item?.id) { mutableStateOf(item?.brand.orEmpty()) }
    var specification by remember(item?.id) { mutableStateOf(item?.specification.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    name,
                    { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم الصنف *") },
                    singleLine = true,
                )
                OutlinedTextField(
                    unit,
                    { unit = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("الوحدة *") },
                    placeholder = { Text("عدد / م / م² / م³ / لفة / كجم") },
                    singleLine = true,
                )
                OutlinedTextField(
                    brand,
                    { brand = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("الماركة") },
                    singleLine = true,
                )
                OutlinedTextField(
                    specification,
                    { specification = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("المواصفة / المقاس") },
                    minLines = 2,
                )
                if (item != null) {
                    Text(
                        "كود الصنف: ${item.code}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        unit.trim(),
                        brand.trim(),
                        specification.trim(),
                    )
                },
                enabled = name.isNotBlank() && unit.isNotBlank(),
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
