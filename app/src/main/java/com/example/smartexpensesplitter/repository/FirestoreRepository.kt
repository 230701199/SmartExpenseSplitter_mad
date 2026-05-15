package com.example.smartexpensesplitter.repository

import com.example.smartexpensesplitter.model.Expense
import com.example.smartexpensesplitter.model.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Singleton repository for all Firestore operations.
 *
 * Firestore Schema:
 *   users/{uid}/groups/{groupId}              ← Group metadata
 *   users/{uid}/groups/{groupId}/expenses/{docId} ← Expense documents
 */
object FirestoreRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /** Base user document — asserts user is logged in */
    private fun userDoc() =
        db.collection("users").document(auth.currentUser!!.uid)

    /** Collection: users/{uid}/groups */
    private fun groupsCollection() =
        userDoc().collection("groups")

    /** Collection: users/{uid}/groups/{groupId}/expenses */
    private fun expensesCollection(groupId: String) =
        groupsCollection().document(groupId).collection("expenses")

    // ─── GROUP OPERATIONS ────────────────────────────────────────────────────

    /** Persist a new group document to Firestore */
    fun saveGroup(
        group: Group,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val data = hashMapOf(
            "groupId"   to group.groupId,
            "groupName" to group.groupName,
            "createdBy" to group.createdBy,
            "createdAt" to group.createdAt
        )
        groupsCollection()
            .document(group.groupId)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    /** Real-time listener for all groups; returns sorted by newest first */
    fun listenToGroups(onUpdate: (List<Group>) -> Unit) {
        groupsCollection()
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val groups = snapshot.documents.mapNotNull { doc ->
                    try {
                        Group(
                            groupId   = doc.getString("groupId")   ?: doc.id,
                            groupName = doc.getString("groupName") ?: "",
                            createdBy = doc.getString("createdBy") ?: "",
                            createdAt = doc.getLong("createdAt")   ?: 0L
                        )
                    } catch (e: Exception) { null }
                }.sortedByDescending { it.createdAt }
                onUpdate(groups)
            }
    }

    // ─── EXPENSE OPERATIONS ──────────────────────────────────────────────────

    /**
     * Save a new expense using a Firestore auto-generated document ID.
     * The generated ID is written back into the "docId" field for easy retrieval.
     */
    fun saveExpense(
        groupId: String,
        expense: Expense,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val ref = expensesCollection(groupId).document() // auto-ID
        val data = hashMapOf(
            "docId"        to ref.id,
            "amount"       to expense.amount,
            "paidBy"       to expense.paidBy,
            "splitBetween" to expense.splitBetween,
            "note"         to expense.note,
            "timestamp"    to expense.timestamp,
            "category"     to expense.category
        )
        ref.set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    /** Real-time listener for all expenses in a group; newest first */
    fun listenToExpenses(groupId: String, onUpdate: (List<Expense>) -> Unit) {
        expensesCollection(groupId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val expenses = snapshot.documents.mapNotNull { doc ->
                    try {
                        Expense(
                            docId        = doc.getString("docId")   ?: doc.id,
                            amount       = doc.getDouble("amount")  ?: 0.0,
                            paidBy       = doc.getString("paidBy")  ?: "",
                            splitBetween = (doc.get("splitBetween") as? List<*>)
                                              ?.filterIsInstance<String>() ?: emptyList(),
                            note         = doc.getString("note")     ?: "",
                            timestamp    = doc.getLong("timestamp")  ?: 0L,
                            category     = doc.getString("category") ?: "OTHER"
                        )
                    } catch (e: Exception) { null }
                }.sortedByDescending { it.timestamp }
                onUpdate(expenses)
            }
    }

    /**
     * Delete an expense document by its Firestore document ID.
     * [docId] must match the "docId" field stored in the document.
     */
    fun deleteExpense(
        groupId: String,
        docId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        expensesCollection(groupId)
            .document(docId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }
}
