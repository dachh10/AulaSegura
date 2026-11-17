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

    /**
     * Uploads a document without re-encoding it.
     *
     * Used by the task PDF: Cloudinary serves it from `/raw/upload` and applies no
     * transformation, so the family downloads the file the teacher attached.
     *
     * Requires the account to allow PDF delivery (Cloudinary → Security); without
     * that toggle the upload succeeds and the URL answers 401.
     */
    suspend fun uploadDocument(
        bytes: ByteArray,
        fileName: String,
        folder: CloudinaryFolder,
        scopeId: String,
        mimeType: String,
    ): CloudinaryResult
}
