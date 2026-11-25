package com.kiyamet.skill;

/** xp_to_next(n) = round((120 + 55 * n^0.42) * 1.018^max(0, n - 880)); ~1.14M XP from 1 to 999. */
public final class XpCurve {
    private static final long[] NEXT = new long[Skill.MAX_LEVEL + 1];
    static {
        for (int n = 1; n <= Skill.MAX_LEVEL; n++)
            NEXT[n] = Math.round((120 + 55 * Math.pow(n, 0.42)) * Math.pow(1.018, Math.max(0, n - 880)));
    }
    public static long toNext(int level) { return level >= Skill.MAX_LEVEL ? Long.MAX_VALUE : NEXT[Math.max(1, level)]; }
    private XpCurve() {}
}
