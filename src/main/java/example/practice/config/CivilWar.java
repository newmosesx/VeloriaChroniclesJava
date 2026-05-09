package example.practice.config;

public enum CivilWar {
    CIVIL_WAR_MINIMUM_REBELS(100),
    CIVIL_WAR_REBEL_TO_SOLDIER_RATIO(0.75f);

    public final float value;

    CivilWar(float value){
        this.value = value;
    }
}

