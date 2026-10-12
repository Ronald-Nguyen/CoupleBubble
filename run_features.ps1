$ErrorActionPreference = "Stop"

# 1. Sicherheitsprüfung: Richtiges Projekt?
if (-not (Test-Path "settings.gradle.kts") -or -not (Test-Path "app/build.gradle.kts")) {
    Write-Host "Fehler: Dieses Skript muss im Hauptverzeichnis des Android-Projekts ausgefuehrt werden." -ForegroundColor Red
    exit 1
}

$buildGradle = Get-Content "app/build.gradle.kts" -Raw
if ($buildGradle -notmatch "com.aistudio.couplebubble.qxztrw") {
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

function Execute-Task($task) {
    Write-Host "--------------------------------------------------" -ForegroundColor Cyan
    Write-Host "Bearbeite Task:"
    Write-Host $task
    Write-Host "--------------------------------------------------" -ForegroundColor Cyan

    # Führenden Bindestrich entfernen, damit Claude CLI es nicht als Option parst
    $cleanPrompt = $task -replace '^\s*-\s*', ''

    # Claude CLI mit Argument-Trenner '--' aufrufen
    claude -p -- "$cleanPrompt" `
      --dangerously-skip-permissions `
      --append-system-prompt "Du bist Android-Entwickler fuer CoupleBubble (Package com.aistudio.couplebubble.qxztrw). Plane kurz und implementiere vollstaendig. Fuehre KEINE Tests oder Builds aus, das macht das Skript."

    if ($LASTEXITCODE -ne 0) {
        Write-Host "Fehler: Claude CLI ist fehlgeschlagen!" -ForegroundColor Red
        return $false
    }

    Write-Host "Validiere Build und Unit-Tests..." -ForegroundColor Yellow
    cmd.exe /c "gradlew.bat assembleDebug testDebugUnitTest"
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Build erfolgreich. Erstelle Git Commit..." -ForegroundColor Green
        git add .
        $firstLine = ($task -split "`r?`n")[0] -replace '^[ \t-]*', ''
        git commit -m "feat: $firstLine"
        return $true
    } else {
        Write-Host "Build fehlgeschlagen fuer Task!" -ForegroundColor Red
        return $false
    }
}

$lines = Get-Content $PromptFile
$newLines = @()
$currentBlock = @()
$isDone = $false

foreach ($line in $lines) {
    if ($line -match '^\s*-\s+' -or $line -match '^\[DONE\]\s*-\s+') {
        if ($currentBlock.Count -gt 0) {
            $taskText = $currentBlock -join "`n"
            if ($isDone) {
                $newLines +=$currentBlock
            } else {
                if (Execute-Task $taskText) {
                    $newLines += ($currentBlock[0] -replace '^\s*-', '[DONE] -')
                    if ($currentBlock.Count -gt 1) {$newLines += $currentBlock[1..($currentBlock.Count - 1)]
                    }
                } else {
                    $newLines +=$currentBlock
                    Set-Content -Path $PromptFile -Value ($newLines + $lines[($lines.IndexOf($line))..($lines.Count - 1)])
                    exit 1
                }
                Start-Sleep -Seconds 3
            }
        }
        $currentBlock = @($line)
        $isDone =$line -match '^[DONE]'
    } elseif ($line -match '^\s+' -and$currentBlock.Count -gt 0) {
        $currentBlock +=$line
    } elseif ([string]::IsNullOrWhiteSpace($line)) {
        if ($currentBlock.Count -gt 0) {
            $taskText =$currentBlock -join "`n"
            if ($isDone) {
                $newLines += $currentBlock
            } else {
                if (Execute-Task $taskText) {
                    $newLines += ($currentBlock[0] -replace '^\s*-', '[DONE] -')
                    if ($currentBlock.Count -gt 1) {
                        $newLines += $currentBlock[1..($currentBlock.Count - 1)]
                    }
                } else {
                    $newLines += $currentBlock
                    Set-Content -Path $PromptFile -Value ($newLines + $lines[($lines.IndexOf($line))..($lines.Count - 1)])
                    exit 1
                }
                Start-Sleep -Seconds 3
            }
            $currentBlock = @()
        }
        $newLines += $line
    }
}

if ($currentBlock.Count -gt 0) {
    $taskText = $currentBlock -join "`n"
    if ($isDone) {
        $newLines +=$currentBlock
    } else {
        if (Execute-Task $taskText) {
            $newLines += ($currentBlock[0] -replace '^\s*-', '[DONE] -')
            if ($currentBlock.Count -gt 1) {$newLines += $currentBlock[1..($currentBlock.Count - 1)]
            }
        } else {
            $newLines +=$currentBlock
            Set-Content -Path $PromptFile -Value$newLines
            exit 1
        }
    }
}

Set-Content -Path $PromptFile -Value$newLines
Write-Host "Alle Aufgaben wurden erfolgreich abgearbeitet." -ForegroundColor Green