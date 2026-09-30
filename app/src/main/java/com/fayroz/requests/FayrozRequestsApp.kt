package com.fayroz.requests

import android.app.Application
import com.fayroz.requests.data.db.FayrozDatabase
import com.fayroz.requests.data.repository.FayrozRepository

class FayrozRequestsApp : Application() {
    val database: FayrozDatabase by lazy { FayrozDatabase.create(this) }
    val repository: FayrozRepository by lazy { FayrozRepository(database) }

    suspend fun ensureStarterCatalogOnce() {
        val prefs = getSharedPreferences("fayroz_requests_setup", MODE_PRIVATE)
        val catalogReady = prefs.getBoolean("starter_catalog_v4_seeded", false)
        val marketCatalogReady = prefs.getBoolean("market_catalog_v6_seeded", false)
        val marketNamesReady = prefs.getBoolean("market_names_v7_seeded", false)

        if (!catalogReady || !marketCatalogReady || !marketNamesReady || !repository.hasAnyItems()) {
            repository.ensureStarterCatalog()
            prefs.edit()
                .putBoolean("starter_catalog_v4_seeded", true)
                .putBoolean("market_catalog_v6_seeded", true)
                .putBoolean("market_names_v7_seeded", true)
                .apply()
        }

        // Idempotent: adds only missing company/brand names and preserves user additions.
        repository.ensureStarterBrands()
    }
}
