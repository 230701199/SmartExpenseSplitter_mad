package com.example.smartexpensesplitter.model

/**
 * Expense categories with emoji labels and ARGB color values.
 * Colors are stored as Long so the model layer stays free of Compose imports.
 * In UI, use Color(category.colorValue) to get the Compose Color.
 */
enum class ExpenseCategory(
    val label: String,
    val emoji: String,
    val colorValue: Long   // 0xAARRGGBB
) {
    FOOD("Food", "🍕", 0xFFFF6B6BL),
    TRAVEL("Travel", "✈️", 0xFF4ECDC4L),
    STAY("Stay", "🏨", 0xFF45B7D1L),
    OTHER("Other", "📦", 0xFFA8A8A8L);

    companion object {
        /** Safely parse a stored string back to an enum, defaulting to OTHER */
        fun fromString(value: String): ExpenseCategory =
            entries.find { it.name == value } ?: OTHER
    }
}
