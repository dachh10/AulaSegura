package com.example.aulasegura.data.remote.cloudinary

import com.example.aulasegura.domain.model.MediaMetadata
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Unsigned upload through the plain HTTP API.
 *
 * No Cloudinary SDK: the upload is one multipart POST, and pulling in a library
 * for that would add a dependency plus its own networking stack on top of the
 * OkHttp the app already uses.
 *
 * The request carries exactly three parts — `file`, `upload_preset` and
 * `asset_folder`. No `api_key`, no signature: unsigned uploads are allowed only
 * this restricted parameter set, and the preset is what pins formats and paths.
 */
class OkHttpCloudinaryUploader(
    private val client: OkHttpClient,
    private val config: CloudinaryConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CloudinaryUploader {

    override suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        folder: CloudinaryFolder,
        scopeId: String,
    ): CloudinaryResult = withContext(ioDispatcher) {
        if (!config.isConfigured) return@withContext CloudinaryResult.NotConfigured
        if (bytes.isEmpty()) return@withContext CloudinaryResult.Rejected(0, "Archivo vacío")

        val assetFolder = folder.path(scopeId)

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", config.uploadPreset)
            .addFormDataPart("asset_folder", assetFolder)
            .addFormDataPart("file", fileName, bytes.toRequestBody(JPEG.toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url(config.imageUploadUrl)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    toSuccess(payload, folder, assetFolder)
                } else if (response.code >= 500) {
                    CloudinaryResult.ServerError(response.code)
                } else {
                    CloudinaryResult.Rejected(response.code, errorMessage(payload))
                }
            }
        } catch (e: IOException) {
            CloudinaryResult.NetworkError
        }
    }

    private fun toSuccess(
        payload: String,
        folder: CloudinaryFolder,
        assetFolder: String,
    ): CloudinaryResult = try {
        val dto = json.decodeFromString<CloudinaryUploadDto>(payload)
        CloudinaryResult.Success(
            MediaMetadata(
                publicId = dto.publicId,
                url = dto.secureUrl,
                thumbnailUrl = CloudinaryTransformations.apply(
                    dto.secureUrl,
                    folder.thumbnailTransformation,
                ),
                // The response echoes the folder; fall back to what we asked for
                // when the account answers without it.
                assetFolder = dto.assetFolder ?: assetFolder,
                type = dto.resourceType,
                format = dto.format,
                bytes = dto.bytes,
                width = dto.width,
                height = dto.height,
                durationSec = dto.duration,
            ),
        )
    } catch (e: Exception) {
        // A 200 we cannot read is still a failed upload: the caller must retry.
        CloudinaryResult.ServerError(200)
    }

    private fun errorMessage(payload: String): String? = try {
        json.decodeFromString<CloudinaryErrorDto>(payload).error?.message
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val JPEG = "image/jpeg"
    }
}
