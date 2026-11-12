package com.example.aulasegura.data.remote.cloudinary

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The uploader is where a wrong field name becomes a silent 400 in production,
 * so it gets a real HTTP server instead of a mock: the JDK's own [HttpServer]
 * records the request exactly as OkHttp sent it, with no extra test dependency.
 */
class OkHttpCloudinaryUploaderTest {

    private lateinit var server: HttpServer
    private lateinit var uploader: OkHttpCloudinaryUploader
    private val requests = mutableListOf<CapturedRequest>()
    private var responseCode = 200
    private var responseBody = ""

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val body = exchange.requestBody.use { it.readBytes() }.toString(Charsets.UTF_8)
            requests += CapturedRequest(
                method = exchange.requestMethod,
                path = exchange.requestURI.path,
                contentType = exchange.requestHeaders.getFirst("Content-Type"),
                body = body,
            )
            val payload = responseBody.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(responseCode, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        server.start()

        uploader = OkHttpCloudinaryUploader(
            client = OkHttpClient(),
            config = CloudinaryConfig(
                cloudName = "test-cloud",
                uploadPreset = "docenteaula",
                apiBaseUrl = "http://127.0.0.1:${server.address.port}",
            ),
        )
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun `posts the multipart fields the unsigned preset expects`() = runBlocking {
        respondWith(SUCCESS_BODY, code = 200)

        uploader.uploadImage(JPEG_BYTES, "foto.jpg", CloudinaryFolder.PROFILES, "uid-1")

        val request = requests.single()
        assertEquals("POST", request.method)
        assertEquals("/v1_1/test-cloud/image/upload", request.path)
        assertTrue(request.body.contains("name=\"upload_preset\""))
        assertTrue(request.body.contains("docenteaula"))
        assertTrue(request.body.contains("name=\"asset_folder\""))
        assertTrue(request.body.contains("aulasegura/profiles/uid-1"))
        assertTrue(request.body.contains("name=\"file\""))
        assertTrue(request.body.contains("filename=\"foto.jpg\""))
    }

    @Test
    fun `never sends credentials`() = runBlocking {
        respondWith(SUCCESS_BODY, code = 200)

        uploader.uploadImage(JPEG_BYTES, "foto.jpg", CloudinaryFolder.PROFILES, "uid-1")

        val body = requests.single().body
        assertFalse(body.contains("api_key"))
        assertFalse(body.contains("api_secret"))
        assertFalse(body.contains("signature"))
    }

    @Test
    fun `maps a successful response into metadata with a thumbnail url`() = runBlocking {
        respondWith(SUCCESS_BODY, code = 200)

        val result = uploader.uploadImage(
            JPEG_BYTES,
            "foto.jpg",
            CloudinaryFolder.PROFILES,
            "uid-1",
        )

        val asset = (result as CloudinaryResult.Success).asset
        assertEquals("aulasegura/profiles/uid-1/abc123", asset.publicId)
        assertEquals(SECURE_URL, asset.url)
        assertEquals("aulasegura/profiles/uid-1", asset.assetFolder)
        assertEquals("image", asset.type)
        assertEquals("jpg", asset.format)
        assertEquals(48_213L, asset.bytes)
        assertEquals(720, asset.width)
        assertEquals(1080, asset.height)
        assertNull(asset.durationSec)
        assertTrue(asset.thumbnailUrl.startsWith("https://res.cloudinary.com/demo/image/upload/w_96,h_96,c_fill,g_face,q_auto,f_auto/v1712/aulasegura/profiles/uid-1/abc123.jpg"))
    }

    @Test
    fun `a rejected upload reports the cloudinary message`() = runBlocking {
        respondWith(
            """{"error":{"message":"Upload preset not found","http_code":400}}""",
            code = 400,
        )

        val result = uploader.uploadImage(JPEG_BYTES, "foto.jpg", CloudinaryFolder.PROFILES, "uid-1")

        val rejected = result as CloudinaryResult.Rejected
        assertEquals(400, rejected.httpCode)
        assertEquals("Upload preset not found", rejected.message)
    }

    @Test
    fun `a server failure is separable from a rejected request`() = runBlocking {
        respondWith("upstream down", code = 503)

        val result = uploader.uploadImage(JPEG_BYTES, "foto.jpg", CloudinaryFolder.PROFILES, "uid-1")

        assertEquals(CloudinaryResult.ServerError(503), result)
    }

    @Test
    fun `an unreachable server is a network error, not a crash`() = runBlocking {
        server.stop(0)

        val result = uploader.uploadImage(JPEG_BYTES, "foto.jpg", CloudinaryFolder.PROFILES, "uid-1")

        assertEquals(CloudinaryResult.NetworkError, result)
    }

    @Test
    fun `a build without credentials never sends a request`() = runBlocking {
        val unconfigured = OkHttpCloudinaryUploader(
            client = OkHttpClient(),
            config = CloudinaryConfig(
                cloudName = "",
                uploadPreset = "",
                apiBaseUrl = "http://127.0.0.1:${server.address.port}",
            ),
        )

        val result = unconfigured.uploadImage(JPEG_BYTES, "foto.jpg", CloudinaryFolder.PROFILES, "uid-1")

        assertEquals(CloudinaryResult.NotConfigured, result)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `an empty file is rejected before hitting the network`() = runBlocking {
        val result = uploader.uploadImage(ByteArray(0), "vacio.jpg", CloudinaryFolder.PROFILES, "uid-1")

        assertTrue(result is CloudinaryResult.Rejected)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `a 200 with an unreadable body is still a failure`() = runBlocking {
        respondWith("<html>proxy</html>", code = 200)

        val result = uploader.uploadImage(JPEG_BYTES, "foto.jpg", CloudinaryFolder.PROFILES, "uid-1")

        assertTrue(result is CloudinaryResult.ServerError)
    }

    private fun respondWith(body: String, code: Int) {
        responseBody = body
        responseCode = code
    }

    private data class CapturedRequest(
        val method: String,
        val path: String,
        val contentType: String?,
        val body: String,
    )

    private companion object {
        val JPEG_BYTES = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x01, 0x02)

        const val SECURE_URL =
            "https://res.cloudinary.com/demo/image/upload/v1712/aulasegura/profiles/uid-1/abc123.jpg"

        const val SUCCESS_BODY = """
            {
              "public_id": "aulasegura/profiles/uid-1/abc123",
              "secure_url": "$SECURE_URL",
              "asset_folder": "aulasegura/profiles/uid-1",
              "resource_type": "image",
              "format": "jpg",
              "bytes": 48213,
              "width": 720,
              "height": 1080,
              "created_at": "2026-11-11T21:10:00Z"
            }
        """
    }
}
