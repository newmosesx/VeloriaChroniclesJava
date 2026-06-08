package example.practice.gui;

import example.practice.engine.SimulationEngine;
import example.practice.user.Player;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #191919;");

        // --- Emperor's status ---
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
        Label lblStr = new Label("Strength: " + player.strength);
        lblStr.setStyle("-fx-text-fill: #aaaaaa;");
        Label lblInt = new Label("Intellect: " + player.intellect);
        lblInt.setStyle("-fx-text-fill: #aaaaaa;");
        Label lblCha = new Label("Charisma: " + player.charisma);
        lblCha.setStyle("-fx-text-fill: #aaaaaa;");
        Label lblAge = new Label("Age: " + player.age);
        lblAge.setStyle("-fx-text-fill: #aaaaaa;");
        statsBox.getChildren().addAll(statusHeader, lblName, lblHealth, healthBar, lblStr, lblInt, lblCha, lblAge);

        // --- Governance & Technology (edicts as switches + research) ---
        Region governance = GovernancePanel.build(engine);
        VBox.setVgrow(governance, Priority.ALWAYS);

        Button closeButton = new Button("Return to Throne Room");
        closeButton.setOnAction(e -> window.close());

        layout.getChildren().addAll(statsBox, governance, closeButton);

        Scene scene = new Scene(layout, 480, 720);
        window.setScene(scene);
        window.showAndWait();
    }
}