package com.fayroz.requests.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fayroz.requests.data.repository.FayrozRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun DataToolsScreen(
    repository: FayrozRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { repository.writeBackup(context, it) }
                            ?: error("تعذر فتح ملف النسخة الاحتياطية")
                    }
                }
                busy = false
                Toast.makeText(
                    context,
                    if (result.isSuccess) "تم إنشاء النسخة الاحتياطية ✓"
                    else "تعذر إنشاء النسخة: ${result.exceptionOrNull()?.message.orEmpty()}",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { repository.restoreBackup(context, it) }
                            ?: error("تعذر فتح ملف النسخة")
                    }
                }
                busy = false
                Toast.makeText(
                    context,
                    if (result.isSuccess) "تمت استعادة البيانات بنجاح ✓"
                    else "تعذر استعادة النسخة: ${result.exceptionOrNull()?.message.orEmpty()}",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FayrozDetailHeader(
            title = "النسخ الاحتياطي",
            subtitle = "حفظ واستعادة كل بيانات FAYROZ Requests",
            onBack = onBack,
        )

        Card {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Security, null)
                Text("النسخة تشمل المشروعات والكشوف وصور الكشوف والأصناف والموردين وقوائم الأسعار ونسخ التسعير والماركات.")
                Text(
                    "الاستعادة تستبدل البيانات الحالية بالكامل بالبيانات الموجودة داخل ملف النسخة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = {
                backupLauncher.launch("FAYROZ-Requests-backup-${LocalDate.now()}.json")
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.FileDownload, null)
            Spacer(Modifier.width(8.dp))
            Text("إنشاء نسخة احتياطية")
        }

        OutlinedButton(
            onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain")) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.FileUpload, null)
            Spacer(Modifier.width(8.dp))
            Text("استعادة نسخة احتياطية")
        }

        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
