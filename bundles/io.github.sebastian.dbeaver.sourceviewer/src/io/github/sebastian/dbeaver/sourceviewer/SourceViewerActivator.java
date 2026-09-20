/*
 * Copyright (C) 2026 Sebastian
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package io.github.sebastian.dbeaver.sourceviewer;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/** Plug-in lifecycle entry point. */
public class SourceViewerActivator extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "io.github.sebastian.dbeaver.sourceviewer";

    private static SourceViewerActivator plugin;

    public static SourceViewerActivator getDefault() {
        return plugin;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }
}
