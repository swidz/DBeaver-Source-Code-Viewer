; Builds an installer which adds this extension to an existing DBeaver installation.
; It registers the standalone bundle in Eclipse's simple configurator because this DBeaver build does not activate drop-ins.

#define ExtensionName "DBeaver Source Code Viewer"
#define ExtensionVersion "0.1.5"
#ifndef BundleVersion
  #define BundleVersion "0.0.0"
#endif

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
Type: files; Name: {app}\plugins\io.github.sebastian.dbeaver.sourceviewer_*.jar

[Files]
Source: "..\bundles\io.github.sebastian.dbeaver.sourceviewer\target\installer-staging\io.github.sebastian.dbeaver.sourceviewer_{#BundleVersion}.jar"; DestDir: "{app}\plugins"; Flags: ignoreversion
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

function RegisterBundle(): Boolean;
var
  RegistryFile: String;
  PluginDirectory: String;
  PluginFileName: String;
  BundleVersion: String;
  NewEntry: String;
  ExistingLines: TArrayOfString;
  NewLines: TArrayOfString;
  FindRec: TFindRec;
  I: Integer;
  Count: Integer;
begin
  Result := False;
  RegistryFile := ExpandConstant('{app}\configuration\org.eclipse.equinox.simpleconfigurator\bundles.info');
  PluginDirectory := ExpandConstant('{app}\plugins');
  if not LoadStringsFromFile(RegistryFile, ExistingLines) then
  begin
    MsgBox('DBeaver bundle registry was not found: ' + RegistryFile, mbError, MB_OK);
    exit;
  end;
  if not FindFirst(AddBackslash(PluginDirectory) + 'io.github.sebastian.dbeaver.sourceviewer_*.jar', FindRec) then
  begin
    MsgBox('The source-code viewer plug-in JAR was not copied to DBeaver.', mbError, MB_OK);
    exit;
  end;
  PluginFileName := FindRec.Name;
  FindClose(FindRec);
  BundleVersion := Copy(PluginFileName, Length('io.github.sebastian.dbeaver.sourceviewer_') + 1, Length(PluginFileName) - Length('io.github.sebastian.dbeaver.sourceviewer_') - Length('.jar'));
  NewEntry := 'io.github.sebastian.dbeaver.sourceviewer,' + BundleVersion + ',plugins/' + PluginFileName + ',4,false';
  SetArrayLength(NewLines, Length(ExistingLines) + 1);
  Count := 0;
  for I := 0 to GetArrayLength(ExistingLines) - 1 do
  begin
    if Pos('io.github.sebastian.dbeaver.sourceviewer,', ExistingLines[I]) <> 1 then
    begin
      NewLines[Count] := ExistingLines[I];
      Count := Count + 1;
    end;
  end;
  NewLines[Count] := NewEntry;
  Count := Count + 1;
  SetArrayLength(NewLines, Count);
  Result := SaveStringsToFile(RegistryFile, NewLines, False);
  if not Result then
    MsgBox('DBeaver bundle registry could not be updated.', mbError, MB_OK);
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if (CurStep = ssPostInstall) and not RegisterBundle() then
    RaiseException('DBeaver bundle registration failed.');
end;