package example.practice.user;

public class Player {
    public String userName;
    public boolean isAlive;

    // Stats
    public float statSmart, statDamage, statDefense;
    public float statHealth, statMaxHealth, statSpeed, statHunger, statExp;
    public int statLevel;

    // RPG specific stats
    public int strength;
    public int intellect;
    public int charisma;
    public int age;

    // Equipment
    public String userHead, userTorso, userLegs, userFeet;
    public String rightHand, leftHand;

    public Player(String name) {
        this.userName = name;
        this.isAlive = true;
        this.statSmart = 20;
        this.statDamage = 35;
        this.statDefense = 40;
        this.statMaxHealth = 200;
        this.statHealth = 200;
        this.statSpeed = 3;
        this.statHunger = 100;
        this.statLevel = 1;
        this.statExp = 0;

        // RPG Details
        this.strength = 15;
        this.intellect = 18;
        this.charisma = 16;
        this.age = 42;
    }

    public void eat(String food) {
        this.statHunger += 20;
    }

    public void levelUp() {
        this.statLevel += 1;
    }
}