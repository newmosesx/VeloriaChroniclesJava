package example.practice.gui;

import example.practice.engine.SimulationEngine;
import example.practice.kingdoms.Kingdom;
import example.practice.world.Agriculture;
import example.practice.world.Climate;
import example.practice.world.Geography;
import example.practice.world.Water;
import example.practice.world.World;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * The in-game world map -- a stylized cartographic view of Veloria's eight
 * compass territories, tinted live by a selectable metric. The center is the
 * Imperial Seat (the capital), NOT a ninth region: there are exactly eight
 * surveyed sectors, matching the simulation's eight CompassDirection sectors.
 *
 * USAGE (drop-in, like GovernancePanel):
 *     Region map = MapPanel.build(engine);
 *     centerContainer.getChildren().add(map);     // centerContainer is your StackPane
 *     map.setVisible(false); map.setManaged(false);
 *
 * DATA: every read from the simulation lives in sampleWorld() and is now LIVE --
 * soil/yield/crop from Agriculture, flood from Water, temperature from Climate,
 * region names from Geography, unrest from the empire kingdom. Sector id == the
 * CompassDirection ordinal (id == kingdom id), so cell->sector mapping is fixed.
 */
public final class MapPanel {

    public static Region build(SimulationEngine engine) {
        return new MapPanel(engine).root;
    }

    // ---- metrics -----------------------------------------------------------
    private enum Metric {
        SOIL ("Soil",  "#8a6a3a", "#2f7d6b", "dry",    "wet"),
        YIELD("Yield", "#9a8b55", "#3f8a2f", "barren", "lush"),
        FLOOD("Flood", "#5a6b4a", "#235c8c", "calm",   "flooded"),
        HEAT ("Heat",  "#3a6ea5", "#c0392b", "cold",   "hot");

        final String label; final Color lo, hi; final String loT, hiT;
        Metric(String label, String lo, String hi, String loT, String hiT) {
            this.label = label; this.lo = Color.web(lo); this.hi = Color.web(hi);
            this.loT = loT; this.hiT = hiT;
        }
    }
    // temperature tint range (degrees C) -> 0..1
    private static final double HEAT_LO = -5, HEAT_HI = 35;

    // ---- one territory -----------------------------------------------------
    private static final class Sector {
        final String dir, region; final boolean capital;
        final int id;                         // world sector id; -1 for the capital
        double soil, yield, flood, tempC;     // live values
        String crop = "";
        Polygon poly; Text badge; double cx, cy;
        Sector(String dir, String region, boolean capital, int id) {
            this.dir = dir; this.region = region; this.capital = capital; this.id = id;
        }
        double value(Metric m) {
            switch (m) { case SOIL: return soil; case YIELD: return yield;
                case FLOOD: return flood; default: return tempC; }
        }
    }

    // 4x4 lattice (jagged); rows run north (top) -> coast (bottom)
    private static final double[][][] G = {
            {{45,38},{210,28},{400,46},{560,36}},
            {{34,158},{226,138},{394,166},{576,148}},
            {{52,268},{214,252},{412,276},{564,258}},
            {{62,352},{204,376},{396,358},{556,372}}
    };
    private static final double W = 600, H = 430;

    private final SimulationEngine engine;
    private final Region root;
    private final Geography geo;
    private final List<Sector> sectors = new ArrayList<>();
    private final List<Button> metricButtons = new ArrayList<>();
    private Metric current = Metric.SOIL;

    private Region ramp;
    private Label legLo, legHi, readout;
    private ProgressBar unrestBar;
    private Label unrestLabel;

    private MapPanel(SimulationEngine engine) {
        this.engine = engine;
        this.geo = engine.getWorld().geography;
        this.root = buildUi();
        startRefresh();
    }

    // strip the leading "the " so labels read "Northern Reach", not "the Northern Reach"
    private static String shortName(String region) {
        if (region == null) return "";
        return region.startsWith("the ") ? region.substring(4) : region;
    }

    // ------------------------------------------------------------------ build
    private Region buildUi() {
        Label title = new Label("THE REALM OF VELORIA");
        title.setStyle("-fx-text-fill: #e3d5a8; -fx-font-family: 'Georgia'; -fx-font-size: 16px; -fx-padding: 0 0 0 4;");

        HBox toggles = new HBox(6);
        toggles.setAlignment(Pos.CENTER_RIGHT);
        for (Metric m : Metric.values()) {
            Button b = new Button(m.label);
            styleToggle(b, m == current);
            b.setOnAction(e -> selectMetric(m));
            metricButtons.add(b);
            toggles.getChildren().add(b);
        }
        Region spring = new Region();
        HBox.setHgrow(spring, Priority.ALWAYS);
        HBox bar = new HBox(8, title, spring, toggles);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label uLbl = new Label("EMPIRE UNREST");
        uLbl.setStyle("-fx-text-fill: #c3b78f; -fx-font-size: 11px;");
        unrestBar = new ProgressBar(0);
        unrestBar.setPrefWidth(220);
        unrestLabel = new Label("--");
        unrestLabel.setStyle("-fx-text-fill: #e7e0cf; -fx-font-size: 12px;");
        HBox hud = new HBox(10, uLbl, unrestBar, unrestLabel);
        hud.setAlignment(Pos.CENTER_LEFT);
        hud.setPadding(new Insets(6, 0, 8, 4));

        Pane canvas = buildCanvas();
        StackPane canvasHost = new StackPane(canvas);
        canvasHost.setStyle("-fx-background-color: #12130e; -fx-background-radius: 8;");
        canvasHost.setPadding(new Insets(6));

        ramp = new Region();
        ramp.setPrefSize(180, 9);
        ramp.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(ramp, Priority.ALWAYS);
        legLo = small("dry"); legHi = small("wet");
        HBox legend = new HBox(8, legLo, ramp, legHi);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(8, 4, 0, 4));

        readout = new Label("A hand-drawn survey  \u00b7  click any territory to inspect it");
        readout.setStyle("-fx-text-fill: #c3b78f; -fx-font-size: 12px; -fx-padding: 8 4 0 4;");

        VBox box = new VBox(4, bar, hud, canvasHost, legend, readout);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: #191b16; -fx-border-color: #3a3a2c; -fx-border-radius: 12; -fx-background-radius: 12;");
        box.setMaxWidth(640);
        applyMetricVisuals();
        return box;
    }

    private Pane buildCanvas() {
        Pane c = new Pane();
        c.setPrefSize(W, H); c.setMinSize(W, H); c.setMaxSize(W, H);

        Polygon sea = new Polygon(
                G[3][0][0],G[3][0][1], G[3][1][0],G[3][1][1],
                G[3][2][0],G[3][2][1], G[3][3][0],G[3][3][1], W,H, 0,H);
        sea.setFill(Color.web("#1d3a52"));
        c.getChildren().add(sea);
        for (double y : new double[]{392, 406}) {
            Polyline wave = new Polyline();
            for (double x = 20; x <= 580; x += 20) wave.getPoints().addAll(x, y + (Math.sin(x / 22.0) * 2.5));
            wave.setStroke(Color.web("#3f6e92", 0.5)); wave.setStrokeWidth(1.4);
            c.getChildren().add(wave);
        }

        // territories: cell -> world sector id (CompassDirection ordinal).
        // N=0 NE=1 E=2 SE=3 S=4 SW=5 W=6 NW=7 ; capital id = -1
        addSector(c, "NW",  7, false, 0,0);
        addSector(c, "N",   0, false, 0,1);
        addSector(c, "NE",  1, false, 0,2);
        addSector(c, "W",   6, false, 1,0);
        addSector(c, "",   -1, true,  1,1);   // Imperial Seat
        addSector(c, "E",   2, false, 1,2);
        addSector(c, "SW",  5, false, 2,0);
        addSector(c, "S",   4, false, 2,1);
        addSector(c, "SE",  3, false, 2,2);

        addRiver(c, 210,40, 216,110, 224,210, 214,300, 204,372);
        addRiver(c, 400,50, 398,150, 404,240, 400,330, 396,358);
        addRiver(c, 224,210, 310,225, 404,240);

        for (double[] m : new double[][]{{150,60},{200,52},{255,64},{390,70},{430,60}}) {
            Polygon peak = new Polygon(m[0]-9,m[1]+9, m[0],m[1]-9, m[0]+9,m[1]+9);
            peak.setFill(Color.web("#cdbf94", 0.55));
            peak.setStroke(Color.web("#0d0e0a")); peak.setStrokeWidth(1);
            c.getChildren().add(peak);
        }

        Sector cap = capitalSector();
        Circle ring = new Circle(cap.cx, cap.cy - 4, 12, Color.web("#e8c14a", 0.95));
        ring.setStroke(Color.web("#0d0e0a")); ring.setStrokeWidth(1.5);
        c.getChildren().add(ring);
        c.getChildren().add(glyph("\u2605", cap.cx, cap.cy - 3, 15, Color.web("#2a230a"), false));

        for (Sector s : sectors) {
            if (s.capital) {
                c.getChildren().add(label("The Imperial Seat", s.cx, s.cy + 16, 11, true));
                continue;                       // no compass tag, no metric badge
            }
            c.getChildren().add(label(s.dir, s.cx, s.cy - 4, 12, true));
            c.getChildren().add(label(s.region, s.cx, s.cy + 9, 10, false));
            s.badge = label("", s.cx, s.cy + 26, 14, true);
            c.getChildren().add(s.badge);
        }

        Circle rose = new Circle(560, 404, 14, Color.TRANSPARENT);
        rose.setStroke(Color.web("#c3b78f", 0.6)); rose.setStrokeWidth(1);
        Polygon needle = new Polygon(560,392, 564,404, 560,401, 556,404);
        needle.setFill(Color.web("#e98a7a"));
        c.getChildren().addAll(rose, needle, glyph("N", 560, 390, 9, Color.web("#e3d5a8"), false));
        return c;
    }

    private Sector capitalSector() {
        for (Sector s : sectors) if (s.capital) return s;
        return sectors.get(0);
    }

    private void addSector(Pane c, String dir, int worldId, boolean cap, int r, int col) {
        double[] a = G[r][col], b = G[r][col+1], d = G[r+1][col+1], e = G[r+1][col];
        Polygon p = new Polygon(a[0],a[1], b[0],b[1], d[0],d[1], e[0],e[1]);
        p.setStroke(cap ? Color.web("#e8c14a") : Color.web("#0d0e0a"));
        p.setStrokeWidth(cap ? 2.5 : 1.5);

        String region = cap ? "The Imperial Seat" : shortName(geo.sector(worldId).region());
        Sector s = new Sector(dir, region, cap, worldId);
        s.poly = p;
        s.cx = (a[0]+b[0]+d[0]+e[0]) / 4.0;
        s.cy = (a[1]+b[1]+d[1]+e[1]) / 4.0;

        if (cap) {
            p.setFill(Color.web("#5b5236"));     // fixed stone-and-gold; never tinted
        } else {
            p.setOnMouseClicked(ev -> showReadout(s));
            p.setOnMouseEntered(ev -> p.setOpacity(0.85));
            p.setOnMouseExited(ev -> p.setOpacity(1.0));
        }
        sectors.add(s);
        c.getChildren().add(p);
    }

    private void addRiver(Pane c, double... pts) {
        Polyline river = new Polyline(pts);
        river.setFill(null);
        river.setStroke(Color.web("#5fa8d8", 0.6));
        river.setStrokeWidth(2.6);
        river.getStrokeDashArray().addAll(5.0, 8.0);
        river.setStrokeLineCap(StrokeLineCap.ROUND);
        c.getChildren().add(river);
        Timeline flow = new Timeline(
                new KeyFrame(Duration.ZERO,       new KeyValue(river.strokeDashOffsetProperty(), 0)),
                new KeyFrame(Duration.seconds(1), new KeyValue(river.strokeDashOffsetProperty(), -13)));
        flow.setCycleCount(Animation.INDEFINITE);
        flow.play();
    }

    // ------------------------------------------------------------------ logic
    private void selectMetric(Metric m) {
        current = m;
        for (int i = 0; i < metricButtons.size(); i++)
            styleToggle(metricButtons.get(i), Metric.values()[i] == m);
        applyMetricVisuals();
    }

    private void applyMetricVisuals() {
        for (Sector s : sectors) {
            if (s.capital) continue;            // the Imperial Seat is never tinted
            double v = s.value(current);
            double t = (current == Metric.HEAT) ? (v - HEAT_LO) / (HEAT_HI - HEAT_LO) : v / 100.0;
            s.poly.setFill(current.lo.interpolate(current.hi, clamp01(t)));
            if (s.badge != null) s.badge.setText(String.valueOf(Math.round(v)));
        }
        ramp.setStyle("-fx-background-radius: 5; -fx-background-color: linear-gradient(to right, "
                + toHex(current.lo) + ", " + toHex(current.hi) + ");");
        legLo.setText(current.loT);
        legHi.setText(current.hiT);
    }

    private void showReadout(Sector s) {
        readout.setText(s.dir + " \u00b7 " + s.region
                + "    soil " + Math.round(s.soil) + "%"
                + "  \u00b7  yield " + Math.round(s.yield) + "%"
                + "  \u00b7  flood " + Math.round(s.flood) + "%"
                + "  \u00b7  " + Math.round(s.tempC) + "\u00b0C"
                + (s.crop.isEmpty() ? "" : "  \u00b7  " + s.crop));
    }

    private void startRefresh() {
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), e -> refresh()));
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
        refresh();
    }

    private void refresh() {
        engine.lock();
        try { sampleWorld(); } finally { engine.unlock(); }
        applyMetricVisuals();
    }

    /** The single data tap -- now fully LIVE off the world systems. */
    private void sampleWorld() {
        // --- empire unrest ---
        Kingdom empire = engine.getKingdoms()[0];
        double unrest = empire.unrestLevel;
        double frac = clamp01(unrest / 2500.0);            // ~ PoliticsConfig.UNREST_FULL_SCALE
        unrestBar.setProgress(frac);
        unrestLabel.setText(String.valueOf((int) unrest));
        String accent = frac > 0.8 ? "#c0392b" : frac > 0.4 ? "#d98a2b" : "#5a9e4a";
        unrestBar.setStyle("-fx-accent: " + accent + ";");

        // --- per-sector environment (real arrays, indexed by sector id) ---
        World w = engine.getWorld();
        Agriculture ag = w.agriculture;
        Water wa = w.water;
        Climate cl = w.climate;
        for (Sector s : sectors) {
            if (s.id < 0) continue;                        // capital has no sector data
            s.soil  = ag.soilMoisture[s.id] * 100.0;
            s.yield = ag.yield[s.id]        * 100.0;
            s.crop  = ag.cropState[s.id];
            s.flood = wa.floodSeverity[s.id] * 100.0;
            s.tempC = cl.temperature[s.id];
        }
    }

    // ------------------------------------------------------------------ ui bits
    private static void styleToggle(Button b, boolean on) {
        String base = "-fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 11;";
        b.setStyle(on
                ? base + "-fx-background-color: #4a4528; -fx-text-fill: #fff7df; -fx-border-color: #a89a5c; -fx-border-radius: 6;"
                : base + "-fx-background-color: #26261c; -fx-text-fill: #c3b78f; -fx-border-color: #45452f; -fx-border-radius: 6;");
    }

    private static Label small(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-text-fill: #8c8460; -fx-font-size: 11px;");
        return l;
    }

    private static Text label(String s, double x, double y, double size, boolean bold) {
        Text t = glyph(s, x, y, size, Color.WHITE, bold);
        t.setEffect(new DropShadow(3, Color.web("#000", 0.8)));
        return t;
    }

    private static Text glyph(String s, double x, double y, double size, Color fill, boolean bold) {
        Text t = new Text(s);
        t.setFill(fill);
        t.setFont(Font.font("Georgia", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        t.setTextOrigin(VPos.CENTER);
        t.applyCss();
        t.setX(x - t.getLayoutBounds().getWidth() / 2.0);
        t.setY(y);
        return t;
    }

    private static double clamp01(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }
}