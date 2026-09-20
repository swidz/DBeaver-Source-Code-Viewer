/*
 * Copyright (C) 2026 Sebastian
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package io.github.sebastian.dbeaver.sourceviewer.highlight;

import io.github.sebastian.dbeaver.sourceviewer.config.ColorSpec;

/** One colored character range in a source value. */
public record StyleSpan(int start, int length, ColorSpec color) {
}
