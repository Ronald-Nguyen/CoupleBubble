package com.aistudio.couplebubble.qxztrw

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.fetch.DrawableResult
import coil.fetch.SourceResult
import coil.request.ImageRequest
import coil.request.Options
import coil.request.SuccessResult
import com.aistudio.couplebubble.qxztrw.ui.coil.Base64Fetcher
import com.aistudio.couplebubble.qxztrw.ui.coil.Base64Image
import com.aistudio.couplebubble.qxztrw.ui.coil.Base64Keyer
import com.aistudio.couplebubble.qxztrw.ui.coil.Base64Mapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class Base64FetcherTest {

    private lateinit var context: Context
    private lateinit var imageLoader: ImageLoader
    private lateinit var options: Options
    private val mapper = Base64Mapper()
    private val keyer = Base64Keyer()
    private val factory = Base64Fetcher.Factory()
    private val testDispatcher = UnconfinedTestDispatcher()

    // 1x1 transparent PNG in Base64
    private val samplePngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
    private val samplePngDataUri = "data:image/png;base64,$samplePngBase64"

    // Minimal JPEG header in Base64
    private val sampleJpegBase64 = "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
    private val sampleJpegDataUri = "data:image/jpeg;base64,$sampleJpegBase64"

    // JPEG base64 that doesn't have slash after /9j (e.g. /9j4...)
    private val sampleJpegBase64NoSlash = "/9j4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        imageLoader = ImageLoader.Builder(context)
            .components {
                add(mapper)
                add(keyer)
                add(factory)
            }
            .dispatcher(testDispatcher)
            .fetcherDispatcher(testDispatcher)
            .decoderDispatcher(testDispatcher)
            .interceptorDispatcher(testDispatcher)
            .allowHardware(false)
            .build()
        options = Options(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Mapper Tests ---

    @Test
    fun mapper_identifiesDataUriJpeg() {
        val result = mapper.map(sampleJpegDataUri, options)
        assertNotNull(result)
        assertEquals(sampleJpegDataUri, result?.payload)
    }

    @Test
    fun mapper_identifiesDataUriPng() {
        val result = mapper.map(samplePngDataUri, options)
        assertNotNull(result)
        assertEquals(samplePngDataUri, result?.payload)
    }

    @Test
    fun mapper_identifiesRawBase64Jpeg() {
        val result = mapper.map(sampleJpegBase64, options)
        assertNotNull(result)
        assertEquals(sampleJpegBase64, result?.payload)
    }

    @Test
    fun mapper_identifiesRawBase64Png() {
        val result = mapper.map(samplePngBase64, options)
        assertNotNull(result)
        assertEquals(samplePngBase64, result?.payload)
    }

    @Test
    fun mapper_identifiesRawBase64JpegWithoutSlash() {
        val result = mapper.map(sampleJpegBase64NoSlash, options)
        assertNotNull(result)
        assertEquals(sampleJpegBase64NoSlash, result?.payload)
    }

    @Test
    fun mapper_identifiesCorruptedFileBase64() {
        val corruptedJpeg = "file://$sampleJpegBase64NoSlash"
        val result = mapper.map(corruptedJpeg, options)
        assertNotNull(result)
        assertEquals(corruptedJpeg, result?.payload)
    }

    @Test
    fun mapper_ignoresHttpAndFileUrls() {
        assertNull(mapper.map("https://storage.googleapis.com/couplebubble/photo.jpg", options))
        assertNull(mapper.map("file:///data/user/0/app/files/memories/photo.jpg", options))
        assertNull(mapper.map("content://media/external/images/1", options))
    }

    // --- Keyer Tests ---

    @Test
    fun keyer_generatesConsistentCacheKey() {
        val image = Base64Image(samplePngDataUri)
        val key = keyer.key(image, options)
        val expectedKey = "b64:${samplePngDataUri.length}:${samplePngDataUri.hashCode()}"
        assertEquals(expectedKey, key)
    }

    // --- Fetcher & Factory Tests ---

    @Test
    fun factory_createsFetcherForBase64Image() {
        val fetcher = factory.create(Base64Image(samplePngDataUri), options, imageLoader)
        assertNotNull(fetcher)
    }

    @Test
    fun fetch_decodesDataUriSuccessfully() = runTest {
        val fetcher = factory.create(Base64Image(samplePngDataUri), options, imageLoader)
        val result = fetcher.fetch()
        assertTrue(result is DrawableResult || result is SourceResult)
        if (result is DrawableResult) {
            assertNotNull(result.drawable)
        } else if (result is SourceResult) {
            assertEquals("image/png", result.mimeType)
            val bytes = result.source.source().readByteArray()
            assertTrue(bytes.isNotEmpty())
        }
    }

    @Test
    fun fetch_decodesRawBase64Successfully() = runTest {
        val fetcher = factory.create(Base64Image(sampleJpegBase64), options, imageLoader)
        val result = fetcher.fetch()
        assertTrue(result is DrawableResult || result is SourceResult)
        if (result is DrawableResult) {
            assertNotNull(result.drawable)
        } else if (result is SourceResult) {
            assertEquals("image/jpeg", result.mimeType)
            val bytes = result.source.source().readByteArray()
            assertTrue(bytes.isNotEmpty())
        }
    }

    @Test
    fun fetch_healsAndDecodesCorruptedFilePrefixSuccessfully() = runTest {
        val corruptedJpeg = "file://$sampleJpegBase64"
        val fetcher = factory.create(Base64Image(corruptedJpeg), options, imageLoader)
        val result = fetcher.fetch()
        assertTrue(result is DrawableResult || result is SourceResult)
        if (result is DrawableResult) {
            assertNotNull(result.drawable)
        } else if (result is SourceResult) {
            assertEquals("image/jpeg", result.mimeType)
            val bytes = result.source.source().readByteArray()
            assertTrue(bytes.isNotEmpty())
        }
    }

    // --- End-to-End ImageLoader Pipeline Tests ---

    @Test
    fun imageLoader_executesPngDataUriSuccessfully() = runTest {
        val request = ImageRequest.Builder(context)
            .data(samplePngDataUri)
            .allowHardware(false)
            .build()
        val result = imageLoader.execute(request)
        assertTrue("Result should be SuccessResult but was $result", result is SuccessResult)
    }

    @Test
    fun imageLoader_executesJpegDataUriSuccessfully() = runTest {
        val request = ImageRequest.Builder(context)
            .data(sampleJpegDataUri)
            .allowHardware(false)
            .build()
        val result = imageLoader.execute(request)
        assertTrue("Result should be SuccessResult but was $result", result is SuccessResult)
    }

    @Test
    fun imageLoader_executesRawBase64JpegSuccessfully() = runTest {
        val request = ImageRequest.Builder(context)
            .data(sampleJpegBase64)
            .allowHardware(false)
            .build()
        val result = imageLoader.execute(request)
        assertTrue("Result should be SuccessResult but was $result", result is SuccessResult)
    }

    @Test
    fun imageLoader_executesCorruptedFilePrefixBase64Successfully() = runTest {
        val request = ImageRequest.Builder(context)
            .data("file://$sampleJpegBase64")
            .allowHardware(false)
            .build()
        val result = imageLoader.execute(request)
        assertTrue("Result should be SuccessResult but was $result", result is SuccessResult)
    }
}
