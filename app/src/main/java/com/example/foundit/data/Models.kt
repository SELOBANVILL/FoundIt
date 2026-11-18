package com.example.foundit.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val location: String? = null,
    val type: String,
    val status: String = "OPEN",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("contact_email") val contactEmail: String? = null,
    @SerialName("contact_info") val contactInfo: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// Used when inserting. id, created_at and status are set by the database;
// user_id is re-checked by RLS and overwritten by a server trigger.
@Serializable
data class NewItem(
    @SerialName("user_id") val userId: String,
    val name: String,
    val description: String,
    val category: String,
    val location: String,
    val type: String,
    @SerialName("image_url") val imageUrl: String? = null,
    // Shown publicly so the finder / owner can reach whoever posted.
    @SerialName("contact_email") val contactEmail: String? = null,
    @SerialName("contact_info") val contactInfo: String? = null
)

@Serializable
data class Comment(
    val id: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    val text: String,
    @SerialName("is_claim") val isClaim: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

// user_name is NOT sent: the server fills it in from the verified login email.
@Serializable
data class NewComment(
    @SerialName("item_id") val itemId: String,
    @SerialName("user_id") val userId: String,
    val text: String,
    @SerialName("is_claim") val isClaim: Boolean
)