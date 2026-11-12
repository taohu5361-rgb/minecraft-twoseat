param([string]$Jdk = $env:JAVA_HOME, [string]$GradleExecutable)
$ErrorActionPreference = 'Stop'
if (-not $Jdk) { $Jdk = 'E:\AI\Phone-MCP-zh\tools\jdk\jdk-17.0.21.0.2+1' }
$env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$env:PATH"
$env:GRADLE_USER_HOME = Join-Path $PSScriptRoot 'tools\gradle-cache'
if (-not $GradleExecutable) { $GradleExecutable = 'E:\AI\Phone-MCP-zh\tools\gradle\gradle-8.13\bin\gradle.bat' }
if (-not (Test-Path $GradleExecutable)) { $GradleExecutable = Join-Path $PSScriptRoot 'mod\gradlew.bat' }
Push-Location (Join-Path $PSScriptRoot 'mod')
try {
    & $GradleExecutable --no-daemon clean build syncControlifyDevBase
    if ($LASTEXITCODE) { throw "Preparation failed: $LASTEXITCODE" }
    & $GradleExecutable --no-daemon runClient
    if ($LASTEXITCODE) { throw "Client failed: $LASTEXITCODE" }
}
finally { Pop-Location }
