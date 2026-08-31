# dev.ps1
# Purpose: Per-repo dev helper. Run FROM a repo root with: .\dev.ps1 -Action compile|build|client|server|stage

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('stage', 'compile', 'build', 'client', 'server')]
    [string]$Action
)

$ErrorActionPreference = 'Stop'

$currentDir = Get-Location
$repoName = Split-Path -Leaf $currentDir

Write-Host "dev.ps1: repo=$repoName action=$Action"

# ============================================================================
# Locate TOOLS directory
# ============================================================================
$toolsPath = $null
try {
    $toolsPath = Resolve-Path (Join-Path $currentDir "..\..\_tools")
} catch {
    throw "ERROR: Cannot locate TOOLS directory at ..\..\._tools relative to $currentDir"
}

Write-Host "  > TOOLS: $toolsPath"

# Verify gradlew.bat exists
$gradlewPath = Join-Path $currentDir "gradlew.bat"
if (-not (Test-Path $gradlewPath)) {
    throw "ERROR: gradlew.bat not found in $currentDir"
}

# ============================================================================
# Gradle Slot Protocol Function
# ============================================================================
function Acquire-GradleSlot {
    param([string]$Context)

    $slotA = Join-Path $toolsPath ".gradle-slot-A"
    $slotB = Join-Path $toolsPath ".gradle-slot-B"
    $timestamp = Get-Date -AsUTC -Format 'o'
    $slotContent = "$Context $timestamp"

    $maxRetries = 30
    $retryCount = 0

    while ($retryCount -lt $maxRetries) {
        # Check if slot A is free
        if (-not (Test-Path $slotA)) {
            Set-Content -Path $slotA -Value $slotContent -Encoding UTF8 -NoNewline
            Write-Host "  > Acquired gradle slot A"
            return $slotA
        }

        # Check if slot B is free
        if (-not (Test-Path $slotB)) {
            Set-Content -Path $slotB -Value $slotContent -Encoding UTF8 -NoNewline
            Write-Host "  > Acquired gradle slot B"
            return $slotB
        }

        # Both slots exist; check if any are stale (> 15 min old)
        $ageA = (Get-Date) - (Get-Item $slotA).LastWriteTime
        $ageB = (Get-Date) - (Get-Item $slotB).LastWriteTime

        if ($ageA.TotalSeconds -gt 900) {
            Set-Content -Path $slotA -Value $slotContent -Encoding UTF8 -NoNewline
            Write-Host "  > Acquired stale gradle slot A (age: $([int]$ageA.TotalSeconds)s)"
            return $slotA
        }

        if ($ageB.TotalSeconds -gt 900) {
            Set-Content -Path $slotB -Value $slotContent -Encoding UTF8 -NoNewline
            Write-Host "  > Acquired stale gradle slot B (age: $([int]$ageB.TotalSeconds)s)"
            return $slotB
        }

        # Both slots are fresh; wait and retry
        $retryCount++
        if ($retryCount -lt $maxRetries) {
            Write-Host "  ! Both gradle slots busy, waiting 60s (retry $retryCount/$maxRetries)..."
            Start-Sleep -Seconds 60
        }
    }

    throw "  ERROR: Failed to acquire gradle slot after $maxRetries retries"
}

# ============================================================================
# Action to gradle task mapping
# ============================================================================
$actionMap = @{
    'stage'   = 'stageRunMods'
    'compile' = 'compileJava'
    'build'   = 'build'
    'client'  = 'runClient'
    'server'  = 'runServer'
}

$gradleTask = $actionMap[$Action]
$useOffline = @('compile', 'build') -contains $Action

# ============================================================================
# Acquire gradle slot and run gradle
# ============================================================================
$slot = $null
$gradleExit = 0

try {
    $slotContext = "$repoName $Action"
    $slot = Acquire-GradleSlot $slotContext

    # Run gradle with appropriate flags
    if ($useOffline) {
        Write-Host "  > Running: .\gradlew.bat $gradleTask --offline --console=plain"
        & .\gradlew.bat $gradleTask --offline --console=plain
        $gradleExit = $LASTEXITCODE

        # Retry without offline if it failed
        if ($gradleExit -ne 0) {
            Write-Host "  ! Gradle failed with offline cache, retrying without --offline..."
            Write-Host "  > Running: .\gradlew.bat $gradleTask --console=plain"
            & .\gradlew.bat $gradleTask --console=plain
            $gradleExit = $LASTEXITCODE
        }
    }
    else {
        Write-Host "  > Running: .\gradlew.bat $gradleTask --console=plain"
        & .\gradlew.bat $gradleTask --console=plain
        $gradleExit = $LASTEXITCODE
    }
}
finally {
    if ($slot -and (Test-Path $slot)) {
        Remove-Item $slot -Force
        Write-Host "  > Released gradle slot"
    }
}

exit $gradleExit
