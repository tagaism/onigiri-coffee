package com.onigiri.spend.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onigiri.spend.ui.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    viewModel: ReceiptDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.receipt?.merchantName ?: "Receipt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, enabled = state.receipt != null) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { confirmDelete = true }, enabled = state.receipt != null) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
            state.error != null && state.receipt == null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
            ) { Text(state.error!!, color = MaterialTheme.colorScheme.error) }
            state.receipt != null -> {
                val receipt = state.receipt!!
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Text(receipt.purchasedAt, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatMoney(receipt.total, receipt.currency),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    if (receipt.totalMismatch) {
                        Text(
                            "Stored total differs from items + tax (${formatMoney(receipt.computedTotal, receipt.currency)})",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (!receipt.notes.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(receipt.notes)
                    }
                    Spacer(Modifier.height(16.dp))
                    receipt.items.forEach { item ->
                        Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    Text("Qty ${item.quantity}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(formatMoney(item.amount, receipt.currency))
                            }
                        }
                    }
                    Text("Tax ${formatMoney(receipt.tax, receipt.currency)}")
                    if (state.error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete receipt?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}
