package com.aistudio.couplebubble.qxztrw

import androidx.compose.ui.graphics.Color
import com.aistudio.couplebubble.qxztrw.data.local.ImageUrls
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.canSwapPartners
import com.aistudio.couplebubble.qxztrw.domain.model.editableRole
import com.aistudio.couplebubble.qxztrw.domain.model.isPartner1
import com.aistudio.couplebubble.qxztrw.domain.model.roleOf
import com.aistudio.couplebubble.qxztrw.ui.theme.parseColorHexToCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExtractedHelpersTest {

    private val jpeg = "/9j/" + "A".repeat(60)

    @Test
    fun healFileBase64_turnsFileWrappedPayloadsIntoDataUris() {
        assertEquals("data:image/jpeg;base64,$jpeg", ImageUrls.healFileBase64("file://$jpeg"))
        assertEquals("data:image/png;base64,iVBORw0KGgoAAA", ImageUrls.healFileBase64("file://iVBORw0KGgoAAA"))
        assertEquals("data:image/webp;base64,UklGRAAA", ImageUrls.healFileBase64("file://UklGRAAA"))
        assertEquals("file:///data/pic.jpg", ImageUrls.healFileBase64("file:///data/pic.jpg"))
    }

    @Test
    fun sanitize_coversEveryInputKind() {
        assertEquals("https://x.y/a.jpg", ImageUrls.sanitize("  https://x.y/a.jpg "))
        assertEquals("content://media/1", ImageUrls.sanitize("content://media/1"))
        assertEquals("data:image/jpeg;base64,$jpeg", ImageUrls.sanitize(jpeg))
        assertEquals("file:///data/pic.jpg", ImageUrls.sanitize("/data/pic.jpg"))
        assertEquals("data:image/jpeg;base64," + "Q".repeat(60), ImageUrls.sanitize("Q".repeat(60)))
        assertNull(ImageUrls.sanitize("kein bild"))
        assertNull(ImageUrls.sanitize(null))
    }

    @Test
    fun partnerRole_readsCurrentAndLegacyValues() {
        assertEquals(PartnerRole.PARTNER_1, PartnerRole.fromStored("1"))
        assertEquals(PartnerRole.PARTNER_1, PartnerRole.fromStored("A"))
        assertEquals(PartnerRole.PARTNER_2, PartnerRole.fromStored("2"))
        assertEquals(PartnerRole.PARTNER_2, PartnerRole.fromStored("B"))
        assertNull(PartnerRole.fromStored(null))
        assertEquals(PartnerRole.PARTNER_2, PartnerRole.PARTNER_1.swapped())
    }

    @Test
    fun partnerRole_displayFallbackOnlyTreatsTwoAsPartner2() {
        assertEquals(PartnerRole.PARTNER_2, PartnerRole.displayFallback("2"))
        assertEquals(PartnerRole.PARTNER_1, PartnerRole.displayFallback("B"))
        assertEquals(PartnerRole.PARTNER_1, PartnerRole.displayFallback(null))
    }

    @Test
    fun isPartner1_prefersExplicitIdsThenUidOrderThenStoredRole() {
        val space = CoupleSpace(partner1Id = "p1", partner2Id = "p2", userUids = listOf("p2", "p1"))
        assertTrue(space.isPartner1("p1", PartnerRole.PARTNER_2))
        assertFalse(space.isPartner1("p2", PartnerRole.PARTNER_1))

        val legacy = CoupleSpace(userUids = listOf("u1", "u2"))
        assertTrue(legacy.isPartner1("u1", PartnerRole.PARTNER_2))
        assertFalse(legacy.isPartner1("u2", PartnerRole.PARTNER_1))

        assertFalse(legacy.isPartner1("stranger", PartnerRole.PARTNER_2))
        assertFalse(legacy.isPartner1(null, PartnerRole.PARTNER_2))
        assertTrue((null as CoupleSpace?).isPartner1("u1", PartnerRole.PARTNER_1))
    }

    @Test
    fun roleOf_onlyResolvesBoundUids() {
        val space = CoupleSpace(partner1Id = "p1", partner2Id = "p2", userUids = listOf("p1", "p2"))
        assertEquals(PartnerRole.PARTNER_1, space.roleOf("p1"))
        assertEquals(PartnerRole.PARTNER_2, space.roleOf("p2"))
        assertNull(space.roleOf("stranger"))
        assertNull(space.roleOf(null))
        assertEquals(PartnerRole.PARTNER_2, CoupleSpace(userUids = listOf("u1", "u2")).roleOf("u2"))
    }

    @Test
    fun editableRole_usesFallbackOnlyWithoutMembers() {
        val bound = CoupleSpace(partner1Id = "p1", partner2Id = "p2", userUids = listOf("p1", "p2"))
        assertEquals(PartnerRole.PARTNER_2, bound.editableRole("p2", PartnerRole.PARTNER_1))
        assertNull(bound.editableRole("stranger", PartnerRole.PARTNER_1))
        assertNull(bound.editableRole(null, PartnerRole.PARTNER_1))

        val demo = CoupleSpace()
        assertEquals(PartnerRole.PARTNER_2, demo.editableRole(null, PartnerRole.PARTNER_2))
        assertNull(demo.editableRole(null, null))
    }

    @Test
    fun canSwapPartners_needsBothPartnersOrNone() {
        assertTrue(CoupleSpace().canSwapPartners)
        assertFalse(CoupleSpace(partner1Id = "p1", userUids = listOf("p1")).canSwapPartners)
        assertFalse(CoupleSpace(userUids = listOf("u1")).canSwapPartners)
        assertTrue(CoupleSpace(partner1Id = "p1", partner2Id = "p2", userUids = listOf("p1", "p2")).canSwapPartners)
        assertTrue(CoupleSpace(userUids = listOf("u1", "u2")).canSwapPartners)
    }

    @Test
    fun parseColorHexToCompose_fallsBackForInvalidValues() {
        assertEquals(Color(0xFF8FA89B), parseColorHexToCompose("#8FA89B"))
        assertEquals(Color(0xFF4ECDC4), parseColorHexToCompose("nope", "#4ECDC4"))
        assertEquals(Color(0xFFFF6B6B), parseColorHexToCompose(null))
        assertEquals(Color(0xFFE65D2E), parseColorHexToCompose("nope", "also-nope"))
    }
}
