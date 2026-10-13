package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.GermanShortDate
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionCity
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlaceSource
import com.aistudio.couplebubble.qxztrw.domain.model.source
import com.aistudio.couplebubble.qxztrw.domain.repository.ReunionCityCatalog
import com.aistudio.couplebubble.qxztrw.ui.components.MemoryDatePickerDialog
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.screens.home.ReunionUiState
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The city search, provided by `MainActivity`; the empty default keeps previews working without the city list. */
val LocalReunionCityCatalog = staticCompositionLocalOf<ReunionCityCatalog> {
    object : ReunionCityCatalog {
        override suspend fun suggestions(query: String, limit: Int): List<ReunionCity> = emptyList()
        override suspend fun find(query: String): ReunionCity? = null
    }
}

private enum class ReunionDateField { START, END }

/**
 * Plans or edits the reunion. The switch only covers the user's own location; the partner's row just says what
 * they share. Location access is asked for when the switch goes on; a refusal flips it back and shows the city field.
 * A typed city is only saved once it matches the bundled list, so the distance can always be computed.
 */
@Composable
internal fun ReunionDialog(
    current: ReunionUiState?,
    partnerName: String,
    ownColor: Color,
    partnerColor: Color,
    onDismiss: () -> Unit,
    onSave: (start: LocalDate, end: LocalDate, shareLocation: Boolean, city: ReunionCity?) -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val catalog = LocalReunionCityCatalog.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    var startDate by remember { mutableStateOf(current?.reunion?.startDate ?: today.plusWeeks(2)) }
    var endDate by remember { mutableStateOf(current?.reunion?.endDate ?: today.plusWeeks(2).plusDays(2)) }
    var pickingDate by remember { mutableStateOf<ReunionDateField?>(null) }
    var shareLocation by remember { mutableStateOf(current?.ownPlace?.sharesLocation == true) }
    var permissionDenied by remember { mutableStateOf(false) }
    var city by remember { mutableStateOf(current?.ownPlace?.city.orEmpty()) }
    var cityNotFound by remember { mutableStateOf(false) }

    // Suggestions wait briefly after each keystroke, so the list isn't searched on every letter
    val suggestions by produceState(initialValue = emptyList<ReunionCity>(), city, shareLocation) {
        val query = city.trim()
        if (shareLocation || query.isEmpty()) {
            value = emptyList()
            return@produceState
        }
        delay(SUGGESTION_DELAY_MILLIS)
        value = catalog.suggestions(query)
    }
    val showSuggestions = suggestions.isNotEmpty() && suggestions.none { it.label == city.trim() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        shareLocation = granted
        permissionDenied = !granted
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.reunion_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField(
                        label = stringResource(R.string.reunion_from),
                        date = startDate,
                        onClick = { pickingDate = ReunionDateField.START },
                        modifier = Modifier.weight(1f)
                    )
                    DateField(
                        label = stringResource(R.string.reunion_to),
                        date = endDate,
                        onClick = { pickingDate = ReunionDateField.END },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Text(
                    text = stringResource(R.string.reunion_distance_label).uppercase(),
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reunion_share_location")
                        .toggleable(
                            value = shareLocation,
                            role = Role.Switch,
                            onValueChange = { enabled ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                permissionDenied = false
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                when {
                                    !enabled -> shareLocation = false
                                    granted -> shareLocation = true
                                    else -> permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    ColorDot(ownColor)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(
                            text = stringResource(R.string.reunion_share_location),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                if (permissionDenied) R.string.reunion_location_denied else R.string.reunion_share_location_hint,
                                partnerName
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (permissionDenied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = shareLocation,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.secondary)
                    )
                }

                if (!shareLocation) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = {
                            city = it
                            cityNotFound = false
                        },
                        label = {
                            Text(stringResource(if (permissionDenied) R.string.reunion_city_instead_label else R.string.reunion_city_label))
                        },
                        singleLine = true,
                        isError = cityNotFound,
                        supportingText = {
                            Text(
                                text = stringResource(
                                    if (cityNotFound) R.string.reunion_city_unknown else R.string.reunion_city_attribution
                                )
                            )
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reunion_city_field")
                    )
                    if (showSuggestions) {
                        Column {
                            suggestions.forEach { suggestion ->
                                TextButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        city = suggestion.label
                                        cityNotFound = false
                                    },
                                    modifier = Modifier.testTag("reunion_city_suggestion")
                                ) {
                                    Text(suggestion.label)
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    ColorDot(partnerColor)
                    Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                        Text(
                            text = partnerName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = partnerStatus(current?.partnerPlace, current?.nowMillis ?: 0L),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (current != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.testTag("reunion_delete")) {
                        Text(text = stringResource(R.string.reunion_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (shareLocation || city.isBlank()) {
                        onSave(startDate, endDate, shareLocation, null)
                    } else {
                        scope.launch {
                            val found = catalog.find(city)
                            if (found == null) cityNotFound = true else onSave(startDate, endDate, false, found)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.testTag("reunion_save")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )

    pickingDate?.let { field ->
        MemoryDatePickerDialog(
            initialDate = if (field == ReunionDateField.START) startDate else endDate,
            onDismiss = { pickingDate = null },
            onDateSelected = { picked ->
                if (field == ReunionDateField.START) {
                    // Keep the length of the stay when the start moves
                    val length = ChronoUnit.DAYS.between(startDate, endDate)
                    startDate = picked
                    if (endDate.isBefore(picked)) endDate = picked.plusDays(length.coerceAtLeast(0))
                } else {
                    endDate = if (picked.isBefore(startDate)) startDate else picked
                }
            }
        )
    }
}

@Composable
private fun DateField(label: String, date: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = date.format(GermanShortDate),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = label, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun partnerStatus(place: ReunionPlace?, nowMillis: Long): String = when (val source = place.source(nowMillis)) {
    is ReunionPlaceSource.Location -> stringResource(R.string.reunion_partner_shares)
    is ReunionPlaceSource.City -> stringResource(R.string.reunion_partner_city, source.name)
    ReunionPlaceSource.None -> stringResource(R.string.reunion_partner_nothing)
}

private const val SUGGESTION_DELAY_MILLIS = 200L

@Preview(name = "ReunionDialog Light", showBackground = true)
@Composable
private fun ReunionDialogLightPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        ReunionDialog(
            current = PreviewData.reunion,
            partnerName = PreviewData.space.partner2Name,
            ownColor = Color(0xFFE65D2E),
            partnerColor = Color(0xFF5B8DB8),
            onDismiss = {},
            onSave = { _, _, _, _ -> },
            onDelete = {}
        )
    }
}

@Preview(name = "ReunionDialog Dark", showBackground = true)
@Composable
private fun ReunionDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        ReunionDialog(
            current = null,
            partnerName = PreviewData.space.partner2Name,
            ownColor = Color(0xFFE65D2E),
            partnerColor = Color(0xFF5B8DB8),
            onDismiss = {},
            onSave = { _, _, _, _ -> },
            onDelete = {}
        )
    }
}
