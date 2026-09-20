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

import org.eclipse.ui.IEditorPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IStreamValueManager;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.stream.TextEditorPart;
import org.jkiss.dbeaver.utils.ContentUtils;

/** Registers Source Code in DBeaver's built-in content-viewer selector. */
public class SourceCodeStreamValueManager implements IStreamValueManager {
    @NotNull
    @Override
    public MatchType matchesTo(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSTypedObject attribute,
        @Nullable DBDContent value
    ) {
        return ContentUtils.isTextContent(value) ? MatchType.APPLIES : MatchType.NONE;
    }

    @NotNull
    @Override
    public IStreamValueEditor createPanelEditor(@NotNull IValueController controller) {
        return new SourceCodeStreamValueEditor(controller);
    }

    /**
     * DBeaver opens a generic text editor for the separate external-editor action.
     * Source coloring is provided in the Value viewer and the Source Code result panel.
     */
    @NotNull
    @Override
    public IEditorPart createEditorPart(@NotNull IValueController controller) {
        return new TextEditorPart();
    }
}
