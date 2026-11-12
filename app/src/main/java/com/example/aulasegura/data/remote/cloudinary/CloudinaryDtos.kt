package com.example.aulasegura.data.remote.cloudinary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subset of the Cloudinary upload response that the app actually stores.
 *
 * Unknown keys are ignored on purpose: Cloudinary returns dozens of fields and
 * a new one must never break an upload.
 */
@Serializable
internal data class CloudinaryUploadDto(
    @SerialName("public_id") val publicId: String,
    @SerialName("secure_url") val secureUrl: String,
    @SerialName("asset_folder") val assetFolder: String? = null,
    @SerialName("resource_type") val resourceType: String = "image",
    @SerialName("format") val format: String = "",
    @SerialName("bytes") val bytes: Long = 0,
    @SerialName("width") val width: Int = 0,
    @SerialName("height") val height: Int = 0,
    @SerialName("duration") val duration: Double? = null,
)

/** Cloudinary answers failures with `{"error":{"message":"..."}}`. */
@Serializable
internal data class CloudinaryErrorDto(
    val error: CloudinaryErrorBody? = null,
)

@Serializable
internal data class CloudinaryErrorBody(
    val message: String? = null,
    @SerialName("http_code") val httpCode: Int? = null,
)
