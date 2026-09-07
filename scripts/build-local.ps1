param([string[]]$Tasks = @(':domain:test', ':app:testDebugUnitTest', ':app:assembleDebug', ':app:lintDebug'))
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    if (Test-Path '.tools/jdk') { $env:JAVA_HOME = (Get-ChildItem '.tools/jdk' -Directory | Select-Object -First 1).FullName }
    if (Test-Path '.tools/android-sdk') { $env:ANDROID_HOME = (Resolve-Path '.tools/android-sdk').Path }
    if (Test-Path '.tools/gradle-home') { $env:GRADLE_USER_HOME = (Resolve-Path '.tools/gradle-home').Path }
    New-Item -ItemType Directory -Force '.tools/android-user' | Out-Null
    $env:ANDROID_USER_HOME = (Resolve-Path '.tools/android-user').Path
    if (Test-Path '.tools/gradle/gradle-8.13/bin/gradle.bat') {
        & '.tools/gradle/gradle-8.13/bin/gradle.bat' --no-daemon @Tasks
    } else { & './gradlew.bat' --no-daemon @Tasks }
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }

