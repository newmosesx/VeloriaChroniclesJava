package example.practice.kingdoms;

import example.practice.config.*;
import example.practice.humans.Human;

import java.util.List;

import static example.practice.logger.Logger.LogCategory.NATURAL;

public class Kingdom {
    public int id;
    public String name;
    public int population;
    public int unrestLevel;
    public boolean isActive;
    public boolean limitersDisabled = false;

    public int food, wood, stone, metal, weapons, gold, armyMorale;

    public int storySkirmishOverride;
    public float storySkirmishChanceModifier = 1.0f;
    public float storyProductionModifier = 1.0f;
    public int storyFoodDailyCap;

    public float divineTaxModifier = 1.0f;
    public float divineProductionModifier = 1.0f;
    public int divinePenaltyTimerDays = 0;
    public boolean canUseDivineIntervention = true;

    public int[] jobCounts = new int[10];

    public KingdomArchetype archetype;

    public Kingdom(int id, String name, boolean isActive) {
        this.id = id;
        this.name = name;
        this.isActive = isActive;

        this.food = (int) InitialKingdomResources.INITIAL_EMPIRE_FOOD.value;
        this.wood = (int) InitialKingdomResources.INITIAL_EMPIRE_WOOD.value;
        this.stone = (int) InitialKingdomResources.INITIAL_EMPIRE_STONE.value;
        this.metal = (int) InitialKingdomResources.INITIAL_EMPIRE_METAL.value;
        this.gold = (int) InitialKingdomResources.INITIAL_EMPIRE_TREASURY.value;
        this.armyMorale = (int) InitialKingdomResources.INITIAL_EMPIRE_MORALE.value;
    }

    public void inflictCasualties(List<Human> populationList, int kingdomId, int jobId, int count) {
        if (count <= 0) return;

        int casualtiesInflicted = 0;
        int attempts = 0;

        while (casualtiesInflicted < count && attempts < populationList.size()) {
            int randomIndex = (int) (Math.random() * populationList.size());
            Human target = populationList.get(randomIndex);

            if (target.isAlive && target.kingdomId == kingdomId && target.job == jobId) {
                target.isAlive = false;
                target.job = 0;
                casualtiesInflicted++;
            }
            attempts++;
        }
    }

    public void updateDailyMorale() {
        if (this.food > this.population * Morale.MORALE_FOOD_SURPLUS_MULTIPLIER.value && this.armyMorale < 100) {
            modifyMorale((int)Morale.MORALE_GAIN_FROM_SURPLUS.value);
        }
        if (this.unrestLevel > Rebellion.DISSENTTHRESHOLD.value && this.armyMorale > 20) {
            modifyMorale(-(int)Morale.MORALE_LOSS_FROM_UNREST.value);
        }
    }

    public void tickDivinePenalty() {
        if (this.divinePenaltyTimerDays > 0) {
            this.divinePenaltyTimerDays--;
            if (this.divinePenaltyTimerDays == 0) {
                example.practice.logger.Logger.logEvent("Divine favor returns to " + this.name + ". Penalties lifted.", NATURAL);
                this.divineTaxModifier = 1.0f;
                this.divineProductionModifier = 1.0f;
            }
        }
        this.canUseDivineIntervention = true; // Reset daily usage
    }

    // Guaranteed clamp to prevent negative or >100 morale bugs
    public void modifyMorale(int amount) {
        this.armyMorale += amount;

        // If limiters are disabled, morale can drop below 0 or exceed 100!
        if (!this.limitersDisabled) {
            if (this.armyMorale < 0) this.armyMorale = 0;
            if (this.armyMorale > 100) this.armyMorale = 100;
        }
    }

    public void triggerMassDesertion(List<Human> populationList, double percentage) {
        long totalSoldiers = populationList.stream()
                .filter(h -> h.isAlive && h.kingdomId == this.id && h.job >= 6 && h.job <= 8).count();

        int desertionCount = (int)(totalSoldiers * percentage);
        int deserted = 0;

        for (Human h : populationList) {
            if (deserted >= desertionCount) break;
            if (h.isAlive && h.kingdomId == this.id && h.job >= 6 && h.job <= 8) {
                h.job = 9; // Instantly become a Rebel
                deserted++;
            }
        }
        example.practice.logger.Logger.logEvent("MASS DESERTION! " + deserted + " imperial troops have joined the rebellion!", example.practice.logger.Logger.LogCategory.MILITARY);
    }

    public void inflictProportionalCasualties(List<Human> populationList, int kingdomId, int soldierLoss, int rebelLoss) {
        // 1. Count current troop types
        long swords = populationList.stream().filter(h -> h.isAlive && h.kingdomId == kingdomId && h.job == 6).count();
        long archers = populationList.stream().filter(h -> h.isAlive && h.kingdomId == kingdomId && h.job == 7).count();
        long cavalry = populationList.stream().filter(h -> h.isAlive && h.kingdomId == kingdomId && h.job == 8).count();
        long totalSoldiers = swords + archers + cavalry;

        if (totalSoldiers > 0) {
            int sLoss = (int) (soldierLoss * ((float)swords / totalSoldiers));
            int aLoss = (int) (soldierLoss * ((float)archers / totalSoldiers));
            int cLoss = (int) (soldierLoss * ((float)cavalry / totalSoldiers));

            inflictCasualties(populationList, kingdomId, 6, sLoss);
            inflictCasualties(populationList, kingdomId, 7, aLoss);
            inflictCasualties(populationList, kingdomId, 8, cLoss);
        }

        inflictCasualties(populationList, kingdomId, 9, rebelLoss);
    }

    public void collectTaxes(List<Human> populationList) {
        if (!this.isActive || this.population == 0) return;
        int totalTaxCollected = 0;

        for (Human h : populationList) {
            if (h.isAlive && h.kingdomId == this.id) {
                if (h.bronze >= ProductionCost.TAXRATEPERPERSON.value) {
                    h.bronze -= ProductionCost.TAXRATEPERPERSON.value;
                    totalTaxCollected += ProductionCost.TAXRATEPERPERSON.value;
                }
            }
        }
        if (this.divinePenaltyTimerDays > 0) {
            totalTaxCollected = (int) (totalTaxCollected * this.divineTaxModifier);
        }
        this.gold += totalTaxCollected;
        this.unrestLevel += (int) Rebellion.UNRESTGAINFROMTAXES.value;
    }

    public void updateCensus(List<Human> populationList) {
        // Reset counts
        for (int i = 0; i < jobCounts.length; i++) jobCounts[i] = 0;

        // Count jobs for living humans in this kingdom
        for (Human h : populationList) {
            if (h.isAlive && h.kingdomId == this.id) {
                if (h.job >= 0 && h.job < jobCounts.length) {
                    jobCounts[h.job]++;
                }
            }
        }
    }

    public void setSkirmishControl(int override, float chanceModifier) {
        this.storySkirmishOverride = override;
        this.storySkirmishChanceModifier = chanceModifier;
    }

    public void setProductionControl(float modifier, int foodCap) {
        this.storyProductionModifier = modifier;
        this.storyFoodDailyCap = foodCap;
    }

    public void recruitSoldiers(List<Human> populationList) {
        if (!this.isActive || this.population == 0) return;
        if (this.unrestLevel < Rebellion.DISSENTTHRESHOLD.value / 2) return;

        int recruitsWanted = 5 + (this.unrestLevel / 20);
        int recruitedCount = 0;

        for (Human h : populationList) {
            if (recruitedCount >= recruitsWanted) break;

            if (h.isAlive && h.kingdomId == this.id && h.job > 0 && h.job <= CivilJobs.BLACKSMITH.value) {
                int unitChoice = (int) (Math.random() * 3);

                if (unitChoice == 0 && this.metal >= RecruitmentCost.COSTSWORDSMAN.value) {
                    this.metal -= RecruitmentCost.COSTSWORDSMAN.value;
                    h.job = Military.SWORDSMAN.value;
                    recruitedCount++;
                } else if (unitChoice == 1 && this.wood >= RecruitmentCost.COSTARCHER.value) {
                    this.wood -= RecruitmentCost.COSTARCHER.value;
                    h.job = Military.ARCHER.value;
                    recruitedCount++;
                } else if (unitChoice == 2 && this.metal >= RecruitmentCost.COSTCAVALRY.value && this.food >= RecruitmentCost.COSTCAVALRYFOOD.value) {
                    this.metal -= RecruitmentCost.COSTCAVALRY.value;
                    this.food -= RecruitmentCost.COSTCAVALRYFOOD.value;
                    h.job = Military.CAVALRY.value;
                    recruitedCount++;
                }
            }
        }
    }
}