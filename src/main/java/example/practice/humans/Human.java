package example.practice.humans;

import example.practice.config.Population;

public class Human {
    public String name = "Adam";
    public int kingdomId;
    public boolean isAlive = true;
    public boolean isGeneral = false;

    public int job = 0; // 0 = Unemployed
    public int bronze;

    // Stats
    public int smart, damage, defense, speed;
    public int health = 200;
    public int hunger = 100;
    public int level = 1;
    public double experience = 0.0;

    // Quirks
    public int[] quirks = new int[3];

    // Equipment (IDs)
    public int head, torso, legs, feet;
    public int rightHand, leftHand;

    // Constructor to easily spawn new humans
    public Human(int kingdomId) {
        this.kingdomId = kingdomId;
        this.bronze = (int) Population.STARTINGBRONZE.value;
        this.smart = (int) (Math.random() * 31) + 1;
        this.damage = (int) (Math.random() * 31) + 1;
        this.defense = (int) (Math.random() * 31) + 1;
        this.speed = (int) (Math.random() * 31) + 1;

        this.quirks[0] = (int)(Math.random() * 3);
        this.quirks[1] = (int)(Math.random() * 3);
        this.quirks[2] = (int)(Math.random() * 3);
    }
}