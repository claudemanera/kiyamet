package com.kiyamet.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable per-player skill state stored as a persistent data attachment. */
public record SkillData(List<Integer> levels, List<Long> xp, long restDay, int restTicks) {
    public static final Codec<SkillData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.listOf().fieldOf("levels").forGetter(SkillData::levels),
            Codec.LONG.listOf().fieldOf("xp").forGetter(SkillData::xp),
            Codec.LONG.optionalFieldOf("rest_day", 0L).forGetter(SkillData::restDay),
            Codec.INT.optionalFieldOf("rest_ticks", 0).forGetter(SkillData::restTicks)
    ).apply(i, SkillData::new));

    public static SkillData empty() {
        return new SkillData(Collections.nCopies(Skill.values().length, 1), Collections.nCopies(Skill.values().length, 0L), 0L, 0);
    }

    public int level(Skill s) { return s.ordinal() < levels.size() ? levels.get(s.ordinal()) : 1; }
    public long xp(Skill s) { return s.ordinal() < xp.size() ? xp.get(s.ordinal()) : 0L; }

    public int totalPower() {
        int t = 0;
        for (Skill s : Skill.values()) t += level(s);
        return t;
    }

    public SkillData with(Skill s, int level, long xpValue) {
        List<Integer> l = new ArrayList<>(levels);
        List<Long> x = new ArrayList<>(xp);
        while (l.size() < Skill.values().length) l.add(1);
        while (x.size() < Skill.values().length) x.add(0L);
        l.set(s.ordinal(), level);
        x.set(s.ordinal(), xpValue);
        return new SkillData(List.copyOf(l), List.copyOf(x), restDay, restTicks);
    }

    public SkillData withRest(long day, int ticks) { return new SkillData(levels, xp, day, ticks); }
}
