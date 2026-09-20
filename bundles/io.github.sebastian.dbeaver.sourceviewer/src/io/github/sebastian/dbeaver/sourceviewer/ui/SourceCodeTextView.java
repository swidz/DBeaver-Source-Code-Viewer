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
package io.github.sebastian.dbeaver.sourceviewer.ui;

import io.github.sebastian.dbeaver.sourceviewer.SourceViewerMessages;
import io.github.sebastian.dbeaver.sourceviewer.config.ColorSpec;
import io.github.sebastian.dbeaver.sourceviewer.config.SourceLanguageDefinition;
import io.github.sebastian.dbeaver.sourceviewer.config.SourceLanguageRegistry;
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

    private String source = "";
    private String columnName;

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
            if (editable) {
                source = text.getText();
                renderStyles();
            }
        });

        refreshDefinitions();
        addDisposeListener(event -> colors.values().forEach(Color::dispose));
    }

    public void setSource(String value, String newColumnName) {
        source = value == null ? "" : value;
        columnName = newColumnName;
        text.setRedraw(false);
        try {
            text.setText(source);
            renderStyles();
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
        if (language == null) {
            text.setStyleRanges(new StyleRange[0]);
            return;
        }
        List<StyleSpan> spans = SourceCodeHighlighter.highlight(text.getText(), language);
        StyleRange[] ranges = new StyleRange[spans.size()];
        for (int index = 0; index < spans.size(); index++) {
            StyleSpan span = spans.get(index);
            ranges[index] = new StyleRange(span.start(), span.length(), getColor(span.color()), null);
        }
        text.setStyleRanges(ranges);
    }

    private Color getColor(ColorSpec color) {
        return colors.computeIfAbsent(color, value -> new Color(getDisplay(), value.red(), value.green(), value.blue()));
    }
}
