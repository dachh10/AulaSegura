package com.example.aulasegura.data.remote.cloudinary

/**
 * Uploads a file to Cloudinary and returns its metadata.
 *
 * Kept as an interface so the sync layer and the tests can decide what "upload"
 * means: the real implementation talks HTTP, a fake one returns a fixed asset.
 */
interface CloudinaryUploader {

    /**
     * Uploads [bytes] into [folder] scoped to [scopeId] (a uid, an incident id).
     *
     * [fileName] is only a hint: Cloudinary decides the stored id, so two files
     * with the same name never collide.
     */
    suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        folder: CloudinaryFolder,
        scopeId: String,
    ): CloudinaryResult
}
