package com.example.model

data class SavingsGoal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val targetAmount: Double,
    val category: String, // e.g. "Vacation Fund", "Emergency Fund", "Down Payment"
    val currentAmount: Double = 0.0,
    val emoji: String = "🎯",
    val colorHex: Long = 0xFF10B981L, // Emerald by default
    val targetDate: String? = null,
    val isCompleted: Boolean = false,
    val notes: String = ""
)
