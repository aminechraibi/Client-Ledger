package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Client
import com.example.data.ClientWithBalance
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    viewModel: FinanceViewModel,
    onNavigateToClientProfile: (Int) -> Unit
) {
    val context = LocalContext.current
    val clientsWithBalances by viewModel.clientsWithBalances.collectAsState()
    val searchQuery by viewModel.clientSearchQuery.collectAsState()

    var showAddClientDialog by remember { mutableStateOf(false) }

    // Dialog state
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var clientNotes by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    clientName = ""
                    clientPhone = ""
                    clientEmail = ""
                    clientNotes = ""
                    showAddClientDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_client_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Client")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Client Ledger Accounts",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.clientSearchQuery.value = it },
                label = { Text("Search client name...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clientSearchQuery.value = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("client_search_input")
            )

            if (clientsWithBalances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching clients found" else "No clients logged yet",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try a different spelling or name search" else "Tap the '+' button at the bottom right to record your first client.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(clientsWithBalances, key = { it.client.id }) { item ->
                        ClientRow(
                            item = item,
                            onClick = { onNavigateToClientProfile(item.client.id) },
                            onCall = { phone -> triggerPhoneCall(context, phone) },
                            onEmail = { email -> triggerEmailSend(context, email) }
                        )
                    }
                }
            }
        }
    }

    // Add Client Dialog
    if (showAddClientDialog) {
        AlertDialog(
            onDismissRequest = { showAddClientDialog = false },
            title = { Text("Create New Client Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Full Name *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth().testTag("add_client_name_field")
                    )

                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("add_client_phone_field")
                    )

                    OutlinedTextField(
                        value = clientEmail,
                        onValueChange = { clientEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth().testTag("add_client_email_field")
                    )

                    OutlinedTextField(
                        value = clientNotes,
                        onValueChange = { clientNotes = it },
                        label = { Text("Internal Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_client_notes_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.trim().isNotEmpty()) {
                            viewModel.insertClient(
                                name = clientName.trim(),
                                phone = clientPhone.trim(),
                                email = clientEmail.trim(),
                                notes = clientNotes.trim()
                            )
                            showAddClientDialog = false
                        } else {
                            Toast.makeText(context, "Full name is a mandatory field.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("submit_client_button")
                ) {
                    Text("Add Client")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClientDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ClientRow(
    item: ClientWithBalance,
    onClick: () -> Unit,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit
) {
    val client = item.client
    val balance = item.balance

    val avatarBg = when {
        balance > 0 -> Color(0xFFD1FAE5) // bg-emerald-100
        balance < 0 -> Color(0xFFFFE4E6) // bg-rose-100
        else -> Color(0xFFF1F5F9) // bg-slate-100
    }
    val avatarText = when {
        balance > 0 -> Color(0xFF065F46) // Emerald-800
        balance < 0 -> Color(0xFF9F1239) // Rose-800
        else -> Color(0xFF475569) // Slate-600
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("client_row_${client.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rounded-2xl avatar initials badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(avatarBg, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (client.name.length >= 2) client.name.substring(0, 2).uppercase() else client.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = avatarText,
                    fontSize = 15.sp
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1.5f)
            ) {
                Text(
                    text = client.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Contact Info badges
                if (client.phone.isNotEmpty() || client.email.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (client.phone.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Has phone",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onCall(client.phone) }
                            )
                        }
                        if (client.email.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Has email",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onEmail(client.email) }
                            )
                        }
                    }
                }
            }

            // Right side running balance
            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val balanceColor = when {
                    balance > 0 -> Color(0xFF065F46) // Positive: Green (I owe the client)
                    balance < 0 -> Color(0xFF9F1239) // Negative: Red (Client owes me)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Settled
                }

                val balanceLabel = when {
                    balance > 0 -> "I owe them"
                    balance < 0 -> "They owe me"
                    else -> "Settled"
                }

                Text(
                    text = formatCurrency(balance),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = balanceColor,
                    maxLines = 1
                )

                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                balance > 0 -> Color(0xFFD1FAE5)
                                balance < 0 -> Color(0xFFFFE4E6)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = balanceLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                }
            }
        }
    }
}

private fun triggerPhoneCall(context: Context, number: String) {
    try {
        val u = Uri.parse("tel:" + number.trim())
        val intent = Intent(Intent.ACTION_DIAL, u)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open dialer app", Toast.LENGTH_SHORT).show()
    }
}

private fun triggerEmailSend(context: Context, emailAddress: String) {
    try {
        val u = Uri.parse("mailto:" + emailAddress.trim())
        val intent = Intent(Intent.ACTION_SENDTO, u)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot find default email client", Toast.LENGTH_SHORT).show()
    }
}
