package com.kiyamet.skill;

import com.kiyamet.Kiyamet;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
    public static final AttachmentType<SkillData> SKILLS = AttachmentRegistry.create(Kiyamet.id("skills"),
            builder -> builder.initializer(SkillData::empty).persistent(SkillData.CODEC).copyOnDeath());

    public static void init() {}
    private ModAttachments() {}
}
