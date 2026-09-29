[CmdletBinding()]
param(
    [switch]$SkipMaven,
    [string]$DBeaverHome = 'C:\Program Files\DBeaver'
)

$ErrorActionPreference = 'Stop'
Write-Warning 'The direct-install EXE is retired. Building a P2 ZIP for Help > Install New Software instead.'
& (Join-Path $PSScriptRoot 'Build-UpdateSite.ps1') -SkipMaven:$SkipMaven -DBeaverHome $DBeaverHome
