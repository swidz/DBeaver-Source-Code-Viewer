/*
 * Copyright (C) 2026 Sebastian
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package io.github.sebastian.dbeaver.sourceviewer.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Reads the portable User Defined Language XML format used by Notepad++. */
public final class NotepadPlusPlusUdlReader {
    private static final String COMMENTS = "COMMENTS";
    private static final String NUMBERS = "NUMBERS";
    private static final String OPERATORS = "OPERATORS";
    private static final String KEYWORDS = "KEYWORDS";
    private static final String DELIMITERS = "DELIMITERS";

    private NotepadPlusPlusUdlReader() {
    }

    public static SourceLanguageDefinition read(InputStream input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        Document document = factory.newDocumentBuilder().parse(input);
        Element userLanguage = firstElement(document, "UserLang");
        if (userLanguage == null) {
            throw new IllegalArgumentException("The XML file does not contain a Notepad++ UserLang element.");
        }

        Element global = firstElement(userLanguage, "Global");
        boolean caseIgnored = global != null && Boolean.parseBoolean(global.getAttribute("caseIgnored"));
        SourceLanguageDefinition.Builder definition = new SourceLanguageDefinition.Builder(userLanguage.getAttribute("name"))
            .caseIgnored(caseIgnored)
            .addExtensions(userLanguage.getAttribute("ext"));

        Map<String, ColorSpec> colors = readColors(userLanguage);
        NodeList keywords = userLanguage.getElementsByTagName("Keywords");
        Map<Integer, String> delimiterStarts = new HashMap<>();
        Map<Integer, String> delimiterEnds = new HashMap<>();
        Map<Integer, String> delimiterEscapes = new HashMap<>();

        for (int index = 0; index < keywords.getLength(); index++) {
            Element keyword = (Element) keywords.item(index);
            String name = keyword.getAttribute("name");
            String normalizedName = name.toUpperCase();
            String content = keyword.getTextContent();

            if (COMMENTS.equals(normalizedName)) {
                addComments(definition, content);
            } else if (normalizedName.startsWith(KEYWORDS)) {
                definition.addKeywords(content, colors.getOrDefault(styleName(name), ColorSpec.KEYWORD));
            } else if (normalizedName.startsWith(NUMBERS)) {
                definition.addKeywords(content, colors.getOrDefault("NUMBER", ColorSpec.NUMBER));
            } else if (normalizedName.startsWith(OPERATORS)) {
                definition.addOperators(content);
            } else if (normalizedName.startsWith(DELIMITERS)) {
                readDelimiters(name, content, delimiterStarts, delimiterEnds, delimiterEscapes);
            }
        }

        for (Map.Entry<Integer, String> entry : delimiterStarts.entrySet()) {
            int number = entry.getKey();
            definition.addDelimiter(
                entry.getValue(),
                delimiterEnds.get(number),
                delimiterEscapes.get(number),
                colors.getOrDefault("DELIMITER" + number, ColorSpec.STRING)
            );
        }
        return definition.build();
    }

    private static Map<String, ColorSpec> readColors(Element userLanguage) {
        Map<String, ColorSpec> result = new HashMap<>();
        NodeList styles = userLanguage.getElementsByTagName("WordsStyle");
        for (int index = 0; index < styles.getLength(); index++) {
            Element style = (Element) styles.item(index);
            result.put(style.getAttribute("name").toUpperCase(), ColorSpec.parse(style.getAttribute("fgColor"), ColorSpec.KEYWORD));
        }
        return result;
    }

    private static void addComments(SourceLanguageDefinition.Builder definition, String raw) {
        Map<String, String> values = numberedValues(raw);
        definition.addLineComment(values.get("00"));
        definition.addBlockComment(values.get("02"), values.get("03"));
    }

    private static void readDelimiters(
        String name,
        String raw,
        Map<Integer, String> starts,
        Map<Integer, String> ends,
        Map<Integer, String> escapes
    ) {
        String suffix = name.substring(DELIMITERS.length());
        int number;
        try {
            number = Integer.parseInt(suffix.trim());
        } catch (NumberFormatException ignored) {
            return;
        }
        Map<String, String> values = numberedValues(raw);
        starts.put(number, values.get("00"));
        escapes.put(number, values.get("01"));
        ends.put(number, values.get("02"));
    }

    private static Map<String, String> numberedValues(String raw) {
        Map<String, String> result = new HashMap<>();
        if (raw == null) {
            return result;
        }
        String[] parts = raw.trim().split("\\s+");
        String currentKey = null;
        StringBuilder value = new StringBuilder();
        for (String part : parts) {
            if (part.length() >= 2 && Character.isDigit(part.charAt(0)) && Character.isDigit(part.charAt(1))) {
                if (currentKey != null) {
                    result.put(currentKey, value.toString());
                }
                currentKey = part.substring(0, 2);
                value.setLength(0);
                value.append(part.substring(2));
            } else if (currentKey != null) {
                value.append(' ').append(part);
            }
        }
        if (currentKey != null) {
            result.put(currentKey, value.toString());
        }
        return result;
    }

    private static String styleName(String keywordName) {
        String normalized = keywordName.toUpperCase();
        if (normalized.startsWith(KEYWORDS)) {
            return "KEYWORD" + normalized.substring(KEYWORDS.length());
        }
        return normalized;
    }

    private static Element firstElement(Element parent, String elementName) {
        NodeList children = parent.getElementsByTagName(elementName);
        return children.getLength() == 0 ? null : (Element) children.item(0);
    }

    private static Element firstElement(Document document, String elementName) {
        NodeList elements = document.getElementsByTagName(elementName);
        for (int index = 0; index < elements.getLength(); index++) {
            Node node = elements.item(index);
            if (node instanceof Element element) {
                return element;
            }
        }
        return null;
    }
}
