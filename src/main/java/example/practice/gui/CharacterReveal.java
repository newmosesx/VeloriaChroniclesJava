package example.practice.gui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * The moment a character steps out of the silhouette. Plays over the central
 * panel: the shared silhouette cross-fades into the real portrait while the
 * name and title rise in. Drives Portraits' reveal state.
 *
 * Two trigger modes, same as the battle cinematic -- same call, different
 * placement:
 *   - mid-chapter (before a paragraph's text):  play(...) then show the text
 *   - on entering a chapter:                    play(...) then advance
 *
 * The beat table below maps each reveal to the exact (chapter, paragraph) where
 * the story first presents that character. revealAllUpTo() reconstructs reveal
 * state silently from any position (used on load / chapter jumps), so reveals
 * never desync and never replay once seen.
 */
public final class CharacterReveal {

    private CharacterReveal() {}

    public static final class Figure {
        public final String name, subtitle;
        public Figure(String name, String subtitle) { this.name = name; this.subtitle = subtitle; }
    }

    private static final class Beat {
        final int ch, p; final Figure[] figs;
        Beat(int ch, int p, Figure... figs) { this.ch = ch; this.p = p; this.figs = figs; }
    }

    // Reveal beats, pinned to the real prose in StoryData.
    private static final List<Beat> BEATS = new ArrayList<>();
    static {
        java.util.Map<Long, java.util.List<Figure>> grouped = new java.util.LinkedHashMap<>();
        for (example.practice.story.StoryData.CharacterBeat cb
                : example.practice.story.StoryData.characterBeats()) {
            long key = ((long) cb.chapterIndex << 32) | (cb.paragraph & 0xffffffffL);
            grouped.computeIfAbsent(key, k -> new java.util.ArrayList<>())
                    .add(new Figure(cb.name, cb.subtitle));
        }
        for (java.util.Map.Entry<Long, java.util.List<Figure>> e : grouped.entrySet()) {
            int ch = (int) (e.getKey() >> 32);
            int p  = (int) (e.getKey() & 0xffffffffL);
            BEATS.add(new Beat(ch, p, e.getValue().toArray(new Figure[0])));
        }
    }

    /** If a reveal sits exactly here and someone is still hidden, return its figures (to animate). */
    public static Figure[] pendingAt(int ch, int p) {
        for (Beat b : BEATS) {
            if (b.ch == ch && b.p == p) {
                for (Figure f : b.figs) if (!Portraits.isRevealed(f.name)) return b.figs;
            }
        }
        return null;
    }

    /** Silently reveal everyone introduced at or before this position (load / jumps / startup). */
    public static void revealAllUpTo(int ch, int p) {
        for (Beat b : BEATS)
            if (b.ch < ch || (b.ch == ch && b.p <= p))
                for (Figure f : b.figs) Portraits.reveal(f.name);
    }

    /** Play the reveal over 'host' (a StackPane), then run onComplete. */
    public static void play(StackPane host, Figure[] figs, Runnable onComplete) {
        if (host == null || figs == null || figs.length == 0) {
            if (onComplete != null) onComplete.run();
            return;
        }

        double size = figs.length > 2 ? 150 : 190;

        StackPane overlay = new StackPane();
        overlay.setOpacity(0);
        Region bg = new Region();
        bg.setStyle("-fx-background-color: radial-gradient(center 50% 42%, radius 80%, #11131aF2, #06070b);");

        HBox row = new HBox(44);
        row.setAlignment(Pos.CENTER);

        List<Runnable> steps = new ArrayList<>();
        for (Figure f : figs) {
            Node real = Portraits.realAvatar(f.name, size);
            Node sil = Portraits.silhouette(size);
            StackPane portrait = new StackPane(real, sil);   // silhouette sits on top

            Label name = new Label(f.name);
            name.setOpacity(0);
            name.setStyle("-fx-text-fill:#e3d5a8; -fx-font-family:'Georgia'; -fx-font-size:21px; -fx-font-weight:bold;");
            Label sub = new Label(f.subtitle);
            sub.setOpacity(0);
            sub.setStyle("-fx-text-fill:#bcb39a; -fx-font-family:'Georgia'; -fx-font-style:italic; -fx-font-size:14px;");

            VBox cell = new VBox(8, portrait, name, sub);
            cell.setAlignment(Pos.CENTER);
            row.getChildren().add(cell);

            steps.add(() -> {
                Portraits.reveal(f.name);
                FadeTransition fade = new FadeTransition(Duration.millis(750), sil);
                fade.setToValue(0); fade.play();
                ScaleTransition pop = new ScaleTransition(Duration.millis(550), portrait);
                pop.setFromX(0.95); pop.setFromY(0.95); pop.setToX(1); pop.setToY(1); pop.play();
                FadeTransition fn = new FadeTransition(Duration.millis(500), name);
                fn.setToValue(1); fn.play();
                FadeTransition fsub = new FadeTransition(Duration.millis(500), sub);
                fsub.setDelay(Duration.millis(160)); fsub.setToValue(1); fsub.play();
            });
        }

        overlay.getChildren().addAll(bg, row);
        host.getChildren().add(overlay);

        FadeTransition in = new FadeTransition(Duration.millis(450), overlay);
        in.setFromValue(0); in.setToValue(1);
        in.play();

        Timeline director = new Timeline();
        int t = 650;
        for (Runnable step : steps) {
            final Runnable r = step;
            director.getKeyFrames().add(new KeyFrame(Duration.millis(t), e -> r.run()));
            t += 750;
        }
        int hold = t + 1300;
        director.getKeyFrames().add(new KeyFrame(Duration.millis(hold), e -> {
            FadeTransition out = new FadeTransition(Duration.millis(550), overlay);
            out.setFromValue(1); out.setToValue(0);
            out.setOnFinished(ev -> {
                host.getChildren().remove(overlay);
                if (onComplete != null) onComplete.run();
            });
            out.play();
        }));
        director.setCycleCount(1);
        director.play();
    }
}