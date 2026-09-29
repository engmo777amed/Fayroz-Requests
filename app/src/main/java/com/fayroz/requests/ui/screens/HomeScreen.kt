package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column {
            Text("FAYROZ", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("طلبات وتسعير المشروعات", style = MaterialTheme.typography.titleMedium)
        }

        PrimaryAction("+ كشف طلبات جديد", Icons.Outlined.AddCircle, onNewSheet)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("الكشوف", "طلبات الصنايعية لكل مشروع", Icons.Outlined.ReceiptLong, onSheets, Modifier.weight(1f))
            FeatureCard("التسعير", "مقارنة أسعار الموردين والخصم", Icons.Outlined.PriceCheck, onPricing, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("دليل الأصناف", "أصناف مرتبة للكشوف والأسعار", Icons.Outlined.Inventory2, onItems, Modifier.weight(1f))
            FeatureCard("الموردون", "المعتمدون وقوائم الأسعار", Icons.Outlined.Storefront, onSuppliers, Modifier.weight(1f))
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("المشروعات", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(6.dp))
                Text("أضف المشروعات ثم سجل كشوف الطلبات عليها", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onProjects) { Text("إدارة المشروعات") }
            }
        }
    }
}
