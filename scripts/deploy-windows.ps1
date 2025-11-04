<#
PowerShell deployment script for NatclinnWebService
Usage:
  .\deploy-windows.ps1 [-ProjectDir <path>] [-TomcatDir <path>] [-WarName <name>] [-SkipTests]

Defaults assume this repository layout and Tomcat at C:\apache-tomcat\apache-tomcat-10.1.48
#>
param(
    [string]$ProjectDir = "C:\Users\conde-salazar\Documents\GitHub\Natcl-inn-Ontology",
    [string]$TomcatDir = "C:\apache-tomcat\apache-tomcat-10.1.48",
    [string]$WarName = "NatclinnWebService-1.0-SNAPSHOT.war",
    [switch]$SkipTests
)

function Write-Log {
    param([string]$msg)
    $ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$ts] $msg"
}

Set-Location -Path $ProjectDir

# 1) Build
Write-Log "Starting Maven build (skip tests: $($SkipTests.IsPresent))"
$mvnArgs = @('-f', "$ProjectDir\pom.xml", 'clean', 'package')
if ($SkipTests.IsPresent) { $mvnArgs += '-DskipTests' }

& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) {
    Write-Log "Maven build failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}
Write-Log "Maven build succeeded"

# 2) Path to WAR
$srcWar = Join-Path -Path $ProjectDir -ChildPath "target\$WarName"
if (-not (Test-Path $srcWar)) {
    Write-Log "WAR not found: $srcWar"
    exit 2
}

$destWar = Join-Path -Path $TomcatDir -ChildPath "webapps\NatclinnWebService.war"

# 3) Stop Tomcat
$shutdown = Join-Path $TomcatDir 'bin\shutdown.bat'
$startup = Join-Path $TomcatDir 'bin\startup.bat'
if (Test-Path $shutdown) {
    Write-Log "Stopping Tomcat using $shutdown"
    & cmd /c `"$shutdown`"
    Start-Sleep -Seconds 2
} else {
    Write-Log "shutdown.bat not found at $shutdown"
}

# Wait for Tomcat to release webapps (optional wait loop)
Start-Sleep -Seconds 1

# 4) Copy WAR
try {
    Write-Log "Copying $srcWar to $destWar"
    Copy-Item -Path $srcWar -Destination $destWar -Force -ErrorAction Stop
    Write-Log "WAR copied successfully"
} catch {
    Write-Log "Failed to copy WAR: $_"
    exit 3
}

# 5) Start Tomcat
if (Test-Path $startup) {
    Write-Log "Starting Tomcat using $startup"
    & cmd /c `"$startup`"
    Write-Log "Tomcat startup triggered"
} else {
    Write-Log "startup.bat not found at $startup"
}

Write-Log "Deployment script finished"
exit 0
