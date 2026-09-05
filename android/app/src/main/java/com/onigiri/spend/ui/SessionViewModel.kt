package com.onigiri.spend.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onigiri.spend.data.ApiClient
import com.onigiri.spend.data.SessionStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

sealed class SessionState {
    data object Loading : SessionState()
    data object LoggedOut : SessionState()
    data class LoggedIn(val token: String) : SessionState()
}

class SessionViewModel(
    session: SessionStore,
    apiClient: ApiClient,
) : ViewModel() {
    val state: StateFlow<SessionState> = session.token
        .onEach { apiClient.tokenSnapshot = it }
        .map { token ->
            if (token.isNullOrBlank()) SessionState.LoggedOut else SessionState.LoggedIn(token)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    companion object {
        fun factory(session: SessionStore, apiClient: ApiClient) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SessionViewModel(session, apiClient) as T
                }
            }
    }
}
