[CmdletBinding()]
param(
    [switch]$SkipMaven,
    [string]$DBeaverHome = 'C:\Program Files\DBeaver'
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot 'Set-DBeaverTarget.ps1') -DBeaverHome $DBeaverHome

if (-not $SkipMaven) {
    & mvn '-Dtycho.p2.httptransport.type=JavaUrl' -f (Join-Path $ProjectRoot 'pom.xml') clean package
    if ($LASTEXITCODE -ne 0) {
        throw 'Maven package failed.'
    }
}

$IsccCandidates = @(
    (Get-Command ISCC.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue),
    (Join-Path $env:LOCALAPPDATA 'Programs\Inno Setup 6\ISCC.exe'),
    (Join-Path ${env:ProgramFiles(x86)} 'Inno Setup 6\ISCC.exe'),
    (Join-Path $env:ProgramFiles 'Inno Setup 6\ISCC.exe')
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

if ($IsccCandidates.Count -eq 0) {
    throw 'Inno Setup 6 was not found. Install JRSoftware.InnoSetup or add ISCC.exe to PATH.'
}

$Iscc = $IsccCandidates | Select-Object -First 1
$BundleJar = Get-ChildItem -LiteralPath (Join-Path $ProjectRoot 'bundles\io.github.sebastian.dbeaver.sourceviewer\target') -Filter 'io.github.sebastian.dbeaver.sourceviewer-*.jar' |
    Select-Object -First 1
if ($null -eq $BundleJar) {
    throw 'The built source-code viewer bundle JAR was not found.'
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$Archive = [System.IO.Compression.ZipFile]::OpenRead($BundleJar.FullName)
try {
    $ManifestEntry = $Archive.GetEntry('META-INF/MANIFEST.MF')
    if ($null -eq $ManifestEntry) {
        throw 'The built source-code viewer bundle has no OSGi manifest.'
    }
    $Reader = [System.IO.StreamReader]::new($ManifestEntry.Open())
    try {
        $Manifest = $Reader.ReadToEnd()
    }
    finally {
        $Reader.Dispose()
    }
}
finally {
    $Archive.Dispose()
}

$VersionMatch = [regex]::Match($Manifest, '(?m)^Bundle-Version:\s*([^\r\n]+)')
if (-not $VersionMatch.Success) {
    throw 'The built source-code viewer bundle has no Bundle-Version manifest entry.'
}
$BundleVersion = $VersionMatch.Groups[1].Value.Trim()
$StagingDirectory = Join-Path $ProjectRoot 'bundles\io.github.sebastian.dbeaver.sourceviewer\target\installer-staging'
New-Item -ItemType Directory -Path $StagingDirectory -Force | Out-Null
$StagedBundle = Join-Path $StagingDirectory "io.github.sebastian.dbeaver.sourceviewer_$BundleVersion.jar"
Copy-Item -LiteralPath $BundleJar.FullName -Destination $StagedBundle -Force
& $Iscc "/DBundleVersion=$BundleVersion" (Join-Path $ProjectRoot 'installer\SourceCodeViewer.iss')
if ($LASTEXITCODE -ne 0) {
    throw 'Inno Setup compilation failed.'
}
