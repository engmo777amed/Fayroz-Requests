package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fayroz.requests.data.model.ProjectEntity
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.launch

@Composable
fun ProjectsScreen(repository: FayrozRepository, onBack: (() -> Unit)? = null) {
    val projects by repository.projects.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ProjectEntity?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FayrozDetailHeader("المشروعات", "البيانات الإضافية اختيارية", onBack)
        PrimaryAction("إضافة مشروع", Icons.Outlined.AddBusiness) { showAdd = true }

        if (projects.isEmpty()) {
            EmptyState(Icons.Outlined.Business, "لا توجد مشروعات بعد", "أضف أول مشروع لبدء كشوف الطلبات.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(projects, key = { it.id }) { project ->
                    Card {
                        Row(Modifier.fillMaxWidth().padding(13.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(project.name, style = MaterialTheme.typography.titleMedium)
                                if (project.clientName.isNotBlank()) {
                                    Text(project.clientName, style = MaterialTheme.typography.bodySmall)
                                }
                                if (project.location.isNotBlank()) {
                                    Text(project.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val facts = buildList {
                                    project.areaSqm?.let { add("${formatArea(it)} م²") }
                                    project.floors?.let { add("$it دور") }
                                    project.rooms?.let { add("$it غرفة") }
                                    project.bathrooms?.let { add("$it حمام") }
                                }
                                if (facts.isNotEmpty()) {
                                    Text(facts.joinToString(" • "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            IconButton(onClick = { editingProject = project }) { Icon(Icons.Outlined.Edit, "تعديل") }
                            IconButton(onClick = { deleteTarget = project }) { Icon(Icons.Outlined.DeleteOutline, "حذف") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ProjectDialog(
            title = "إضافة مشروع",
            initial = ProjectEntity(name = ""),
            onDismiss = { showAdd = false },
            onSave = { project ->
                scope.launch { repository.addProject(project) }
                showAdd = false
            },
        )
    }

    editingProject?.let { project ->
        ProjectDialog(
            title = "تعديل المشروع",
            initial = project,
            onDismiss = { editingProject = null },
            onSave = { updated ->
                scope.launch { repository.updateProject(updated) }
                editingProject = null
            },
        )
    }

    deleteTarget?.let { project ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف المشروع؟") },
            text = { Text("سيتم حذف المشروع وكل الكشوف المرتبطة به.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteProject(project) }
                    deleteTarget = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("إلغاء") } },
        )
    }
}

@Composable
private fun ProjectDialog(
    title: String,
    initial: ProjectEntity,
    onDismiss: () -> Unit,
    onSave: (ProjectEntity) -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var client by remember(initial.id) { mutableStateOf(initial.clientName) }
    var location by remember(initial.id) { mutableStateOf(initial.location) }
    var projectType by remember(initial.id) { mutableStateOf(initial.projectType) }
    var area by remember(initial.id) { mutableStateOf(initial.areaSqm?.toString().orEmpty()) }
    var floors by remember(initial.id) { mutableStateOf(initial.floors?.toString().orEmpty()) }
    var units by remember(initial.id) { mutableStateOf(initial.units?.toString().orEmpty()) }
    var rooms by remember(initial.id) { mutableStateOf(initial.rooms?.toString().orEmpty()) }
    var bedrooms by remember(initial.id) { mutableStateOf(initial.bedrooms?.toString().orEmpty()) }
    var bathrooms by remember(initial.id) { mutableStateOf(initial.bathrooms?.toString().orEmpty()) }
    var kitchens by remember(initial.id) { mutableStateOf(initial.kitchens?.toString().orEmpty()) }
    var balconies by remember(initial.id) { mutableStateOf(initial.balconies?.toString().orEmpty()) }
    var status by remember(initial.id) { mutableStateOf(initial.projectStatus) }
    var notes by remember(initial.id) { mutableStateOf(initial.notes) }
    var expanded by remember(initial.id) {
        mutableStateOf(
            initial.projectType.isNotBlank() || initial.areaSqm != null || initial.floors != null ||
                initial.units != null || initial.rooms != null || initial.bathrooms != null ||
                initial.kitchens != null || initial.notes.isNotBlank()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("اسم المشروع *") }, singleLine = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(client, { client = it }, modifier = Modifier.weight(1f), label = { Text("العميل") }, singleLine = true)
                        OutlinedTextField(location, { location = it }, modifier = Modifier.weight(1f), label = { Text("الموقع") }, singleLine = true)
                    }
                }
                item {
                    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                        Spacer(Modifier.width(6.dp))
                        Text("بيانات إضافية (اختياري)")
                    }
                }
                if (expanded) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(projectType, { projectType = it }, modifier = Modifier.weight(1f), label = { Text("نوع المشروع") }, placeholder = { Text("شقة / فيلا") }, singleLine = true)
                            OutlinedTextField(status, { status = it }, modifier = Modifier.weight(1f), label = { Text("الحالة") }, placeholder = { Text("جديد / تجديد") }, singleLine = true)
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactNumberField("المساحة م²", area, { area = it }, Modifier.weight(1f), decimal = true)
                            CompactNumberField("الأدوار", floors, { floors = it }, Modifier.weight(1f))
                            CompactNumberField("الوحدات", units, { units = it }, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactNumberField("الغرف", rooms, { rooms = it }, Modifier.weight(1f))
                            CompactNumberField("الحمامات", bathrooms, { bathrooms = it }, Modifier.weight(1f))
                            CompactNumberField("المطابخ", kitchens, { kitchens = it }, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactNumberField("غرف النوم", bedrooms, { bedrooms = it }, Modifier.weight(1f))
                            CompactNumberField("البلكونات", balconies, { balconies = it }, Modifier.weight(1f))
                        }
                    }
                    item { OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth(), label = { Text("ملاحظات") }, maxLines = 2) }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            clientName = client.trim(),
                            location = location.trim(),
                            projectType = projectType.trim(),
                            areaSqm = area.toDoubleOrNull(),
                            floors = floors.toIntOrNull(),
                            units = units.toIntOrNull(),
                            rooms = rooms.toIntOrNull(),
                            bedrooms = bedrooms.toIntOrNull(),
                            bathrooms = bathrooms.toIntOrNull(),
                            kitchens = kitchens.toIntOrNull(),
                            balconies = balconies.toIntOrNull(),
                            projectStatus = status.trim(),
                            notes = notes.trim(),
                        )
                    )
                },
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun CompactNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            val ok = if (decimal) text.isEmpty() || text.matches(Regex("\\d*(\\.\\d*)?"))
            else text.isEmpty() || text.all(Char::isDigit)
            if (ok) onValueChange(text)
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
    )
}

private fun formatArea(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
