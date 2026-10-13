package com.aistudio.couplebubble.qxztrw.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryModelTest {

    @Test
    fun dualPhotoProperties() {
        val dual = Memory(id = "dual1", partner1ImageUrl = "https://example.com/a.jpg", partner2ImageUrl = "https://example.com/b.jpg")
        assertEquals("https://example.com/a.jpg", dual.effectivePartner1Image)
        assertEquals("https://example.com/b.jpg", dual.effectivePartner2Image)
        assertTrue(dual.hasAnyImage)
        assertEquals(2, dual.imageCount)

        val empty = Memory(id = "empty1", title = "No photo")
        assertFalse(empty.hasAnyImage)
        assertEquals(0, empty.imageCount)
    }

    @Test
    fun legacySinglePhotoCountsAsPartner1() {
        val legacy = Memory(id = "legacy", imageUrl = "https://example.com/single.jpg")

        assertEquals("https://example.com/single.jpg", legacy.effectivePartner1Image)
        assertNull(legacy.effectivePartner2Image)
        assertTrue(legacy.hasAnyImage)
        assertEquals(1, legacy.imageCount)
    }

    @Test
    fun localCopiesAreRegularPhotos() {
        val memory = Memory(
            id = "local",
            partner1ImageUrl = "file:///data/memories/memory_local_a_1.jpg",
            partner2ImageUrl = "file:///data/memories/memory_local_b_1.jpg"
        )

        assertEquals(2, memory.imageCount)
        assertEquals("file:///data/memories/memory_local_b_1.jpg", memory.effectivePartner2Image)
    }
}
