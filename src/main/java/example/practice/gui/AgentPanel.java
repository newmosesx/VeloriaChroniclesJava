package example.practice.gui;

import example.practice.agents.Agent;
import example.practice.agents.AgentCombat;
import example.practice.agents.AgentRoster;
import example.practice.engine.SimulationEngine;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

/**
 * The cast, on screen at last: a roster of agents with their portraits, live
 * disposition/stance/renown, and a duel sandbox that runs AgentCombat and plays
 * back the blow-by-blow. Opens its own window (like the World Inspector).
 *
 * Reads engine.getRoster() -- see the 3-line engine hookup. Open with:
 *     AgentPanel.button(engine)   // a styled button, or
 *     AgentPanel.show(engine);
 */
public final class AgentPanel {

    private AgentPanel() {}

    public static Button button(SimulationEngine engine) {
        Button b = new Button("Agents");
        b.setStyle("-fx-background-color: #292929; -fx-text-fill: #dddddd; -fx-border-color: #444444; -fx-border-radius: 4;");
        b.setOnAction(e -> show(engine));
        return b;
    }

    public static void show(SimulationEngine engine) {
        AgentRoster roster = engine.getRoster();

        Stage window = new Stage();
        window.setTitle("Veloria - Agents");

        VBox rosterBox = new VBox(8);
        rosterBox.setPadding(new Insets(12));
        ScrollPane rosterScroll = new ScrollPane(rosterBox);
        rosterScroll.setFitToWidth(true);
        rosterScroll.setPrefWidth(300);
        rosterScroll.setStyle("-fx-background: #161616; -fx-border-color: #333;");

        VBox detail = new VBox(12);
        detail.setPadding(new Insets(18));
        detail.setAlignment(Pos.TOP_CENTER);
        detail.setStyle("-fx-background-color: #1b1b1b;");

        // duel sandbox
        ComboBox<String> left = new ComboBox<>(), right = new ComboBox<>();
        for (Agent a : roster.agents) { left.getItems().add(a.name); right.getItems().add(a.name); }
        if (roster.agents.size() > 0) left.getSelectionModel().select(0);
        if (roster.agents.size() > 1) right.getSelectionModel().select(1);
        styleCombo(left); styleCombo(right);
        Button duelBtn = action("Duel");
        Button assassinBtn = action("Assassinate");
        Button restBtn = action("Rest (heal all)");
        TextArea log = new TextArea();
        log.setEditable(false); log.setWrapText(true); log.setPrefRowCount(7);
        log.setStyle("-fx-control-inner-background: #0f0f0f; -fx-text-fill: #cdc3a8; -fx-font-family: 'monospace';");

        HBox duelRow = new HBox(8, left, new Label("vs"), right, duelBtn, assassinBtn, restBtn);
        duelRow.setAlignment(Pos.CENTER_LEFT);
        ((Label) duelRow.getChildren().get(1)).setStyle("-fx-text-fill: #888;");
        VBox sandbox = new VBox(8, new sectionLabel("DUEL SANDBOX"), duelRow, log);
        sandbox.setPadding(new Insets(12));
        sandbox.setStyle("-fx-background-color: #141414; -fx-border-color: #333; -fx-border-width: 1 0 0 0;");

        // selection state held in a 1-element array so lambdas can write it
        final Agent[] selected = { roster.agents.isEmpty() ? null : roster.agents.get(0) };

        Runnable render = () -> {
            rosterBox.getChildren().clear();
            for (Agent a : roster.agents) {
                rosterBox.getChildren().add(rosterRow(a, a == selected[0], () -> { selected[0] = a; }));
            }
            detail.getChildren().setAll(detailCard(selected[0]));
        };
        // clicking a row re-renders; we rebuild rows each render so re-bind the click via a shared runnable
        Runnable[] renderHolder = { render };
        Runnable doRender = () -> {
            rosterBox.getChildren().clear();
            for (Agent a : roster.agents) {
                Region row = rosterRow(a, a == selected[0], () -> { selected[0] = a; renderHolder[0].run(); });
                rosterBox.getChildren().add(row);
            }
            detail.getChildren().setAll(detailCard(selected[0]));
        };
        renderHolder[0] = doRender;

        duelBtn.setOnAction(e -> {
            Agent a = roster.get(left.getValue()), b = roster.get(right.getValue());
            if (a == null || b == null || a == b) { log.setText("Pick two different agents."); return; }
            engine.lock();
            AgentCombat.Result res;
            try { res = AgentCombat.duel(a, b); } finally { engine.unlock(); }
            log.setText(render(res));
            doRender.run();
        });
        assassinBtn.setOnAction(e -> {
            Agent a = roster.get(left.getValue()), b = roster.get(right.getValue());
            if (a == null || b == null || a == b) { log.setText("Pick a killer and a target."); return; }
            engine.lock();
            AgentCombat.Result res;
            try { res = AgentCombat.assassinate(a, b); } finally { engine.unlock(); }
            log.setText(render(res));
            doRender.run();
        });
        restBtn.setOnAction(e -> {
            engine.lock();
            try { for (Agent a : roster.agents) { a.alive = true; a.hp = a.maxHp; } } finally { engine.unlock(); }
            log.setText("The cast is rested and whole again.");
            doRender.run();
        });

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #161616;");
        root.setLeft(rosterScroll);
        ScrollPane detailScroll = new ScrollPane(detail);
        detailScroll.setFitToWidth(true);
        detailScroll.setStyle("-fx-background: #1b1b1b; -fx-border-color: #333;");
        root.setCenter(detailScroll);
        root.setBottom(sandbox);

        doRender.run();

        // light refresh so renown/stance stay current as days tick
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), e -> doRender.run()));
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
        window.setOnHidden(e -> t.stop());

        window.setScene(new Scene(root, 880, 640));
        window.show();
    }

    // ---- roster row --------------------------------------------------------
    private static Region rosterRow(Agent a, boolean selected, Runnable onClick) {
        javafx.scene.Node face = Portraits.avatar(a.name, 42);

        Label name = new Label(a.name);
        name.setStyle("-fx-text-fill: " + (a.alive ? "#eee" : "#777") + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label sub = new Label(a.epithet + "  \u00b7  " + a.allegiance);
        sub.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        VBox text = new VBox(1, name, sub);

        Region spring = new Region();
        HBox.setHgrow(spring, Priority.ALWAYS);
        Label chip = stanceChip(a);

        HBox row = new HBox(10, face, text, spring, chip);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-radius: 8; -fx-background-color: " + (selected ? "#33312a" : "#1f1f1f") + ";"
                + "-fx-border-color: " + (selected ? "#a89a5c" : "#2c2c2c") + "; -fx-border-radius: 8; -fx-cursor: hand;");
        if (!a.alive) row.setOpacity(0.55);
        row.setOnMouseClicked(e -> onClick.run());
        return row;
    }

    // ---- detail card -------------------------------------------------------
    private static Region detailCard(Agent a) {
        if (a == null) { Label l = new Label("No agent selected."); l.setStyle("-fx-text-fill:#888;"); return new VBox(l); }

        javafx.scene.Node face = Portraits.avatar(a.name, 150);

        Label name = new Label(a.name);
        name.setStyle("-fx-text-fill:#fff; -fx-font-size:24px; -fx-font-weight:bold;");
        Label epi = new Label("\u201c" + a.epithet + "\u201d   \u00b7   " + a.allegiance);
        epi.setStyle("-fx-text-fill:#ffc107; -fx-font-style:italic; -fx-font-size:14px;");

        Label twist = new Label(a.twist.label + " \u2014 " + a.twist.flavor);
        twist.setWrapText(true); twist.setMaxWidth(360);
        twist.setStyle("-fx-text-fill:#bcb39a; -fx-font-size:13px;");

        Label lvl = new Label("Level " + a.level + "    \u00b7    power " + Math.round(a.power())
                + (a.alive ? "" : "    \u00b7    FALLEN"));
        lvl.setStyle("-fx-text-fill:#ddd; -fx-font-size:13px;");

        VBox stats = new VBox(5,
                bar("HP",      a.hp, a.maxHp, "#c0392b"),
                bar("Attack",  a.attack, 60, "#e08a3a"),
                bar("Defense", a.defense, 60, "#5a9ed6"),
                bar("Speed",   a.speed, 40, "#7ad17a"),
                bar("Cunning", a.cunning, 60, "#b07ad1"));
        stats.setMaxWidth(360);

        Label dispH = new Label("Disposition toward the throne" + (a.lockedDisposition ? "   (locked)" : ""));
        dispH.setStyle("-fx-text-fill:#888; -fx-font-size:11px; -fx-padding: 6 0 0 0;");
        VBox disp = new VBox(5,
                bar("Tension",   a.tension, 100, "#c0392b"),
                bar("Suspicion", a.suspicion, 100, "#d98a2b"),
                bar("Trust",     a.trust, 100, "#5a9e4a"),
                bar("Likeness",  a.likeness, 100, "#5a9ed6"));
        disp.setMaxWidth(360);

        HBox stanceRow = new HBox(8, new Label(""), stanceChip(a));
        stanceRow.setAlignment(Pos.CENTER);

        VBox card = new VBox(10, face, name, epi, twist, lvl, stanceChip(a), new sectionLabel("COMBAT"), stats, dispH, disp);
        card.setAlignment(Pos.TOP_CENTER);
        return card;
    }

    // ---- bits --------------------------------------------------------------
    private static Label stanceChip(Agent a) {
        Agent.Stance s = a.stance();
        String color = s == Agent.Stance.SHIELD ? "#5a9e4a" : s == Agent.Stance.DAGGER ? "#c0392b" : "#777";
        String txt   = s == Agent.Stance.SHIELD ? "SHIELD" : s == Agent.Stance.DAGGER ? "DAGGER" : "NEUTRAL";
        Label chip = new Label(txt);
        chip.setStyle("-fx-text-fill:#fff; -fx-font-size:11px; -fx-font-weight:bold; -fx-padding:3 9; "
                + "-fx-background-radius:10; -fx-background-color:" + color + ";");
        return chip;
    }

    private static Region bar(String label, int value, int scale, String color) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:#aaa; -fx-font-size:11px;");
        l.setMinWidth(64);
        Region track = new Region();
        track.setPrefHeight(10); track.setMaxWidth(Double.MAX_VALUE);
        track.setStyle("-fx-background-color:#ffffff14; -fx-background-radius:5;");
        Region fill = new Region();
        double frac = Math.max(0, Math.min(1, (double) value / Math.max(1, scale)));
        fill.setPrefHeight(10);
        fill.setStyle("-fx-background-color:" + color + "; -fx-background-radius:5;");
        HBox trackBox = new HBox(fill);
        trackBox.setStyle("-fx-background-color:#ffffff14; -fx-background-radius:5;");
        HBox.setHgrow(trackBox, Priority.ALWAYS);
        // emulate proportional fill: use a sized region in a max-width container
        fill.prefWidthProperty().bind(trackBox.widthProperty().multiply(frac));
        Label val = new Label(String.valueOf(value));
        val.setStyle("-fx-text-fill:#ccc; -fx-font-size:11px;");
        val.setMinWidth(34);
        HBox row = new HBox(8, l, trackBox, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String render(AgentCombat.Result r) {
        StringBuilder b = new StringBuilder();
        for (String line : r.log) b.append("\u2022 ").append(line).append("\n");
        if (r.winner != null) {
            b.append("\n");
            if (r.decisive && r.loser != null) b.append(">> ").append(r.winner.name).append(" stands. ").append(r.loser.name).append(" does not.");
            else b.append(">> ").append(r.winner.name).append(" comes out ahead.");
        }
        return b.toString();
    }

    private static Button action(String txt) {
        Button b = new Button(txt);
        b.setStyle("-fx-background-color:#292929; -fx-text-fill:#ddd; -fx-border-color:#444; -fx-border-radius:4;");
        return b;
    }
    private static void styleCombo(ComboBox<String> c) {
        c.setStyle("-fx-background-color:#222; -fx-text-fill:#ddd;");
        c.setPrefWidth(150);
    }

    // tiny labelled section header
    private static final class sectionLabel extends Label {
        sectionLabel(String s) { super(s); setStyle("-fx-text-fill:#ffc107; -fx-font-size:11px; -fx-padding:6 0 0 0;"); }
    }
}