# CoupleBubble 🫧❤️

Ein intimer, minimalistischer und haptischer Begleiter für Paare – entwickelt als native Android-App mit Jetpack Compose und Firebase.

---

## 🌟 Features

- ⏱️ **Beziehungs-Counter:** Präzise Zählung der gemeinsamen Tage, Monate und Jahre mit typografischem Fokus.
- 🔗 **Einfaches Paar-Pairing:** Sicheres Verbinden zweier Accounts via kurzem Pairing-Code.
- 📸 **Gemeinsame Erinnerungen (Bubbles):** Teilen von Fotos und Meilensteinen im gemeinsamen Feed.
- 📳 **Haptisches Feedback:** Subtiles physisches Feedback bei Interaktionen (Codes kopieren, Reaktionen senden).
- 🌙 **Material You & Dark Mode:** Elegantes Deep Navy & Terracotta Design-System.

---

## 🛠️ Tech Stack & Architektur

- **Sprache & Platform:** Kotlin | Native Android (`minSdk = 26`, `targetSdk = 34`)
- **UI & Design:** Jetpack Compose, Material 3, Custom Design System
- **Architektur:** MVVM mit Unidirectional Data Flow (UDF)
- **State-Management:** `StateFlow`, `Kotlin Coroutines`, `Flow`
- **Backend:** Firebase (Authentication, Cloud Firestore, Cloud Storage)
- **Dependency Management:** Gradle Version Catalog (`gradle/libs.versions.toml`)
- **Bildverarbeitung:** Coil for Compose (inkl. Client-seitiger Komprimierung)

---

## 🎨 Design-Philosophie ("Anti-AI-Slop")

CoupleBubble wurde bewusst gegen den Trend klischeehafter KI-SaaS-Templates gestaltet:
- **Keine generischen Violett/Neon-Gradients** – Stattdessen edles Deep Navy Blue (`#0F2137`) und warmes Terracotta (`#E65D2E`).
- **Keine kitschigen Sparkle-Icons oder KI-Floskeln** – Fokus auf authentische, warme deutsche Microcopy und klare Typografie.
- **Haptik & Physik** – Federbasierte Animationen (`spring()`) und haptisches Touch-Feedback.

---

## 📂 Projektstruktur

```text
CoupleBubble/
├── app/
│   ├── src/main/java/com/couplebubble/
│   │   ├── data/           # Repositories, Firebase Data Sources & DTOs
│   │   ├── domain/         # Use Cases, Modelle & Date Math Utilities
│   │   ├── ui/             # Composables, ViewModels, States & Theme
│   │   │   ├── components/ # Wiederverwendbare UI-Elemente
│   │   │   ├── screens/    # Screen-Composables (Pairing, Home, Memories)
│   │   │   └── theme/      # Color, Type, Shape & Theme definitions
│   │   └── MainActivity.kt
│   └── src/main/res/       # Vector Assets, Strings (de) & Layouts
├── gradle/
│   └── libs.versions.toml  # Globale Dependency Versionen
├── AGENTS.md               # Vorgaben & Qualitätsstandards für KI-Agenten
├── PRIVACY_POLICY.md       # Öffentliche Datenschutzerklärung
└── README.md               # Projekt-Dokumentation
```

---

## 🚀 Setup & Build

### Voraussetzungen
- Android Studio (Hedgehog | Iguana oder neuer)
- JDK 17
- Android SDK 34

### Schritte
1. Repository klonen.
2. Eigene `google-services.json` in den Ordner `app/` legen (Firebase Projekt konfigurieren).
3. Projekt in Android Studio öffnen und Gradle-Sync ausführen.

### Build- & Test-Befehle

```bash
# Debug APK bauen
./gradlew assembleDebug

# Unit Tests ausführen
./gradlew test

# Linting prüfen
./gradlew lintDebug
```

---

## 🔒 Datenschutz & Sicherheit

- **Zero-Cost Firebase Spark Plan:** Firestore-Zugriffe sind auf minimale Reads/Writes optimiert.
- **Sensible Daten:** Lokale Konfigurationen (`google-services.json`, Key-Files) sind in `.gitignore` geschützt und werden nicht ins Repository übertragen.
- **Datenschutzerklärung:** Siehe [`PRIVACY_POLICY.md`](./PRIVACY_POLICY.md).

---

## 📝 Lizenz & Regelwerk

Alle Entwicklungsrichtlinien für Erweiterungen sind strikt in der [`AGENTS.md`](./AGENTS.md) geregelt.
