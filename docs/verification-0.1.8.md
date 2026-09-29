# Selection highlighting verification, 0.1.8

Verified on Windows on 2026-09-29 using Microsoft JDK 21 and DBeaver Community 26.2.1 libraries. No additional development tools were installed. Development tests did not change the running DBeaver installation or the user's database workspace. The subsequent approved local installation is recorded below.

## Behavior

- Selecting text adds a subtle, theme-relative background to literal, case-insensitive matches throughout the displayed value.
- Partial words, punctuation, whitespace, multiline selections, overlapping matches, and supplementary Unicode characters are supported. Matching uses locale-independent simple Unicode case comparison, not linguistic expansions such as `ss` matching `ß`.
- Syntax foreground colors are preserved, and SWT retains its normal active-selection appearance.
- Mouse selection events and keyboard selection changes update the matches after an 80 ms debounce. Clearing the selection or changing cells removes the highlights.
- Selection updates reuse cached syntax spans. Matching is linear-time, and overlapping/adjacent match backgrounds are coalesced.

## Checks performed

- Existing shipped-UDL parser/syntax regression test passed.
- Selection tests passed for empty/missing matches, case variants, literal punctuation, partial words, multiline and overlapping matches, Unicode offsets, and Turkish-locale independence.
- 500 deterministic randomized cases checked matching and syntax-color preservation against a simple reference implementation.
- Two one-million-character matching tests, including a 50,001-character repetitive selection, completed in approximately 47-51 ms combined on this machine. This measures the matching checks, not end-to-end rendering time.
- The real shared `SourceCodeTextView` passed a hidden SWT test in light and dark colors: selection events, keyboard selection, clearing, preservation of syntax foregrounds and selection text, pending-update cancellation when changing cells, edits after enabling editing, matching without a language definition, and safe disposal.
- The standalone SWT test supplies the shipped C# definition in its own process because no OSGi application is running there. The registry logs the expected unavailable-platform message in this test; production language loading is unchanged. This is not a manual visual test inside the user's running DBeaver.
- Maven/Tycho build and P2 metadata, localization, language-file, license and checksum checks passed.
- P2 Director upgraded a disposable DBeaver copy from feature `0.1.7.202609291329` to `0.1.8.202609291945`. The new feature and persistent plugin JAR survived a `-clean` startup; the installed JAR checksum matched the built package.
- `git diff --check` passed.

## Package

`dist/DBeaver-Source-Code-Viewer-0.1.8-p2.zip`

SHA-256: `677016365c05990cb9de4b70617ac8caa020a54db16991d22af2aabe07da358e`

The release uses this tested package without rebuilding it. It can be installed with **Help > Install New Software... > Add... > Archive...**, choosing the feature and restarting when prompted. Existing P2 installations can use **Help > Check for Updates** once the release is available on the update site.

## Approved local installation

After the user closed DBeaver on 2026-09-29, its per-user installation state was backed up and verified (452 files; backup retained locally). P2 Director upgraded the actual per-user installation from 0.1.7 to 0.1.8 using a separate diagnostic workspace. A `-clean` startup with DBeaver's bundled Java runtime listed feature `0.1.8.202609291945`; the persistent JAR matched the tested package. All 14 backed-up application-level configuration/P2/launcher files remained unchanged. The user's database workspace and custom language definitions were not modified. After reopening DBeaver, the user confirmed that the feature works.
