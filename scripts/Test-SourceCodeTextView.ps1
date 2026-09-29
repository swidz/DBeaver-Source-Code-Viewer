[CmdletBinding()]
param([string]$DBeaverHome = 'C:\Program Files\DBeaver')

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$OutputDirectory = Join-Path $ProjectRoot 'target\swt-test'
$BundleClasses = Join-Path $ProjectRoot 'bundles\io.github.sebastian.dbeaver.sourceviewer\target\classes'
if (-not (Test-Path -LiteralPath $BundleClasses)) { throw 'Build the plug-in first.' }
$Libraries = @(Get-ChildItem -LiteralPath (Join-Path $DBeaverHome 'plugins') -Filter '*.jar' -File |
    Where-Object { $_.Name -notlike 'io.github.sebastian.dbeaver.sourceviewer_*' } |
    ForEach-Object { $_.FullName })
$ClassPath = (@($OutputDirectory, $BundleClasses) + $Libraries) -join [IO.Path]::PathSeparator
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
Push-Location $ProjectRoot
try {
    & javac --release 21 -cp $ClassPath -d $OutputDirectory (Join-Path $ProjectRoot 'tests\SourceCodeTextViewTest.java')
    if ($LASTEXITCODE -ne 0) { throw 'SWT viewer test compilation failed.' }
    & java --enable-native-access=ALL-UNNAMED "-Djava.library.path=$DBeaverHome" -cp $ClassPath SourceCodeTextViewTest
    if ($LASTEXITCODE -ne 0) { throw 'SWT viewer regression test failed.' }
} finally {
    Pop-Location
}
