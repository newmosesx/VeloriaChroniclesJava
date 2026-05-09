package example.practice.kingdoms;

import example.practice.config.*;
import example.practice.humans.Generational;
import example.practice.humans.Human;
import example.practice.shared.ShareData;

import java.util.Random;

public class Kingdom {
    public int id;
    public String name;
    public int population;
    public int unrestLevel;
    public int isActive;

    public int food;
    public int wood;
    public int stone;
    public int metal;
    public int weapons;
    public int treasury;

    public int armyMorale;

    public int storySkirmishOverride;
    public float storySkirmishChanceModifier;
    public float storyProductionModifier;
    public int storyFoodDailyCap;
    public float storyConsumptionModifier;

    public float divineTaxModifier;
    public float divineProductionModifier;
    public int divinePenaltyTimerDays;
    public boolean canUseDivineIntervention;

    public int[] jobCounts = new int[10];

    public void inflictCasualties(Human[] population, int kingdomId, int jobId, int count){
        if (count <= 0) return;

        int casualties_inflicted = 0;
        int attempts = 0; // prevent infinite loops

        while (casualties_inflicted < count && attempts < population.length){
            int randomIndex = (int) (Math.random() * population.length);
            if (population[randomIndex].alive == 1 && population[randomIndex].kingdom_id == kingdomId && population[randomIndex].job == jobId){
                population[randomIndex].alive = 0;
                casualties_inflicted++;
            }
            attempts++;
        }
    }

    public void updateAllKingdomDetailsForGUI(Kingdom[] kingdoms, Human[] population, ShareData Share){
        int startingZero = Startings.STARTING_ZERO.value;
        int startingOne = Startings.STARTING_ONE.value;
        int rebelJob = Military.REBEL.value;
        int numKingdoms = Share.numkingdoms;

        int[][] tempJobCounts = new int[numKingdoms][10];
        for (int i = startingZero; i < population.length; i++){
            if (population[i].alive == startingOne){
                int kingdomId = population[i].kingdom_id;
                int jobId = population[i].job;
                // possible bug here, kingdom 8 could possibly not be updated
                // cause: kingdom id must be smaller than numKingdoms (Which is 8)
                if (kingdomId >= startingZero && kingdomId < numKingdoms && jobId >= startingZero && jobId <= rebelJob){
                    tempJobCounts[kingdomId][jobId]++;
                }
            }
        }
    }

    public void life(Generational generations){
        generations.humanPopulation = (int)(Population.INITIALPOPULATION.value);
    }

    public void initializeWorldPolitics(Kingdom[] kingdoms){
        // need to add the log message here log event
        kingdoms[0] = new Kingdom(
                0, "The Great Empire",
                0, 1,
                InitialKingdomResources.INITIAL_EMPIRE_FOOD.value,
                InitialKingdomResources.INITIAL_EMPIRE_WOOD.value, InitialKingdomResources.INITIAL_EMPIRE_STONE.value,
                InitialKingdomResources.INITIAL_EMPIRE_METAL.value, InitialKingdomResources.INITIAL_EMPIRE_MORALE.value,
                InitialKingdomResources.INITIAL_EMPIRE_TREASURY.value, 0, 1.0f,
                1.0f, 0,
                1.0f, 1.0f,
                1.0f, 0,
                true);

        kingdoms[1] = new Kingdom(
                0, "Esther",
                0, 0,
                InitialKingdomResources.INITIAL_EMPIRE_FOOD.value,
                InitialKingdomResources.INITIAL_EMPIRE_WOOD.value, InitialKingdomResources.INITIAL_EMPIRE_STONE.value,
                InitialKingdomResources.INITIAL_EMPIRE_METAL.value, InitialKingdomResources.INITIAL_EMPIRE_MORALE.value,
                InitialKingdomResources.INITIAL_EMPIRE_TREASURY.value, 0, 1.0f,
                1.0f, 0,
                1.0f, 1.0f,
                1.0f, 0,
                true);

        kingdoms[2] = new Kingdom(
                0, "Magento",
                0, 0,
                InitialKingdomResources.INITIAL_EMPIRE_FOOD.value,
                InitialKingdomResources.INITIAL_EMPIRE_WOOD.value, InitialKingdomResources.INITIAL_EMPIRE_STONE.value,
                InitialKingdomResources.INITIAL_EMPIRE_METAL.value, InitialKingdomResources.INITIAL_EMPIRE_MORALE.value,
                InitialKingdomResources.INITIAL_EMPIRE_TREASURY.value, 0, 1.0f,
                1.0f, 0,
                1.0f, 1.0f,
                1.0f, 0,
                true);

        kingdoms[3] = new Kingdom(
                0, "Nightingale",
                0, 0,
                InitialKingdomResources.INITIAL_EMPIRE_FOOD.value,
                InitialKingdomResources.INITIAL_EMPIRE_WOOD.value, InitialKingdomResources.INITIAL_EMPIRE_STONE.value,
                InitialKingdomResources.INITIAL_EMPIRE_METAL.value, InitialKingdomResources.INITIAL_EMPIRE_MORALE.value,
                InitialKingdomResources.INITIAL_EMPIRE_TREASURY.value, 0, 1.0f,
                1.0f, 0,
                1.0f, 1.0f,
                1.0f, 0,
                true);

        kingdoms[4] = new Kingdom(
                0, "Lionheart",
                0, 0,
                InitialKingdomResources.INITIAL_EMPIRE_FOOD.value,
                InitialKingdomResources.INITIAL_EMPIRE_WOOD.value, InitialKingdomResources.INITIAL_EMPIRE_STONE.value,
                InitialKingdomResources.INITIAL_EMPIRE_METAL.value, InitialKingdomResources.INITIAL_EMPIRE_MORALE.value,
                InitialKingdomResources.INITIAL_EMPIRE_TREASURY.value, 0, 1.0f,
                1.0f, 0,
                1.0f, 1.0f,
                1.0f, 0,
                true);

        kingdoms[5] = new Kingdom(
                0, "Rethmar",
                0, 0,
                InitialKingdomResources.INITIAL_EMPIRE_FOOD.value,
                InitialKingdomResources.INITIAL_EMPIRE_WOOD.value, InitialKingdomResources.INITIAL_EMPIRE_STONE.value,
                InitialKingdomResources.INITIAL_EMPIRE_METAL.value, InitialKingdomResources.INITIAL_EMPIRE_MORALE.value,
                InitialKingdomResources.INITIAL_EMPIRE_TREASURY.value, 0, 1.0f,
                1.0f, 0,
                1.0f, 1.0f,
                1.0f, 0,
                true);

        kingdoms[6] = new Kingdom(
                0, "Tehran",
                0, 0,
                InitialKingdomResources.INITIAL_EMPIRE_FOOD.value,
                InitialKingdomResources.INITIAL_EMPIRE_WOOD.value, InitialKingdomResources.INITIAL_EMPIRE_STONE.value,
                InitialKingdomResources.INITIAL_EMPIRE_METAL.value, InitialKingdomResources.INITIAL_EMPIRE_MORALE.value,
                InitialKingdomResources.INITIAL_EMPIRE_TREASURY.value, 0, 1.0f,
                1.0f, 0,
                1.0f, 1.0f,
                1.0f, 0,
                true);

        kingdoms[7] = new Kingdom(
                0, "Asfahan",
                0, 0,
                InitialKingdomResources.INITIAL_EMPIRE_FOOD.value,
                InitialKingdomResources.INITIAL_EMPIRE_WOOD.value, InitialKingdomResources.INITIAL_EMPIRE_STONE.value,
                InitialKingdomResources.INITIAL_EMPIRE_METAL.value, InitialKingdomResources.INITIAL_EMPIRE_MORALE.value,
                InitialKingdomResources.INITIAL_EMPIRE_TREASURY.value, 0, 1.0f,
                1.0f, 0,
                1.0f, 1.0f,
                1.0f, 0,
                true);

    }

    public void initializePopulation(Human[] humans){
        int capacity = humans.length;

        for (int i = 0; i < capacity; i++){
            Human person = new Human();
            person.name = "Adam";
            person.health = 200;
            person.hunger = 100;
            person.speed = (int)(Math.random() * 31)+1;
            person.damage = (int)(Math.random() * 31)+1;
            person.defense = (int)(Math.random() * 31)+1;
            person.smart = (int)(Math.random() * 31)+1;
            person.job = 0; // Unemployed
            person.isGeneral = 0;
            person.bronze = (int)(Population.STARTINGBRONZE.value);
            person.alive = 1;
            person.kingdom_id = 0;
            humans[i] = person;
        }
    }

    public void initialJobAssignment(Human[] humans){
        final int archerChance = 5;
        final int cavalryChance = 5;
        final int swordsmanChance = 5;
        final int blacksmithChance = 10;
        final int minerChance = 10;
        final int lumberjackChance = 15;

        int generalCount = 0;

        for (int i = 0; i < humans.length; i++){
            if (humans[i].alive ==1) {
                int jobRoll = (int) (Math.random() * 101);

                if (jobRoll < archerChance) {
                    humans[i].job = Military.ARCHER.value;
                } else if (jobRoll < (archerChance + cavalryChance)) {
                    humans[i].job = Military.CAVALRY.value;
                } else if (jobRoll < (archerChance + cavalryChance + swordsmanChance)) {
                    humans[i].job = Military.SWORDSMAN.value;
                    if (generalCount < Combat.INITIAL_GENERAL_LIMIT.value && ((Math.random() * 101) < Combat.GENERAL_SPAWN_CHANCE_PERCENT.value)) {
                        humans[i].isGeneral = 1;
                        generalCount++;
                        // generals don't seem to have jobs, this might be a logical flaw
                    }
                } else if (jobRoll < blacksmithChance) {
                    humans[i].job = CivilJobs.BLACKSMITH.value;
                } else if (jobRoll < minerChance) {
                    humans[i].job = CivilJobs.MINER.value;
                } else if (jobRoll < lumberjackChance) {
                    humans[i].job = CivilJobs.LUMBERJACK.value;
                } else {
                    humans[i].job = CivilJobs.FARMER.value;
                }
            }
        }
        System.out.printf("Initial job assignments complete. The empire's army, workforce, and %d generals are ready!\n", generalCount);

    }

    public void calculateStablePopulationChanges(Kingdom kingdom, int currentPopulation, int newBirths, int newDeaths){
        if (currentPopulation <= 0){
            newBirths = 0;
            newDeaths = 0;
        }

        float dailyNaturalDeaths = (currentPopulation * Population.DAILYNATURALDEATH.value) / Population.DAYSINMONTH.value;
        float hourlyNaturalDeaths = dailyNaturalDeaths / Population.ACTIVEHOURSPERDAY.value;
        float deathFraction = 0.0f;

        deathFraction += hourlyNaturalDeaths;
        newDeaths = (int) deathFraction;

        if(newDeaths >0){
            deathFraction -= newDeaths;
        }

        int foodSurplus = food - currentPopulation;
        if (foodSurplus < 0) foodSurplus = 0;
        float dailyBirthFromSurplus = foodSurplus / Population.FOODSURPLUSPERBIRTH.value;
        float hourlyBirth = dailyBirthFromSurplus / Population.ACTIVEHOURSPERDAY.value;
        float birthFraction = 0.0f;
        birthFraction += hourlyBirth;
        newBirths = (int)birthFraction;
        if (newBirths > 0){
            birthFraction -= newBirths;
        }

        if (currentPopulation < Population.POPULATIONGROWTHFLOOR.value && newDeaths > newBirths){
            newDeaths = newBirths;
        }

    }

    public void aliveStatus(int deathsToInflict,Human[] humans){
        if (deathsToInflict <=0)return;

        int casualties = 0;
        int startIndex = (int)(Math.random() * humans.length);

        for (int i = 0; i< humans.length; i++){
            if (casualties >= deathsToInflict) break;

            int currentIndex = (startIndex+i) % humans.length;
            if (humans[currentIndex].alive == 1){
                humans[currentIndex].alive = 0;
                humans[currentIndex].job = 0;
                casualties++;
            }
        }
    }

    public void persona(int newBirths, Generational generation , Human[] humans, int empireHasFallen){
        if (newBirths <= 0) return;

        int startIndex = humans.length;
        generation.humanPopulation = humans.length + newBirths;

        for (int i = startIndex; i < generation.humanPopulation; i++){
            Human people = new Human();

            people.name = "Adam";
            people.health = 200;
            people.speed = (int)(Math.random()*11);;
            people.damage = (int)(Math.random()*11);;
            people.defense = (int)(Math.random()*11);
            people.smart = (int)(Math.random()*11);
            people.job = 0;
            people.isGeneral = 0;
            people.bronze = (int)(Population.STARTINGBRONZE.value);
            people.alive = 1;
            people.quirks[0] = (int)(Math.random()*3);
            people.quirks[1] = (int)(Math.random()*3);
            people.quirks[2] = (int)(Math.random()*3);

            if (empireHasFallen==1){
                people.kingdom_id = (int)((Math.random()* 8)+1);
            } else {
                people.kingdom_id = 0;
            }
            humans[i] = people;

        }

    }

    public void surpriseAttack(int soldierHealth, int rebelDamage){
        soldierHealth -= (int)(rebelDamage * (Combat.ELEMENT_OF_SURPRISE.value));
    }

    public void tankMode(int soldierHealth, int residualSoldierHealth, int soldierDefense){
        if(soldierHealth < residualSoldierHealth * 0.5){
            int defenseToHealth = (int)(soldierDefense*0.5f);
            soldierDefense -= defenseToHealth;
            soldierHealth += defenseToHealth;
        }
    }

    public void soldierAttack(int soldierDamage, int rebelDefense, int rebelHealth)
    {
        if (rebelDefense < soldierDamage)
        {
            int damageSpillover = soldierDamage - rebelDefense;
        rebelDefense = 0; // Defense is broken
        rebelHealth -= damageSpillover;
        } else
        {
        rebelDefense -= soldierDamage;
        }
    }

    public void regroupSoldiers(int soldierCount, int residualSoldierDamage,
                                 int residualSoldierHealth, int residualSoldierDefense,
                                 int soldierHealth, int soldierDamage, int soldierDefense)
    {
        if (soldierCount <= 0) return; // Prevent division by zero
        float soldierDmgApprox = (float)residualSoldierDamage / soldierCount;
        float soldierHltApprox = (float)residualSoldierHealth / soldierCount;
        float soldierDefApprox = (float)residualSoldierDefense / soldierCount;

        // Check for division by zero
        float soldiersCountApprox = (soldierHltApprox > 0) ? (float)soldierHealth / soldierHltApprox : 0;

        soldiersCountApprox = (int)(Math.floor(soldiersCountApprox));

        soldierDamage = (int)(soldiersCountApprox * soldierDmgApprox);
        soldierHealth = (int)(soldiersCountApprox * soldierHltApprox);
        soldierDefense = (int)(soldiersCountApprox * soldierDefApprox);
        //log_event("The army lost %d soldiers.", (int)(*soldierCount - soldiersCountApprox));
        soldierCount = (int)(soldiersCountApprox);
    }

    public void regroupRebels(int rebelCount, int residualRebelDamage,
                        int residualRebelHealth, int residualRebelDefense,
                        int rebelDeath, int rebelDamage, int rebelDefense)
    {
        if (rebelCount <= 0) return; // Prevent division by zero
        float rebelDmgApprox = (float)residualRebelDamage / rebelCount;
        float rebelDltApprox = (float)residualRebelHealth / rebelCount;
        float rebelDefApprox = (float)residualRebelDefense / rebelCount;

        // Check for division by zero
        float rebelsCountApprox = (rebelDltApprox > 0) ? (float)rebelDeath / rebelDltApprox : 0;

        rebelsCountApprox = (int)(Math.floor(rebelsCountApprox));
        rebelDamage = (int)(rebelsCountApprox * rebelDmgApprox);
        rebelDeath = (int)(rebelsCountApprox * rebelDltApprox);
        rebelDefense = (int)(rebelsCountApprox * rebelDefApprox);
        //log_event("The rebels lost %d fighters.", (int)(rebelCount - rebelsCountApprox));
        rebelCount = (int)(rebelsCountApprox);
    }

    public void rebelAttack(int rebelDamage, int soldierDefense, int soldierDealth)
    {
        if ((soldierDefense * Combat.ORGANIZED_COMMAND.value) > rebelDamage)
        {
        soldierDefense -= rebelDamage;
        } else
        {
            int damageSpillover = rebelDamage - (int)(soldierDefense * Combat.ORGANIZED_COMMAND.value);
            soldierDefense = 0; // Defense is broken
            soldierDealth -= damageSpillover;
        }
    }

    public int declareResult(int rebelCount, int soldierCount, int residualRebelCount, int residualSoldierCount)
    {
        if (soldierCount <= 0 && rebelCount <= 0) {
            // log_event("The battle ended with no victor.. Just blood..");
            return 2; // End the battle
        }

        if (soldierCount >= rebelCount) {
            if (rebelCount >= residualSoldierCount * 0.2 && soldierCount < residualSoldierCount * 0.3) {
                return 3; // Continue the battle
            } else {
                // log_event("Victory of the Empire!: Rebel Retreat");
                return 2; // End the battle
            }
        } else { // rebelCount >= soldierCount
            if (soldierCount >= residualRebelCount * 0.2 && rebelCount < residualRebelCount * 0.3) {
                return 3; // Continue the battle
            } else {
                // log_event("Victory of the Rebels!: Soldiers Retreat");
                return 2; // End the battle
            }
        }
    }

    public void triggerHourlySkirmish(Kingdom reign, Human[] humans){
        if (reign.isActive==0) return;

        int modifiedChance = (int)(Battle.HOURLY_SKIRMISH_BASE_CHANCE_PERCENT.value + ((float) (reign.unrestLevel + 1) /10));

        if (reign.storySkirmishOverride == -1) return;
        if (reign.storySkirmishOverride == 1) {
            //Story forces skirmish, - skip chance roll
        } else {
            modifiedChance = (int) (Battle.HOURLY_SKIRMISH_BASE_CHANCE_PERCENT.value * reign.storySkirmishChanceModifier);
        }

        reign.storySkirmishOverride = 0;

        if ((Math.random() * 101) < Battle.HOURLY_SKIRMISH_BASE_CHANCE_PERCENT.value){
            int soldierHealth = 0; int rebelHealth = 0;
            int soldierDamage = 0; int rebelDamage = 0;
            int soldierDefense = 0; int rebelDefense = 0;
            int soldierCount = 0; int rebelCount = 0;
            int generalCount = 0; int leaderCount = 0;

            int swordsmanCount = 0;
            int archerCount = 0;
            int cavalryCount = 0;


            // The compiler didn't allow the use of enums
            // as the values would lose their constant status.
            final int swordId = 6; // hardcoded - swordsman role
            final int bowId = 7; // hardcoded - archer role
            final int horseId = 8; // hardcoded - cavalry role
            final int rebelId = 9; // hardcoded - rebel role

            for (int i = 0; i < humans.length; i++){
                if (humans[i].alive == 1 && humans[i].kingdom_id == reign.id){
                    switch (humans[i].job){
                        case swordId:
                            soldierHealth += humans[i].health;
                            soldierDamage += humans[i].damage;
                            soldierDefense += humans[i].defense;
                            if (humans[i].isGeneral == 1) {generalCount++;}
                            break;

                        case bowId:
                            soldierHealth += humans[i].health;
                            soldierDamage += humans[i].damage;
                            soldierDefense += humans[i].defense;
                            if (humans[i].isGeneral == 1) {generalCount++;}
                            break;

                        case horseId:
                            soldierHealth += humans[i].health;
                            soldierDamage += humans[i].damage;
                            soldierDefense += humans[i].defense;
                            if (humans[i].isGeneral == 1) {generalCount++;}
                            break;

                        case rebelId:
                            rebelHealth += humans[i].health;
                            rebelDamage += humans[i].damage;
                            rebelDefense += humans[i].defense;
                            if (humans[i].isGeneral == 1) {leaderCount++;}
                            break;
                    }  // REMINDER: Body equipment should enhance defense. E.g. iron boots gives +2 defense
                }
            }

            soldierCount = swordsmanCount + archerCount + cavalryCount;
            if (soldierCount == 0 || rebelCount == 0)return;

            int rebelFighters = (int) (rebelCount*(Battle.REBELS_IN_SKIRMISH.value));
            int soldierFighters = (int) (soldierCount*(Battle.SOLDIERS_IN_SKIRMISH.value));

            rebelDamage = (int) (rebelDamage*(Battle.REBELS_IN_SKIRMISH.value));
            soldierDamage = (int) (soldierDamage*(Battle.SOLDIERS_IN_SKIRMISH.value));
            rebelHealth = (int) (rebelHealth*(Battle.REBELS_IN_SKIRMISH.value));
            soldierHealth = (int) (soldierHealth*(Battle.SOLDIERS_IN_SKIRMISH.value));
            rebelDefense = (int) (rebelDefense*(Battle.REBELS_IN_SKIRMISH.value));
            soldierDefense = (int) (soldierDefense*(Battle.SOLDIERS_IN_SKIRMISH.value));

            int residualSoldierHealth = soldierHealth; int residualRebelHealth = rebelHealth;
            int residualSoldierDamage = soldierDamage; int residualRebelDamage = rebelDamage;
            int residualSoldierDefense = soldierDefense; int residualRebelDefense = rebelDefense;
            int residualSoldierCount = soldierFighters; int residualRebelCount = rebelFighters;
            int battleState = 3;

            // we need to add the log event function here

            if (leaderCount > 0){
                rebelDamage *= (int) (1.0f+(Combat.REBEL_LEADER_COMBAT_BONUS.value - 1.0f)* leaderCount);
            }

            if (generalCount > 0){
                soldierDamage *= (int) (1.0f+(Combat.GENERAL_COMBAT_BONUS.value - 1.0f)* generalCount);
            }

            while (battleState == 3){
                // Rebels always attack first. They have the element of surprise. Dealing more damage at first.
                surpriseAttack(soldierHealth, rebelDamage);
                tankMode(soldierHealth, residualSoldierHealth, soldierDefense);
                regroupSoldiers(soldierFighters, residualSoldierDamage,
                        residualSoldierHealth, residualSoldierDefense,
                            soldierHealth, soldierDamage, soldierDefense);
                soldierAttack(soldierDamage, rebelDefense, rebelHealth);
                regroupRebels(rebelFighters, residualRebelDamage,
                        residualRebelHealth, residualRebelDefense,
                            rebelHealth, rebelDamage, rebelDefense);
                rebelAttack(rebelDamage, rebelDefense, rebelHealth);
                regroupSoldiers(soldierFighters, residualSoldierDamage,
                        residualSoldierHealth, residualSoldierDefense,
                            soldierHealth, soldierDamage, soldierDefense);
                battleState = declareResult(rebelFighters, soldierFighters, residualRebelCount, residualSoldierCount);
            }

            int totalSoldierCasualties = residualSoldierCount - soldierFighters;
            int totalRebelCasualties = residualRebelCount - rebelFighters;

            if (totalSoldierCasualties > 0 && soldierCount > 0){
                // Distribute casualties proportionally
                int swordsmanCasualties = Math.round(totalSoldierCasualties * ((float)swordsmanCount / soldierCount));
                int archerCasualties = Math.round(totalSoldierCasualties * ((float)archerCount / soldierCount));
                int cavalryCasualties = Math.round(totalSoldierCasualties * ((float)cavalryCount / soldierCount));

                // Use the simple, correct tool to inflict the casualties
                inflictCasualties(humans, reign.id, Military.SWORDSMAN.value, swordsmanCasualties);
                inflictCasualties(humans, reign.id, Military.SWORDSMAN.value, archerCasualties);
                inflictCasualties(humans, reign.id, Military.SWORDSMAN.value, cavalryCasualties);
            }

            inflictCasualties(humans, reign.id, Military.REBEL.value, totalRebelCasualties);

            // Skirmishes affect morale
            reign.armyMorale--;
            if (reign.armyMorale < 0) reign.armyMorale = 0;
        }
    }

    public void occupation(Generational generation, Human[] humans){
        for (int i = 0; i < humans.length; i++){
            if (humans[i].alive == 1 && humans[i].job ==0){
                humans[i].job = (int)((Math.random() * 9)+1);
            }
        }
    }

    void createNewHumansJob(int count, int job_id, int kingdom_id, Human[] humans) {
        if (count <= 0) return;

        // --- Create the new humans at the end of the array ---
        int start_index = humans.length;
        for (int i = start_index; i < start_index + count; i++) {
            Human person = new Human();
            person.name = "Divine Recruit";
            person.health = 100;
            person.hunger = 100;
            person.job = job_id;
            person.isGeneral = 0; // Divine recruits are not leaders
            person.bronze = (int) Population.STARTINGBRONZE.value;
            person.alive = 1;
            person.kingdom_id = kingdom_id;
            humans[i] = person;
        }
    }

    public void dailyNeed(Kingdom reign, Generational generation, Human[] humans){
        int militaryCount = 0;
        int foodProduced = 0; int stoneProduced = 0;
        int woodProduced = 0; int metalProduced = 0;

        final int farmerId = 1;
        final int butcherId = 2;
        final int lumberjackId = 3;
        final int minerId = 4;
        final int blacksmithId = 5;
        final int swordsmanId = 6;
        final int archerId = 7;
        final int cavalryId = 8;
        final int rebelId = 9;

        for(int i = 0; i < humans.length; i++){

            if (humans[i].alive ==1) {

                if (humans[i].health <= 0) {
                    humans[i].alive = 0;
                    continue;
                }

                if (humans[i].health > 10) {
                    int work = (int) ((Math.random() * 3) + 1);
                    if (work >= 1) {
                        switch (humans[i].job) {
                            case farmerId:
                                humans[i].health -= 5;
                                humans[i].hunger -= 10;
                                foodProduced += (int) (Math.random() * (ProductionCost.FARMERFOODPRODUCTION.value + 1));
                                break;
                            case butcherId:
                                humans[i].health -= 10;
                                humans[i].hunger -= 15;
                                foodProduced += (int) (Math.random() * (ProductionCost.BUTCHERPRODUCTION.value + 1));
                                break;
                            case lumberjackId:
                                humans[i].health -= 15;
                                humans[i].hunger -= 20;
                                woodProduced += (int) (Math.random() * (ProductionCost.LUMBERJACKPRODUCTION.value + 1));
                                break;
                            case minerId:
                                humans[i].health -= 30;
                                humans[i].hunger -= 35;
                                if (Math.random() * 100 > ProductionCost.MINERPRODUCTIONCHANCE.value) {
                                    metalProduced += (int) (Math.random() * (ProductionCost.MINERPRODUCTION.value + 1));
                                } else {
                                    stoneProduced += (int) (Math.random() * (ProductionCost.MINERPRODUCTION.value + 1));
                                }
                                break;
                            case blacksmithId:
                                if (reign.metal > 2) {
                                    humans[i].health -= 20;
                                    humans[i].hunger -= 25;
                                    reign.metal -= ProductionCost.BLACKSMITHNEED.value;
                                } else {
                                    humans[i].hunger -= 10;
                                }
                                break;
                            case swordsmanId:
                                humans[i].health -= 5;
                                humans[i].hunger -= 10;
                                break;
                            case archerId:
                                humans[i].health -= 5;
                                humans[i].hunger -= 10;
                                break;
                            case cavalryId:
                                humans[i].health -= 5;
                                humans[i].hunger -= 10;
                                break;
                            case rebelId:
                                break;
                        }
                    } else {
                        humans[i].health += 20;
                    }
                } else {
                    humans[i].health += 5;
                }

                if (humans[i].job != Military.REBEL.value) consumeResources(reign, humans, i);

            }
        }

        reign.food += foodProduced;
        reign.wood += woodProduced;
        reign.stone += stoneProduced;
        reign.metal += metalProduced;

        applyProductionEffect(reign);

        if (reign.storyFoodDailyCap > 0 && reign.food > reign.storyFoodDailyCap){
            reign.food = reign.storyFoodDailyCap;
        }
    }

    private void consumeResources(Kingdom reign, Human[] humans, int i) {
        // --- 2. Consumption Phase ---
        // Civilians eat 2 food. Military members eat 2 + an extra amount.

        if (reign.food > 1) {

            // People need to eat to heal themselves
            if (humans[i].hunger <= ProductionCost.EATHUNGERTHRESHOLD.value) {
                if (humans[i].bronze >= ProductionCost.FOODCOST.value){
                    humans[i].bronze -= ProductionCost.FOODCOST.value;
                    reign.food -= 2;
                    if (humans[i].job > 6 && humans[i].job < 9) reign.food -= (int) Combat.MILITARY_EXTRA_FOOD_CONSUMPTION.value;
                    humans[i].hunger += (int)(Math.random() * 41)+10;
                } else {
                    humans[i].health -= 5;
                }
            }
            //if (reign.unrestLevel > WELL_FED_UNREST_REDUCTION_AMOUNT) reign.unrestLevel -= WELL_FED_UNREST_REDUCTION_AMOUNT; // Well-fed people are happier
            // this will cause unrest to never increase. keep it commented for now.
        } else {
            //log_event("!!! FAMINE in %s !!!", reign.name);
            reign.food = 0;
            reign.unrestLevel += (int) Rebellion.UNRESTGAINFROMFAMINE.value;
            int deathsFrom_starvation = (int)(reign.population * (RandomEvents.FAMINE_POPULATION_LOSS_PERCEN.value / 100.0f));
            if (deathsFrom_starvation < 1 && reign.population > 0) deathsFrom_starvation = 1;

            //log_event(" -> %d people have died from starvation!", deathsFrom_starvation);

            // --- STARVATION LOOP ---
            if (humans[i].alive == 1 && humans[i].kingdom_id == reign.id && humans[i].hunger > 0) {
                humans[i].hunger -= 10;
            } else if (humans[i].hunger < 0){
                humans[i].alive = 0;
            }
        }
    }

    private void applyProductionEffect(Kingdom reign) {
        // Apply story production modifiers
        if (reign.storyProductionModifier != 1.0) {
            reign.food = (int)(reign.food * reign.storyProductionModifier);
            reign.wood = (int)(reign.wood * reign.storyProductionModifier);
            reign.stone = (int)(reign.stone * reign.storyProductionModifier);
            reign.metal = (int)(reign.metal * reign.storyProductionModifier);
        }

        if (reign.divinePenaltyTimerDays > 0) {
            reign.food = (int)(reign.food * reign.divineProductionModifier);
            reign.wood = (int)(reign.wood * reign.divineProductionModifier);
            reign.stone = (int)(reign.stone * reign.divineProductionModifier);
            reign.metal = (int)(reign.metal * reign.divineProductionModifier);
        }
    }

    public void payments(Generational generation, Human[] humans)
    {
        // --- Iterate over actual data count, not world population estimate ---
        for (int i = 0; i < humans.length; i++)
        {
            if (humans[i].alive == 1)
            {
                switch (humans[i].job) {
                    case 1: humans[i].bronze += (int) ((Math.random() % 30) + 1); break; // Farmer
                    case 2: humans[i].bronze += (int) ((Math.random() % 50) + 1); break; // Butcher
                    case 3: humans[i].bronze += (int) ((Math.random() % 40) + 1); break; // Lumberjack
                    case 4: humans[i].bronze += (int) ((Math.random() % 30) + 1); break; // Miner
                    case 5: humans[i].bronze += (int) ((Math.random() % 85) + 1); break; // Blacksmith
                    case 6:
                    case 7:
                    case 8: humans[i].bronze += (int) ((Math.random() % 41) + 20); break; // Military
                    case 9: break; // Rebel
                }
            }
        }
    }

    public void recalculateKingdomPopulations(Kingdom[] kingdoms, Human[] humans) {
        for (int i = 0; i < kingdoms.length; i++) {
            if (kingdoms[i].isActive == 1) {
                kingdoms[i].population = 0;
            }
        }
        for (int i = 0; i < humans.length; i++) {
            if (humans[i].alive == 1) {
                int kingdom_id = humans[i].kingdom_id;
                if (kingdom_id >= 0 && kingdom_id < kingdoms.length) {
                    kingdoms[kingdom_id].population++;
                }
            }
        }
    }

    void updateKingdomUnrest(Kingdom reign, Human[] humans) {
        if (reign.isActive==0) return;

        // To balance the hourly call, we give it a 1 in 24 chance to decrease.
        // This averages out to about 1 point of unrest reduction per day.
        if (reign.unrestLevel > 0 && (Math.random() * Rebellion.HOURLYUNRESTDECAYCHANCE.value == 0)) {
            reign.unrestLevel--;
        }
    }


    // --- Handles military recruitment and its costs ---
    void recruitSoldiers(Kingdom reign, Human[] humans) {
        if (reign.isActive == 0 || reign.population == 0) return;

        // Only recruit if unrest is moderate, as a show of force.
        if (reign.unrestLevel < Rebellion.DISSENTTHRESHOLD.value / 2) return;

        // Try to recruit a small number of troops each day
        int recruitsWanted = 5 + (reign.unrestLevel / 20);
        int recruitedCount = 0;

        for (int i = 0; i < humans.length && recruitedCount < recruitsWanted; i++) {
            if (humans[i].alive == 1 && humans[i].kingdom_id == reign.id &&
                    humans[i].job > 0 && humans[i].job <= CivilJobs.BLACKSMITH.value) {
                int unitChoice = (int)(Math.random() * 3);
                if (unitChoice == 0 && reign.metal >= RecruitmentCost.COSTSWORDSMAN.value) {
                    reign.metal -= RecruitmentCost.COSTSWORDSMAN.value;
                    humans[i].job = Military.SWORDSMAN.value;
                    recruitedCount++;
                } else if (unitChoice == 1 && reign.wood >= RecruitmentCost.COSTARCHER.value) {
                    reign.wood -= RecruitmentCost.COSTARCHER.value;
                    humans[i].job = Military.ARCHER.value;
                    recruitedCount++;
                } else if (unitChoice == 2 && reign.metal >= RecruitmentCost.COSTCAVALRY.value && reign.food >= RecruitmentCost.COSTCAVALRYFOOD.value) {
                    reign.metal -= RecruitmentCost.COSTCAVALRY.value;
                    reign.food -=  RecruitmentCost.COSTCAVALRYFOOD.value;
                    humans[i].job = Military.CAVALRY.value;
                    recruitedCount++;
                }
            }
        }
    }

    void collectTaxes(Kingdom reign, Human[] humans)
    {
        if (reign.isActive ==0 || reign.population == 0) return;
        int totalTaxCollected = 0;

        for (int i = 0; i < humans.length; i++) {
            if (humans[i].alive == 1 && humans[i].kingdom_id == reign.id) {
                if (humans[i].bronze >= ProductionCost.TAXRATEPERPERSON.value) {
                    humans[i].bronze -= ProductionCost.TAXRATEPERPERSON.value;
                    totalTaxCollected += ProductionCost.TAXRATEPERPERSON.value;
                }
            }
        }
        if (reign.divinePenaltyTimerDays > 0) {
            totalTaxCollected = (int)(totalTaxCollected * reign.divineTaxModifier);
        }
        reign.treasury += totalTaxCollected;
        reign.unrestLevel += (int) Rebellion.UNRESTGAINFROMTAXES.value;
    }

    void handleRecruitmentDissent(Kingdom reign, Human[] humans) {
        if (reign.isActive == 0 || reign.unrestLevel <= Rebellion.DISSENTTHRESHOLD.value) return;

        int newRebelsThisDay = 0;
        int maxNewRebelsPerDay = (int) Rebellion.MAXNEWREBELSPERDAY.value;
        int unrestOverThreshold = (int)(reign.unrestLevel - Rebellion.DISSENTTHRESHOLD.value);
        if (unrestOverThreshold > Rebellion.MAXUNRESTFORREBELCONVERSION.value) {
            unrestOverThreshold = (int)Rebellion.MAXUNRESTFORREBELCONVERSION.value;
        }

        for (int i = 0; i < humans.length; i++) {
            if (humans[i].alive == 0 || humans[i].job == Military.REBEL.value || humans[i].kingdom_id != reign.id) continue;
            if (newRebelsThisDay >= Rebellion.MAXNEWREBELSPERDAY.value) break;

            boolean is_soldier = (humans[i].job >= Military.SWORDSMAN.value && humans[i].job <= Military.CAVALRY.value);
            boolean becameRebel = false;

            if (is_soldier) {
                int soldierDefectionChance = (int)(unrestOverThreshold * Rebellion.SOLDIERDEFECTIONCHANCE.value);
                if ((Math.random() * Rebellion.REBELCHANCEDIVISOR.value) < soldierDefectionChance) {
                    reign.armyMorale -= 5;
                    humans[i].job = Military.REBEL.value;
                    becameRebel = true;
                }
            } else {
                if ((Math.random() * Rebellion.REBELCHANCEDIVISOR.value) < unrestOverThreshold) {
                    humans[i].job = Military.REBEL.value;
                    becameRebel = true;
                }
            }

            if (becameRebel) {
                newRebelsThisDay++;
                if (Math.random() * 100 < Rebellion.REBELLEADERSPAWNCHANCE.value) {
                    humans[i].isGeneral = 1;
                }
            }
        }

        if (reign.armyMorale < 0) reign.armyMorale = 0;
    }

    public void EmpireAI(Kingdom reign, Human[] humans) {
        // --- 1. Intelligence Gathering Phase ---
        // Avoids action if the population is too small to matter.
        if (reign.population < 100) return;

        // Calculate key metrics
        int dailyFoodConsumption = reign.population + 1;
        float foodDaysLeft = (dailyFoodConsumption > 0) ? (float)reign.food / dailyFoodConsumption : 999;
        int soldierCount = 0, rebelCount = 0;
        for (int i = 0; i < humans.length; i++) {
            if (humans[i].alive == 1 && humans[i].kingdom_id == reign.id) {
                if (humans[i].job >= Military.SWORDSMAN.value && humans[i].job <= Military.CAVALRY.value) soldierCount++;
                if (humans[i].job == Military.REBEL.value) rebelCount++;
            }
        }

        float militaryRatio = (float)soldierCount / (float)(rebelCount + 1);
        float food_urgency = (foodDaysLeft < AIGovernor.AI_FOOD_DAYS_THRESHOLD.value) ? 1.0f - (foodDaysLeft / AIGovernor.AI_FOOD_DAYS_THRESHOLD.value) : 0.0f;
        float unrest_urgency = (float)reign.unrestLevel / Rebellion.REBELLIONTHRESHOLD.value;
        float military_urgency = (militaryRatio < 1.0f) ? 1.0f - militaryRatio : 0.0f;

        // --- TIER 0: DIVINE INTERVENTION ---
        // The AI's last resort when it is rich but desperate.
        if (reign.canUseDivineIntervention && reign.treasury > DivineIntervention.AI_DIVINE_INTERVENTION_TREASURY_THRESHOLD.value) {
            // Condition 1: About to be militarily overrun
            if (military_urgency > 0.8f) {
                //log_event("GOVERNOR AI: Our armies are collapsing! We pray for divine reinforcements!");
                reign.treasury -= (int) ReinforceIntervention.DI_COST_REINFORCEMENTS.value;
                createNewHumansJob((int) ReinforceIntervention.DI_REINFORCEMENTS_COUNT.value, Military.SWORDSMAN.value, reign.id, humans);
                reign.divineTaxModifier = ReinforceIntervention.DI_PENALTY_TAX_MODIFIER.value;
                reign.divinePenaltyTimerDays = DivineIntervention.PENALTY_DURATION_DAYS.value;
                reign.canUseDivineIntervention = false; // Used its one miracle for the day
                return;
            }
            // Condition 2: Mass unrest threatening imminent collapse
            if (reign.unrestLevel > InterventionAbsolution.DI_ABSOLUTION_UNREST_THRESHOLD.value && unrest_urgency > 0.7f) {
                //log_event("GOVERNOR AI: The people threaten to tear down the walls! We beg for divine absolution!");
                reign.treasury -= InterventionAbsolution.DI_COST_ABSOLUTION.value;
                reign.unrestLevel -= InterventionAbsolution.DI_ABSOLUTION_UNREST_REDUCTION.value;
                reign.armyMorale -= InterventionAbsolution.DI_PENALTY_MORALE_DROP.value;
                if (reign.armyMorale < 0) reign.armyMorale = 0;
                reign.divinePenaltyTimerDays = DivineIntervention.PENALTY_DURATION_DAYS.value; // Can apply a generic penalty if desired
                reign.canUseDivineIntervention = false;
                return;
            }
            // Condition 3: Catastrophic famine imminent
            if (food_urgency > 0.9f) {
                //log_event("GOVERNOR: The granaries are empty and the people starve! We plead for a miracle of sustenance!");
                reign.treasury -= (int)(InterventionSustenance.DI_COST_SUSTENANCE.value);
                reign.food += (int)(InterventionSustenance.DI_SUSTENANCE_FOOD_AMOUNT.value);
                reign.divineProductionModifier = InterventionSustenance.DI_PENALTY_PRODUCTION_MODIFIER.value;
                reign.divinePenaltyTimerDays = DivineIntervention.PENALTY_DURATION_DAYS.value;
                reign.canUseDivineIntervention = false;
                return;
            }
        }


        // --- 2. Tier 1: Catastrophe Aversion ---
        // If we are about to starve, this is the ONLY priority. Nothing else matters.
        if (foodDaysLeft < AIGovernor.AI_CRITICAL_FOOD_DAYS_THRESHOLD.value) {
            // log_event("GOVERNOR: Reassigning all available");
            int converted_workers = 0;
            for (int i = 0; i < humans.length && converted_workers < AIGovernor.AI_FARMER_CONVERSION_COUNT.value; i++) {
                if (humans[i].alive == 1 && humans[i].kingdom_id == reign.id) {
                    // Convert non-essential jobs to farmers
                    if (humans[i].job == CivilJobs.LUMBERJACK.value || humans[i].job == CivilJobs.MINER.value || humans[i].job == CivilJobs.BLACKSMITH.value) {
                        humans[i].job = CivilJobs.FARMER.value;
                        converted_workers++;
                    }
                }
            }
            return; // Override all other logic
        }

        // --- 3. Tier 2: Reactive Problem Solving ---
        // Calculate urgency scores for major problems.

        // Find the most urgent problem
        float max_urgency = 0.0f;
        if (food_urgency > max_urgency) max_urgency = food_urgency;
        if (unrest_urgency > max_urgency) max_urgency = unrest_urgency;
        if (military_urgency > max_urgency) max_urgency = military_urgency;

        // Only act if the problem is significant enough to warrant a response.
        if (max_urgency > AIGovernor.AI_ACTION_THRESHOLD.value) {
            if (military_urgency == max_urgency) {
                //log_event("GOVERNOR: Prioritizing recruitment.");
                recruitSoldiers(reign, humans);
                return;
            }
            if (unrest_urgency == max_urgency && reign.treasury >= AIGovernor.AI_FESTIVAL_COST.value) {
                //log_event("GOVERNOR: Hosting festivals to calm the populace.");
                reign.treasury -= (int) AIGovernor.AI_FESTIVAL_COST.value;
                reign.unrestLevel -= (int) AIGovernor.AI_FESTIVAL_UNREST_REDUCTION.value;
                if (reign.unrestLevel < 0) reign.unrestLevel = 0;
                return;
            }
            if (food_urgency == max_urgency) {
                //log_event("GOVERNOR: Assigning more workers to farms.");
                int converted_workers = 0;
                for (int i = 0; i < humans.length && converted_workers < AIGovernor.AI_FARMER_CONVERSION_COUNT.value / 2; i++) { // Less drastic than the catastrophe response
                    if (humans[i].alive == 1 && humans[i].kingdom_id == reign.id && (humans[i].job == CivilJobs.LUMBERJACK.value || humans[i].job == CivilJobs.MINER.value)) {
                        humans[i].job = CivilJobs.FARMER.value;
                        converted_workers++;
                    }
                }
                return;
            }
        }

        // --- 4. Tier 3: Proactive Management ---
        // If there are no immediate crises, the AI works on long-term stability.
        // Its primary goal is to maintain a standing army relative to its population.
        float ideal_army_size = reign.population * AIGovernor.AI_ARMY_SIZE_GOAL_PERCENT.value;
        if (soldierCount < ideal_army_size) {
            //log_event("GOVERNOR: The kingdom is stable.");
            recruitSoldiers(reign, humans); // Recruit slowly to reach the ideal size
        } else {
            // If all goals are met, the AI does nothing and saves resources.
            // You could add a log_event here for debugging if you want.
            // log_event("GOVERNOR AI: The kingdom is stable and well-defended. Conserving resources.");
        }
    }

    public void manageKingdomDaily(Kingdom reign, Human[] humans) {
        if (reign.isActive == 0) return;

        if (reign.divinePenaltyTimerDays > 0) {
            reign.divinePenaltyTimerDays--;
            if (reign.divinePenaltyTimerDays == 0) {
                //log_event("Divine collateral has been paid for %s. The economy returns to normal.", reign.name);
                reign.divineTaxModifier = 1.0f;
                reign.divineProductionModifier = 1.0f;
            }
        }

        reign.canUseDivineIntervention = true;

        // --- Store "Before" State ---
        int unrestBefore = reign.unrestLevel;
        int popBefore = reign.population;
        int foodBefore = reign.food;
        int woodBefore = reign.wood;
        int stoneBefore = reign.stone;
        int metalBefore = reign.metal;

        // --- Run Daily Logic ---
        collectTaxes(reign, humans);
        updateKingdomUnrest(reign, humans);
        handleRecruitmentDissent(reign, humans);
        recruitSoldiers(reign, humans);
        EmpireAI(reign, humans);

        // Morale Management (This is a daily check)
        if (reign.food > reign.population * Morale.MORALE_FOOD_SURPLUS_MULTIPLIER.value && reign.armyMorale < 100) { reign.armyMorale += (int) Morale.MORALE_GAIN_FROM_SURPLUS.value; }
        if (reign.unrestLevel > Rebellion.DISSENTTHRESHOLD.value && reign.armyMorale > Morale.MINIMUM_MORALE_FOR_UNREST_LOSS.value) { reign.armyMorale -= (int) Morale.MORALE_LOSS_FROM_UNREST.value; }

        // After potential famine deaths, recount the population
        //recalculateKingdomPopulations(reign, humans);

        //wait this function takes an array of kingdoms not 1 by itself.
        //why was this like that in c?

    }

    void manage_empire(Kingdom reign, Human[] humans) {
        if (reign.isActive == 0) return;
        manageKingdomDaily(reign, humans);
    }

    void EstherKingdom(Kingdom reign, Human[] humans) {
        if (reign.isActive == 0) return;
        // This is where I could add unique behavior for Esther
        manageKingdomDaily(reign, humans);

    }

    void MagentanoKingdom(Kingdom reign, Human[] humans) {
        if (reign.isActive == 0) return;
        manageKingdomDaily(reign, humans);
    }

    void NightingaleKingdom(Kingdom reign, Human[] humans) {
        if (reign.isActive == 0) return;
        manageKingdomDaily(reign, humans);
    }

    void LionheartKingdom(Kingdom reign, Human[] humans) {
        if (reign.isActive == 0) return;
        manageKingdomDaily(reign, humans);
    }

    void RethmarKingdom(Kingdom reign, Human[] humans) {
        if (reign.isActive == 0) return;
        manageKingdomDaily(reign, humans);
    }

    void TehranKingdom(Kingdom reign, Human[] humans) {
        if (reign.isActive == 0) return;
        manageKingdomDaily(reign, humans);
    }

    void AsfahanKingdom(Kingdom reign, Human[] humans) {
        if (reign.isActive == 0) return;
        manageKingdomDaily(reign, humans);
    }


    static void event_bountiful_harvest(Kingdom reign) {
        int food_gain = (int)(EventSpecific.HARVEST_BASE_FOOD_GAIN.value + (reign.population * EventSpecific.HARVEST_POPULATION_FOOD_MULTIPLIER.value));
        reign.food += food_gain;
        //log_event("EVENT: A bountiful harvest in %s\n", reign.name);
    }

    static void event_discovery_of_gold(Kingdom reign, Human[] humans) {
        //log_event("EVENT: A vein of gold discovery in %s!\n", reign.name);
        for (int i = 0; i < humans.length; i++) {
            if (humans[i].alive == 1 && humans[i].kingdom_id == reign.id) {
                humans[i].bronze = (int)(humans[i].bronze + EventSpecific.GOLD_DISCOVERY_BRONZE_BONUS.value); // Give a nice bonus to everyone
            }
        }
    }

    static void killRandomCivilians(Kingdom reign, Human[] humans, int count) {
        if (count <= 0) return;
        int casualties = 0;
        int attempts = 0;
        while (casualties < count && attempts < humans.length * 2) {
            int rand_idx = (int)(Math.random() * humans.length);
            // Target is alive, belongs to the kingdom, and is a civilian (job 1-5)
            if (humans[rand_idx].alive == 1 &&
                    humans[rand_idx].kingdom_id == reign.id &&
                            humans[rand_idx].job >= 1 && humans[rand_idx].job <= 5)
            {
                humans[rand_idx].alive = 0;
                casualties++;
            }
            attempts++;
        }
        //log_event(" -> A disaster has claimed the lives of %d civilians in %s.\n", casualties, reign.name);
    }

    static void event_plague(Kingdom reign, Human[] humans) {
        //log_event("EVENT: Sickness and decay!");
        int deaths = (int)(reign.population * RandomEvents.PLAGUE_POPULATION_LOSS_PERCENT.value); // 10% of the population will perish
        killRandomCivilians(reign, humans, deaths);
        reign.unrestLevel = (int)(reign.unrestLevel + EventSpecific.PLAGUE_UNREST_GAIN.value);
    }


    static void event_drought(Kingdom reign, Human[] humans) {
        //log_event("EVENT: Heat wave, the fields are dust!");

        reign.food = (int)(reign.food*0.15); // keep 15% of total food

        if (reign.food < 0) reign.food=0;

        // Increase the unrest penalty to reflect the severity.
        reign.unrestLevel = (int)(reign.unrestLevel + EventSpecific.DROUGHT_UNREST_GAIN.value);
    }

    static void event_barbarian_raids(Kingdom reign, Human[] humans) {
        //log_event("EVENT: Barbarians raiding !");
        int deaths = (int)(reign.population * RandomEvents.BARBARIAN_POPULATION_LOSS_PERCENT.value); // 2% of population lost
        killRandomCivilians(reign, humans, deaths);
        reign.wood = (int)(reign.wood * EventSpecific.BARBARIAN_RAID_RESOURCE_LOSS_PERCENT.value); // Lose 20% of wood
        reign.stone = (int)(reign.stone * EventSpecific.BARBARIAN_RAID_RESOURCE_LOSS_PERCENT.value); // Lose 20% of stone
        reign.unrestLevel = (int)(reign.unrestLevel + EventSpecific.BARBARIAN_RAID_UNREST_GAIN.value);
    }

    static void event_political_intrigue(Kingdom reign) {
        //log_event("EVENT: A plot is uncovered in the court!", reign.name);
        reign.unrestLevel = (int)(reign.unrestLevel + EventSpecific.BARBARIAN_RAID_UNREST_GAIN.value);
    }

    void trigger_random_event(Kingdom reign, Human[] humans) {
        // Only active kingdoms with a population can have events
        if (reign.isActive == 0 || reign.population < 50) return;

        if ((Math.random() * 101) >= RandomEvents.DAILY_RANDOM_EVENT_CHANCE_PERCENT.value) return; // Nothing happens today.
        int event_id = (int)(Math.random() * (RandomEvents.TOTAL_RANDOM_EVENTS.value+1));

        switch (event_id) {
            case 0:
                event_bountiful_harvest(reign);
                break;
            case 1:
                event_discovery_of_gold(reign, humans);
                break;
            case 2:
                event_plague(reign, humans);
                break;
            case 3:
                event_drought(reign, humans);
                break;
            case 4:
                event_barbarian_raids(reign, humans);
                break;
            case 5:
                event_political_intrigue(reign);
                break;
        }
    }

    void compact_dead_humans(Human[] humans) {
        int write_index = 0;

        // Compact: move all living humans to the front
        for (int read_index = 0; read_index < humans.length; read_index++) {
            if (humans[read_index].alive == 1) {
                if (write_index != read_index) {
                    humans[write_index] = humans[read_index];
                }
                write_index++;
            }
        }

        // Update count to only living humans
        int old_count = humans.length;

        // No need to reallocate here, we can just use less of the existing buffer.
        // The buffer will grow again naturally via the persona function when needed.
        //printf("Compacted humans: %d -> %d (removed %d dead)\n",
        //       old_count, humans.length, old_count - humans.length);
    }

    public Kingdom(
            int id, String name,
            int unrestLevel, int isActive,
            int food, int wood,
            int stone, int metal,
            int treasury, int armyMorale,
            int storySkirmishOverride, float storySkirmishChanceModifier,
            float storyProductionModifier, int storyFoodDailyCap,
            float storyConsumptionModifier, float divineTaxModifier,
            float divineProductionModifier, int divinePenaltyTimerDays,
            boolean canUseDivineIntervention) {

        this.id = id;
        this.name = name;
        this.unrestLevel = unrestLevel;
        this.isActive = isActive;

        this.food = food;
        this.wood = wood;
        this.stone = stone;
        this.metal = metal;
        this.treasury = treasury;

        this.armyMorale = armyMorale;

        this.storySkirmishOverride = storySkirmishOverride;
        this.storySkirmishChanceModifier = storySkirmishChanceModifier;
        this.storyProductionModifier = storyProductionModifier;
        this.storyFoodDailyCap = storyFoodDailyCap;
        this.storyConsumptionModifier = storyConsumptionModifier;

        this.divineTaxModifier = divineTaxModifier;
        this.divineProductionModifier = divineProductionModifier;
        this.divinePenaltyTimerDays = divinePenaltyTimerDays;
        this.canUseDivineIntervention = canUseDivineIntervention;
    }
}
