package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "operations",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clientId"])]
)
data class Operation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val date: Long,
    val amount: Double,
    val type: String, // "GIVEN" or "RECEIVED"
    val notes: String = "",
    val attachmentPath: String? = null
)

// Helper container that bundles calculated values for each client
data class ClientWithBalance(
    val client: Client,
    val balance: Double,
    val totalGiven: Double,
    val totalReceived: Double,
    val lastActive: Long
)
