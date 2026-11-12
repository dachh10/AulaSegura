package com.example.aulasegura.data.remote.cloudinary

/**
 * Builds the derived URLs Cloudinary serves from a stored original.
 *
 * The transformation goes between `/upload/` and the version segment:
 * `.../upload/w_96,h_96,c_fill/v1712/abc.jpg`. Doing it here, as a pure string
 * function, keeps the rule testable without a network call.
 */
object CloudinaryTransformations {

    private const val UPLOAD_MARKER = "/upload/"

    /**
     * Inserts [transformation] into [secureUrl].
     *
     * Returns the URL untouched when it is not a Cloudinary upload URL, or when
     * it already carries a transformation. That second guard matters: applying
     * the function twice would otherwise nest two transformations and Cloudinary
     * would honour the inner one silently.
     */
    fun apply(secureUrl: String, transformation: String): String {
        val markerAt = secureUrl.indexOf(UPLOAD_MARKER)
        if (markerAt < 0 || transformation.isBlank()) return secureUrl

        val insertAt = markerAt + UPLOAD_MARKER.length
        val rest = secureUrl.substring(insertAt)
        val firstSegment = rest.substringBefore('/')
        if (firstSegment.contains('_')) return secureUrl

        return secureUrl.substring(0, insertAt) + transformation + "/" + rest
    }
}
