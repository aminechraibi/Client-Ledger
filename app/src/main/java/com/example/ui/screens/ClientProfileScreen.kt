package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.data.Client
import com.example.data.Operation
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.AttachmentHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileScreen(
    viewModel: FinanceViewModel,
    clientId: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val clientsMap by viewModel.clientsMap.collectAsState()
    val client = clientsMap[clientId]

    // Register active filters
    val filteredOperations by viewModel.selectedClientFilteredOperations.collectAsState()
    val rawOperations by viewModel.selectedClientOperations.collectAsState()
    
    val balanceValue by viewModel.selectedClientBalance.collectAsState()
    val givenTotal by viewModel.selectedClientGivenTotal.collectAsState()
    val receivedTotal by viewModel.selectedClientReceivedTotal.collectAsState()

    // Filter controls inside viewmodel
    val operationsSearchQuery by viewModel.operationSearchQuery.collectAsState()
    val operationsFilterType by viewModel.filterType.collectAsState()
    val filterStartDate by viewModel.filterStartDate.collectAsState()
    val filterEndDate by viewModel.filterEndDate.collectAsState()

    // Local dialog visibility controllers
    var showEditClientDialog by remember { mutableStateOf(false) }
    var showDeleteClientConfirmation by remember { mutableStateOf(false) }
    var showAddOperationDialog by remember { mutableStateOf(false) }
    
    // Operation edit visibility
    var selectedOpForAction by remember { mutableStateOf<Operation?>(null) }
    var showEditOperationDialog by remember { mutableStateOf(false) }
    var showDeleteOpConfirmation by remember { mutableStateOf(false) }

    // Selected attachment full visualizer modal
    var activeViewerAttachmentPath by remember { mutableStateOf<String?>(null) }

    // Sync selected client state in VM
    LaunchedEffect(clientId) {
        viewModel.selectedClientId.value = clientId
    }

    if (client == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Client Account Not Found", fontWeight = FontWeight.Bold)
                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditClientDialog = true }, modifier = Modifier.testTag("edit_client_btn")) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Credentials")
                    }
                    IconButton(onClick = { showDeleteClientConfirmation = true }, modifier = Modifier.testTag("delete_client_btn")) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Client")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddOperationDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_operation_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Log Operation")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Client Quick Card Details (Email, phone, date created)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (client.phone.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable { triggerPhone(context, client.phone) }
                        ) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(text = client.phone, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (client.email.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable { triggerEmail(context, client.email) }
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(text = client.email, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (client.notes.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            Text(text = client.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        text = "Registered since: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(client.createdAt))}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Client ledger balance snapshots & automated calculation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusColor = when {
                        balanceValue > 0 -> Color(0xFF2E7D32) // Green: I owe client (Funds in trust)
                        balanceValue < 0 -> Color(0xFFC62828) // Red: Client owes me
                        else -> MaterialTheme.colorScheme.onBackground
                    }

                    val balanceLabel = when {
                        balanceValue > 0 -> "Current Balance: You owe ${client.name}"
                        balanceValue < 0 -> "Current Balance: ${client.name} owes you"
                        else -> "Current Balance: Settled"
                    }

                    Text(
                        text = balanceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )

                    Text(
                        text = formatCurrency(balanceValue),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = statusColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Total Given (I gave)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatCurrency(givenTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Total Received (Paid to me)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatCurrency(receivedTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                    }

                    // Native PDF Statement Button
                    Button(
                        onClick = {
                            val rangeStr = if (filterStartDate != null || filterEndDate != null) {
                                val s = filterStartDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } ?: "Beginning"
                                val e = filterEndDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } ?: "Today"
                                "$s to $e"
                            } else null
                            
                            sharePdfStatement(
                                context = context,
                                client = client,
                                operations = filteredOperations,
                                balance = balanceValue,
                                totalGiven = givenTotal,
                                totalReceived = receivedTotal,
                                dateRangeStr = rangeStr
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.fillMaxWidth().testTag("export_pdf_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF icon", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Statement (PDF)", fontSize = 13.sp)
                    }
                }
            }

            // Operations Search/Filter Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // search keyword
                    OutlinedTextField(
                        value = operationsSearchQuery,
                        onValueChange = { viewModel.operationSearchQuery.value = it },
                        label = { Text("Filter logs...", fontSize = 11.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    // Direction filters: GIVEN, RECEIVED, ALL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("ALL", "GIVEN", "RECEIVED").forEach { filter ->
                            val selected = operationsFilterType == filter
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
                                        "GIVEN" -> "Given"
                                        "RECEIVED" -> "Received"
                                        else -> "All Type"
                                    },
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // range picker shortcuts bounds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sText = if (filterStartDate != null) SimpleDateFormat("yy-MM-dd", Locale.getDefault()).format(Date(filterStartDate!!)) else "Start"
                        val eText = if (filterEndDate != null) SimpleDateFormat("yy-MM-dd", Locale.getDefault()).format(Date(filterEndDate!!)) else "End"

                        OutlinedButton(
                            onClick = {
                                showPicker(context, filterStartDate ?: System.currentTimeMillis()) { viewModel.filterStartDate.value = it }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("From: $sText", fontSize = 10.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                showPicker(context, filterEndDate ?: System.currentTimeMillis()) { viewModel.filterEndDate.value = it }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("To: $eText", fontSize = 10.sp, maxLines = 1)
                        }

                        if (operationsSearchQuery.isNotEmpty() || operationsFilterType != "ALL" || filterStartDate != null || filterEndDate != null) {
                            IconButton(
                                onClick = { viewModel.clearFilters() },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Client transactions list
            if (filteredOperations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                        Text(text = "No operations match filters", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        if (rawOperations.isEmpty()) {
                            Text(text = "Tap the bottom right FAB to add your first transaction.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredOperations, key = { it.id }) { operation ->
                        OperationItemRow(
                            operation = operation,
                            onActionClick = {
                                selectedOpForAction = operation
                                showEditOperationDialog = true
                            },
                            onDeleteClick = {
                                selectedOpForAction = operation
                                showDeleteOpConfirmation = true
                            },
                            onViewAttachment = { path ->
                                activeViewerAttachmentPath = path
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Fullscreen Thumbnail Attachment Viewer
    if (activeViewerAttachmentPath != null) {
        val file = File(activeViewerAttachmentPath!!)
        AlertDialog(
            onDismissRequest = { activeViewerAttachmentPath = null },
            title = { Text("Attachment Photo Viewer") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    if (file.exists()) {
                        Image(
                            painter = rememberAsyncImagePainter(file),
                            contentDescription = "Attachment document",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Text("Source attachment is cached but couldn't be loaded (file path unretrievable).")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeViewerAttachmentPath = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Confirm Delete Client Account
    if (showDeleteClientConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteClientConfirmation = false },
            title = { Text("Delete Client Ledger Account?") },
            text = { Text("Warning: Deleting ${client.name} will permanently remove all logs and cash transactions associated with them. This is irreversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClient(client)
                        showDeleteClientConfirmation = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteClientConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Delete individual Operation
    if (showDeleteOpConfirmation && selectedOpForAction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteOpConfirmation = false },
            title = { Text("Delete Transaction Log?") },
            text = { Text("Are you sure you want to delete this operation of ${formatCurrency(selectedOpForAction!!.amount)}? This will shift the computed client balance.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOperation(selectedOpForAction!!)
                        showDeleteOpConfirmation = false
                        selectedOpForAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteOpConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Client Info modal
    if (showEditClientDialog) {
        var editName by remember { mutableStateOf(client.name) }
        var editPhone by remember { mutableStateOf(client.phone) }
        var editEmail by remember { mutableStateOf(client.email) }
        var editNotes by remember { mutableStateOf(client.notes) }

        AlertDialog(
            onDismissRequest = { showEditClientDialog = false },
            title = { Text("Edit Client Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth().testTag("edit_client_name_field")
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("edit_client_phone_field")
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth().testTag("edit_client_email_field")
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Internal reference notes") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_client_notes_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateClient(
                                client.copy(
                                    name = editName.trim(),
                                    phone = editPhone.trim(),
                                    email = editEmail.trim(),
                                    notes = editNotes.trim()
                                )
                            )
                            showEditClientDialog = false
                        } else {
                            Toast.makeText(context, "Full name cannot be blank", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_client_changes_btn")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditClientDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Operation Modal Builder
    if (showAddOperationDialog) {
        OperationFormDialog(
            title = "Log New Operation",
            defaultType = "GIVEN",
            defaultAmount = "",
            defaultNotes = "",
            defaultDate = System.currentTimeMillis(),
            defaultAttachment = null,
            confirmButtonLabel = "Record Transaction",
            onDismiss = { showAddOperationDialog = false },
            onSubmit = { amount, type, date, notes, attach ->
                viewModel.insertOperation(
                    clientId = client.id,
                    date = date,
                    amount = amount,
                    type = type,
                    notes = notes,
                    attachmentPath = attach
                )
                showAddOperationDialog = false
            }
        )
    }

    // Edit Operation Modal Builder
    if (showEditOperationDialog && selectedOpForAction != null) {
        OperationFormDialog(
            title = "Edit Operation Log",
            defaultType = selectedOpForAction!!.type,
            defaultAmount = selectedOpForAction!!.amount.toString(),
            defaultNotes = selectedOpForAction!!.notes,
            defaultDate = selectedOpForAction!!.date,
            defaultAttachment = selectedOpForAction!!.attachmentPath,
            confirmButtonLabel = "Update Log",
            onDismiss = {
                showEditOperationDialog = false
                selectedOpForAction = null
            },
            onSubmit = { amount, type, date, notes, attach ->
                viewModel.updateOperation(
                    selectedOpForAction!!.copy(
                        amount = amount,
                        type = type,
                        date = date,
                        notes = notes,
                        attachmentPath = attach
                    )
                )
                showEditOperationDialog = false
                selectedOpForAction = null
            }
        )
    }
}

@Composable
fun OperationItemRow(
    operation: Operation,
    onActionClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onViewAttachment: (String) -> Unit
) {
    val isGiven = operation.type == "GIVEN"
    val dateText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(operation.date))
    val textAndIconColor = if (isGiven) Color(0xFF065F46) else Color(0xFF9F1239)
    val containerBgColor = if (isGiven) Color(0xFFD1FAE5) else Color(0xFFFFE4E6)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("operation_row_${operation.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction badge + timestamp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(containerBgColor, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isGiven) "GIVEN" else "RECEIVED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textAndIconColor
                        )
                    }

                    Text(
                        text = dateText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Action buttons: Edit, Delete
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onActionClick, modifier = Modifier.size(24.dp).testTag("edit_op_${operation.id}")) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Log", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp).testTag("delete_op_${operation.id}")) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Log", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Notes & attachments links
                Column(
                    modifier = Modifier.weight(1.5f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = operation.notes.ifEmpty { "No transaction reference" },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Optional Attachment Thumbnail
                    if (operation.attachmentPath != null) {
                        val file = File(operation.attachmentPath)
                        if (file.exists()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .clickable { onViewAttachment(operation.attachmentPath) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attachment Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "View Receipt",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Amount display
                Text(
                    text = formatCurrency(operation.amount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = textAndIconColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationFormDialog(
    title: String,
    defaultType: String,
    defaultAmount: String,
    defaultNotes: String,
    defaultDate: Long,
    defaultAttachment: String?,
    confirmButtonLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, type: String, date: Long, notes: String, attachmentPath: String?) -> Unit
) {
    val context = LocalContext.current

    var typeState by remember { mutableStateOf(defaultType) } // "GIVEN" or "RECEIVED"
    var amountState by remember { mutableStateOf(defaultAmount) }
    var notesState by remember { mutableStateOf(defaultNotes) }
    var dateState by remember { mutableStateOf(defaultDate) }
    var dateString by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(defaultDate))) }
    var attachmentPathState by remember { mutableStateOf<String?>(defaultAttachment) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val localPath = AttachmentHelper.saveUriToAppStorage(context, uri)
            if (localPath != null) {
                attachmentPathState = localPath
                Toast.makeText(context, "Attachment saved successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Given / Received Toggler (Segmented button structure style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { typeState = "GIVEN" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (typeState == "GIVEN") Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (typeState == "GIVEN") Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = if (typeState == "GIVEN") BorderStroke(1.5.dp, Color(0xFF2E7D32)) else null
                    ) {
                        Text("Given (I Gave)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { typeState = "RECEIVED" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (typeState == "RECEIVED") Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (typeState == "RECEIVED") Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = if (typeState == "RECEIVED") BorderStroke(1.5.dp, Color(0xFFC62828)) else null
                    ) {
                        Text("Received (Got)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                // Amount Text Field (Number keyboard)
                OutlinedTextField(
                    value = amountState,
                    onValueChange = { amountState = it },
                    label = { Text("Transaction Amount *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("op_amount_input")
                )

                // Date Picker Button Trigger
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPicker(context, dateState) { picked ->
                                dateState = picked
                                dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(picked))
                            }
                        }
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Date: $dateString", fontSize = 14.sp)
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                // Transaction notes
                OutlinedTextField(
                    value = notesState,
                    onValueChange = { notesState = it },
                    label = { Text("Notes / Details") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("op_notes_input")
                )

                // Document Attachment Picker
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Receipt Document File", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            
                            IconButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                                Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Pick Image", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (attachmentPathState != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val fileName = File(attachmentPathState!!).name
                                Text(
                                    text = fileName,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { attachmentPathState = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Delete attachment", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            Text("No voucher or statement image attached.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val doubleAmount = amountState.toDoubleOrNull()
                    if (doubleAmount != null && doubleAmount > 0) {
                        onSubmit(doubleAmount, typeState, dateState, notesState.trim(), attachmentPathState)
                    } else {
                        Toast.makeText(context, "Please enter a valid positive financial amount.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("submit_operation_btn")
            ) {
                Text(confirmButtonLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun showPicker(context: Context, currentDate: Long, action: (Long) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = currentDate
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val resultCal = Calendar.getInstance()
            resultCal.set(year, month, dayOfMonth, 0, 0, 0)
            action(resultCal.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun triggerPhone(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    } catch (_: Exception) {}
}

private fun triggerEmail(context: Context, email: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
    } catch (_: Exception) {}
}

private fun sharePdfStatement(
    context: Context,
    client: Client,
    operations: List<Operation>,
    balance: Double,
    totalGiven: Double,
    totalReceived: Double,
    dateRangeStr: String?
) {
    try {
        val pdfFile = com.example.util.FinanceExportHelper.generateClientStatementPdf(
            context = context,
            client = client,
            operations = operations,
            balance = balance,
            totalGiven = totalGiven,
            totalReceived = totalReceived,
            dateRangeStr = dateRangeStr
        )
        val authority = "${context.packageName}.fileprovider"
        val pdfUri = FileProvider.getUriForFile(context, authority, pdfFile)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "Statement of Account - ${client.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Client Statement"))
    } catch (e: Exception) {
        Toast.makeText(context, "PDF generation failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
