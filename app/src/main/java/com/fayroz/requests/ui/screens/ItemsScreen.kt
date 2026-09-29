package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.requests.data.model.CategoryEntity
import com.fayroz.requests.data.model.ItemEntity
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.launch

@Composable
fun ItemsScreen(repository: FayrozRepository, onOpenHistory: (Long) -> Unit = {}) {
    val allItems by repository.items.collectAsState(initial = emptyList())
    val categories by repository.categories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItemEntity?>(null) }
    var deletingItem by remember { mutableStateOf<ItemEntity?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    val visibleCategories = remember(categories) {
        categories.filterNot { it.name in com.fayroz.requests.data.repository.StarterCatalog.hiddenCategories }
    }
    val visibleCategoryIds = remember(visibleCategories) { visibleCategories.map { it.id }.toSet() }
    val visibleItems = remember(allItems, visibleCategoryIds) {
        allItems.filter { it.categoryId == null || it.categoryId in visibleCategoryIds }
    }
    val categoryById = remember(visibleCategories) { visibleCategories.associateBy { it.id } }
    val filtered = remember(visibleItems, query, categoryById, selectedCategoryId) {
        visibleItems.filter { item ->
            val categoryMatches = selectedCategoryId == null || item.categoryId == selectedCategoryId
            val categoryName = item.categoryId?.let { categoryById[it]?.name }.orEmpty()
            val queryMatches = query.isBlank() ||
                item.name.contains(query, true) ||
                item.code.contains(query, true) ||
                item.specification.contains(query, true) ||
                categoryName.contains(query, true)
            categoryMatches && queryMatches
        }
    }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            "دليل الأصناف",
            "الأصناف مرتبة حسب القسم ويمكن إضافتها أو تعديلها أو حذف غير المستخدم منها",
        )

        PrimaryAction("إضافة صنف", Icons.Outlined.AddBox) { showAdd = true }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("بحث في دليل الأصناف") },
            placeholder = { Text("اسم الصنف أو اسم الصنايعي") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { selectedCategoryId = null },
                    label = { Text("الكل") },
                )
            }
            items(visibleCategories, key = { it.id }) { category ->
                FilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = {
                        selectedCategoryId = if (selectedCategoryId == category.id) null else category.id
                    },
                    label = { Text(category.name) },
                )
            }
        }

        Text(
            "عدد الأصناف: ${filtered.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (filtered.isEmpty()) {
            EmptyState(
                Icons.Outlined.Inventory2,
                "لا توجد أصناف",
                if (query.isBlank()) {
                    "أضف أول صنف إلى دليل الأصناف."
                } else {
                    "لا توجد نتائج مطابقة للبحث."
                },
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { item ->
                    Card {
                        Row(
                            Modifier.fillMaxWidth().padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                val categoryName = item.categoryId?.let { categoryById[it]?.name }
                                if (!categoryName.isNullOrBlank()) {
                                    Text(
                                        categoryName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                                if (item.specification.isNotBlank()) {
                                    Text(
                                        item.specification,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    "الوحدة: ${item.defaultUnit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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

    if (showAdd) {
        ItemEditorDialog(
            title = "إضافة صنف",
            item = null,
            categories = visibleCategories,
            onDismiss = { showAdd = false },
            onSave = { name, unit, specification, categoryId ->
                scope.launch {
                    runCatching {
                        repository.addItem(
                            name = name,
                            unit = unit,
                            specification = specification,
                            categoryId = categoryId,
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
            categories = visibleCategories,
            onDismiss = { editingItem = null },
            onSave = { name, unit, specification, categoryId ->
                scope.launch {
                    val error = repository.updateItemDetails(
                        itemId = item.id,
                        name = name,
                        unit = unit,
                        brand = item.brand,
                        specification = specification,
                        categoryId = categoryId,
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
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        unit: String,
        specification: String,
        categoryId: Long?,
    ) -> Unit,
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var unit by remember(item?.id) { mutableStateOf(item?.defaultUnit.orEmpty()) }
    var specification by remember(item?.id) { mutableStateOf(item?.specification.orEmpty()) }
    var categoryId by remember(item?.id) { mutableStateOf(item?.categoryId) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    val selectedCategory = categories.firstOrNull { it.id == categoryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { categoryMenuOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Category, null)
                        Spacer(Modifier.width(8.dp))
                        Text(selectedCategory?.name ?: "اختيار القسم *")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Outlined.KeyboardArrowDown, null)
                    }
                    DropdownMenu(
                        expanded = categoryMenuOpen,
                        onDismissRequest = { categoryMenuOpen = false },
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    categoryId = category.id
                                    categoryMenuOpen = false
                                },
                            )
                        }
                    }
                }

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
                        specification.trim(),
                        categoryId,
                    )
                },
                enabled = name.isNotBlank() && unit.isNotBlank() && categoryId != null,
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
