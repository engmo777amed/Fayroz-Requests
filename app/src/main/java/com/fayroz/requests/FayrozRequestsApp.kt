package com.fayroz.requests

import android.app.Application
import com.fayroz.requests.data.db.FayrozDatabase
import com.fayroz.requests.data.repository.FayrozRepository

class FayrozRequestsApp : Application() {
    val database: FayrozDatabase by lazy { FayrozDatabase.create(this) }
    val repository: FayrozRepository by lazy { FayrozRepository(database) }
}
