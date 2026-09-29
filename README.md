# DBeaver Source Code Viewer

An Eclipse/DBeaver extension that colors source-code values returned by the SQL Script result set.

## What it adds

- **Source Code** in DBeaver's existing Value-panel format selector for text LOB values.
- A **Source Code** Query Result panel for ordinary PostgreSQL `text` and `varchar` cells.
- Currently included language definitions: **C#**, **C++**, **SQL**, and **X++**.
- Other language definitions can be downloaded from the [Notepad++ User Defined Languages Collection](https://github.com/notepad-plus-plus/userDefinedLanguages). Add a UDL XML file, then choose **Reload languages**.

The parser supports UDL extensions, keyword groups, line/block comments, string delimiters, operators, and foreground colors. Advanced Notepad++ lexer behaviour is outside this small lexical renderer; not every UDL will render identically to Notepad++.

## Install through DBeaver (recommended)

Starting with **0.1.7**, install using DBeaver's Eclipse plug-in manager. A separate Windows installer is no longer needed.

1. Open **Help > Install New Software...**.
2. Click **Add...**, name the repository **DBeaver Source Code Viewer**, and use this location:

   ```text
   https://raw.githubusercontent.com/swidz/DBeaver-Source-Code-Viewer/update-site/
   ```

3. Select **DBeaver extensions > DBeaver Source Code Viewer**.
4. Click **Next**, review and accept the MIT license, and finish the installation.
5. The extension is unsigned. Review any trust prompt and approve this extension if you trust this repository.
6. Restart DBeaver.

The four included language definitions are bundled in the plug-in. No additional files or development tools are needed.

For DBeaver installed in a protected folder such as `C:\Program Files\DBeaver`, installation may require running DBeaver as Administrator. See [DBeaver's extension installation documentation](https://dbeaver.com/docs/dbeaver/Eclipse-extensions/).

### Install from a ZIP

Download `DBeaver-Source-Code-Viewer-0.1.7-p2.zip` from the [releases page](https://github.com/swidz/DBeaver-Source-Code-Viewer/releases/latest). In **Help > Install New Software... > Add... > Archive...**, select that ZIP, then follow steps 3-6 above. Do not select GitHub's automatically generated source-code ZIP. All DBeaver dependencies must already be installed.

### Upgrade, migration, and removal

The old 0.1.6 EXE copied a JAR and edited DBeaver's `bundles.info` directly. DBeaver could overwrite that file during an update, and the extension was not recorded as an installed feature. That installer is retired.

If the extension disappeared after a DBeaver update, install 0.1.7 through the instructions above. If the old extension is still present, the newer version uses the same plug-in identity. After restarting, confirm **DBeaver Source Code Viewer** appears under **Help > About DBeaver > Installation Details > Installed Software**. Do not run the old EXE installer over a managed installation.

The update site is registered for future extension updates. Use **Help > Check for Updates**, and use **Installation Details > Installed Software > Uninstall** to remove the feature.

P2 records the installed extension and manages compatible updates. It does **not** guarantee survival of a full DBeaver replacement: on Windows, DBeaver's application updater downloads and runs a new installer, which can replace the installation or its profile. A clean reinstall or incompatible DBeaver release may still require reinstalling the extension using the same URL. See [DBeaver's application update documentation](https://dbeaver.com/docs/dbeaver/Installation/#automatic-updates).

## Use it in DBeaver

1. Run a query returning a source-code column.
2. Select a source-code cell, then choose **Panels > Source Code** in the query results.
3. Select the required language in the panel's dropdown.
4. For LOB/content values, the built-in Value panel also offers **Source Code**.

DBeaver's standard string Value pane has no public hook for adding a format per column. This extension uses a Query Result panel for regular strings and the existing Value-pane format extension for content values.

## Add a language without development

Download a UDL XML file from the [Notepad++ User Defined Languages Collection](https://github.com/notepad-plus-plus/userDefinedLanguages). Save the **Raw** XML, not the GitHub HTML page.

For user-specific definitions, create this folder inside your DBeaver workspace:

```text
<workspace>/.metadata/.plugins/io.github.sebastian.dbeaver.sourceviewer/languages/
```

For the default Windows workspace, this is usually:

```text
%APPDATA%\DBeaverData\workspace6\.metadata\.plugins\io.github.sebastian.dbeaver.sourceviewer\languages\
```

This location is outside the DBeaver application folder. Keep your workspace when upgrading or reinstalling DBeaver.

The original installation-wide location is also supported:

```text
<DBeaver installation>\source-code-viewer\languages\
```

Restart DBeaver or choose **Reload language definitions** in the Source Code panel. Files with the same language name override bundled definitions; user definitions also override installation-wide definitions.

Bundled examples are in `bundles/io.github.sebastian.dbeaver.sourceviewer/languages`.

## Build and publish

Version 0.1.7 is built against **DBeaver Community 26.2.1**, with Java 21 and Maven 3.9.16. Inno Setup is no longer required.

```powershell
.\scripts\Test-UdlParser.ps1
.\scripts\Build-UpdateSite.ps1 -DBeaverHome 'C:\Program Files\DBeaver'
```

The build produces:

- `repository/target/repository/` - the P2 repository (feature, plug-in, and metadata).
- `dist/DBeaver-Source-Code-Viewer-0.1.7-p2.zip` - an archive for DBeaver's **Install New Software** dialog.

`Build-UpdateSite.ps1` updates the local Tycho target, runs Maven, checks artifact checksums and installation metadata, and copies the archive. `Build-Installer.ps1` remains as a deprecated alias for the new build; it no longer produces an EXE.

Maintainers can publish the built repository with:

```powershell
.\scripts\Publish-UpdateSite.ps1
```

This pushes only generated P2 files to the separate `update-site` branch using the existing Git credentials. It preserves earlier releases in versioned folders and updates the composite repository. Published versions cannot be overwritten; increment the version for each release. Upload the generated P2 ZIP as a GitHub Release asset.

The project uses the locally installed DBeaver plug-ins as its Tycho target. Eclipse IDE for RCP/PDE is optional.

## License

This project is released under the [MIT License](LICENSE).
