package example.practice.agents;

import java.util.ArrayList;
import java.util.List;

// The card-game core: two characters resolve a duel or an assassination from their
// stats and twists, returning a blow-by-blow log so the UI can play it out
// card-against-card. No global state - hand it any two agents.
public class AgentCombat {

    public static class Result {
        public Agent winner, loser;
        public boolean decisive;             // someone died or yielded
        public final List<String> log = new ArrayList<>();
    }

    // A straight fight. Speed sets the order; a Duelist always strikes first.
    public static Result duel(Agent a, Agent b) {
        Result r = new Result();
        a.hp = a.maxHp; b.hp = b.maxHp; a.usedSurvivor = false; b.usedSurvivor = false;

        if (a.twist == AgentTwist.IMMOVABLE || b.twist == AgentTwist.IMMOVABLE) {
            r.log.add("One of them will not be drawn into a fight. No contest.");
            return r;
        }
        if (talksDown(a) || talksDown(b)) {
            r.log.add("Words win where steel would not. The duel is talked down.");
            return r;
        }

        Agent first = order(a, b), second = (first == a) ? b : a;
        r.log.add(first.name + " moves first.");

        int guard = 0;
        while (a.hp > 0 && b.hp > 0 && guard++ < 100) {
            if (!exchange(first, second, r)) break;
            if (!exchange(second, first, r)) break;
        }
        finish(a, b, r);
        return r;
    }

    // A knife in the dark: cunning against the target's wits and reflexes.
    public static Result assassinate(Agent killer, Agent target) {
        Result r = new Result();
        if (killer.twist == AgentTwist.IMMOVABLE) {
            r.log.add(killer.name + " refuses such work.");
            return r;
        }
        float chance = 0.40f
                + (killer.cunning - target.cunning) * 0.010f
                - target.speed * 0.004f;
        if (killer.twist == AgentTwist.SHADOW) chance += 0.25f;
        if (target.twist == AgentTwist.JUGGERNAUT) chance -= 0.20f;
        chance = Math.max(0.05f, Math.min(0.92f, chance));

        if (Math.random() < chance) {
            if (target.twist == AgentTwist.SURVIVOR && !target.usedSurvivor) {
                target.usedSurvivor = true; target.hp = Math.max(1, target.maxHp / 4);
                r.log.add(killer.name + " strikes true - but " + target.name + " somehow walks away.");
                r.winner = target;
            } else {
                target.alive = false;
                r.log.add(killer.name + " ends " + target.name + " in the dark.");
                r.winner = killer; r.loser = target; r.decisive = true;
            }
        } else {
            target.suspicion = Math.min(100, target.suspicion + 25);
            r.log.add(target.name + " catches the blade in time. The attempt is known.");
            r.winner = target;
        }
        return r;
    }

    private static boolean exchange(Agent atk, Agent def, Result r) {
        int dmg = Math.max(1, atk.attack - def.defense / 2);
        if (atk.twist == AgentTwist.GLASS_CANNON) dmg = Math.round(dmg * 1.2f);
        if (atk.twist == AgentTwist.WILDCARD) {
            double roll = Math.random();
            if (roll < 0.20) { dmg *= 2; r.log.add(atk.name + " does something reckless and brilliant."); }
            else if (roll > 0.85) { dmg = Math.round(dmg * 0.3f); r.log.add(atk.name + " fumbles it."); }
        }
        def.hp -= dmg;
        r.log.add(atk.name + " hits " + def.name + " for " + dmg + " (" + Math.max(0, def.hp) + " left).");
        if (def.hp <= 0 && def.twist == AgentTwist.SURVIVOR && !def.usedSurvivor) {
            def.usedSurvivor = true; def.hp = Math.max(1, def.maxHp / 4);
            r.log.add(def.name + " should be dead - and isn't.");
        }
        return def.hp > 0;
    }

    private static Agent order(Agent a, Agent b) {
        if (a.twist == AgentTwist.DUELIST && b.twist != AgentTwist.DUELIST) return a;
        if (b.twist == AgentTwist.DUELIST && a.twist != AgentTwist.DUELIST) return b;
        return a.speed >= b.speed ? a : b;
    }
    private static boolean talksDown(Agent a) {
        return a.twist == AgentTwist.SILVER_TONGUE && Math.random() < 0.30;
    }
    private static void finish(Agent a, Agent b, Result r) {
        if (a.hp <= 0 && b.hp > 0) { a.alive = false; r.winner = b; r.loser = a; r.decisive = true; }
        else if (b.hp <= 0 && a.hp > 0) { b.alive = false; r.winner = a; r.loser = b; r.decisive = true; }
        if (r.decisive) r.log.add(r.winner.name + " stands over " + r.loser.name + ".");
    }
}