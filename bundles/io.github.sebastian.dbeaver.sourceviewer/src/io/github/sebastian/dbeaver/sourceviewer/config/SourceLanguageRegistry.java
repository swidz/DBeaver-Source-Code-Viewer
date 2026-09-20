/*
 * Copyright (C) 2026 Sebastian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.sebastian.dbeaver.sourceviewer.config;

import io.github.sebastian.dbeaver.sourceviewer.SourceViewerActivator;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.Log;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/** Loads bundled and administrator-provided Notepad++ UDL definitions. */
public final class SourceLanguageRegistry {
    public static final String INSTALL_LANGUAGE_DIRECTORY = "source-code-viewer/languages";
    public static final String USER_LANGUAGE_DIRECTORY = "languages";

    private static final Log LOG = Log.getLog(SourceLanguageRegistry.class);
    private static final List<String> BUNDLED_DEFINITIONS = List.of("xpp.udl.xml", "csharp.udl.xml", "cpp.udl.xml", "sql.udl.xml");
    private static final SourceLanguageRegistry INSTANCE = new SourceLanguageRegistry();

    private List<SourceLanguageDefinition> languages = List.of();

    private SourceLanguageRegistry() {
        reload();
    }

    public static SourceLanguageRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void reload() {
        Map<String, SourceLanguageDefinition> definitions = new LinkedHashMap<>();
        loadBundledDefinitions(definitions);
        loadDirectory(definitions, getInstallationDirectory());
        loadDirectory(definitions, getUserDirectory());
        languages = definitions.values().stream()
            .sorted(Comparator.comparing(SourceLanguageDefinition::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public synchronized List<SourceLanguageDefinition> getLanguages() {
        return languages;
    }

    public synchronized SourceLanguageDefinition findByName(String languageName) {
        if (languageName == null) {
            return null;
        }
        return languages.stream()
            .filter(language -> language.name().equalsIgnoreCase(languageName))
            .findFirst()
            .orElse(null);
    }

    /** Uses a column alias such as source_xpp or code.cs to pick a language automatically. */
    public synchronized SourceLanguageDefinition inferFromColumnName(String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return languages.isEmpty() ? null : languages.getFirst();
        }
        String normalized = columnName.toLowerCase(Locale.ROOT);
        for (SourceLanguageDefinition language : languages) {
            String name = language.name().toLowerCase(Locale.ROOT);
            if (normalized.contains(name.replace("#", "sharp")) || normalized.contains(name.replace("++", "pp"))) {
                return language;
            }
            for (String extension : language.extensions()) {
                if (normalized.endsWith("_" + extension) || normalized.endsWith("." + extension)) {
                    return language;
                }
            }
        }
        return languages.isEmpty() ? null : languages.getFirst();
    }

    private void loadBundledDefinitions(Map<String, SourceLanguageDefinition> definitions) {
        Bundle bundle = FrameworkUtil.getBundle(SourceLanguageRegistry.class);
        if (bundle == null) {
            return;
        }
        for (String fileName : BUNDLED_DEFINITIONS) {
            try {
                URL url = FileLocator.find(bundle, new org.eclipse.core.runtime.Path("languages/" + fileName), null);
                if (url != null) {
                    try (InputStream stream = url.openStream()) {
                        addDefinition(definitions, NotepadPlusPlusUdlReader.read(stream));
                    }
                }
            } catch (Exception e) {
                LOG.warn("Unable to load bundled source-language definition " + fileName, e);
            }
        }
    }

    private void loadDirectory(Map<String, SourceLanguageDefinition> definitions, Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        try (var files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml"))
                .sorted()
                .forEach(path -> loadFile(definitions, path));
        } catch (Exception e) {
            LOG.warn("Unable to load source-language definitions from " + directory, e);
        }
    }

    private void loadFile(Map<String, SourceLanguageDefinition> definitions, Path file) {
        try (InputStream stream = Files.newInputStream(file)) {
            addDefinition(definitions, NotepadPlusPlusUdlReader.read(stream));
        } catch (Exception e) {
            LOG.warn("Unable to load source-language definition " + file, e);
        }
    }

    private void addDefinition(Map<String, SourceLanguageDefinition> definitions, SourceLanguageDefinition definition) {
        if (definition.name() == null || definition.name().isBlank()) {
            return;
        }
        definitions.put(definition.name().toLowerCase(Locale.ROOT), definition);
    }

    private Path getInstallationDirectory() {
        try {
            if (Platform.getInstallLocation() == null) {
                return null;
            }
            return Path.of(Platform.getInstallLocation().getURL().toURI()).resolve(INSTALL_LANGUAGE_DIRECTORY);
        } catch (Exception e) {
            LOG.debug("Unable to locate the DBeaver installation directory", e);
            return null;
        }
    }

    private Path getUserDirectory() {
        SourceViewerActivator activator = SourceViewerActivator.getDefault();
        if (activator == null) {
            return null;
        }
        return activator.getStateLocation().append(USER_LANGUAGE_DIRECTORY).toFile().toPath();
    }
}
