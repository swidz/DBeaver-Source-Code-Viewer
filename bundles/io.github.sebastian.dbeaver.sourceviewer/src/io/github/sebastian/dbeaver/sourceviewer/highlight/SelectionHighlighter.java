/*
 * Copyright (C) 2026 Sebastian
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package io.github.sebastian.dbeaver.sourceviewer.highlight;

import io.github.sebastian.dbeaver.sourceviewer.config.ColorSpec;
import java.util.ArrayList;
import java.util.List;

/** Literal, case-insensitive selection matches, expressed in SWT's UTF-16 offsets. */
public final class SelectionHighlighter {
    private SelectionHighlighter() {
    }

    public record Match(int start, int length) {
        public int end() {
            return start + length;
        }
    }

    public record Segment(int start, int length, ColorSpec foreground, boolean matched) {
    }

    public static List<Match> findMatches(String source, String selection) {
        if (selection.isEmpty() || source.isEmpty()) {
            return List.of();
        }
        int[] pattern = selection.codePoints().map(SelectionHighlighter::foldCase).toArray();
        int[] prefix = new int[pattern.length];
        for (int index = 1, matched = 0; index < pattern.length; index++) {
            while (matched > 0 && pattern[index] != pattern[matched]) {
                matched = prefix[matched - 1];
            }
            if (pattern[index] == pattern[matched]) {
                matched++;
            }
            prefix[index] = matched;
        }

        List<Match> matches = new ArrayList<>();
        int[] offsets = new int[pattern.length];
        // KMP keeps long or repetitive selections linear-time. A ring of offsets
        // preserves UTF-16 positions even for supplementary Unicode characters.
        for (int offset = 0, pointIndex = 0, matched = 0; offset < source.length(); pointIndex++) {
            int point = source.codePointAt(offset);
            offsets[pointIndex % pattern.length] = offset;
            offset += Character.charCount(point);
            int folded = foldCase(point);
            while (matched > 0 && folded != pattern[matched]) {
                matched = prefix[matched - 1];
            }
            if (folded == pattern[matched]) {
                matched++;
            }
            if (matched == pattern.length) {
                int start = offsets[(pointIndex + 1 - pattern.length) % pattern.length];
                if (!matches.isEmpty() && matches.getLast().end() >= start) {
                    Match previous = matches.removeLast();
                    matches.add(new Match(previous.start(), offset - previous.start()));
                } else {
                    matches.add(new Match(start, offset - start));
                }
                // Include overlapping occurrences, e.g. "ana" in "banana".
                matched = prefix[matched - 1];
            }
        }
        return matches;
    }

    private static int foldCase(int point) {
        return Character.toLowerCase(Character.toUpperCase(point));
    }

    /** Merge sorted, non-overlapping syntax spans and match ranges without losing foreground colors. */
    public static List<Segment> overlay(List<StyleSpan> syntax, List<Match> matches) {
        List<Segment> result = new ArrayList<>();
        int styleIndex = 0;
        int matchIndex = 0;
        int position = 0;
        while (styleIndex < syntax.size() || matchIndex < matches.size()) {
            StyleSpan style = styleIndex < syntax.size() ? syntax.get(styleIndex) : null;
            Match match = matchIndex < matches.size() ? matches.get(matchIndex) : null;
            if (style != null && style.start() + style.length() <= position) {
                styleIndex++;
                continue;
            }
            if (match != null && match.end() <= position) {
                matchIndex++;
                continue;
            }
            boolean styled = style != null && style.start() <= position;
            boolean highlighted = match != null && match.start() <= position;
            int styleBoundary = style == null ? Integer.MAX_VALUE
                : styled ? style.start() + style.length() : style.start();
            int matchBoundary = match == null ? Integer.MAX_VALUE
                : highlighted ? match.end() : match.start();
            int end = Math.min(styleBoundary, matchBoundary);
            if (styled || highlighted) {
                result.add(new Segment(position, end - position, styled ? style.color() : null, highlighted));
            }
            position = end;
        }
        return result;
    }
}
