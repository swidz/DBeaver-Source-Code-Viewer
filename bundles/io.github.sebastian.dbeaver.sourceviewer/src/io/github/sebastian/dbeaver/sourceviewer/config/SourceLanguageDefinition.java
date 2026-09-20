/*
 * Copyright (C) 2026 Sebastian
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package io.github.sebastian.dbeaver.sourceviewer.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** A language definition read from a Notepad++ User Defined Language XML file. */
public final class SourceLanguageDefinition {
    private final String name;
    private final boolean caseIgnored;
    private final Set<String> extensions;
    private final Map<String, ColorSpec> keywordColors;
    private final List<String> lineCommentStarts;
    private final List<BlockComment> blockComments;
    private final List<Delimiter> delimiters;
    private final List<String> operators;

    private SourceLanguageDefinition(Builder builder) {
        name = builder.name;
        caseIgnored = builder.caseIgnored;
        extensions = Set.copyOf(builder.extensions);
        keywordColors = Map.copyOf(builder.keywordColors);
        lineCommentStarts = sortLongestFirst(builder.lineCommentStarts);
        blockComments = builder.blockComments.stream()
            .sorted(Comparator.comparingInt((BlockComment value) -> value.start().length()).reversed())
            .toList();
        delimiters = builder.delimiters.stream()
            .sorted(Comparator.comparingInt((Delimiter value) -> value.start().length()).reversed())
            .toList();
        operators = sortLongestFirst(builder.operators);
    }

    public String name() {
        return name;
    }

    public Set<String> extensions() {
        return extensions;
    }

    public ColorSpec keywordColor(String value) {
        return keywordColors.get(normalize(value));
    }

    public List<String> lineCommentStarts() {
        return lineCommentStarts;
    }

    public List<BlockComment> blockComments() {
        return blockComments;
    }

    public List<Delimiter> delimiters() {
        return delimiters;
    }

    public List<String> operators() {
        return operators;
    }

    private String normalize(String value) {
        return caseIgnored ? value.toLowerCase(Locale.ROOT) : value;
    }

    private static List<String> sortLongestFirst(Collection<String> values) {
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();
    }

    public record BlockComment(String start, String end, ColorSpec color) {
    }

    public record Delimiter(String start, String end, String escape, ColorSpec color) {
    }

    public static final class Builder {
        private final String name;
        private boolean caseIgnored;
        private final Set<String> extensions = new LinkedHashSet<>();
        private final Map<String, ColorSpec> keywordColors = new LinkedHashMap<>();
        private final List<String> lineCommentStarts = new ArrayList<>();
        private final List<BlockComment> blockComments = new ArrayList<>();
        private final List<Delimiter> delimiters = new ArrayList<>();
        private final List<String> operators = new ArrayList<>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder caseIgnored(boolean value) {
            caseIgnored = value;
            return this;
        }

        public Builder addExtensions(String extensionList) {
            if (extensionList == null) {
                return this;
            }
            for (String extension : extensionList.trim().split("\\s+")) {
                if (!extension.isBlank()) {
                    extensions.add(extension.toLowerCase(Locale.ROOT));
                }
            }
            return this;
        }

        public Builder addKeywords(String keywordList, ColorSpec color) {
            if (keywordList == null || keywordList.isBlank()) {
                return this;
            }
            for (String keyword : keywordList.trim().split("\\s+")) {
                if (!keyword.isBlank()) {
                    keywordColors.put(normalize(keyword), color);
                }
            }
            return this;
        }

        public Builder addLineComment(String value) {
            if (value != null && !value.isBlank()) {
                lineCommentStarts.add(value);
            }
            return this;
        }

        public Builder addBlockComment(String start, String end) {
            if (start != null && !start.isBlank() && end != null && !end.isBlank()) {
                blockComments.add(new BlockComment(start, end, ColorSpec.COMMENT));
            }
            return this;
        }

        public Builder addDelimiter(String start, String end, String escape, ColorSpec color) {
            if (start != null && !start.isBlank() && end != null && !end.isBlank()) {
                delimiters.add(new Delimiter(start, end, escape == null ? "" : escape, color));
            }
            return this;
        }

        public Builder addOperators(String operatorList) {
            if (operatorList == null || operatorList.isBlank()) {
                return this;
            }
            for (String operator : operatorList.trim().split("\\s+")) {
                if (!operator.isBlank()) {
                    operators.add(operator);
                }
            }
            return this;
        }

        public SourceLanguageDefinition build() {
            return new SourceLanguageDefinition(this);
        }

        private String normalize(String value) {
            return caseIgnored ? value.toLowerCase(Locale.ROOT) : value;
        }
    }
}
