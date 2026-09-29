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
- **Architecture:** MVVM with Unidirectional Data Flow (UDF)
- **State Handling:** `StateFlow` backed by immutable UI state objects (`sealed interface` / `data class`)
- **Image Loading:** Coil for Compose (`io.coil-kt:coil-compose`) with memory & disk caching
- **Backend:** Firebase (Authentication, Cloud Firestore, Cloud Storage) operating on Spark Free Tier
- **Date Math:** Strictly `java.time` (`LocalDate`, `Period`, `ChronoUnit`, `Instant`). Never use `java.util.Date` or `java.util.Calendar` for business logic.

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
- **Client-Side Image Compression:** Compress images client-side before uploading to Firebase Storage (target resolution max 1200px, quality 80% JPEG/WebP, file size under 400 KB).
- **Offline Resilience:** Rely on Firestore offline persistence for immediate UI rendering while syncing in the background.

---

## 7. Verification & Build Protocol

Before finalizing any task:
1. Verify Kotlin compilation and dependencies against the Android Gradle Plugin (`./gradlew assembleDebug`).
2. Ensure unit tests pass cleanly via `./gradlew test`.
3. Check for lint errors via `./gradlew lintDebug`.
4. Verify that all `@Preview` composables render cleanly without runtime exceptions.
5. Confirm that both `README.md` and `AGENTS.md` accurately reflect all modifications made.
