package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Client
import com.example.data.Operation
import com.example.ui.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToClient: (Int) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val totalOwedByMeState by viewModel.totalOwedByMe.collectAsState()
    val totalOwedToMeState by viewModel.totalOwedToMe.collectAsState()
    val clientsWithBalancesState by viewModel.clientsWithBalances.collectAsState()
    val rawOperationsState by viewModel.allOperations.collectAsState()
    val clientsMapState by viewModel.clientsMap.collectAsState()
    val backupRestoreStatusState by viewModel.backupRestoreStatus.collectAsState()

    // Date picker dialog triggers
    var startDateText by remember { mutableStateOf("Select Start Date") }
    var endDateText by remember { mutableStateOf("Select End Date") }
    var reportStartDate by remember { mutableStateOf<Long?>(null) }
    var reportEndDate by remember { mutableStateOf<Long?>(null) }

    // Text field state for restoring from clipboard/JSON text string
    var jsonInputRestoreOpen by remember { mutableStateOf(false) }
    var backupJsonInputString by remember { mutableStateOf("") }

    // Backup Save flow
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                val json = viewModel.triggerBackup()
                if (json != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray())
                    }
                    Toast.makeText(context, "Backup written successfully!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to write backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Backup Read flow
    val selectBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { r ->
                    val content = r.bufferedReader().use { it.readText() }
                    viewModel.triggerRestore(content)
                    Toast.makeText(context, "Backup successfully imported!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to parse backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(backupRestoreStatusState) {
        backupRestoreStatusState?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissBackupStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header
        Text(
            text = "Client Ledger Dashboard",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Financial summary cards "Dashboard showing total amounts owed by me and owed to me" - Clean Minimalism theme
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Owed By Me (I Owe Total)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(128.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.CallReceived,
                        contentDescription = "Owed by me",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "I OWE TOTAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(totalOwedByMeState),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Owed To Me (Owed to Me)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(128.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.CallMade,
                        contentDescription = "Owed to me",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "OWED TO ME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(totalOwedToMeState),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Standing Net Custody Balance
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Custody Balance",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                val netBalance = totalOwedByMeState - totalOwedToMeState
                val netColor = if (netBalance >= 0) Color(0xFF065F46) else Color(0xFF9F1239)
                Text(
                    text = formatCurrency(netBalance),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = netColor
                )
            }
        }

        // Custom Period Date Range Calculator & Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Date Range Summarizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Calculate totals and generate statistics for a selected window of operations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start Date Button
                    Button(
                        onClick = {
                            showDatePickerDialog(context, reportStartDate) { timestamp ->
                                reportStartDate = timestamp
                                startDateText = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text(text = startDateText, maxLines = 1, fontSize = 12.sp)
                    }

                    // End Date Button
                    Button(
                        onClick = {
                            showDatePickerDialog(context, reportEndDate) { timestamp ->
                                reportEndDate = timestamp
                                endDateText = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text(text = endDateText, maxLines = 1, fontSize = 12.sp)
                    }
                }

                // Show Clear Filter if filtering
                if (reportStartDate != null || reportEndDate != null) {
                    Button(
                        onClick = {
                            reportStartDate = null
                            reportEndDate = null
                            startDateText = "Select Start Date"
                            endDateText = "Select End Date"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Clear Range Period", fontSize = 12.sp)
                    }

                    // Display filtered metrics
                    val filteredOps = rawOperationsState.filter { op ->
                        val startOk = reportStartDate == null || op.date >= reportStartDate!!
                        val endOk = reportEndDate == null || op.date <= reportEndDate!!
                        startOk && endOk
                    }

                    val rangeGiven = filteredOps.filter { it.type == "GIVEN" }.sumOf { it.amount }
                    val rangeReceived = filteredOps.filter { it.type == "RECEIVED" }.sumOf { it.amount }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Report Period Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Operations Logged: ${filteredOps.size}", style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Sum of Given:", style = MaterialTheme.typography.bodySmall)
                                Text(text = formatCurrency(rangeGiven), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Sum of Received:", style = MaterialTheme.typography.bodySmall)
                                Text(text = formatCurrency(rangeReceived), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Net Statement:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(text = formatCurrency(rangeGiven - rangeReceived), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        // Recent Operations Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Recent Operations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Recent Operations limited to 5 items
        val sortedOps = rawOperationsState.take(5)
        if (sortedOps.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageUrl = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Text(
                            text = "No operations logger history found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Add a client and log transactions to see data here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (op in sortedOps) {
                    val client = clientsMapState[op.clientId]
                    val isGiven = op.type == "GIVEN"
                    val avatarBg = if (isGiven) Color(0xFFD1FAE5) else Color(0xFFFFE4E6)
                    val avatarText = if (isGiven) Color(0xFF065F46) else Color(0xFF9F1239)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { client?.let { onNavigateToClient(it.id) } },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rounded-2xl design initials badge
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(avatarBg, shape = RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = client?.name?.take(2)?.uppercase() ?: "??",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = avatarText
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = client?.name ?: "Unknown Client",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = op.notes.ifEmpty { "No transaction notes" },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(op.date)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = (if (isGiven) "+" else "-") + formatCurrency(op.amount),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                                    color = if (isGiven) Color(0xFF065F46) else Color(0xFF9F1239)
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
                        }
                    }
                }
            }
        }

        // Backup and Restore Management Panel
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Database icon",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Backup & Local Restore Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Keep copy files of client sheets and restore whenever you move devices. Backups are exported as clean, standard JSON data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export to File SAF
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            createBackupLauncher.launch("client_ledger_backup_$timestamp.json")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Export File", fontSize = 11.sp, maxLines = 1)
                    }

                    // Import from File SAF
                    Button(
                        onClick = {
                            selectBackupLauncher.launch("application/json")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Import File", fontSize = 11.sp, maxLines = 1)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                // Clipboard share backup method is convenient when Storage permissions aren't accessible
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val backupJson = viewModel.triggerBackup()
                            if (backupJson != null) {
                                clipboardManager.setText(AnnotatedString(backupJson))
                                Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Copy Backup Text", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { jsonInputRestoreOpen = !jsonInputRestoreOpen },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Restore Text", fontSize = 11.sp)
                    }
                }

                AnimatedVisibility(visible = jsonInputRestoreOpen) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = backupJsonInputString,
                            onValueChange = { backupJsonInputString = it },
                            label = { Text("Paste Backup JSON Text") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        Button(
                            onClick = {
                                if (backupJsonInputString.isNotBlank()) {
                                    viewModel.triggerRestore(backupJsonInputString)
                                    backupJsonInputString = ""
                                    jsonInputRestoreOpen = false
                                } else {
                                    Toast.makeText(context, "Please paste valid JSON text", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Overwrite Local DB from Text")
                        }
                    }
                }
            }
        }
    }
}

// Custom Picker Dialog loader
fun showDatePickerDialog(context: Context, currentDate: Long?, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance()
    if (currentDate != null) {
        calendar.timeInMillis = currentDate
    }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val resultCal = Calendar.getInstance()
            resultCal.set(year, month, dayOfMonth, 0, 0, 0)
            onDateSelected(resultCal.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return format.format(amount)
}

@Composable
fun Icon(imageUrl: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, modifier: Modifier, tint: Color) {
    Icon(imageVector = imageUrl, contentDescription = contentDescription, modifier = modifier, tint = tint)
}
