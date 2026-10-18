package com.swordfish.libretrodroid

/**
 * A core option. [key] and [value] are all the frontend needs to set one; the remaining fields
 * describe options the core declared (legacy variables or core options v1/v2) and stay null for
 * values the core never declared.
 */
data class Variable(
    val key: String? = null,
    val value: String? = null,
    val description: String? = null,
    val label: String? = null,
    val labelCategorized: String? = null,
    val info: String? = null,
    val category: String? = null,
    val categoryLabel: String? = null,
    val categoryInfo: String? = null,
    val values: Array<String>? = null,
    val valueLabels: Array<String>? = null,
    val defaultValue: String? = null,
    val visible: Boolean = true,
)
