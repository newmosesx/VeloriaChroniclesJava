package example.practice.gui;

import example.practice.engine.SimulationEngine;
import example.practice.user.Player;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ManagerPopup {

    public static void display(SimulationEngine engine) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Emperor's Management Table");

        Player player = engine.getPlayer(); // live player data

        // --- Emperor's status (header strip across the top) ---
        VBox statsBox = new VBox(8);
        statsBox.setStyle("-fx-border-color: #3c3c3c; -fx-padding: 15; -fx-border-radius: 4;");

        Label statusHeader = new Label("Emperor's Status");
        statusHeader.setStyle("-fx-text-fill: #dddddd; -fx-font-weight: bold;");
        Label lblName = new Label("Name: " + player.userName);
        lblName.setStyle("-fx-text-fill: #ffc107; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label lblHealth = new Label("Health: " + (int) player.statHealth + " / " + (int) player.statMaxHealth);
        lblHealth.setStyle("-fx-text-fill: #dddddd;");
        ProgressBar healthBar = new ProgressBar(player.statHealth / player.statMaxHealth);
        healthBar.setPrefWidth(300);
        healthBar.setStyle("-fx-accent: #ff4d4d;");

        // stats laid out in a row to keep the header compact
        Label lblStr = new Label("Strength: " + player.strength);
        lblStr.setStyle("-fx-text-fill: #aaaaaa;");
        Label lblInt = new Label("Intellect: " + player.intellect);
        lblInt.setStyle("-fx-text-fill: #aaaaaa;");
        Label lblCha = new Label("Charisma: " + player.charisma);
        lblCha.setStyle("-fx-text-fill: #aaaaaa;");
        Label lblAge = new Label("Age: " + player.age);
        lblAge.setStyle("-fx-text-fill: #aaaaaa;");
        HBox statRow = new HBox(20, lblStr, lblInt, lblCha, lblAge);
        statsBox.getChildren().addAll(statusHeader, lblName, lblHealth, healthBar, statRow);

        // --- LEFT COLUMN: the estates (faction favourability + demands) ---
        Region estates = FactionPanel.build(engine);
        VBox leftCol = new VBox(estates);
        leftCol.setPrefWidth(360);
        leftCol.setMinWidth(320);
        VBox.setVgrow(estates, Priority.ALWAYS);

        // --- RIGHT COLUMN: governance / tech / military (already scrollable) ---
        Region governance = GovernancePanel.build(engine);
        VBox rightCol = new VBox(governance);
        VBox.setVgrow(governance, Priority.ALWAYS);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        HBox columns = new HBox(14, leftCol, rightCol);
        columns.setPadding(new Insets(0));
        VBox.setVgrow(columns, Priority.ALWAYS);

        Button closeButton = new Button("Return to Throne Room");
        closeButton.setOnAction(e -> window.close());

        VBox layout = new VBox(16, statsBox, columns, closeButton);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #191919;");

        Scene scene = new Scene(layout, 1040, 760);
        window.setScene(scene);
        window.showAndWait();
    }
}