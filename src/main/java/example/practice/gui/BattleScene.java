package example.practice.gui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * Battle cinematic for the central control panel -- pure JavaFX, no WebView.
 *
 * HOW IT WORKS
 *   BattleScene.play(host, spec, onComplete) lays a full-cover overlay on top of
 *   whatever the central panel is showing, fades it IN, plays the ~6s animation,
 *   fades it OUT, removes itself, then runs onComplete. The panel underneath is
 *   never touched -- just covered for the duration and revealed again. That IS
 *   the "transition into the animation, then back to the text" effect.
 *
 * REQUIREMENT: 'host' must be a StackPane (your centerContainer already is one),
 *   because the overlay is stacked on top of it.
 *
 * TWO TRIGGER MODES (same call, different placement):
 *   (a) mid-chapter, before a battle paragraph's text:
 *           BattleScene.play(center, spec, () -> updateStoryView());
 *   (b) right after a chapter:
 *           BattleScene.play(center, spec, () -> goToNextChapter());
 */
public final class BattleScene {

    private BattleScene() {}

    /** Everything the cinematic needs to know about one battle. */
    public static final class Spec {
        public final String title;
        public final int imperials, rebels, impSurvivors, rebSurvivors;
        public final boolean empireWon;

        public Spec(String title, int imperials, int rebels,
                    int impSurvivors, int rebSurvivors, boolean empireWon) {
            this.title = title;
            this.imperials = imperials;
            this.rebels = rebels;
            this.impSurvivors = impSurvivors;
            this.rebSurvivors = rebSurvivors;
            this.empireWon = empireWon;
        }
    }

    private static final Color IMP_FILL   = Color.web("#e8c14a");
    private static final Color IMP_STROKE = Color.web("#7a5e12");
    private static final Color REB_FILL   = Color.web("#c0392b");
    private static final Color REB_STROKE = Color.web("#5e1812");
    private static final int UNITS_PER_SIDE = 12;

    /** Play the cinematic over 'host', then run onComplete on the FX thread. */
    public static void play(StackPane host, Spec spec, Runnable onComplete) {
        if (host == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        double w = host.getWidth()  > 0 ? host.getWidth()  : 760;
        double h = host.getHeight() > 0 ? host.getHeight() : 500;

        // --- overlay container ---
        StackPane overlay = new StackPane();
        overlay.setOpacity(0);

        Region bg = new Region();
        bg.setStyle("-fx-background-color: radial-gradient(center 50% 18%, radius 95%, "
                + "#2a2f3a 0%, #14161c 60%, #0c0d12 100%);");

        Pane field = new Pane();   // absolute-positioned battlefield

        // --- title ---
        Label title = new Label(spec.title == null ? "" : spec.title.toUpperCase());
        title.setStyle("-fx-text-fill: #d8c79a; -fx-font-size: 26px; -fx-font-family: 'Georgia';"
                + " -fx-effect: dropshadow(gaussian, #000, 8, 0, 0, 2);");

        // --- armies ---
        TilePane left  = buildArmy(IMP_FILL, IMP_STROKE);
        TilePane right = buildArmy(REB_FILL, REB_STROKE);
        left.setLayoutX(w * 0.18);  left.setLayoutY(h * 0.38);
        right.setLayoutX(w * 0.62); right.setLayoutY(h * 0.38);

        // --- clash flash ---
        RadialGradient flashGrad = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffe896")),
                new Stop(0.55, Color.web("#ff7828", 0.18)),
                new Stop(0.70, Color.TRANSPARENT));
        Circle flash = new Circle(w * 0.5, h * 0.45, 45, flashGrad);
        flash.setOpacity(0);
        flash.setScaleX(0.3); flash.setScaleY(0.3);

        // --- count readouts ---
        Label impCount = new Label(String.valueOf(spec.imperials));
        Label rebCount = new Label(String.valueOf(spec.rebels));
        VBox impBox = countBox("IMPERIALS", impCount, "#e8c14a");
        VBox rebBox = countBox("REBELS",    rebCount, "#e07a6a");
        impBox.setLayoutX(w * 0.10); impBox.setLayoutY(h * 0.78);
        rebBox.setLayoutX(w * 0.78); rebBox.setLayoutY(h * 0.78);

        field.getChildren().addAll(left, right, flash, impBox, rebBox);

        // --- result banner ---
        Label result = new Label(spec.empireWon ? "IMPERIAL VICTORY" : "REBEL VICTORY");
        result.setStyle("-fx-font-size: 44px; -fx-font-weight: bold; -fx-font-family: 'Georgia';"
                + " -fx-text-fill: " + (spec.empireWon ? "#7ad17a" : "#e07a6a") + ";"
                + " -fx-effect: dropshadow(gaussian, #000, 12, 0, 0, 3);");
        result.setOpacity(0);
        result.setScaleX(0.6); result.setScaleY(0.6);

        overlay.getChildren().addAll(bg, field, title, result);
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        StackPane.setMargin(title, new Insets(h * 0.06, 0, 0, 0));
        StackPane.setAlignment(result, Pos.CENTER);

        host.getChildren().add(overlay);   // cover the panel

        // --- fade the cinematic in ---
        FadeTransition fadeIn = new FadeTransition(Duration.millis(450), overlay);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        fadeIn.play();

        // --- director: phase timeline, mirrors the preview's pacing ---
        Timeline director = new Timeline(
                new KeyFrame(Duration.millis(250), e -> {       // ADVANCE
                    slide(left,  w * 0.12);
                    slide(right, -w * 0.12);
                }),
                new KeyFrame(Duration.millis(1750), e -> {      // CLASH
                    burst(flash);
                    shake(field);
                    dropCasualties(left,  1.0 - frac(spec.impSurvivors, spec.imperials));
                    dropCasualties(right, 1.0 - frac(spec.rebSurvivors, spec.rebels));
                    tickCount(impCount, spec.imperials, spec.impSurvivors);
                    tickCount(rebCount, spec.rebels,    spec.rebSurvivors);
                }),
                new KeyFrame(Duration.millis(3500), e -> {      // RESOLVE -- loser retreats
                    TilePane loser = spec.empireWon ? right : left;
                    double dir = spec.empireWon ? w * 0.3 : -w * 0.3;
                    TranslateTransition r = new TranslateTransition(Duration.millis(900), loser);
                    r.setByX(dir); r.play();
                    FadeTransition rf = new FadeTransition(Duration.millis(900), loser);
                    rf.setToValue(0.25); rf.play();
                }),
                new KeyFrame(Duration.millis(4700), e -> {      // RESULT
                    FadeTransition rf = new FadeTransition(Duration.millis(550), result);
                    rf.setToValue(1); rf.play();
                    ScaleTransition rs = new ScaleTransition(Duration.millis(550), result);
                    rs.setToX(1); rs.setToY(1); rs.play();
                }),
                new KeyFrame(Duration.millis(6500), e -> {      // FADE OUT + hand back
                    FadeTransition out = new FadeTransition(Duration.millis(550), overlay);
                    out.setFromValue(1); out.setToValue(0);
                    out.setOnFinished(ev -> {
                        host.getChildren().remove(overlay);
                        if (onComplete != null) onComplete.run();
                    });
                    out.play();
                })
        );
        director.play();
    }

    // ------------------------------------------------------------------ helpers

    private static TilePane buildArmy(Color fill, Color stroke) {
        TilePane tp = new TilePane();
        tp.setPrefColumns(4);
        tp.setHgap(4); tp.setVgap(4);
        for (int i = 0; i < UNITS_PER_SIDE; i++) {
            SVGPath s = new SVGPath();
            s.setContent("M11 1 L21 4 V12 C21 18 16 22 11 23 C6 22 1 18 1 12 V4 Z");
            s.setFill(fill);
            s.setStroke(stroke);
            s.setStrokeWidth(1.2);
            tp.getChildren().add(s);
        }
        return tp;
    }

    private static VBox countBox(String name, Label count, String colorHex) {
        Label nm = new Label(name);
        nm.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 13px; -fx-opacity: .8;");
        count.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 32px; -fx-font-weight: bold;"
                + " -fx-effect: dropshadow(gaussian, #000, 6, 0, 0, 2);");
        VBox box = new VBox(2, nm, count);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static void slide(Node army, double dx) {
        TranslateTransition t = new TranslateTransition(Duration.millis(1400), army);
        t.setByX(dx);
        t.play();
    }

    private static void burst(Circle flash) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(flash.opacityProperty(), 0),
                        new KeyValue(flash.scaleXProperty(), 0.3),
                        new KeyValue(flash.scaleYProperty(), 0.3)),
                new KeyFrame(Duration.millis(180), new KeyValue(flash.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(600),
                        new KeyValue(flash.opacityProperty(), 0),
                        new KeyValue(flash.scaleXProperty(), 2.2),
                        new KeyValue(flash.scaleYProperty(), 2.2))
        );
        t.play();
    }

    private static void shake(Node field) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(field.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(60),  new KeyValue(field.translateXProperty(), -5)),
                new KeyFrame(Duration.millis(120), new KeyValue(field.translateXProperty(), 5)),
                new KeyFrame(Duration.millis(180), new KeyValue(field.translateXProperty(), -3)),
                new KeyFrame(Duration.millis(240), new KeyValue(field.translateXProperty(), 0))
        );
        t.play();
    }

    private static void dropCasualties(TilePane army, double lossFrac) {
        int n = army.getChildren().size();
        int k = (int) Math.round(n * Math.max(0, Math.min(1, lossFrac)));
        for (int i = 0; i < k; i++) {
            Node u = army.getChildren().get(n - 1 - i);
            Duration delay = Duration.millis(i * 90);
            FadeTransition f = new FadeTransition(Duration.millis(500), u);
            f.setToValue(0); f.setDelay(delay); f.play();
            TranslateTransition t = new TranslateTransition(Duration.millis(500), u);
            t.setByY(28); t.setDelay(delay); t.play();
        }
    }

    private static void tickCount(Label lbl, int from, int to) {
        DoubleProperty p = new SimpleDoubleProperty(0);
        p.addListener((o, a, b) ->
                lbl.setText(String.valueOf((int) Math.round(from + (to - from) * b.doubleValue()))));
        Timeline t = new Timeline(new KeyFrame(Duration.millis(1500), new KeyValue(p, 1.0)));
        t.play();
    }

    private static double frac(int part, int whole) {
        return whole <= 0 ? 0 : (double) part / whole;
    }
}