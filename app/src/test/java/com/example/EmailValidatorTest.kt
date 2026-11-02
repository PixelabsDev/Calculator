package com.example

import com.example.report.EmailValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidatorTest {

    @Test
    fun testValidEmailFormats() {
        val validEmails = listOf(
            "user@domain.com",
            "developer@org.in",
            "contact@custom.xyz",
            "first.last+tag@sub.domain.co.uk",
            "support-123_abc@app.io",
            "test_user@university.edu",
            "founder@pixelabs.dev",
            "user1234@email.technology"
        )

        for (email in validEmails) {
            assertTrue("Expected '$email' to be valid", EmailValidator.isValid(email))
        }
    }

    @Test
    fun testInvalidEmailFormats() {
        val invalidEmails = listOf(
            "plain text",
            "user@",
            "@domain.com",
            "user@domain",
            "user@.com",
            "user@domain.",
            "user@domain.c",
            "user @domain.com",
            "user@ domain.com",
            "user@@domain.com",
            "user@domain..com",
            "missing-at-sign.com",
            "user@sub_domain.com"
        )

        for (email in invalidEmails) {
            assertFalse("Expected '$email' to be invalid", EmailValidator.isValid(email))
        }
    }

    @Test
    fun testOptionalBlankEmail() {
        // When allowBlank = true (default for optional fields)
        assertTrue(EmailValidator.isValid("", allowBlank = true))
        assertTrue(EmailValidator.isValid("   ", allowBlank = true))
        assertTrue(EmailValidator.isValid(null, allowBlank = true))

        // When allowBlank = false (for required fields)
        assertFalse(EmailValidator.isValid("", allowBlank = false))
        assertFalse(EmailValidator.isValid("   ", allowBlank = false))
        assertFalse(EmailValidator.isValid(null, allowBlank = false))
    }
}
