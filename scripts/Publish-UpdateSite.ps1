[CmdletBinding()]
param(
    [string]$Remote = 'https://github.com/swidz/DBeaver-Source-Code-Viewer.git'
)
$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$RepositoryDirectory = Join-Path $ProjectRoot 'repository\target\repository'
& (Join-Path $PSScriptRoot 'Test-UpdateSite.ps1') -RepositoryDirectory $RepositoryDirectory
[xml]$Pom = Get-Content -LiteralPath (Join-Path $ProjectRoot 'pom.xml') -Raw
$ReleaseVersion = $Pom.project.version -replace '-SNAPSHOT$', ''
if ($ReleaseVersion -notmatch '^\d+\.\d+\.\d+$') { throw 'Invalid release version.' }
$Checkout = Join-Path $ProjectRoot ("work\publish-update-site-" + [guid]::NewGuid().ToString('N'))
function Invoke-Git {
    & git @args
    if ($LASTEXITCODE -ne 0) { throw "Git failed: $args" }
}
$RemoteBranch = & git ls-remote --heads $Remote refs/heads/update-site
if ($LASTEXITCODE -ne 0) { throw 'Unable to read the update-site branch.' }
if ($RemoteBranch) {
    Invoke-Git clone --single-branch --branch update-site $Remote $Checkout
} else {
    New-Item -ItemType Directory -Path $Checkout -Force | Out-Null
    Invoke-Git -C $Checkout init --initial-branch=update-site
    Invoke-Git -C $Checkout remote add origin $Remote
}
$ReleaseDirectory = Join-Path $Checkout "releases/$ReleaseVersion"
if (Test-Path -LiteralPath $ReleaseDirectory) {
    throw "Version $ReleaseVersion is already published. Published artifacts are immutable; increment the version."
}
New-Item -ItemType Directory -Path $ReleaseDirectory -Force | Out-Null
Get-ChildItem -LiteralPath $RepositoryDirectory | Copy-Item -Destination $ReleaseDirectory -Recurse
$Children = @(Get-ChildItem -LiteralPath (Join-Path $Checkout 'releases') -Directory |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'p2.index') } |
    Sort-Object Name)
$Timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
foreach ($Kind in @('Content', 'Artifacts')) {
    $Type = if ($Kind -eq 'Content') { 'Metadata' } else { 'Artifact' }
    $Xml = [xml]::new()
    $null = $Xml.AppendChild($Xml.CreateXmlDeclaration('1.0', 'UTF-8', $null))
    $Root = $Xml.CreateElement('repository')
    $Root.SetAttribute('name', 'DBeaver Source Code Viewer')
    $Root.SetAttribute('type', "org.eclipse.equinox.internal.p2.$($Type.ToLowerInvariant()).repository.Composite${Type}Repository")
    $Root.SetAttribute('version', '1.0.0')
    $null = $Xml.AppendChild($Root)
    $Properties = $Xml.CreateElement('properties')
    $Properties.SetAttribute('size', '1')
    $Property = $Xml.CreateElement('property')
    $Property.SetAttribute('name', 'p2.timestamp')
    $Property.SetAttribute('value', [string]$Timestamp)
    $null = $Properties.AppendChild($Property)
    $null = $Root.AppendChild($Properties)
    $ChildNodes = $Xml.CreateElement('children')
    $ChildNodes.SetAttribute('size', [string]$Children.Count)
    foreach ($Child in $Children) {
        $Node = $Xml.CreateElement('child')
        $Node.SetAttribute('location', "releases/$($Child.Name)")
        $null = $ChildNodes.AppendChild($Node)
    }
    $null = $Root.AppendChild($ChildNodes)
    $Xml.Save((Join-Path $Checkout "composite$Kind.xml"))
}
$Index = "version=1`nmetadata.repository.factory.order=compositeContent.xml,!`nartifact.repository.factory.order=compositeArtifacts.xml,!`n"
[IO.File]::WriteAllText((Join-Path $Checkout 'p2.index'), $Index, [Text.UTF8Encoding]::new($false))
Copy-Item -LiteralPath (Join-Path $ProjectRoot 'LICENSE') -Destination $Checkout
Invoke-Git -C $Checkout add .
Invoke-Git -C $Checkout commit -m "Publish P2 update site $ReleaseVersion"
Invoke-Git -C $Checkout push origin HEAD:refs/heads/update-site
Write-Host 'Update site: https://raw.githubusercontent.com/swidz/DBeaver-Source-Code-Viewer/update-site/'
Write-Host "Publishing checkout retained for inspection: $Checkout"
