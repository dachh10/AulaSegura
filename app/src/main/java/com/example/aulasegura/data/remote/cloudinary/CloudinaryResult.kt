package com.example.aulasegura.data.remote.cloudinary

import com.example.aulasegura.domain.model.MediaMetadata

/**
 * Outcome of one upload attempt.
 *
 * Split by what the caller can actually do about it: retrying a [NetworkError]
 * is right, retrying a [Rejected] request is not (the preset or the file is
 * wrong), and [NotConfigured] means the build has no credentials at all.
 */
sealed interface CloudinaryResult {

    data class Success(val asset: MediaMetadata) : CloudinaryResult

    /** IO failure or timeout: the same request may work later. */
    data object NetworkError : CloudinaryResult

    /** Missing cloud name or preset, so no request was even sent. */
    data object NotConfigured : CloudinaryResult

    /** Cloudinary refused the request (bad preset, unsupported format, size). */
    data class Rejected(val httpCode: Int, val message: String?) : CloudinaryResult

    /** Cloudinary itself failed. Worth one retry, not a user-facing error. */
    data class ServerError(val httpCode: Int) : CloudinaryResult
}
