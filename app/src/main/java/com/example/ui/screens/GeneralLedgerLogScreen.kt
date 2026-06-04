package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.Client
import com.example.data.Operation
import com.example.ui.viewmodel.FinanceViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralLedgerLogScreen(
    viewModel: FinanceViewModel,
    onNavigateToClientProfile: (Int) -> Unit
) {
    val context = LocalContext.current

    val filteredOperations by viewModel.filteredOperations.collectAsState()
    val clientsMap by viewModel.clientsMap.collectAsState()

    // Filters in view model
    val searchQuery by viewModel.operationSearchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val filterStartDate by viewModel.filterStartDate.collectAsState()
    val filterEndDate by viewModel.filterEndDate.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App header
        Text(
            text = "General Ledger Ledger Logs",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Filter panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.operationSearchQuery.value = it },
                    label = { Text("Filter notes reference...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ledger_search_field")
                )

                // Given / Received Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("ALL", "GIVEN", "RECEIVED").forEach { filter ->
                        val selected = filterType == filter
                        Button(
                            onClick = { viewModel.filterType.value = filter },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = when (filter) {
                                    "GIVEN" -> "GIVEN (I Paid)"
                                    "RECEIVED" -> "RECEIVED"
                                    else -> "ALL TYPES"
                                },
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Date Picker ranges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val startText = if (filterStartDate != null) SimpleDateFormat("yy-MM-dd", Locale.getDefault()).format(Date(filterStartDate!!)) else "Start"
                    val endText = if (filterEndDate != null) SimpleDateFormat("yy-MM-dd", Locale.getDefault()).format(Date(filterEndDate!!)) else "End"

                    ElevatedButton(
                        onClick = {
                            showDateRangeSelector(context, filterStartDate) { viewModel.filterStartDate.value = it }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("From: $startText", fontSize = 10.sp, maxLines = 1)
                    }

                    ElevatedButton(
                        onClick = {
                            showDateRangeSelector(context, filterEndDate) { viewModel.filterEndDate.value = it }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("To: $endText", fontSize = 10.sp, maxLines = 1)
                    }

                    // Reset button if filters active
                    if (searchQuery.isNotEmpty() || filterType != "ALL" || filterStartDate != null || filterEndDate != null) {
                        IconButton(
                            onClick = { viewModel.clearFilters() },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Filters", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Export metrics & Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val count = filteredOperations.size
            Text(
                text = "$count transactions logged",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Export to CSV Sheet button
            Button(
                onClick = {
                    shareLedgerCsvReport(context, filteredOperations, clientsMap)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White),
                modifier = Modifier.testTag("export_csv_btn")
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Excel icon", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export Excel (CSV)", fontSize = 11.sp)
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // Lazy logs rendering
        if (filteredOperations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                    Text("No ledger logs match configuration.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredOperations, key = { it.id }) { op ->
                    val client = clientsMap[op.clientId]
                    LedgerRow(
                        operation = op,
                        clientName = client?.name ?: "Unknown Client",
                        onClientClick = { client?.let { onNavigateToClientProfile(it.id) } }
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerRow(
    operation: Operation,
    clientName: String,
    onClientClick: () -> Unit
) {
    val isGiven = operation.type == "GIVEN"
    val dateText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(operation.date))
    val avatarBg = if (isGiven) Color(0xFFD1FAE5) else Color(0xFFFFE4E6)
    val avatarText = if (isGiven) Color(0xFF065F46) else Color(0xFF9F1239)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClientClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Rounded-2xl avatar initials badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(avatarBg, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (clientName.length >= 2) clientName.substring(0, 2).uppercase() else clientName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = avatarText,
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier.weight(1.5f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = clientName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(
                        modifier = Modifier
                            .background(
                                if (isGiven) Color(0xFFD1FAE5) else Color(0xFFFFE4E6),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isGiven) "Given" else "Received",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGiven) Color(0xFF065F46) else Color(0xFF9F1239)
                        )
                    }
                }

                Text(
                    text = operation.notes.ifEmpty { "No details specified" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Text(
                    text = dateText,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Cash Column
            Text(
                text = (if (isGiven) "+" else "-") + formatCurrency(operation.amount),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                color = if (isGiven) Color(0xFF065F46) else Color(0xFF9F1239)
            )
        }
    }
}

private fun showDateRangeSelector(context: Context, previousDate: Long?, action: (Long) -> Unit) {
    val cal = Calendar.getInstance()
    if (previousDate != null) cal.timeInMillis = previousDate
    android.app.DatePickerDialog(
        context,
        { _, y, m, d ->
            val res = Calendar.getInstance()
            res.set(y, m, d, 0, 0, 0)
            action(res.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

/**
 * Creates Excel-compatible CSV stream, saves cache, and runs Android share sheet.
 */
private fun shareLedgerCsvReport(context: Context, operations: List<Operation>, clients: Map<Int, Client>) {
    try {
        val csvData = com.example.util.FinanceExportHelper.exportOperationsToCsv(operations, clients)
        val directory = context.cacheDir
        val reportFile = File(directory, "Ledger_Transaction_Report.csv")
        reportFile.writeText(csvData)

        val authority = "${context.packageName}.fileprovider"
        val csvUri = FileProvider.getUriForFile(context, authority, reportFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, csvUri)
            putExtra(Intent.EXTRA_SUBJECT, "General Ledger Transaction Export (Excel)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Transaction Sheet"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving spreadsheet: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
