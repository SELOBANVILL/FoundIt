package com.example.foundit

import com.example.foundit.security.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    @Test fun email_blank_isRequired() =
        assertEquals("Email is required", Validators.email("   "))

    @Test fun email_malformed_isRejected() {
        assertNotNull(Validators.email("a@b"))
        assertNotNull(Validators.email("not-an-email"))
        assertNotNull(Validators.email("a b@school.edu"))
    }

    @Test fun email_valid_passes() =
        assertNull(Validators.email("juan.delacruz@school.edu.ph"))

    @Test fun email_tooLong_isRejected() =
        assertNotNull(Validators.email("a".repeat(250) + "@x.com"))

    @Test fun password_rules() {
        assertEquals("Password is required", Validators.password(""))
        assertNotNull(Validators.password("abc123"))      // too short
        assertNotNull(Validators.password("abcdefgh"))    // no number
        assertNotNull(Validators.password("12345678"))    // no letter
        assertNull(Validators.password("abc12345"))
    }

    @Test fun confirmPassword_mismatch() {
        assertNotNull(Validators.confirmPassword("abc12345", "abc12346"))
        assertNull(Validators.confirmPassword("abc12345", "abc12345"))
    }

    @Test fun otp_mustBeExactlySixDigits() {
        assertNotNull(Validators.otp(""))
        assertNotNull(Validators.otp("12345"))
        assertNotNull(Validators.otp("12 345"))
        assertNotNull(Validators.otp("12a456"))
        assertNotNull(Validators.otp("1234567"))
        assertNull(Validators.otp("123456"))
    }

    @Test fun itemName_lengths() {
        assertEquals("Item name is required", Validators.itemName(" "))
        assertNotNull(Validators.itemName("a"))
        assertNotNull(Validators.itemName("x".repeat(61)))
        assertNull(Validators.itemName("Blue umbrella"))
    }

    @Test fun location_lengths() {
        assertNotNull(Validators.location(""))
        assertNotNull(Validators.location("x".repeat(81)))
        assertNull(Validators.location("Library 2F"))
    }

    @Test fun description_optionalButCapped() {
        assertNull(Validators.description(""))
        assertNotNull(Validators.description("x".repeat(501)))
    }

    @Test fun comment_lengths() {
        assertNotNull(Validators.comment("  "))
        assertNotNull(Validators.comment("x".repeat(301)))
        assertNull(Validators.comment("I saw it near the gym"))
    }

    @Test fun type_and_category_mustBeKnownValues() {
        assertNotNull(Validators.type("STOLEN"))
        assertNull(Validators.type("LOST"))
        assertNotNull(Validators.category(""))
        assertNull(Validators.category("Keys"))
    }

    @Test
    fun ciitEmail_requiresSchoolDomain() {
        assertNull(Validators.ciitEmail("juan.delacruz@ciit.edu.ph"))
        assertNull(Validators.ciitEmail("  Juan@CIIT.edu.ph "))
        assertNotNull(Validators.ciitEmail(""))
        assertNotNull(Validators.ciitEmail("juan@gmail.com"))
        assertNotNull(Validators.ciitEmail("juan@ciit.edu.ph.evil.com"))
    }

    @Test
    fun contactInfo_isOptionalButLimited() {
        assertNull(Validators.contactInfo(""))
        assertNull(Validators.contactInfo("FB: Juan Dela Cruz"))
        assertNotNull(Validators.contactInfo("x".repeat(Validators.CONTACT_INFO_MAX + 1)))
    }
}
