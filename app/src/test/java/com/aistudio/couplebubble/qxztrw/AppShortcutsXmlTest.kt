package com.aistudio.couplebubble.qxztrw

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.aistudio.couplebubble.qxztrw.domain.model.DeepLinkContracts
import com.aistudio.couplebubble.qxztrw.domain.model.DeepLinkTarget
import com.aistudio.couplebubble.qxztrw.domain.model.parseDeepLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
class AppShortcutsXmlTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    data class ParsedShortcut(
        val id: String,
        val shortLabelResId: Int,
        val iconResId: Int,
        val action: String?,
        val targetPackage: String?,
        val targetClass: String?,
        val dataUri: String?,
        val extraTarget: String?
    )

    @Test
    fun shortcutsXml_definesExpectedShortcutsWithValidLabelsIconsAndIntents() {
        val parser = context.resources.getXml(R.xml.shortcuts)
        val shortcuts = mutableListOf<ParsedShortcut>()

        var currentId: String? = null
        var currentShortLabel = 0
        var currentIcon = 0
        var currentAction: String? = null
        var currentPackage: String? = null
        var currentClass: String? = null
        var currentData: String? = null
        var currentExtraTarget: String? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "shortcut" -> {
                            currentId = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "shortcutId")
                            currentShortLabel = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "shortcutShortLabel", 0)
                            currentIcon = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "icon", 0)
                        }
                        "intent" -> {
                            currentAction = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "action")
                            currentPackage = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "targetPackage")
                            currentClass = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "targetClass")
                            currentData = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "data")
                        }
                        "extra" -> {
                            val name = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                            val value = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "value")
                            if (name == DeepLinkContracts.EXTRA_TARGET) {
                                currentExtraTarget = value
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "shortcut") {
                        assertNotNull(currentId)
                        shortcuts.add(
                            ParsedShortcut(
                                id = currentId!!,
                                shortLabelResId = currentShortLabel,
                                iconResId = currentIcon,
                                action = currentAction,
                                targetPackage = currentPackage,
                                targetClass = currentClass,
                                dataUri = currentData,
                                extraTarget = currentExtraTarget
                            )
                        )
                        currentId = null
                        currentShortLabel = 0
                        currentIcon = 0
                        currentAction = null
                        currentPackage = null
                        currentClass = null
                        currentData = null
                        currentExtraTarget = null
                    }
                }
            }
            eventType = parser.next()
        }

        assertEquals(3, shortcuts.size)
        val ids = shortcuts.map { it.id }
        assertEquals(listOf("new_moment", "new_note", "new_list"), ids)

        // 1. Neuer Moment
        val momentShortcut = shortcuts.first { it.id == "new_moment" }
        assertEquals("Neuer Moment", context.getString(momentShortcut.shortLabelResId))
        assertNotNull(ContextCompat.getDrawable(context, momentShortcut.iconResId))
        assertEquals(Intent.ACTION_VIEW, momentShortcut.action)
        assertEquals("com.aistudio.couplebubble.qxztrw", momentShortcut.targetPackage)
        assertEquals("com.aistudio.couplebubble.qxztrw.MainActivity", momentShortcut.targetClass)
        assertEquals("couplebubble://new_moment", momentShortcut.dataUri)
        assertEquals(DeepLinkContracts.TARGET_NEW_MOMENT, momentShortcut.extraTarget)

        val momentIntentFromExtra = Intent(momentShortcut.action).apply {
            putExtra(DeepLinkContracts.EXTRA_TARGET, momentShortcut.extraTarget)
        }
        assertEquals(DeepLinkTarget.NewMoment, parseDeepLink(momentIntentFromExtra))

        val momentIntentFromData = Intent(momentShortcut.action, Uri.parse(momentShortcut.dataUri))
        assertEquals(DeepLinkTarget.NewMoment, parseDeepLink(momentIntentFromData))

        // 2. Neue Notiz
        val noteShortcut = shortcuts.first { it.id == "new_note" }
        assertEquals("Neue Notiz", context.getString(noteShortcut.shortLabelResId))
        assertNotNull(ContextCompat.getDrawable(context, noteShortcut.iconResId))
        assertEquals(Intent.ACTION_VIEW, noteShortcut.action)
        assertEquals("com.aistudio.couplebubble.qxztrw", noteShortcut.targetPackage)
        assertEquals("com.aistudio.couplebubble.qxztrw.MainActivity", noteShortcut.targetClass)
        assertEquals("couplebubble://new_note", noteShortcut.dataUri)
        assertEquals(DeepLinkContracts.TARGET_NEW_NOTE, noteShortcut.extraTarget)

        val noteIntentFromExtra = Intent(noteShortcut.action).apply {
            putExtra(DeepLinkContracts.EXTRA_TARGET, noteShortcut.extraTarget)
        }
        assertEquals(DeepLinkTarget.NewNote, parseDeepLink(noteIntentFromExtra))

        val noteIntentFromData = Intent(noteShortcut.action, Uri.parse(noteShortcut.dataUri))
        assertEquals(DeepLinkTarget.NewNote, parseDeepLink(noteIntentFromData))

        // 3. Neue Liste
        val listShortcut = shortcuts.first { it.id == "new_list" }
        assertEquals("Neue Liste", context.getString(listShortcut.shortLabelResId))
        assertNotNull(ContextCompat.getDrawable(context, listShortcut.iconResId))
        assertEquals(Intent.ACTION_VIEW, listShortcut.action)
        assertEquals("com.aistudio.couplebubble.qxztrw", listShortcut.targetPackage)
        assertEquals("com.aistudio.couplebubble.qxztrw.MainActivity", listShortcut.targetClass)
        assertEquals("couplebubble://new_list", listShortcut.dataUri)
        assertEquals(DeepLinkContracts.TARGET_NEW_LIST, listShortcut.extraTarget)

        val listIntentFromExtra = Intent(listShortcut.action).apply {
            putExtra(DeepLinkContracts.EXTRA_TARGET, listShortcut.extraTarget)
        }
        assertEquals(DeepLinkTarget.NewList, parseDeepLink(listIntentFromExtra))

        val listIntentFromData = Intent(listShortcut.action, Uri.parse(listShortcut.dataUri))
        assertEquals(DeepLinkTarget.NewList, parseDeepLink(listIntentFromData))
    }
}
