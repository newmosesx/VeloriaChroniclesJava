package example.practice.config;

public enum Population {
    INITIALPOPULATION(13000),
    STARTINGBRONZE(80),
    POPULATIONGROWTHFLOOR(10000),
    DAYSINMONTH(30),
    ACTIVEHOURSPERDAY(12),
    FOODSURPLUSPERBIRTH(500),
    NUMKINGDOMS(8),
    DAILYNATURALDEATH(0.008f);
    
    public final float value;
    
    Population(float value){
        this.value = value;
    }
}


