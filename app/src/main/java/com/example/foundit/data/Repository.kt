package com.example.foundit.data

import com.example.foundit.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import java.util.UUID

object Repository {
    private const val BUCKET = "item-images"

    // ---------- AUTH ----------
    suspend fun signUp(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() = supabase.auth.signOut()

    fun userId(): String = supabase.auth.currentUserOrNull()?.id ?: error("Not logged in")

    fun userName(): String =
        supabase.auth.currentUserOrNull()?.email?.substringBefore("@") ?: "user"

    // ---------- PHOTO ----------
    suspend fun uploadImage(bytes: ByteArray): String {
        val path = "${userId()}/${UUID.randomUUID()}.jpg"
        supabase.storage.from(BUCKET).upload(path, bytes)
        return supabase.storage.from(BUCKET).publicUrl(path)
    }

    private suspend fun deleteImage(url: String?) {
        val path = url?.substringAfter("/$BUCKET/", "") ?: return
        if (path.isNotBlank()) {
            runCatching { supabase.storage.from(BUCKET).delete(listOf(path)) }
        }
    }

    // ---------- ITEMS: CREATE ----------
    suspend fun createItem(
        name: String, description: String, category: String,
        location: String, type: String, imageBytes: ByteArray?
    ) {
        val url = imageBytes?.let { uploadImage(it) }
        supabase.from("items").insert(
            NewItem(userId(), name, description, category, location, type, url)
        )
    }

    // ---------- ITEMS: READ ----------
    suspend fun getItems(query: String, type: String?, category: String?): List<Item> =
        supabase.from("items").select {
            filter {
                if (query.isNotBlank()) {
                    or {
                        ilike("name", "%$query%")
                        ilike("description", "%$query%")
                        ilike("location", "%$query%")
                    }
                }
                if (type != null) eq("type", type)
                if (category != null) eq("category", category)
            }
            order("created_at", Order.DESCENDING)
        }.decodeList<Item>()

    suspend fun getMyItems(): List<Item> =
        supabase.from("items").select {
            filter { eq("user_id", userId()) }
            order("created_at", Order.DESCENDING)
        }.decodeList<Item>()

    // ---------- ITEMS: UPDATE ----------
    suspend fun updateItem(
        id: String, name: String, description: String, category: String,
        location: String, type: String, newImageBytes: ByteArray?
    ) {
        val url = newImageBytes?.let { uploadImage(it) }
        supabase.from("items").update({
            set("name", name)
            set("description", description)
            set("category", category)
            set("location", location)
            set("type", type)
            if (url != null) set("image_url", url)
        }) {
            filter { eq("id", id) }
        }
    }

    suspend fun markClaimed(id: String) {
        supabase.from("items").update({ set("status", "CLAIMED") }) {
            filter { eq("id", id) }
        }
    }

    // ---------- ITEMS: DELETE ----------
    suspend fun deleteItem(item: Item) {
        supabase.from("items").delete { filter { eq("id", item.id) } }
        deleteImage(item.imageUrl)
    }

    // ---------- COMMENTS ----------
    suspend fun getComments(itemId: String): List<Comment> =
        supabase.from("comments").select {
            filter { eq("item_id", itemId) }
            order("created_at", Order.ASCENDING)
        }.decodeList<Comment>()

    suspend fun addComment(itemId: String, text: String, isClaim: Boolean) {
        supabase.from("comments").insert(
            NewComment(itemId, userId(), userName(), text, isClaim)
        )
    }

    suspend fun deleteComment(id: String) {
        supabase.from("comments").delete { filter { eq("id", id) } }
    }

    // ---------- REALTIME: hear about new posts ----------
    suspend fun listenForNewItems(onNew: (Item) -> Unit) {
        val channel = supabase.channel("public-items")
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "items"
        }
        channel.subscribe()
        inserts.collect { onNew(it.decodeRecord<Item>()) }
    }
}