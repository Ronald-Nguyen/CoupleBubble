package com.aistudio.couplebubble.qxztrw.domain.session

import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.repository.CoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.ReunionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

/**
 * The active space's collections, each listened to once no matter how many screens and background tasks read
 * them (Spark plan: every listener costs reads). Keyed on the space id, so name or photo changes on the space
 * document don't re-open the listeners.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpaceContent(
    repository: CoupleRepository,
    reunionRepository: ReunionRepository,
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(5_000)
) {
    private val activeSpaceId: Flow<String?> = repository.currentSpace.map { it?.id }.distinctUntilChanged()

    /** Memories with the id of the space they belong to; `null` while no space is active. */
    val spaceMemories: SharedFlow<Pair<String, List<Memory>>?> = activeSpaceId.flatMapLatest { spaceId ->
        if (spaceId != null) repository.getMemories(spaceId).map { spaceId to it } else flowOf(null)
    }.shareIn(scope, started, replay = 1)

    val memories: Flow<List<Memory>> = spaceMemories.map { it?.second.orEmpty() }

    val customMilestones: SharedFlow<List<CustomMilestone>> = activeSpaceId.flatMapLatest { spaceId ->
        if (spaceId != null) repository.getCustomMilestones(spaceId) else flowOf(emptyList())
    }.shareIn(scope, started, replay = 1)

    /** The planned reunion with the id of the space it belongs to; `null` while nothing is planned. */
    val reunion: SharedFlow<Pair<String, Reunion>?> = activeSpaceId.flatMapLatest { spaceId ->
        if (spaceId != null) reunionRepository.getReunion(spaceId).map { reunion -> reunion?.let { spaceId to it } } else flowOf(null)
    }.shareIn(scope, started, replay = 1)
}
