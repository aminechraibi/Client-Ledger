package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Client
import com.example.data.ClientWithBalance
import com.example.data.FinanceRepository
import com.example.data.Operation
import com.example.util.FinanceExportHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(database.appDao())

    // Search query for filtering clients
    val clientSearchQuery = MutableStateFlow("")

    // List of all operations
    val allOperations: StateFlow<List<Operation>> = repository.allOperations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Raw clients with balances from repository
    private val rawClientsWithBalances = repository.clientsWithBalances

    // Filtered clients with balances based on search query
    val clientsWithBalances: StateFlow<List<ClientWithBalance>> = combine(
        rawClientsWithBalances,
        clientSearchQuery
    ) { clients, query ->
        if (query.isBlank()) {
            clients
        } else {
            clients.filter { it.client.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard metrics
    val totalOwedByMe: StateFlow<Double> = rawClientsWithBalances
        .combine(flowOf(Unit)) { clients, _ ->
            clients.filter { it.balance > 0 }.sumOf { it.balance }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalOwedToMe: StateFlow<Double> = rawClientsWithBalances
        .combine(flowOf(Unit)) { clients, _ ->
            clients.filter { it.balance < 0 }.sumOf { -it.balance }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Filters for Operations (Reports, History search, Date Ranges)
    val operationSearchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow("ALL") // "ALL", "GIVEN", "RECEIVED"
    val filterStartDate = MutableStateFlow<Long?>(null)
    val filterEndDate = MutableStateFlow<Long?>(null)

    // Combined stream of filtered operations across ALL clients for central logs report
    val filteredOperations: StateFlow<List<Operation>> = combine(
        repository.allOperations,
        operationSearchQuery,
        filterType,
        filterStartDate,
        filterEndDate
    ) { ops, query, type, start, end ->
        ops.filter { op ->
            val matchesQuery = query.isBlank() || op.notes.contains(query, ignoreCase = true)
            val matchesType = type == "ALL" || op.type == type
            val matchesStart = start == null || op.date >= start
            // Note: end date filters are inclusive of the entire day, so we check <= end
            val matchesEnd = end == null || op.date <= end
            matchesQuery && matchesType && matchesStart && matchesEnd
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map of client IDs to Clients for fast lookups
    val clientsMap: StateFlow<Map<Int, Client>> = repository.allClients
        .combine(flowOf(Unit)) { list, _ ->
            list.associateBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Selected client for the profile route
    val selectedClientId = MutableStateFlow<Int?>(null)

    val selectedClientOperations: StateFlow<List<Operation>> = selectedClientId
        .flatMapLatest { id ->
            if (id != null) repository.getOperationsForClient(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered selected client operations
    val selectedClientFilteredOperations: StateFlow<List<Operation>> = combine(
        selectedClientOperations,
        operationSearchQuery,
        filterType,
        filterStartDate,
        filterEndDate
    ) { ops, query, type, start, end ->
        ops.filter { op ->
            val matchesQuery = query.isBlank() || op.notes.contains(query, ignoreCase = true)
            val matchesType = type == "ALL" || op.type == type
            val matchesStart = start == null || op.date >= start
            val matchesEnd = end == null || op.date <= end
            matchesQuery && matchesType && matchesStart && matchesEnd
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Operations for current client metrics
    val selectedClientGivenTotal: StateFlow<Double> = selectedClientOperations
        .combine(flowOf(Unit)) { ops, _ ->
            ops.filter { it.type == "GIVEN" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val selectedClientReceivedTotal: StateFlow<Double> = selectedClientOperations
        .combine(flowOf(Unit)) { ops, _ ->
            ops.filter { it.type == "RECEIVED" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val selectedClientBalance: StateFlow<Double> = selectedClientOperations
        .combine(flowOf(Unit)) { ops, _ ->
            val given = ops.filter { it.type == "GIVEN" }.sumOf { it.amount }
            val rec = ops.filter { it.type == "RECEIVED" }.sumOf { it.amount }
            given - rec
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Back up and Restore feedback
    val backupRestoreStatus = MutableStateFlow<String?>(null)

    // --- Actions ---

    fun insertClient(name: String, phone: String, email: String, notes: String) {
        viewModelScope.launch {
            repository.insertClient(
                Client(name = name, phone = phone, email = email, notes = notes)
            )
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch {
            repository.updateClient(client)
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
            if (selectedClientId.value == client.id) {
                selectedClientId.value = null
            }
        }
    }

    fun insertOperation(clientId: Int, date: Long, amount: Double, type: String, notes: String, attachmentPath: String?) {
        viewModelScope.launch {
            repository.insertOperation(
                Operation(
                    clientId = clientId,
                    date = date,
                    amount = amount,
                    type = type,
                    notes = notes,
                    attachmentPath = attachmentPath
                )
            )
        }
    }

    fun updateOperation(operation: Operation) {
        viewModelScope.launch {
            repository.updateOperation(operation)
        }
    }

    fun deleteOperation(operation: Operation) {
        viewModelScope.launch {
            repository.deleteOperation(operation)
        }
    }

    fun clearFilters() {
        operationSearchQuery.value = ""
        filterType.value = "ALL"
        filterStartDate.value = null
        filterEndDate.value = null
    }

    // --- Backup & Restore execution ---

    fun triggerBackup(): String? {
        return try {
            val clients = rawClientsWithBalances.stateIn(
                viewModelScope, SharingStarted.Eagerly, emptyList()
            ).value.map { it.client }
            
            val operations = allOperations.value
            val json = FinanceExportHelper.exportBackupJson(clients, operations)
            backupRestoreStatus.value = "Backup created successfully!"
            json
        } catch (e: Exception) {
            backupRestoreStatus.value = "Backup failed: ${e.message}"
            null
        }
    }

    fun triggerRestore(jsonString: String) {
        viewModelScope.launch {
            try {
                val (clients, operations) = FinanceExportHelper.importBackupJson(jsonString)
                // Perform cascade clear
                database.appDao().clearAllClients()
                
                // Re-insert clients, keeping track of pre-existing IDs (although they will autoinsert with CASCADE mapping)
                for (client in clients) {
                    database.appDao().insertClient(client)
                }
                for (op in operations) {
                    database.appDao().insertOperation(op)
                }
                backupRestoreStatus.value = "Backup restored successfully!"
            } catch (e: Exception) {
                backupRestoreStatus.value = "Restore failed: ${e.message}"
            }
        }
    }

    fun dismissBackupStatus() {
        backupRestoreStatus.value = null
    }
}
