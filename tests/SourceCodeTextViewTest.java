import io.github.sebastian.dbeaver.sourceviewer.config.NotepadPlusPlusUdlReader;
import io.github.sebastian.dbeaver.sourceviewer.config.SourceLanguageRegistry;
import io.github.sebastian.dbeaver.sourceviewer.ui.SourceCodeTextView;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/** Exercises the real shared viewer on a hidden SWT shell, without opening a database workspace. */
public final class SourceCodeTextViewTest {
    public static void main(String[] args) throws Exception {
        Display display = new Display();
        Shell shell = new Shell(display);
        try {
            SourceCodeTextView view = new SourceCodeTextView(shell, false);
            StyledText text = Arrays.stream(view.getChildren()).filter(StyledText.class::isInstance)
                .map(StyledText.class::cast).findFirst().orElseThrow();
            // Outside OSGi the registry has no bundle. Supply a real shipped UDL
            // in this test process only; the production loading path is unchanged.
            var languages = SourceLanguageRegistry.class.getDeclaredField("languages");
            languages.setAccessible(true);
            try (var input = Files.newInputStream(Path.of("bundles", "io.github.sebastian.dbeaver.sourceviewer",
                "languages", "csharp.udl.xml"))) {
                languages.set(SourceLanguageRegistry.getInstance(), List.of(NotepadPlusPlusUdlReader.read(input)));
            }
            for (boolean dark : new boolean[] {false, true}) {
                Color background = new Color(display, dark ? new RGB(40, 40, 40) : new RGB(255, 255, 255));
                Color foreground = new Color(display, dark ? new RGB(220, 220, 220) : new RGB(0, 0, 0));
                try {
                    text.setBackground(background);
                    text.setForeground(foreground);
                    view.setSource("public PUBLIC publicValue", "source_cs");
                    RGB syntaxColor = text.getStyleRangeAtOffset(1).foreground.getRGB();
                    text.setSelection(0, 6);
                    text.notifyListeners(SWT.Selection, new Event());
                    pump(display);
                    require(text.getSelectionText().equals("public"), "Selection changed while applying styles");
                    require(text.getStyleRangeAtOffset(8).background != null, "Other case was not highlighted");
                    require(text.getStyleRangeAtOffset(16).background != null, "Partial-word match was not highlighted");
                    require(text.getStyleRangeAtOffset(1).foreground.getRGB().equals(syntaxColor), "Syntax color lost");
                    RGB match = text.getStyleRangeAtOffset(8).background.getRGB();
                    require(!match.equals(background.getRGB()), "Highlight invisible against theme background");
                    require(Math.abs(match.red - background.getRed()) <= 50, "Highlight is too strong");

                    text.setSelection(0);
                    text.notifyListeners(SWT.Selection, new Event());
                    pump(display);
                    require(Arrays.stream(text.getStyleRanges()).noneMatch(range -> range.background != null),
                        "Highlights did not clear");
                    require(text.getStyleRangeAtOffset(1).foreground.getRGB().equals(syntaxColor), "Clearing erased syntax");

                    text.invokeAction(ST.SELECT_COLUMN_NEXT);
                    pump(display);
                    require(text.getSelectionText().equals("p"), "Keyboard selection failed");
                    require(text.getStyleRangeAtOffset(7).background != null, "Keyboard selection did not highlight");

                    text.setSelection(0, 6);
                    text.notifyListeners(SWT.Selection, new Event());
                    // A new result cell must cancel pending work for the old cell.
                    view.setSource("public AnotherCell", "source_cs");
                    pump(display);
                    require(Arrays.stream(text.getStyleRanges()).noneMatch(range -> range.background != null),
                        "Highlights leaked into the next cell");
                } finally {
                    text.setBackground(null);
                    text.setForeground(null);
                    background.dispose();
                    foreground.dispose();
                }
            }
            view.setEditable(true);
            view.setSource("foo FOO", "source_cs");
            text.setSelection(0, 3);
            text.notifyListeners(SWT.Selection, new Event());
            pump(display);
            text.replaceTextRange(4, 3, "bar");
            pump(display);
            require(text.getStyleRangeAtOffset(4) == null, "Editing left a stale match");

            languages.set(SourceLanguageRegistry.getInstance(), List.of());
            view.setSource("test TEST", "unknown");
            text.setSelection(0, 4);
            text.notifyListeners(SWT.Selection, new Event());
            pump(display);
            require(text.getStyleRangeAtOffset(6).background != null, "Matching requires a syntax definition");
            text.setSelection(0);
            text.notifyListeners(SWT.Selection, new Event());
            view.dispose();
            pump(display);
            System.out.println("SWT viewer tests passed: light/dark themes, selection events, keyboard, clearing, cell switch, editing, disposal");
        } finally {
            shell.dispose();
            display.dispose();
        }
    }

    private static void pump(Display display) throws InterruptedException {
        long until = System.nanoTime() + 180_000_000;
        do {
            while (display.readAndDispatch()) {
                // Drain selection and timer events.
            }
            Thread.sleep(5);
        } while (System.nanoTime() < until);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
