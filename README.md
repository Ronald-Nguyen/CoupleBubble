# CoupleBubble

A minimal companion app for couples, built as a native Android app with Jetpack Compose, Jetpack Glance and Firebase.

The app's user interface is German; code, comments and documentation are English.

---

## Features

- **Relationship counter:** Counts the days, weeks, months and years together, based on `java.time.LocalDate` in the device time zone so time-zone changes never shift the count. "Together since" and the next milestone share one compact, left-aligned card (`TogetherCard`): chips inside the card switch the view between days, weeks, months and years (stored on your device only). Below a subtle divider follows the next milestone with a countdown and a slim progress bar.
- **Milestones your way:** "Meilensteine" in the menu (or a tap on the milestone) switches each kind on or off: days (100, 500, 1,000, 1,111 …), weeks (10, 50, 100 …), month days (every month on the day you got together; in shorter months on the last day), anniversaries and custom milestones. Custom milestones have a name and a date (e.g. "Hochzeit"), live in Firestore under `spaces/{id}/milestones` and show up for your partner right away. "Jedes Jahr feiern" repeats a custom milestone every year on the same day, and the list then shows its next date. Each custom milestone can be switched off for both of you (field `enabled`): it stays greyed out in the list but is no longer counted or notified. The bin icon next to a custom milestone deletes it for both of you after a confirmation. When two milestones fall on the same day, the more personal one wins (custom > moment > anniversary > month day > weeks > days). The kind switches apply to your device only.
- **Moments as milestones:** A moment can be switched on as a yearly milestone in two places that share one value (field `countsAsMilestone` on the memory, shared by both partners, off by default): the checkbox "Jedes Jahr als Meilenstein feiern" in the add/edit moment dialog, and the collapsible "Momente (n)" section below the custom milestones, which lists every moment with a checkbox. A switched-on moment celebrates its yearly anniversary from the first year on, e.g. "Erster Urlaub · 1 Jahr": it counts as the next milestone, appears in the widgets and triggers the milestone notification.
- **Reunion countdown ("Wiedersehen planen"):** For couples who live apart. "Wiedersehen planen" in the top-right menu (or a tap on the card) asks for the first and last day of your next visit. Below the together counter a Terracotta-framed card then counts the days until you meet, with a progress bar from the day you planned it; during the visit it shows "Tag 1 von 3" and offers "Moment festhalten", and once it is over it disappears. The dates are shared via `spaces/{id}/reunion/current`. Optionally the card shows how far apart you are ("≈ 476 km"): each partner switches their own approximate location on or off. Only `ACCESS_COARSE_LOCATION` is requested, only while the app is open (never in the background), at most once an hour, and the coordinates are stored rounded to one decimal (about 10 km) under your UID; your partner only sees the distance. Sharing is optional: without it (or without permission) the card simply shows no distance. A location older than 7 days no longer counts. Switching it off, leaving the space or the visit ending deletes the stored location. If saving or removing the reunion is refused, a snackbar says so.
- **Share the room code anytime:** "Raum-Code teilen" in the top-right menu opens the share sheet with the space's code, even when the invite card is gone. Old spaces without a stored `pairingCode` derive it from their id (`space_482913`).
- **Google sign-in as a data anchor:** Google Sign-In and Firebase Auth anchor each user at `/users/{uid}`. After a reinstall or update, signing in with Google restores the connection to your space (`coupleId`) from Firestore. On a slow network or offline the app shows the last known state from the Firestore cache instead of placeholder names; without a cached copy it waits instead of inventing data.
- **Simple pairing and space setup:** Two accounts connect through a 6-digit code. Every device draws its own random code (`SecureRandom`, re-drawn if the code is already taken) at start and after leaving a space, e.g. `482-913`. The segmented PIN field uses the number keyboard and connects automatically after the sixth digit; a code in the clipboard is detected and pasted with one tap. While the partner is still missing, the dashboard shows the code with "Teilen" and "Code kopieren". Both partners are then asked for nicknames and the day they got together (Material 3 date picker).
- **Strictly two people per space:** Spaces are limited to two partners on the client and in Firestore. A third person sees "Dieser Raum ist bereits belegt" with "Code erneut prüfen" (clears the input) and "Eigenen Raum erstellen" (switches to creating a space with a fresh code).
- **Session persistence:** The app opens straight into the dashboard thanks to DataStore, Firebase Auth's persisted login and the Firestore offline cache.
- **Profile photos with cropping:** Each partner can only change their own profile photo. Which slot is "your own" is decided by the repository when saving, inside a Firestore transaction, from the signed-in UID, so a role swap during the upload cannot put the photo into the partner's slot. If your account is not bound to the space yet, nothing is guessed: the app says so and binds the account in the background. Tapping a profile photo opens it full screen with zoom and download (your own photo via a small menu "Im Vollbild ansehen" / "Foto ändern"). Picking a photo opens an interactive round crop (`ProfilePhotoCropDialog`) with pan, pinch-to-zoom and a zoom slider. Thanks to **local-first caching** the photo shows immediately; the upload to Firebase Storage runs in the background with a 10-second timeout and falls back to a Base64 data URI (`data:image/jpeg;base64,...`), so the partner's device shows the change through the snapshot listener even when Storage is slow or unavailable on the Spark plan.
- **Free color choice and a curated palette:** Besides eight CoupleBubble colors there is a full color picker (`CustomColorPickerDialog`) with a hue slider, saturation and brightness, a hex field (`#RRGGBB`) and a live avatar-ring preview. Initials and check marks on a partner color pick their color by brightness (`getContrastingTextColor`): dark navy on light colors, white on dark ones.
- **Moment timeline with two photo slots:** Moments have a title, a date, a note and one photo slot per partner. When creating or editing, each partner can only pick, change or remove **their own photo**; the partner's photo stays read-only and is never written back from the device but taken from the stored version, so a photo lands in the right slot even after a role swap during editing. Both photos are shown stacked. Each slot has a 1.5 dp frame in its owner's accent color, and the partner's slot shows a small lock. Collapsed cards show small thumbnails framed in the owner's color; cards start expanded. The full-screen viewer supports pinch-to-zoom (1×–4×), panning, double-tap zoom (1× ↔ 2.5×) and swipe-to-dismiss: at normal zoom the photo can be dragged up or down while the background fades; beyond 150 dp the viewer closes, below that the photo springs back. A Terracotta floating action button ("Erinnerung festhalten") adds moments and collapses to its icon while scrolling. An empty timeline shows a single card with "Ersten Moment eintragen". Deleting a moment needs no confirmation: it disappears right away and a snackbar "Moment gelöscht" offers "Rückgängig"; only once the snackbar goes away is it deleted for both of you. If that fails, the moment comes back and a snackbar says so.
- **Save photos to the gallery:** Photos can be exported from the full-screen viewer or, for all photos of a moment, from the card's menu to `Pictures/CoupleBubble` (Scoped Storage on Android 10+ without a permission prompt, with a fallback for Android 8–9).
- **Back in your space after a reinstall:** Updates keep login and session. After a reinstall, Android Auto Backup restores the session (space id, role) and the Firebase login (local photo copies are excluded). Anyone who signed in with Google once is also signed in silently at start and returns to the space through `/users/{uid}`, so connecting Google is the safest option. Inside a space there is no Google sign-out, and signing out never drops the Firebase login: the space membership is tied to that UID.
- **Local-first photos with an upload badge:** Photos are saved locally right away, so a bad connection never loses them. While the upload runs, the moment shows the local photo with a small translucent badge (spinner and cloud icon); once Firebase Storage confirms the HTTPS URL, it briefly turns into a check mark and fades out. A Base64 fallback or a failure just removes the badge. Coil's component pipeline in `CoupleBubbleApplication` (`Base64Mapper`, `Base64Keyer`, `Base64Fetcher`) renders `https://` URLs, local `file://` / `content://` URIs, Base64 data URIs and raw Base64 payloads on both partners' devices.
- **EXIF correction and Firestore size limits:** Photos are rotated upright from their EXIF data and compressed below 250 KB, so a moment document with two photos never exceeds Firestore's 1 MB limit.
- **Shared notes and lists:** A bottom navigation ("Uns" | "Notizen") leads to your shared notebook, like Google Keep for two. A note has a title and either text or a checklist and can be converted at any time: text lines become items (bullets are dropped, `[x]` is already checked) and back. Optional labels work as filter chips. You start with Einkauf, Bucket List and Ideen and can rename, delete or add labels together ("Etiketten bearbeiten"): changes are only saved with "Fertig", "Abbrechen" throws them away, leaving with unsaved changes asks first, a typed new label is included even without "+", and a deleted label stays crossed out with "Wiederherstellen" until you save. Deleted labels only disappear from notes; the notes stay. Pinned notes come first. The overview is a two-column grid with previews and progress ("3 von 8 erledigt"). Open items can be reordered by dragging their handle, and your partner sees the new order right away. Checked items move under "Erledigt" and can be cleared together. A small dot in the accent color shows who wrote a note or item. Notes live in Firestore under `spaces/{id}/notes`, with checklist items as a map in the same document; changes are field-path updates, so you can check off different items at the same time without overwriting each other. A new item is saved without Enter once you leave the field or close the note. Typing is batched (500 ms), and a note left empty disappears with a short "Leere Notiz verworfen". Long-press a note to select it, tap more, and delete them together. Deleting needs no confirmation: notes disappear right away and a snackbar ("Notiz gelöscht" / "3 Notizen gelöscht") offers "Rückgängig"; they are only deleted for both of you once it goes away. Deleted list items and "Erledigte entfernen" can be undone the same way and come back exactly as they were.
- **Home-screen widgets (Jetpack Glance):** "CoupleBubble Status" shows your time together in the unit chosen in the app (days, weeks, months or years), with names and the next milestone when there is room; "Nächster Meilenstein" shows only the countdown, title, date and progress; "Wiedersehen" counts the days until your next visit with its dates, the progress since you planned it and, while both of you share a location, how far apart you are (during the visit "Tag 2 von 3"). All three can be resized down to 1×1 and adapt to their size: 1×1 and one-row strips (2×1–4×1) show just two lines, the number and one label ("412" / "Tage zusammen"), 2×2 and larger add headings, and from three rows on progress bars, dates and the distance appear. All three compute with today's date when drawn, are redrawn shortly after midnight and move on to the next milestone by themselves.
- **Haptic feedback:** Subtle feedback on interactions (copying codes, saving photos, saving moments, swapping roles, deleting notes, clearing checked items): strong for confirmed actions, light for switching tabs, filters, colors or dates and checking items.
- **Role swap (partner 1 ↔ 2):** Roles are tied to fixed ids (`partner1Id` / `partner2Id`), not to the order of members. A swap moves names, colors, profile photos and every moment's photos in a single Firestore transaction: everything or nothing. Every upload gets a unique file name, so after a swap nobody overwrites the other's photo. A swap needs both partners bound to the space and is refused while a photo is uploading. Both devices' stored roles follow the partner ids after every swap. In the nickname dialog, both partner sections visibly trade places once the swap is confirmed; while you are alone in the space the swap button is disabled and a short note explains why, so a refused swap can never be saved as a rename.
- **Material You and dark mode:** A deep navy and Terracotta design system.

---

## Tech stack

- **Language and platform:** Kotlin, native Android (`minSdk = 26`, `targetSdk = 36`)
- **UI:** Jetpack Compose with Material 3
- **Widgets:** Jetpack Glance with Material 3; all three widgets are also offered on the lock screen (`widgetCategory="home_screen|keyguard"`, where the system supports lock-screen widgets)
- **Architecture:** MVVM with unidirectional data flow: screens get an immutable state and report actions as events (`PairingEvent`, `DashboardEvent`, `NotesEvent`); one-shot feedback (toast, snackbar, haptics) comes back as `CoupleEffect`
- **Honest feedback:** confirmations („Neuer Moment festgehalten!“, „Spitznamen und Farben wurden aktualisiert“) only appear once the repository `Result` succeeded; every refused write (names/colors, space setup, moments, milestones, reunion, notes, items, labels, deletions) shows a warm German snackbar saying what to do. Debounced note text writes report at most one snackbar per failing series.
- **State:** `StateFlow` of immutable UI state (`DashboardUiState`, `PairingUiState`, `NotesUiState`, all `@Immutable`); texts produced outside the UI are `UiText` (string or plural resource, never a raw exception message)
- **Dependency injection:** manual, through `AppContainer` / `DefaultAppContainer`; no DI framework
- **Navigation:** state-based without a navigation library (`AppRoute` for loading / pairing / paired, `MainTab` for the bottom navigation, the open note in `NotesViewModel`)
- **Local persistence:** Jetpack DataStore Preferences
- **Backend:** Firebase on the Spark (free) plan: Authentication (anonymous, Google through AndroidX Credential Manager), Cloud Firestore with offline cache, Cloud Storage
- **Images:** Coil for Compose with custom Base64 components
- **Location:** Google Play services location (fused provider, approximate accuracy only) for the reunion distance
- **Dates:** strictly `java.time`; "today" comes from an injected `java.time.Clock`
- **Dependencies:** Gradle version catalog (`gradle/libs.versions.toml`)

---

## Architecture

```text
UI (Compose screens) ──events──▶ ViewModels ──▶ domain (models, rules, repository interfaces)
        ▲                            │                         ▲
        └──────── state / effects ───┘          data (Firebase, DataStore, local files)
```

- **`core/`** – app-wide plumbing: `di/` (`AppContainer`, `DefaultAppContainer`, `AppDispatchers`), `ui/` (`UiText`, date formats), `Cancellation.kt` (`rethrowIfCancellation`, called in every catch-all around suspend calls).
- **`domain/`** – plain Kotlin: `model/` (couple space, memories, milestones, notes, `NoteOrganizer`, `PairingCodes`, the reunion), `calendar/` (`RelationshipDateCalculator`, `RelationshipCalendar`), `error/` (the sealed `CoupleException` family), `repository/` (interfaces: `SpaceRepository`, `MemoryRepository`, `MilestoneRepository`, their union `CoupleRepository`, `NotesRepository`, `ReunionRepository`), `session/` (background work of a session).
- **`data/`** – implementations: `auth/` (Credential Manager), `location/` (`LocationProvider`, approximate fused location), `local/` (`SessionStore` on DataStore, `LocalImageStore` for compression, local copies and gallery export, `ImageUrls`), `remote/` (`FirebaseProvider`, `RemoteImageUploader`, `FirestoreSchema` field names, `FirestoreRefs`, and `mapper/` converting Firestore documents as plain maps), `repository/` (Firebase repositories; `FirebaseCoupleRepository` combines space, memory and milestone repositories by delegation).
- **`ui/`** – Compose screens with their ViewModels: `screens/home` (`AppViewModel`, `PairedHomeScreen`, shared UI state and events, `GoogleAuthController`), `screens/pairing` (`PairingViewModel`), `screens/dashboard` (`DashboardViewModel`), `screens/notes` (`NotesViewModel`); plus `components/`, `coil/`, `theme/` and `preview/` (sample data for previews).
- **`notifications/`, `widget/`** – Android notifications and Glance widgets behind small interfaces (`MomentNotifier`, `WidgetPublisher`).

How the pieces work together:

- **ViewModels:** `AppViewModel` decides between loading, pairing and the paired home and runs the session's background work. `PairingViewModel` owns the pairing screen. `DashboardViewModel` owns the "Uns" tab. `NotesViewModel` owns the notes tab. They never depend on each other.
- **Shared session pieces** live in the container, one per process:
  - `SessionRestorer` restores the session once. If the device has lost access to the stored space (no Firebase login, or a new UID facing a full space), it leaves the space instead of showing it read-only from the cache, and the pairing screen explains why.
  - `GoogleAuthController` holds the Google sign-in state shared by pairing and dashboard.
  - `SpaceContent` listens to the active space's memories, custom milestones and reunion once and shares them.
- **Background coordinators** in `domain/session/`, started by `AppViewModel`:
  - `PartnerMomentWatcher` (partner moment notifications)
  - `MilestoneReminder` (milestone-today notifications)
  - `PartnerRoleSync` (stored role follows the partner ids)
- **Firestore:** every field name comes from `FirestoreSchema`; documents are converted only in the mappers, which are unit-tested without Firebase. Listener errors never emit empty lists, so a failed snapshot can't look like a deletion. All writes that decide whose photo slot changes run in transactions against the server copy.

---

## Project structure

```text
CoupleBubble/
├── app/
│   ├── compose_stability.conf   # Classes the Compose compiler may treat as stable
│   ├── src/main/java/com/aistudio/couplebubble/qxztrw/
│   │   ├── core/                # di/, ui/ (UiText, date formats), Cancellation
│   │   ├── domain/              # model/, calendar/, error/, repository/, session/
│   │   ├── data/                # auth/, local/, location/, remote/ (+ mapper/), repository/
│   │   ├── notifications/       # MomentNotifier and its Android implementation
│   │   ├── ui/                  # coil/, components/, preview/, theme/, screens/{home,pairing,dashboard,notes}
│   │   ├── widget/              # Glance widgets, snapshot store, midnight refresh
│   │   ├── CoupleBubbleApplication.kt
│   │   └── MainActivity.kt      # Routing between screens and effect collection
│   ├── src/test/                # Unit and Robolectric tests; fakes in data/fake/
│   └── src/main/res/            # Strings (German), drawables, widget providers
├── firebase/                    # Firestore and Storage security rules (firebase.json points here)
├── .github/workflows/           # CI: build, unit tests, lint
├── gradle/libs.versions.toml    # Dependency versions
├── AGENTS.md                    # Rules and architecture notes for coding agents
├── PRIVACY_POLICY.md            # Privacy policy
└── README.md
```

---

## Setup and build

### Requirements
- Android Studio (recent stable)
- JDK 17
- Android SDK 36
- A Firebase project with `app/google-services.json` (not committed; builds without it work, but Firebase stays unavailable)

### Commands

```bash
./gradlew assembleDebug        # Build a debug APK
./gradlew testDebugUnitTest    # Unit and Robolectric tests
./gradlew lintDebug            # Android lint
```

### Google sign-in and release signing
- **Web client id:** generated from `google-services.json` by the Google Services Gradle plugin; never put a placeholder into `strings.xml`.
- **SHA-1 fingerprints:** register the local debug keystore's SHA-1 in the Firebase console. For Play Store releases, also register the SHA-1 of the upload key and of the Play App Signing key (Play Console → Release → Setup → App integrity). Then download `google-services.json` again: it must contain an `oauth_client` with `client_type: 1` and a `certificate_hash` for every signing key. Otherwise Credential Manager fails with code 10/16/28444, typically only in Play Store installs, because Google re-signs the bundle. Without `debug.keystore` in the project root, debug builds are signed with `~/.android/debug.keystore`.
- **Anonymous → Google:** an anonymous Firebase user is upgraded with `linkWithCredential`, keeping the UID and the `/users/{uid}` anchor; only on `FirebaseAuthUserCollisionException` does the app switch accounts with `signInWithCredential`.
- **R8 keep rules:** `app/proguard-rules.pro` keeps `androidx.credentials.**`, `com.google.android.libraries.identity.googleid.**` and `com.google.firebase.auth.**`.

---

## Notifications

When your partner adds a moment, the app shows a local notification (channel "Neue Momente"). It comes from the Firestore listener, so only while the app process is alive; push through FCM would need Cloud Functions, which the Spark plan doesn't include. On Android 13+ the app asks for the permission after pairing.

When an enabled milestone is reached today, each device notifies once in the channel "Meilensteine". The last notified milestone (`spaceId:date`) is kept in DataStore, so it doesn't repeat on every start. This also only happens if the app runs that day.

---

## Privacy and security

- **Location:** only approximate location, only in the foreground, rounded to about 10 km before it is stored, opt-in per partner; the Firestore rules let each partner write only their own entry in the reunion document.
- **Spark plan:** Firestore access is kept to few reads and writes (snapshot listeners instead of polling, one shared listener per collection).
- **Secrets:** `google-services.json` and key files are git-ignored and never committed.
- **Security rules:** `firebase/firestore.rules` and `firebase/storage.rules` describe the access the app needs; they are deployed manually with the Firebase CLI.
- **Privacy policy:** see [`PRIVACY_POLICY.md`](./PRIVACY_POLICY.md).
