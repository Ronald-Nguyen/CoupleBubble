$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# 1. Sicherheitsprüfung: Richtiges Projekt?
if (-not (Test-Path "settings.gradle.kts") -or -not (Test-Path "app/build.gradle.kts")) {
    Write-Host "Fehler: Dieses Skript muss im Hauptverzeichnis des Android-Projekts liegen." -ForegroundColor Red
    exit 1
}

$buildGradle = Get-Content "app/build.gradle.kts" -Raw
if ($buildGradle -notmatch "com\.aistudio\.couplebubble\.qxztrw") {
    Write-Host "Fehler: Falsches Projekt! com.aistudio.couplebubble.qxztrw nicht gefunden." -ForegroundColor Red
    exit 1
}

$PromptFile = "features.txt"
if (-not (Test-Path $PromptFile)) {
    Write-Host "Fehler: Datei $PromptFile existiert nicht!" -ForegroundColor Red
    exit 1
}

Write-Host "Projekt validiert: CoupleBubble (com.aistudio.couplebubble.qxztrw)" -ForegroundColor Green
Write-Host "Starte Task-Abarbeitung..."

function Invoke-Build {
    # Direkt ueber den Pfad aufrufen: "cmd /c gradlew.bat" findet die Datei nicht,
    # wenn NoDefaultCurrentDirectoryInExePath gesetzt ist.
    & .\gradlew.bat assembleDebug testDebugUnitTest --console=plain
    return ($LASTEXITCODE -eq 0)
}

function Invoke-Task([string]$task) {
    Write-Host "--------------------------------------------------" -ForegroundColor Cyan
    Write-Host "Bearbeite Task:"
    Write-Host $task
    Write-Host "--------------------------------------------------" -ForegroundColor Cyan

    # Führenden Bindestrich entfernen, damit Claude CLI es nicht als Option parst
    $cleanPrompt = $task -replace '^\s*-\s*', ''

    # Optionen MUESSEN vor '--' stehen, alles danach ist Prompt-Text
    claude -p `
      --dangerously-skip-permissions `
      --append-system-prompt "Du bist Android-Entwickler fuer CoupleBubble (Package com.aistudio.couplebubble.qxztrw). Plane kurz und implementiere vollstaendig. Fuehre KEINE Builds aus und committe NICHT, das macht das Skript." `
      -- $cleanPrompt

    if ($LASTEXITCODE -ne 0) {
        Write-Host "Fehler: Claude CLI ist fehlgeschlagen!" -ForegroundColor Red
        return $false
    }

    Write-Host "Validiere Build und Unit-Tests..." -ForegroundColor Yellow
    if (-not (Invoke-Build)) {
        Write-Host "Build fehlgeschlagen fuer Task!" -ForegroundColor Red
        return $false
    }

    Write-Host "Build erfolgreich. Erstelle Git Commit..." -ForegroundColor Green
    git add .
    $status = git status --porcelain
    if ($status) {
        $firstLine = ($task -split "`r?`n")[0] -replace '^[ \t-]*', ''
        git commit -m "feat: $firstLine"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Fehler: git commit fehlgeschlagen!" -ForegroundColor Red
            return $false
        }
    } else {
        Write-Host "Nichts zu committen (bereits committet)." -ForegroundColor Yellow
    }
    return $true
}

# Tasks einlesen: Zeile mit "- " oder "[DONE] - " startet einen Block, eingerückte Zeilen gehören dazu
$lines = [System.Collections.Generic.List[string]](Get-Content $PromptFile -Encoding UTF8)
$blocks = @()
$current = $null
for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    if ($line -match '^(\[DONE\]\s*)?-\s+') {
        if ($current) { $blocks += $current }
        $current = @{ Start = $i; End = $i; Done = ($line -match '^\[DONE\]') }
    } elseif ($current -and $line -match '^\s+\S') {
        $current.End = $i
    } elseif ($current) {
        $blocks += $current
        $current = $null
    }
}
if ($current) { $blocks += $current }

foreach ($block in $blocks) {
    if ($block.Done) { continue }

    $taskText = ($lines[$block.Start..$block.End]) -join "`n"
    if (-not (Invoke-Task $taskText)) {
        exit 1
    }

    # Fortschritt sofort speichern, damit er bei Abbruch nicht verloren geht
    $lines[$block.Start] = $lines[$block.Start] -replace '^\s*-', '[DONE] -'
    Set-Content -Path $PromptFile -Value $lines -Encoding UTF8
    Start-Sleep -Seconds 3
}

Write-Host "Alle Aufgaben wurden erfolgreich abgearbeitet." -ForegroundColor Green
