package com.aistudio.couplebubble.qxztrw.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputRulesTest {

    @Test
    fun requiredFieldsRejectBlankText() {
        assertEquals(InputError.EMPTY, InputRules.validate(InputField.MEMORY_TITLE, "   "))
        assertEquals(InputError.EMPTY, InputRules.validate(InputField.MILESTONE_TITLE, ""))
        assertEquals(InputError.EMPTY, InputRules.validate(InputField.NICKNAME, " "))
        assertEquals(InputError.EMPTY, InputRules.validate(InputField.LABEL_NAME, ""))
    }

    @Test
    fun optionalFieldsAcceptBlankText() {
        assertNull(InputRules.validate(InputField.MEMORY_NOTE, ""))
        assertNull(InputRules.validate(InputField.NOTE_TITLE, "  "))
    }

    @Test
    fun nicknameNeedsTwoCharactersAfterTrimming() {
        assertEquals(InputError.TOO_SHORT, InputRules.validate(InputField.NICKNAME, " L "))
        assertNull(InputRules.validate(InputField.NICKNAME, " Li "))
    }

    @Test
    fun textAboveTheLimitIsTooLong() {
        val field = InputField.MEMORY_TITLE
        assertNull(InputRules.validate(field, "a".repeat(field.maxLength)))
        assertEquals(InputError.TOO_LONG, InputRules.validate(field, "a".repeat(field.maxLength + 1)))
        // Surrounding spaces are not counted, they are trimmed before saving
        assertTrue(InputRules.isValid(field, "  " + "a".repeat(field.maxLength) + "  "))
    }

    @Test
    fun counterAppearsFromEightyPercentOfTheLimit() {
        val field = InputField.NICKNAME // 30 characters
        assertFalse(InputRules.showsCounter(field, "a".repeat(23)))
        assertTrue(InputRules.showsCounter(field, "a".repeat(24)))
        assertTrue(InputRules.showsCounter(field, "a".repeat(40)))
    }

    @Test
    fun labelLimitMatchesNoteOrganizer() {
        assertEquals(InputField.LABEL_NAME.maxLength, NoteOrganizer.MAX_LABEL_NAME_LENGTH)
    }
}
