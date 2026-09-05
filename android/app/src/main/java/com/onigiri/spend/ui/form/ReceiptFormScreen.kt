package com.onigiri.spend.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.onigiri.spend.ui.formatMoney
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptFormScreen(
    viewModel: ReceiptFormViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var showDate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edit receipt" else "New receipt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = state.merchantName,
                onValueChange = viewModel::updateMerchant,
                label = { Text("Merchant") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.purchasedAt,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { showDate = true }) { Text("Pick") }
                },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.tax,
                onValueChange = viewModel::updateTax,
                label = { Text("Tax") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Items", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = viewModel::addItem) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add item")
                }
            }
            state.items.forEach { item ->
                ItemCard(
                    item = item,
                    onChange = { transform -> viewModel.updateItem(item.key, transform) },
                    onRemove = { viewModel.removeItem(item.key) },
                    canRemove = state.items.size > 1,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                "Total ${formatMoney(viewModel.computedTotal(), state.currency)}",
                style = MaterialTheme.typography.titleLarge,
            )
            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.saving) "Saving…" else "Save")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDate) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                LocalDate.parse(state.purchasedAt).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrNull(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.updateDate(date.toString())
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = picker)
        }
    }
}

@Composable
private fun ItemCard(
    item: ItemDraft,
    onChange: ((ItemDraft) -> ItemDraft) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { value -> onChange { it.copy(name = value) } },
                    label = { Text("Item name") },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove, enabled = canRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove item")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.quantity,
                    onValueChange = { value -> onChange { it.copy(quantity = value) } },
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = item.unitPrice,
                    onValueChange = { value -> onChange { it.copy(unitPrice = value) } },
                    label = { Text("Unit price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = item.amount,
                    onValueChange = { value -> onChange { it.copy(amount = value) } },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
