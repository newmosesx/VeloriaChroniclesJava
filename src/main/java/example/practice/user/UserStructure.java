package example.practice.user;


public class UserStructure {

    public UserStructure(String name){

        this.userName = name;
        this.statSmart = 20;
        this.statDamage = 35;
        this.statDefense = 40;
        this.statHealth = 200;
        this.statSpeed = 3;
        this.statHunger = 100;
        this.statExp = 0;
        this.userLife = true;

    }

    String userName;

    String userHead;
    String userTorso;
    String userLegs;
    String userFoots;
    String rightHand;
    String leftHand;

    int statLevel;

    float statSmart;
    float statDamage;
    float statDefense;
    float statHealth;
    float statSpeed;
    float statHunger;
    float statExp;

    boolean userLife;



    private void userAddHead(String equipment){

        this.userHead = equipment;
    }

    private void userAddTorso(String equipment){

        this.userTorso = equipment;
    }

    private void userAddLegs(String equipment){

        this.userLegs = equipment;
    }

    private void userAddFoots(String equipment){

        this.userFoots = equipment;
    }

    private void userAddLeft(String equipment){

        this.leftHand = equipment;
    }

    private void userAddRight(String equipment){

        this.rightHand = equipment;
    }

    private void userEat(String food){

        this.statHunger += 0; // function(food) -> return int
    }

    private void levelUp(){
        this.statLevel += 1;
    }
}