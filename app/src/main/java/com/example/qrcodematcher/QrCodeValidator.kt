package com.example.qrcodematcher

internal object QrCodeValidator {
    fun isAccepted(value: String): Boolean =
        value.startsWith("KKNI") && (value.length == 19 || value.length == 20)
}
