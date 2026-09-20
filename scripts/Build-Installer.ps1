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
& $Iscc (Join-Path $ProjectRoot 'installer\SourceCodeViewer.iss')
if ($LASTEXITCODE -ne 0) {
    throw 'Inno Setup compilation failed.'
}
