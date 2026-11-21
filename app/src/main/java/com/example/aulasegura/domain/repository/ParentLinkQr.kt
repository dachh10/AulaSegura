package com.example.aulasegura.domain.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The payload carried by the tutor's QR. It is deliberately versioned and tiny:
 * the director validates it before writing anything to Firestore (T3), and a
 * code from another app or an older version must be ignored, not interpreted.
 *
 * Property order defines the JSON order: `v`, `uid`, `alumno`.
 */
@Serializable
data class ParentLinkQr(
    val v: Int,
    val uid: String,
    val alumno: String,
) {
    companion object {
        private const val VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        fun encode(uid: String, alumno: String): String = json.encodeToString(
            serializer(),
            ParentLinkQr(v = VERSION, uid = uid.trim(), alumno = alumno.trim()),
        )

        fun decode(raw: String): ParentLinkQr? = runCatching {
            json.decodeFromString(serializer(), raw)
        }.getOrNull()?.takeIf {
            it.v == VERSION && it.uid.isNotBlank() && it.alumno.isNotBlank()
        }
    }
}
