package com.onigiri.spend.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onigiri.spend.data.ApiClient
import com.onigiri.spend.data.AuthRequest
import com.onigiri.spend.data.SessionStore
import com.onigiri.spend.data.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isRegister: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(
    private val session: SessionStore,
    private val apiClient: ApiClient,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun updateEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun toggleMode() = _state.update { it.copy(isRegister = !it.isRegister, error = null) }

    fun submit(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.email.isBlank() || current.password.length < 8) {
            _state.update { it.copy(error = "Email and a password of at least 8 characters are required.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val api = apiClient.api()
                val body = AuthRequest(current.email.trim(), current.password)
                if (current.isRegister) api.register(body) else api.login(body)
            }.onSuccess { token ->
                session.setToken(token.accessToken)
                session.setCurrency(token.user.defaultCurrency)
                apiClient.tokenSnapshot = token.accessToken
                _state.update { it.copy(loading = false) }
                onSuccess()
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    companion object {
        fun factory(session: SessionStore, apiClient: ApiClient) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(session, apiClient) as T
                }
            }
    }
}
