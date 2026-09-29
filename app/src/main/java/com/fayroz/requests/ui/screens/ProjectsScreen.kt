package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FayrozDetailHeader(
            title = "المشروعات",
            subtitle = "كل كشف يتبع مشروعًا واحدًا",
            onBack = onBack,
        )
        PrimaryAction("إضافة مشروع", Icons.Outlined.AddBusiness) { showAdd = true }

        if (projects.isEmpty()) {
            EmptyState(Icons.Outlined.Business, "لا توجد مشروعات بعد", "أضف أول مشروع لبدء تسجيل كشوف الطلبات.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(projects, key = { it.id }) { project ->
                    Card {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(project.name, style = MaterialTheme.typography.titleMedium)
                                if (project.clientName.isNotBlank()) Text("العميل: ${project.clientName}", style = MaterialTheme.typography.bodyMedium)
                                if (project.location.isNotBlank()) Text(project.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row {
                                IconButton(onClick = { editingProject = project }) { Icon(Icons.Outlined.Edit, "تعديل") }
                                IconButton(onClick = { deleteTarget = project }) { Icon(Icons.Outlined.DeleteOutline, "حذف") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ProjectDialog(
            title = "إضافة مشروع",
            onDismiss = { showAdd = false },
            onSave = { name, client, location ->
                scope.launch { repository.addProject(name, client, location) }
                showAdd = false
            },
        )
    }

    editingProject?.let { project ->
        ProjectDialog(
            title = "تعديل المشروع",
            initialName = project.name,
            initialClient = project.clientName,
            initialLocation = project.location,
            onDismiss = { editingProject = null },
            onSave = { name, client, location ->
                scope.launch {
                    repository.updateProject(project.copy(name = name.trim(), clientName = client.trim(), location = location.trim()))
                }
                editingProject = null
            },
        )
    }

    deleteTarget?.let { project ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف المشروع؟") },
            text = { Text("سيتم حذف المشروع وكل كشوف الطلبات المرتبطة به. هذا الإجراء لا يمكن التراجع عنه.") },
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
    initialName: String = "",
    initialClient: String = "",
    initialLocation: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var client by remember { mutableStateOf(initialClient) }
    var location by remember { mutableStateOf(initialLocation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("اسم المشروع *") }, singleLine = true)
                OutlinedTextField(client, { client = it }, label = { Text("العميل") }, singleLine = true)
                OutlinedTextField(location, { location = it }, label = { Text("الموقع") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, client, location) }, enabled = name.isNotBlank()) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
