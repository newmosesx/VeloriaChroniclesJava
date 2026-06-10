package example.practice.gui;

import example.practice.config.FactionType;
import example.practice.engine.DemandManager;
import example.practice.engine.Faction;
import example.practice.engine.FactionDemand;
import example.practice.engine.PoliticsManager;
import example.practice.engine.SimulationEngine;
import example.practice.kingdoms.Kingdom;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

// The court, made visible. One row per estate showing how angry they are
// (grievance), how much weight they can throw (power), and the product the
// realm actually feels (pressure). When an estate's pressure boils over it
// raises a demand - shown here as a banner with Concede / Refuse, the political
// mirror of a battle order. Reads PoliticsManager + DemandManager under the lock
// and refreshes once a second, matching GovernancePanel.
public class FactionPanel {

    public static Region build(SimulationEngine engine) {
        Kingdom empire = engine.getKingdoms()[0];

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color:#1c1c1e;");

        Label title = new Label("The Estates");
        title.setStyle("-fx-text-fill:#f0f0f0; -fx-font-size:18px; -fx-font-weight:bold;");
        Label sub = new Label("Anger lingers; power is the capacity to act on it. Pressure is what the realm feels.");
        sub.setStyle("-fx-text-fill:#8a8a8a; -fx-font-size:11px;");
        sub.setWrapText(true);
        VBox header = new VBox(2, title, sub);

        // one row of bars per estate, kept for live updates
        VBox estates = new VBox(10);
        FactionType[] types = FactionType.values();
        ProgressBar[] grievBars = new ProgressBar[types.length];
        ProgressBar[] powerBars = new ProgressBar[types.length];
        Label[] pressureLbls = new Label[types.length];

        for (int i = 0; i < types.length; i++) {
            Label name = new Label(types[i].title);
            name.setStyle("-fx-text-fill:#eaeaea; -fx-font-size:13px; -fx-font-weight:bold;");
            Label pressure = new Label();
            pressure.setStyle("-fx-text-fill:#e8c14a; -fx-font-size:12px;");
            pressureLbls[i] = pressure;
            Region spring = new Region();
            HBox.setHgrow(spring, Priority.ALWAYS);
            HBox nameRow = new HBox(8, name, spring, pressure);
            nameRow.setAlignment(Pos.CENTER_LEFT);

            grievBars[i] = new ProgressBar(0);
            grievBars[i].setMinHeight(13);
            grievBars[i].setStyle("-fx-accent:#c0392b;");
            powerBars[i] = new ProgressBar(0);
            powerBars[i].setMinHeight(13);
            powerBars[i].setStyle("-fx-accent:#5a9ed6;");

            VBox row = new VBox(4, nameRow, captioned("anger", grievBars[i]), captioned("power", powerBars[i]));
            row.setPadding(new Insets(8));
            row.setStyle("-fx-background-color:#242427; -fx-background-radius:8;");
            estates.getChildren().add(row);
        }

        // --- demand banner (hidden until an estate boils over) ---
        VBox demandBox = new VBox(8);
        demandBox.setPadding(new Insets(12));
        demandBox.setStyle("-fx-background-color:#2c2118; -fx-background-radius:8; -fx-border-color:#a86a2c; -fx-border-radius:8;");
        Label demandTitle = new Label();
        demandTitle.setStyle("-fx-text-fill:#f0c070; -fx-font-size:13px; -fx-font-weight:bold;");
        demandTitle.setWrapText(true);
        Label demandBody = new Label();
        demandBody.setStyle("-fx-text-fill:#d8c4a0; -fx-font-size:12px;");
        demandBody.setWrapText(true);
        Button concede = new Button();
        Button refuse = new Button("Refuse");
        concede.setOnAction(ev -> { engine.lock(); try { DemandManager.concede(empire); } finally { engine.unlock(); } });
        refuse.setOnAction(ev -> { engine.lock(); try { DemandManager.refuse(empire); } finally { engine.unlock(); } });
        HBox demandBtns = new HBox(10, concede, refuse);
        demandBtns.setAlignment(Pos.CENTER_LEFT);
        demandBox.getChildren().addAll(demandTitle, demandBody, demandBtns);
        demandBox.setVisible(false);
        demandBox.setManaged(false);

        root.getChildren().addAll(header, demandBox, estates);

        Runnable refresh = () -> {
            engine.lock();
            try {
                Faction[] f = PoliticsManager.factionsOf(empire);
                for (int i = 0; i < types.length; i++) {
                    Faction fac = f[types[i].ordinal()];
                    grievBars[i].setProgress(fac.grievance);
                    powerBars[i].setProgress(fac.power);
                    pressureLbls[i].setText("pressure " + Math.round(fac.pressure() * 100) + "%");
                }

                FactionDemand d = DemandManager.current(empire);
                if (d != null) {
                    demandBox.setVisible(true);
                    demandBox.setManaged(true);
                    demandTitle.setText("The " + d.from.title + " demand:  " + d.headline);
                    String cost = d.goldCost + "g"
                            + (d.foodCost > 0 ? " + " + d.foodCost + " food" : "")
                            + (d.rival != null ? "  \u2014  will anger the " + d.rival.title : "");
                    demandBody.setText("Patience: " + Math.max(0, d.daysLeft) + " days.   Concede costs " + cost + ".");
                    concede.setText(d.concedeLabel + " (" + d.goldCost + "g)");
                    concede.setDisable(empire.gold < d.goldCost);
                    styleConcede(concede, empire.gold >= d.goldCost);
                    styleRefuse(refuse);
                } else {
                    demandBox.setVisible(false);
                    demandBox.setManaged(false);
                }
            } finally {
                engine.unlock();
            }
        };
        refresh.run();

        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), ev -> refresh.run()));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();

        return root;
    }

    // a bar with a small fixed-width caption to its left
    private static HBox captioned(String caption, ProgressBar pb) {
        Label c = new Label(caption);
        c.setMinWidth(42);
        c.setStyle("-fx-text-fill:#8a8a8a; -fx-font-size:10px;");
        HBox.setHgrow(pb, Priority.ALWAYS);
        pb.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(8, c, pb);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static void styleConcede(Button b, boolean affordable) {
        b.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:6; -fx-padding:5 12; -fx-cursor:hand; "
                + "-fx-background-color:" + (affordable ? "#1f7a3f" : "#3a3a3a") + ";");
    }
    private static void styleRefuse(Button b) {
        b.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:6; -fx-padding:5 12; -fx-cursor:hand; "
                + "-fx-background-color:#8a2f2f;");
    }
}