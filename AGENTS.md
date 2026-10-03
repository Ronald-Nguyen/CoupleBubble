# AGENTS.md – Operational Guidelines & Architecture Standards

This document defines operational rules, architectural constraints, and quality standards for AI agents working on the **CoupleBubble** codebase.

---

## 1. Core Operating Directives

### Documentation Synchronization
- **Mandatory Update Rule:** Whenever an architectural decision is made, a new screen or model is added, or the tech stack/dependencies change, both `README.md` and `AGENTS.md` must be updated within the same task or commit.
- Never leave discrepancies between the documented architecture and the actual codebase.

### Scope Control & Minimal Blast Radius
- Modify only the files strictly required to accomplish the specific prompt or task.
- Do not reformat untouched files or reorganize directory structures without explicit instruction.
- Remove all dead code, temporary debug variables, and unused logging statements before marking a task complete.

### Dependency Governance
- All dependencies must be managed strictly inside `gradle/libs.versions.toml` (Gradle Version Catalog).
- Never hardcode library versions inside `app/build.gradle.kts` or module-level build scripts.
- Do not introduce new third-party libraries without explicit user confirmation.

### Committing
- **Commit Without Asking:** Once `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass, commit the task's changes right away without asking for confirmation, using the commit message given in the task.
- **One Commit, No Splitting:** If uncommitted changes from earlier tasks are still in the working tree, commit everything together (`git add .`) instead of splitting it into separate commits.

---

## 2. UI/UX & Design Standards ("Anti-AI-Slop")

The UI must feel human, intimate, tactile, and custom-tailored for couples. Generic AI SaaS templates and clichés are strictly prohibited.

### Prohibited UI Patterns
- **No Generic Purple/Indigo Gradients:** Do NOT use `#6366f1`, `#8b5cf6`, or neon glow borders.
- **No Gimmicky Decorative Icons:** No sparkle icons (`✨`), magic wands (`🪄`), or heart badges enclosed in random floating circular backgrounds.
- **No Generic AI Marketing Copy:** Avoid clichés like "AI-driven love" or "Empower your relationship". Use authentic, warm German microcopy.
- **No Excessive Card Nesting:** Maximum one card layer. Structure inner content via whitespace, typography scale, subtle dividers, and tonal surface shifts.
- **No Automatic Center-Alignment:** Use natural, left-aligned editorial layouts with intentional white space.

### Mandatory Design Tokens & Styling Rules
- **Color Palette (Material 3):**
  - **Primary:** Deep Navy Blue (`#0F2137` / Dark: `#97CBFF`)
  - **Secondary / Accent:** Sunset Orange / Terracotta (`#E65D2E` / Dark: `#FF8A65`)
  - **Surfaces:** Deep Navy-tinted containers (`#162B48`). Never use pitch-black OLED black (`#000000`) for cards or dialogs.
- **Depth & Borders:** Use crisp 1px borders (`BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))`) and tonal elevation rather than heavy, blurred ambient shadows.
- **Micro-Interactions & Haptics:** Apply `LocalHapticFeedback.current` on key touchpoints (copying pairing codes, milestone triggers, date submissions, emotional check-ins). Primary actions use `HapticFeedbackType.LongPress`, fired only once the action is confirmed: successful gallery downloads, copying the pairing code (only if the clipboard service accepted it), saving a new or edited moment, swapping partner roles. Light selection changes use `HapticFeedbackType.TextHandleMove` (pairing tab switch, accent-color palette pick, date changes in `MemoryDatePickerDialog`), and only when the selection actually changes.
- **Physics-Based Motion:** Use `spring()` animations (e.g., `spring(stiffness = Spring.StiffnessLow)`) for scaling and state transitions. Avoid linear animations.
- **Typographic Contrast:** Hero counter numbers must be large (`48.sp` to `56.sp`, `FontWeight.ExtraBold`), paired with small, tracked uppercase labels (`12.sp`, `letterSpacing = 1.5.sp`).

---

## 3. Localization & Strings

- **Zero Hardcoded User-Facing Strings:** All display texts, button labels, content descriptions, and dialog messages must reside in `app/src/main/res/values/strings.xml`.
- **Tone:** UI strings must be written in natural, warm, personal German ("Du" form, empathetic, clear).

---

## 4. Tech Stack & Architecture

- **Platform:** Native Android (Kotlin DSL, `minSdk = 26`, `targetSdk = 34` or latest)
- **UI Framework:** Jetpack Compose with Material 3 (`androidx.compose.material3`)
- **Homescreen Widget:** Jetpack Glance with Material 3 (`androidx.glance:glance-appwidget`, `androidx.glance:glance-material3`)
- **Architecture:** MVVM with Unidirectional Data Flow (UDF)
- **State Handling:** `StateFlow` backed by immutable UI state objects (`sealed interface` / `data class`)
- **Local Persistence:** Jetpack DataStore Preferences (`androidx.datastore:datastore-preferences`)
- **Image Loading & Handling:** Coil for Compose (`io.coil-kt:coil-compose`) with custom components registered in `CoupleBubbleApplication` (`ImageLoaderFactory`): `Base64Mapper` (maps `String` → `Base64Image`), `Base64Keyer` (generates memory cache keys `"b64:${length}:${hashCode}"` for zero redundant decoding), and `Base64Fetcher` (decodes `Base64Image` directly into `BitmapDrawable` via `DrawableResult` to ensure zero stream exhaustion issues in Coil's `BitmapFactoryDecoder`). Because Coil executes mappers before fetchers, `Base64Mapper` runs ahead of the built-in `StringMapper` to intercept Base64 data and raw payloads before Coil can convert them into unhandled `android.net.Uri` instances. Handles remote `https://` URLs, local `file://` / `content://` URIs, data URIs (`data:image/...`), and raw Base64 image payloads (`/9j`, `iVBORw0KGgo`, `UklGR`) seamlessly across all devices and partners with automatic `file:///9j` healing. Moments support dual photos (Partner A & Partner B) stacked vertically, stored in `partnerAImageUrl` / `partnerBImageUrl`. Every upload gets a unique Storage name (`{memoryId}_{slot}_{uid}_{epochMillis}.jpg`, profiles `profile_{uid}_{epochMillis}.jpg`) and local files are versioned the same way, so a role swap can never make one partner overwrite the file the other partner's URL points to, and Coil never serves a stale cached image. The legacy `imageUrl` field is read-only: it is only used as A's photo for old single-photo moments (when B is empty), is deleted on every update/swap, and an A slot equal to B is treated as a duplicate (`FirebaseCoupleRepository.resolveMemorySlots`). Memory photos utilize Local-First persistence (`LocalImageStorage.saveImage`) for immediate 0ms rendering and offline resilience, followed by background Firebase Storage upload with a 10s timeout, falling back to compressed Base64 data URIs so partner devices never receive inaccessible local sandbox `file:///` URIs. Image compression (`LocalImageStorage.compressImage`) automatically corrects EXIF rotation and applies adaptive compression guaranteeing payloads stay under 250 KB to ensure multi-photo Firestore documents never breach the 1MB document limit. Image URLs are sanitized via `sanitizeImageUrl` with raw Base64 normalization. Exporting/downloading photos to the device gallery is handled via `LocalImageStorage.saveImageToGallery` supporting Base64, local files, and remote URLs using `MediaStore.Images.Media` (`Pictures/CoupleBubble`) with Scoped Storage support (Android 10+ without prompts, and API 26-28 fallback). Fullscreen modal viewer supports multi-touch pinch-to-zoom (1f–4f), double-tap zoom (1f <-> 2.5f), bounded pan translation, direct gallery downloads, and swipe-to-dismiss: at normal zoom (`scale <= 1.05f`) a single-finger vertical drag moves the photo (`dragOffsetY`), fades the black backdrop (`alpha = (1f - |dragOffsetY| / 600f).coerceIn(0.2f, 1f)`), closes the viewer with a spring exit beyond 150 dp, and springs back to `0f` below it; multi-touch gestures and zoomed-in pans stay with the transform detector. The memory card menu action "In Galerie speichern" exports all photos of a moment (Partner A and B, de-duplicated). Memory cards default to expanded so photos remain immediately discoverable in the timeline. Each photo slot in a memory card (`MemoryPhotoSlot`) is framed by a `BorderStroke(1.5.dp)` in the owner's accent color (`partner1ColorHex` / `partner2ColorHex`); the partner's slot shows a subtle 14 dp `Icons.Default.Lock` in its top-right corner to signal it is read-only for the current user. Upload status: `CoupleViewModel.trackPhotoUpload` keeps the local-first copy of an added/edited memory in `_pendingMemories` (overlaid on the Firestore list by id) and marks every slot receiving new bytes as `PhotoSyncState.UPLOADING` in `DashboardUiState.photoSyncStates` (keyed by `PhotoSlotKey(memoryId, isPartnerA)`); add/edit dialogs close right away. When the repository returns, slots with an `https://` Storage URL switch to `SYNCED` for 1.5 s, while Base64 fallbacks and failures just drop the state. `PhotoSyncBadge` (bottom-right of `MemoryPhotoSlot`, black 45 % pill with 1px border) shows spinner + `Icons.Default.CloudUpload` while uploading, a check mark when synced, and fades out with `spring` animations. The dashboard `Scaffold` hosts an `ExtendedFloatingActionButton` (`Icons.Default.Add`, `colorScheme.secondary` Terracotta) that fires a short haptic tick and emits `onShowAddMemoryDialog(true)`; it collapses to icon-only via `derivedStateOf` once the content is scrolled. An empty timeline renders a single Terracotta-bordered card containing only a centered "Ersten Moment eintragen" button (`Icons.Default.Add`) that opens the same add-memory dialog. In add/edit memory dialogs, users can only modify their own photo slot (`isCurrentUserPartner1` check), while the partner's photo slot remains read-only and strictly preserved upon saves. Avatar rings gracefully fall back to partner initials via `onError` in `PhotoAvatarWithRing`. All `ImageRequest` instances in Compose are wrapped with `remember(url)` to prevent recomposition loops.
- **Backend:** Firebase (Authentication with Google SSO / Credential Manager, Cloud Firestore with Offline-Cache, Cloud Storage) operating on Spark Free Tier.
- **Room Membership & Limitation:** Spaces are strictly limited to exactly 2 participants (`userUids.size <= 2` / `members.size <= 2`). `connectWithCode` validates client- and backend-side to prevent unauthorized 3rd-party joining; a full space fails with the typed `SpaceFullException`, which `CoupleViewModel.onConnectClicked` maps to `PairingUiState.showSpaceFullDialog` instead of an inline error. `SpaceFullDialog` in `PairingScreen` offers "Code erneut prüfen" (`onRecheckCodeAfterSpaceFull`: closes the dialog, clears the PIN) and "Eigenen Raum erstellen" (`onCreateOwnSpaceAfterSpaceFull`: clears the PIN, switches to the CREATE tab and generates a fresh code). Documented Security Rule: `allow update: if request.auth != null && (resource.data.members.size() < 2 || request.auth.uid in resource.data.members);`.
- **Profile Accent Colors & Avatars:** Individual hex colors (`partner1ColorHex`, `partner2ColorHex`) customizable via 8 curated couple palette colors or any custom color via `CustomColorPickerDialog` (supporting HSV sliders, live avatar ring preview, and direct Hex input). Text and icons drawn on a surface filled with a partner color (avatar initials, palette check marks, picker preview) use `getContrastingTextColor` (`ui/theme/Color.kt`): `ColorUtils.calculateLuminance` > 0.45 returns `ContrastNavy` (`#0F172A`), otherwise `Color.White`. Fallback initials sit on a solid accent-color fill. Only one's own profile photo can be edited by each partner (`isCurrentUserPartner1` check). Profile photo selection includes interactive circular cropping (`ProfilePhotoCropDialog`) with pan and zoom gestures and zoom slider. Avatars use Local-First caching (`LocalImageStorage.saveProfilePhoto`) for 0ms immediate UI display, followed by background Firebase Storage upload with a strict 10s timeout and Base64 Data URI fallback (`data:image/jpeg;base64,...`) written unconditionally to Firestore so changes immediately sync across all devices via snapshot listeners even if storage permissions or network fail.
- **Pairing Codes:** Codes are 6 random digits, displayed as `482-913` and stored without the hyphen (`space_482913`, `pairingCode`). `connectWithCode` rejects anything that is not exactly 6 digits. `PairingScreen` enters codes via a segmented 6-box PIN field (`PinDigitSlot`, active box has a Terracotta border) backed by a transparent `BasicTextField` with `KeyboardType.NumberPassword`; the cursor is pinned to the end so Backspace removes the previous digit, and the 6th digit hides the keyboard, fires a haptic tick and calls `onConnectClicked()` automatically. On every resume (`LifecycleResumeEffect`) the screen reads `LocalClipboardManager`; if the text (minus `-`/whitespace) matches `^[0-9]{6}$`, an `AssistChip` (`Icons.Default.ContentPaste`) below the PIN field offers to paste it, which fills the field and triggers the same auto-connect.
- **Partner Roles:** Who is Partner 1/2 is decided by the explicit `partner1Id` / `partner2Id` fields on the space (fallbacks: `userUids` order, then the stored preference). `claimPartnerSlot` binds users transactionally: the joiner in `connectWithCode` claims slot 2, everyone else slot 1 if free; old spaces are backfilled from `userUids`, and a changed UID after Google sign-in replaces the old one instead of being appended. `userUids` is kept as `[partner1Id, partner2Id]`.
- **Atomic Partner Role Swap:** `swapPartners` swaps the space (partner IDs, `userUids`, names, profile photo URLs, accent colors) and every memory's photo slots in a single `firestore.runTransaction`, so either everything swaps or nothing does (limit: 499 memories). Missing values are deleted rather than skipped so no photo remains in both slots. If only one partner is bound to the space, the swap fails with `PartnerNotConnectedException`; the UI only confirms success after the transaction completes. In `EditNamesDialog` the swap is an `IconButton` (`Icons.Default.SwapVert`, Terracotta, rotates 180° per swap via `spring`) between the two partner sections; it fires `HapticFeedbackType.LongPress` and calls `onSwapRoles` (→ `CoupleViewModel.swapPartnerRoles`). Both sections are `AnimatedContent` keyed on a swap counter (`EditNamesFields.swapCount`): Partner 1 springs in from below, Partner 2 from above, inside a column with `animateContentSize()`.
- **User Anchor & Recovery:** Primary user entity stored in `/users/{uid}` mapping `uid` to `coupleId` to prevent data loss across reinstalls or Play Store app updates.
- **Date Math:** Strictly `java.time` (`LocalDate`, `Period`, `ChronoUnit`, `ZoneId.systemDefault()`). Never use `java.util.Date` or `java.util.Calendar` for business logic.

---

## 5. Jetpack Compose & State Rules

- **Unidirectional Data Flow (UDF):** Composables accept state objects (`uiState: MyUiState`) and emit lambda events (`onEvent: (MyUiEvent) -> Unit`). Never inject ViewModels into leaf composables.
- **Preview Coverage:** Every screen and shared component must include at least one `@Preview` composable wrapped in `CoupleBubbleTheme` and populated with realistic mock data (supporting both Light and Dark mode previews).
- **Logic Separation:** Zero business calculations, Firebase operations, or network requests inside Composable functions.
- **Performance Hygiene:** Never instantiate heavy objects inside Composable bodies without wrapping them in `remember`. Use `derivedStateOf` for calculated properties dependent on changing state.
- **Accessibility:** All clickable elements without text must have meaningful `contentDescription` string resources.

---

## 6. Security, Secrets & Zero-Cost Firebase Guardrails

### Secrets Lockdown
- Never commit, edit, or expose: `release.jks`, `key.properties`, `google-services.json`, or environment configuration files.
- Keep all credentials, API keys, aliases, and passwords out of Git history.

### Firebase Spark Plan Constraints & Optimization
- **Prevent Firestore Read/Write Loops:** Avoid continuous polling. Rely on snapshot listeners (`addSnapshotListener`) with lifecycle-aware cleanup (`callbackFlow` / `awaitClose`) or single fetch operations.
- **Atomic Operations:** Use Firestore transactions or WriteBatches during pairing/unpairing to prevent orphaned partner documents.
- **Client-Side Image Compression:** Compress images client-side before uploading to Firebase Storage via `LocalImageStorage.compressImage` (target resolution max 1200px, quality 80% JPEG, file size under 400 KB).
- **Offline Resilience:** Rely on Firestore offline persistence for immediate UI rendering while syncing in the background.

### Google SSO & Release Signing Configuration
- **Web-Client-ID Auto-Resolution:** `default_web_client_id` is generated automatically from `google-services.json` by the Google Services Gradle plugin. Never place placeholder strings like `YOUR_GOOGLE_WEB_CLIENT_ID` in `strings.xml`, as Android resource merging prioritizes `strings.xml` over generated values and breaks Google Sign-In.
- **R8 / ProGuard Keep-Rules:** Reflection classes used by AndroidX Credential Manager (`androidx.credentials.**`), Google Identity (`com.google.android.libraries.identity.googleid.**`), and Firebase Auth (`com.google.firebase.auth.**`) must remain preserved in `app/proguard-rules.pro` to prevent reflection and obfuscation breakage in release builds.
- **Debug & Play App Signing SHA-1 Fingerprints:**
  - **Local Development / Testing:** The local debug keystore SHA-1 must be registered in the Firebase Console under Android Apps so Google Play Services authorizes sign-in.
  - **Release / Play Console:** When deploying via Google Play Console, Google re-signs the bundle with its own Play App Signing key. The SHA-1 fingerprint from Play Console (**Release > Setup > App integrity > Play App Signing key certificate**) must be added to the Firebase project settings under Android Apps.
- **OAuth Client Check:** `google-services.json` must contain an `oauth_client` with `client_type: 1` and a `certificate_hash` for every signing key (local debug, upload key, Play App Signing). If only the Web client (`client_type: 3`) is present, Credential Manager fails with code 10/16. Without `${rootDir}/debug.keystore`, debug builds are signed with `~/.android/debug.keystore`.
- **Anonymous → Google Linking:** `GoogleAuthClient.signIn` upgrades an anonymous Firebase user via `linkWithCredential` to preserve the UID and `/users/{uid}` anchor, falling back to `signInWithCredential` only on `FirebaseAuthUserCollisionException`.
- **Credential Manager Error Handling:** Distinguish `GetCredentialCancellationException` (silent user cancellation, debug-logged only) from genuine authentication failures, displaying user-friendly German error messages in toasts or snackbars. Errors are mapped by exception type (`NoCredentialException`, `GetCredentialException` with `[10]`/`[16]`, `FirebaseAuthException`, `FirebaseNetworkException`), never by loose substring matching.

---

## 7. Verification & Build Protocol

Before finalizing any task:
1. Verify Kotlin compilation and dependencies against the Android Gradle Plugin (`./gradlew assembleDebug`).
2. Ensure unit tests pass cleanly via `./gradlew test`.
3. Check for lint errors via `./gradlew lintDebug`.
4. Verify that all `@Preview` composables render cleanly without runtime exceptions.
5. Confirm that both `README.md` and `AGENTS.md` accurately reflect all modifications made.
