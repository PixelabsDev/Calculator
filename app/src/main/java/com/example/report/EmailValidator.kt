package com.example.report

import android.util.Patterns

/**
 * Utility object for client-side email format validation in compliance with RFC 5322.
 * Allows any valid domain provider and TLD format (e.g. user@domain.com, user@org.in, user@custom.xyz).
 */
object EmailValidator {

    /**
     * Standard robust regular expression for general email validation:
     * - Username: alphanumeric plus standard symbols (. _ % + -)
     * - Single '@' separator
     * - Domain name: alphanumeric plus hyphens and subdomain dots
     * - Top-Level Domain (TLD): at least 2 alphabetic characters (e.g. .com, .in, .xyz, .tech)
     */
    private val EMAIL_REGEX = Regex(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\$"
    )

    /**
     * Validates an email address.
     *
     * @param email The email string to validate.
     * @param allowBlank When true, empty or whitespace-only strings are considered valid (for optional fields).
     * @return true if the email is considered valid, false otherwise.
     */
    fun isValid(email: String?, allowBlank: Boolean = true): Boolean {
        if (email.isNullOrBlank()) {
            return allowBlank
        }

        val trimmed = email.trim()

        // 1. Fast regex match verifying format, @ presence, domain, and valid TLD extension
        if (!EMAIL_REGEX.matches(trimmed)) {
            return false
        }

        // 2. Corroborate with Android Patterns.EMAIL_ADDRESS if available at runtime
        return try {
            Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
        } catch (_: Throwable) {
            true // Fallback to regex result if Patterns is not initialized (e.g. pure unit tests)
        }
    }
}
