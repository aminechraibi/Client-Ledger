package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Client queries
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("DELETE FROM clients")
    suspend fun clearAllClients()

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Int): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    // Operation queries
    @Query("SELECT * FROM operations ORDER BY date DESC")
    fun getAllOperations(): Flow<List<Operation>>

    @Query("SELECT * FROM operations WHERE clientId = :clientId ORDER BY date DESC")
    fun getOperationsForClient(clientId: Int): Flow<List<Operation>>

    @Query("SELECT * FROM operations WHERE id = :id")
    suspend fun getOperationById(id: Int): Operation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: Operation): Long

    @Update
    suspend fun updateOperation(operation: Operation)

    @Delete
    suspend fun deleteOperation(operation: Operation)
}
