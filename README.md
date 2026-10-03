# CoupleBubble

Ein minimalistischer Begleiter für Paare – entwickelt als native Android-App mit Jetpack Compose, Jetpack Glance und Firebase.

---

## Features

- **Beziehungs-Counter:** Präzise Zählung der gemeinsamen Tage, Monate und Jahre mit typografischem Fokus, abgesichert gegen Zeitzonenwechsel mit `java.time.LocalDate.now(ZoneId.systemDefault())`.
- **Google SSO & Datenanker (Garantie gegen Datenverlust):** Anbindung an Google Sign-In und Firebase Auth (`/users/{uid}`). Nach Neuinstallationen oder App-Updates stellt die Google-Anmeldung eure Raumverbindung (`coupleId`) aus Firestore sofort wieder her.
- **Einfaches Paar-Pairing & Raum-Einrichtung:** Sicheres Verbinden zweier Accounts via 6-stelligem Zahlen-Code (z. B. `482-913`, segmentiertes PIN-Feld mit Ziffern-Tastatur, verbindet automatisch nach der 6. Ziffer; ein Code in der Zwischenablage wird erkannt und lässt sich mit einem Tipp einfügen) mit direktem Onboarding-Dialog für Spitznamen und Zusammenkommens-Datum (Material 3 DatePicker).
- **Strikte 2-Personen-Raumbeschränkung:** Räume sind client- und backendseitig auf maximal 2 Partner limitiert. Dritte Personen sehen den Dialog „Dieser Raum ist bereits belegt“ mit den Optionen „Code erneut prüfen“ (leert die Eingabe) und „Eigenen Raum erstellen“ (wechselt in die Raumerstellung mit frischem Code).
- **Session-Persistenz & Dauerhafte Anmeldung:** Automatischer Einstieg ins Dashboard ohne Datenverlust dank Jetpack DataStore Preferences, Firebase Auth User-Persistence und Firestore-Offline-Cache.
- **Individuelle Profilfotos mit Zuschnitt & Geräte-Synchronisation:** Jeder Partner kann ausschließlich das eigene Profilbild bearbeiten. Beim Auswählen öffnet sich ein interaktiver Bildzuschnitt-Dialog (`ProfilePhotoCropDialog`) mit Verschieben (Pan), Pinch-to-Zoom und Zoom-Slider für den perfekten runden Ausschnitt. Dank **Local-First Caching** ist das Profilbild sofort in 0 ms sichtbar; der Cloud-Upload zu Firebase Storage erfolgt im Hintergrund auf `Dispatchers.IO` mit 10s-Timeout und Base64-Data-URI-Fallback (`data:image/jpeg;base64,...`), sodass Änderungen auch bei Netzwerkverzögerungen oder Spark-Plan-Einschränkungen sofort und zuverlässig auf dem Gerät des Partners über Firestore-Snapshot-Listener erscheinen.
- **Freie Farbwahl & Kuratierte Farbpalette:** Neben den 8 harmonischen CoupleBubble-Standardfarben steht ein vollwertiger Farbwähler (`CustomColorPickerDialog`) zur Verfügung mit Farbton-Slider (360° Hue-Regenbogen), Sättigungs- & Helligkeitsreglern, direktem Hex-Code-Eingabefeld (`#RRGGBB`) und Live-Avatarring-Vorschau. Initialen und Häkchen auf einer Profilfarbe wählen ihre Farbe automatisch nach der Helligkeit (`getContrastingTextColor`): auf hellen Farben dunkles Navy, auf dunklen Weiß – so bleibt jede frei gewählte Farbe gut lesbar.
- **Memory-Timeline (Chronik) mit Dual-Foto-Synchronisation & Partner-Rechte:** Festhalten besonderer Momente mit Titel, Datum (interaktiver Material 3 DatePicker), Notiz und zwei separaten Foto-Slots (Partner A & Partner B). Jeder Partner kann beim Erstellen oder Bearbeiten ausschließlich das **eigene Foto** auswählen, anpassen oder entfernen; das Foto des Partners bleibt schreibgeschützt und wird beim Speichern strikt unverändert erhalten. Beide Partner sehen die Fotos synchronisiert und übereinander gestapelt in der Chronik. Jeder Foto-Slot ist mit einem 1,5-dp-Rahmen in der Akzentfarbe seines Partners eingefasst; der Slot des Partners zeigt oben rechts ein dezentes Schloss, damit sofort klar ist, dass du dieses Foto nicht ändern kannst. Karten sind standardmäßig ausgeklappt, inklusive nativer Vollbildansicht mit stufenlosem Pinch-to-Zoom (1x bis 4x), Pan-Gesten, Doppelklick-Zoom/Reset (1x <-> 2.5x) und Wischen zum Schließen: In normaler Zoomstufe lässt sich das Foto nach oben oder unten wegziehen, der Hintergrund wird dabei transparenter; ab 150 dp schließt sich die Ansicht sanft, darunter federt das Bild zurück. Neue Momente lassen sich jederzeit über einen Terracotta-Floating-Action-Button („Erinnerung festhalten“) mit kurzem haptischem Feedback anlegen; beim Scrollen klappt er dezent auf das Plus-Icon zusammen. Ist die Chronik noch leer, zeigt eine schlichte Karte mit Terracotta-Rand nur den Button „Ersten Moment eintragen“, der denselben Erstellungsdialog öffnet.
- **Foto-Download in Smartphone-Galerie:** Momente-Fotos können sowohl in der Vollbildansicht als auch über das 3-Punkte-Menü jeder Erinnerungskarte (dort werden alle Fotos des Moments – Partner A und B – gespeichert) direkt in den lokalen Android-Medienspeicher (`Pictures/CoupleBubble`) exportiert werden. Unterstützt Android 10+ Scoped Storage (ohne Berechtigungsdialog), API 26-28 Fallback sowie haptisches und visuelles Feedback.
- **Lokale & Cloud-Datensicherheit (Local-First):** Fotos werden beim Hinzufügen/Aktualisieren sofort lokal gesichert (`LocalImageStorage`), sodass keine Bilder durch schlechte Netzverbindungen verloren gehen. Über die Komponenten-Pipeline in `CoupleBubbleApplication` (`ImageLoaderFactory`) mit `Base64Mapper` (String → `Base64Image`), `Base64Keyer` (Memory-Cache-Keying) und `Base64Fetcher` verarbeitet Coil remote `https://` URLs, lokale `file://`/`content://` URIs sowie eingebettete Base64 Data-URIs (`data:image/jpeg;base64,...`) und Roh-Payloads nahtlos auf allen Geräten beider Partner, ohne an Coils vorgelagerter `StringMapper`-URI-Konvertierung zu scheitern.
- **Automatische EXIF-Korrektur & Schutz vor Firestore-Größenlimits:** Bilder werden vor dem Speichern anhand ihrer EXIF-Metadaten automatisch aufrecht gedreht und auf unter 250 KB komprimiert, sodass Firestore-Dokumente auch bei zwei hochauflösenden Fotos niemals das 1MB-Dokumentenlimit überschreiten.
- **Homescreen-Widget (Jetpack Glance):** Minimalistisches Android-Widget mit Anzeige der gemeinsamen Tage, Partnernamen und nächstem Jubiläum.
- **Haptisches Feedback:** Subtiles physisches Feedback bei Interaktionen (Codes kopieren, Fotos in die Galerie speichern, Momente sichern, Rollentausch, Reaktionen senden) – kräftig bei wichtigen Aktionen, ganz leicht beim Wechseln von Tabs, Farben oder Datum.
- **Rollentausch (Partner 1 ↔ 2):** Die Rollen hängen an festen IDs (`partner1Id` / `partner2Id`), nicht an der Reihenfolge der Mitglieder. Ein Tausch verschiebt Namen, Farben, Profilbilder und alle Moment-Fotos in einer einzigen Firestore-Transaktion – entweder alles oder nichts. Jeder Upload bekommt einen eindeutigen Dateinamen, damit nach einem Tausch niemand das Foto des anderen überschreibt und keine doppelten Bilder entstehen. Getauscht werden kann erst, wenn beide Partner mit dem Raum verbunden sind. Im Spitznamen-Dialog tauschen die beiden Partner-Bereiche beim Tippen auf den Tauschen-Button sichtbar die Plätze (Spring-Animation, kräftiges haptisches Feedback).
- **Material You & Dark Mode:** Elegantes Deep Navy & Terracotta Design-System ohne KI-Klischees.

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

### Google SSO & Release-Konfiguration
- **Web-Client-ID:** Wird automatisch aus der `google-services.json` über das Google Services Gradle Plugin bereitgestellt.
- **Debug & Play App Signing SHA-1:**
  - **Lokales Testen / Debug:** Der Debug-SHA-1-Fingerprint des lokalen Keystores muss in den Firebase-Projekteinstellungen hinterlegt sein.
  - **Play Store Release:** Der SHA-1-Fingerprint aus der Google Play Console (**Release > Einrichtung > App-Integrität > Play App-Signaturschlüssel**) muss in den Firebase-Projekteinstellungen hinterlegt werden, ebenso der SHA-1 des Upload-Keys.
  - **Kontrolle:** Nach dem Hinterlegen die `google-services.json` neu herunterladen. Sie muss einen `oauth_client` mit `client_type: 1` und `certificate_hash` enthalten, sonst schlägt der Credential Manager mit Code 10/16 fehl. Ohne `debug.keystore` im Projekt-Root signiert Gradle Debug-Builds mit `~/.android/debug.keystore`.
- **Anonym → Google:** Ist der aktuelle Firebase-Nutzer anonym, wird das Google-Credential per `linkWithCredential` verknüpft (UID und `/users/{uid}`-Anker bleiben erhalten). Nur bei `FirebaseAuthUserCollisionException` (Google-Konto existiert bereits) wird per `signInWithCredential` gewechselt.
- **R8 / ProGuard Keep-Rules:** In `app/proguard-rules.pro` sind Keep-Rules für `androidx.credentials.**`, `com.google.android.libraries.identity.googleid.**` und `com.google.firebase.auth.**` hinterlegt.


---

##  Datenschutz & Sicherheit

- **Zero-Cost Firebase Spark Plan:** Firestore-Zugriffe sind auf minimale Reads/Writes optimiert.
- **Sensible Daten:** Lokale Konfigurationen (`google-services.json`, Key-Files) sind in `.gitignore` geschützt und werden nicht ins Repository übertragen.
- **Datenschutzerklärung:** Siehe [`PRIVACY_POLICY.md`](./PRIVACY_POLICY.md).
