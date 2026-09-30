package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.fayroz.requests.data.repository.StarterCatalog
import kotlinx.coroutines.launch

@Composable
fun ItemsScreen(repository: FayrozRepository, onOpenHistory: (Long) -> Unit = {}) {
    val allItems by repository.items.collectAsState(initial = emptyList())
    val categories by repository.categories.collectAsState(initial = emptyList())
    val categoryBrands by repository.categoryBrands.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var showCompanies by remember { mutableStateOf(false) }
    var showCategoryOrder by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItemEntity?>(null) }
    var deletingItem by remember { mutableStateOf<ItemEntity?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    val visibleCategories = remember(categories) {
        categories.filterNot { it.name in StarterCatalog.hiddenCategories }
    }
    val visibleCategoryIds = remember(visibleCategories) { visibleCategories.map { it.id }.toSet() }
    val categoryOrderById = remember(visibleCategories) {
        visibleCategories.mapIndexed { index, category -> category.id to index }.toMap()
    }
    val visibleItems = remember(allItems, visibleCategoryIds, categoryOrderById) {
        allItems
            .filter { it.categoryId == null || it.categoryId in visibleCategoryIds }
            .sortedWith(
                compareBy<ItemEntity> { categoryOrderById[it.categoryId] ?: Int.MAX_VALUE }
                    .thenBy { StarterCatalog.itemFamilyRank(it.name) }
                    .thenBy { StarterCatalog.firstMarketNumber(it.name) }
                    .thenBy { it.marketName.ifBlank { StarterCatalog.marketName(it.name) } }
                    .thenBy { it.name }
            )
    }
    val categoryById = remember(visibleCategories) { visibleCategories.associateBy { it.id } }
    val filtered = remember(visibleItems, query, categoryById, selectedCategoryId) {
        visibleItems.filter { item ->
            val categoryMatches = selectedCategoryId == null || item.categoryId == selectedCategoryId
            val categoryName = item.categoryId?.let { categoryById[it]?.name }.orEmpty()
            val queryMatches = query.isBlank() ||
                item.name.contains(query, true) ||
                item.marketName.contains(query, true) ||
                item.code.contains(query, true) ||
                item.specification.contains(query, true) ||
                StarterCatalog.marketLabel(item.name).contains(query, true) ||
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

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { showAdd = true },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.AddBox, null)
                Spacer(Modifier.width(6.dp))
                Text("إضافة صنف")
            }
            OutlinedButton(
                onClick = { showCategoryOrder = true },
            ) {
                Icon(Icons.Outlined.SwapVert, null)
                Spacer(Modifier.width(4.dp))
                Text("ترتيب")
            }
            OutlinedButton(
                onClick = { showCompanies = true },
            ) {
                Icon(Icons.Outlined.Storefront, null)
                Spacer(Modifier.width(4.dp))
                Text("الشركات")
            }
        }

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
            val grouped = filtered.groupBy { it.categoryId }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                grouped.forEach { (categoryId, groupItems) ->
                    item(key = "cat_${categoryId ?: 0}") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                categoryId?.let { categoryById[it]?.name } ?: "أخرى",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "${groupItems.size} صنف",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(groupItems, key = { it.id }) { item ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                            ),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    val marketTitle = item.marketName.ifBlank {
                                        StarterCatalog.marketName(item.name)
                                    }
                                    Text(
                                        marketTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                    )
                                    val detail = buildList {
                                        if (!marketTitle.equals(item.name, ignoreCase = true)) add(item.name)
                                        if (item.specification.isNotBlank()) add(item.specification)
                                        add(item.defaultUnit)
                                    }.joinToString(" • ")
                                    Text(
                                        detail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }

                                IconButton(
                                    onClick = { editingItem = item },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Outlined.Edit, "تعديل الصنف", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onOpenHistory(item.id) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.History,
                                        "تاريخ السعر",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { deletingItem = item },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        "حذف الصنف",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryOrder) {
        CategoryOrderDialog(
            categories = visibleCategories,
            onDismiss = { showCategoryOrder = false },
            onSave = { ids ->
                scope.launch {
                    repository.reorderCategories(ids)
                    showCategoryOrder = false
                }
            },
        )
    }

    if (showCompanies) {
        CatalogCompaniesDialog(
            categories = visibleCategories,
            brands = categoryBrands,
            onDismiss = { showCompanies = false },
        )
    }

    if (showAdd) {
        ItemEditorDialog(
            title = "إضافة صنف",
            item = null,
            categories = visibleCategories,
            onDismiss = { showAdd = false },
            onSave = { marketName, technicalName, unit, specification, categoryId ->
                scope.launch {
                    runCatching {
                        repository.addItem(
                            name = technicalName.ifBlank { marketName },
                            marketName = marketName,
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
            onSave = { marketName, technicalName, unit, specification, categoryId ->
                scope.launch {
                    val error = repository.updateItemDetails(
                        itemId = item.id,
                        name = technicalName.ifBlank { marketName },
                        marketName = marketName,
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
private fun CategoryOrderDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (List<Long>) -> Unit,
) {
    val ordered = remember(categories) {
        mutableStateListOf<CategoryEntity>().apply { addAll(categories) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ترتيب أقسام المكتبة") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(ordered, key = { _, item -> item.id }) { index, category ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${index + 1}. ${category.name}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val moved = ordered.removeAt(index)
                                        ordered.add(index - 1, moved)
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(Icons.Outlined.KeyboardArrowUp, "لأعلى")
                            }
                            IconButton(
                                onClick = {
                                    if (index < ordered.lastIndex) {
                                        val moved = ordered.removeAt(index)
                                        ordered.add(index + 1, moved)
                                    }
                                },
                                enabled = index < ordered.lastIndex,
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(Icons.Outlined.KeyboardArrowDown, "لأسفل")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(ordered.map { it.id }) }) { Text("حفظ الترتيب") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun CatalogCompaniesDialog(
    categories: List<CategoryEntity>,
    brands: List<com.fayroz.requests.data.model.CategoryBrandEntity>,
    onDismiss: () -> Unit,
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    val filteredCategories = remember(categories, selectedCategoryId) {
        if (selectedCategoryId == null) categories
        else categories.filter { it.id == selectedCategoryId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الشركات والماركات") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("الكل") },
                        )
                    }
                    items(categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.name) },
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(filteredCategories, key = { it.id }) { category ->
                        val names = brands
                            .filter { it.categoryId == category.id }
                            .map { it.name }
                            .distinct()
                        if (names.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    names.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
    )
}

@Composable
private fun ItemEditorDialog(
    title: String,
    item: ItemEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        marketName: String,
        technicalName: String,
        unit: String,
        specification: String,
        categoryId: Long?,
    ) -> Unit,
) {
    var marketName by remember(item?.id) {
        mutableStateOf(item?.marketName?.ifBlank { StarterCatalog.marketName(item.name) }.orEmpty())
    }
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
                    marketName,
                    { marketName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم السوق *") },
                    placeholder = { Text("مثال: سلك نحاس شعر 2.5 مم²") },
                    singleLine = true,
                )
                OutlinedTextField(
                    name,
                    { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("الاسم الفني / التجاري") },
                    placeholder = { Text("مثال: H07V-K 450/750V 1×2.5 مم²") },
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
                        marketName.trim(),
                        name.trim(),
                        unit.trim(),
                        specification.trim(),
                        categoryId,
                    )
                },
                enabled = marketName.isNotBlank() && unit.isNotBlank() && categoryId != null,
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
