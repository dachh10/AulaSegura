package com.example.aulasegura.data.mapper

/**
 * The two fields of `users/{uid}` an avatar changes.
 *
 * A `update()` rather than a `set()` is deliberate: the profile holds role,
 * school and active flags written by the director's flows, and replacing the whole
 * document from the teacher's device would race with them. The rules already
 * filter the forbidden keys, but sending only these two is the safer contract.
 */
internal fun profilePhotoUpdate(
    photoUrl: String,
    photoPublicId: String,
): Map<String, Any> = mapOf(
    FIELD_PHOTO_URL to photoUrl,
    FIELD_PHOTO_PUBLIC_ID to photoPublicId,
)

private const val FIELD_PHOTO_URL = "photoUrl"
private const val FIELD_PHOTO_PUBLIC_ID = "photoPublicId"
