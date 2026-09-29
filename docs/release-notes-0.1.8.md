## New: highlight matching selections

Select text in the Source Code viewer to lightly highlight every matching occurrence in the displayed value, ignoring case.

- Works in the Source Code query-result panel and the Source Code viewer for content/LOB values.
- Preserves syntax colors and the normal active-selection appearance.
- Uses subtle backgrounds suited to light and dark themes.
- Updates with mouse or keyboard selection; clears when deselected or when another result cell is displayed.
- Supports literal text, partial words, punctuation, multiline selections, Unicode, and overlapping matches.
- Reuses syntax colors and debounces selection changes to keep the viewer responsive.

C#, C++, SQL, and X++ definitions remain included. Additional Notepad++ UDL files can still be added without development.

## Install or update

Existing managed installations: choose **Help > Check for Updates**, select Source Code Viewer 0.1.8, and restart DBeaver.

For a new installation, use **Help > Install New Software...** with:

```text
https://raw.githubusercontent.com/swidz/DBeaver-Source-Code-Viewer/update-site/
```

Alternatively, download **DBeaver-Source-Code-Viewer-0.1.8-p2.zip** below and select it through **Add... > Archive...**. Do not use GitHub's automatically generated source-code ZIP. A separate Windows EXE installer is not required. The extension is unsigned; approve trust prompts only if you trust this project.

## Verification

Tested on Windows with DBeaver Community 26.2.1, including automated matching and text-control tests, upgrading from 0.1.7, and a cache-refresh startup. The feature was also confirmed working in the user's DBeaver installation. See the [verification report](https://github.com/swidz/DBeaver-Source-Code-Viewer/blob/v0.1.8/docs/verification-0.1.8.md).

SHA-256 for the P2 ZIP:

```text
677016365c05990cb9de4b70617ac8caa020a54db16991d22af2aabe07da358e
```
