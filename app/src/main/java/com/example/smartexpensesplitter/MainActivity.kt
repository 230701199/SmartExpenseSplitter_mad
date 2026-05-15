package com.example.smartexpensesplitter

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartexpensesplitter.ui.ExpenseScreen
import com.example.smartexpensesplitter.ui.GroupScreen
import com.example.smartexpensesplitter.ui.theme.SmartExpenseSplitterTheme
import com.example.smartexpensesplitter.viewmodel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    // ViewModel scoped to this Activity's lifecycle
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Guard: redirect to Auth if not logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        val userEmail = auth.currentUser?.email ?: "User"

        setContent {
            SmartExpenseSplitterTheme {

                // Collect state from ViewModel
                val groups        by viewModel.groups.collectAsStateWithLifecycle()
                val expenses      by viewModel.expenses.collectAsStateWithLifecycle()
                val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
                val errorMessage  by viewModel.errorMessage.collectAsStateWithLifecycle()

                // Show Toast on errors
                LaunchedEffect(errorMessage) {
                    errorMessage?.let { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }

                // ── Navigation: Group List ↔ Expense Screen ───────────────────
                if (selectedGroup == null) {
                    // Screen 1: Group list
                    GroupScreen(
                        groups        = groups,
                        userEmail     = userEmail,
                        onGroupSelected = { group -> viewModel.selectGroup(group) },
                        onCreateGroup   = { name  -> viewModel.createGroup(name) },
                        onLogout        = { logout() }
                    )
                } else {
                    // Screen 2: Expenses for selected group
                    ExpenseScreen(
                        group        = selectedGroup!!,
                        expenses     = expenses,
                        userEmail    = userEmail,
                        onAddExpense = { amount, paidBy, split, note, category ->
                            viewModel.addExpense(amount, paidBy, split, note, category)
                        },
                        onDeleteExpense = { docId -> viewModel.deleteExpense(docId) },
                        onBack    = { viewModel.clearSelectedGroup() },
                        onLogout  = { logout() }
                    )
                }
            }
        }
    }

    /** Sign out and return to AuthActivity */
    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    /** Handle Android back button — navigate back to group list if in expense screen */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (viewModel.selectedGroup.value != null) {
            viewModel.clearSelectedGroup()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
