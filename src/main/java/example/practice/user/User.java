package example.practice.user;

public class User {
    final String currentPlayerName = "Thermal"; // default for now

    //stats
    int currentPlayerSmart;
    int currentPlayerDamage;
    int currentPlayerDefense;
    int currentPlayerHealth;
    int currentPlayerSpeed;
    int currentPlayerHunger;
    int currentPlayerLevel;
    double currentPlayerExpirience;

    //armor
    int currentPlayerHead;
    int currentPlayerTorso;
    int currentPlayerLegs;
    int currentPlayerFoots;

    //items
    int currentPlayerRight;
    int currentPlayerLeft;

    //status
    int alive; // 0 dead - 1 alive

    public void playerSetup(User player){
        // Status
        player.alive = 1;

        // Stats
        player.currentPlayerDamage = 35;
        player.currentPlayerHealth = 200;
        player.currentPlayerDefense = 40;
        player.currentPlayerSpeed = 3; //1 - 10
        player.currentPlayerHunger = 100;
        player.currentPlayerLevel = 1;
        player.currentPlayerExpirience = 0;

        // Equipment
        player.currentPlayerHead = 2; // 2 for base iron
        player.currentPlayerTorso = 2;
        player.currentPlayerLegs = 2;
        player.currentPlayerFoots = 2;

        // Items
        player.currentPlayerRight = 0;
        player.currentPlayerLeft = 0;
    }
}
