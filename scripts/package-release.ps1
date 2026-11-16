param()

$ErrorActionPreference = 'Stop'
$projectDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$configuration = Get-Content -LiteralPath (Join-Path $projectDirectory 'app/build.gradle.kts') -Raw
$versionName = [regex]::Match($configuration, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
$versionCode = [regex]::Match($configuration, 'versionCode\s*=\s*(\d+)').Groups[1].Value
if ($versionName -notmatch '^\d+\.\d+\.\d+$' -or !$versionCode) { throw 'Version metadata is missing.' }
$destination = Join-Path $projectDirectory "build/releases/$versionName"
[IO.Directory]::CreateDirectory($destination) | Out-Null
$files = @{
    'app/build/outputs/apk/release/app-release.apk' = "DigitalQuran-$versionName-release.apk"
    'app/build/outputs/bundle/release/app-release.aab' = "DigitalQuran-$versionName-playstore.aab"
    "docs/releases/v$versionName.md" = 'RELEASE-NOTES.md'
    'docs/playstore/whats-new-id-ID.txt' = 'whats-new-id-ID.txt'
}
foreach ($entry in $files.GetEnumerator()) {
    $source = Join-Path $projectDirectory $entry.Key
    if (!(Test-Path -LiteralPath $source)) { throw "Missing release file: $($entry.Key)" }
    Copy-Item -LiteralPath $source -Destination (Join-Path $destination $entry.Value) -Force
}
$mapping = Join-Path $projectDirectory 'app/build/outputs/mapping/release/mapping.txt'
if (Test-Path -LiteralPath $mapping) {
    Copy-Item -LiteralPath $mapping -Destination (Join-Path $destination 'mapping.txt') -Force
}
$checksums = foreach ($name in @("DigitalQuran-$versionName-release.apk", "DigitalQuran-$versionName-playstore.aab")) {
    $hash = (Get-FileHash -LiteralPath (Join-Path $destination $name) -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $name"
}
[IO.File]::WriteAllText((Join-Path $destination 'SHA256SUMS.txt'), ($checksums -join "`n") + "`n", [Text.UTF8Encoding]::new($false))
Write-Output "Release $versionName (version code $versionCode) packaged at $destination"
