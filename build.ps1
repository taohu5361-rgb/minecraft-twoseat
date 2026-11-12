param([string]$Jdk = $env:JAVA_HOME, [string]$GradleExecutable)
$ErrorActionPreference = 'Stop'
if (-not $Jdk -and (Test-Path 'E:\AI\Phone-MCP-zh\tools\jdk\jdk-17.0.21.0.2+1')) {
    $Jdk = 'E:\AI\Phone-MCP-zh\tools\jdk\jdk-17.0.21.0.2+1'
}
if (-not (Test-Path (Join-Path $Jdk 'bin\javac.exe'))) { throw 'Specify a Java 17 JDK with -Jdk or JAVA_HOME.' }
$env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$env:PATH"
$env:GRADLE_USER_HOME = Join-Path $PSScriptRoot 'tools\gradle-cache'
if (-not $GradleExecutable -and (Test-Path 'E:\AI\Phone-MCP-zh\tools\gradle\gradle-8.13\bin\gradle.bat')) {
    $GradleExecutable = 'E:\AI\Phone-MCP-zh\tools\gradle\gradle-8.13\bin\gradle.bat'
}
if (-not $GradleExecutable) { $GradleExecutable = Join-Path $PSScriptRoot 'mod\gradlew.bat' }
Push-Location (Join-Path $PSScriptRoot 'mod')
try { & $GradleExecutable --no-daemon clean build; if ($LASTEXITCODE) { throw "Gradle failed: $LASTEXITCODE" } }
finally { Pop-Location }
