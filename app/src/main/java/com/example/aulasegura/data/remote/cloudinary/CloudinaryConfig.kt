package com.example.aulasegura.data.remote.cloudinary

/**
 * Everything needed to talk to one Cloudinary account.
 *
 * Both values ship inside the APK by design: an unsigned preset exists so the
 * client can upload without a signature. The API secret must never appear here
 * (CONTEXT §7.2); if a flow ever needs signed uploads, that decision changes the
 * architecture, not this class.
 */
data class CloudinaryConfig(
    val cloudName: String,
    val uploadPreset: String,
    val apiBaseUrl: String = DEFAULT_API_BASE_URL,
) {

    val isConfigured: Boolean
        get() = cloudName.isNotBlank() && uploadPreset.isNotBlank()

    /** Unsigned endpoint for images. Videos would use `/video/upload`. */
    val imageUploadUrl: String
        get() = "$apiBaseUrl/v1_1/$cloudName/image/upload"

    companion object {
        /** Overridable so tests can point the uploader at a local stub. */
        const val DEFAULT_API_BASE_URL: String = "https://api.cloudinary.com"
    }
}
