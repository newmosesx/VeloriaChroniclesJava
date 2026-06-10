package example.practice.gui;

import example.practice.engine.ConflictManager;
import example.practice.engine.PublicWorksManager;
import example.practice.engine.PublicWorksManager.WorkType;
import example.practice.engine.SimulationEngine;
import example.practice.kingdoms.Kingdom;
import example.practice.world.Agriculture;
import example.practice.world.Climate;
import example.practice.world.Geography;
import example.practice.world.Water;
import example.practice.world.World;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
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
import javafx.scene.shape.Line;
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
 * The in-game world map, now a COMMAND surface, not just a survey. Click a
 * territory and a command drawer slides over the map's right edge: the full
 * live readout (soil, yield, flood, river, snowpack, levee line) plus three
 * public works the throne can order on the spot - levee, irrigation, granary -
 * paid in wood/stone/gold and raised over days you can watch tick down.
 * Completed works are drawn on the map as small structure glyphs.
 *
 * The map also breathes: floodwater pulses over swamped sectors, rain (or
 * snow, below freezing) falls where the weather fronts are, the Imperial Seat
 * has a heartbeat, rivers flow, and metric tints cross-fade when switched.
 *
 * USAGE is unchanged - a drop-in replacement:
 *     Region map = MapPanel.build(engine);
 *     centerContainer.getChildren().add(map);
 *     map.setVisible(false); map.setManaged(false);
 *
 * DATA: every read from the simulation lives in sampleWorld() and is LIVE.
 * Sector id == the CompassDirection ordinal (id == kingdom id). Build orders
 * go through PublicWorksManager under the engine lock.
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
        HEAT ("Heat",  "#3a6ea5", "#c0392b", "cold",   "hot"),
        CONTROL("Control", "#c0392b", "#e8c14a", "rebel", "loyal");

        final String label; final Color lo, hi; final String loT, hiT;
        Metric(String label, String lo, String hi, String loT, String hiT) {
            this.label = label; this.lo = Color.web(lo); this.hi = Color.web(hi);
            this.loT = loT; this.hiT = hiT;
        }
    }
    private static final double HEAT_LO = -5, HEAT_HI = 35;

    // ---- one territory -----------------------------------------------------
    private static final class Sector {
        final String dir, region; final boolean capital;
        final int id;                          // world sector id; -1 for the capital
        double soil, yield, flood, tempC;      // live values
        double river, snow, levee, precip;     // live values for the drawer + weather
        double control = 1.0;
        String crop = "", waterCond = "";
        Polygon poly; Text badge; double cx, cy;
        Polygon floodOverlay;                  // pulsing water sheet
        FadeTransition floodPulse;
        Group weather;                         // rain/snow streaks
        Text[] workGlyphs = new Text[WorkType.values().length];
        Sector(String dir, String region, boolean capital, int id) {
            this.dir = dir; this.region = region; this.capital = capital; this.id = id;
        }
        double value(Metric m) {
            switch (m) { case SOIL: return soil; case YIELD: return yield;
                case FLOOD: return flood; case CONTROL: return control;
                default: return tempC; }
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
    private static final double DRAWER_W = 252;

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

    // --- command drawer state ---
    private Sector selected = null;
    private VBox drawer;
    private Label dTitle, dSub, dEnviron, dWater, dWorksNote;
    private final Label[] workState = new Label[WorkType.values().length];
    private final Button[] workBtn = new Button[WorkType.values().length];
    private final ProgressBar[] workBar = new ProgressBar[WorkType.values().length];
    private TranslateTransition drawerSlide;

    private MapPanel(SimulationEngine engine) {
        this.engine = engine;
        this.geo = engine.getWorld().geography;
        this.root = buildUi();
        startRefresh();
    }

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
        buildDrawer();
        StackPane canvasHost = new StackPane(canvas, drawer);
        StackPane.setAlignment(drawer, Pos.CENTER_RIGHT);
        canvasHost.setStyle("-fx-background-color: #12130e; -fx-background-radius: 8;");
        canvasHost.setPadding(new Insets(6));
        // clip the drawer's off-screen rest position so it doesn't widen the panel
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(W + 12, H + 12);
        canvasHost.setClip(clip);

        ramp = new Region();
        ramp.setPrefSize(180, 9);
        ramp.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(ramp, Priority.ALWAYS);
        legLo = small("dry"); legHi = small("wet");
        HBox legend = new HBox(8, legLo, ramp, legHi);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(8, 4, 0, 4));

        readout = new Label("A hand-drawn survey  \u00b7  click any territory to command it");
        readout.setStyle("-fx-text-fill: #c3b78f; -fx-font-size: 12px; -fx-padding: 8 4 0 4;");

        VBox box = new VBox(4, bar, hud, canvasHost, legend, readout);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: #191b16; -fx-border-color: #3a3a2c; -fx-border-radius: 12; -fx-background-radius: 12;");
        box.setMaxWidth(640);
        applyMetricVisuals(false);
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
        // the capital's heartbeat - slow, regal
        ScaleTransition beat = new ScaleTransition(Duration.seconds(2.2), ring);
        beat.setFromX(1.0); beat.setFromY(1.0);
        beat.setToX(1.12);  beat.setToY(1.12);
        beat.setAutoReverse(true);
        beat.setCycleCount(Animation.INDEFINITE);
        beat.play();

        for (Sector s : sectors) {
            if (s.capital) {
                c.getChildren().add(label("The Imperial Seat", s.cx, s.cy + 16, 11, true));
                continue;
            }
            c.getChildren().add(label(s.dir, s.cx, s.cy - 4, 12, true));
            c.getChildren().add(label(s.region, s.cx, s.cy + 9, 10, false));
            s.badge = label("", s.cx, s.cy + 26, 14, true);
            c.getChildren().add(s.badge);
        }

        // flood overlays + weather + structure glyphs sit ABOVE the labels
        for (Sector s : sectors) {
            if (s.capital) continue;
            addFloodOverlay(c, s);
            addWeather(c, s);
            addWorkGlyphs(c, s);
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
            p.setOnMouseClicked(ev -> closeDrawer());
        } else {
            p.setOnMouseClicked(ev -> selectSector(s));
            p.setOnMouseEntered(ev -> { if (s != selected) p.setOpacity(0.85); });
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

    // ------------------------------------------------------- ambient visuals
    // A translucent water sheet over the sector that pulses while it's flooded.
    private void addFloodOverlay(Pane c, Sector s) {
        Polygon overlay = new Polygon(s.poly.getPoints().stream().mapToDouble(Double::doubleValue).toArray());
        overlay.setFill(Color.web("#3f8ac9", 0.30));
        overlay.setStroke(null);
        overlay.setMouseTransparent(true);
        overlay.setVisible(false);
        s.floodOverlay = overlay;
        FadeTransition pulse = new FadeTransition(Duration.seconds(1.4), overlay);
        pulse.setFromValue(0.12); pulse.setToValue(0.45);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        s.floodPulse = pulse;
        c.getChildren().add(overlay);
    }

    // Three falling streaks per sector: blue drizzle in rain, white drift in snow.
    private void addWeather(Pane c, Sector s) {
        Group g = new Group();
        for (int i = 0; i < 3; i++) {
            double x = s.cx - 26 + i * 24 + (i % 2) * 5;
            Line streak = new Line(x, s.cy - 30, x - 3, s.cy - 18);
            streak.setStrokeWidth(1.6);
            streak.setStroke(Color.web("#7fb4e0", 0.7));
            streak.setStrokeLineCap(StrokeLineCap.ROUND);
            g.getChildren().add(streak);
        }
        g.setMouseTransparent(true);
        g.setVisible(false);
        TranslateTransition fall = new TranslateTransition(Duration.seconds(0.9), g);
        fall.setFromY(0); fall.setToY(26);
        fall.setCycleCount(Animation.INDEFINITE);
        fall.play();
        FadeTransition fade = new FadeTransition(Duration.seconds(0.9), g);
        fade.setFromValue(0.9); fade.setToValue(0.0);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.play();
        s.weather = g;
        c.getChildren().add(g);
    }

    // Small structure glyphs in the sector's lower-left: levee, irrigation, granary.
    private void addWorkGlyphs(Pane c, Sector s) {
        String[] icons = { "\u2630", "\u2248", "\u2302" };   // levee bars, water waves, house
        for (int t = 0; t < WorkType.values().length; t++) {
            Text g = glyph(icons[t], s.cx - 22 + t * 22, s.cy + 42, 13, Color.web("#e3d5a8", 0.95), true);
            g.setEffect(new DropShadow(3, Color.web("#000", 0.8)));
            g.setMouseTransparent(true);
            g.setVisible(false);
            s.workGlyphs[t] = g;
            c.getChildren().add(g);
        }
    }

    // ------------------------------------------------------ command drawer
    private void buildDrawer() {
        dTitle = new Label();
        dTitle.setStyle("-fx-text-fill:#e3d5a8; -fx-font-family:'Georgia'; -fx-font-size:15px; -fx-font-weight:bold;");
        dSub = new Label();
        dSub.setStyle("-fx-text-fill:#bcb39a; -fx-font-style:italic; -fx-font-size:11px;");
        Button close = new Button("\u2715");
        close.setStyle("-fx-background-color:transparent; -fx-text-fill:#8c8460; -fx-font-size:12px; -fx-cursor:hand;");
        close.setOnAction(e -> closeDrawer());
        Region spring = new Region();
        HBox.setHgrow(spring, Priority.ALWAYS);
        HBox head = new HBox(6, new VBox(1, dTitle, dSub), spring, close);
        head.setAlignment(Pos.TOP_LEFT);

        dEnviron = new Label();
        dEnviron.setWrapText(true);
        dEnviron.setStyle("-fx-text-fill:#d8d2bd; -fx-font-size:11px;");
        dWater = new Label();
        dWater.setWrapText(true);
        dWater.setStyle("-fx-text-fill:#9fc2dd; -fx-font-size:11px;");

        Label worksTitle = new Label("PUBLIC WORKS");
        worksTitle.setStyle("-fx-text-fill:#c3b78f; -fx-font-size:10px; -fx-padding:6 0 0 0;");
        dWorksNote = new Label();
        dWorksNote.setStyle("-fx-text-fill:#8c8460; -fx-font-size:10px;");

        VBox works = new VBox(8);
        WorkType[] types = WorkType.values();
        for (int t = 0; t < types.length; t++) {
            final WorkType w = types[t];
            Label name = new Label(w.title + "   " + costText(w));
            name.setStyle("-fx-text-fill:#eaeaea; -fx-font-size:11px; -fx-font-weight:bold;");
            Label desc = new Label(w.description);
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill:#8a8a8a; -fx-font-size:10px;");

            workState[t] = new Label();
            workState[t].setStyle("-fx-text-fill:#e8c14a; -fx-font-size:10px;");
            workBar[t] = new ProgressBar(0);
            workBar[t].setPrefWidth(110);
            workBar[t].setMinHeight(9);
            workBar[t].setStyle("-fx-accent:#a89a5c;");
            workBtn[t] = new Button("Build");
            workBtn[t].setOnAction(e -> orderWork(w));

            HBox stateRow = new HBox(8, workBtn[t], workBar[t], workState[t]);
            stateRow.setAlignment(Pos.CENTER_LEFT);
            VBox row = new VBox(3, name, desc, stateRow);
            row.setPadding(new Insets(6));
            row.setStyle("-fx-background-color:#1f211a; -fx-background-radius:6;");
            works.getChildren().add(row);
        }

        drawer = new VBox(8, head, dEnviron, dWater, worksTitle, works, dWorksNote);
        drawer.setPadding(new Insets(12));
        drawer.setPrefWidth(DRAWER_W);
        drawer.setMaxWidth(DRAWER_W);
        drawer.setMaxHeight(H);
        drawer.setStyle("-fx-background-color:#191b16ee; -fx-border-color:#3a3a2c; "
                + "-fx-border-radius:10 0 0 10; -fx-background-radius:10 0 0 10;");
        drawer.setTranslateX(DRAWER_W + 20);     // resting off-canvas

        drawerSlide = new TranslateTransition(Duration.millis(260), drawer);
    }

    private static String costText(WorkType w) {
        StringBuilder b = new StringBuilder("(");
        if (w.wood > 0)  b.append(w.wood).append("w ");
        if (w.stone > 0) b.append(w.stone).append("s ");
        if (w.gold > 0)  b.append(w.gold).append("g ");
        b.setLength(b.length() - 1);
        return b.append(", ").append(w.days).append("d)").toString();
    }

    private void selectSector(Sector s) {
        if (selected != null && selected != s) clearHighlight(selected);
        selected = s;
        s.poly.setOpacity(1.0);
        s.poly.setStroke(Color.web("#e8c14a"));
        s.poly.setStrokeWidth(3);
        s.poly.setEffect(new DropShadow(14, Color.web("#e8c14a", 0.55)));
        showReadout(s);
        refreshDrawer();
        drawerSlide.stop();
        drawerSlide.setToX(0);
        drawerSlide.play();
    }

    private void closeDrawer() {
        if (selected != null) clearHighlight(selected);
        selected = null;
        drawerSlide.stop();
        drawerSlide.setToX(DRAWER_W + 20);
        drawerSlide.play();
    }

    private void clearHighlight(Sector s) {
        s.poly.setStroke(Color.web("#0d0e0a"));
        s.poly.setStrokeWidth(1.5);
        s.poly.setEffect(null);
    }

    private void orderWork(WorkType w) {
        if (selected == null || selected.id < 0) return;
        Kingdom empire = engine.getKingdoms()[0];
        engine.lock();
        try { PublicWorksManager.start(empire, selected.id, w); }
        finally { engine.unlock(); }
        refresh();   // immediate feedback: button greys, bar appears
    }

    private void refreshDrawer() {
        if (selected == null) return;
        Sector s = selected;
        dTitle.setText(s.dir + "  \u00b7  " + s.region);
        dSub.setText(s.crop.isEmpty() ? "surveyed territory" : s.crop);
        dEnviron.setText("Soil " + Math.round(s.soil) + "%   \u00b7   Yield " + Math.round(s.yield) + "%"
                + "   \u00b7   " + Math.round(s.tempC) + "\u00b0C"
                + "\nControl " + Math.round(s.control * 100) + "%");
        dWater.setText("River " + Math.round(s.river * 100) + "%   \u00b7   Snowpack " + Math.round(s.snow * 100) + "%"
                + "\nFlood " + Math.round(s.flood) + "%   \u00b7   Levee line +" + Math.round(s.levee * 100)
                + (s.waterCond == null || s.waterCond.isEmpty() ? "" : "   \u00b7   " + s.waterCond));

        Kingdom empire = engine.getKingdoms()[0];
        WorkType[] types = WorkType.values();
        boolean busy = PublicWorksManager.sectorBusy(s.id);
        for (int t = 0; t < types.length; t++) {
            PublicWorksManager.State st = PublicWorksManager.stateOf(s.id, types[t]);
            switch (st) {
                case BUILT:
                    workBtn[t].setVisible(false); workBtn[t].setManaged(false);
                    workBar[t].setVisible(false); workBar[t].setManaged(false);
                    workState[t].setText("\u2713 standing");
                    workState[t].setStyle("-fx-text-fill:#5fd17a; -fx-font-size:10px; -fx-font-weight:bold;");
                    break;
                case BUILDING:
                    workBtn[t].setVisible(false); workBtn[t].setManaged(false);
                    workBar[t].setVisible(true); workBar[t].setManaged(true);
                    workBar[t].setProgress(PublicWorksManager.progressOf(s.id, types[t]));
                    workState[t].setText(PublicWorksManager.daysLeftOf(s.id, types[t]) + "d left");
                    workState[t].setStyle("-fx-text-fill:#e8c14a; -fx-font-size:10px;");
                    break;
                default:
                    workBar[t].setVisible(false); workBar[t].setManaged(false);
                    workBtn[t].setVisible(true); workBtn[t].setManaged(true);
                    boolean can = PublicWorksManager.canStart(empire, s.id, types[t]);
                    workBtn[t].setDisable(!can);
                    workBtn[t].setStyle("-fx-font-size:10px; -fx-background-radius:5; -fx-padding:3 10; -fx-cursor:hand; "
                            + "-fx-text-fill:white; -fx-background-color:" + (can ? "#1f7a3f" : "#3a3a3a") + ";");
                    workState[t].setText(busy ? "" : (can ? "" : "can't afford"));
                    workState[t].setStyle("-fx-text-fill:#8a5a5a; -fx-font-size:10px;");
            }
        }
        dWorksNote.setText(busy ? "Workers are already raising something here."
                : "One project per territory at a time.");
    }

    // ------------------------------------------------------------------ logic
    private void selectMetric(Metric m) {
        current = m;
        for (int i = 0; i < metricButtons.size(); i++)
            styleToggle(metricButtons.get(i), Metric.values()[i] == m);
        applyMetricVisuals(true);
    }

    private void applyMetricVisuals(boolean animate) {
        for (Sector s : sectors) {
            if (s.capital) continue;
            double v = s.value(current);
            double t;
            if (current == Metric.HEAT) t = (v - HEAT_LO) / (HEAT_HI - HEAT_LO);
            else if (current == Metric.CONTROL) t = (v + 1) / 2.0;
            else t = v / 100.0;
            Color target = current.lo.interpolate(current.hi, clamp01(t));
            if (animate && s.poly.getFill() instanceof Color) {
                FillTransition ft = new FillTransition(Duration.millis(420), s.poly,
                        (Color) s.poly.getFill(), target);
                ft.play();
            } else {
                s.poly.setFill(target);
            }
            if (s.badge != null)
                s.badge.setText(current == Metric.CONTROL
                        ? Math.round(v * 100) + ""
                        : String.valueOf(Math.round(v)));
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
                + "  \u00b7  control " + Math.round(s.control * 100) + "%"
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
        applyMetricVisuals(false);
        applyAmbientVisuals();
        if (selected != null) { showReadout(selected); refreshDrawer(); }
    }

    /** The single data tap -- fully LIVE off the world systems. */
    private void sampleWorld() {
        Kingdom empire = engine.getKingdoms()[0];
        double unrest = empire.unrestLevel;
        double frac = clamp01(unrest / 2500.0);
        unrestBar.setProgress(frac);
        unrestLabel.setText((int) unrest + "  \u00b7  " + ConflictManager.stateOf(empire));
        String accent = frac > 0.8 ? "#c0392b" : frac > 0.4 ? "#d98a2b" : "#5a9e4a";
        unrestBar.setStyle("-fx-accent: " + accent + ";");

        World w = engine.getWorld();
        Agriculture ag = w.agriculture;
        Water wa = w.water;
        Climate cl = w.climate;
        for (Sector s : sectors) {
            if (s.id < 0) continue;
            s.soil  = ag.soilMoisture[s.id] * 100.0;
            s.yield = ag.yield[s.id]        * 100.0;
            s.crop  = ag.cropState[s.id];
            s.flood = wa.floodSeverity[s.id] * 100.0;
            s.river = wa.riverLevel[s.id];
            s.snow  = wa.snowpack[s.id];
            s.levee = wa.leveeHeight[s.id];
            s.waterCond = wa.condition[s.id];
            s.tempC = cl.temperature[s.id];
            s.precip = cl.precipitation[s.id];
            s.control = ConflictManager.sectorControl(s.id);
        }
    }

    // Flood pulses, falling weather, and structure glyphs - driven by the sample.
    private void applyAmbientVisuals() {
        for (Sector s : sectors) {
            if (s.capital) continue;

            boolean flooded = s.flood > 35;
            if (flooded && !s.floodOverlay.isVisible()) {
                s.floodOverlay.setVisible(true);
                s.floodPulse.play();
            } else if (!flooded && s.floodOverlay.isVisible()) {
                s.floodPulse.stop();
                s.floodOverlay.setVisible(false);
            }

            boolean wet = s.precip > 0.45;
            s.weather.setVisible(wet);
            if (wet) {
                Color streak = s.tempC < 0 ? Color.web("#e8eef4", 0.85) : Color.web("#7fb4e0", 0.7);
                for (javafx.scene.Node n : s.weather.getChildren())
                    if (n instanceof Line) ((Line) n).setStroke(streak);
            }

            WorkType[] types = WorkType.values();
            for (int t = 0; t < types.length; t++)
                s.workGlyphs[t].setVisible(
                        PublicWorksManager.stateOf(s.id, types[t]) == PublicWorksManager.State.BUILT);
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