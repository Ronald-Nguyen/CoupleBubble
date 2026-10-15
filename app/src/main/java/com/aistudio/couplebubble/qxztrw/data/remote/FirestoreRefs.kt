package com.aistudio.couplebubble.qxztrw.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

internal fun FirebaseFirestore.space(coupleId: String): DocumentReference =
    collection(FirestoreSchema.SPACES).document(coupleId)

internal fun FirebaseFirestore.memories(coupleId: String): CollectionReference =
    space(coupleId).collection(FirestoreSchema.MEMORIES)

internal fun FirebaseFirestore.milestones(coupleId: String): CollectionReference =
    space(coupleId).collection(FirestoreSchema.MILESTONES)

internal fun FirebaseFirestore.notes(coupleId: String): CollectionReference =
    space(coupleId).collection(FirestoreSchema.NOTES)

internal fun FirebaseFirestore.noteLabels(coupleId: String): DocumentReference =
    space(coupleId).collection(FirestoreSchema.SETTINGS).document(FirestoreSchema.NOTE_LABELS_DOC)

internal fun FirebaseFirestore.reunion(coupleId: String): DocumentReference =
    space(coupleId).collection(FirestoreSchema.REUNION).document(FirestoreSchema.REUNION_DOC)

internal fun FirebaseFirestore.user(uid: String): DocumentReference = collection(FirestoreSchema.USERS).document(uid)

internal fun firestoreUnavailable(): Result<Nothing> = Result.failure(IllegalStateException("Firestore uninitialized"))

/**
 * Runs snapshot listeners off the main thread: mapping every document (some carry Base64 photos) would otherwise
 * stall the UI. One thread keeps the snapshots of a listener in order.
 */
internal val snapshotExecutor: java.util.concurrent.Executor = java.util.concurrent.Executors.newSingleThreadExecutor()
