package com.kiyamet.npc;

/** Rank inside a group. Level ranges: member 5-120, guard 40-180, trader 20-80, leader 150-230, commander 260-300. */
public enum Role {
    MEMBER("Member", 1.0, 1.0),
    GUARD("Guard", 1.25, 1.15),
    TRADER("Trader", 1.0, 0.9),
    LEADER("Leader", 1.6, 1.3),
    COMMANDER("Commander", 2.6, 1.7);

    public final String title;
    public final double hpMult, dmgMult;

    Role(String title, double hpMult, double dmgMult) { this.title = title; this.hpMult = hpMult; this.dmgMult = dmgMult; }

    public static Role byId(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }
}
