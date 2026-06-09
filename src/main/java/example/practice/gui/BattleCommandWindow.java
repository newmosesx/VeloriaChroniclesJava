package example.practice.gui;

import example.practice.agents.AgentRoster;
import example.practice.engine.BattleDecision;
import example.practice.engine.BattleManager;
import example.practice.engine.FieldBattle;
import example.practice.engine.SimulationEngine;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * The Emperor takes the field. When a battle is fought under MANUAL command, this
 * window walks its decision windows: at each one it shows a (deliberately noisy)
 * read of the enemy and lets the player Hold, Reinforce, commit Both reserves, or
 * Retreat -- or hand the rest to Castius. On the last window it applies the result
 * to the world (BattleManager.applyManual); the main screen then plays the cinematic.
 */
public final class BattleCommandWindow {

    private BattleCommandWindow() {}

    public static void open(SimulationEngine engine, FieldBattle fb, Kingdom k,
                            AgentRoster roster, List<Human> pop) {
        Stage win = new Stage();
        win.setTitle("Field Command \u2014 " + fb.report.title);

        Label round   = new Label();
        round.setStyle("-fx-text-fill:#e8c14a; -fx-font-size:16px; -fx-font-weight:bold;");
        Label intel   = new Label();
        intel.setStyle("-fx-text-fill:#dddddd; -fx-font-size:13px;");
        intel.setWrapText(true);
        Label verdict = new Label();
        verdict.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");

        TextArea log = new TextArea();
        log.setEditable(false); log.setWrapText(true); log.setPrefRowCount(9);
        log.setStyle("-fx-control-inner-background:#0f0f0f; -fx-text-fill:#cdc3a8; -fx-font-family:'monospace';");

        Button hold     = btn("Hold the line");
        Button reOne    = btn("Reinforce (1 wave)");
        Button reBoth   = btn("Commit both reserves");
        Button retreat  = btn("Sound the retreat");
        Button auto     = btn("Let Castius finish it");

        HBox actions = new HBox(8, hold, reOne, reBoth, retreat);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(12, round, intel, verdict, log, actions, auto);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color:#191919;");

        Runnable[] refresh = new Runnable[1];

        Runnable finish = () -> {
            BattleManager.applyManual(engine, k, pop, roster, fb);   // locks engine, applies, queues cinematic
            win.close();
        };

        refresh[0] = () -> {
            if (fb.isOver()) { finish.run(); return; }
            FieldBattle.Assess a = fb.assess(FieldBattle.Side.IMPERIAL);
            round.setText("Decision window " + (fb.window() + 1) + " of " + fb.maxWindows());
            intel.setText(String.format(
                    "Your line: ~%d men in the field (%d reserve waves left).%n"
                            + "Scouts' read of the enemy: roughly %d strength (estimates drift).",
                    fb.impEngaged(), fb.impReinforcements(), Math.round(a.estEnemyStrength)));
            double r = a.ratio();
            if (r > 1.15)      { verdict.setText("They look STRONGER than us."); verdict.setStyle("-fx-text-fill:#e07a6a; -fx-font-size:13px; -fx-font-weight:bold;"); }
            else if (r < 0.85) { verdict.setText("They look WEAKER than us.");   verdict.setStyle("-fx-text-fill:#7ad17a; -fx-font-size:13px; -fx-font-weight:bold;"); }
            else               { verdict.setText("An even field.");             verdict.setStyle("-fx-text-fill:#e8c14a; -fx-font-size:13px; -fx-font-weight:bold;"); }
            reOne.setDisable(fb.impReinforcements() <= 0);
            reBoth.setDisable(fb.impReinforcements() <= 0);
            StringBuilder sb = new StringBuilder();
            for (String line : fb.report.log) sb.append(line).append('\n');
            log.setText(sb.toString());
            log.setScrollTop(Double.MAX_VALUE);
        };

        hold.setOnAction(e -> { fb.step(BattleDecision.HOLD); refresh[0].run(); });
        reOne.setOnAction(e -> { fb.step(BattleDecision.REINFORCE_ONE); refresh[0].run(); });
        reBoth.setOnAction(e -> { fb.step(BattleDecision.REINFORCE_BOTH); refresh[0].run(); });
        retreat.setOnAction(e -> { fb.step(BattleDecision.RETREAT); refresh[0].run(); });
        auto.setOnAction(e -> {
            while (!fb.isOver())
                fb.step(FieldBattle.AutoCommander.decide(fb.assess(FieldBattle.Side.IMPERIAL)));
            finish.run();
        });

        refresh[0].run();
        win.setScene(new Scene(box, 560, 420));
        win.show();
    }

    private static Button btn(String txt) {
        Button b = new Button(txt);
        b.setStyle("-fx-background-color:#292929; -fx-text-fill:#ddd; -fx-border-color:#444; -fx-border-radius:4; -fx-padding:6 12;");
        return b;
    }
}