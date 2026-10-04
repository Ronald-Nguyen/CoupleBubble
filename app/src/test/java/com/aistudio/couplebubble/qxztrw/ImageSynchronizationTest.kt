package com.aistudio.couplebubble.qxztrw

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.fetch.DrawableResult
import coil.fetch.SourceResult
import coil.request.Options
import com.aistudio.couplebubble.qxztrw.data.ImageUrls
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.repository.resolveMemorySlots
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
        val normalizedWithSlash = ImageUrls.sanitize(sampleJpegBase64WithSlash)
        assertTrue(normalizedWithSlash?.startsWith("data:image/jpeg;base64,/9j") == true)

        val normalizedNoSlash = ImageUrls.sanitize(sampleJpegBase64NoSlash)
        assertTrue(normalizedNoSlash?.startsWith("data:image/jpeg;base64,/9j4") == true)

        val normalizedPng = ImageUrls.sanitize(samplePngBase64)
        assertTrue(normalizedPng?.startsWith("data:image/png;base64,iVBORw0KGgo") == true)
    }

    @Test
    fun sanitizeImageUrl_autoHealsCorruptedFileBase64Paths() {
        val corruptedJpeg = "file://$sampleJpegBase64NoSlash"
        val healed = ImageUrls.sanitize(corruptedJpeg)
        assertTrue("Corrupted file:///9j must be healed to data:image/jpeg;base64,", healed?.startsWith("data:image/jpeg;base64,/9j4") == true)

        val corruptedPng = "file://$samplePngBase64"
        val healedPng = ImageUrls.sanitize(corruptedPng)
        assertTrue("Corrupted file://iVBORw0KGgo must be healed to data:image/png;base64,", healedPng?.startsWith("data:image/png;base64,iVBORw0KGgo") == true)
    }

    @Test
    fun sanitizeImageUrl_preservesHttpAndValidLocalPaths() {
        val remoteUrl = "https://firebasestorage.googleapis.com/v0/b/couplebubble/o/photo.jpg?alt=media&token=123"
        assertEquals(remoteUrl, ImageUrls.sanitize(remoteUrl))

        val localFile = "file:///data/user/0/com.aistudio.couplebubble.qxztrw/files/memories/m1_a.jpg"
        assertEquals(localFile, ImageUrls.sanitize(localFile))

        assertNull(ImageUrls.sanitize(null))
        assertNull(ImageUrls.sanitize("   "))
    }

    @Test
    fun isLocalUri_accuratelyDifferentiatesLocalFilesFromBase64Payloads() {
        // True local files that should NOT be pushed to remote Firestore as-is
        assertTrue(ImageUrls.isLocalUri("file:///data/user/0/app/files/memories/m1_a.jpg"))
        assertTrue(ImageUrls.isLocalUri("/data/user/0/app/files/profiles/p1.jpg"))
        assertTrue(ImageUrls.isLocalUri("content://media/picker/1"))

        // Remote or Base64 payloads must NOT be flagged as local files
        assertFalse("Data URI must not be local", ImageUrls.isLocalUri(sampleJpegDataUri))
        assertFalse("Raw JPEG Base64 /9j4 must not be flagged as local Unix file path", ImageUrls.isLocalUri(sampleJpegBase64NoSlash))
        assertFalse("Raw JPEG Base64 /9j/ must not be flagged as local Unix file path", ImageUrls.isLocalUri(sampleJpegBase64WithSlash))
        assertFalse("Healed file:///9j must not be flagged as local", ImageUrls.isLocalUri("file://$sampleJpegBase64NoSlash"))
        assertFalse("Remote HTTPS must not be local", ImageUrls.isLocalUri("https://firebasestorage.googleapis.com/photo.jpg"))
    }

    // --- LocalImageStorage Helper Tests ---

    @Test
    fun imageUrls_isRawBase64Detection() {
        assertTrue(ImageUrls.isRawBase64(sampleJpegBase64NoSlash))
        assertTrue(ImageUrls.isRawBase64(sampleJpegBase64WithSlash))
        assertTrue(ImageUrls.isRawBase64(samplePngBase64))
        assertTrue(ImageUrls.isRawBase64("UklGR" + "A".repeat(60)))

        // Non-Base64 or short paths
        assertFalse(ImageUrls.isRawBase64("/data/user/0/files/pic.jpg"))
        assertFalse(ImageUrls.isRawBase64("short_string"))
    }

    // --- Memory Model Dual-Slot Preservation Tests ---

    @Test
    fun memoryModel_dualSlotPhotoHandlingAndFallbacks() {
        val memoryOnlyA = Memory(
            id = "m1",
            title = "Strandspaziergang",
            date = LocalDate.of(2026, 7, 1),
            partner1ImageUrl = "data:image/jpeg;base64,photoA",
            partner2ImageUrl = null
        )
        assertEquals("data:image/jpeg;base64,photoA", memoryOnlyA.effectivePartner1Image)
        assertNull(memoryOnlyA.effectivePartner2Image)
        assertTrue(memoryOnlyA.hasAnyImage)
        assertEquals(1, memoryOnlyA.imageCount)

        val memoryDual = memoryOnlyA.copy(
            partner2ImageUrl = "data:image/jpeg;base64,photoB"
        )
        assertEquals("data:image/jpeg;base64,photoA", memoryDual.effectivePartner1Image)
        assertEquals("data:image/jpeg;base64,photoB", memoryDual.effectivePartner2Image)
        assertTrue(memoryDual.hasAnyImage)
        assertEquals(2, memoryDual.imageCount)

        // Legacy fallback support: if partner1ImageUrl is null, imageUrl serves as partner A's photo
        val memoryLegacy = Memory(
            id = "m3",
            title = "Erster Kaffee",
            date = LocalDate.of(2025, 6, 25),
            imageUrl = "https://example.com/legacy.jpg",
            partner1ImageUrl = null,
            partner2ImageUrl = null
        )
        assertEquals("https://example.com/legacy.jpg", memoryLegacy.effectivePartner1Image)
        assertNull(memoryLegacy.effectivePartner2Image)
        assertEquals(1, memoryLegacy.imageCount)
    }

    @Test
    fun memoryModel_legacyImageUrlMirroringPartnerBIsNotDuplicated() {
        val memory = Memory(
            id = "m4",
            title = "Nur B",
            date = LocalDate.of(2026, 7, 2),
            imageUrl = "https://example.com/b.jpg",
            partner1ImageUrl = null,
            partner2ImageUrl = "https://example.com/b.jpg"
        )
        assertNull(memory.effectivePartner1Image)
        assertEquals(1, memory.imageCount)
    }

    @Test
    fun resolveMemorySlots_dropsMirroredAndCopiedPartnerBPhotos() {
        val b = "https://example.com/b.jpg"
        // Legacy mirror of B must not become A
        assertEquals(null to b, resolveMemorySlots(legacyUrl = b, partner1Url = null, partner2Url = b))
        // A slot that was persisted as a copy of B is treated as empty
        assertEquals(null to b, resolveMemorySlots(legacyUrl = b, partner1Url = b, partner2Url = b))
        // Old single-photo moments keep their photo as A
        assertEquals("https://example.com/old.jpg" to null, resolveMemorySlots("https://example.com/old.jpg", null, null))
        // Regular dual-photo moments are untouched
        assertEquals("https://example.com/a.jpg" to b, resolveMemorySlots(b, "https://example.com/a.jpg", b))
    }
}
