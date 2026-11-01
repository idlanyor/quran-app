param([switch]$Offline)

$ErrorActionPreference = 'Stop'
$projectDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$credentialFile = Join-Path $projectDirectory 'release-signing\credentials.json'
if (-not (Test-Path -LiteralPath $credentialFile)) {
    throw 'Release credentials are missing from release-signing\credentials.json.'
}
$credentials = Get-Content -LiteralPath $credentialFile -Raw | ConvertFrom-Json
$previousKeystorePath = $env:KEYSTORE_PATH
$previousStorePassword = $env:STORE_PASSWORD
$previousKeyPassword = $env:KEY_PASSWORD
try {
    $env:KEYSTORE_PATH = Join-Path $projectDirectory $credentials.keystore
    $env:STORE_PASSWORD = $credentials.storePassword
    $env:KEY_PASSWORD = $credentials.keyPassword
    $arguments = @(':app:lintRelease', ':app:testDebugUnitTest', ':app:bundleRelease', '--no-configuration-cache')
    if ($Offline) { $arguments += '--offline' }
    Push-Location $projectDirectory
    try {
        & (Join-Path $projectDirectory 'gradlew.bat') @arguments
        if ($LASTEXITCODE -ne 0) { throw "Release build failed with exit code $LASTEXITCODE." }
    } finally { Pop-Location }
} finally {
    $env:KEYSTORE_PATH = $previousKeystorePath
    $env:STORE_PASSWORD = $previousStorePassword
    $env:KEY_PASSWORD = $previousKeyPassword
}
