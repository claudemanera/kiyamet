package com.kiyamet.twd;

import com.kiyamet.Kiyamet;
import com.kiyamet.entity.KZombie;
import com.kiyamet.npc.Communities;
import com.kiyamet.npc.Faction;
import com.kiyamet.npc.HumanEntity;
import com.kiyamet.registry.ModItems;
import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mission board next to every settlement leader: 3 jobs a day per player (kill walkers, kill raiders, bring supplies).
 * Rewards: rubies, XP and reputation with that settlement.
 */
public class MissionBoard extends PathfinderMob implements GeoEntity {
    /** "day|type,item,need,have,reward,done|..." */
    public static final AttachmentType<String> MISSIONS = AttachmentRegistry.create(Kiyamet.id("missions"),
            b -> b.initializer(() -> "").persistent(Codec.STRING).copyOnDeath());
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    private static final String[][] BRING = {{"minecraft:bread", "16"}, {"minecraft:arrow", "32"}, {"minecraft:iron_ingot", "12"}, {"minecraft:leather", "10"},
            {"minecraft:cooked_beef", "12"}, {"kiyamet:bandage", "4"}, {"minecraft:wheat", "32"}, {"minecraft:oak_log", "32"}, {"kiyamet:walker_guts", "6"}, {"minecraft:coal", "16"}};
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public String community = "";

    public MissionBoard(EntityType<? extends MissionBoard> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> {
            if (!(src.getEntity() instanceof ServerPlayer p)) return;
            int type = e instanceof KZombie ? 0 : e instanceof HumanEntity h && (h.faction() == Faction.RAIDER || h.faction() == Faction.WHISPERER) ? 1 : -1;
            if (type < 0) return;
            List<int[]> ms = load(p);
            boolean changed = false;
            for (int[] m : ms) if (m[0] == type && m[5] == 0 && m[3] < m[2]) {
                m[3]++; changed = true;
                if (m[3] == m[2]) p.displayClientMessage(Component.literal("Mission complete! Turn it in at a mission board.").withStyle(ChatFormatting.GREEN), true);
            }
            if (changed) save(p, ms);
        });
    }

    private static int day(ServerPlayer p) { return (int) (p.level().getServer().overworld().getDayTime() / 24000); }

    /** Today's 3 missions (rolled once per day). */
    static List<int[]> load(ServerPlayer p) {
        String s = p.getAttachedOrElse(MISSIONS, "");
        int today = day(p);
        List<int[]> out = new ArrayList<>();
        String[] parts = s.split("\\|");
        if (parts.length > 1 && parts[0].equals(String.valueOf(today))) {
            for (int i = 1; i < parts.length; i++) {
                String[] f = parts[i].split(",");
                int[] m = new int[6];
                for (int j = 0; j < 6 && j < f.length; j++) m[j] = Integer.parseInt(f[j]);
                out.add(m);
            }
            return out;
        }
        Random r = new Random(p.getUUID().getLeastSignificantBits() ^ today * 31L);
        int power = SkillManager.get(p).totalPower();
        int walkers = 15 + r.nextInt(25) + power / 200;
        out.add(new int[]{0, 0, walkers, 0, 3 + walkers / 6, 0});
        int raiders = 2 + r.nextInt(5) + power / 1500;
        out.add(new int[]{power >= 300 ? 1 : 0, 0, power >= 300 ? raiders : walkers + 10, 0, power >= 300 ? raiders * 3 : 5 + walkers / 6, 0});
        int item = r.nextInt(BRING.length);
        out.add(new int[]{2, item, Integer.parseInt(BRING[item][1]), 0, 4 + r.nextInt(6), 0});
        save(p, out);
        return out;
    }

    static void save(ServerPlayer p, List<int[]> ms) {
        StringBuilder sb = new StringBuilder().append(day(p));
        for (int[] m : ms) { sb.append('|'); for (int j = 0; j < 6; j++) { if (j > 0) sb.append(','); sb.append(m[j]); } }
        p.setAttached(MISSIONS, sb.toString());
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
            String key = community;
            sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Menu(id, inv, sp, key), Component.literal("Mission Board")));
        }
        return InteractionResult.SUCCESS;
    }

    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return source.getEntity() instanceof Player p && p.isCreative() && super.hurtServer(level, source, amount);
    }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected boolean shouldDropLoot(ServerLevel level) { return false; }
    @Override public boolean canBeLeashed() { return false; }

    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putString("Community", community); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); community = in.getStringOr("Community", ""); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<MissionBoard>("main", 0, t -> t.setAndContinue(IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    static class Menu extends PanelMenu {
        private final String community;
        private static final int[] SLOTS = {20, 22, 24};

        Menu(int id, Inventory inv, ServerPlayer p, String community) { super(id, inv, p); this.community = community; redraw(); }

        @Override
        protected void draw() {
            List<int[]> ms = load(player);
            c.setItem(4, Ui.glint(Ui.icon(Items.WRITABLE_BOOK, Ui.text("Mission Board", ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Ui.text("3 new jobs every day", ChatFormatting.GRAY), Ui.text("Rewards: rubies, XP" + (community.isEmpty() ? "" : ", reputation"), ChatFormatting.GRAY)), true));
            for (int i = 0; i < ms.size() && i < 3; i++) {
                int[] m = ms.get(i);
                boolean done = m[5] == 1;
                String title; Item icon;
                if (m[0] == 0) { title = "Kill " + m[2] + " walkers"; icon = Items.ROTTEN_FLESH; }
                else if (m[0] == 1) { title = "Kill " + m[2] + " raiders"; icon = Items.CROSSBOW; }
                else { Item it = Ui.item(BRING[m[1]][0]); title = "Bring " + m[2] + "× " + new ItemStack(it).getHoverName().getString(); icon = it; }
                int have = m[0] == 2 ? Math.min(m[2], player.getInventory().countItem(Ui.item(BRING[m[1]][0]))) : m[3];
                boolean ready = !done && have >= m[2];
                c.setItem(SLOTS[i], Ui.glint(Ui.icon(done ? Items.LIME_DYE : icon, Ui.text(title, done ? ChatFormatting.DARK_GREEN : ready ? ChatFormatting.GREEN : ChatFormatting.WHITE, ChatFormatting.BOLD),
                        Ui.bar(have / (double) m[2], 20), Ui.text(have + " / " + m[2], ChatFormatting.GRAY),
                        Ui.text("Reward: " + m[4] + " rubies + XP", ChatFormatting.GOLD),
                        Ui.text(done ? "✔ Done for today" : ready ? "Click to turn in!" : "In progress", done ? ChatFormatting.DARK_GREEN : ready ? ChatFormatting.YELLOW : ChatFormatting.GRAY)), ready));
            }
            c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
            int ch = Story.chapter(player), st = Story.stage(player);
            if (ch <= Story.CHAPTERS.length) {
                StoryChapters.Chapter sc = Story.CHAPTERS[ch - 1];
                c.setItem(40, Ui.glint(Ui.icon(Items.FILLED_MAP, Ui.text("★ STORY - Chapter " + ch + "/" + Story.CHAPTERS.length + ": " + sc.title(), ChatFormatting.GOLD, ChatFormatting.BOLD),
                        Ui.text("\"" + sc.tagline() + "\"", ChatFormatting.GRAY), Ui.text("The Long Road", ChatFormatting.DARK_GRAY),
                        Ui.text("Reward: " + Story.reward(ch) + " rubies + XP", ChatFormatting.GREEN),
                        Ui.text(st > 0 ? "In progress - follow the arrow on screen" : "Click: talk to Marcus", ChatFormatting.YELLOW)), st == 0));
            } else c.setItem(40, Ui.icon(Items.MAP, Ui.text("★ The Long Road - complete", ChatFormatting.GOLD), Ui.text("Thank you, hero.", ChatFormatting.GRAY)));
        }

        @Override
        protected boolean click(int slot, ClickType type) {
            if (slot == 49) { player.closeContainer(); return false; }
            if (slot == 40) {
                player.closeContainer();
                for (MissionBoard b : player.level().getEntitiesOfClass(MissionBoard.class, player.getBoundingBox().inflate(10))) { Story.offer(player, b); break; }
                return false;
            }
            for (int i = 0; i < 3; i++) if (SLOTS[i] == slot) {
                List<int[]> ms = load(player);
                int[] m = ms.get(i);
                if (m[5] == 1) return false;
                if (m[0] == 2) {
                    Item it = Ui.item(BRING[m[1]][0]);
                    if (player.getInventory().countItem(it) < m[2]) return false;
                    int left = m[2];
                    for (int s = 0; s < player.getInventory().getContainerSize() && left > 0; s++) {
                        ItemStack st = player.getInventory().getItem(s);
                        if (st.is(it)) { int take = Math.min(left, st.getCount()); st.shrink(take); left -= take; }
                    }
                } else if (m[3] < m[2]) return false;
                m[5] = 1;
                save(player, ms);
                ItemStack reward = new ItemStack(ModItems.get("ruby"), m[4]);
                if (!player.getInventory().add(reward)) player.drop(reward, false);
                SkillManager.addXp(player, m[0] == 2 ? Skill.SURVIVAL : Skill.SWORDS, 60 + m[4] * 8);
                if (!community.isEmpty()) Communities.change(player, community, 5);
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.2f);
                for (HumanEntity h : player.level().getEntitiesOfClass(HumanEntity.class, player.getBoundingBox().inflate(16), h -> h.faction() == Faction.COMMUNITY)) {
                    String[] thanks = {"Thank you. That'll keep us going another week.", "You did good out there.", "We won't forget this.", "Here, you earned it.", "Fewer of them, more of us. Good work."};
                    com.kiyamet.npc.Talk.say(player, h, thanks[player.getRandom().nextInt(thanks.length)]);
                    break;
                }
                player.sendSystemMessage(Component.literal("Mission done: +" + m[4] + " rubies" + (community.isEmpty() ? "" : ", +5 reputation")).withStyle(ChatFormatting.GREEN));
                return true;
            }
            return false;
        }
    }
}
