package com.example.smartexpensesplitter.model

/**
 * Represents a single expense entry within a group.
 *
 * Firestore path: users/{uid}/groups/{groupId}/expenses/{docId}
 *
 * - [docId]        : Firestore auto-generated document ID (used for delete)
 * - [timestamp]    : Unix millis; formatted to "15 May, 6:06 PM" in the UI
 * - [category]     : Stored as ExpenseCategory enum name (e.g., "FOOD")
 * - [note]         : Optional description; hidden on card when blank
 */
data class Expense(
    val docId: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val splitBetween: List<String> = emptyList(),
    val note: String = "",
    val timestamp: Long = 0L,
    val category: String = ExpenseCategory.OTHER.name
)