package com.example.foundit.security

import com.example.foundit.data.CATEGORIES
import com.example.foundit.data.CIIT_EMAIL_DOMAIN
import com.example.foundit.data.ItemType

/**
 * Client-side input validation. Each function returns an error message to show
 * under the field, or null when the value is valid.
 *
 * The same limits are enforced again in the database (CHECK constraints in
 * supabase/schema.sql), because client checks alone can be bypassed.
 */
object Validators {
    const val NAME_MIN = 2
    const val NAME_MAX = 60
    const val LOCATION_MIN = 2
    const val LOCATION_MAX = 80
    const val DESCRIPTION_MAX = 500
    const val COMMENT_MAX = 300
    const val PASSWORD_MIN = 8
    const val EMAIL_MAX = 254
    const val CONTACT_INFO_MAX = 150

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    private val OTP_REGEX = Regex("^\\d{6}$")

    fun email(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Email is required"
            v.length > EMAIL_MAX -> "Email is too long"
            !EMAIL_REGEX.matches(v) -> "Enter a valid email address"
            else -> null
        }
    }

    fun password(value: String): String? = when {
        value.isEmpty() -> "Password is required"
        value.length < PASSWORD_MIN -> "Password must be at least $PASSWORD_MIN characters"
        value.length > 72 -> "Password is too long"
        value.none { it.isLetter() } -> "Password must contain a letter"
        value.none { it.isDigit() } -> "Password must contain a number"
        else -> null
    }

    /** Login only checks presence; strength rules apply at sign-up. */
    fun loginPassword(value: String): String? =
        if (value.isEmpty()) "Password is required" else null

    fun confirmPassword(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Please confirm your password"
        password != confirm -> "Passwords do not match"
        else -> null
    }

    fun otp(value: String): String? =
        if (OTP_REGEX.matches(value)) null else "Enter the 6-digit code from Google Authenticator"

    fun itemName(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Item name is required"
            v.length < NAME_MIN -> "Item name is too short"
            v.length > NAME_MAX -> "Item name must be at most $NAME_MAX characters"
            else -> null
        }
    }

    fun location(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Location is required"
            v.length < LOCATION_MIN -> "Location is too short"
            v.length > LOCATION_MAX -> "Location must be at most $LOCATION_MAX characters"
            else -> null
        }
    }

    /** Finder's contact email: required, and must be a CIIT school address. */
    fun ciitEmail(value: String): String? {
        val v = value.trim()
        return email(v) ?: if (v.lowercase().endsWith("@$CIIT_EMAIL_DOMAIN")) null
        else "Use your CIIT email (@$CIIT_EMAIL_DOMAIN)"
    }

    fun contactInfo(value: String): String? =
        if (value.trim().length > CONTACT_INFO_MAX) "Contact info must be at most $CONTACT_INFO_MAX characters" else null

    fun description(value: String): String? =
        if (value.trim().length > DESCRIPTION_MAX) "Description must be at most $DESCRIPTION_MAX characters" else null

    fun comment(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Comment cannot be empty"
            v.length > COMMENT_MAX -> "Comment must be at most $COMMENT_MAX characters"
            else -> null
        }
    }

    fun type(value: String): String? =
        if (value in ItemType.ALL) null else "Choose Lost or Found"

    fun category(value: String): String? =
        if (value in CATEGORIES) null else "Choose a category"
}
