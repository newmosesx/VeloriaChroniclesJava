package example.practice.gui;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * One place that turns a character NAME into a portrait. Panels call
 * Portraits.avatar(name, size) and get a round portrait back -- or, if that
 * character's image isn't on disk yet, a neutral lettered placeholder (so the
 * not-yet-generated faces never crash anything).
 *
 * SWAPPING ART LATER: just replace the .png file with the same name. No code
 * changes. Regenerated your Emperor in OpenAI? Drop it over The_Emperor.png.
 *
 * WHERE THE FILES GO (it checks these in order):
 *   1. a folder you set explicitly:  Portraits.setDirectory("C:/.../portraits");
 *   2. the classpath:                 /portraits/<file>.png   (resources folder)
 *   3. a "portraits" folder next to where the game runs:  ./portraits/<file>.png
 * Pick whichever is easiest; option 3 (a plain "portraits" folder in your
 * project root) needs zero build setup.
 */
public final class Portraits {

    private Portraits() {}

    private static String directory = null;                  // optional explicit folder
    public static void setDirectory(String dir) { directory = dir; }

    // name (normalised) -> file name. Aliases let titles, first names, and full
    // names all resolve to the same portrait.
    private static final Map<String, String> FILES = new HashMap<>();
    static {
        alias("Dusk_Bane.png",       "kaelen duskbane", "duskbane", "kaelen", "the blade");
        alias("Mara_Voss.png",       "mara voss", "mara", "the whisper");
        alias("General_Castius.png", "general castius", "castius", "the hawk");
        alias("Joric_Fen.png",       "joric fen", "joric", "the stray");
        alias("Iriah_Sable.png",     "iriah sable", "iriah");
        alias("Bram_Thorne.png",     "bram thorne", "bram");
        alias("Lyra_Veylen.png",     "lyra veylen", "lyra");
        alias("The_Speaker.png",     "the speaker", "speaker", "the unmoved", "house speaker");
        alias("The_Emperor.png",     "the emperor", "emperor");
    }
    private static void alias(String file, String... names) {
        for (String n : names) FILES.put(n, file);
    }

    private static final Map<String, Image> CACHE = new HashMap<>();

    /** True if a real portrait file resolved (vs. falling back to initials). */
    public static boolean has(String name) { return image(name) != null; }

    /**
     * A round portrait Node at the given diameter. Real image if available,
     * otherwise a lettered circle in a colour seeded from the name.
     */
    public static Node avatar(String name, double size) {
        Image img = image(name);
        Circle ring = new Circle(size / 2.0);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#0d0e0a"));
        ring.setStrokeWidth(2);

        StackPane pane = new StackPane();
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);

        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);          // fill-crop look; portraits are ~square/tall
            Circle clip = new Circle(size / 2.0, size / 2.0, size / 2.0);
            iv.setClip(clip);
            pane.getChildren().addAll(iv, ring);
        } else {
            Circle bg = new Circle(size / 2.0, seededColor(name));
            Text letter = new Text(initial(name));
            letter.setFill(Color.web("#0d0e0a"));
            letter.setFont(Font.font("Georgia", FontWeight.BOLD, size * 0.45));
            pane.getChildren().addAll(bg, letter, ring);
        }
        return pane;
    }

    // --- loading ------------------------------------------------------------
    private static Image image(String name) {
        if (name == null) return null;
        String file = FILES.get(normalise(name));
        if (file == null) return null;
        if (CACHE.containsKey(file)) return CACHE.get(file);

        Image img = load(file);
        CACHE.put(file, img);                    // cache nulls too, so we don't retry every frame
        return img;
    }

    private static Image load(String file) {
        // 1) explicit directory
        if (directory != null) {
            Image i = fromFile(new File(directory, file));
            if (i != null) return i;
        }
        // 2) classpath /portraits/<file>
        try (InputStream in = Portraits.class.getResourceAsStream("/portraits/" + file)) {
            if (in != null) {
                Image i = new Image(in);
                if (!i.isError()) return i;
            }
        } catch (Exception ignored) {}
        // 3) ./portraits/<file>
        return fromFile(new File("portraits", file));
    }

    private static Image fromFile(File f) {
        if (f == null || !f.exists()) return null;
        try (FileInputStream in = new FileInputStream(f)) {
            Image i = new Image(in);
            return i.isError() ? null : i;
        } catch (Exception e) {
            return null;
        }
    }

    // --- fallback helpers ---------------------------------------------------
    private static String normalise(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();
    }

    private static String initial(String name) {
        String n = name == null ? "" : name.trim();
        return n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase();
    }

    private static Color seededColor(String name) {
        int h = (name == null ? 0 : name.hashCode());
        double hue = Math.abs(h) % 360;
        return Color.hsb(hue, 0.30, 0.62);       // muted, readable behind dark text
    }
}