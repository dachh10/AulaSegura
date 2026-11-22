package com.example.aulasegura.data.location

import android.content.Context
import android.location.Geocoder
import com.example.aulasegura.domain.repository.SchoolNameResolver
import com.example.aulasegura.domain.repository.resolvedSchoolName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Platform geocoder: no API key, best effort, never blocks the save button. */
class AndroidSchoolNameResolver(
    private val context: Context,
) : SchoolNameResolver {

    override suspend fun resolve(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(context, Locale.forLanguageTag("es-MX"))
                @Suppress("DEPRECATION")
                val address = geocoder.getFromLocation(latitude, longitude, 1)
                    ?.firstOrNull() ?: return@runCatching null
                resolvedSchoolName(
                    locality = address.locality,
                    subAdminArea = address.subAdminArea,
                    adminArea = address.adminArea,
                    featureName = address.featureName,
                )
            }.getOrNull()
        }
}
