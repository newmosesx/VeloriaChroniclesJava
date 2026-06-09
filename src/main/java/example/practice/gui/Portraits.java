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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Turns a character NAME into a round portrait -- but spoiler-safe: until the
 * story has *presented* a character, they show the shared SILHOUETTE. Once
 * revealed (see CharacterReveal), their real face shows everywhere (council
 * bubbles, character window, agent panel).
 *
 * Reveal state is intentionally not persisted: CharacterReveal.revealAllUpTo()
 * rebuilds it from the current story position on load/startup, so it can never
 * drift from where the player actually is.
 *
 * Files live in a "portraits" folder (project root works with no build setup),
 * or the classpath /portraits/, or a folder set via setDirectory(). Drop in
 * silhouette.png alongside the character images.
 */
public final class Portraits {

    private Portraits() {}

    private static String directory = null;
    public static void setDirectory(String dir) { directory = dir; }

    private static final String SILHOUETTE = "silhouette.png";

    // name (normalised) -> file
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
    private static void alias(String file, String... names) { for (String n : names) FILES.put(n, file); }

    private static final Map<String, Image> CACHE = new HashMap<>();

    // which portrait files have been revealed. The Emperor (you) and Castius
    // (your commander) are known from the start; everyone else is earned.
    private static final Set<String> revealed = new HashSet<>();
    static { revealed.add("The_Emperor.png"); revealed.add("General_Castius.png"); }

    public static void reveal(String name) {
        String f = FILES.get(norm(name));
        if (f != null) revealed.add(f);
    }
    public static boolean isRevealed(String name) {
        String f = FILES.get(norm(name));
        return f != null && revealed.contains(f);
    }

    /** Gated portrait: silhouette until the character has been presented in-story. */
    public static Node avatar(String name, double size) {
        String f = FILES.get(norm(name));
        if (f != null && revealed.contains(f)) return realNode(f, size, name);
        return silhouette(size);
    }

    /** The real portrait regardless of reveal state -- used by the reveal animation. */
    public static Node realAvatar(String name, double size) {
        return realNode(FILES.get(norm(name)), size, name);
    }

    /** The shared "unknown figure" silhouette. */
    public static Node silhouette(double size) {
        Image img = loadCached(SILHOUETTE);
        StackPane p = fixed(size);
        if (img != null) {
            p.getChildren().addAll(clipped(img, size), ring(size));
        } else {
            Circle bg = new Circle(size / 2.0, Color.web("#2a2e35"));
            Text q = new Text("?");
            q.setFill(Color.web("#5b6470"));
            q.setFont(Font.font("Georgia", FontWeight.BOLD, size * 0.5));
            p.getChildren().addAll(bg, q, ring(size));
        }
        return p;
    }

    // --- internals ----------------------------------------------------------
    private static Node realNode(String file, double size, String name) {
        Image img = (file == null) ? null : loadCached(file);
        StackPane p = fixed(size);
        if (img != null) {
            p.getChildren().addAll(clipped(img, size), ring(size));
        } else {
            Circle bg = new Circle(size / 2.0, seededColor(name));
            Text letter = new Text(initial(name));
            letter.setFill(Color.web("#0d0e0a"));
            letter.setFont(Font.font("Georgia", FontWeight.BOLD, size * 0.45));
            p.getChildren().addAll(bg, letter, ring(size));
        }
        return p;
    }

    private static StackPane fixed(double size) {
        StackPane p = new StackPane();
        p.setMinSize(size, size); p.setPrefSize(size, size); p.setMaxSize(size, size);
        return p;
    }
    private static ImageView clipped(Image img, double size) {
        ImageView iv = new ImageView(img);
        iv.setFitWidth(size); iv.setFitHeight(size); iv.setPreserveRatio(true);
        iv.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
        return iv;
    }
    private static Circle ring(double size) {
        Circle ring = new Circle(size / 2.0);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#0d0e0a"));
        ring.setStrokeWidth(2);
        return ring;
    }

    private static Image loadCached(String file) {
        if (file == null) return null;
        if (CACHE.containsKey(file)) return CACHE.get(file);
        Image img = load(file);
        CACHE.put(file, img);
        return img;
    }
    private static Image load(String file) {
        if (directory != null) {
            Image i = fromFile(new File(directory, file));
            if (i != null) return i;
        }
        try (InputStream in = Portraits.class.getResourceAsStream("/portraits/" + file)) {
            if (in != null) { Image i = new Image(in); if (!i.isError()) return i; }
        } catch (Exception ignored) {}
        return fromFile(new File("portraits", file));
    }
    private static Image fromFile(File f) {
        if (f == null || !f.exists()) return null;
        try (FileInputStream in = new FileInputStream(f)) {
            Image i = new Image(in); return i.isError() ? null : i;
        } catch (Exception e) { return null; }
    }

    private static String norm(String s) { return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim(); }
    private static String initial(String name) {
        String n = name == null ? "" : name.trim();
        return n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase();
    }
    private static Color seededColor(String name) {
        int h = (name == null ? 0 : name.hashCode());
        return Color.hsb(Math.abs(h) % 360, 0.30, 0.62);
    }
}