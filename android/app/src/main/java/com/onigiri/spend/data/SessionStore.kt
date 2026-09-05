package com.onigiri.spend.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("onigiri_session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val currencyKey = stringPreferencesKey("currency")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val baseUrl: Flow<String> = context.dataStore.data.map {
        it[baseUrlKey] ?: DEFAULT_BASE_URL
    }
    val currency: Flow<String> = context.dataStore.data.map {
        it[currencyKey] ?: "JPY"
    }

    suspend fun setToken(value: String?) {
        context.dataStore.edit { prefs ->
            if (value == null) prefs.remove(tokenKey) else prefs[tokenKey] = value
        }
    }

    suspend fun setBaseUrl(value: String) {
        context.dataStore.edit { it[baseUrlKey] = value.trim().trimEnd('/') }
    }

    suspend fun setCurrency(value: String) {
        context.dataStore.edit { it[currencyKey] = value.uppercase() }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000"
    }
}
