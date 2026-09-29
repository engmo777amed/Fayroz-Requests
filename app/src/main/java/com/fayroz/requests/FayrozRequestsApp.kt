package com.fayroz.requests

import android.app.Application
import com.fayroz.requests.data.db.FayrozDatabase
import com.fayroz.requests.data.repository.FayrozRepository

class FayrozRequestsApp : Application() {
    val database: FayrozDatabase by lazy { FayrozDatabase.create(this) }
    val repository: FayrozRepository by lazy { FayrozRepository(database) }

    suspend fun ensureStarterCatalogOnce() {
        val prefs = getSharedPreferences("fayroz_requests_setup", MODE_PRIVATE)
        if (prefs.getBoolean("starter_catalog_v2_seeded", false)) return

        repository.ensureStarterCatalog()
        prefs.edit().putBoolean("starter_catalog_v1_seeded", true).apply()
    }
}
