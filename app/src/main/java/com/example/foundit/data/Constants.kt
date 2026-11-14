package com.example.foundit.data

val CATEGORIES = listOf("Bags", "IDs & Cards", "Electronics", "Clothing", "Books", "Keys", "Others")

object ItemType {
    const val LOST = "LOST"
    const val FOUND = "FOUND"
    val ALL = listOf(LOST, FOUND)
}

object ItemStatus {
    const val OPEN = "OPEN"
    const val CLAIMED = "CLAIMED"
}
