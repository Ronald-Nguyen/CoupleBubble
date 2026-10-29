package com.aistudio.couplebubble.qxztrw.data.demo

import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * The simulated partner of the demo mode. Once per space and process, a while after the space is set up, the
 * partner adds a moment (a notification on this device) and then a checklist (the unread badge on the notes tab).
 */
class DemoPartner(
    private val coupleRepository: DemoCoupleRepository,
    private val notesRepository: DemoNotesRepository,
    private val momentDelayMillis: Long = MOMENT_DELAY_MILLIS,
    private val noteDelayMillis: Long = NOTE_DELAY_MILLIS
) {
    private val visitedSpaces = mutableSetOf<String>()

    suspend fun run() {
        coupleRepository.currentSpace
            .map { space -> space?.takeIf { it.isActive && it.isSetupComplete }?.id }
            .distinctUntilChanged()
            .filter { it != null && visitedSpaces.add(it) }
            .collect { spaceId -> actIn(spaceId ?: return@collect) }
    }

    private suspend fun actIn(spaceId: String) {
        delay(momentDelayMillis)
        if (!isStillIn(spaceId)) return
        coupleRepository.addPartnerMemory(spaceId, DemoSeed.partnerMoment(LocalDate.now()), DemoSeed.PHOTO_SEA)

        delay(noteDelayMillis)
        if (!isStillIn(spaceId)) return
        notesRepository.addPartnerNote(spaceId, DemoSeed.partnerNote(System.currentTimeMillis()))
    }

    private fun isStillIn(spaceId: String): Boolean = coupleRepository.currentSpace.value?.id == spaceId

    private companion object {
        const val MOMENT_DELAY_MILLIS = 45_000L
        const val NOTE_DELAY_MILLIS = 30_000L
    }
}
