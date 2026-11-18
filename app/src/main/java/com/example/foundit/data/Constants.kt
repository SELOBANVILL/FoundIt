package com.example.foundit.data

/** Finders must give a school email ending in this domain. */
const val CIIT_EMAIL_DOMAIN = "ciit.edu.ph"

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
