package com.onigiri.spend.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ApiClient(private val session: SessionStore) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Volatile
    private var cached: Pair<String, SpendApi>? = null

    suspend fun api(): SpendApi {
        val baseUrl = session.baseUrl.first().let { if (it.endsWith("/")) it else "$it/" }
        cached?.let { if (it.first == baseUrl) return it.second }
        val created = build(baseUrl)
        cached = baseUrl to created
        return created
    }

    fun invalidate() {
        cached = null
    }

    private fun build(baseUrl: String): SpendApi {
        val auth = Interceptor { chain ->
            val token = tokenSnapshot
            val request = if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SpendApi::class.java)
    }

    @Volatile
    var tokenSnapshot: String? = null
}
