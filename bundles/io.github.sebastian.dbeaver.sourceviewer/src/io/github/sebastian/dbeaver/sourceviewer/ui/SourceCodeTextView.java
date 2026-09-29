/*
 * Copyright (C) 2026 Sebastian
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package io.github.sebastian.dbeaver.sourceviewer.ui;

import io.github.sebastian.dbeaver.sourceviewer.SourceViewerMessages;
import io.github.sebastian.dbeaver.sourceviewer.config.ColorSpec;
import io.github.sebastian.dbeaver.sourceviewer.config.SourceLanguageDefinition;
import io.github.sebastian.dbeaver.sourceviewer.config.SourceLanguageRegistry;
import io.github.sebastian.dbeaver.sourceviewer.highlight.SelectionHighlighter;
import io.github.sebastian.dbeaver.sourceviewer.highlight.SourceCodeHighlighter;
import io.github.sebastian.dbeaver.sourceviewer.highlight.StyleSpan;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/** SWT control shared by the result panel and the content-viewer integration. */
public class SourceCodeTextView extends Composite {
    private final SourceLanguageRegistry registry = SourceLanguageRegistry.getInstance();
    private final Combo languageCombo;
    private final StyledText text;
    private final Map<ColorSpec, Color> colors = new HashMap<>();
    private final Runnable selectionUpdate = this::renderSelectionStyles;

    private String source = "";
    private String columnName;
    private List<StyleSpan> syntaxSpans = List.of();
    private String lastSelection;

    public SourceCodeTextView(Composite parent, boolean editable) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(1, false));

        languageCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
        languageCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        languageCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                render();
            }
        });

        text = new StyledText(this, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        text.setEditable(editable);
        text.setFont(JFaceResources.getTextFont());
        text.addModifyListener(event -> {
            source = text.getText();
            renderStyles();
        });
        text.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                scheduleSelectionUpdate();
            }
        });
        text.addCaretListener(event -> scheduleSelectionUpdate());

        refreshDefinitions();
        addDisposeListener(event -> {
            getDisplay().timerExec(-1, selectionUpdate);
            colors.values().forEach(Color::dispose);
        });
    }

    public void setSource(String value, String newColumnName) {
        source = value == null ? "" : value;
        columnName = newColumnName;
        text.setRedraw(false);
        try {
            text.setText(source);
        } finally {
            text.setRedraw(true);
        }
    }

    public String getSource() {
        return text.getText();
    }

    public void refreshDefinitions() {
        String selected = languageCombo.getSelectionIndex() > 0 ? languageCombo.getText() : null;
        registry.reload();
        languageCombo.removeAll();
        languageCombo.add(SourceViewerMessages.auto_language);
        for (SourceLanguageDefinition language : registry.getLanguages()) {
            languageCombo.add(language.name());
        }
        selectLanguage(selected);
        renderStyles();
    }

    public void setEditable(boolean editable) {
        text.setEditable(editable);
    }

    @Override
    public boolean setFocus() {
        return text.setFocus();
    }

    private void selectLanguage(String languageName) {
        if (languageName != null) {
            for (int index = 1; index < languageCombo.getItemCount(); index++) {
                if (languageCombo.getItem(index).equalsIgnoreCase(languageName)) {
                    languageCombo.select(index);
                    return;
                }
            }
        }
        languageCombo.select(0);
    }

    private SourceLanguageDefinition getSelectedLanguage() {
        int index = languageCombo.getSelectionIndex();
        if (index <= 0) {
            return registry.inferFromColumnName(columnName);
        }
        return registry.findByName(languageCombo.getItem(index));
    }

    private void render() {
        renderStyles();
    }

    private void renderStyles() {
        if (text.isDisposed()) {
            return;
        }
        SourceLanguageDefinition language = getSelectedLanguage();
        syntaxSpans = language == null ? List.of() : SourceCodeHighlighter.highlight(source, language);
        lastSelection = null;
        getDisplay().timerExec(-1, selectionUpdate);
        renderSelectionStyles();
    }

    private void scheduleSelectionUpdate() {
        // Let SWT finish updating both selection endpoints; coalesce mouse drags
        // and keyboard repeats without reparsing the source's syntax.
        getDisplay().timerExec(-1, selectionUpdate);
        getDisplay().timerExec(80, selectionUpdate);
    }

    private void renderSelectionStyles() {
        if (text.isDisposed()) {
            return;
        }
        String selection = text.getSelectionText();
        if (selection.equals(lastSelection)) {
            return;
        }
        lastSelection = selection;
        List<SelectionHighlighter.Match> matches = SelectionHighlighter.findMatches(source, selection);
        List<SelectionHighlighter.Segment> segments = SelectionHighlighter.overlay(syntaxSpans, matches);
        Color matchBackground = matches.isEmpty() ? null : getMatchBackground();
        StyleRange[] ranges = new StyleRange[segments.size()];
        for (int index = 0; index < segments.size(); index++) {
            SelectionHighlighter.Segment segment = segments.get(index);
            ranges[index] = new StyleRange(segment.start(), segment.length(),
                segment.foreground() == null ? null : getColor(segment.foreground()),
                segment.matched() ? matchBackground : null);
        }
        // SWT paints the active selection over these styles, preserving its
        // normal appearance while other occurrences receive a subtle background.
        text.setStyleRanges(ranges);
    }

    private Color getMatchBackground() {
        RGB background = text.getBackground().getRGB();
        RGB foreground = text.getForeground().getRGB();
        return getColor(new ColorSpec(
            (background.red * 82 + foreground.red * 18) / 100,
            (background.green * 82 + foreground.green * 18) / 100,
            (background.blue * 82 + foreground.blue * 18) / 100
        ));
    }

    private Color getColor(ColorSpec color) {
        return colors.computeIfAbsent(color, value -> new Color(getDisplay(), value.red(), value.green(), value.blue()));
    }
}
