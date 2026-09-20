# DBeaver Source Code Viewer

An Eclipse/DBeaver extension that colors source-code values returned by the SQL Script result set.

## What it adds

- **Source Code** in DBeaver's existing Value-panel format selector for text LOB values.
- A **Source Code** Query Result panel for ordinary PostgreSQL `text` and `varchar` cells, which DBeaver does not route through the LOB value viewer.
- A language picker and syntax coloring for X++, C#, C++, and SQL.
- Configuration-only language additions: copy Notepad++ User Defined Language (UDL) XML files to DBeaver's installation folder, then choose **Reload languages**.

The parser reads standard Notepad++ UDL XML and applies its extensions, keyword groups, line/block comments, string delimiters, operators, and foreground colors. Advanced Notepad++ lexer behaviour is intentionally outside this small lexical renderer.

## Use it in DBeaver

1. Install the extension, restart DBeaver, run a query returning a source column.
2. In the Query Result panel selector, choose **Source Code**. This works for normal PostgreSQL `text`/`varchar` result cells.
3. Select the required language in the panel's dropdown.
4. For LOB/content values, open the built-in Value panel and select **Source Code** alongside Text, XML, JSON, HTML, and Binary.

DBeaver does not offer a public hook that replaces the renderer of its standard string Value pane per column. The extension therefore uses both supported extension points: the native Value-pane choice for content values and a supported Query Result panel for regular string values. No DBeaver internals are patched.

## Add a language without development

Copy a Notepad++ UDL XML file to:

`<DBeaver installation>\source-code-viewer\languages\`

For this installation, that is:

`C:\Program Files\DBeaver\source-code-viewer\languages\`

Restart DBeaver or use **Reload languages**. A UDL file with the same language name overrides a bundled definition. The extension also loads UDLs from its per-user DBeaver state folder, so users without administrator rights can add their own definitions.

Bundled examples are in `bundles/io.github.sebastian.dbeaver.sourceviewer/languages`.

## Build and test

This project was compiled against the locally installed **DBeaver Community 26.2.0**, Java 21, Maven 3.9.16, and Inno Setup 6.7.3.

```powershell
.\scripts\Test-UdlParser.ps1
.\scripts\Build-Installer.ps1
```

The installer build creates:

- `repository/target/repository/` - a P2 update site
- `dist/installer/DBeaver-Source-Code-Viewer-0.1.6-Setup.exe` - Windows installer

The project uses the installed DBeaver plug-in directory as its Tycho target platform. If DBeaver is installed elsewhere or is upgraded, update the target files first:

```powershell
.\scripts\Set-DBeaverTarget.ps1 -DBeaverHome 'D:\Apps\DBeaver'
```

The target is deliberately local: DBeaver's public update site distributes installable features but not all internal bundles needed to compile an extension against its APIs.

## Development tools

Installed for this project at user scope:

- Apache Maven 3.9.16, with `MAVEN_HOME` and user PATH configured
- Inno Setup 6.7.3, installed through Winget

The Eclipse IDE for RCP/PDE is optional; Maven/Tycho builds the project, while Eclipse is useful for interactive debugging.
