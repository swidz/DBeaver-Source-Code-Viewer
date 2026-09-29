[CmdletBinding()]
param(
    [switch]$SkipMaven,
    [string]$DBeaverHome = 'C:\Program Files\DBeaver'
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
[xml]$Pom = Get-Content -LiteralPath (Join-Path $ProjectRoot 'pom.xml') -Raw
$ReleaseVersion = $Pom.project.version -replace '-SNAPSHOT$', ''
if ($ReleaseVersion -notmatch '^\d+\.\d+\.\d+$') {
    throw "Unsupported release version: $ReleaseVersion"
}

if (-not $SkipMaven) {
    & (Join-Path $PSScriptRoot 'Set-DBeaverTarget.ps1') -DBeaverHome $DBeaverHome
    $MavenCandidates = @()
    $MavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($null -ne $MavenCommand) { $MavenCandidates += $MavenCommand.Source }
    if ($env:MAVEN_HOME) { $MavenCandidates += Join-Path $env:MAVEN_HOME 'bin\mvn.cmd' }
    $ApachePrograms = Join-Path $env:LOCALAPPDATA 'Programs\Apache'
    if (Test-Path -LiteralPath $ApachePrograms) {
        $MavenCandidates += Get-ChildItem -LiteralPath $ApachePrograms -Directory -Filter 'apache-maven-*' |
            ForEach-Object { Join-Path $_.FullName 'bin\mvn.cmd' }
    }
    $Maven = $MavenCandidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
    if ($null -eq $Maven) { throw 'Apache Maven was not found. Set MAVEN_HOME or add mvn.cmd to PATH.' }
    & $Maven '-Dtycho.p2.httptransport.type=JavaUrl' -f (Join-Path $ProjectRoot 'pom.xml') clean package
    if ($LASTEXITCODE -ne 0) { throw 'Maven package failed.' }
}

$RepositoryDirectory = Join-Path $ProjectRoot 'repository\target\repository'
& (Join-Path $PSScriptRoot 'Test-UpdateSite.ps1') -RepositoryDirectory $RepositoryDirectory
$SourceArchive = Join-Path $ProjectRoot "repository\target\io.github.sebastian.dbeaver.sourceviewer.repository-$($Pom.project.version).zip"
if (-not (Test-Path -LiteralPath $SourceArchive)) { throw 'Build the P2 repository before packaging.' }
$DistDirectory = Join-Path $ProjectRoot 'dist'
New-Item -ItemType Directory -Path $DistDirectory -Force | Out-Null
$ArchivePath = Join-Path $DistDirectory "DBeaver-Source-Code-Viewer-$ReleaseVersion-p2.zip"
Copy-Item -LiteralPath $SourceArchive -Destination $ArchivePath -Force
Write-Host "Update repository: $RepositoryDirectory"
Write-Host "Install with Help > Install New Software > Add > Archive: $ArchivePath"
Get-FileHash -LiteralPath $ArchivePath -Algorithm SHA256
