package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.fayroz.requests.data.model.CategoryEntity
import com.fayroz.requests.data.model.ItemEntity
import com.fayroz.requests.data.model.ProjectEntity
import com.fayroz.requests.data.model.RequestLineDraft
import com.fayroz.requests.data.model.RequestSheetDraft
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.launch

private data class EditableLineUi(
    val localId: Long,
    val categoryId: Long? = null,
    val existingItemId: Long? = null,
    val itemName: String = "",
    val quantity: String = "",
    val unit: String = "",
    val usage: String = "",
    val description: String = "",
    val notes: String = "",
)

@Composable
fun RequestSheetEditorScreen(
    repository: FayrozRepository,
    sheetId: Long?,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onOpenProjects: () -> Unit,
) {
    val projects by repository.projects.collectAsState(initial = emptyList())
    val categories by repository.categories.collectAsState(initial = emptyList())
    val items by repository.items.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var sheetNumber by remember { mutableStateOf("") }
    var trade by remember { mutableStateOf("") }
    var craftsmanName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var sheetDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isLoading by remember { mutableStateOf(sheetId != null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var projectPickerOpen by remember { mutableStateOf(false) }
    var itemPickerIndex by remember { mutableStateOf<Int?>(null) }
    val lines = remember { mutableStateListOf<EditableLineUi>() }

    fun addBlankLine() {
        val nextId = (lines.maxOfOrNull { it.localId } ?: 0L) + 1L
        lines.add(EditableLineUi(localId = nextId))
    }

    LaunchedEffect(sheetId) {
        if (sheetId != null) {
            val draft = repository.loadSheetDraft(sheetId)
            if (draft == null) {
                errorMessage = "تعذر العثور على الكشف."
            } else {
                selectedProjectId = draft.projectId
                sheetNumber = draft.sheetNumber
                trade = draft.trade
                craftsmanName = draft.craftsmanName
                notes = draft.notes
                sheetDate = draft.sheetDate
                lines.clear()
                draft.lines.forEachIndexed { index, line ->
                    lines.add(
                        EditableLineUi(
                            localId = index.toLong() + 1,
                            categoryId = items.firstOrNull { it.id == line.existingItemId }?.categoryId,
                            existingItemId = line.existingItemId,
                            itemName = line.itemName,
                            quantity = formatQuantity(line.quantity),
                            unit = line.unit,
                            usage = line.usage,
                            description = line.lineDescription,
                            notes = line.notes,
                        )
                    )
                }
                if (lines.isEmpty()) addBlankLine()
            }
            isLoading = false
        } else {
            if (lines.isEmpty()) addBlankLine()
            isLoading = false
        }
    }

    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            lines.indices.forEach { index ->
                val line = lines[index]
                if (line.categoryId == null && line.existingItemId != null) {
                    val categoryId = items.firstOrNull { it.id == line.existingItemId }?.categoryId
                    if (categoryId != null) {
                        lines[index] = line.copy(categoryId = categoryId)
                    }
                }
            }
        }
    }

    LaunchedEffect(projects, sheetId) {
        if (sheetId == null && selectedProjectId == null && projects.isNotEmpty()) {
            val project = projects.first()
            selectedProjectId = project.id
            sheetNumber = repository.suggestedSheetNumber(project.id)
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    if (projects.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Business, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(14.dp))
            Text("أضف مشروعًا أولًا", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text("كل كشف طلبات يجب أن يتبع مشروعًا.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onOpenProjects) { Text("فتح المشروعات") }
            TextButton(onClick = onCancel) { Text("رجوع") }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) { Icon(Icons.Outlined.ArrowForward, "رجوع") }
            Column(Modifier.weight(1f)) {
                Text(if (sheetId == null) "كشف طلبات جديد" else "تعديل كشف الطلبات", style = MaterialTheme.typography.titleLarge)
                Text("أدخل الكشف كما وصلك من الصنايعي", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = {
                    val validation = validateSheet(selectedProjectId, sheetNumber, lines)
                    errorMessage = validation
                    val projectId = selectedProjectId
                    if (validation == null && projectId != null) {
                        val validLines = lines.filter { it.existingItemId != null && (it.quantity.toDoubleOrNull() ?: 0.0) > 0 }
                        scope.launch {
                            isSaving = true
                            runCatching {
                                repository.saveSheet(
                                    RequestSheetDraft(
                                        id = sheetId ?: 0L,
                                        projectId = projectId,
                                        sheetNumber = sheetNumber,
                                        sheetDate = sheetDate,
                                        trade = trade,
                                        craftsmanName = craftsmanName,
                                        notes = notes,
                                        lines = validLines.map {
                                            RequestLineDraft(
                                                existingItemId = it.existingItemId,
                                                itemName = it.itemName,
                                                quantity = it.quantity.toDouble(),
                                                unit = it.unit,
                                                usage = it.usage,
                                                lineDescription = it.description,
                                                notes = it.notes,
                                            )
                                        },
                                    )
                                )
                            }.onSuccess {
                                onDone()
                            }.onFailure {
                                errorMessage = if (it.message?.contains("UNIQUE", true) == true) {
                                    "رقم الكشف مستخدم بالفعل داخل هذا المشروع. غيّر رقم الكشف."
                                } else {
                                    "تعذر حفظ الكشف: ${it.message ?: "خطأ غير معروف"}"
                                }
                            }
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Save, null)
                }
                Spacer(Modifier.width(6.dp))
                Text("حفظ")
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("بيانات الكشف", style = MaterialTheme.typography.titleMedium)
                        val project = projects.firstOrNull { it.id == selectedProjectId }
                        OutlinedButton(onClick = { projectPickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Business, null)
                            Spacer(Modifier.width(8.dp))
                            Text(project?.name ?: "اختيار المشروع")
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Outlined.KeyboardArrowDown, null)
                        }
                        OutlinedTextField(
                            sheetNumber,
                            { sheetNumber = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("رقم الكشف *") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            trade,
                            { trade = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("التخصص / نوع الشغل") },
                            placeholder = { Text("كهرباء، سباكة، نجارة...") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            craftsmanName,
                            { craftsmanName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("اسم الصنايعي") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            notes,
                            { notes = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("ملاحظات الكشف") },
                            minLines = 2,
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("بنود الكشف", style = MaterialTheme.typography.titleLarge)
                        Text("اختار القسم ثم الصنف، والوحدة تتحدد تلقائيًا", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { addBlankLine() }) {
                        Icon(Icons.Outlined.Add, null)
                        Spacer(Modifier.width(5.dp))
                        Text("بند")
                    }
                }
            }

            itemsIndexed(lines, key = { _, line -> line.localId }) { index, line ->
                RequestLineEditorCard(
                    index = index,
                    line = line,
                    categories = categories,
                    onChange = { lines[index] = it },
                    onPickItem = { if (line.categoryId != null) itemPickerIndex = index },
                    onDelete = { if (lines.size > 1) lines.removeAt(index) else lines[index] = EditableLineUi(localId = line.localId) },
                )
            }

            errorMessage?.let { message ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (projectPickerOpen) {
        ProjectPickerDialog(
            projects = projects,
            onDismiss = { projectPickerOpen = false },
            onSelect = { project ->
                selectedProjectId = project.id
                projectPickerOpen = false
                if (sheetId == null) {
                    scope.launch { sheetNumber = repository.suggestedSheetNumber(project.id) }
                }
            },
        )
    }

    itemPickerIndex?.let { lineIndex ->
        if (lineIndex in lines.indices) {
            ItemPickerDialog(
                items = items.filter { it.categoryId == lines[lineIndex].categoryId },
                categories = categories,
                onDismiss = { itemPickerIndex = null },
                onSelect = { item ->
                    val current = lines[lineIndex]
                    lines[lineIndex] = current.copy(
                        categoryId = item.categoryId,
                        existingItemId = item.id,
                        itemName = item.name,
                        unit = item.defaultUnit,
                    )
                    itemPickerIndex = null
                },
            )
        }
    }
}

@Composable
private fun RequestLineEditorCard(
    index: Int,
    line: EditableLineUi,
    categories: List<CategoryEntity>,
    onChange: (EditableLineUi) -> Unit,
    onPickItem: () -> Unit,
    onDelete: () -> Unit,
) {
    var categoryMenuOpen by remember(line.localId) { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.id == line.categoryId }

    Card {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "بند ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, "حذف البند")
                }
            }

            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { categoryMenuOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Category, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        selectedCategory?.name ?: "اختيار القسم *",
                        modifier = Modifier.weight(1f),
                    )
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
                                categoryMenuOpen = false
                                if (line.categoryId != category.id) {
                                    onChange(
                                        line.copy(
                                            categoryId = category.id,
                                            existingItemId = null,
                                            itemName = "",
                                            unit = "",
                                        )
                                    )
                                }
                            },
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onPickItem,
                modifier = Modifier.fillMaxWidth(),
                enabled = line.categoryId != null,
            ) {
                Icon(Icons.Outlined.Inventory2, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (line.existingItemId == null) "اختيار الصنف *" else line.itemName,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.KeyboardArrowDown, null)
            }

            if (line.categoryId == null) {
                Text(
                    "اختار القسم الأول، وبعدها هتظهر أصناف القسم.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (line.existingItemId != null) {
                Text(
                    "الوحدة: ${line.unit}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            OutlinedTextField(
                value = line.quantity,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("\\d*(\\.\\d*)?"))) {
                        onChange(line.copy(quantity = value))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("الكمية *") },
                placeholder = { Text("أدخل الكمية") },
                suffix = {
                    if (line.unit.isNotBlank()) Text(line.unit)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = line.existingItemId != null,
            )

            OutlinedTextField(
                value = line.usage,
                onValueChange = { onChange(line.copy(usage = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ده لإيه / مكان الاستخدام") },
                placeholder = { Text("مثال: تأسيس كهرباء الدور الأول") },
                singleLine = true,
            )

            OutlinedTextField(
                value = line.description,
                onValueChange = { onChange(line.copy(description = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("وصف أو مواصفة إضافية") },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun ProjectPickerDialog(
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onSelect: (ProjectEntity) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار المشروع") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(projects, key = { _, item -> item.id }) { _, project ->
                    Card(onClick = { onSelect(project) }) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(project.name, style = MaterialTheme.typography.titleMedium)
                            if (project.location.isNotBlank()) Text(project.location, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun ItemPickerDialog(
    items: List<ItemEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSelect: (ItemEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(items, query) {
        items.filter { item ->
            query.isBlank() ||
                item.name.contains(query, true) ||
                item.code.contains(query, true) ||
                item.brand.contains(query, true) ||
                item.specification.contains(query, true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار الصنف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("بحث في الأصناف") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                )

                if (filtered.isEmpty()) {
                    Text("لا توجد أصناف مطابقة. أضف الصنف من «دليل الأصناف» ثم ارجع للكشف.")
                } else {
                    LazyColumn(
                        Modifier.heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(filtered, key = { _, item -> item.id }) { _, item ->
                            Card(onClick = { onSelect(item) }) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                                        if (item.specification.isNotBlank()) {
                                            Text(
                                                item.specification,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Text(item.defaultUnit, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

private fun validateSheet(projectId: Long?, sheetNumber: String, lines: List<EditableLineUi>): String? {
    if (projectId == null) return "اختار المشروع أولًا."
    if (sheetNumber.isBlank()) return "اكتب رقم الكشف."
    val validLines = lines.filter { it.existingItemId != null || it.quantity.isNotBlank() }
    if (validLines.isEmpty()) return "أضف بندًا واحدًا على الأقل."
    val invalid = validLines.firstOrNull {
        it.categoryId == null ||
            it.existingItemId == null ||
            (it.quantity.toDoubleOrNull() ?: 0.0) <= 0.0 ||
            it.unit.isBlank()
    }
    if (invalid != null) return "راجع البنود: اختار القسم والصنف وأدخل الكمية لكل بند."
    return null
}

private fun formatQuantity(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
