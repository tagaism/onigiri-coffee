package com.onigiri.spend

import android.app.Application
import com.onigiri.spend.data.ApiClient
import com.onigiri.spend.data.SessionStore

class SpendApplication : Application() {
    lateinit var session: SessionStore
        private set
    lateinit var apiClient: ApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionStore(this)
        apiClient = ApiClient(session)
    }
}
