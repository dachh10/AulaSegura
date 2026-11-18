package com.example.aulasegura.data.mapper

/**
 * The editable fields of `users/{uid}`.
 *
 * Kept to an `update()` of exactly these two keys: the rules blacklist the
 * identity fields, but a blacklist is not a whitelist, and never sending the
 * rest of the document is the only way this client cannot overwrite a change
 * made by the director at the same time.
 */
internal fun profileUpdate(
    fullName: String,
    subjects: List<String>,
): Map<String, Any> = mapOf(
    FIELD_FULL_NAME to fullName,
    FIELD_SUBJECTS to subjects,
)

private const val FIELD_FULL_NAME = "fullName"
private const val FIELD_SUBJECTS = "subjects"
