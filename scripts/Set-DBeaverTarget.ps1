[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateScript({ Test-Path -LiteralPath (Join-Path $_ 'dbeaver.exe') })]
    [string]$DBeaverHome
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$PluginsPath = Join-Path $DBeaverHome.TrimEnd('\') 'plugins'
$EscapedPluginsPath = [Security.SecurityElement]::Escape($PluginsPath)

$TargetContent = @"
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<?pde version="3.8"?>
<target name="Local DBeaver installation" sequenceNumber="1">
    <locations>
        <location path="$EscapedPluginsPath" type="Directory"/>
    </locations>
</target>
"@

$TargetFiles = @(
    'targets\dbeaver-26.2.target',
    'bundles\io.github.sebastian.dbeaver.sourceviewer\targets\dbeaver-26.2.target',
    'features\io.github.sebastian.dbeaver.sourceviewer.feature\targets\dbeaver-26.2.target',
    'repository\targets\dbeaver-26.2.target'
) | ForEach-Object { Join-Path $ProjectRoot $_ }

foreach ($TargetFile in $TargetFiles) {
    [IO.File]::WriteAllText($TargetFile, $TargetContent, [Text.UTF8Encoding]::new($false))
}

Write-Host "DBeaver target set to $PluginsPath"
