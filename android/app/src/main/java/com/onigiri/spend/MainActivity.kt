package com.onigiri.spend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.onigiri.spend.ui.SessionState
import com.onigiri.spend.ui.SessionViewModel
import com.onigiri.spend.ui.auth.AuthScreen
import com.onigiri.spend.ui.auth.AuthViewModel
import com.onigiri.spend.ui.detail.ReceiptDetailScreen
import com.onigiri.spend.ui.detail.ReceiptDetailViewModel
import com.onigiri.spend.ui.form.ReceiptFormScreen
import com.onigiri.spend.ui.form.ReceiptFormViewModel
import com.onigiri.spend.ui.home.HomeScreen
import com.onigiri.spend.ui.home.HomeViewModel
import com.onigiri.spend.ui.settings.SettingsScreen
import com.onigiri.spend.ui.theme.OnigiriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SpendApplication
        setContent {
            OnigiriTheme {
                Surface(Modifier.fillMaxSize()) {
                    SpendNav(app)
                }
            }
        }
    }
}

@Composable
private fun SpendNav(app: SpendApplication) {
    val sessionVm: SessionViewModel = viewModel(factory = SessionViewModel.factory(app.session, app.apiClient))
    val sessionState by sessionVm.state.collectAsState()
    val loggedIn = sessionState is SessionState.LoggedIn

    if (sessionState is SessionState.Loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val start = if (loggedIn) "home" else "login"

    key(loggedIn) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = start) {
        composable("login") {
            val vm: AuthViewModel = viewModel(factory = AuthViewModel.factory(app.session, app.apiClient))
            AuthScreen(
                viewModel = vm,
                onSuccess = {
                    nav.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onOpenSettings = { nav.navigate("settings") },
            )
        }
        composable("home") {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app.apiClient))
            HomeScreen(
                viewModel = vm,
                onAdd = { nav.navigate("form") },
                onOpen = { id -> nav.navigate("detail/$id") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("form") {
            val vm: ReceiptFormViewModel = viewModel(
                factory = ReceiptFormViewModel.factory(null, app.apiClient, app.session),
            )
            ReceiptFormScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onSaved = { id ->
                    nav.navigate("detail/$id") {
                        popUpTo("home")
                    }
                },
            )
        }
        composable(
            "form/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id")
            val vm: ReceiptFormViewModel = viewModel(
                factory = ReceiptFormViewModel.factory(id, app.apiClient, app.session),
            )
            ReceiptFormScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onSaved = {
                    nav.popBackStack()
                },
            )
        }
        composable(
            "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            val vm: ReceiptDetailViewModel = viewModel(
                factory = ReceiptDetailViewModel.factory(id, app.apiClient),
            )
            ReceiptDetailScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("form/$id") },
                onDeleted = {
                    nav.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
            )
        }
        composable("settings") {
            SettingsScreen(
                session = app.session,
                apiClient = app.apiClient,
                showLogout = loggedIn,
                onBack = { nav.popBackStack() },
                onLoggedOut = {
                    nav.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
    }
}
