package example.practice.engine;

import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.config.Population;

import java.util.List;

public class PopulationManager {
    private static float deathFractionAccumulator = 0.0f;
    private static float birthFractionAccumulator = 0.0f;

    public static void processPopulationChanges(Kingdom kingdom, List<Human> populationList) {
        if (!kingdom.isActive || kingdom.population <= 0) return;

        // 1. Natural Deaths Calculation
        float dailyNaturalDeaths = (kingdom.population * Population.DAILYNATURALDEATH.value) / Population.DAYSINMONTH.value;
        deathFractionAccumulator += dailyNaturalDeaths;
        int newDeaths = (int) deathFractionAccumulator;

        // 2. Births based on Food Surplus
        int foodSurplus = Math.max(0, kingdom.food - kingdom.population);
        float dailyBirths = foodSurplus / Population.FOODSURPLUSPERBIRTH.value;
        birthFractionAccumulator += dailyBirths;
        int newBirths = (int) birthFractionAccumulator;

        // 3. THE GROWTH FLOOR
        if (kingdom.population < (int)Population.POPULATIONGROWTHFLOOR.value) {
            if (newDeaths > newBirths) {
                newDeaths = newBirths;
            }
        }

        if (newDeaths > 0) {
            deathFractionAccumulator -= newDeaths;
            kingdom.inflictCasualties(populationList, kingdom.id, (int)(Math.random()*5)+1, newDeaths);
        }

        if (newBirths > 0) {
            birthFractionAccumulator -= newBirths;
            for (int i = 0; i < newBirths; i++) {
                Human baby = new Human(kingdom.id);
                baby.job = 0; // NEWBORNS ARE NOW UNEMPLOYED (Matches C)
                populationList.add(baby);
            }
        }
    }

    public static void compactDeadHumans(List<Human> data) {
        int writeIndex = 0;
        int originalSize = data.size();

        for (int readIndex = 0; readIndex < originalSize; readIndex++) {
            Human h = data.get(readIndex);
            if (h.isAlive) {
                if (writeIndex != readIndex) {
                    data.set(writeIndex, h);
                }
                writeIndex++;
            }
        }
        if (writeIndex < originalSize) {
            data.subList(writeIndex, originalSize).clear();
        }
    }
}