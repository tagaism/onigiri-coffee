package com.onigiri.spend.data

import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class ApiException(message: String) : Exception(message)

private val errorJson = Json { ignoreUnknownKeys = true }

fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Something went wrong"
    is HttpException -> {
        val raw = response()?.errorBody()?.string()
        val parsed = raw?.let {
            runCatching { errorJson.decodeFromString<ApiError>(it).detail }.getOrNull()
        }
        parsed ?: "Request failed (${code()})"
    }
    is IOException -> "Cannot reach the server. Check the API URL and that Docker is running."
    else -> message ?: "Something went wrong"
}
