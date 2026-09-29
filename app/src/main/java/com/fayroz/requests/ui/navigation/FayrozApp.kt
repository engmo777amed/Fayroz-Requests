package com.fayroz.requests.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fayroz.requests.data.repository.FayrozRepository
import com.fayroz.requests.ui.screens.*

data class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    MainDestination("home", "الرئيسية", Icons.Outlined.Home),
    MainDestination("sheets", "الكشوف", Icons.Outlined.ReceiptLong),
    MainDestination("items", "دليل الأصناف", Icons.Outlined.Inventory2),
    MainDestination("suppliers", "الموردون", Icons.Outlined.Storefront),
)

@Composable
fun FayrozApp(repository: FayrozRepository) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val detailOpen = currentRoute in setOf(
        "sheet/new",
        "sheet/edit/{sheetId}",
        "supplier/{supplierId}/lists",
        "pricelist/{priceListId}",
        "pricelist/{priceListId}/import",
        "pricing/{sheetId}",
        "item/{itemId}/history",
    )

    Scaffold(
        bottomBar = {
            if (!detailOpen) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    onNewSheet = { navController.navigate("sheet/new") },
                    onProjects = { navController.navigate("projects") },
                    onSheets = { navController.navigate("sheets") },
                    onPricing = { navController.navigate("pricing") },
                    onSuppliers = { navController.navigate("suppliers") },
                    onItems = { navController.navigate("items") },
                )
            }
            composable("projects") { ProjectsScreen(repository) }
            composable("sheets") {
                RequestSheetsScreen(
                    repository = repository,
                    onNewSheet = { navController.navigate("sheet/new") },
                    onEditSheet = { navController.navigate("sheet/edit/$it") },
                    onPriceSheet = { navController.navigate("pricing/$it") },
                )
            }
            composable("pricing") {
                PricingScreen(
                    repository = repository,
                    onPriceSheet = { navController.navigate("pricing/$it") },
                )
            }
            composable("suppliers") {
                SuppliersScreen(
                    repository = repository,
                    onOpenSupplier = { navController.navigate("supplier/$it/lists") },
                )
            }
            composable("items") {
                ItemsScreen(repository, onOpenHistory = { navController.navigate("item/$it/history") })
            }
            composable(
                route = "item/{itemId}/history",
                arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
            ) { entry ->
                ItemPriceHistoryScreen(
                    repository = repository,
                    itemId = entry.arguments?.getLong("itemId") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("sheet/new") {
                RequestSheetEditorScreen(
                    repository = repository,
                    sheetId = null,
                    onDone = {
                        navController.popBackStack()
                        navController.navigate("sheets") { launchSingleTop = true }
                    },
                    onCancel = { navController.popBackStack() },
                    onOpenProjects = { navController.navigate("projects") },
                )
            }
            composable(
                route = "sheet/edit/{sheetId}",
                arguments = listOf(navArgument("sheetId") { type = NavType.LongType }),
            ) { entry ->
                RequestSheetEditorScreen(
                    repository = repository,
                    sheetId = entry.arguments?.getLong("sheetId"),
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                    onOpenProjects = { navController.navigate("projects") },
                )
            }
            composable(
                route = "supplier/{supplierId}/lists",
                arguments = listOf(navArgument("supplierId") { type = NavType.LongType }),
            ) { entry ->
                SupplierPriceListsScreen(
                    repository = repository,
                    supplierId = entry.arguments?.getLong("supplierId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onOpenPriceList = { navController.navigate("pricelist/$it") },
                )
            }
            composable(
                route = "pricelist/{priceListId}",
                arguments = listOf(navArgument("priceListId") { type = NavType.LongType }),
            ) { entry ->
                val priceListId = entry.arguments?.getLong("priceListId") ?: 0L
                PriceListEntriesScreen(
                    repository = repository,
                    priceListId = priceListId,
                    onBack = { navController.popBackStack() },
                    onImport = { navController.navigate("pricelist/$priceListId/import") },
                )
            }
            composable(
                route = "pricelist/{priceListId}/import",
                arguments = listOf(navArgument("priceListId") { type = NavType.LongType }),
            ) { entry ->
                val priceListId = entry.arguments?.getLong("priceListId") ?: 0L
                PriceListImportScreen(
                    repository = repository,
                    priceListId = priceListId,
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                route = "pricing/{sheetId}",
                arguments = listOf(navArgument("sheetId") { type = NavType.LongType }),
            ) { entry ->
                SheetPricingScreen(
                    repository = repository,
                    sheetId = entry.arguments?.getLong("sheetId") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
