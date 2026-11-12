package com.example.aulasegura.data.remote.cloudinary

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aulasegura.AulaSeguraApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.max
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The end-to-end proof of the Cloudinary block: compress a real bitmap, upload
 * it with the unsigned preset, and store the returned metadata in Firestore.
 *
 * This test talks to the real account on purpose — a mocked upload would prove
 * the code compiles, not that the preset, the folder and the rules agree. It
 * leaves the profile document exactly as it found it.
 */
@RunWith(AndroidJUnit4::class)
class CloudinaryUploadInstrumentedTest {

    private val app: AulaSeguraApp = ApplicationProvider.getApplicationContext()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var previousPhoto: Any? = null

    @Before
    fun signIn() = runBlocking {
        auth.signInWithEmailAndPassword(DOCENTE_EMAIL, DOCENTE_PASSWORD).await()
        previousPhoto = profileDocument().get().await().get(PHOTO_FIELD)
    }

    @After
    fun cleanUp() = runBlocking {
        try {
            val user = auth.currentUser ?: return@runBlocking
            val doc = firestore.collection("users").document(user.uid)
            if (previousPhoto == null) {
                doc.update(PHOTO_FIELD, FieldValue.delete()).await()
            } else {
                doc.update(PHOTO_FIELD, previousPhoto).await()
            }
        } finally {
            auth.signOut()
        }
    }

    @Test
    fun uploadsATestImageAndStoresItsMetadata() = runBlocking {
        val user = requireNotNull(auth.currentUser) { "not signed in" }
        val bitmap = Bitmap.createBitmap(2000, 1500, Bitmap.Config.ARGB_8888)
        val jpeg = app.container.imageCompressor.compress(bitmap)

        val result = app.container.cloudinaryUploader.uploadImage(
            bytes = jpeg,
            fileName = "aulasegura-test.jpg",
            folder = CloudinaryFolder.PROFILES,
            scopeId = TEST_SCOPE,
        )

        val asset = (result as? CloudinaryResult.Success)?.asset
        assertNotNull("upload failed: $result", asset)
        asset!!

        assertTrue(asset.url.startsWith("https://res.cloudinary.com/"))
        assertEquals("aulasegura/profiles/$TEST_SCOPE", asset.assetFolder)
        assertEquals("image", asset.type)
        assertTrue(asset.bytes > 0)
        assertTrue("metadata must describe the compressed file", max(asset.width, asset.height) <= 1080)
        assertTrue(asset.thumbnailUrl.contains("/upload/"))

        val doc = firestore.collection("users").document(user.uid)
        doc.update(PHOTO_FIELD, asset.toFirestoreMap()).await()

        val stored = doc.get().await().get(PHOTO_FIELD) as Map<*, *>
        assertEquals(asset.publicId, stored["publicId"])
        assertEquals(asset.url, stored["url"])
        assertEquals(asset.thumbnailUrl, stored["thumbnailUrl"])
        assertEquals(asset.assetFolder, stored["assetFolder"])
    }

    private fun profileDocument() = firestore
        .collection("users")
        .document(requireNotNull(auth.currentUser) { "not signed in" }.uid)

    private companion object {
        const val DOCENTE_EMAIL = "docente@aulasegura.com"
        const val DOCENTE_PASSWORD = "123456"

        /** H5 asks for a scratch folder so real profiles never mix with test uploads. */
        const val TEST_SCOPE = "test"
        const val PHOTO_FIELD = "photo"
    }
}
