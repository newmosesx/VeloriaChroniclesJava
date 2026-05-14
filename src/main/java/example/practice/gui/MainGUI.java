package example.practice.gui;

import example.practice.config.UIColors;
import example.practice.engine.SimulationEngine;
import example.practice.kingdoms.Kingdom;
import example.practice.story.CharacterData;
import example.practice.story.StoryData;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Timer;
import java.util.TimerTask;

import example.practice.logger.Logger;
import example.practice.logger.Logger.LogCategory;
import example.practice.engine.DebateManager;
import java.util.EnumMap;
import java.util.Map;

public class MainGUI extends Application {
    private SimulationEngine engine;
    private Label lblWorldPop, lblTime, lblCivilWarStatus, lblChapterTitle, lblStoryText, lblPerspective;
    private Accordion logAccordion;
    private Map<LogCategory, TextArea> categoryTextAreas;
    private int currentChapter = 0;
    private int currentParagraph = 0;
    private boolean[] chapterCompleted;

    private javafx.animation.FadeTransition currentTransition;


    private VBox kingdomContainer;
    private KingdomUI[] kingdomUIs;
    private int logAutoscrollState = 0;

    // Navigation Buttons
    private Button btnPrevChapter, btnNextChapter;

    private StackPane centerContainer;
    private VBox storyView, debateView;

    // Debate UI
    private VBox chatContainer;
    private ScrollPane chatScroll;
    private HBox choicesContainer;
    private Label lblTimer;
    private Timeline debateTimer;
    private int timeLeft = 6;

    private static class KingdomUI {
        TitledPane pane;
        Label lblPop, lblResources, lblUnrest, lblTroops;

        KingdomUI(String name) {
            VBox content = new VBox(5);
            lblPop = new Label(); lblPop.setStyle("-fx-text-fill: #dddddd;");
            lblResources = new Label(); lblResources.setStyle("-fx-text-fill: #dddddd;");
            lblUnrest = new Label();
            lblTroops = new Label(); lblTroops.setStyle("-fx-text-fill: #dddddd;");

            content.getChildren().addAll(lblPop, lblResources, lblUnrest, lblTroops);
            content.setStyle("-fx-background-color: #252525;");
            pane = new TitledPane(name, content);
            pane.setAnimated(false);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        engine = new SimulationEngine();
        new Thread(engine).start();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #191919;");

        // --- LEFT PANEL ---
        VBox left = new VBox(10);
        left.setPadding(new Insets(15));
        left.setPrefWidth(350);

        lblWorldPop = createLabel("World Pop: ", 14);
        lblTime = createLabel("Time: ", 14);
        lblCivilWarStatus = createLabel("Status: Initializing...", 14);

        kingdomContainer = new VBox(5);
        ScrollPane scrollKingdoms = new ScrollPane(kingdomContainer);
        scrollKingdoms.setFitToWidth(true);
        scrollKingdoms.setPrefHeight(300);
        scrollKingdoms.setStyle("-fx-background: #191919; -fx-border-color: #3c3c3c;");

        kingdomUIs = new KingdomUI[8];
        for (int i = 0; i < 8; i++) {
            kingdomUIs[i] = new KingdomUI("Kingdom " + i);
            kingdomUIs[i].pane.setVisible(false);
            kingdomUIs[i].pane.setManaged(false);
            kingdomContainer.getChildren().add(kingdomUIs[i].pane);
        }

        categoryTextAreas = new EnumMap<>(LogCategory.class);
        logAccordion = new Accordion();

        for (LogCategory cat : LogCategory.values()) {
            TextArea ta = new TextArea();
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setStyle("-fx-control-inner-background: #252525; -fx-text-fill: #aaaaaa;");

            TitledPane tp = new TitledPane(cat.name() + " REPORT", ta);
            tp.setStyle("-fx-base: #191919; -fx-text-fill: #dddddd;");
            tp.setAnimated(false); // Disable animation so it feels snappy like the C version

            categoryTextAreas.put(cat, ta);
            logAccordion.getPanes().add(tp);
        }

        // Expand the STORY tab by default so it isn't completely collapsed
        if (!logAccordion.getPanes().isEmpty()) {
            logAccordion.setExpandedPane(logAccordion.getPanes().get(0));
        }

        // Accordion to fill all remaining vertical space in the left panel!
        VBox.setVgrow(logAccordion, Priority.ALWAYS);

        // Finally, add it all to the 'left' VBox
        left.getChildren().addAll(
                lblWorldPop,
                lblTime,
                lblCivilWarStatus,
                new Separator(),
                new Label("KINGDOMS"),
                scrollKingdoms,
                new Separator(),
                new Label("EVENT LOGS"),
                logAccordion    // The Accordion sits right at the bottom of the Left Panel
        );

        root.setLeft(left);

        // --- CENTER PANEL ---
        centerContainer = new StackPane();
        centerContainer.setPadding(new Insets(20));

        // MODE 1: Standard Story View
        storyView = new VBox(25);
        storyView.setAlignment(Pos.CENTER);
        lblChapterTitle = createLabel("", 32);
        lblPerspective = createLabel("", 20);
        lblStoryText = createLabel("", 22);
        lblStoryText.setWrapText(true);
        lblStoryText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        lblStoryText.setMaxWidth(650);
        storyView.getChildren().addAll(lblChapterTitle, lblPerspective, lblStoryText);

        storyView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) advanceParagraph();
            else if (e.getButton() == MouseButton.SECONDARY) rewindParagraph();
        });

        // MODE 2: Chat / Debate View
        debateView = new VBox(15);
        debateView.setAlignment(Pos.CENTER);
        debateView.setVisible(false);

        chatContainer = new VBox(15);
        chatContainer.setPadding(new Insets(10));
        chatContainer.setStyle("-fx-background-color: #1e1e1e;");

        chatScroll = new ScrollPane(chatContainer);
        chatScroll.setFitToWidth(true);
        chatScroll.setPrefHeight(500);
        chatScroll.setStyle("-fx-background: #1e1e1e; -fx-border-color: #3c3c3c;");

        chatContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            // Every time the chat grows, scroll to the bottom automatically
            chatScroll.setVvalue(1.0);
        });

        lblTimer = new Label("6");
        lblTimer.setStyle("-fx-text-fill: #ff4d4d; -fx-font-size: 36px; -fx-font-weight: bold;");
        lblTimer.setVisible(false);

        choicesContainer = new HBox(15);
        choicesContainer.setAlignment(Pos.CENTER);

        Button btnDebateNext = createStyledButton("Next >>");
        btnDebateNext.setOnAction(e -> advanceParagraph());

        debateView.getChildren().addAll(new Label("THE COUNCIL HOUSE"), chatScroll, lblTimer, choicesContainer, btnDebateNext);

        centerContainer.getChildren().addAll(storyView, debateView);
        root.setCenter(centerContainer);

        // --- RIGHT PANEL ---
        VBox right = new VBox(15);
        right.setPadding(new Insets(15));
        right.setPrefWidth(250);
        right.setStyle("-fx-border-color: #3c3c3c; -fx-border-width: 0 0 0 1;");

        Button btnManager = createStyledButton("Manager");
        btnManager.setOnAction(e -> ManagerPopup.display(engine));

        // EXPLICIT CHAPTER NAVIGATION
        btnPrevChapter = createStyledButton("Previous Chapter");
        btnPrevChapter.setOnAction(e -> {
            if (currentChapter > 0) { currentChapter--; currentParagraph = 0; updateStoryView(); }
        });

        btnNextChapter = createStyledButton("Next Chapter");
        btnNextChapter.setOnAction(e -> {
            if (currentChapter < StoryData.CHAPTERS.size() - 1) { currentChapter++; currentParagraph = 0; updateStoryView(); }
        });

        // COMBO BOX CHARACTERS
        ComboBox<String> comboChars = new ComboBox<>();
        comboChars.setPromptText("Characters");
        comboChars.setPrefWidth(200);
        comboChars.setStyle("-fx-background-color: #292929; -fx-text-fill: #dddddd;");
        for(CharacterData cd : CharacterData.CHARACTERS) comboChars.getItems().add(cd.name);

        comboChars.setOnAction(e -> {
            int idx = comboChars.getSelectionModel().getSelectedIndex();
            if(idx >= 0) {
                CharacterWindow.displaySingle(idx);
                Platform.runLater(() -> comboChars.getSelectionModel().clearSelection());
            }
        });

        Button btnQuit = createStyledButton("Quit");
        btnQuit.setStyle("-fx-background-color: #702020; -fx-text-fill: white;");
        btnQuit.setOnAction(e -> System.exit(0));

        right.getChildren().addAll(new Label("ACTIONS"), btnManager, btnPrevChapter, btnNextChapter, comboChars, new Spacer(), btnQuit);
        root.setRight(right);

        chapterCompleted = new boolean[StoryData.CHAPTERS.size()];
        for(int i=0; i < chapterCompleted.length; i++) {
            if(StoryData.CHAPTERS.get(i).paragraphs.length <= 1) chapterCompleted[i] = true;
        }

        primaryStage.setScene(new Scene(root, 1400, 700));
        primaryStage.setTitle("Chronicles of Veloria");
        primaryStage.show();

        updateStoryView();
        startTimers();
    }

    private void advanceParagraph() {
        if (currentParagraph < StoryData.CHAPTERS.get(currentChapter).paragraphs.length - 1) {
            currentParagraph++;
            updateStoryView();
        }
    }

    private void rewindParagraph() {
        if (currentParagraph > 0) {
            currentParagraph--;
            updateStoryView();
        }
    }

    private VBox createChatBubble(String senderName, String message, boolean isPlayer) {
        VBox bubble = new VBox(5);
        bubble.setPadding(new Insets(10));

        if (isPlayer) {
            // Player Bubble: Dark Red/Purple, Aligned Right
            bubble.setStyle("-fx-background-color: #3b2a2f; -fx-border-color: #5e303b; -fx-border-radius: 8; -fx-background-radius: 8;");
            bubble.setAlignment(Pos.CENTER_RIGHT);
        } else {
            // NPC Bubble: Dark Grey, Aligned Left
            bubble.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #444; -fx-border-radius: 8; -fx-background-radius: 8;");
            bubble.setAlignment(Pos.CENTER_LEFT);
        }

        Label sender = new Label(senderName);
        sender.setStyle(isPlayer ? "-fx-text-fill: #ff7676; -fx-font-weight: bold; -fx-font-size: 14px;" : "-fx-text-fill: #ffc107; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label msg = new Label(message);
        msg.setStyle("-fx-text-fill: #dddddd; -fx-font-size: 16px;");
        msg.setWrapText(true);
        if (isPlayer) msg.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);

        bubble.getChildren().addAll(sender, msg);
        return bubble;
    }

    private void updateStoryView() {
        StoryData.Chapter ch = StoryData.CHAPTERS.get(currentChapter);

        // Check if we are in the Debate Chapter (Index 8)
        boolean isDebateMode = (currentChapter == 8);

        if (isDebateMode) {
            // Hide Story, Show Debate
            storyView.setVisible(false);
            storyView.setManaged(false); // Removes from layout calculations
            debateView.setVisible(true);
            debateView.setManaged(true);

            // Rebuild Chat up to current paragraph
            chatContainer.getChildren().clear();
            for (int i = 0; i <= currentParagraph; i++) {
                // Add NPC dialogue
                chatContainer.getChildren().add(createChatBubble(ch.perspectives[i], ch.paragraphs[i], false));

                // Check if THIS specific paragraph index was a choice point
                if (DebateManager.hasChoices(currentChapter, i) && DebateManager.chosenResponse != null) {
                    chatContainer.getChildren().add(createChatBubble(engine.getPlayer().userName, DebateManager.chosenResponse, true));
                }
            }
            chatScroll.setVvalue(1.0);

            // Check for Choices
            choicesContainer.getChildren().clear();
            if (DebateManager.hasChoices(currentChapter, currentParagraph) && DebateManager.chosenResponse == null) {
                setupDebateTimer();

                for (DebateManager.Choice choice : DebateManager.getChoices(currentChapter, currentParagraph, engine)) {
                    Button btnChoice = createStyledButton(choice.text);
                    btnChoice.setPrefWidth(250); // Slightly wider for long text
                    btnChoice.setOnAction(e -> {
                        debateTimer.stop();
                        lblTimer.setVisible(false);
                        choicesContainer.getChildren().clear();

                        choice.action.run(); // This sets DebateManager.chosenResponse

                        // Explicitly refresh the view to show the new bubble
                        updateStoryView();
                    });
                    choicesContainer.getChildren().add(btnChoice);
                }
            } else {
                if (debateTimer != null) debateTimer.stop();
                lblTimer.setVisible(false);
            }

        } else {
            // Hide Debate, Show Story
            debateView.setVisible(false);
            debateView.setManaged(false); // Removes from layout calculations
            storyView.setVisible(true);
            storyView.setManaged(true);

            // Stop any overlapping animation from rapid clicking
            if (currentTransition != null) {
                currentTransition.stop();
            }

            // IMMEDIATELY set the text so it can never get stuck blank
            lblChapterTitle.setText(ch.title);
            lblStoryText.setText(ch.paragraphs[currentParagraph]);
            lblPerspective.setText("— " + ch.perspectives[currentParagraph] + " —");

            // Play a fresh Fade In
            lblStoryText.setOpacity(0.0);
            currentTransition = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), lblStoryText);
            currentTransition.setFromValue(0.0);
            currentTransition.setToValue(1.0);
            currentTransition.play();
        }

        // Logic for Button States
        if (currentParagraph == ch.paragraphs.length - 1) {
            chapterCompleted[currentChapter] = true;
        }

        btnPrevChapter.setDisable(currentChapter == 0);
        btnNextChapter.setDisable(!chapterCompleted[currentChapter] || currentChapter == StoryData.CHAPTERS.size() - 1);

        engine.updateStoryPosition(currentChapter, currentParagraph);
    }

    // Helper for the 6-second timer
    private void setupDebateTimer() {
        if (debateTimer != null) debateTimer.stop();
        timeLeft = 6;
        lblTimer.setText(String.valueOf(timeLeft));
        lblTimer.setVisible(true);

        debateTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            lblTimer.setText(String.valueOf(timeLeft));
            if (timeLeft <= 0) {
                debateTimer.stop();
                lblTimer.setVisible(false);
                choicesContainer.getChildren().clear();
                DebateManager.handleTimeout(currentChapter, currentParagraph, engine);
                advanceParagraph();
            }
        }));
        debateTimer.setCycleCount(6);
        debateTimer.play();
    }

    private void startTimers() {
        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    engine.lock();
                    try {
                        lblWorldPop.setText("World Pop: " + engine.getTotalPopulation());
                        lblTime.setText(engine.getFormattedTime());
                        if (engine.sharedData.civilWarStatus != null) {
                            lblCivilWarStatus.setText(engine.sharedData.civilWarStatus);
                        }

                        Kingdom[] kingdoms = engine.getKingdoms();
                        for (int i = 0; i < kingdoms.length; i++) {
                            Kingdom k = kingdoms[i];
                            KingdomUI ui = kingdomUIs[i];

                            if (k.isActive) {
                                ui.pane.setVisible(true);
                                ui.pane.setManaged(true);
                                ui.pane.setText(i == 0 ? "The Great Empire" : "Successor Kingdom");

                                ui.lblPop.setText("Population: " + k.population);
                                ui.lblResources.setText(String.format("F: %d | W: %d | S: %d | M: %d | T: %d", k.food, k.wood, k.stone, k.metal, k.treasury));

                                ui.lblUnrest.setText("Unrest: " + k.unrestLevel + " | Morale: " + k.armyMorale);
                                if (k.unrestLevel > 2000) ui.lblUnrest.setTextFill(Color.web(UIColors.UNREST_RED));
                                else if (k.unrestLevel > 100) ui.lblUnrest.setTextFill(Color.web(UIColors.UNREST_YELLOW));
                                else ui.lblUnrest.setTextFill(Color.web(UIColors.TEXT_COLOR));

                                int totalTroops = k.jobCounts[6] + k.jobCounts[7] + k.jobCounts[8];
                                ui.lblTroops.setText("Troops: " + totalTroops + " | Rebels: " + k.jobCounts[9]);
                            } else {
                                ui.pane.setVisible(false);
                                ui.pane.setManaged(false);
                            }
                        }
                    } finally {
                        engine.unlock();
                    }

                    for (LogCategory cat : LogCategory.values()) {
                        if (Logger.hasUpdates(cat)) {
                            TextArea ta = categoryTextAreas.get(cat);
                            ta.appendText(Logger.getNewLogs(cat));
                            ta.setScrollTop(Double.MAX_VALUE);
                        }
                    }

                    if (logAutoscrollState == 1) {
                        logAutoscrollState = 2; // Will scroll on the next tick to ensure rendering
                    }
                });
            }
        }, 0, 1000); // Ticks every 1 second
    }

    private Label createLabel(String text, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #dddddd; -fx-font-size: " + size + "px;");
        return l;
    }

    private Button createStyledButton(String text) {
        Button b = new Button(text);
        b.setPrefWidth(200);
        b.setPrefHeight(40);
        b.setStyle("-fx-background-color: #292929; -fx-text-fill: #dddddd; -fx-border-color: #444444; -fx-border-radius: 4;");
        return b;
    }

    private static class Spacer extends Region { public Spacer() { VBox.setVgrow(this, Priority.ALWAYS); } }

    public static void main(String[] args) {
        launch(args);
    }
}