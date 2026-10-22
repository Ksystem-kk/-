package com.example.qrcodematcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeValidatorTest {
    @Test
    fun acceptsValuesStartingWithKkniAtSupportedLengths() {
        assertTrue(QrCodeValidator.isAccepted("KKNI" + "1".repeat(15)))
        assertTrue(QrCodeValidator.isAccepted("KKNI" + "1".repeat(16)))
    }

    @Test
    fun rejectsWrongPrefixAndUnsupportedLengths() {
        assertFalse(QrCodeValidator.isAccepted("ABCD" + "1".repeat(15)))
        assertFalse(QrCodeValidator.isAccepted("KKNI" + "1".repeat(14)))
        assertFalse(QrCodeValidator.isAccepted("KKNI" + "1".repeat(17)))
    }
}
