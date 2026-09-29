# P2 distribution verification, 0.1.7

Verified on 2026-09-29 against a disposable copy of DBeaver Community 26.2.1 on Windows, using the existing Microsoft JDK 21. The user's installed DBeaver and database workspace were not modified.

- Maven/Tycho packaging and the existing UDL parser/highlighter regression test passed.
- `Test-UpdateSite.ps1` checked the installable feature, installation category, MIT license, update-repository registration, artifact SHA-256 values, localization, and all four bundled UDL files.
- P2 Director successfully installed the feature from the local repository and from the published HTTPS composite update site.
- `-listInstalledRoots` showed `io.github.sebastian.dbeaver.sourceviewer.feature.feature.group/0.1.7.202609291329`.
- Both artifact and metadata update-repository preferences were registered.
- P2 uninstallation removed the bundle registration.
- After initializing a persistent artifact store in the test copy, a subsequent install placed the JAR in `plugins/`; the installed feature and JAR survived `-clean`.

## Test environment limitations

The local Java runtime failed to initialize Unix-domain loopback sockets during P2 certificate checks. The automated test supplied `-Djdk.net.unixdomain.tmpdir=<test-copy>/socket-temp`. This long, test-local directory caused Java's existing fallback to TCP sockets. Trust checks remained enabled; only the exact local or published project repository URLs were trusted for these unattended tests. No JVM setting was changed in the user's installation.

The copied Windows installation had no root `artifacts.xml`. Eclipse treated its bundle directory as an extension-location repository and initially stored new artifacts under `configuration/org.eclipse.osgi/.../data/`. A cache-clean restart removed those artifacts even though the feature remained in the P2 profile. For the persistent-storage test, an empty standard P2 artifact index was created only in the disposable installation before reinstalling the feature. This host configuration issue is separate from the extension's update-site metadata and was not silently repaired in the user's installation.

These checks verify the package and the P2 installation path. They do not demonstrate that an arbitrary future DBeaver installer preserves extensions. A full application replacement, a profile/cache reset, or an incompatible release can still require restoration. The new installation method records the extension as installed software and provides a stable source for installation and updates; it does not control DBeaver's application installer.
