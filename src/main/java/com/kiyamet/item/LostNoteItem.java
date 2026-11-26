package com.kiyamet.item;

import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Notes left by people from before and during the fall. Reading one for the first time gives Exploration XP. */
public class LostNoteItem extends Item {
    public static final String[][] NOTES = {
            {"Day 3", "They said stay inside. The radio said help is coming. The radio stopped saying anything this morning. - M."},
            {"To whoever finds this", "The pharmacy on the corner still has bandages in the back room. The front is full of them. Go around."},
            {"Mom", "I waited at the school for two days. If you read this, I went north to the farm. Please come. - Lily"},
            {"Rule #1", "Never shoot unless you have to. The noise brings more of them. Use a knife. Aim for the head."},
            {"Ration log", "4 cans of beans, 1 bag of rice, 2 bottles. Dave took more than his share again. Something has to be done."},
            {"Warning", "DO NOT TRUST THE PEOPLE AT THE WAREHOUSE. They take your stuff and leave you outside the fence at night."},
            {"Hospital, floor 2", "We locked the doors. Dr. Reyes says it's in everyone already. You don't need a bite. You just need to die."},
            {"Map scribble", "Fuel at the truck stop off the highway. Raiders watch it from the water tower. Go at dawn."},
            {"Last entry", "I got bit on the arm. I'm writing this so nobody opens the cellar. Sorry. Tell Sam I tried."},
            {"Kid's drawing", "A drawing of a house, a dog and four people. Someone wrote under it: 'we will go home soon'."},
            {"Scout report", "Herd moving east along the river. Hundreds. They follow the loudest thing. Don't be the loudest thing."},
            {"Church sign", "All are welcome. Leave your weapons at the door. Ring the bell twice. Not three times. Never three."},
            {"Mechanic's note", "The pickup runs. Just needs a canister of fuel and someone stupid enough to drive it through town."},
            {"Soldier", "Command stopped answering on day 9. We're holding the bridge until the ammo runs out. It's running out."},
            {"Masked ones", "They wear the dead. They walk with them. If the herd goes quiet and parts around you, it's not the dead you should fear."},
            {"Trade terms", "One medkit = five rubies. One gun = we talk. Your people = not for sale. Sign here."},
            {"Farmer's diary", "Planted the last seeds today. If it rains, we eat in the fall. If it doesn't... we'll see who's left."},
            {"Prison note", "Cell block C is clear. We have walls and a yard. It isn't freedom, but it's a home now."},
            {"Love letter", "If the world ends tomorrow I'm still glad I met you in that grocery store, fighting over the last can of peaches."},
            {"Blood Moon", "The old man said when the moon turns red, the dead don't sleep. We laughed. We're not laughing now."},
            {"Engineer", "Built a generator from a lawn mower and two car batteries. The fence hums now. They stay away from the hum."},
            {"Commander's order", "Every settlement pays or burns. No exceptions. - The Commander"},
            {"Unsent text", "where are you. the bridge is closed. im at the gas station. please answer. please"},
            {"Doctor's warning", "Fever, then sleep, then they wake up. Keep the infected tied before they sleep. Vaccine works if early."},
            {"Kingdom notice", "Fair at noon. Bring what you can spare. Those who share, eat. Those who hoard, walk."},
    };

    public LostNoteItem(Properties p) { super(p); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        CustomData data = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int id = data.copyTag().getIntOr("Note", -1);
        boolean first = id < 0;
        if (first) {
            id = sp.getRandom().nextInt(NOTES.length);
            CompoundTag tag = data.copyTag();
            tag.putInt("Note", id);
            s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            s.set(DataComponents.CUSTOM_NAME, Component.literal("Lost Note: " + NOTES[id][0]).withStyle(st -> st.withItalic(false).withColor(ChatFormatting.YELLOW)));
            List<Component> lore = new ArrayList<>();
            for (String line : wrap(NOTES[id][1], 34)) lore.add(Component.literal(line).withStyle(st -> st.withItalic(true).withColor(ChatFormatting.GRAY)));
            s.set(DataComponents.LORE, new ItemLore(lore));
            SkillManager.addXp(sp, Skill.EXPLORATION, 40);
        }
        sp.sendSystemMessage(Component.literal("✉ " + NOTES[id][0]).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        sp.sendSystemMessage(Component.literal("\"" + NOTES[id][1] + "\"").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (first) sp.sendSystemMessage(Component.literal("+40 Exploration XP").withStyle(ChatFormatting.GREEN));
        level.playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        return InteractionResult.SUCCESS;
    }

    static List<String> wrap(String s, int w) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            if (line.length() + word.length() + 1 > w && line.length() > 0) { out.add(line.toString()); line.setLength(0); }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }
}
