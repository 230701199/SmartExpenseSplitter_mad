package com.example.smartexpensesplitter.viewmodel

import androidx.lifecycle.ViewModel
import com.example.smartexpensesplitter.model.Expense
import com.example.smartexpensesplitter.model.Group
import com.example.smartexpensesplitter.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared ViewModel for groups and expenses.
 * Holds UI state and delegates all Firestore operations to [FirestoreRepository].
 */
class ExpenseViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // ─── Exposed State ───────────────────────────────────────────────────────

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    /** Currently open group; null means show the group list screen */
    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup.asStateFlow()

    /** One-shot error message for Snackbar / Toast */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Start listening to groups as soon as ViewModel is created
        listenToGroups()
    }

    // ─── Group Actions ───────────────────────────────────────────────────────

    private fun listenToGroups() {
        FirestoreRepository.listenToGroups { updated -> _groups.value = updated }
    }

    /** Open a group and begin listening to its expenses */
    fun selectGroup(group: Group) {
        _selectedGroup.value = group
        _expenses.value = emptyList()
        FirestoreRepository.listenToExpenses(group.groupId) { updated ->
            _expenses.value = updated
        }
    }

    /** Navigate back to the group list */
    fun clearSelectedGroup() {
        _selectedGroup.value = null
        _expenses.value = emptyList()
    }

    /** Create and persist a new named group */
    fun createGroup(name: String) {
        val uid = auth.currentUser?.uid ?: return
        val groupId = System.currentTimeMillis().toString()
        FirestoreRepository.saveGroup(
            Group(
                groupId   = groupId,
                groupName = name.trim(),
                createdBy = uid,
                createdAt = System.currentTimeMillis()
            ),
            onSuccess = {},
            onError   = { e -> _errorMessage.value = "Failed to create group: ${e.message}" }
        )
    }

    // ─── Expense Actions ─────────────────────────────────────────────────────

    /** Add an expense to the currently selected group */
    fun addExpense(
        amount: Double,
        paidBy: String,
        splitBetween: List<String>,
        note: String,
        category: String
    ) {
        val groupId = _selectedGroup.value?.groupId ?: return
        FirestoreRepository.saveExpense(
            groupId,
            Expense(
                amount       = amount,
                paidBy       = paidBy,
                splitBetween = splitBetween,
                note         = note.trim(),
                timestamp    = System.currentTimeMillis(),
                category     = category
            ),
            onSuccess = {},
            onError   = { e -> _errorMessage.value = "Failed to save expense: ${e.message}" }
        )
    }

    /** Delete an expense from the currently selected group */
    fun deleteExpense(docId: String) {
        val groupId = _selectedGroup.value?.groupId ?: return
        FirestoreRepository.deleteExpense(
            groupId, docId,
            onSuccess = {},
            onError   = { e -> _errorMessage.value = "Failed to delete: ${e.message}" }
        )
    }

    /** Clear the displayed error after it has been consumed */
    fun clearError() { _errorMessage.value = null }
}
