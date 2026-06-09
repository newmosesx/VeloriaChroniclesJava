package example.practice.gui;

import example.practice.engine.ConflictManager;
import example.practice.engine.SimulationEngine;
import example.practice.report.KingdomReport;
import example.practice.report.WorldReport;
import example.practice.world.Agriculture;
import example.practice.world.Climate;
import example.practice.world.Geography;
import example.practice.world.World;
import example.practice.logger.Logger;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import static example.practice.logger.Logger.LogCategory.*;

// A read-only live inspector for verifying the world systems before the Director
// exists. Opens its own window; polls a locked snapshot of the engine ~1/sec.
// Open it (on the JavaFX thread) with: AdminDashboard.show(engine);
public class AdminDashboard {

    private static final String MONO =
            "-fx-font-family: 'monospace'; -fx-font-size: 12px; "
                    + "-fx-control-inner-background: #0f0f0f; -fx-text-fill: #dddddd;";

    public static void show(SimulationEngine engine) {
        Stage window = new Stage();
        window.setTitle("Veloria - World Inspector (Admin)");

        Label header = new Label("Booting inspector...");
        header.setStyle("-fx-text-fill: #ffc107; -fx-font-size: 14px; -fx-font-family: 'monospace';");
        header.setPadding(new Insets(10));

        TextArea worldArea = mono();
        TextArea envArea = mono();

        TextArea logStory = mono(), logMil = mono(), logNat = mono(), logPol = mono();
        GridPane logs = new GridPane();
        logs.setHgap(8); logs.setVgap(8); logs.setPadding(new Insets(8));
        logs.add(titled("Story", logStory), 0, 0);
        logs.add(titled("Military", logMil), 1, 0);
        logs.add(titled("Natural", logNat), 0, 1);
        logs.add(titled("Political", logPol), 1, 1);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(50); logs.getColumnConstraints().add(cc);
            RowConstraints rc = new RowConstraints(); rc.setPercentHeight(50); logs.getRowConstraints().add(rc);
        }

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("Environment", envArea),
                new Tab("Kingdoms", worldArea),
                new Tab("Logs", logs));

        CheckBox auto = new CheckBox("Auto-refresh");
        auto.setSelected(true);
        auto.setStyle("-fx-text-fill: #dddddd;");
        Button now = new Button("Refresh now");
        Label stamp = new Label();
        stamp.setStyle("-fx-text-fill: #888888; -fx-font-family: 'monospace';");
        HBox bar = new HBox(12, auto, now, stamp);
        bar.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #191919;");
        root.setTop(header);
        root.setCenter(tabs);
        root.setBottom(bar);

        Runnable refresh = () -> {
            String h, w, e;
            engine.lock();
            try {
                WorldReport rep = WorldReport.from(engine);
                World world = engine.getWorld();
                h = buildHeader(rep);
                w = buildKingdoms(rep, engine);
                e = buildEnvironment(world);
            } finally {
                engine.unlock();
            }
            header.setText(h);
            preserveScroll(worldArea, w);
            preserveScroll(envArea, e);
            logStory.setText(Logger.peek(STORY));
            logMil.setText(Logger.peek(MILITARY));
            logNat.setText(Logger.peek(NATURAL));
            logPol.setText(Logger.peek(POLITICAL));
            stamp.setText("updated " + System.currentTimeMillis() % 100000);
        };

        now.setOnAction(ev -> refresh.run());

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> { if (auto.isSelected()) refresh.run(); }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        window.setOnHidden(ev -> timeline.stop());

        refresh.run();
        window.setScene(new Scene(root, 900, 620));
        window.show();
    }

    // Drop-in button: add AdminDashboard.button(engine) to any layout (a toolbar,
    // a VBox, MainGUI's controls) and it opens the inspector on click. Styled to
    // match the dark UI.
    public static Button button(SimulationEngine engine) {
        Button b = new Button("World Inspector");
        b.setStyle("-fx-background-color: #292929; -fx-text-fill: #dddddd; -fx-border-color: #444444; -fx-border-radius: 4;");
        b.setOnAction(e -> show(engine));
        return b;
    }

    private static TextArea mono() {
        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setStyle(MONO);
        return ta;
    }

    private static VBox titled(String name, TextArea area) {
        Label l = new Label(name);
        l.setStyle("-fx-text-fill: #ffc107; -fx-font-family: 'monospace';");
        VBox box = new VBox(4, l, area);
        VBox.setVgrow(area, Priority.ALWAYS);
        return box;
    }

    private static void preserveScroll(TextArea area, String text) {
        if (!text.equals(area.getText())) area.setText(text);
    }

    private static String buildHeader(WorldReport rep) {
        String env = rep.environment == null ? "" :
                String.format("  |  %s y%d  %.1fC %s  rain %.0f%%",
                        rep.environment.season, rep.environment.year, rep.environment.temperature,
                        rep.environment.warming ? "up" : "down", rep.environment.precipitationTendency * 100);
        return String.format("%s   |   pop %d   |   stability %d%%   |   %s%s",
                rep.time, rep.worldPopulation, Math.round(rep.worldStabilityIndex * 100),
                rep.civilWarStatus == null ? "-" : rep.civilWarStatus, env);
    }

    private static String buildEnvironment(World world) {
        Climate cl = world.climate;
        Agriculture ag = world.agriculture;
        Geography geo = world.geography;

        StringBuilder b = new StringBuilder();
        b.append(world.calendar.reportLine()).append("\n");
        b.append(cl.reportLine()).append("\n");
        b.append(ag.reportLine()).append("\n\n");

        b.append(String.format("%-22s %6s %5s %5s %-11s %5s %6s %7s %-16s%n",
                "Region", "Temp", "Rain", "Wind", "Sky", "Soil", "Yield", "Loyal%", "Crop"));
        b.append("----------------------------------------------------------------------------------------------\n");
        for (int i = 0; i < geo.count(); i++) {
            String region = geo.sector(i).region() + (geo.sector(i).coastal ? " *" : "");
            float loyal = ConflictManager.sectorControl(i) * 100;
            b.append(String.format("%-22s %5.1fC %4.0f%% %4.0f%% %-11s %4.0f%% %5.0f%% %6.0f%% %-16s%n",
                    region,
                    cl.temperature[i],
                    cl.precipitation[i] * 100,
                    cl.windStrength[i] * 100,
                    cl.condition[i],
                    ag.soilMoisture[i] * 100,
                    ag.yield[i] * 100,
                    loyal,
                    ag.cropState[i]));
        }
        b.append("\n(* coastal sector)\n");
        return b.toString();
    }

    private static String buildKingdoms(WorldReport rep, SimulationEngine engine) {
        StringBuilder b = new StringBuilder();
        b.append(String.format("%-22s %-5s %7s %5s %6s %9s %6s %5s %5s %5s %-9s%n",
                "Kingdom", "State", "Pop", "Stab", "Morale", "Food", "FDays", "Sol", "Reb", "Org%", "Weakest"));
        b.append("------------------------------------------------------------------------------------------------------\n");
        for (KingdomReport k : rep.kingdoms) {
            float org = ConflictManager.organizationOf(engine.getKingdoms()[k.id]) * 100;
            b.append(String.format("%-22s %-5s %7d %4d%% %6d %9d %6.1f %5d %5d %4.0f%% %-9s%n",
                    truncate(k.name, 22),
                    k.active ? "ON" : "off",
                    k.population,
                    k.stabilityPercent,
                    k.morale,
                    k.food,
                    k.foodDaysLeft,
                    k.soldiers,
                    k.rebels,
                    org,
                    k.weakestPillar));
        }
        return b.toString();
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "~";
    }
}