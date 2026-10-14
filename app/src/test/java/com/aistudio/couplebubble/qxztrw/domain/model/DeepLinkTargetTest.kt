package com.aistudio.couplebubble.qxztrw.domain.model

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepLinkTargetTest {

    @Test
    fun parseDeepLinkTarget_momentWithValidId_returnsMomentTarget() {
        val target = parseDeepLinkTarget(
            target = DeepLinkContracts.TARGET_MOMENT,
            memoryId = "memory_123"
        )
        assertEquals(DeepLinkTarget.Moment("memory_123"), target)
    }

    @Test
    fun parseDeepLinkTarget_momentWithBlankId_returnsNull() {
        assertNull(parseDeepLinkTarget(DeepLinkContracts.TARGET_MOMENT, memoryId = null))
        assertNull(parseDeepLinkTarget(DeepLinkContracts.TARGET_MOMENT, memoryId = ""))
        assertNull(parseDeepLinkTarget(DeepLinkContracts.TARGET_MOMENT, memoryId = "   "))
    }

    @Test
    fun parseDeepLinkTarget_milestones_returnsMilestonesTarget() {
        val target = parseDeepLinkTarget(DeepLinkContracts.TARGET_MILESTONES)
        assertEquals(DeepLinkTarget.Milestones, target)
    }

    @Test
    fun parseDeepLinkTarget_reunion_returnsReunionTarget() {
        val target = parseDeepLinkTarget(DeepLinkContracts.TARGET_REUNION)
        assertEquals(DeepLinkTarget.Reunion, target)
    }

    @Test
    fun parseDeepLinkTarget_home_returnsHomeTarget() {
        val target = parseDeepLinkTarget(DeepLinkContracts.TARGET_HOME)
        assertEquals(DeepLinkTarget.Home, target)
    }

    @Test
    fun parseDeepLinkTarget_shortcuts_returnsShortcutTargets() {
        assertEquals(DeepLinkTarget.NewMoment, parseDeepLinkTarget(DeepLinkContracts.TARGET_NEW_MOMENT))
        assertEquals(DeepLinkTarget.NewNote, parseDeepLinkTarget(DeepLinkContracts.TARGET_NEW_NOTE))
        assertEquals(DeepLinkTarget.NewList, parseDeepLinkTarget(DeepLinkContracts.TARGET_NEW_LIST))
    }

    @Test
    fun parseDeepLinkTarget_caseInsensitiveAndWhitespaceTrimmed() {
        assertEquals(
            DeepLinkTarget.Moment("mem_abc"),
            parseDeepLinkTarget("  MOMENT  ", memoryId = "  mem_abc  ")
        )
        assertEquals(
            DeepLinkTarget.Milestones,
            parseDeepLinkTarget("  Milestones  ")
        )
        assertEquals(
            DeepLinkTarget.Reunion,
            parseDeepLinkTarget("  REUNION ")
        )
        assertEquals(
            DeepLinkTarget.Home,
            parseDeepLinkTarget(" Home ")
        )
        assertEquals(
            DeepLinkTarget.NewMoment,
            parseDeepLinkTarget("  NEW_MOMENT ")
        )
        assertEquals(
            DeepLinkTarget.NewMoment,
            parseDeepLinkTarget("  new-moment  ")
        )
        assertEquals(
            DeepLinkTarget.NewNote,
            parseDeepLinkTarget(" new_note ")
        )
        assertEquals(
            DeepLinkTarget.NewNote,
            parseDeepLinkTarget("  NEW-NOTE  ")
        )
        assertEquals(
            DeepLinkTarget.NewList,
            parseDeepLinkTarget(" new_list ")
        )
        assertEquals(
            DeepLinkTarget.NewList,
            parseDeepLinkTarget("  new-list ")
        )
    }

    @Test
    fun parseDeepLinkTarget_unknownOrBlank_returnsNull() {
        assertNull(parseDeepLinkTarget("unknown_target"))
        assertNull(parseDeepLinkTarget(null))
        assertNull(parseDeepLinkTarget(""))
        assertNull(parseDeepLinkTarget("   "))
    }

    @Test
    fun parseDeepLinkTarget_uriParsing() {
        assertEquals(
            DeepLinkTarget.Moment("mem_456"),
            parseDeepLinkTarget(null, uriString = "couplebubble://moment/mem_456")
        )
        assertEquals(
            DeepLinkTarget.Moment("mem_789"),
            parseDeepLinkTarget(null, uriString = "couplebubble://moment?id=mem_789")
        )
        assertEquals(
            DeepLinkTarget.Milestones,
            parseDeepLinkTarget(null, uriString = "couplebubble://milestones")
        )
        assertEquals(
            DeepLinkTarget.Reunion,
            parseDeepLinkTarget(null, uriString = "couplebubble://reunion")
        )
        assertEquals(
            DeepLinkTarget.Home,
            parseDeepLinkTarget(null, uriString = "couplebubble://home")
        )
        assertEquals(
            DeepLinkTarget.NewMoment,
            parseDeepLinkTarget(null, uriString = "couplebubble://new_moment")
        )
        assertEquals(
            DeepLinkTarget.NewMoment,
            parseDeepLinkTarget(null, uriString = "couplebubble://new-moment")
        )
        assertEquals(
            DeepLinkTarget.NewNote,
            parseDeepLinkTarget(null, uriString = "couplebubble://new_note")
        )
        assertEquals(
            DeepLinkTarget.NewNote,
            parseDeepLinkTarget(null, uriString = "couplebubble://new-note")
        )
        assertEquals(
            DeepLinkTarget.NewList,
            parseDeepLinkTarget(null, uriString = "couplebubble://new_list")
        )
        assertEquals(
            DeepLinkTarget.NewList,
            parseDeepLinkTarget(null, uriString = "couplebubble://new-list")
        )
        assertNull(
            parseDeepLinkTarget(null, uriString = "https://couplebubble.com/milestones")
        )
        assertNull(
            parseDeepLinkTarget(null, uriString = "invalid://uri")
        )
    }

    @Test
    fun parseDeepLink_fromIntentExtras() {
        assertNull(parseDeepLink(null))

        val emptyIntent = Intent()
        assertNull(parseDeepLink(emptyIntent))

        val momentIntent = Intent().apply {
            putExtra(DeepLinkContracts.EXTRA_TARGET, DeepLinkContracts.TARGET_MOMENT)
            putExtra(DeepLinkContracts.EXTRA_MEMORY_ID, "intent_mem_1")
        }
        assertEquals(DeepLinkTarget.Moment("intent_mem_1"), parseDeepLink(momentIntent))

        val milestonesIntent = Intent().apply {
            putExtra(DeepLinkContracts.EXTRA_TARGET, DeepLinkContracts.TARGET_MILESTONES)
        }
        assertEquals(DeepLinkTarget.Milestones, parseDeepLink(milestonesIntent))

        val reunionIntent = Intent().apply {
            putExtra(DeepLinkContracts.EXTRA_TARGET, DeepLinkContracts.TARGET_REUNION)
        }
        assertEquals(DeepLinkTarget.Reunion, parseDeepLink(reunionIntent))

        val homeIntent = Intent().apply {
            putExtra(DeepLinkContracts.EXTRA_TARGET, DeepLinkContracts.TARGET_HOME)
        }
        assertEquals(DeepLinkTarget.Home, parseDeepLink(homeIntent))

        val newMomentIntent = Intent().apply {
            putExtra(DeepLinkContracts.EXTRA_TARGET, DeepLinkContracts.TARGET_NEW_MOMENT)
        }
        assertEquals(DeepLinkTarget.NewMoment, parseDeepLink(newMomentIntent))

        val newNoteIntent = Intent().apply {
            putExtra(DeepLinkContracts.EXTRA_TARGET, DeepLinkContracts.TARGET_NEW_NOTE)
        }
        assertEquals(DeepLinkTarget.NewNote, parseDeepLink(newNoteIntent))

        val newListIntent = Intent().apply {
            putExtra(DeepLinkContracts.EXTRA_TARGET, DeepLinkContracts.TARGET_NEW_LIST)
        }
        assertEquals(DeepLinkTarget.NewList, parseDeepLink(newListIntent))
    }

    @Test
    fun parseDeepLink_fromIntentDataUri() {
        val newMomentIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("couplebubble://new_moment"))
        assertEquals(DeepLinkTarget.NewMoment, parseDeepLink(newMomentIntent))

        val newNoteIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("couplebubble://new_note"))
        assertEquals(DeepLinkTarget.NewNote, parseDeepLink(newNoteIntent))

        val newListIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("couplebubble://new_list"))
        assertEquals(DeepLinkTarget.NewList, parseDeepLink(newListIntent))
    }

    @Test
    fun parseDeepLink_actionSendImageStream_returnsShareImage() {
        val uri = android.net.Uri.parse("content://media/external/images/media/42")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        val target = parseDeepLink(intent)
        assertEquals(DeepLinkTarget.ShareImage(uri), target)
    }

    @Test
    fun parseDeepLink_actionSendImageClipData_returnsShareImage() {
        val uri = android.net.Uri.parse("content://com.android.providers.media/image/99")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            clipData = android.content.ClipData.newRawUri("image", uri)
        }
        val target = parseDeepLink(intent)
        assertEquals(DeepLinkTarget.ShareImage(uri), target)
    }

    @Test
    fun parseDeepLink_actionSendImageWithoutUri_returnsNull() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
        }
        assertNull(parseDeepLink(intent))
    }

    @Test
    fun parseDeepLink_actionSendTextPlain_returnsShareText() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Schau mal, das müssen wir ausprobieren!")
        }
        val target = parseDeepLink(intent)
        assertEquals(
            DeepLinkTarget.ShareText("Schau mal, das müssen wir ausprobieren!", title = ""),
            target
        )
    }

    @Test
    fun parseDeepLink_actionSendTextWithSubject_returnsShareTextWithTitle() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "- Äpfel\n- Birnen")
            putExtra(Intent.EXTRA_SUBJECT, "Einkaufszettel")
        }
        val target = parseDeepLink(intent)
        assertEquals(
            DeepLinkTarget.ShareText("- Äpfel\n- Birnen", title = "Einkaufszettel"),
            target
        )
    }

    @Test
    fun parseDeepLink_actionSendBlankText_returnsNull() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "   \n  ")
        }
        assertNull(parseDeepLink(intent))
    }

    @Test
    fun pendingIntentRequestCodes_areUnique() {
        val codes = setOf(
            PendingIntentRequestCodes.NOTIFICATION_MOMENTS_SUMMARY,
            PendingIntentRequestCodes.NOTIFICATION_MILESTONES_SUMMARY,
            PendingIntentRequestCodes.notificationMilestone("2026-10-14"),
            PendingIntentRequestCodes.notificationMilestone("2027-01-01"),
            PendingIntentRequestCodes.notificationMoment("mem1"),
            PendingIntentRequestCodes.notificationMoment("mem2")
        )
        // Ensure no collisions between the set of codes
        assertEquals(6, codes.size)

        // Summary codes must be strictly distinct from each other
        assertNotEquals(
            PendingIntentRequestCodes.NOTIFICATION_MOMENTS_SUMMARY,
            PendingIntentRequestCodes.NOTIFICATION_MILESTONES_SUMMARY
        )

        // Milestone and moment ranges do not overlap
        assertTrue(PendingIntentRequestCodes.notificationMilestone("2026-10-14") < PendingIntentRequestCodes.NOTIFICATION_MOMENTS_BASE)
        assertTrue(PendingIntentRequestCodes.notificationMoment("mem1") >= PendingIntentRequestCodes.NOTIFICATION_MOMENTS_BASE)
    }
}
