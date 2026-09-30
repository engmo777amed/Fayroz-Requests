package com.fayroz.requests.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fayroz.requests.data.model.CategoryEntity
import com.fayroz.requests.data.model.ItemEntity
import com.fayroz.requests.data.model.ProjectEntity
import com.fayroz.requests.data.model.RequestLineDraft
import com.fayroz.requests.data.model.RequestSheetDraft
import com.fayroz.requests.data.repository.FayrozRepository
import com.fayroz.requests.data.repository.StarterCatalog
import com.fayroz.requests.export.FayrozReports
import com.fayroz.requests.export.ShareFiles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private data class EditableLineUi(
    val localId: Long,
    val categoryId: Long? = null,
    val existingItemId: Long? = null,
    val itemName: String = "",
    val quantity: String = "",
    val unit: String = "",
    val brand: String = "",
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
    val allCategories by repository.categories.collectAsState(initial = emptyList())
    val allItems by repository.items.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var attachmentUri by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            attachmentUri = uri.toString()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            runCatching {
                val dir = File(context.filesDir, "attachments").apply { mkdirs() }
                val file = File(dir, "request_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }
                attachmentUri = file.absolutePath
            }.onFailure {
                Toast.makeText(context, "تعذر حفظ الصورة.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val id = sheetId
        if (uri != null && id != null) {
            scope.launch {
                runCatching {
                    val savedSheet = repository.getSheet(id) ?: error("الكشف غير موجود")
                    val project = repository.getProject(savedSheet.projectId) ?: error("المشروع غير موجود")
                    val savedLines = repository.getRequestLines(id)
                    context.contentResolver.openOutputStream(uri)?.use {
                        FayrozReports.writeRequestSheetPdf(it, project, savedSheet, savedLines)
                    } ?: error("تعذر فتح الملف")
                }.onSuccess {
                    Toast.makeText(context, "تم تصدير PDF ✓", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "تعذر تصدير PDF", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val categories = remember(allCategories) {
        allCategories.filterNot { it.name in StarterCatalog.hiddenCategories }
    }
    val visibleCategoryIds = remember(categories) { categories.map { it.id }.toSet() }
    val items = remember(allItems, visibleCategoryIds) {
        allItems.filter { it.categoryId == null || it.categoryId in visibleCategoryIds }
    }
    val itemById = remember(items) { items.associateBy { it.id } }
    val categoryById = remember(categories) { categories.associateBy { it.id } }

    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var sheetNumber by remember { mutableStateOf("") }
    var trade by remember { mutableStateOf("") }
    var craftsmanName by remember { mutableStateOf("") }
    var workLocation by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var sheetDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isLoading by remember { mutableStateOf(sheetId != null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveConfirmed by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var projectPickerOpen by remember { mutableStateOf(false) }
    var multiPickerOpen by remember { mutableStateOf(false) }
    val lines = remember { mutableStateListOf<EditableLineUi>() }

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
                workLocation = draft.workLocation
                attachmentUri = draft.attachmentUri
                notes = draft.notes
                sheetDate = draft.sheetDate
                lines.clear()
                draft.lines.forEachIndexed { index, line ->
                    lines.add(
                        EditableLineUi(
                            localId = index.toLong() + 1,
                            categoryId = allItems.firstOrNull { it.id == line.existingItemId }?.categoryId,
                            existingItemId = line.existingItemId,
                            itemName = line.itemName,
                            quantity = formatQuantity(line.quantity),
                            unit = line.unit,
                            brand = line.brand,
                            usage = line.usage,
                            description = line.lineDescription,
                            notes = line.notes,
                        )
                    )
                }
            }
        }
        isLoading = false
    }

    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            lines.indices.forEach { index ->
                val line = lines[index]
                if (line.categoryId == null && line.existingItemId != null) {
                    itemById[line.existingItemId]?.categoryId?.let { categoryId ->
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (projects.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Outlined.Business,
                null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(14.dp))
            Text("أضف مشروعًا أولًا", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text("كل كشف طلبات لازم يتبع مشروع.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            IconButton(onClick = onCancel) {
                Icon(Icons.Outlined.ArrowForward, "رجوع")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (sheetId == null) "كشف طلبات جديد" else "تعديل كشف الطلبات",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    if (lines.isEmpty()) "اختار الأصناف مرة واحدة وبعدها كمل الكميات"
                    else "${lines.size} صنف في الكشف",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = {
                    val validation = validateSheet(selectedProjectId, lines)
                    errorMessage = validation
                    val projectId = selectedProjectId
                    if (validation == null && projectId != null) {
                        scope.launch {
                            isSaving = true
                            runCatching {
                                val finalSheetNumber = sheetNumber.ifBlank {
                                    repository.suggestedSheetNumber(projectId)
                                }
                                repository.saveSheet(
                                    RequestSheetDraft(
                                        id = sheetId ?: 0L,
                                        projectId = projectId,
                                        sheetNumber = finalSheetNumber,
                                        sheetDate = sheetDate,
                                        trade = trade,
                                        craftsmanName = craftsmanName.trim(),
                                        workLocation = workLocation.trim(),
                                        attachmentUri = attachmentUri,
                                        notes = notes,
                                        lines = lines.map { line ->
                                            RequestLineDraft(
                                                existingItemId = line.existingItemId,
                                                itemName = line.itemName,
                                                quantity = line.quantity.toDouble(),
                                                unit = line.unit,
                                                brand = "",
                                                usage = "",
                                                lineDescription = line.description,
                                                notes = line.notes,
                                            )
                                        },
                                    )
                                )
                            }.onSuccess {
                                saveConfirmed = true
                                Toast.makeText(context, "تم حفظ الكشف بنجاح ✓", Toast.LENGTH_SHORT).show()
                                delay(650)
                                onDone()
                            }.onFailure {
                                errorMessage = friendlySheetSaveError(it)
                            }
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
            ) {
                when {
                    saveConfirmed -> Icon(Icons.Outlined.CheckCircle, null)
                    isSaving -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else -> Icon(Icons.Outlined.Save, null)
                }
                Spacer(Modifier.width(6.dp))
                Text(if (saveConfirmed) "تم الحفظ" else "حفظ")
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Card {
                    Column(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        val project = projects.firstOrNull { it.id == selectedProjectId }
                        OutlinedButton(
                            onClick = { projectPickerOpen = true },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                        ) {
                            Icon(Icons.Outlined.Business, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(project?.name ?: "اختيار المشروع", modifier = Modifier.weight(1f), maxLines = 1)
                            Icon(Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            OutlinedTextField(
                                value = craftsmanName,
                                onValueChange = { craftsmanName = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("الصنايعي") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = workLocation,
                                onValueChange = { workLocation = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("المكان (اختياري)") },
                                placeholder = { Text("حمام / مطبخ") },
                                singleLine = true,
                            )
                        }
                    }
                }
            }

            item {
                Card {
                    Column(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.AttachFile, null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (attachmentUri == null) "صورة الكشف الأصلي"
                                else "تم إرفاق صورة بالكشف ✓",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (attachmentUri != null) {
                                IconButton(
                                    onClick = {
                                        attachmentUri?.let { openRequestAttachment(context, it) }
                                    },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(Icons.Outlined.Visibility, "فتح المرفق")
                                }
                                IconButton(
                                    onClick = { attachmentUri = null },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(Icons.Outlined.Close, "حذف المرفق")
                                }
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            OutlinedButton(
                                onClick = { imagePicker.launch(arrayOf("image/*")) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Image, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("إرفاق صورة")
                            }
                            OutlinedButton(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("تصوير")
                            }
                        }

                        if (sheetId != null) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        pdfLauncher.launch("FAYROZ-Request-${sheetNumber.ifBlank { sheetId.toString() }}.pdf")
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Outlined.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("PDF")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            runCatching {
                                                val savedSheet = repository.getSheet(sheetId) ?: error("الكشف غير موجود")
                                                val project = repository.getProject(savedSheet.projectId) ?: error("المشروع غير موجود")
                                                val savedLines = repository.getRequestLines(sheetId)
                                                ShareFiles.sharePdf(
                                                    context,
                                                    "FAYROZ-Request-${savedSheet.sheetNumber}.pdf",
                                                ) {
                                                    FayrozReports.writeRequestSheetPdf(
                                                        it,
                                                        project,
                                                        savedSheet,
                                                        savedLines,
                                                    )
                                                }
                                            }.onFailure {
                                                Toast.makeText(context, "تعذر مشاركة الكشف.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Outlined.Share, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("مشاركة")
                                }
                            }
                            Text(
                                "التصدير والمشاركة يستخدمان آخر نسخة محفوظة من الكشف.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { multiPickerOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.PlaylistAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (lines.isEmpty()) "اختيار الأصناف" else "إضافة أصناف أخرى")
                }
            }

            if (lines.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Outlined.PlaylistAdd,
                        "لسه مفيش أصناف",
                        "اضغط «اختيار الأصناف» وحدد كل المطلوب مرة واحدة.",
                    )
                }
            } else {
                itemsIndexed(lines, key = { _, line -> line.localId }) { index, line ->
                    RequestLineEditorCard(
                        index = index,
                        line = line,
                        onChange = { lines[index] = it },
                        onDelete = { lines.removeAt(index) },
                    )
                }
            }

            errorMessage?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
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
                    scope.launch {
                        sheetNumber = repository.suggestedSheetNumber(project.id)
                    }
                }
            },
        )
    }

    if (multiPickerOpen) {
        MultiSelectItemPickerDialog(
            catalogItems = items,
            categories = categories,
            onDismiss = { multiPickerOpen = false },
            onAdd = { selectedItems ->
                var nextId = (lines.maxOfOrNull { it.localId } ?: 0L) + 1L
                selectedItems.forEach { item ->
                    lines.add(
                        EditableLineUi(
                            localId = nextId++,
                            categoryId = item.categoryId,
                            existingItemId = item.id,
                            itemName = item.name,
                            unit = item.defaultUnit,
                        )
                    )
                }
                multiPickerOpen = false
            },
        )
    }
}

@Composable
private fun RequestLineEditorCard(
    index: Int,
    line: EditableLineUi,
    onChange: (EditableLineUi) -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "${index + 1}. ${line.itemName}",
                modifier = Modifier.weight(1.65f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )

            CompactLineField(
                value = line.quantity,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("\\d*(\\.\\d*)?"))) {
                        onChange(line.copy(quantity = value))
                    }
                },
                placeholder = "العدد",
                suffix = line.unit,
                modifier = Modifier.weight(0.70f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "حذف البند",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CompactLineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    suffix: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(38.dp),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isBlank()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                    if (suffix.isNotBlank()) {
                        Spacer(Modifier.width(3.dp))
                        Text(
                            suffix,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun MultiSelectItemPickerDialog(
    catalogItems: List<ItemEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (List<ItemEntity>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    val filtered = remember(catalogItems, query, selectedCategoryId) {
        catalogItems.filter { item ->
            val categoryMatches = selectedCategoryId == null || item.categoryId == selectedCategoryId
            val queryMatches = query.isBlank() ||
                item.name.contains(query, true) ||
                item.code.contains(query, true) ||
                item.specification.contains(query, true)
            categoryMatches && queryMatches
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, "إغلاق")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("اختيار الأصناف", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "حدد أي عدد من الأصناف ثم أضفهم للكشف",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selectedIds.isNotEmpty()) {
                        Badge { Text(selectedIds.size.toString()) }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    label = { Text("بحث باسم الصنف أو اسم الصنايعي") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                )

                Spacer(Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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

                HorizontalDivider(Modifier.padding(top = 8.dp))

                if (filtered.isEmpty()) {
                    Box(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("مفيش أصناف مطابقة للبحث.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            val checked = item.id in selectedIds
                            Card(
                                onClick = {
                                    if (checked) selectedIds.remove(item.id)
                                    else selectedIds.add(item.id)
                                },
                                colors = if (checked) {
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    )
                                } else {
                                    CardDefaults.cardColors()
                                },
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            if (checked) selectedIds.remove(item.id)
                                            else selectedIds.add(item.id)
                                        },
                                    )
                                    Spacer(Modifier.width(8.dp))
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
                                    Text(
                                        item.defaultUnit,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(tonalElevation = 2.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "المحدد: ${selectedIds.size}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Button(
                            onClick = {
                                val selected = selectedIds.mapNotNull { id ->
                                    catalogItems.firstOrNull { it.id == id }
                                }
                                onAdd(selected)
                            },
                            enabled = selectedIds.isNotEmpty(),
                        ) {
                            Icon(Icons.Outlined.AddTask, null)
                            Spacer(Modifier.width(6.dp))
                            Text("إضافة للكشف")
                        }
                    }
                }
            }
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
            LazyColumn(
                Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(projects, key = { it.id }) { project ->
                    Card(onClick = { onSelect(project) }) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(project.name, style = MaterialTheme.typography.titleMedium)
                            if (project.location.isNotBlank()) {
                                Text(
                                    project.location,
                                    style = MaterialTheme.typography.bodySmall,
                                )
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

private fun validateSheet(
    projectId: Long?,
    lines: List<EditableLineUi>,
): String? {
    if (projectId == null) return "اختار المشروع أولًا."
    if (lines.isEmpty()) return "اختار صنف واحد على الأقل."
    val invalid = lines.firstOrNull {
        it.existingItemId == null ||
            (it.quantity.toDoubleOrNull() ?: 0.0) <= 0.0 ||
            it.unit.isBlank()
    }
    if (invalid != null) return "راجع الكميات: كل صنف لازم يكون له كمية أكبر من صفر."
    return null
}

private fun commonBrandSuggestions(categoryName: String): List<String> = when {
    categoryName.startsWith("سباكة") -> listOf("الشريف", "أكوافلو", "BR", "باننجر")
    categoryName == "أدوات صحية" -> listOf("Ideal Standard", "Lecico", "Grohe", "Hansgrohe", "Roca", "Duravit")
    categoryName == "كهرباء - تأسيس" -> listOf("السويدي", "كابلات مصر")
    categoryName == "كهرباء - لوحات وحماية" -> listOf("Schneider", "ABB", "Legrand", "Eaton", "Siemens")
    categoryName == "كهرباء - مفاتيح وبرايز" -> listOf("Schneider", "Legrand", "BTicino", "Gewiss", "Vimar")
    categoryName == "إضاءة" -> listOf("Philips", "Osram")
    categoryName == "تيار خفيف وسمارت" -> listOf("Hikvision", "Dahua", "TP-Link", "Ubiquiti", "Schneider", "Legrand")
    categoryName == "جبس بورد وأسقف" -> listOf("Knauf", "Gyproc")
    categoryName == "دهانات" -> listOf("Jotun", "GLC", "Sipes", "Scib", "Pachin")
    categoryName == "مواد لاصقة وكيماويات" -> listOf("Sika", "Bostik", "Pattex")
    else -> emptyList()
}

private fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()


private fun friendlySheetSaveError(error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        message.contains("UNIQUE constraint", ignoreCase = true) ||
            message.contains("SQLITE_CONSTRAINT_UNIQUE", ignoreCase = true) ->
            "حصل تعارض في رقم الكشف. اقفل الرسالة واضغط حفظ مرة أخرى."

        message.contains("foreign key", ignoreCase = true) ->
            "تعذر الحفظ لأن مشروع أو صنف مرتبط بالكشف لم يعد موجودًا."

        else ->
            "تعذر حفظ الكشف. راجع البيانات وحاول مرة أخرى."
    }
}


private fun openRequestAttachment(context: Context, reference: String) {
    runCatching {
        val uri = if (reference.startsWith("content://")) {
            Uri.parse(reference)
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(reference),
            )
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "تعذر فتح المرفق.", Toast.LENGTH_SHORT).show()
    }
}
