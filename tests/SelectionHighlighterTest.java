import io.github.sebastian.dbeaver.sourceviewer.config.ColorSpec;
import io.github.sebastian.dbeaver.sourceviewer.highlight.SelectionHighlighter;
import io.github.sebastian.dbeaver.sourceviewer.highlight.SelectionHighlighter.Match;
import io.github.sebastian.dbeaver.sourceviewer.highlight.StyleSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Dependency-free matching, style-composition, Unicode and large-value regression tests. */
public final class SelectionHighlighterTest {
    public static void main(String[] args) {
        expect("Foo foo FOO food", "foo", new Match(0, 3), new Match(4, 3), new Match(8, 3), new Match(12, 3));
        expect("a.b A.B axb", "a.b", new Match(0, 3), new Match(4, 3));
        expect("[X++] [x++]", "[x++]", new Match(0, 5), new Match(6, 5));
        expect("banana", "ana", new Match(1, 5));
        expect("AAAA", "aa", new Match(0, 4));
        expect("a\r\nb A\r\nB", "a\r\nb", new Match(0, 4), new Match(5, 4));
        expect("a  b", " ", new Match(1, 2));
        expect("text", "");
        expect("", "text");
        expect("short", "longer selection");
        expect("abc", "z");
        expect("Żółć ŻÓŁĆ", "żółć", new Match(0, 4), new Match(5, 4));
        expect("\uD801\uDC00x \uD801\uDC28X", "\uD801\uDC28x", new Match(0, 3), new Match(4, 3));
        expect("\uD83D\uDE00Foo FOO", "foo", new Match(2, 3), new Match(6, 3));
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            expect("ITEM item", "item", new Match(0, 4), new Match(5, 4));
        } finally {
            Locale.setDefault(original);
        }
        List<StyleSpan> syntax = List.of(new StyleSpan(0, 6, ColorSpec.KEYWORD), new StyleSpan(8, 5, ColorSpec.STRING));
        verifyOverlay("public  VALUE public", "lic  va", syntax);
        verifyOverlay("public  VALUE public", "", syntax);
        verifyOverlay("public  VALUE public", "public", List.of());
        Random random = new Random(815);
        for (int iteration = 0; iteration < 500; iteration++) {
            StringBuilder source = new StringBuilder();
            String alphabet = "abAB .İıŻż";
            for (int index = 0; index < 150; index++) {
                source.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            int start = random.nextInt(source.length());
            String selection = source.substring(start, Math.min(start + 1 + random.nextInt(12), source.length()));
            List<StyleSpan> styles = new ArrayList<>();
            for (int index = 0; index < source.length() - 5; index += 9) {
                styles.add(new StyleSpan(index, 5, ColorSpec.KEYWORD));
            }
            verifyOverlay(source.toString(), selection, styles);
        }
        long started = System.nanoTime();
        expect("a".repeat(1_000_000) + "B", "a".repeat(50_000) + "b", new Match(950_000, 50_001));
        expect("a".repeat(1_000_000), "a", new Match(0, 1_000_000));
        System.out.println("Selection matching and style tests passed; large-value checks: "
            + (System.nanoTime() - started) / 1_000_000 + " ms");
    }

    private static void expect(String source, String selection, Match... expected) {
        require(SelectionHighlighter.findMatches(source, selection).equals(List.of(expected)),
            "Unexpected matches for selection of length " + selection.length());
    }

    private static void verifyOverlay(String source, String selection, List<StyleSpan> syntax) {
        boolean[] expectedMatches = new boolean[source.length()];
        if (!selection.isEmpty()) {
            for (int index = 0; index <= source.length() - selection.length(); index++) {
                if (source.regionMatches(true, index, selection, 0, selection.length())) {
                    for (int offset = index; offset < index + selection.length(); offset++) {
                        expectedMatches[offset] = true;
                    }
                }
            }
        }
        ColorSpec[] expectedColors = new ColorSpec[source.length()];
        for (StyleSpan style : syntax) {
            for (int index = style.start(); index < style.start() + style.length(); index++) {
                expectedColors[index] = style.color();
            }
        }
        boolean[] actualMatches = new boolean[source.length()];
        ColorSpec[] actualColors = new ColorSpec[source.length()];
        int previousEnd = 0;
        for (var segment : SelectionHighlighter.overlay(syntax, SelectionHighlighter.findMatches(source, selection))) {
            require(segment.start() >= previousEnd && segment.length() > 0, "Overlapping or empty styles");
            previousEnd = segment.start() + segment.length();
            for (int index = segment.start(); index < previousEnd; index++) {
                actualMatches[index] = segment.matched();
                actualColors[index] = segment.foreground();
            }
        }
        require(java.util.Arrays.equals(expectedMatches, actualMatches), "Match overlay differs from literal search");
        require(java.util.Arrays.equals(expectedColors, actualColors), "Syntax colors changed during overlay");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
