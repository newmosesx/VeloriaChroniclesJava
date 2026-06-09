package example.practice.gui;

import example.practice.engine.SimulationEngine;
import example.practice.engine.EdictManager;
import example.practice.engine.TechManager;
import example.practice.engine.ConflictManager;
import example.practice.engine.ConflictState;
import example.practice.engine.MilitaryPosture;
import example.practice.config.EdictType;
import example.practice.config.TechType;
import example.practice.kingdoms.Kingdom;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;

// Drop-in governance UI. Reads/writes shared state under the engine lock and
// refreshes once a second so gold and tech progress stay live. (Treasury field is
// kingdom.gold to match your project.)
public class GovernancePanel {

    public static Region build(SimulationEngine engine) {
        Kingdom empire = engine.getKingdoms()[0];

        VBox root = new VBox(14);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color:#1c1c1e;");

        Label title = new Label("Governance & Edicts");
        title.setStyle("-fx-text-fill:#f0f0f0; -fx-font-size:18px; -fx-font-weight:bold;");
        Label gold = new Label();
        gold.setStyle("-fx-text-fill:#e8c14a; -fx-font-size:13px;");
        Label slots = new Label();
        slots.setStyle("-fx-text-fill:#b0b0b0; -fx-font-size:12px;");
        VBox header = new VBox(2, title, gold, slots);

        // --- EDICTS: each a red/green switch, two slots only ---
        VBox edictBox = new VBox(8);
        Map<EdictType, Button> switches = new EnumMap<>(EdictType.class);
        for (EdictType e : EdictType.values()) {
            VBox info = new VBox(1);
            Label name = new Label(e.title + "   (" + e.dailyCost + "g/day)");
            name.setStyle("-fx-text-fill:#eaeaea; -fx-font-size:13px; -fx-font-weight:bold;");
            Label desc = new Label(e.description);
            desc.setStyle("-fx-text-fill:#8a8a8a; -fx-font-size:11px;");
            desc.setWrapText(true);
            info.getChildren().addAll(name, desc);
            info.setMaxWidth(Double.MAX_VALUE);

            Button sw = new Button();
            sw.setMinWidth(64);
            sw.setOnAction(ev -> {
                engine.lock();
                try { EdictManager.toggle(empire, e); }
                finally { engine.unlock(); }
            });
            switches.put(e, sw);

            HBox row = new HBox(10, info, sw);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(info, Priority.ALWAYS);
            edictBox.getChildren().add(row);
        }

        // --- TECHNOLOGY: research, then watch it roll in ---
        Label techTitle = new Label("Technology");
        techTitle.setStyle("-fx-text-fill:#f0f0f0; -fx-font-size:16px; -fx-font-weight:bold;");
        VBox techBox = new VBox(8);
        Map<TechType, HBox> techControls = new EnumMap<>(TechType.class);
        for (TechType t : TechType.values()) {
            VBox info = new VBox(1);
            Label name = new Label(t.title + "   (" + t.cost + "g, " + t.days + "d)");
            name.setStyle("-fx-text-fill:#eaeaea; -fx-font-size:13px; -fx-font-weight:bold;");
            Label eff = new Label("improves " + t.effect.name().toLowerCase()
                    + " by " + Math.round(t.magnitude * 100) + "%");
            eff.setStyle("-fx-text-fill:#8a8a8a; -fx-font-size:11px;");
            info.getChildren().addAll(name, eff);
            info.setMaxWidth(Double.MAX_VALUE);

            HBox control = new HBox(6);
            control.setAlignment(Pos.CENTER_RIGHT);
            control.setMinWidth(150);
            techControls.put(t, control);

            HBox row = new HBox(10, info, control);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(info, Priority.ALWAYS);
            techBox.getChildren().add(row);
        }

        // --- MILITARY & CONFLICT: state, organization, readiness, posture command ---
        Label milTitle = new Label("Military & Conflict");
        milTitle.setStyle("-fx-text-fill:#f0f0f0; -fx-font-size:16px; -fx-font-weight:bold;");
        Label conflictLbl = new Label();
        conflictLbl.setStyle("-fx-text-fill:#e8c14a; -fx-font-size:13px; -fx-font-weight:bold;");

        Label orgCap = new Label("Rebellion organization");
        orgCap.setStyle("-fx-text-fill:#b0b0b0; -fx-font-size:11px;");
        ProgressBar orgBar = new ProgressBar(0);
        orgBar.setPrefWidth(180);
        orgBar.setStyle("-fx-accent:#c0392b;");
        Label readyCap = new Label("Army readiness");
        readyCap.setStyle("-fx-text-fill:#b0b0b0; -fx-font-size:11px;");
        ProgressBar readyBar = new ProgressBar(0);
        readyBar.setPrefWidth(180);
        readyBar.setStyle("-fx-accent:#3f8a6a;");

        Label postureCap = new Label("Posture  (runs on Auto until you override)");
        postureCap.setStyle("-fx-text-fill:#b0b0b0; -fx-font-size:11px;");
        Button bAuto = new Button("Auto");
        bAuto.setOnAction(ev -> { engine.lock(); try { ConflictManager.setAuto(empire, true); } finally { engine.unlock(); } });
        Map<MilitaryPosture, Button> postureBtns = new EnumMap<>(MilitaryPosture.class);
        HBox postureRow = new HBox(6, bAuto);
        for (MilitaryPosture mp : MilitaryPosture.values()) {
            String nm = mp.name().charAt(0) + mp.name().substring(1).toLowerCase();
            Button b = new Button(nm);
            b.setOnAction(ev -> { engine.lock(); try { ConflictManager.setPosture(empire, mp); } finally { engine.unlock(); } });
            postureBtns.put(mp, b);
            postureRow.getChildren().add(b);
        }
        VBox milBox = new VBox(6, conflictLbl, orgCap, orgBar, readyCap, readyBar, postureCap, postureRow);

        root.getChildren().addAll(header, new Separator(), edictBox,
                new Separator(), techTitle, techBox,
                new Separator(), milTitle, milBox);

        Runnable refresh = () -> {
            engine.lock();
            try {
                gold.setText("Treasury: " + empire.gold + " gold");
                int active = EdictManager.activeEdicts(empire).size();
                slots.setText("Active edicts: " + active + " / " + EdictManager.MAX_ACTIVE
                        + (active >= EdictManager.MAX_ACTIVE ? "  (slots full)" : ""));

                for (EdictType e : EdictType.values()) {
                    boolean on = EdictManager.isActive(empire, e);
                    Button sw = switches.get(e);
                    sw.setText(on ? "ON" : "OFF");
                    sw.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:6; "
                            + "-fx-background-color:" + (on ? "#1f7a3f" : "#8a2f2f") + ";");
                }

                for (TechType t : TechType.values()) {
                    HBox control = techControls.get(t);
                    control.getChildren().clear();
                    TechManager.State st = TechManager.stateOf(t);
                    if (st == TechManager.State.ACTIVE) {
                        Label l = new Label("\u2713 Active");
                        l.setStyle("-fx-text-fill:#5fd17a; -fx-font-weight:bold; -fx-font-size:12px;");
                        control.getChildren().add(l);
                    } else if (st == TechManager.State.IMPLEMENTING) {
                        ProgressBar pb = new ProgressBar(TechManager.progressOf(t));
                        pb.setPrefWidth(80);
                        Label l = new Label("rolling out " + Math.round(TechManager.progressOf(t) * 100) + "%");
                        l.setStyle("-fx-text-fill:#e8c14a; -fx-font-size:11px;");
                        control.getChildren().addAll(pb, l);
                    } else if (st == TechManager.State.RESEARCHING) {
                        Label l = new Label("researching: " + TechManager.daysLeft(t) + "d");
                        l.setStyle("-fx-text-fill:#7fa8d8; -fx-font-size:12px;");
                        control.getChildren().add(l);
                    } else { // LOCKED
                        Button b = new Button("Research");
                        b.setDisable(empire.gold < t.cost);
                        b.setOnAction(ev -> {
                            engine.lock();
                            try { TechManager.startResearch(t, empire); }
                            finally { engine.unlock(); }
                        });
                        control.getChildren().add(b);
                    }
                }

                ConflictState cs = ConflictManager.stateOf(empire);
                conflictLbl.setText("State:  " + cs);
                orgBar.setProgress(ConflictManager.organizationOf(empire));
                readyBar.setProgress(ConflictManager.readinessOf(empire));
                boolean auto = ConflictManager.isAuto(empire);
                MilitaryPosture cur = ConflictManager.posture(empire);
                styleMil(bAuto, auto);
                for (MilitaryPosture mp : MilitaryPosture.values())
                    styleMil(postureBtns.get(mp), !auto && mp == cur);
            } finally {
                engine.unlock();
            }
        };
        refresh.run();

        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), ev -> refresh.run()));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(440);
        scroll.setStyle("-fx-background:#1c1c1e; -fx-background-color:#1c1c1e;");
        return scroll;
    }

    private static void styleMil(Button b, boolean on) {
        b.setStyle("-fx-text-fill:white; -fx-font-size:12px; -fx-background-radius:6; -fx-padding:4 10; -fx-cursor:hand; "
                + "-fx-background-color:" + (on ? "#4a4528" : "#2b2b2b") + ";"
                + (on ? " -fx-border-color:#a89a5c; -fx-border-radius:6;" : ""));
    }
}