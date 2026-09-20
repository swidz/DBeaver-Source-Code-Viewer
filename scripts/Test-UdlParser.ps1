[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$OutputDirectory = Join-Path $ProjectRoot 'target\udl-test'
$SourceRoot = Join-Path $ProjectRoot 'bundles\io.github.sebastian.dbeaver.sourceviewer\src'

$Sources = @(
    'io\github\sebastian\dbeaver\sourceviewer\config\ColorSpec.java',
    'io\github\sebastian\dbeaver\sourceviewer\config\SourceLanguageDefinition.java',
    'io\github\sebastian\dbeaver\sourceviewer\config\NotepadPlusPlusUdlReader.java',
    'io\github\sebastian\dbeaver\sourceviewer\highlight\StyleSpan.java',
    'io\github\sebastian\dbeaver\sourceviewer\highlight\SourceCodeHighlighter.java'
) | ForEach-Object { Join-Path $SourceRoot $_ }

[IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
Push-Location $ProjectRoot
try {
    & javac --release 21 -d $OutputDirectory @Sources (Join-Path $ProjectRoot 'tests\SourceCodeHighlighterTest.java')
    if ($LASTEXITCODE -ne 0) {
        throw 'UDL parser compilation failed.'
    }

    & java -cp $OutputDirectory SourceCodeHighlighterTest
    if ($LASTEXITCODE -ne 0) {
        throw 'UDL parser regression test failed.'
    }
}
finally {
    Pop-Location
}
