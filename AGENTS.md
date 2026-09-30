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
- **Micro-Interactions & Haptics:** Apply `LocalHapticFeedback.current` on key touchpoints (copying pairing codes, milestone triggers, date submissions, emotional check-ins).
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
- **Image Loading & Handling:** Coil for Compose (`io.coil-kt:coil-compose`) with custom `Base64Fetcher` registered in `CoupleBubbleApplication` (`ImageLoaderFactory`). Handles remote `https://` URLs, local `file://` / `content://` URIs, data URIs (`data:image/...`), and raw Base64 image payloads seamlessly across all devices and partners. Moments support dual photos (Partner A & Partner B) stacked vertically, saved with slot suffixes (`_a.jpg`, `_b.jpg`). Memory photos utilize Local-First persistence (`LocalImageStorage.saveImage`) for immediate 0ms rendering and offline resilience, followed by background Firebase Storage upload with a 10s timeout, falling back to compressed Base64 data URIs so partner devices never receive inaccessible local sandbox `file:///` URIs. Image compression (`LocalImageStorage.compressImage`) automatically corrects EXIF rotation and applies adaptive compression guaranteeing payloads stay under 250 KB to ensure multi-photo Firestore documents never breach the 1MB document limit. Image URLs are sanitized via `sanitizeImageUrl` with raw Base64 normalization. Exporting/downloading photos to the device gallery is handled via `LocalImageStorage.saveImageToGallery` supporting Base64, local files, and remote URLs using `MediaStore.Images.Media` (`Pictures/CoupleBubble`) with Scoped Storage support (Android 10+ without prompts, and API 26-28 fallback). Fullscreen modal viewer supports multi-touch pinch-to-zoom (1f–4f), double-tap zoom (1f <-> 2.5f), bounded pan translation, and direct gallery downloads. Memory cards default to expanded so photos remain immediately discoverable in the timeline. In add/edit memory dialogs, users can only modify their own photo slot (`isCurrentUserPartner1` check), while the partner's photo slot remains read-only and strictly preserved upon saves. Avatar rings gracefully fall back to partner initials via `onError` in `PhotoAvatarWithRing`.
- **Backend:** Firebase (Authentication with Google SSO / Credential Manager, Cloud Firestore with Offline-Cache, Cloud Storage) operating on Spark Free Tier.
- **Room Membership & Limitation:** Spaces are strictly limited to exactly 2 participants (`userUids.size <= 2` / `members.size <= 2`). `connectWithCode` validates client- and backend-side to prevent unauthorized 3rd-party joining. Documented Security Rule: `allow update: if request.auth != null && (resource.data.members.size() < 2 || request.auth.uid in resource.data.members);`.
- **Profile Accent Colors & Avatars:** Individual hex colors (`partner1ColorHex`, `partner2ColorHex`) customizable via 8 curated couple palette colors or any custom color via `CustomColorPickerDialog` (supporting HSV sliders, live avatar ring preview, and direct Hex input). Only one's own profile photo can be edited by each partner (`isCurrentUserPartner1` check). Profile photo selection includes interactive circular cropping (`ProfilePhotoCropDialog`) with pan and zoom gestures and zoom slider. Avatars use Local-First caching (`LocalImageStorage.saveProfilePhoto`) for 0ms immediate UI display, followed by background Firebase Storage upload with a strict 10s timeout and Base64 Data URI fallback (`data:image/jpeg;base64,...`) written unconditionally to Firestore so changes immediately sync across all devices via snapshot listeners even if storage permissions or network fail.
- **Atomic Partner Role Swap:** Role switching between Partner 1 and Partner 2 is handled via atomic Firestore transactions (`firestore.runTransaction`), swapping partner IDs, names/nicknames, profile photo URLs, accent colors, and memory photo slots to keep memories bound to individuals.
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
- **Credential Manager Error Handling:** Distinguish `GetCredentialCancellationException` (silent user cancellation, debug-logged only) from genuine authentication failures, displaying user-friendly German error messages in toasts or snackbars.

---

## 7. Verification & Build Protocol

Before finalizing any task:
1. Verify Kotlin compilation and dependencies against the Android Gradle Plugin (`./gradlew assembleDebug`).
2. Ensure unit tests pass cleanly via `./gradlew test`.
3. Check for lint errors via `./gradlew lintDebug`.
4. Verify that all `@Preview` composables render cleanly without runtime exceptions.
5. Confirm that both `README.md` and `AGENTS.md` accurately reflect all modifications made.
