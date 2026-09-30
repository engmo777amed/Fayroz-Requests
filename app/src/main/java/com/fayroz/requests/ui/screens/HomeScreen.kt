package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNewSheet: () -> Unit,
    onProjects: () -> Unit,
    onSheets: () -> Unit,
    onPricing: () -> Unit,
    onSuppliers: () -> Unit,
    onItems: () -> Unit,
    onDataTools: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "FAYROZ REQUESTS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("طلبات وتسعير المشروعات", style = MaterialTheme.typography.titleMedium)
            }
            AssistChip(
                onClick = {},
                label = { Text("V0.9.0") },
                leadingIcon = { Icon(Icons.Outlined.Verified, null) },
            )
        }

        PrimaryAction("كشف طلبات جديد", Icons.Outlined.AddCircle, onNewSheet)

        Text(
            "الوصول السريع",
            style = MaterialTheme.typography.titleMedium,
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FeatureCard(
                "الكشوف",
                "راجع وعدّل وسعّر",
                Icons.Outlined.ReceiptLong,
                onSheets,
                Modifier.weight(1f),
            )
            FeatureCard(
                "دليل الأصناف",
                "بحث وإضافة وتعديل",
                Icons.Outlined.Inventory2,
                onItems,
                Modifier.weight(1f),
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FeatureCard(
                "الموردون",
                "الموردين وقوائم الأسعار",
                Icons.Outlined.Storefront,
                onSuppliers,
                Modifier.weight(1f),
            )
            FeatureCard(
                "المشروعات",
                "إدارة المشروعات",
                Icons.Outlined.Business,
                onProjects,
                Modifier.weight(1f),
            )
        }

        FeatureCard(
            "النسخ الاحتياطي",
            "حفظ واستعادة كل البيانات",
            Icons.Outlined.Security,
            onDataTools,
            Modifier.fillMaxWidth(),
        )

        Text(
            "التسعير موجود داخل كل كشف؛ مش محتاج شاشة منفصلة في التنقل الرئيسي.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
