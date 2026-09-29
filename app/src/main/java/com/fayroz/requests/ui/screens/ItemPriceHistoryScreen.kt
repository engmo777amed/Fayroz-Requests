package com.fayroz.requests.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.requests.data.model.ItemEntity
import com.fayroz.requests.data.model.PriceSource
import com.fayroz.requests.data.repository.FayrozRepository
import com.fayroz.requests.domain.PriceFreshnessEngine
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ItemPriceHistoryScreen(
    repository: FayrozRepository,
    itemId: Long,
    onBack: () -> Unit,
) {
    var item by remember { mutableStateOf<ItemEntity?>(null) }
    val historyFlow = remember(itemId) { repository.itemPriceHistory(itemId) }
    val history by historyFlow.collectAsState(initial = emptyList())

    LaunchedEffect(itemId) { item = repository.getItem(itemId) }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ScreenHeader(
                item?.name ?: "تاريخ السعر",
                item?.let { "${it.code} • ${it.defaultUnit}" } ?: "سجل الأسعار",
            )
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "رجوع") }
        }

        if (history.isEmpty()) {
            EmptyState(Icons.Outlined.History, "لا يوجد تاريخ أسعار", "أضف سعرًا من ليستة مورد أو عرضًا مباشرًا وسيظهر هنا.")
        } else {
            val latestBySupplier = remember(history) { history.groupBy { it.supplierId }.mapValues { it.value.first() } }
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("آخر الأسعار", style = MaterialTheme.typography.titleMedium)
                    Text("${latestBySupplier.size} مورد • ${history.size} سجل سعري", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history, key = { it.priceId }) { row ->
                    val freshness = PriceFreshnessEngine.evaluate(row.priceDate)
                    Card {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(row.supplierName, style = MaterialTheme.typography.titleMedium)
                                Text(historyMoney(row.netPrice), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                "ليستة ${historyMoney(row.listPrice)} • خصم ${historyDiscount(row.appliedDiscountPercent)} • صافي الوحدة ${historyMoney(row.netPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuggestionChip(onClick = {}, label = { Text(freshness.statusLabel) })
                                Text("${historyDate(row.priceDate)} • ${freshness.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                buildString {
                                    append(historySource(row.source))
                                    row.priceListName?.takeIf { it.isNotBlank() }?.let { append(" • ").append(it) }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (row.notes.isNotBlank()) Text(row.notes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private val itemHistoryMoney = DecimalFormat("#,##0.00")
private fun historyMoney(value: Double): String = "${itemHistoryMoney.format(value)} ج"
private fun historyDiscount(value: Double): String = if (value % 1.0 == 0.0) "${value.toInt()}%" else "${itemHistoryMoney.format(value)}%"
private fun historyDate(timestamp: Long): String = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))
private fun historySource(source: PriceSource): String = when (source) {
    PriceSource.PRICE_LIST -> "ليستة مورد"
    PriceSource.DIRECT_QUOTE -> "عرض مباشر"
    PriceSource.MANUAL -> "سعر يدوي"
}
