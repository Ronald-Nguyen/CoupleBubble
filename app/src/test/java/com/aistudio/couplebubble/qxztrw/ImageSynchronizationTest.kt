package com.aistudio.couplebubble.qxztrw

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.fetch.DrawableResult
import coil.fetch.SourceResult
import coil.request.Options
import com.aistudio.couplebubble.qxztrw.data.LocalImageStorage
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.repository.FirebaseCoupleRepository
import com.aistudio.couplebubble.qxztrw.ui.coil.Base64Fetcher
import com.aistudio.couplebubble.qxztrw.ui.coil.Base64Image
import com.aistudio.couplebubble.qxztrw.ui.coil.Base64Mapper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ImageSynchronizationTest {

    private lateinit var context: Context
    private lateinit var imageLoader: ImageLoader
    private lateinit var options: Options
    private val mapper = Base64Mapper()
    private val factory = Base64Fetcher.Factory()
    private val repository = FirebaseCoupleRepository()

    // 1x1 transparent PNG in Base64
    private val samplePngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
    private val samplePngDataUri = "data:image/png;base64,$samplePngBase64"

    // JPEG base64 with /9j/
    private val sampleJpegBase64WithSlash = "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
    private val sampleJpegDataUri = "data:image/jpeg;base64,$sampleJpegBase64WithSlash"

    // JPEG base64 with /9j4 (no trailing slash after /9j, standard EXIF camera output)
    private val sampleJpegBase64NoSlash = "/9j4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        imageLoader = ImageLoader(context)
        options = Options(context)
    }

    // --- Base64Mapper & Fetcher Tests ---

    @Test
    fun base64Mapper_identifiesAllValidBase64Variants() {
        assertNotNull(mapper.map(sampleJpegDataUri, options))
        assertNotNull(mapper.map(samplePngDataUri, options))
        assertNotNull(mapper.map(sampleJpegBase64WithSlash, options))
        assertNotNull(mapper.map(sampleJpegBase64NoSlash, options))
        assertNotNull(mapper.map(samplePngBase64, options))

        // Auto-healed corrupted URIs
        assertNotNull(mapper.map("file:///9j4AAQSkZJRgABAQEASABIAAD", options))
        assertNotNull(mapper.map("file://iVBORw0KGgoAAAANSUhEUgAA", options))
    }

    @Test
    fun base64Mapper_ignoresRemoteAndLegitimateLocalPaths() {
        assertNull(mapper.map("https://firebasestorage.googleapis.com/v0/b/app/o/photo.jpg", options))
        assertNull(mapper.map("http://example.com/couple.jpg", options))
        assertNull(mapper.map("file:///data/user/0/com.app/files/memories/m1_a.jpg", options))
        assertNull(mapper.map("content://media/external/images/media/42", options))
    }

    @Test
    fun base64Fetcher_decodesDirectlyIntoDrawableResultWithoutStreamExhaustion() = runTest {
        val image = Base64Image(sampleJpegDataUri)
        val fetcher = factory.create(image, options, imageLoader)
        assertNotNull("Fetcher must be created for valid JPEG data URI", fetcher)

        val result1 = fetcher.fetch()
        assertTrue("Result must be DrawableResult or SourceResult", result1 is DrawableResult || result1 is SourceResult)
        if (result1 is DrawableResult) {
            assertNotNull(result1.drawable)
        }

        // Fetch again with a new fetcher to ensure repeat decoding works reliably
        val fetcher2 = factory.create(image, options, imageLoader)
        val result2 = fetcher2.fetch()
        assertTrue("Second fetch must succeed", result2 is DrawableResult || result2 is SourceResult)
    }

    // --- FirebaseCoupleRepository Image Sanitization Tests ---

    @Test
    fun sanitizeImageUrl_normalizesRawBase64ToDataUris() {
        val normalizedWithSlash = repository.sanitizeImageUrl(sampleJpegBase64WithSlash)
        assertTrue(normalizedWithSlash?.startsWith("data:image/jpeg;base64,/9j") == true)

        val normalizedNoSlash = repository.sanitizeImageUrl(sampleJpegBase64NoSlash)
        assertTrue(normalizedNoSlash?.startsWith("data:image/jpeg;base64,/9j4") == true)

        val normalizedPng = repository.sanitizeImageUrl(samplePngBase64)
        assertTrue(normalizedPng?.startsWith("data:image/png;base64,iVBORw0KGgo") == true)
    }

    @Test
    fun sanitizeImageUrl_autoHealsCorruptedFileBase64Paths() {
        val corruptedJpeg = "file://$sampleJpegBase64NoSlash"
        val healed = repository.sanitizeImageUrl(corruptedJpeg)
        assertTrue("Corrupted file:///9j must be healed to data:image/jpeg;base64,", healed?.startsWith("data:image/jpeg;base64,/9j4") == true)

        val corruptedPng = "file://$samplePngBase64"
        val healedPng = repository.sanitizeImageUrl(corruptedPng)
        assertTrue("Corrupted file://iVBORw0KGgo must be healed to data:image/png;base64,", healedPng?.startsWith("data:image/png;base64,iVBORw0KGgo") == true)
    }

    @Test
    fun sanitizeImageUrl_preservesHttpAndValidLocalPaths() {
        val remoteUrl = "https://firebasestorage.googleapis.com/v0/b/couplebubble/o/photo.jpg?alt=media&token=123"
        assertEquals(remoteUrl, repository.sanitizeImageUrl(remoteUrl))

        val localFile = "file:///data/user/0/com.aistudio.couplebubble.qxztrw/files/memories/m1_a.jpg"
        assertEquals(localFile, repository.sanitizeImageUrl(localFile))

        assertNull(repository.sanitizeImageUrl(null))
        assertNull(repository.sanitizeImageUrl("   "))
    }

    @Test
    fun isLocalUri_accuratelyDifferentiatesLocalFilesFromBase64Payloads() {
        // True local files that should NOT be pushed to remote Firestore as-is
        assertTrue(repository.isLocalUri("file:///data/user/0/app/files/memories/m1_a.jpg"))
        assertTrue(repository.isLocalUri("/data/user/0/app/files/profiles/p1.jpg"))
        assertTrue(repository.isLocalUri("content://media/picker/1"))

        // Remote or Base64 payloads must NOT be flagged as local files
        assertFalse("Data URI must not be local", repository.isLocalUri(sampleJpegDataUri))
        assertFalse("Raw JPEG Base64 /9j4 must not be flagged as local Unix file path", repository.isLocalUri(sampleJpegBase64NoSlash))
        assertFalse("Raw JPEG Base64 /9j/ must not be flagged as local Unix file path", repository.isLocalUri(sampleJpegBase64WithSlash))
        assertFalse("Healed file:///9j must not be flagged as local", repository.isLocalUri("file://$sampleJpegBase64NoSlash"))
        assertFalse("Remote HTTPS must not be local", repository.isLocalUri("https://firebasestorage.googleapis.com/photo.jpg"))
    }

    // --- LocalImageStorage Helper Tests ---

    @Test
    fun localImageStorage_isLikelyBase64Detection() {
        assertTrue(LocalImageStorage.isLikelyBase64(sampleJpegBase64NoSlash))
        assertTrue(LocalImageStorage.isLikelyBase64(sampleJpegBase64WithSlash))
        assertTrue(LocalImageStorage.isLikelyBase64(samplePngBase64))
        assertTrue(LocalImageStorage.isLikelyBase64("UklGR" + "A".repeat(60)))

        // Non-Base64 or short paths
        assertFalse(LocalImageStorage.isLikelyBase64("/data/user/0/files/pic.jpg"))
        assertFalse(LocalImageStorage.isLikelyBase64("short_string"))
    }

    // --- Memory Model Dual-Slot Preservation Tests ---

    @Test
    fun memoryModel_dualSlotPhotoHandlingAndFallbacks() {
        val memoryOnlyA = Memory(
            id = "m1",
            title = "Strandspaziergang",
            date = LocalDate.of(2026, 7, 1),
            partnerAImageUrl = "data:image/jpeg;base64,photoA",
            partnerBImageUrl = null
        )
        assertEquals("data:image/jpeg;base64,photoA", memoryOnlyA.effectivePartnerAImage)
        assertNull(memoryOnlyA.effectivePartnerBImage)
        assertTrue(memoryOnlyA.hasAnyImage)
        assertEquals(1, memoryOnlyA.imageCount)

        val memoryDual = memoryOnlyA.copy(
            partnerBImageUrl = "data:image/jpeg;base64,photoB"
        )
        assertEquals("data:image/jpeg;base64,photoA", memoryDual.effectivePartnerAImage)
        assertEquals("data:image/jpeg;base64,photoB", memoryDual.effectivePartnerBImage)
        assertTrue(memoryDual.hasAnyImage)
        assertEquals(2, memoryDual.imageCount)

        // Legacy fallback support: if partnerAImageUrl is null, imageUrl serves as partner A's photo
        val memoryLegacy = Memory(
            id = "m3",
            title = "Erster Kaffee",
            date = LocalDate.of(2025, 6, 25),
            imageUrl = "https://example.com/legacy.jpg",
            partnerAImageUrl = null,
            partnerBImageUrl = null
        )
        assertEquals("https://example.com/legacy.jpg", memoryLegacy.effectivePartnerAImage)
        assertNull(memoryLegacy.effectivePartnerBImage)
        assertEquals(1, memoryLegacy.imageCount)
    }
}
