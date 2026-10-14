package com.aistudio.couplebubble.qxztrw.domain.model

/** Every free-text input the app stores, with its limits on the trimmed text. A [minLength] of 0 makes it optional. */
enum class InputField(val minLength: Int, val maxLength: Int) {
    MEMORY_TITLE(minLength = 1, maxLength = 80),
    MEMORY_NOTE(minLength = 0, maxLength = 1000),
    NICKNAME(minLength = 2, maxLength = 30),
    MILESTONE_TITLE(minLength = 1, maxLength = 60),
    NOTE_TITLE(minLength = 0, maxLength = 120),
    LABEL_NAME(minLength = 1, maxLength = 30)
}

enum class InputError { EMPTY, TOO_SHORT, TOO_LONG }

/** Shared validation for dialogs and ViewModels, so a value the UI accepts is never refused later and vice versa. */
object InputRules {

    /** The character counter appears from this share of [InputField.maxLength] on. */
    private const val COUNTER_THRESHOLD = 0.8

    fun validate(field: InputField, value: String): InputError? {
        val trimmed = value.trim()
        return when {
            field.minLength > 0 && trimmed.isEmpty() -> InputError.EMPTY
            trimmed.length < field.minLength -> InputError.TOO_SHORT
            trimmed.length > field.maxLength -> InputError.TOO_LONG
            else -> null
        }
    }

    fun isValid(field: InputField, value: String): Boolean = validate(field, value) == null

    fun showsCounter(field: InputField, value: String): Boolean =
        value.length >= field.maxLength * COUNTER_THRESHOLD
}
