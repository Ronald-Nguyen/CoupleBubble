package com.aistudio.couplebubble.qxztrw

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.fetch.SourceResult
import coil.request.Options
import com.aistudio.couplebubble.qxztrw.ui.coil.Base64Fetcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Base64FetcherTest {

    private lateinit var context: Context
    private lateinit var imageLoader: ImageLoader
    private lateinit var options: Options
    private val factory = Base64Fetcher.Factory()

    // 1x1 transparent PNG in Base64
    private val samplePngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
    private val samplePngDataUri = "data:image/png;base64,$samplePngBase64"

    // Minimal JPEG header in Base64
    private val sampleJpegBase64 = "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
    private val sampleJpegDataUri = "data:image/jpeg;base64,$sampleJpegBase64"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        imageLoader = ImageLoader(context)
        options = Options(context)
    }

    @Test
    fun factory_identifiesDataUriJpeg() {
        val fetcher = factory.create(sampleJpegDataUri, options, imageLoader)
        assertNotNull(fetcher)
    }

    @Test
    fun factory_identifiesDataUriPng() {
        val fetcher = factory.create(samplePngDataUri, options, imageLoader)
        assertNotNull(fetcher)
    }

    @Test
    fun factory_identifiesRawBase64Jpeg() {
        val fetcher = factory.create(sampleJpegBase64, options, imageLoader)
        assertNotNull(fetcher)
    }

    @Test
    fun factory_identifiesRawBase64Png() {
        val fetcher = factory.create(samplePngBase64, options, imageLoader)
        assertNotNull(fetcher)
    }

    @Test
    fun factory_ignoresHttpAndFileUrls() {
        assertNull(factory.create("https://storage.googleapis.com/couplebubble/photo.jpg", options, imageLoader))
        assertNull(factory.create("file:///data/user/0/app/files/memories/photo.jpg", options, imageLoader))
        assertNull(factory.create("content://media/external/images/1", options, imageLoader))
    }

    @Test
    fun fetch_decodesDataUriSuccessfully() = runTest {
        val fetcher = factory.create(samplePngDataUri, options, imageLoader)
        assertNotNull(fetcher)

        val result = fetcher!!.fetch()
        assertTrue(result is SourceResult)
        val sourceResult = result as SourceResult
        assertEquals("image/png", sourceResult.mimeType)

        val bytes = sourceResult.source.source().readByteArray()
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun fetch_decodesRawBase64Successfully() = runTest {
        val fetcher = factory.create(sampleJpegBase64, options, imageLoader)
        assertNotNull(fetcher)

        val result = fetcher!!.fetch()
        assertTrue(result is SourceResult)
        val sourceResult = result as SourceResult
        assertEquals("image/jpeg", sourceResult.mimeType)

        val bytes = sourceResult.source.source().readByteArray()
        assertTrue(bytes.isNotEmpty())
    }
}
