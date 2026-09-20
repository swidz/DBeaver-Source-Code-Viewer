/*
 * Copyright (C) 2026 Sebastian
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package io.github.sebastian.dbeaver.sourceviewer;

import org.eclipse.osgi.util.NLS;

/** Localized UI messages. */
public class SourceViewerMessages extends NLS {
    private static final String BUNDLE_NAME = "io.github.sebastian.dbeaver.sourceviewer.SourceViewerMessages";

    public static String action_reload_languages;
    public static String auto_language;
    public static String no_source_selected;

    static {
        NLS.initializeMessages(BUNDLE_NAME, SourceViewerMessages.class);
    }

    private SourceViewerMessages() {
    }
}
