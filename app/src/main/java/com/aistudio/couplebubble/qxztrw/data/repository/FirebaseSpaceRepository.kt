package com.aistudio.couplebubble.qxztrw.data.repository

import com.aistudio.couplebubble.qxztrw.core.di.AppDispatchers
import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.aistudio.couplebubble.qxztrw.data.local.LocalImageStore
import com.aistudio.couplebubble.qxztrw.data.remote.FirebaseProvider
import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema
import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema.Space
import com.aistudio.couplebubble.qxztrw.data.remote.RemoteImageUploader
import com.aistudio.couplebubble.qxztrw.data.remote.firestoreUnavailable
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.Fields
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.MemoryMapper
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.SpaceMapper
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.UserMapper
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.fields
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.string
import com.aistudio.couplebubble.qxztrw.data.remote.memories
import com.aistudio.couplebubble.qxztrw.data.remote.space
import com.aistudio.couplebubble.qxztrw.data.remote.user
import com.aistudio.couplebubble.qxztrw.domain.error.InvalidPairingCodeException
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerNotConnectedException
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerSlotUnknownException
import com.aistudio.couplebubble.qxztrw.domain.error.SpaceFullException
import com.aistudio.couplebubble.qxztrw.domain.error.SpaceUnavailableException
import com.aistudio.couplebubble.qxztrw.domain.error.findCause
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.PairingCode
import com.aistudio.couplebubble.qxztrw.domain.model.PairingCodes
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.editableRole
import com.aistudio.couplebubble.qxztrw.domain.model.photoOf
import com.aistudio.couplebubble.qxztrw.domain.model.withPhoto
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.domain.repository.SpaceRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Sign-in, pairing, the user anchor `/users/{uid}` and the space document with its two partner slots. */
class FirebaseSpaceRepository(
    private val firebase: FirebaseProvider,
    private val uploader: RemoteImageUploader,
    private val localImageStore: LocalImageStore,
    private val dispatchers: AppDispatchers = AppDispatchers()
) : SpaceRepository {

    private companion object {
        const val DEMO_SPACE_ID = "demo_space"
        const val DEMO_SPACE_PREFIX = "demo_"
        const val STORAGE_PROFILES = "profiles"
        const val PAIRING_CODE_VALID_SECONDS = 900
        const val MAX_PAIRING_CODE_ATTEMPTS = 5

        // One transaction holds at most 500 writes: the space document plus every memory
        const val MAX_SWAP_MEMORIES = 499

        // Reads wait this long for the server before falling back to the offline cache
        val CACHE_FIRST_TIMEOUT = 2.seconds
        val SPACE_WRITE_TIMEOUT = 5.seconds
    }

    private val _currentSpace = MutableStateFlow<CoupleSpace?>(null)
    override val currentSpace: StateFlow<CoupleSpace?> = _currentSpace.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    override val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private var spaceListenerRegistration: ListenerRegistration? = null

    private val db: FirebaseFirestore? get() = firebase.firestore

    override val currentAuthUid: String? get() = firebase.currentUid

    override suspend fun ensureSignedIn(): String? = firebase.ensureSignedIn()

    private fun listenToSpaceChanges(spaceId: String) {
        spaceListenerRegistration?.remove()
        val firestore = db ?: return

        spaceListenerRegistration = firestore.space(spaceId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            // An empty offline cache reports the space as missing; only the server may say it is gone
            if (!snapshot.exists() && snapshot.metadata.isFromCache) return@addSnapshotListener
            _currentSpace.value = spaceFromSnapshot(snapshot)
        }
    }

    /** Parses a space document; returns null when it no longer exists or was disconnected. */
    private fun spaceFromSnapshot(snapshot: DocumentSnapshot): CoupleSpace? =
        if (snapshot.exists()) SpaceMapper.fromFields(snapshot.id, snapshot.fields()) else null

    private fun profileOf(user: FirebaseUser, coupleId: String?): UserProfile =
        UserProfile(uid = user.uid, email = user.email, displayName = user.displayName, coupleId = coupleId)

    override suspend fun updatePartnerNames(
        coupleId: String,
        partner1Name: String,
        partner2Name: String
    ): Result<Unit> {
        val firestore = db ?: return firestoreUnavailable()
        return try {
            // The offline cache applies the write at once; do not block on the server ack
            withTimeoutOrNull(SPACE_WRITE_TIMEOUT) { firestore.space(coupleId).update(
                SpaceMapper.names(partner1Name, partner2Name) + (Space.IS_SETUP_COMPLETE to true)
            ).await() }
            _currentSpace.value = _currentSpace.value?.copy(
                partner1Name = partner1Name,
                partner2Name = partner2Name,
                isSetupComplete = true
            )
            Result.success(Unit)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun updateSpaceDetails(
        coupleId: String,
        partner1Name: String,
        partner2Name: String,
        anniversary: LocalDate
    ): Result<Unit> {
        val firestore = db
        val epochMillis = anniversary.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (firestore != null) {
            try {
                val updateData = SpaceMapper.names(partner1Name, partner2Name) +
                    SpaceMapper.anniversary(anniversary.year, anniversary.monthValue, anniversary.dayOfMonth) +
                    mapOf(
                        Space.ANNIVERSARY_EPOCH_MILLIS to epochMillis,
                        Space.IS_SETUP_COMPLETE to true,
                        Space.UPDATED_AT to System.currentTimeMillis()
                    )
                firestore.space(coupleId).set(updateData, SetOptions.merge()).await()

                if (firebase.auth?.currentUser != null) {
                    linkCurrentUserToSpace(coupleId)
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                // Ignore network error and update locally
            }
        }

        _currentSpace.value = _currentSpace.value?.copy(
            partner1Name = partner1Name,
            partner2Name = partner2Name,
            anniversaryYear = anniversary.year,
            anniversaryMonth = anniversary.monthValue,
            anniversaryDay = anniversary.dayOfMonth,
            anniversaryEpochMillis = epochMillis,
            isSetupComplete = true
        )
        return Result.success(Unit)
    }

    override suspend fun listenToPairingCode(code: String) {
        if (!PairingCodes.isValid(code)) return
        val cleanCode = PairingCodes.normalize(code)
        try {
            val firestore = db ?: return
            val spaceId = SpaceMapper.spaceIdForCode(cleanCode)
            val spaceDocRef = firestore.space(spaceId)
            val snapshot = withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { spaceDocRef.get().await() }
            if (snapshot == null || !snapshot.exists()) {
                withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                    spaceDocRef.set(SpaceMapper.pendingSpace(spaceId, cleanCode, System.currentTimeMillis())).await()
                }
            }
            listenToSpaceChanges(spaceId)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            // Ignore if Firebase uninitialized
        }
    }

    override suspend fun generateNewPairingCode(): PairingCode {
        // A code whose space already exists would make this device listen to someone else's space
        var generatedCode = PairingCodes.generate()
        for (attempt in 1 until MAX_PAIRING_CODE_ATTEMPTS) {
            if (!isPairingCodeTaken(generatedCode)) break
            generatedCode = PairingCodes.generate()
        }

        listenToPairingCode(generatedCode)

        return PairingCode(code = generatedCode, totalValidSeconds = PAIRING_CODE_VALID_SECONDS)
    }

    /** Unknown (offline, no Firebase) counts as free; the server check is only there to avoid collisions. */
    private suspend fun isPairingCodeTaken(code: String): Boolean {
        val firestore = db ?: return false
        return try {
            withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                firestore.space(SpaceMapper.spaceIdForCode(PairingCodes.normalize(code))).get(Source.SERVER).await().exists()
            } ?: false
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            false
        }
    }

    override suspend fun connectWithCode(code: String): Result<CoupleSpace> {
        if (!PairingCodes.isValid(code)) return Result.failure(InvalidPairingCodeException())
        val cleanCode = PairingCodes.normalize(code)

        try {
            val firestore = db
            if (firestore != null) {
                var snapshot = withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                    firestore.space(SpaceMapper.spaceIdForCode(cleanCode)).get().await()
                }

                if (snapshot == null || !snapshot.exists()) {
                    val query = withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                        firestore.collection(FirestoreSchema.SPACES)
                            .whereEqualTo(Space.PAIRING_CODE, cleanCode)
                            .limit(1)
                            .get()
                            .await()
                    }
                    if (query != null && !query.isEmpty) {
                        snapshot = query.documents.first()
                    }
                }

                val existing = snapshot?.takeIf { it.exists() }
                // A snapshot that does not exist carries no fields, so all values below fall back to the defaults
                val fields: Fields = existing?.fields().orEmpty()
                if (SpaceMapper.isJoinRefused(fields, firebase.currentUid)) {
                    return Result.failure(SpaceFullException())
                }

                val docId = existing?.id ?: SpaceMapper.spaceIdForCode(cleanCode)
                val joined = SpaceMapper.joined(docId, cleanCode, fields, System.currentTimeMillis())
                withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                    firestore.space(docId).set(joined, SetOptions.merge()).await()
                }

                // The same mapping as the snapshot listener, applied to the stored fields plus this write
                val pairedSpace = SpaceMapper.fromFields(docId, fields + joined)
                    ?: return Result.failure(SpaceUnavailableException())
                _currentSpace.value = pairedSpace
                listenToSpaceChanges(pairedSpace.id)

                val currentUser = firebase.auth?.currentUser
                if (currentUser != null) {
                    claimPartnerSlot(pairedSpace.id, currentUser.uid, preferPartner2 = true)
                    linkCurrentUserToSpace(pairedSpace.id)
                }

                return Result.success(pairedSpace)
            }
        } catch (e: IllegalStateException) {
            // Kotlin's CancellationException is an IllegalStateException too
            e.rethrowIfCancellation()
            return Result.failure(e)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            // Fallback if network or Firebase unavailable
        }

        return Result.failure(SpaceUnavailableException())
    }

    override suspend fun restoreSession(coupleId: String): Result<CoupleSpace> {
        val firestore = db ?: return Result.failure(SpaceUnavailableException())
        val docRef = firestore.space(coupleId)
        // A slow start must not fall back to placeholder names: the offline cache holds the last known space
        val doc = try {
            withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { docRef.get().await() }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            null
        } ?: try {
            docRef.get(Source.CACHE).await()
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            null
        }

        if (doc == null || (!doc.exists() && doc.metadata.isFromCache)) {
            // Not loaded yet; the listener fills in the space as soon as Firestore answers
            listenToSpaceChanges(coupleId)
            return Result.failure(SpaceUnavailableException())
        }

        val space = spaceFromSnapshot(doc)
        if (space == null) {
            _currentSpace.value = null
            return Result.failure(IllegalStateException("Couple space is inactive"))
        }
        _currentSpace.value = space
        listenToSpaceChanges(space.id)
        firebase.auth?.currentUser?.let { user ->
            if (!space.id.startsWith(DEMO_SPACE_PREFIX) && user.uid != space.partner1Id && user.uid != space.partner2Id) {
                claimPartnerSlot(space.id, user.uid, preferPartner2 = false)
            }
            if (_currentUserProfile.value == null) {
                _currentUserProfile.value = profileOf(user, space.id)
            }
        }
        return Result.success(space)
    }

    override suspend fun restoreSessionForUser(uid: String): Result<CoupleSpace?> {
        val firestore = db ?: return Result.success(null)
        try {
            val userDoc = withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { firestore.user(uid).get().await() }
            if (userDoc != null && userDoc.exists()) {
                val profile = UserMapper.fromFields(uid, userDoc.fields())
                _currentUserProfile.value = profile
                val coupleId = profile.coupleId

                if (!coupleId.isNullOrBlank()) {
                    return Result.success(restoreSession(coupleId).getOrNull())
                }
            }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            // Ignore error on restore
        }
        return Result.success(null)
    }

    override suspend fun disconnect() {
        spaceListenerRegistration?.remove()
        spaceListenerRegistration = null
        _currentSpace.value = null
    }

    override suspend fun disconnectCouple(coupleId: String): Result<Unit> {
        try {
            // Offline the write only completes with the server ack; the local disconnect below must not wait for it
            withTimeoutOrNull(SPACE_WRITE_TIMEOUT) {
                db?.space(coupleId)?.update(
                    mapOf(
                        Space.IS_ACTIVE to false,
                        Space.DISCONNECTED_AT to System.currentTimeMillis()
                    )
                )?.await()
            }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            // Ignore if Firebase uninitialized
        }
        disconnect()
        return Result.success(Unit)
    }

    /**
     * Opens a demo space owned by the current user alone (`demo_{uid}`). A single shared demo document would fill
     * up with foreign members or be left inactive by another device, and its listener would then throw this
     * device straight back to pairing.
     */
    override suspend fun openDemoSpace(): CoupleSpace {
        val uid = try {
            ensureSignedIn()
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            null
        }
        val demoSpace = CoupleSpace(
            id = if (uid != null) "${DEMO_SPACE_PREFIX}$uid" else DEMO_SPACE_ID,
            isSetupComplete = true,
            isActive = true,
            partner1Id = uid,
            userUids = listOfNotNull(uid)
        )

        try {
            withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                db?.space(demoSpace.id)?.set(
                    SpaceMapper.names(demoSpace.partner1Name, demoSpace.partner2Name) +
                        SpaceMapper.anniversary(demoSpace.anniversaryYear, demoSpace.anniversaryMonth, demoSpace.anniversaryDay) +
                        mapOf(
                            Space.ID to demoSpace.id,
                            Space.IS_SETUP_COMPLETE to true,
                            Space.IS_ACTIVE to true
                        ) +
                        (if (uid != null) mapOf(Space.PARTNER_1_ID to uid, Space.USER_UIDS to listOf(uid)) else emptyMap()),
                    SetOptions.merge()
                )?.await()
            }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            // Ignore if Firebase uninitialized
        }

        _currentSpace.value = demoSpace
        listenToSpaceChanges(demoSpace.id)
        return demoSpace
    }

    override suspend fun signInWithGoogleUser(
        uid: String,
        email: String?,
        displayName: String?,
        previousUid: String?
    ): Result<UserProfile> {
        val firestore = db
        var coupleId: String? = _currentSpace.value?.id

        if (firestore != null) {
            try {
                val userDocRef = firestore.user(uid)
                val snapshot = withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { userDocRef.get().await() }

                if (snapshot != null && snapshot.exists()) {
                    val existingCoupleId = snapshot.fields().string(FirestoreSchema.User.COUPLE_ID)
                    if (!existingCoupleId.isNullOrBlank()) {
                        coupleId = existingCoupleId
                        restoreSession(existingCoupleId)
                    }
                }

                userDocRef.set(
                    UserMapper.anchor(uid, email, displayName, coupleId, System.currentTimeMillis()),
                    SetOptions.merge()
                ).await()

                if (!coupleId.isNullOrBlank() && !coupleId.startsWith(DEMO_SPACE_PREFIX)) {
                    claimPartnerSlot(coupleId, uid, preferPartner2 = false, replaceUid = previousUid)
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                // Ignore network error and keep local state
            }
        }

        val profile = UserProfile(uid = uid, email = email, displayName = displayName, coupleId = coupleId)
        _currentUserProfile.value = profile
        return Result.success(profile)
    }

    override suspend fun linkCurrentUserToSpace(coupleId: String): Result<Unit> {
        val user = firebase.auth?.currentUser ?: return Result.success(Unit)
        val firestore = db ?: return Result.success(Unit)

        try {
            firestore.user(user.uid).set(
                UserMapper.anchor(user.uid, user.email, user.displayName, coupleId, System.currentTimeMillis()),
                SetOptions.merge()
            ).await()

            claimPartnerSlot(coupleId, user.uid, preferPartner2 = false)

            _currentUserProfile.value = profileOf(user, coupleId)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            // Ignore error if network fails
        }
        return Result.success(Unit)
    }

    private fun profilePhotoFields(role: PartnerRole, url: String): Map<String, Any> =
        SpaceMapper.photo(role, url) + (Space.UPDATED_AT to System.currentTimeMillis())

    override suspend fun uploadProfilePhoto(
        coupleId: String,
        fallbackRole: PartnerRole?,
        imageBytes: ByteArray
    ): Result<String> = withContext(dispatchers.io) {
        val uid = currentAuthUid
        val previousSpace = _currentSpace.value
        val expectedRole = previousSpace?.editableRole(uid, fallbackRole)
            ?: return@withContext Result.failure(PartnerSlotUnknownException())
        val previousUrl = previousSpace.photoOf(expectedRole)

        // 1. Save locally for immediate display and offline resilience
        val localUrl = localImageStore.saveProfilePhoto(coupleId, uid, imageBytes)
        if (localUrl != null) {
            _currentSpace.value = _currentSpace.value?.withPhoto(expectedRole, localUrl)
        }

        // 2. Prefer a Storage URL; the Base64 data URI keeps partner devices in sync if the upload fails
        val syncUrl = uploader.uploadOrDataUri(coupleId, STORAGE_PROFILES, "profile", imageBytes)
        val firestore = db
        if (syncUrl == null || firestore == null) {
            return@withContext localUrl?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Profile photo could not be saved"))
        }

        // 3. The slot is resolved again from the server copy, so a role swap during the upload cannot make
        // the photo land in the partner's slot.
        val written = try {
            withTimeoutOrNull(SPACE_WRITE_TIMEOUT) {
                writeOwnProfilePhoto(firestore, coupleId, uid, fallbackRole, syncUrl)
            }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            if (e.findCause<PartnerSlotUnknownException>() != null) {
                _currentSpace.value = _currentSpace.value?.withPhoto(expectedRole, previousUrl)
                return@withContext Result.failure(PartnerSlotUnknownException())
            }
            null
        }

        if (written != null) {
            val (role, replacedUrl) = written
            _currentSpace.value?.let { current ->
                // Undo the optimistic local photo if the slot changed meanwhile; the listener delivers the rest
                val restored = if (role != expectedRole && current.photoOf(expectedRole) == localUrl) {
                    current.withPhoto(expectedRole, previousUrl)
                } else {
                    current
                }
                _currentSpace.value = restored.withPhoto(role, syncUrl)
            }
            val partnerUrl = _currentSpace.value?.photoOf(role.swapped())
            if (replacedUrl != syncUrl && replacedUrl != partnerUrl) {
                uploader.delete(replacedUrl)
            }
        } else {
            // Offline: transactions need the server; queue a merge into the slot known locally right now
            val role = _currentSpace.value?.editableRole(uid, fallbackRole) ?: expectedRole
            try {
                firestore.space(coupleId).set(profilePhotoFields(role, syncUrl), SetOptions.merge())
                _currentSpace.value = _currentSpace.value?.withPhoto(role, syncUrl)
            } catch (_: Exception) {
                // Firestore sync delayed; local state preserved
            }
        }
        Result.success(syncUrl)
    }

    /** Writes [url] into the slot [uid] holds on the server; returns that slot and the URL it replaced. */
    private suspend fun writeOwnProfilePhoto(
        firestore: FirebaseFirestore,
        coupleId: String,
        uid: String?,
        fallbackRole: PartnerRole?,
        url: String
    ): Pair<PartnerRole, String?> {
        val spaceRef = firestore.space(coupleId)
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(spaceRef)
            if (!snapshot.exists()) throw PartnerSlotUnknownException()
            val fields = snapshot.fields()
            val role = SpaceMapper.members(fields).editableRole(uid, fallbackRole) ?: throw PartnerSlotUnknownException()
            val replaced = SpaceMapper.photoOf(fields, role)
            transaction.set(spaceRef, profilePhotoFields(role, url), SetOptions.merge())
            role to replaced
        }.await()
    }

    override suspend fun updatePartnerColor(
        coupleId: String,
        isPartner1: Boolean,
        colorHex: String
    ): Result<Unit> {
        val firestore = db ?: return firestoreUnavailable()
        return try {
            withTimeoutOrNull(SPACE_WRITE_TIMEOUT) {
                firestore.space(coupleId).set(
                    SpaceMapper.color(isPartner1, colorHex) + (Space.UPDATED_AT to System.currentTimeMillis()),
                    SetOptions.merge()
                ).await()
            }

            _currentSpace.value = if (isPartner1) {
                _currentSpace.value?.copy(partner1ColorHex = colorHex)
            } else {
                _currentSpace.value?.copy(partner2ColorHex = colorHex)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun ensurePartnerSlot(coupleId: String, preferPartner2: Boolean) {
        val uid = currentAuthUid ?: return
        claimPartnerSlot(coupleId, uid, preferPartner2)
    }

    /**
     * Binds [uid] to an explicit partner slot (`partner1Id` / `partner2Id`) and keeps `userUids` ordered
     * as [partner1, partner2]. Roles are decided by these IDs, not by array order, so `arrayUnion`
     * appends can no longer flip who is who. Spaces from before the IDs existed are backfilled from
     * the old `userUids` order. [replaceUid] swaps an old UID (e.g. anonymous before Google sign-in) for [uid].
     */
    private suspend fun claimPartnerSlot(
        coupleId: String,
        uid: String,
        preferPartner2: Boolean,
        replaceUid: String? = null
    ) {
        val firestore = db ?: return
        val spaceRef = firestore.space(coupleId)
        try {
            withTimeoutOrNull(SPACE_WRITE_TIMEOUT) {
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(spaceRef)
                    if (!snapshot.exists()) return@runTransaction
                    val fields = snapshot.fields()
                    val storedUids = SpaceMapper.userUids(fields)
                    val storedP1 = fields.string(Space.PARTNER_1_ID)
                    val storedP2 = fields.string(Space.PARTNER_2_ID)
                    var (p1, p2) = SpaceMapper.boundPartnerIds(fields)
                    if (replaceUid != null && replaceUid != uid) {
                        if (p1 == replaceUid) p1 = uid
                        if (p2 == replaceUid) p2 = uid
                    }
                    if (uid != p1 && uid != p2) {
                        if (preferPartner2) {
                            if (p2 == null) p2 = uid else if (p1 == null) p1 = uid
                        } else {
                            if (p1 == null) p1 = uid else if (p2 == null) p2 = uid
                        }
                    }
                    if (uid != p1 && uid != p2) return@runTransaction // Room is full

                    val newUids = listOfNotNull(p1, p2).distinct()
                    if (p1 != storedP1 || p2 != storedP2 || newUids != storedUids) {
                        transaction.update(
                            spaceRef,
                            mapOf(
                                Space.PARTNER_1_ID to (p1 ?: FieldValue.delete()),
                                Space.PARTNER_2_ID to (p2 ?: FieldValue.delete()),
                                Space.USER_UIDS to newUids
                            )
                        )
                    }
                }.await()
            }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            // Offline: transactions need the server; retried on next restore/sign-in
        }
    }

    override suspend fun swapPartners(coupleId: String): Result<Unit> {
        val firestore = db ?: return firestoreUnavailable()

        return try {
            val spaceRef = firestore.space(coupleId)
            val memoryRefs = firestore.memories(coupleId).get().await().documents.map { it.reference }
            if (memoryRefs.size > MAX_SWAP_MEMORIES) {
                return Result.failure(IllegalStateException("Too many memories to swap atomically"))
            }

            // Space and every memory swap in one transaction: either everything changes or nothing does.
            val swapped = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(spaceRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Space document does not exist")
                }
                val memorySnapshots = memoryRefs.map { transaction.get(it) }
                val fields = snapshot.fields()

                val (p1Id, p2Id) = SpaceMapper.boundPartnerIds(fields)
                // With only one known member the swap would move their data to slot 2 while they stay partner 1
                if ((p1Id == null) != (p2Id == null)) {
                    throw PartnerNotConnectedException()
                }

                // Missing values must be deleted, not skipped, or the old photo would stay in both slots
                val spaceUpdate = mutableMapOf<String, Any>()
                spaceUpdate += SpaceMapper.names(SpaceMapper.partner2Name(fields), SpaceMapper.partner1Name(fields))
                spaceUpdate += SpaceMapper.photo(PartnerRole.PARTNER_1, SpaceMapper.partner2PhotoUrl(fields) ?: FieldValue.delete())
                spaceUpdate += SpaceMapper.photo(PartnerRole.PARTNER_2, SpaceMapper.partner1PhotoUrl(fields) ?: FieldValue.delete())
                spaceUpdate += SpaceMapper.color(isPartner1 = true, colorHex = SpaceMapper.partner2ColorHex(fields))
                spaceUpdate += SpaceMapper.color(isPartner1 = false, colorHex = SpaceMapper.partner1ColorHex(fields))
                spaceUpdate[Space.UPDATED_AT] = System.currentTimeMillis()
                if (p1Id != null && p2Id != null) {
                    spaceUpdate[Space.PARTNER_1_ID] = p2Id
                    spaceUpdate[Space.PARTNER_2_ID] = p1Id
                    spaceUpdate[Space.USER_UIDS] = listOf(p2Id, p1Id)
                }
                transaction.update(spaceRef, spaceUpdate)

                for (memory in memorySnapshots) {
                    if (!memory.exists()) continue
                    val (slot1, slot2) = MemoryMapper.storedSlots(memory.fields())
                    transaction.update(
                        memory.reference,
                        mapOf(
                            FirestoreSchema.Memory.PARTNER_1_IMAGE to (slot2 ?: FieldValue.delete()),
                            FirestoreSchema.Memory.PARTNER_2_IMAGE to (slot1 ?: FieldValue.delete()),
                            FirestoreSchema.Memory.LEGACY_IMAGE_URL to FieldValue.delete()
                        )
                    )
                }
                listOfNotNull(p2Id, p1Id)
            }.await()

            _currentSpace.value?.let { current ->
                _currentSpace.value = current.copy(
                    partner1Name = current.partner2Name,
                    partner2Name = current.partner1Name,
                    partner1PhotoUrl = current.partner2PhotoUrl,
                    partner2PhotoUrl = current.partner1PhotoUrl,
                    partner1ColorHex = current.partner2ColorHex,
                    partner2ColorHex = current.partner1ColorHex,
                    partner1Id = swapped.getOrNull(0),
                    partner2Id = swapped.getOrNull(1),
                    userUids = swapped.ifEmpty { current.userUids }
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e.findCause<PartnerNotConnectedException>() ?: e)
        }
    }

    override suspend fun signOutUser() {
        // The UID is the space membership: signing it out would lock this device out of a full space
        if (_currentSpace.value != null) return
        try {
            firebase.auth?.signOut()
        } catch (_: Exception) {
            // Ignore
        }
        _currentUserProfile.value = null
        ensureSignedIn()
    }
}
