package com.aistudio.couplebubble.qxztrw.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidLocalImageStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = AndroidLocalImageStore(context)

    private fun fileOf(url: String?): File = File(requireNotNull(url).removePrefix("file://"))

    @Test
    fun memoryPhotosAreSavedLocallyPerSlot() {
        val urlA = store.saveMemoryImage("test_local_id", "a", byteArrayOf(10, 20, 30))
        val urlB = store.saveMemoryImage("test_local_id", "b", byteArrayOf(40, 50, 60))

        assertTrue(urlA!!.startsWith("file://"))
        assertArrayEquals(byteArrayOf(10, 20, 30), fileOf(urlA).readBytes())
        assertArrayEquals(byteArrayOf(40, 50, 60), fileOf(urlB).readBytes())
    }

    @Test
    fun aNewVersionReplacesTheOldFileUnderANewName() {
        val first = store.saveMemoryImage("m1", "a", byteArrayOf(1))
        Thread.sleep(2)
        val second = store.saveMemoryImage("m1", "a", byteArrayOf(2))

        // A new name keeps Coil from serving the old image from its cache
        assertNotEquals(first, second)
        assertFalse(fileOf(first).exists())
        assertArrayEquals(byteArrayOf(2), fileOf(second).readBytes())
    }

    @Test
    fun profilePhotosAreNamedAfterTheOwner() {
        val url = store.saveProfilePhoto("space_1", "uid_mia", byteArrayOf(7))

        assertTrue(fileOf(url).name.startsWith("profile_space_1_uid_mia_"))
        assertEquals("profiles", fileOf(url).parentFile?.name)
    }
}
