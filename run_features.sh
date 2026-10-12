#!/bin/bash
set -euo pipefail

# --- 1. Sicherheitspruefung: Richtiges Projekt? ---
if [ ! -f "settings.gradle.kts" ] || [ ! -f "app/build.gradle.kts" ]; then
    echo "Fehler: Dieses Skript muss im Hauptverzeichnis des Android-Projekts ausgefuehrt werden."
    exit 1
fi

if ! grep -q "com.aistudio.couplebubble.qxztrw" app/build.gradle.kts; then
    echo "Fehler: Falsches Projekt! Das Ziel-Package com.aistudio.couplebubble.qxztrw wurde nicht gefunden."
    exit 1
fi

PROMPT_FILE="features.txt"
if [ ! -f "$PROMPT_FILE" ]; then
    echo "Fehler: Datei $PROMPT_FILE existiert nicht!"
    exit 1
fi

echo "Projekt validiert: CoupleBubble (com.aistudio.couplebubble.qxztrw)"
echo "Starte Task-Abarbeitung..."

TMP_FILE=$(mktemp)
current_block=""
block_done=false

execute_task() {
    local task="$1"
    echo "--------------------------------------------------"
    echo "Bearbeite Task:"
    echo "$task"
    echo "--------------------------------------------------"

    # Claude Code ausfuehren
    claude -p "$task" \
      --dangerously-skip-permissions \
      --append-system-prompt "Du bist Entwickler fuer die App CoupleBubble (Package com.aistudio.couplebubble.qxztrw). Arbeite strikt in zwei Phasen: 1. Plane kurz die Dateiaenderungen. 2. Implementiere sie vollstaendig. Führe am Ende KEINE Tests oder Builds aus, das uebernimmt das umgebende Skript."

    echo "Validiere Build und Unit-Tests..."
    if ./gradlew assembleDebug testDebugUnitTest; then
        echo "Build erfolgreich. Erstelle Git Commit..."
        git add .
        # Erste Zeile des Blocks fuer die Commit-Message nutzen
        local commit_msg
        commit_msg=$(echo "$task" | head -n 1 | sed 's/^[ \t-]*//')
        git commit -m "feat: $commit_msg" || echo "Keine Code-Aenderungen zum Committen."
        return 0
    else
        echo "Build fehlgeschlagen fuer Task!"
        return 1
    fi
}

# Blockweises Einlesen (Hauptpunkte + eingerueckte Unterpunkte zusammenfassen)
while IFS= read -r line || [ -n "$line" ]; do
    # Neuer Hauptpunkt (faengt mit '- ' oder '[DONE] - ' an)
    if [[ "$line" =~ ^[[:space:]]*-\  || "$line" =~ ^\[DONE\][[:space:]]*-\  ]]; then
        # Vorherigen Block abarbeiten, falls vorhanden
        if [ -n "$current_block" ]; then
            if [ "$block_done" = true ]; then
                echo "$current_block" >> "$TMP_FILE"
            else
                if execute_task "$current_block"; then
                    echo "$current_block" | sed 's/^[[:space:]]*-/[DONE] -/' >> "$TMP_FILE"
                else
                    echo "$current_block" >> "$TMP_FILE"
                    cat >> "$TMP_FILE"
                    mv "$TMP_FILE" "$PROMPT_FILE"
                    exit 1
                fi
                sleep 3
            fi
        fi

        current_block="$line"
        if [[ "$line" =~ ^\[DONE\] ]]; then
            block_done=true
        else
            block_done=false
        fi
    # Unterpunkt oder Folgezeile (eingerueckt mit Leerzeichen/Tabs)
    elif [[ "$line" =~ ^[[:space:]]+ && -n "$current_block" ]]; then
        current_block="$current_block"$'\n'"$line"
    elif [[ -z "$line" ]]; then
        if [ -n "$current_block" ]; then
            if [ "$block_done" = true ]; then
                echo "$current_block" >> "$TMP_FILE"
            else
                if execute_task "$current_block"; then
                    echo "$current_block" | sed 's/^[[:space:]]*-/[DONE] -/' >> "$TMP_FILE"
                else
                    echo "$current_block" >> "$TMP_FILE"
                    cat >> "$TMP_FILE"
                    mv "$TMP_FILE" "$PROMPT_FILE"
                    exit 1
                fi
                sleep 3
            fi
            current_block=""
        fi
        echo "" >> "$TMP_FILE"
    fi
done < "$PROMPT_FILE"

# Letzten Block abarbeiten
if [ -n "$current_block" ]; then
    if [ "$block_done" = true ]; then
        echo "$current_block" >> "$TMP_FILE"
    else
        if execute_task "$current_block"; then
            echo "$current_block" | sed 's/^[[:space:]]*-/[DONE] -/' >> "$TMP_FILE"
        else
            echo "$current_block" >> "$TMP_FILE"
            mv "$TMP_FILE" "$PROMPT_FILE"
            exit 1
        fi
    fi
fi

mv "$TMP_FILE" "$PROMPT_FILE"
echo "Alle Aufgaben wurden erfolgreich abgearbeitet."