package example.practice.gui;

import example.practice.engine.SimulationEngine;
import example.practice.kingdoms.Kingdom;
import example.practice.user.Player;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import static example.practice.logger.Logger.LogCategory.STORY;

public class ManagerPopup {

    public static void display(SimulationEngine engine) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Emperor's Management Table");

        Player player = engine.getPlayer(); // Fetch live player data

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #191919;");

        // --- LEFT COLUMN: Governance ---
        Label title = new Label("Governance & Edicts");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");

        Button btnFestival = new Button("Host Grand Festival (-50 Unrest)");
        btnFestival.setStyle("-fx-background-color: #292929; -fx-text-fill: white;");
        btnFestival.setOnAction(e -> {
            Kingdom empire = engine.getKingdoms()[0];
            if (empire.treasury >= example.practice.config.Player.PLAYER_FESTIVAL_COST.value) {
                empire.treasury -= example.practice.config.Player.PLAYER_FESTIVAL_COST.value;
                empire.unrestLevel -= example.practice.config.Player.PLAYER_FESTIVAL_UNREST_REDUCTION.value;
                if (empire.unrestLevel < 0) empire.unrestLevel = 0;
                example.practice.logger.Logger.logEvent("A Grand Festival is held! Unrest falls.", STORY);
                window.close();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Not enough gold in the treasury!");
                alert.show();
            }
        });

        // --- RIGHT COLUMN: Emperor Stats ---
        VBox statsBox = new VBox(8);
        statsBox.setStyle("-fx-border-color: #3c3c3c; -fx-padding: 15; -fx-border-radius: 4;");

        Label lblName = new Label("Name: " + player.userName);
        lblName.setStyle("-fx-text-fill: #ffc107; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label lblHealth = new Label("Health: " + (int)player.statHealth + " / " + (int)player.statMaxHealth);
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

        statsBox.getChildren().addAll(new Label("Emperor's Status"), lblName, lblHealth, healthBar, lblStr, lblInt, lblCha, lblAge);

        Button closeButton = new Button("Return to Throne Room");
        closeButton.setOnAction(e -> window.close());

        layout.getChildren().addAll(title, btnFestival, statsBox, closeButton);

        Scene scene = new Scene(layout, 400, 500);
        window.setScene(scene);
        window.showAndWait();
    }
}