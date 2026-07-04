package nl.rolfpeters.larry.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lokale chatgeschiedenis (Room). Larry's backend heeft GEEN server-side sessie/geheugen
 * (bevestigd tijdens API-inventarisatie, zie scope.md) -- de app is dus zelf verantwoordelijk
 * voor het bewaren van de geschiedenis EN voor het meesturen van de volledige messages-array
 * bij elke request naar /larry/chat.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,       // "user" of "assistant" -- matcht Larry's API-contract 1-op-1
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val failed: Boolean = false, // true als de request voor dit bericht mislukte (netwerkfout e.d.)
)
