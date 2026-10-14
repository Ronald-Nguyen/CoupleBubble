param(
    [string]$PromptFile = "features.txt",
    [string]$Model = "claude-opus-4-6-thinking",
    [int]$MaxFixAttempts = 2,
    [int]$AgyTimeoutMinutes = 30,
    [int]$GradleTimeoutMinutes = 20,
    [switch]$SkipLint,
    [switch]$AllowDirty
)

# Native Tools (git, gradle) schreiben Warnungen nach stderr; Fehler werden über Exit-Codes geprüft
$ErrorActionPreference = "Continue"
Set-Location $PSScriptRoot

# 1. Sicherheitsprüfung: Richtiges Projekt?
if (-not (Test-Path "settings.gradle.kts") -or -not (Test-Path "app/build.gradle.kts")) {
    Write-Host "Fehler: Dieses Skript muss im Hauptverzeichnis des Android-Projekts liegen." -ForegroundColor Red
    exit 1
}
if ((Get-Content "app/build.gradle.kts" -Raw) -notmatch "com\.aistudio\.couplebubble\.qxztrw") {
    Write-Host "Fehler: Falsches Projekt! com.aistudio.couplebubble.qxztrw nicht gefunden." -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $PromptFile)) {
    Write-Host "Fehler: Datei $PromptFile existiert nicht!" -ForegroundColor Red
    exit 1
}

$AgyCmd = (Get-Command agy.exe -ErrorAction SilentlyContinue).Source
if (-not $AgyCmd) {
    $fallback = "C:\Users\$env:USERNAME\AppData\Local\agy\bin\agy.exe"
    if (Test-Path $fallback) {
        $AgyCmd = $fallback
    } else {
        Write-Host "Fehler: agy.exe weder im PATH noch unter $fallback gefunden." -ForegroundColor Red
        exit 1
    }
}

$MainBranch = (git rev-parse --abbrev-ref HEAD).Trim()
$RunDir = Join-Path "logs" (Get-Date -Format "yyyy-MM-dd_HH-mm-ss")
New-Item -ItemType Directory -Force $RunDir | Out-Null

$SystemPromptFile = Join-Path $RunDir "system-prompt.txt"
Set-Content -Path $SystemPromptFile -Encoding UTF8 -Value @"
Du bist Android-Entwickler fuer CoupleBubble (Package com.aistudio.couplebubble.qxztrw).
- Lies zuerst AGENTS.md und halte dich strikt daran.
- Plane kurz und implementiere den Task vollstaendig.
- Halte README.md und AGENTS.md aktuell, wenn sich Architektur, Screens, Modelle oder Abhaengigkeiten aendern.
- Schreibe Unit-Tests fuer neue Logik und passe bestehende Tests an, wenn sich Verhalten absichtlich aendert.
- Pruefe deine Aenderungen mit .\gradlew.bat compileDebugKotlin compileDebugUnitTestKotlin, bevor du fertig bist.
- Committe NICHT und fuehre keine git-Befehle aus, die den Verlauf aendern. Das macht das Skript.
"@

Write-Host "Projekt validiert: CoupleBubble (Branch $MainBranch)" -ForegroundColor Green
Write-Host "Antigravity CLI: $AgyCmd (Modell: $Model)" -ForegroundColor Green
Write-Host "Logs: $RunDir"

function Get-Slug([string]$text) {
    $slug = $text.ToLower() -replace 'ä', 'ae' -replace 'ö', 'oe' -replace 'ü', 'ue' -replace 'ß', 'ss' -replace '[^a-z0-9]+', '-'
    $slug = $slug.Trim('-')
    if ($slug.Length -gt 40) { $slug = $slug.Substring(0, 40).Trim('-') }
    return $slug
}

# Startet einen Prozess mit Zeitlimit, Ausgabe landet in $LogFile. Rückgabe: Exit-Code, -1 bei Timeout
function Invoke-WithTimeout([string]$Arguments, [string]$LogFile, [int]$Minutes, [string]$StdinFile) {
    $params = @{
        FilePath               = "cmd.exe"
        ArgumentList           = "/d /s /c `"$Arguments`""
        RedirectStandardOutput = $LogFile
        RedirectStandardError  = "$LogFile.err"
        NoNewWindow            = $true
        PassThru               = $true
    }
    if ($StdinFile) { $params.RedirectStandardInput = $StdinFile }
    $proc = Start-Process @params
    $handle = $proc.Handle  # nötig, damit ExitCode nach dem Ende lesbar bleibt
    if (-not $proc.WaitForExit($Minutes * 60 * 1000)) {
        taskkill /PID $proc.Id /T /F | Out-Null
        Add-Content -Path $LogFile -Value "`n*** Abgebrochen nach $Minutes Minuten ***"
        return -1
    }
    if (Test-Path "$LogFile.err") {
        Get-Content "$LogFile.err" | Add-Content -Path $LogFile
        Remove-Item "$LogFile.err" -Force -ErrorAction SilentlyContinue
    }
    return $proc.ExitCode
}

function Invoke-AgyWithTimeout([string[]]$AgyArgs, [string]$LogFile, [int]$Minutes) {
    $errFile = "$LogFile.err"
    $params = @{
        FilePath               = $AgyCmd
        ArgumentList           = $AgyArgs
        RedirectStandardOutput = $LogFile
        RedirectStandardError  = $errFile
        NoNewWindow            = $true
        PassThru               = $true
    }
    $proc = Start-Process @params
    $handle = $proc.Handle
    if (-not $proc.WaitForExit($Minutes * 60 * 1000)) {
        taskkill /PID $proc.Id /T /F | Out-Null
        Add-Content -Path $LogFile -Value "`n*** Abgebrochen nach $Minutes Minuten ***"
        return -1
    }
    if (Test-Path $errFile) {
        Get-Content $errFile | Add-Content -Path $LogFile
        Remove-Item $errFile -Force -ErrorAction SilentlyContinue
    }
    return $proc.ExitCode
}

function Invoke-Agy([string]$Prompt, [switch]$ContinueSession, [string]$LogFile) {
    $promptPath = "$LogFile.prompt.txt"
    Set-Content -Path $promptPath -Value $Prompt -Encoding UTF8

    $agyArgs = @("--dangerously-skip-permissions")
    if ($Model) {
        $agyArgs += "--model"
        $agyArgs += $Model
    }
    if ($ContinueSession) {
        $agyArgs += "-c"
    }
    $agyArgs += "-p"
    $agyArgs += $Prompt

    Write-Host "  Antigravity arbeitet... ($LogFile)" -ForegroundColor DarkGray
    $code = Invoke-AgyWithTimeout $agyArgs $LogFile $AgyTimeoutMinutes
    if (Test-Path $LogFile) {
        Get-Content $LogFile -Tail 15 | ForEach-Object { Write-Host "  | $_" -ForegroundColor DarkGray }
    }
    return ($code -eq 0)
}

function Invoke-Gradle([string]$Tasks, [string]$LogFile) {
    Write-Host "  Gradle: $Tasks" -ForegroundColor Yellow
    return (Invoke-WithTimeout ".\gradlew.bat $Tasks --console=plain" $LogFile $GradleTimeoutMinutes) -eq 0
}

# Die relevanten Fehlerzeilen eines Gradle-Logs für den Reparatur-Prompt
function Get-BuildErrors([string]$LogFile) {
    $hits = Select-String -Path $LogFile -Pattern '^e: ', '^w: .*error', ' FAILED$', 'Error', 'Exception', 'Abgebrochen nach' -Context 0, 3
    $text = ($hits | ForEach-Object { $_.Line; $_.Context.PostContext }) | Select-Object -First 120
    if (-not $text) { $text = Get-Content $LogFile -Tail 80 }
    return ($text -join "`n")
}

# Verschiebt die Änderungen eines gescheiterten Tasks auf einen eigenen Branch, main bleibt sauber
function Save-FailedWork([string]$Slug, [string]$Title) {
    if (-not (git status --porcelain)) { return $null }
    $branch = "failed/$(Get-Date -Format 'MMdd-HHmm')-$Slug"
    git switch -q -c $branch
    git add .
    git commit -q -m "wip (fehlgeschlagen): $Title"
    git switch -q $MainBranch
    return $branch
}

function Invoke-Task([string]$Task, [string]$TaskDir) {
    New-Item -ItemType Directory -Force $TaskDir | Out-Null
    $cleanPrompt = $Task -replace '^\s*-\s*', ''
    $systemPromptText = Get-Content $SystemPromptFile -Raw -Encoding UTF8

    $initialPrompt = @"
$systemPromptText

Aufgabe:
$cleanPrompt
"@

    if (-not (Invoke-Agy $initialPrompt -LogFile (Join-Path $TaskDir "agy-0.log"))) {
        return "Antigravity CLI fehlgeschlagen oder Zeitlimit erreicht"
    }

    for ($attempt = 0; ; $attempt++) {
        $buildLog = Join-Path $TaskDir "gradle-$attempt.log"
        if (Invoke-Gradle "assembleDebug testDebugUnitTest" $buildLog) { break }

        if ($attempt -ge $MaxFixAttempts) {
            return "Build/Tests nach $MaxFixAttempts Reparaturversuchen weiterhin rot"
        }
        Write-Host "  Build rot, Reparaturversuch $($attempt + 1)/$MaxFixAttempts" -ForegroundColor Magenta
        $fixPrompt = @"
$systemPromptText

Der Build bzw. die Unit-Tests (.\gradlew.bat assembleDebug testDebugUnitTest) sind nach deinen Aenderungen fehlgeschlagen.
Behebe genau diese Fehler. Wenn ein Test wegen absichtlich geaendertem Verhalten fehlschlaegt, passe den Test an, sonst den Code.
Vollstaendiges Log: $buildLog

$(Get-BuildErrors $buildLog)
"@
        if (-not (Invoke-Agy $fixPrompt -ContinueSession -LogFile (Join-Path $TaskDir "agy-fix-$($attempt + 1).log"))) {
            return "Antigravity CLI beim Reparaturversuch fehlgeschlagen"
        }
    }

    if (-not $SkipLint) {
        if (-not (Invoke-Gradle "lintDebug" (Join-Path $TaskDir "lint.log"))) {
            Write-Host "  Warnung: lintDebug meldet Probleme (siehe lint.log), Task wird trotzdem committet." -ForegroundColor DarkYellow
        }
    }
    return $null
}

# Tasks einlesen: "- ", "[DONE] - " oder "[FAILED] - " startet einen Block, eingerückte Zeilen gehören dazu
$lines = [System.Collections.Generic.List[string]](Get-Content $PromptFile -Encoding UTF8)
$blocks = @()
$current = $null
for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    if ($line -match '^(\[(DONE|FAILED)\]\s*)?-\s+') {
        if ($current) { $blocks += $current }
        $current = @{ Start = $i; End = $i; Skip = ($line -match '^\[(DONE|FAILED)\]') }
    } elseif ($current -and $line -match '^\s+\S') {
        $current.End = $i
    } elseif ($current) {
        $blocks += $current
        $current = $null
    }
}
if ($current) { $blocks += $current }

$summary = @()
$number = 0
foreach ($block in $blocks) {
    if ($block.Skip) { continue }
    $number++

    $taskText = ($lines[$block.Start..$block.End]) -join "`n"
    $title = ($lines[$block.Start] -replace '^[ \t-]*', '').Trim()
    $slug = Get-Slug $title
    $taskDir = Join-Path $RunDir ("{0:D2}-{1}" -f $number, $slug)

    Write-Host "--------------------------------------------------" -ForegroundColor Cyan
    Write-Host "Task ${number}:"
    Write-Host $taskText
    Write-Host "--------------------------------------------------" -ForegroundColor Cyan

    if (-not $AllowDirty -and (git status --porcelain)) {
        Write-Host "Abbruch: Das Arbeitsverzeichnis ist nicht sauber. Erst committen/aufraeumen oder mit -AllowDirty starten." -ForegroundColor Red
        git status --short
        break
    }

    $started = Get-Date
    $failure = Invoke-Task $taskText $taskDir
    $duration = "{0:N0} min" -f ((Get-Date) - $started).TotalMinutes

    if ($failure) {
        Write-Host "Task fehlgeschlagen: $failure" -ForegroundColor Red
        $branch = Save-FailedWork $slug $title
        if ($branch) { Write-Host "Aenderungen gesichert auf Branch $branch" -ForegroundColor Yellow }
        $lines[$block.Start] = $lines[$block.Start] -replace '^\s*-', '[FAILED] -'
        $summary += [pscustomobject]@{ Status = "FAILED"; Dauer = $duration; Task = $title; Info = "$failure $(if ($branch) { "-> $branch" })" }
    } else {
        git add .
        if (git status --porcelain) {
            git commit -q -m "feat: $title"
            if ($LASTEXITCODE -ne 0) {
                Write-Host "Fehler: git commit fehlgeschlagen, Skript stoppt." -ForegroundColor Red
                break
            }
        }
        Write-Host "Task erfolgreich und committet." -ForegroundColor Green
        $lines[$block.Start] = $lines[$block.Start] -replace '^\s*-', '[DONE] -'
        $summary += [pscustomobject]@{ Status = "DONE"; Dauer = $duration; Task = $title; Info = "" }
    }

    # Fortschritt sofort speichern, damit er bei Abbruch nicht verloren geht
    Set-Content -Path $PromptFile -Value $lines -Encoding UTF8
    Start-Sleep -Seconds 3
}

Write-Host ""
Write-Host "================ Zusammenfassung ================" -ForegroundColor Cyan
if ($summary.Count -eq 0) {
    Write-Host "Keine offenen Tasks bearbeitet."
} else {
    $summary | Format-Table -AutoSize -Wrap | Out-String | Write-Host
    $summary | Format-Table -AutoSize -Wrap | Out-String | Set-Content (Join-Path $RunDir "summary.txt") -Encoding UTF8
}
Write-Host "Logs: $RunDir"
if ($summary | Where-Object Status -eq "FAILED") { exit 1 }
