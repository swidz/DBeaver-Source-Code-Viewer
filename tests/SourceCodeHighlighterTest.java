import io.github.sebastian.dbeaver.sourceviewer.config.ColorSpec;
import io.github.sebastian.dbeaver.sourceviewer.config.NotepadPlusPlusUdlReader;
import io.github.sebastian.dbeaver.sourceviewer.config.SourceLanguageDefinition;
import io.github.sebastian.dbeaver.sourceviewer.highlight.SourceCodeHighlighter;
import io.github.sebastian.dbeaver.sourceviewer.highlight.StyleSpan;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Dependency-free regression test for the shipped Notepad++ UDL syntax subset. */
public final class SourceCodeHighlighterTest {
    private SourceCodeHighlighterTest() {
    }

    public static void main(String[] args) throws Exception {
        Path languages = Path.of("bundles", "io.github.sebastian.dbeaver.sourceviewer", "languages");


        try (var files = Files.list(languages)) {
            for (Path path : files.filter(file -> file.getFileName().toString().endsWith(".xml")).toList()) {
                try (InputStream input = Files.newInputStream(path)) {
                    require(!NotepadPlusPlusUdlReader.read(input).extensions().isEmpty(), "No extension declared in " + path);
                }
            }
        }

        SourceLanguageDefinition csharp;
        try (InputStream input = Files.newInputStream(languages.resolve("csharp.udl.xml"))) {
            csharp = NotepadPlusPlusUdlReader.read(input);
        }

        String source = "public class Sample { string value = \"hello\"; // comment\n}";
        List<StyleSpan> spans = SourceCodeHighlighter.highlight(source, csharp);

        require(csharp.extensions().contains("cs"), "C# UDL extension was not read");
        requireSpan(spans, source.indexOf("public"), "public".length(), csharp.keywordColor("public"));
        requireSpan(spans, source.indexOf("\"hello\""), "\"hello\"".length(), csharp.delimiters().getFirst().color());
        requireSpan(spans, source.indexOf("// comment"), "// comment".length(), ColorSpec.COMMENT);
    }

    private static void requireSpan(List<StyleSpan> spans, int start, int length, ColorSpec color) {
        require(spans.stream().anyMatch(span -> span.start() == start && span.length() == length && span.color().equals(color)),
            "Expected syntax color at " + start);
    }

    private static void require(boolean expression, String message) {
        if (!expression) {
            throw new AssertionError(message);
        }
    }
}
