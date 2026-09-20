; Builds an installer which adds this extension to an existing DBeaver installation.
; Run scripts\Build-Installer.ps1 after Maven has produced the plug-in JAR.

#define ExtensionName "DBeaver Source Code Viewer"
#define ExtensionVersion "0.1.0"

[Setup]
AppId={{4A8B6DBD-5507-4EBB-AFD4-B5400D2D78F0}
AppName={#ExtensionName}
AppVersion={#ExtensionVersion}
AppPublisher=Sebastian
DefaultDirName={autopf}\DBeaver
DisableProgramGroupPage=yes
OutputDir=..\dist\installer
OutputBaseFilename=DBeaver-Source-Code-Viewer-{#ExtensionVersion}-Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
UninstallDisplayName={#ExtensionName}

[Files]
Source: "..\bundles\io.github.sebastian.dbeaver.sourceviewer\target\io.github.sebastian.dbeaver.sourceviewer-*.jar"; DestDir: "{app}\dropins\source-code-viewer\plugins"; Flags: ignoreversion
Source: "..\bundles\io.github.sebastian.dbeaver.sourceviewer\languages\*.xml"; DestDir: "{app}\source-code-viewer\languages"; Flags: ignoreversion

[Code]
function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if CurPageID = wpSelectDir then
  begin
    if not FileExists(AddBackslash(WizardDirValue) + 'dbeaver.exe') then
    begin
      MsgBox('Choose the existing DBeaver installation folder: it must contain dbeaver.exe.', mbError, MB_OK);
      Result := False;
    end;
  end;
end;
