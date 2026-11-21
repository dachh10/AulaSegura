package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.repository.ParentLinkQr

/** Result of reading a tutor's QR, before any student document is written. */
data class ScanStudentUi(
    val found: ParentLinkQr? = null,
    val message: String? = null,
) {
    companion object {
        fun from(raw: String): ScanStudentUi {
            val qr = ParentLinkQr.decode(raw)
            return if (qr == null) {
                ScanStudentUi(message = "QR no válido")
            } else {
                ScanStudentUi(found = qr)
            }
        }
    }
}
