package com.example.foundit.data

import com.example.foundit.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.mfa.AuthenticatorAssuranceLevel
import io.github.jan.supabase.auth.mfa.FactorType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import java.util.UUID

/** Where the signed-in user is in the login + 2FA flow. */
enum class AuthStage { LOGGED_OUT, NEEDS_ENROLL, NEEDS_VERIFY, READY }

/** Data needed to show the Google Authenticator setup QR code. */
data class TotpEnrollment(val factorId: String, val otpauthUri: String, val secret: String)

/**
 * All network access goes through Supabase over HTTPS. Postgrest builds parameterized
 * queries (values are never concatenated into SQL), and every request is checked by
 * Row Level Security on the server.
 */
object Repository {
    private const val BUCKET = "item-images"
    private const val ISSUER = "FoundIt"

    // ---------- AUTH (factor 1: email + password) ----------

    /** Returns true when a session was created (false if email confirmation is required). */
    suspend fun signUp(email: String, password: String): Boolean {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return supabase.auth.currentSessionOrNull() != null
    }

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Revokes the session on the server and wipes the encrypted local copy. */
    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } finally {
            supabase.auth.clearSession()
        }
    }

    fun userId(): String = supabase.auth.currentUserOrNull()?.id ?: error("Not logged in")

    fun currentUserIdOrNull(): String? = supabase.auth.currentUserOrNull()?.id

    fun email(): String = supabase.auth.currentUserOrNull()?.email ?: ""

    // ---------- AUTH (factor 2: Google Authenticator TOTP) ----------

    suspend fun authStage(): AuthStage {
        supabase.auth.awaitInitialization()
        if (supabase.auth.currentSessionOrNull() == null) return AuthStage.LOGGED_OUT
        // Refresh the user so the list of verified factors is current.
        val factors = supabase.auth.mfa.retrieveFactorsForCurrentUser()
        val hasVerifiedTotp = factors.any { it.factorType == "totp" && it.isVerified }
        return when {
            !hasVerifiedTotp -> AuthStage.NEEDS_ENROLL
            supabase.auth.mfa.getAuthenticatorAssuranceLevel().current == AuthenticatorAssuranceLevel.AAL2 -> AuthStage.READY
            else -> AuthStage.NEEDS_VERIFY
        }
    }

    /** Creates a new TOTP secret for this user and returns the otpauth:// URI for the QR code. */
    suspend fun startTotpEnrollment(): TotpEnrollment {
        // Remove abandoned, never-verified setups so a fresh QR can be issued.
        supabase.auth.mfa.retrieveFactorsForCurrentUser()
            .filter { it.factorType == "totp" && !it.isVerified }
            .forEach { supabase.auth.mfa.unenroll(it.id) }

        val factor = supabase.auth.mfa.enroll(FactorType.TOTP, friendlyName = "$ISSUER ${UUID.randomUUID().toString().take(6)}") {
            issuer = ISSUER
        }
        return TotpEnrollment(factor.id, factor.data.uri, factor.data.secret)
    }

    /** Verifies the first code during setup; on success the session is upgraded to AAL2. */
    suspend fun confirmTotpEnrollment(factorId: String, code: String) {
        supabase.auth.mfa.createChallengeAndVerify(factorId, code)
    }

    /** Verifies a code at login using the user's verified TOTP factor. */
    suspend fun verifyTotpLogin(code: String) {
        val factor = supabase.auth.mfa.retrieveFactorsForCurrentUser()
            .firstOrNull { it.factorType == "totp" && it.isVerified }
            ?: error("No authenticator is set up for this account")
        supabase.auth.mfa.createChallengeAndVerify(factor.id, code)
    }

    // ---------- PHOTO ----------
    private suspend fun uploadImage(bytes: ByteArray): String {
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
        try {
            supabase.from("items").insert(
                NewItem(userId(), name, description, category, location, type, url)
            )
        } catch (e: Exception) {
            deleteImage(url) // don't leave an orphaned photo behind
            throw e
        }
    }

    /** True if this user already has an OPEN post with the same name, location and type. */
    suspend fun hasOpenDuplicate(name: String, location: String, type: String, excludeId: String?): Boolean =
        supabase.from("items").select {
            filter {
                eq("user_id", userId())
                eq("status", ItemStatus.OPEN)
                eq("type", type)
                ilike("name", name.escapeLike())
                ilike("location", location.escapeLike())
                if (excludeId != null) neq("id", excludeId)
            }
            limit(1)
        }.decodeList<Item>().isNotEmpty()

    // ---------- ITEMS: READ ----------
    suspend fun getItems(query: String, type: String?, category: String?): List<Item> =
        supabase.from("items").select {
            filter {
                val q = query.trim().take(60)
                if (q.isNotBlank()) {
                    val pattern = "%${q.escapeLike()}%"
                    or {
                        ilike("name", pattern)
                        ilike("description", pattern)
                        ilike("location", pattern)
                    }
                }
                if (type != null) eq("type", type)
                if (category != null) eq("category", category)
            }
            order("created_at", Order.DESCENDING)
        }.decodeList<Item>()

    suspend fun getItem(id: String): Item? =
        supabase.from("items").select { filter { eq("id", id) } }.decodeSingleOrNull<Item>()

    suspend fun getMyItems(): List<Item> =
        supabase.from("items").select {
            filter { eq("user_id", userId()) }
            order("created_at", Order.DESCENDING)
        }.decodeList<Item>()

    // ---------- ITEMS: UPDATE ----------
    suspend fun updateItem(
        id: String, name: String, description: String, category: String,
        location: String, type: String, newImageBytes: ByteArray?, oldImageUrl: String?
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
        if (url != null) deleteImage(oldImageUrl)
    }

    suspend fun markClaimed(id: String) {
        supabase.from("items").update({ set("status", ItemStatus.CLAIMED) }) {
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
        supabase.from("comments").insert(NewComment(itemId, userId(), text, isClaim))
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

    suspend fun stopListening() {
        runCatching { supabase.realtime.removeAllChannels() }
    }

    /** Escapes LIKE wildcards so user text is matched literally. */
    private fun String.escapeLike(): String =
        replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
