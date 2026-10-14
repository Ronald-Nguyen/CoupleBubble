package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aistudio.couplebubble.qxztrw.R

/** TalkBack state of an expandable element: „ausgeklappt“ / „eingeklappt“. */
@Composable
fun expansionStateDescription(expanded: Boolean): String =
    stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)

/** Spells a code digit by digit („482-913“ → „4, 8, 2, 9, 1, 3“) so TalkBack never reads it as a number. */
fun spokenDigits(code: String): String = code.filter { it.isDigit() }.toList().joinToString(", ")

/** Whole minutes left, rounded up, so a countdown live region speaks at most once a minute. */
fun minutesLeft(totalSeconds: Int): Int = (totalSeconds.coerceAtLeast(0) + 59) / 60
