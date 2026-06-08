package example.practice.gui;

import example.practice.story.CharacterData;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CharacterWindow {

    // Shows only the selected character
    public static void displaySingle(int index) {
        if(index < 0 || index >= CharacterData.CHARACTERS.length) return;

        CharacterData cd = CharacterData.CHARACTERS[index];

        Stage window = new Stage();
        // locks the main window when this is open:
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(cd.name);

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle("-fx-background-color: #191919; -fx-border-color: #3c3c3c; -fx-border-width: 2px;");

        // Portrait (real image if the file exists, lettered circle otherwise)
        javafx.scene.Node portrait = Portraits.avatar(cd.name, 160);

        Label nameLbl = new Label(cd.name);
        nameLbl.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label titleLbl = new Label(cd.title);
        titleLbl.setStyle("-fx-text-fill: #ffc107; -fx-font-style: italic; -fx-font-size: 16px;");

        Label descLbl = new Label(cd.description);
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-text-fill: #dddddd; -fx-font-size: 14px; -fx-text-alignment: center;");
        descLbl.setMaxWidth(300);

        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #292929; -fx-text-fill: #dddddd; -fx-border-color: #444444; -fx-border-radius: 4;");
        closeButton.setOnAction(e -> window.close());

        layout.getChildren().addAll(portrait, nameLbl, titleLbl, new Separator(), descLbl, new Region(), closeButton);
        VBox.setVgrow(layout.getChildren().get(5), Priority.ALWAYS); // Push close button to bottom

        Scene scene = new Scene(layout, 400, 540);
        window.setScene(scene);
        window.showAndWait();
    }
}