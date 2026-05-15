package com.example.smartexpensesplitter.model

/**
 * Represents a named group/trip (e.g., "Goa Trip", "Flat Expenses").
 * Each group has its own isolated list of expenses in Firestore.
 *
 * Firestore path: users/{uid}/groups/{groupId}
 */
data class Group(
    val groupId: String = "",
    val groupName: String = "",
    val createdBy: String = "",   // uid of creator
    val createdAt: Long = 0L      // System.currentTimeMillis()
)
