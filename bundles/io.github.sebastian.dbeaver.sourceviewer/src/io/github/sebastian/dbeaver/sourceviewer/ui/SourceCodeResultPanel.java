/*
 * Copyright (C) 2026 Sebastian
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package io.github.sebastian.dbeaver.sourceviewer.ui;

import io.github.sebastian.dbeaver.sourceviewer.SourceViewerMessages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCellLocation;
import org.jkiss.dbeaver.ui.controls.resultset.panel.ResultSetPanelBase;
import org.jkiss.dbeaver.utils.ContentUtils;

/** Shows syntax-highlighted source for any selected result-set value, including PostgreSQL text columns. */
public class SourceCodeResultPanel extends ResultSetPanelBase {
    private IResultSetPresentation presentation;
    private SourceCodeTextView sourceView;

    @Override
    public Control createContents(IResultSetPresentation newPresentation, Composite parent) {
        presentation = newPresentation;
        sourceView = new SourceCodeTextView(parent, false);
        if (presentation instanceof ISelectionProvider selectionProvider) {
            ISelectionChangedListener listener = event -> {
                if (presentation.getController().getVisiblePanel() == SourceCodeResultPanel.this) {
                    refresh(false);
                }
            };
            selectionProvider.addSelectionChangedListener(listener);
            sourceView.addDisposeListener(event -> selectionProvider.removeSelectionChangedListener(listener));
        }
        return sourceView;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void activatePanel() {
        refresh(true);
    }

    @Override
    public void deactivatePanel() {
        // The panel stays alive while its result tab is open.
    }

    @Override
    public void setFocus() {
        sourceView.setFocus();
    }

    @Override
    public void refresh(boolean force) {
        if (sourceView == null || sourceView.isDisposed()) {
            return;
        }
        ResultSetCellLocation location = presentation.getCurrentCellLocation();
        if (location == null) {
            sourceView.setSource(SourceViewerMessages.no_source_selected, null);
            return;
        }
        Object value = presentation.getController().getModel().getCellValue(location);
        sourceView.setSource(readValue(value), location.getAttribute().getName());
    }

    @Override
    public void contributeActions(IContributionManager manager) {
        manager.add(new Action(SourceViewerMessages.action_reload_languages) {
            @Override
            public void run() {
                sourceView.refreshDefinitions();
                refresh(true);
            }
        });
    }

    @Override
    public boolean needsSeparator() {
        return true;
    }

    private String readValue(Object value) {
        if (value == null) {
            return "";
        }
        try {
            if (value instanceof DBDContent content) {
                return ContentUtils.getContentStringValue(new VoidProgressMonitor(), content);
            }
        } catch (Exception e) {
            return e.getMessage();
        }
        return String.valueOf(value);
    }
}
