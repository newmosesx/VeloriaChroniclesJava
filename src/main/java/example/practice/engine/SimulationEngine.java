package example.practice.engine;

import example.practice.agents.AgentRoster;
import example.practice.config.DailyBatch;
import example.practice.config.ProductionCost;
import example.practice.events.EventSystem;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.config.Population;
import example.practice.shared.ShareData;
import example.practice.user.Player;
import example.practice.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import example.practice.world.World;

public class SimulationEngine implements Runnable {
    private List<Human> worldPopulation;
    private Kingdom[] kingdoms;
    private boolean isRunning = true;
    private Player player;

    public World world = new World();             // with the other fields
    public World getWorld() { return world; }

    private final ReentrantLock dataLock = new ReentrantLock();

    public List<Human> getWorldPopulation() { return worldPopulation; }

    public void stop() { isRunning = false; }
    public void lock() { dataLock.lock(); }
    public void unlock() { dataLock.unlock(); }

    private int day = 1;
    private int hour = 0;

    public ShareData sharedData = new ShareData();

    public final AgentRoster roster = AgentRoster.seedDefault();
    public AgentRoster getRoster() { return roster; }

    public SimulationEngine() {
        this.worldPopulation = new ArrayList<>();
        this.kingdoms = new Kingdom[(int)Population.NUMKINGDOMS.value];
        this.player = new Player("Emperor Valerius");

        System.out.printf("Engine booted at: %02d:%02d on Day %d%n",
                TimeSpace.getSysHour(), TimeSpace.getSysMin(), TimeSpace.getSysDay());

        kingdoms[0] = new Kingdom(0, "The Great Empire", true);
        for(int i = 1; i < kingdoms.length; i++) {
            kingdoms[i] = new Kingdom(i, "Successor " + i, false);
        }

        int initialPop = (int) Population.INITIALPOPULATION.value;
        int generalCount = 0; // Track initial generals

        for (int i = 0; i < initialPop; i++) {
            Human h = new Human(0);
            int roll = (int)(Math.random() * 100);
            if (roll < 5) h.job = 7;
            else if (roll < 10) h.job = 8;
            else if (roll < 15) {
                h.job = 6;
                // --- INITIAL GENERAL SPAWNING (Max 3, 2% chance) ---
                if (generalCount < 3 && Math.random() * 100 < 2) {
                    h.isGeneral = true;
                    generalCount++;
                }
            }
            else if (roll < 25) h.job = 5;
            else if (roll < 35) h.job = 4;
            else if (roll < 50) h.job = 3;
            else h.job = 1;

            worldPopulation.add(h);
        }
        System.out.println("Initial job assignments complete. The empire's army, workforce, and " + generalCount + " generals are ready!");

        sharedData.currentStoryChapter = 0;
        sharedData.currentStoryParagraph = 0;

        for (Kingdom k : kingdoms) {
            k.population = (int) worldPopulation.stream().filter(h -> h.kingdomId == k.id).count();
            if(k.isActive) k.updateCensus(worldPopulation);
        }
    }

    public Kingdom[] getKingdoms() { return kingdoms; }
    public Player getPlayer() { return player; }
    public int getTotalPopulation() { return worldPopulation.size(); }
    public String getFormattedTime() { return "Day " + day + ", " + String.format("%02d:00", hour); }

    public void updateStoryPosition(int ch, int p) {
        this.sharedData.currentStoryChapter = ch;
        this.sharedData.currentStoryParagraph = p;
        StoryManager.applyStoryEffects(ch, p, kingdoms[0], worldPopulation);
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                dataLock.lock();
                try {
                    processHour();
                    hour++;
                    if (hour >= 24) {
                        hour = 0;
                        processDay();
                        day++;
                    }
                } finally {
                    dataLock.unlock();
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void processHour() {
        StoryManager.applyStoryEffects(sharedData.currentStoryChapter, sharedData.currentStoryParagraph, kingdoms[0], worldPopulation);

        // --- HOURLY UNREST DECAY ---
        for (Kingdom k : kingdoms) {
            // If limiters are disabled, unrest NEVER naturally decays anymore
            if (k.isActive && k.unrestLevel > 0 && !k.limitersDisabled && Math.random() * 4 < 1) {
                k.unrestLevel--;
            }
        }

        int totalBatches = (int) DailyBatch.BATCHES_PER_DAY.value;
        int batchSize = worldPopulation.size() / totalBatches;
        int currentBatch = hour / (24 / totalBatches);

        int startIdx = currentBatch * batchSize;
        int endIdx = (currentBatch == totalBatches - 1) ? worldPopulation.size() : (currentBatch + 1) * batchSize;

        List<Human> currentShift = worldPopulation.subList(startIdx, endIdx);

        if (hour >= (int)ProductionCost.WORKSTARTHOUR.value && hour < (int) ProductionCost.WORKENDHOUR.value) {
            for (Kingdom k : kingdoms) {
                if (k.isActive) {
                    EconomyManager.occupation(currentShift);
                    EconomyManager.processBatchNeeds(k, currentShift);
                }
            }
        }

        if (hour == 6 || hour == 14 || hour == 22) {
            EconomyManager.distributePayments(currentShift);
        }

        for (Kingdom k : kingdoms) {
            if (k.isActive) CombatManager.triggerHourlySkirmish(k, worldPopulation);
        }

        for (Kingdom k : kingdoms) {
            if (k.isActive) k.updateCensus(worldPopulation);
        }

        if (hour % 4 == 0) {
            Logger.pruneLogs();
        }
    }

    private void processDay() {
        world.advanceDay();
        DailyEventTracker.resetDailyFlags();

        RebellionManager.checkEmpireCollapse(kingdoms, worldPopulation);
        RebellionManager.checkCivilWarTrigger(kingdoms, worldPopulation, sharedData);

        for (Kingdom k : kingdoms) {
            if (k.isActive) {
                k.collectTaxes(worldPopulation);
                SubsistenceManager.process(k, worldPopulation, world);
                EdictManager.processDaily(k, worldPopulation);
                PoliticsManager.process(k, worldPopulation, world);
                RebellionManager.handleRecruitmentAndDissent(k, worldPopulation);
                k.tickDivinePenalty();
                k.updateDailyMorale();
                if (ManpowerManager.canRecruit(k, worldPopulation)) k.recruitSoldiers(worldPopulation);
                EventSystem.process(k, worldPopulation, world, day);
                ManpowerManager.process(k, worldPopulation);
                roster.tick();
            }
        }

        TechManager.processDaily();
        PopulationManager.compactDeadHumans(worldPopulation);

        for (Kingdom k : kingdoms) {
            if (k.isActive) {
                k.population = (int) worldPopulation.stream().filter(h -> h.kingdomId == k.id).count();
            }
        }
    }

    public int getDay() { return day; }
    public int getHour() { return hour; }
    public void setDayHour(int d, int h) { this.day = d; this.hour = h; }

}