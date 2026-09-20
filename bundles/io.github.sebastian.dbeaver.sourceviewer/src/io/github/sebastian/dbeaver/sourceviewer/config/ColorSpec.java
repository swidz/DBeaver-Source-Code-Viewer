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

/** An RGB color stored in a Notepad++ UDL-compatible form. */
public record ColorSpec(int red, int green, int blue) {

    public static final ColorSpec COMMENT = new ColorSpec(106, 153, 85);
    public static final ColorSpec KEYWORD = new ColorSpec(86, 156, 214);
    public static final ColorSpec NUMBER = new ColorSpec(181, 206, 168);
    public static final ColorSpec OPERATOR = new ColorSpec(212, 212, 212);
    public static final ColorSpec STRING = new ColorSpec(206, 145, 120);

    public static ColorSpec parse(String value, ColorSpec fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().replace("#", "");
        if (!normalized.matches("(?i)[0-9a-f]{6}")) {
            return fallback;
        }
        return new ColorSpec(
            Integer.parseInt(normalized.substring(0, 2), 16),
            Integer.parseInt(normalized.substring(2, 4), 16),
            Integer.parseInt(normalized.substring(4, 6), 16)
        );
    }
}
