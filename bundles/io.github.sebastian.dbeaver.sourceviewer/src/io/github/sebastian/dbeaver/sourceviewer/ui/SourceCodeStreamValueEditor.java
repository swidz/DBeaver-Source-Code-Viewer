/*
 * Copyright (C) 2026 Sebastian
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package io.github.sebastian.dbeaver.sourceviewer.ui;

import io.github.sebastian.dbeaver.sourceviewer.SourceViewerMessages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.storage.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.utils.ContentUtils;

/** The Source Code editor hosted by DBeaver's standard Value viewer. */
public class SourceCodeStreamValueEditor implements IStreamValueEditor<SourceCodeTextView> {
    private final IValueController controller;

    public SourceCodeStreamValueEditor(IValueController controller) {
        this.controller = controller;
    }

    @Override
    public SourceCodeTextView createControl(IValueController valueController) {
        return new SourceCodeTextView(valueController.getEditPlaceholder(), !valueController.isReadOnly());
    }

    @Override
    public void extractEditorValue(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SourceCodeTextView control,
        @NotNull DBDContent value
    ) throws DBException {
        value.updateContents(monitor, new StringContentStorage(control.getSource()));
    }

    @Override
    public void primeEditorValue(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SourceCodeTextView control,
        @NotNull DBDContent value
    ) throws DBException {
        String source = value.isNull() ? "" : ContentUtils.getContentStringValue(monitor, value);
        control.setSource(source, controller.getValueType().getTypeName());
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull SourceCodeTextView control) {
        manager.add(new Action(SourceViewerMessages.action_reload_languages) {
            @Override
            public void run() {
                control.refreshDefinitions();
            }
        });
    }

    @Override
    public void contributeSettings(@NotNull IContributionManager manager, @NotNull SourceCodeTextView control) {
        contributeActions(manager, control);
    }
}
