[CmdletBinding()]
param(
    [string]$RepositoryDirectory = (Join-Path (Split-Path -Parent $PSScriptRoot) 'repository\target\repository')
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Read-ZipText([string]$ArchivePath, [string]$EntryName) {
    $Archive = [IO.Compression.ZipFile]::OpenRead($ArchivePath)
    try {
        $Entry = $Archive.GetEntry($EntryName)
        if ($null -eq $Entry) { throw "$EntryName is missing in $ArchivePath" }
        $Reader = [IO.StreamReader]::new($Entry.Open())
        try { $Reader.ReadToEnd() } finally { $Reader.Dispose() }
    } finally { $Archive.Dispose() }
}
[xml]$Content = Read-ZipText (Join-Path $RepositoryDirectory 'content.jar') 'content.xml'
[xml]$Artifacts = Read-ZipText (Join-Path $RepositoryDirectory 'artifacts.jar') 'artifacts.xml'
$FeatureId = 'io.github.sebastian.dbeaver.sourceviewer.feature.feature.group'
$Feature = @($Content.repository.units.unit | Where-Object { $_.id -eq $FeatureId })
if ($Feature.Count -ne 1) { throw 'The repository must contain exactly one installable Source Code Viewer feature.' }
if ($Feature[0].licenses.license.InnerText -notmatch 'MIT License') { throw 'The feature license is missing.' }
if ($Feature[0].OuterXml -notmatch 'addRepository') { throw 'The feature must register its update repository.' }
$Category = @($Content.repository.units.unit | Where-Object {
    @($_.properties.property | Where-Object { $_.name -eq 'org.eclipse.equinox.p2.type.category' -and $_.value -eq 'true' }).Count -gt 0
})
if ($Category.Count -lt 1 -or $Category[0].OuterXml -notmatch [regex]::Escape($FeatureId)) {
    throw 'The feature is missing from the installation category.'
}
foreach ($Artifact in $Artifacts.repository.artifacts.artifact) {
    $Folder = switch ($Artifact.classifier) {
        'osgi.bundle' { 'plugins' }
        'org.eclipse.update.feature' { 'features' }
        default { throw "Unexpected artifact classifier: $($Artifact.classifier)" }
    }
    $JarPath = Join-Path $RepositoryDirectory "$Folder/$($Artifact.id)_$($Artifact.version).jar"
    if (-not (Test-Path -LiteralPath $JarPath)) { throw "Repository artifact is missing: $JarPath" }
    $Sha = $Artifact.properties.property | Where-Object { $_.name -eq 'download.checksum.sha-256' }
    if (-not $Sha -or (Get-FileHash -LiteralPath $JarPath -Algorithm SHA256).Hash -ne $Sha.value) {
        throw "Artifact checksum mismatch: $JarPath"
    }
    if ($Artifact.classifier -eq 'osgi.bundle') {
        $Manifest = Read-ZipText $JarPath 'META-INF/MANIFEST.MF'
        if ($Manifest -notmatch 'Bundle-Localization: plugin') { throw 'Plug-in localization is missing.' }
        $null = Read-ZipText $JarPath 'plugin.properties'
        $BundleLicense = Read-ZipText $JarPath 'LICENSE'
        if ($BundleLicense -notmatch 'MIT License') { throw 'The plug-in license is missing.' }
        foreach ($Language in @('cpp', 'csharp', 'sql', 'xpp')) {
            [xml]$Definition = Read-ZipText $JarPath "languages/$Language.udl.xml"
            if (-not $Definition.NotepadPlus.UserLang) { throw "Invalid bundled UDL: $Language" }
        }
    }
}
Write-Host "P2 repository checks passed: $FeatureId $($Feature[0].version)"
