/*
 * Copyright (C) 2026 Sebastian
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package io.github.sebastian.dbeaver.sourceviewer.highlight;

import io.github.sebastian.dbeaver.sourceviewer.config.ColorSpec;
import io.github.sebastian.dbeaver.sourceviewer.config.SourceLanguageDefinition;
import java.util.ArrayList;
import java.util.List;

/** Applies the portable, lexical subset of a Notepad++ UDL definition. */
public final class SourceCodeHighlighter {
    private SourceCodeHighlighter() {
    }

    public static List<StyleSpan> highlight(String source, SourceLanguageDefinition language) {
        List<StyleSpan> spans = new ArrayList<>();
        for (int offset = 0; offset < source.length();) {
            String lineComment = startsWith(source, offset, language.lineCommentStarts());
            if (lineComment != null) {
                int end = source.indexOf('\n', offset + lineComment.length());
                addSpan(spans, offset, end < 0 ? source.length() : end, ColorSpec.COMMENT);
                offset = end < 0 ? source.length() : end;
                continue;
            }

            SourceLanguageDefinition.BlockComment blockComment = findBlockComment(source, offset, language.blockComments());
            if (blockComment != null) {
                int close = source.indexOf(blockComment.end(), offset + blockComment.start().length());
                int end = close < 0 ? source.length() : close + blockComment.end().length();
                addSpan(spans, offset, end, blockComment.color());
                offset = end;
                continue;
            }

            SourceLanguageDefinition.Delimiter delimiter = findDelimiter(source, offset, language.delimiters());
            if (delimiter != null) {
                int end = endOfDelimitedValue(source, offset, delimiter);
                addSpan(spans, offset, end, delimiter.color());
                offset = end;
                continue;
            }

            char current = source.charAt(offset);
            if (Character.isJavaIdentifierStart(current) || current == '_') {
                int end = offset + 1;
                while (end < source.length() && (Character.isJavaIdentifierPart(source.charAt(end)) || source.charAt(end) == '_')) {
                    end++;
                }
                ColorSpec color = language.keywordColor(source.substring(offset, end));
                if (color != null) {
                    addSpan(spans, offset, end, color);
                }
                offset = end;
                continue;
            }

            if (Character.isDigit(current)) {
                int end = offset + 1;
                while (end < source.length() && (Character.isDigit(source.charAt(end)) || source.charAt(end) == '.' || source.charAt(end) == '_')) {
                    end++;
                }
                addSpan(spans, offset, end, ColorSpec.NUMBER);
                offset = end;
                continue;
            }

            String operator = startsWith(source, offset, language.operators());
            if (operator != null) {
                addSpan(spans, offset, offset + operator.length(), ColorSpec.OPERATOR);
                offset += operator.length();
                continue;
            }
            offset++;
        }
        return spans;
    }

    private static SourceLanguageDefinition.BlockComment findBlockComment(
        String source,
        int offset,
        List<SourceLanguageDefinition.BlockComment> comments
    ) {
        for (SourceLanguageDefinition.BlockComment comment : comments) {
            if (source.startsWith(comment.start(), offset)) {
                return comment;
            }
        }
        return null;
    }

    private static SourceLanguageDefinition.Delimiter findDelimiter(
        String source,
        int offset,
        List<SourceLanguageDefinition.Delimiter> delimiters
    ) {
        for (SourceLanguageDefinition.Delimiter delimiter : delimiters) {
            if (source.startsWith(delimiter.start(), offset)) {
                return delimiter;
            }
        }
        return null;
    }

    private static int endOfDelimitedValue(String source, int offset, SourceLanguageDefinition.Delimiter delimiter) {
        int cursor = offset + delimiter.start().length();
        while (cursor < source.length()) {
            if (!delimiter.escape().isEmpty() && source.startsWith(delimiter.escape(), cursor)) {
                cursor += delimiter.escape().length();
                if (cursor < source.length()) {
                    cursor++;
                }
                continue;
            }
            if (source.startsWith(delimiter.end(), cursor)) {
                return cursor + delimiter.end().length();
            }
            cursor++;
        }
        return source.length();
    }

    private static String startsWith(String source, int offset, List<String> candidates) {
        for (String candidate : candidates) {
            if (source.startsWith(candidate, offset)) {
                return candidate;
            }
        }
        return null;
    }

    private static void addSpan(List<StyleSpan> spans, int start, int end, ColorSpec color) {
        if (end <= start) {
            return;
        }
        if (!spans.isEmpty()) {
            StyleSpan previous = spans.getLast();
            if (previous.start() + previous.length() == start && previous.color().equals(color)) {
                spans.set(spans.size() - 1, new StyleSpan(previous.start(), previous.length() + end - start, color));
                return;
            }
        }
        spans.add(new StyleSpan(start, end - start, color));
    }
}
