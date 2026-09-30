# CoupleBubble

Ein minimalistischer Begleiter für Paare – entwickelt als native Android-App mit Jetpack Compose, Jetpack Glance und Firebase.

---

## Features

-  **Beziehungs-Counter:** Präzise Zählung der gemeinsamen Tage, Monate und Jahre mit typografischem Fokus, abgesichert gegen Zeitzonenwechsel mit `java.time.LocalDate.now(ZoneId.systemDefault())`.
-  **Google SSO & Datenanker (Garantie gegen Datenverlust):** Anbindung an Google Sign-In und Firebase Auth (`/users/{uid}`). Nach Neuinstallationen oder App-Updates stellt die Google-Anmeldung eure Raumverbindung (`coupleId`) aus Firestore sofort wieder her.
-  **Einfaches Paar-Pairing & Raum-Einrichtung:** Sicheres Verbinden zweier Accounts via kurzem Pairing-Code mit direktem Onboarding-Dialog für Kosenamen und Zusammenkommens-Datum (Material 3 DatePicker).
-  **Session-Persistenz & Dauerhafte Anmeldung:** Automatischer Einstieg ins Dashboard ohne Datenverlust dank Jetpack DataStore Preferences, Firebase Auth User-Persistence und Firestore-Offline-Cache.
-  **Dynamische Kosenamen:** Personalisierte Begrüßung im Header ("Anna & Ben") sowie beidseitige Bearbeitung und Synchronisation von Partnernamen.
-  **Echtzeit-Disconnect-Sync:** Bei Verbindungsauflösung setzt ein Snapshot-Listener beide Partnergeräte unmittelbar auf den Pairing-Screen zurück und bereinigt lokale Sessions.
-  **Memory-Timeline (Chronik) mit Dual-Foto-Unterstützung:** Festhalten besonderer Momente mit Titel, Datum, Notiz und zwei separaten Foto-Slots (für beide Partner). Fotos werden in der Detailansicht übersichtlich untereinander mit Kosenamen-Kennzeichnung dargestellt. Inklusive clientseitiger Bildkomprimierung gemäß Firebase Spark Plan Richtlinien.
-  **Homescreen-Widget (Jetpack Glance):** Minimalistisches Android-Widget mit Anzeige der gemeinsamen Tage, Partnernamen und nächstem Jubiläum.
-  **Haptisches Feedback:** Subtiles physisches Feedback bei Interaktionen (Codes kopieren, Reaktionen senden).
-  **Material You & Dark Mode:** Elegantes Deep Navy & Terracotta Design-System ohne KI-Klischees.

---

##  Tech Stack & Architektur

- **Sprache & Platform:** Kotlin | Native Android (`minSdk = 26`, `targetSdk = 34`)
- **UI Framework:** Jetpack Compose mit Material 3 (`androidx.compose.material3`)
- **Homescreen Widget:** Jetpack Glance Material 3 (`androidx.glance:glance-appwidget`, `androidx.glance:glance-material3`)
- **Architektur:** MVVM mit Unidirectional Data Flow (UDF)
- **State-Handling:** `StateFlow` backed by immutable UI state objects (`CoupleMainState`, `DashboardUiState`)
- **Lokale Persistenz:** Jetpack DataStore Preferences (`androidx.datastore:datastore-preferences`)
- **Backend:** Firebase (Authentication via AndroidX Credential Manager & Google ID Helper, Cloud Firestore mit Offline-Cache, Cloud Storage)
- **Dependency Management:** Gradle Version Catalog (`gradle/libs.versions.toml`)
- **Bildverarbeitung:** Coil for Compose (`io.coil-kt:coil-compose`)
- **Datumsberechnungen:** Strikt `java.time` (`LocalDate`, `Period`, `ChronoUnit`, `ZoneId.systemDefault()`)

---

##  Projektstruktur

```text
CoupleBubble/
├── app/
│   ├── src/main/java/com/aistudio/couplebubble/qxztrw/
│   │   ├── auth/           # GoogleAuthClient (AndroidX Credential Manager & Google ID Helper)
│   │   ├── data/           # DataStore Preferences (Session Caching)
│   │   ├── model/          # CoupleSpace, Memory, RelationshipDateCalculator
│   │   ├── repository/     # CoupleRepository, FirebaseCoupleRepository, MockCoupleRepository
│   │   ├── ui/             # CoupleViewModel, CoupleMainState, DashboardUiState
│   │   │   ├── screens/    # DashboardScreen (Timeline, Names Dialog), PairingScreen
│   │   │   └── theme/      # Color, Type, Shape & Theme definitions
│   │   ├── widget/         # Glance Homescreen Widget & Receiver
│   │   └── MainActivity.kt # Entry Point mit flackerfreiem Session-Routing
│   └── src/main/res/       # Vector Assets, Strings (de) & Widget XML Provider
├── gradle/
│   └── libs.versions.toml  # Globale Dependency Versionen
├── AGENTS.md               # Vorgaben & Qualitätsstandards für KI-Agenten
├── PRIVACY_POLICY.md       # Öffentliche Datenschutzerklärung
└── README.md               # Projekt-Dokumentation
```

---

##  Setup & Build

### Voraussetzungen
- Android Studio (Hedgehog | Iguana oder neuer)
- JDK 17
- Android SDK 34

### Build- & Test-Befehle

```bash
# Debug APK bauen
./gradlew assembleDebug

# Unit Tests ausführen
./gradlew testDebugUnitTest

# Linting prüfen
./gradlew lintDebug
```

---

##  Datenschutz & Sicherheit

- **Zero-Cost Firebase Spark Plan:** Firestore-Zugriffe sind auf minimale Reads/Writes optimiert.
- **Sensible Daten:** Lokale Konfigurationen (`google-services.json`, Key-Files) sind in `.gitignore` geschützt und werden nicht ins Repository übertragen.
- **Datenschutzerklärung:** Siehe [`PRIVACY_POLICY.md`](./PRIVACY_POLICY.md).
