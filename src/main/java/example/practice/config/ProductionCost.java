package example.practice.config;

public enum ProductionCost {
    TAXRATEPERPERSON(1),
    FARMERFOODPRODUCTION(2),
    LUMBERJACKPRODUCTION(1),
    BUTCHERPRODUCTION(2),
    MINERPRODUCTION(3),
    MINERPRODUCTIONCHANCE(25),
    BLACKSMITHNEED(3),
    FAMINEPOPULATIONLOSSPERCENT(5),
    WORKSTARTHOUR(4),
    WORKENDHOUR(22),
    EATHUNGERTHRESHOLD(40),
    FOODCOST(15);

    public final int value;

    ProductionCost(int value){
        this.value = value;
    }
}


