package com.example.smartexpensesplitter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensesplitter.model.Expense
import com.example.smartexpensesplitter.model.ExpenseCategory
import com.example.smartexpensesplitter.model.Group
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Utility Functions ────────────────────────────────────────────────────────

fun formatAmount(value: Double): String = String.format("%.2f", value)

/** Format timestamp to "15 May, 6:06 PM" */
fun formatExpenseDate(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(timestamp))
}

// ─── Expense Screen ───────────────────────────────────────────────────────────

/**
 * Full-featured expense screen for a selected [group].
 * Shows add-expense form + expense list with delete + settlement summary.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseScreen(
    group: Group,
    expenses: List<Expense>,
    userEmail: String,
    onAddExpense: (amount: Double, paidBy: String, split: List<String>, note: String, category: String) -> Unit,
    onDeleteExpense: (docId: String) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    // ─── Local form state ─────────────────────────────────────────────────────
    var amount        by remember { mutableStateOf("") }
    var newPerson     by remember { mutableStateOf("") }
    var people        by remember { mutableStateOf(listOf<String>()) }
    var selectedPaidBy  by remember { mutableStateOf("") }
    var selectedSplit   by remember { mutableStateOf(setOf<String>()) }
    var note          by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }

    // ─── Delete confirmation state ────────────────────────────────────────────
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            group.groupName,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ─── Add Person + Amount Form ────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Text("Add Expense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))

                        // ── Add person ───────────────────────────────────────
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newPerson,
                                onValueChange = { newPerson = it },
                                label = { Text("Person name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPerson.isNotBlank() && !people.contains(newPerson.trim())) {
                                        people = people + newPerson.trim()
                                        newPerson = ""
                                    }
                                }
                            ) { Text("Add") }
                        }

                        // Show added people as dismissible chips
                        if (people.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                people.forEach { person ->
                                    AssistChip(
                                        onClick = {
                                            people = people - person
                                            if (selectedPaidBy == person) selectedPaidBy = ""
                                            selectedSplit = selectedSplit - person
                                        },
                                        label = { Text(person) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Amount ───────────────────────────────────────────
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount (₹)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        // ── Note / Description ───────────────────────────────
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Expense note (e.g., Dinner, Petrol)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        // ── Category Chips ───────────────────────────────────
                        Text("Category", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExpenseCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text("${cat.emoji} ${cat.label}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(cat.colorValue).copy(alpha = 0.25f),
                                        selectedLabelColor = Color(cat.colorValue)
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Paid By ──────────────────────────────────────────
                        if (people.isNotEmpty()) {
                            Text("Paid By", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                people.forEach { person ->
                                    FilterChip(
                                        selected = selectedPaidBy == person,
                                        onClick = { selectedPaidBy = person },
                                        label = { Text(person) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // ── Split Between ────────────────────────────────
                            Text("Split Between", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                people.forEach { person ->
                                    FilterChip(
                                        selected = selectedSplit.contains(person),
                                        onClick = {
                                            selectedSplit = if (selectedSplit.contains(person))
                                                selectedSplit - person else selectedSplit + person
                                        },
                                        label = { Text(person) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // ── Submit Button ────────────────────────────────────
                        Button(
                            onClick = {
                                val amt = amount.toDoubleOrNull() ?: return@Button
                                if (selectedPaidBy.isNotEmpty() && selectedSplit.isNotEmpty() && amt > 0) {
                                    onAddExpense(
                                        amt,
                                        selectedPaidBy,
                                        selectedSplit.toList(),
                                        note,
                                        selectedCategory.name
                                    )
                                    // Reset form (keep people list)
                                    amount = ""
                                    note = ""
                                    selectedPaidBy = ""
                                    selectedSplit = emptySet()
                                    selectedCategory = ExpenseCategory.OTHER
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            enabled = amount.isNotBlank() && selectedPaidBy.isNotEmpty() && selectedSplit.isNotEmpty()
                        ) {
                            Text("Add Expense", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ─── Section header ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (expenses.isNotEmpty()) {
                        Text(
                            "${expenses.size} entries",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ─── Empty state ─────────────────────────────────────────────────
            if (expenses.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text("No expenses yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Add your first expense above",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ─── Expense Cards ───────────────────────────────────────────────
            items(expenses, key = { it.docId }) { expense ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
                ) {
                    ExpenseCard(
                        expense = expense,
                        onDeleteClick = { expenseToDelete = expense }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // ─── Settlement Section ──────────────────────────────────────────
            if (expenses.isNotEmpty()) {
                item { SettlementSection(expenses = expenses) }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    // ─── Delete Confirmation Dialog ───────────────────────────────────────────
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense?") },
            text = {
                Text(
                    "Remove ₹${formatAmount(expense.amount)} paid by ${expense.paidBy}?\n" +
                    "This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(expense.docId)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ─── Expense Card ─────────────────────────────────────────────────────────────

@Composable
private fun ExpenseCard(expense: Expense, onDeleteClick: () -> Unit) {
    val category = ExpenseCategory.fromString(expense.category)
    val categoryColor = Color(category.colorValue)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Top row: amount + category badge + delete ────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "₹${formatAmount(expense.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = categoryColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            "${category.emoji} ${category.label}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = categoryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Delete icon
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete expense",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Note (only if not blank) ─────────────────────────────────────
            if (expense.note.isNotBlank()) {
                Text(
                    "📝 ${expense.note}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }

            // ── Paid by + split ──────────────────────────────────────────────
            Text(
                "Paid by ${expense.paidBy}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Split: ${expense.splitBetween.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))

            // ── Per-person amount ────────────────────────────────────────────
            val perPerson = if (expense.splitBetween.isNotEmpty())
                expense.amount / expense.splitBetween.size else 0.0
            Text(
                "Each owes ₹${formatAmount(perPerson)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            // ── Date ─────────────────────────────────────────────────────────
            val dateStr = formatExpenseDate(expense.timestamp)
            if (dateStr.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Settlement Section ───────────────────────────────────────────────────────

@Composable
private fun SettlementSection(expenses: List<Expense>) {

    // Build balance map: positive = should receive, negative = owes
    val balanceMap = mutableMapOf<String, Double>()
    expenses.forEach { expense ->
        if (expense.splitBetween.isEmpty()) return@forEach
        val perPerson = expense.amount / expense.splitBetween.size
        expense.splitBetween.forEach { person ->
            if (person != expense.paidBy) {
                balanceMap[person] = (balanceMap[person] ?: 0.0) - perPerson
                balanceMap[expense.paidBy] = (balanceMap[expense.paidBy] ?: 0.0) + perPerson
            }
        }
    }

    val creditors = balanceMap.filter { it.value > 0.001 }.toMutableMap()
    val debtors   = balanceMap.filter { it.value < -0.001 }.toMutableMap()

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    Text("Final Settlement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))

    if (creditors.isEmpty() && debtors.isEmpty()) {
        Text(
            "✅ Everyone is settled up!",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    } else {
        // Greedy settlement algorithm
        val creditorsWork = creditors.toMutableMap()
        val debtorsWork   = debtors.toMutableMap()

        while (debtorsWork.isNotEmpty() && creditorsWork.isNotEmpty()) {
            val debtor   = debtorsWork.entries.first()
            val creditor = creditorsWork.entries.first()
            val pay      = minOf(-debtor.value, creditor.value)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "💸 ${debtor.key} → ${creditor.key}  ₹${formatAmount(pay)}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            debtorsWork[debtor.key] = debtor.value + pay
            creditorsWork[creditor.key] = creditor.value - pay
            if (debtorsWork[debtor.key]!! >= -0.001) debtorsWork.remove(debtor.key)
            if (creditorsWork[creditor.key]!! <= 0.001) creditorsWork.remove(creditor.key)
        }
    }

    Spacer(Modifier.height(16.dp))

    // Summary
    Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))

    balanceMap.filter { it.value > 0.001 }
        .toList().sortedByDescending { it.second }
        .forEach { (person, bal) ->
            Text(
                "🟢 $person receives ₹${formatAmount(bal)}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

    balanceMap.filter { it.value < -0.001 }
        .toList().sortedBy { it.second }
        .forEach { (person, bal) ->
            Text(
                "🔴 $person owes ₹${formatAmount(-bal)}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
}
