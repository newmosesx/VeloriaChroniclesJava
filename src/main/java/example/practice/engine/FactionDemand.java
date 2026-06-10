package example.practice.engine;

import example.practice.config.FactionType;

// A standing demand raised by an estate once its pressure boils over. It is the
// political mirror of a battle: the player can CONCEDE (pay the price, calm the
// estate, and usually anger a rival) or REFUSE (spend nothing, but the slighted
// estate's grievance spikes through the aggravate() hook). Left unanswered past
// its patience, it lapses into a refusal on its own - silence is an answer.
//
// Demands are DERIVED, never scripted: PoliticsManager raises one when an estate
// crosses the boil-over line, and the concede/refuse fallout flows back through
// the same grievance model everything else uses. Nothing here pokes unrest.
public class FactionDemand {

    public final FactionType from;        // who is making the demand
    public final FactionType rival;       // who resents you giving in (may be null)
    public final String headline;         // one line shown to the player
    public final String concedeLabel;     // button text for giving in
    public final int goldCost;            // treasury cost of conceding
    public final int foodCost;            // granary cost of conceding
    public final float concedeRelief;     // grievance eased on 'from' if conceded
    public final float rivalBacklash;     // grievance added to 'rival' if conceded
    public final float refuseBacklash;    // grievance added to 'from' if refused

    public int daysLeft;                  // patience; hits 0 -> auto-refuse

    public FactionDemand(FactionType from, FactionType rival, String headline,
                         String concedeLabel, int goldCost, int foodCost,
                         float concedeRelief, float rivalBacklash, float refuseBacklash,
                         int patienceDays) {
        this.from = from;
        this.rival = rival;
        this.headline = headline;
        this.concedeLabel = concedeLabel;
        this.goldCost = goldCost;
        this.foodCost = foodCost;
        this.concedeRelief = concedeRelief;
        this.rivalBacklash = rivalBacklash;
        this.refuseBacklash = refuseBacklash;
        this.daysLeft = patienceDays;
    }

    public boolean expired() { return daysLeft <= 0; }
}