package example.practice;

import example.practice.gui.MainGUI;

public class Main {
    public static void main(String[] args) {
        // This is a trick to bypass JavaFX module checks
        MainGUI.main(args);
    }
}