package example.practice.config;

public enum RGBConfig {
    RGB_BACKGROUND_DARK(25, 25, 25),
    RGB_BACKGROUND_LIGHT(41, 41, 41),
    RGB_TEXT_COLOR(221, 221,221),
    RGB_BORDER_COLOR(60, 60, 60),
    RGB_CURSOR_HOVER_COLOR(70, 70, 70),
    RGB_CURSOR_ACTIVE_COLOR(80, 80, 80),
    RGB_CHART_HIGHLIGHT(255, 0, 0),
    RGB_UNREST_RED(255, 77, 77),
    RGB_UNREST_YELLOW_LIGHT(255, 197, 7),
    RGB_UNREST_GRAY_LIGHT(221, 221,221);

    public final int value;
    public final int value2;
    public final int value3;

    RGBConfig(int value, int value2, int value3){
        this.value = value;
        this.value2 = value2;
        this.value3 = value3;
    }
}


