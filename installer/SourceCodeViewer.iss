; Builds an installer which adds this extension to an existing DBeaver installation.
; Run scripts\Build-Installer.ps1 after Maven has produced the P2 update site.

#define ExtensionName "DBeaver Source Code Viewer"
#define ExtensionVersion "0.1.3"

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

[InstallDelete]
Type: filesandordirs; Name: {app}\dropins\source-code-viewer

[Files]
Source: "..\repository\target\repository\*"; DestDir: "{tmp}\dbeaver-source-code-viewer-p2"; Flags: recursesubdirs createallsubdirs deleteafterinstall ignoreversion
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

function InstallExtensionWithP2(): Boolean;
var
  ExitCode: Integer;
  RepositoryPath: String;
  RepositoryUri: String;
  Parameters: String;
begin
  RepositoryPath := ExpandConstant('{tmp}\dbeaver-source-code-viewer-p2');
  StringChangeEx(RepositoryPath, '\', '/', True);
  RepositoryUri := 'file:/' + RepositoryPath;
  Parameters := '-application org.eclipse.equinox.p2.director -repository "' + RepositoryUri +
    '" -installIU io.github.sebastian.dbeaver.sourceviewer.feature.feature.group -profile DefaultProfile';
  Result := Exec(ExpandConstant('{app}\dbeaverc.exe'), Parameters, '', SW_HIDE, ewWaitUntilTerminated, ExitCode);
  if not Result or (ExitCode <> 0) then
  begin
    MsgBox('The extension could not be installed. Close every DBeaver window and run this installer again as Administrator.', mbError, MB_OK);
    Result := False;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if (CurStep = ssPostInstall) and not InstallExtensionWithP2() then
  begin
    RaiseException('DBeaver P2 installation failed.');
  end;
end;