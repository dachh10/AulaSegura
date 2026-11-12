package com.example.aulasegura.data.remote.cloudinary

/**
 * Where an asset belongs inside the Cloudinary account.
 *
 * One folder per resource type, never everything mixed (CONTEXT §7.1). The
 * account uses dynamic folders, so the folder travels in the `asset_folder`
 * parameter of the upload request rather than being baked into the preset.
 *
 * [thumbnailTransformation] lives here because the right crop depends on the
 * content: a face crop for avatars, a plain width limit for evidence photos that
 * have no face to track.
 */
enum class CloudinaryFolder(
    private val segment: String,
    val thumbnailTransformation: String,
) {
    PROFILES("profiles", "w_96,h_96,c_fill,g_face,q_auto,f_auto"),
    INCIDENTS("incidents", "w_400,c_limit,q_auto,f_auto"),
    JUSTIFICATIONS("justifications", "w_400,c_limit,q_auto,f_auto"),
    TASKS("tasks", "w_400,c_limit,q_auto,f_auto"),
    NOTICES("notices", "w_400,c_limit,q_auto,f_auto"),
    SCHOOLS("schools", "w_96,h_96,c_fill,q_auto,f_auto"),
    ;

    /**
     * `aulasegura/profiles/<scopeId>`, with the scope sanitised.
     *
     * [scopeId] comes from the app (a uid, an incident id), but it is still
     * filtered: Cloudinary treats a folder as a path, so a stray `/` or `..`
     * would escape the intended prefix.
     */
    fun path(scopeId: String): String {
        val safe = scopeId
            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .ifBlank { "sin-id" }
        return "$ROOT/$segment/$safe"
    }

    companion object {
        const val ROOT: String = "aulasegura"
    }
}
