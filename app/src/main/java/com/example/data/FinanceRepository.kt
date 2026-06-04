package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FinanceRepository(private val appDao: AppDao) {

    val allClients: Flow<List<Client>> = appDao.getAllClients()
    val allOperations: Flow<List<Operation>> = appDao.getAllOperations()

    // Reactive stream combining clients and operations to compute client balances, totals, and activities
    val clientsWithBalances: Flow<List<ClientWithBalance>> = combine(
        allClients,
        allOperations
    ) { clients, operations ->
        clients.map { client ->
            val clientOps = operations.filter { it.clientId == client.id }
            val totalGiven = clientOps.filter { it.type == "GIVEN" }.sumOf { it.amount }
            val totalReceived = clientOps.filter { it.type == "RECEIVED" }.sumOf { it.amount }
            val balance = totalGiven - totalReceived
            val lastActive = if (clientOps.isNotEmpty()) {
                clientOps.maxOf { it.date }
            } else {
                client.createdAt
            }
            ClientWithBalance(
                client = client,
                balance = balance,
                totalGiven = totalGiven,
                totalReceived = totalReceived,
                lastActive = lastActive
            )
        }
    }

    fun getOperationsForClient(clientId: Int): Flow<List<Operation>> {
        return appDao.getOperationsForClient(clientId)
    }

    suspend fun getClientById(id: Int): Client? {
        return appDao.getClientById(id)
    }

    suspend fun insertClient(client: Client): Long {
        return appDao.insertClient(client)
    }

    suspend fun updateClient(client: Client) {
        appDao.updateClient(client)
    }

    suspend fun deleteClient(client: Client) {
        appDao.deleteClient(client)
    }

    suspend fun getOperationById(id: Int): Operation? {
        return appDao.getOperationById(id)
    }

    suspend fun insertOperation(operation: Operation): Long {
        return appDao.insertOperation(operation)
    }

    suspend fun updateOperation(operation: Operation) {
        appDao.updateOperation(operation)
    }

    suspend fun deleteOperation(operation: Operation) {
        appDao.deleteOperation(operation)
    }
}
