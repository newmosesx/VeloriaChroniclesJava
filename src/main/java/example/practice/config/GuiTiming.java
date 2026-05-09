package example.practice.config;

public enum GuiTiming {
    SIMULATIONTICKSECONDS(250),
    FADESPEED(0.05f);

    public final float fade;

    GuiTiming(float fade){
        this.fade = fade;
    }
}


