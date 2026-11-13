package games.brennan.dungeontotems.gametest;

import com.google.gson.JsonParser;
import net.minecraft.server.packs.resources.Resource;
import games.brennan.dungeontotems.advancement.TotemAdvancements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementHolder;
import games.brennan.dungeontotems.curse.DeadlineCurses;
import games.brennan.dungeontotems.curse.CurseTrials;
import games.brennan.dungeontotems.advancement.TabComplete;
import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.api.DungeonTotemsApi;
import games.brennan.dungeontotems.curse.CurseMemory;
import games.brennan.dungeontotems.compat.PlayerMobGifts;
import games.brennan.dungeontotems.curse.Curses;
import games.brennan.dungeontotems.curse.CurseTimers;
import games.brennan.dungeontotems.curse.HeldCurses;
import games.brennan.dungeontotems.curse.InsomniaPhantoms;
import games.brennan.dungeontotems.loot.EvokerTotemLootModifier;
import games.brennan.dungeontotems.morph.FormAir;
import games.brennan.dungeontotems.morph.FormAttacks;
import games.brennan.dungeontotems.morph.FormTraits;
import games.brennan.dungeontotems.morph.FormUse;
import games.brennan.dungeontotems.morph.MorphForm;
import games.brennan.dungeontotems.morph.Morphs;
import games.brennan.dungeontotems.morph.WaterLife;
import games.brennan.dungeontotems.morph.WaterMoves;
import games.brennan.dungeontotems.registry.TotemRegistries;
import games.brennan.dungeontotems.voidtotem.VoidRescue;
import net.minecraft.core.registries.BuiltInRegistries;
import games.brennan.dungeontotems.morph.MobGaits;
import games.brennan.dungeontotems.morph.MorphFlight;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side behaviour of every totem, against a mock player in a real level.
 * Run with {@code ./gradlew runGameTestServer}; never runs in a normal game.
 */
@GameTestHolder(DungeonTotems.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DungeonTotemsGameTests {

    private static final String EMPTY = "empty";
    private static final float LETHAL = 1000.0F;
    private static final int SPAWN_INVULNERABLE_TICKS = 61;
    private static final int LOOT_ROLLS = 50;

    private DungeonTotemsGameTests() {}

    // ---- the pop ----

    @GameTest(template = EMPTY)
    public static void curseTotemSavesAndCurses(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.WEAKNESS);
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, player.isAlive(), "player should survive");
        check(helper, player.getMainHandItem().isEmpty(), "totem should be used up");
        check(helper, player.hasEffect(MobEffects.REGENERATION), "vanilla regeneration");
        check(helper, player.hasEffect(MobEffects.ABSORPTION), "vanilla absorption");
        check(helper, player.hasEffect(MobEffects.FIRE_RESISTANCE), "a curse totem still gives Fire Resistance");
        expectEffect(helper, player, MobEffects.WEAKNESS, Curse.WEAKNESS.afterSaveTicks());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void everyCurseTotemAppliesItsAfterSaveCurse(GameTestHelper helper) {
        for (Curse curse : Curse.values()) {
            if (curse.effect() == null) continue;
            ServerPlayer player = holding(helper, curse);
            player.hurt(player.damageSources().generic(), LETHAL);
            check(helper, player.isAlive(), curse + ": player should survive");
            // a morph lasts the time of the form it landed on (morphLastsItsFormsTime)
            int ticks = curse == Curse.MORPHING
                ? Morphs.formOf(player).flatMap(Morphs::formData).map(MorphForm::ticks).orElse(curse.afterSaveTicks())
                : curse.afterSaveTicks();
            expectEffect(helper, player, curse.effect(), ticks);
            MobEffectInstance instance = player.getEffect(curse.effect());
            check(helper, instance.getAmplifier() == curse.afterSaveAmplifier(), curse + " level " + instance.getAmplifier());
            check(helper, Curses.isSaved(player, curse), curse + " should be shielded after the save");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void afterSaveTimesMatchTheTable(GameTestHelper helper) {
        check(helper, Curse.MELON_DIET.afterSaveTicks() == 10 * 60 * 20, "melon diet lasts 10 minutes");
        check(helper, Curse.RAGE.afterSaveTicks() == 5 * 60 * 20 && Curse.RAGE.afterSaveAmplifier() == 1, "rage II for 5 minutes");
        check(helper, Curse.LONELINESS.letGoTicks() == 60 * 20, "lonely I for 1 minute after letting go");
        for (Curse curse : List.of(Curse.WEAKNESS, Curse.FATIGUE, Curse.HEARTBREAK, Curse.HUNGER, Curse.SLOWNESS,
                Curse.PACIFISM, Curse.RESTRAINT, Curse.BREATHLESSNESS, Curse.INSOMNIA)) {
            check(helper, curse.afterSaveTicks() == 4 * 60 * 20, curse + " lasts 4 minutes after a save");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void vanillaTotemIsUsedFirst(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.WEAKNESS);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, player.isAlive(), "player should survive");
        check(helper, player.getOffhandItem().isEmpty(), "vanilla totem should be used");
        check(helper, !player.getMainHandItem().isEmpty(), "ours should be kept");
        check(helper, !Curses.isSaved(player, Curse.WEAKNESS), "no after-save curse from a vanilla pop");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void killCommandDamageIsNotSaved(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.WEAKNESS);
        player.hurt(player.damageSources().genericKill(), LETHAL);
        check(helper, !player.isAlive(), "/kill damage bypasses totems, like vanilla");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void heartbreakTakesThreeHearts(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.HEARTBREAK);
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, player.getMaxHealth() == 14.0F, "max health should be 14, was " + player.getMaxHealth());
        helper.succeed();
    }

    // ---- held curses ----

    @GameTest(template = EMPTY)
    public static void heldTotemsCurseOnlyWhileHeld(GameTestHelper helper) {
        for (Curse curse : Curse.values()) {
            if (!curse.isHeld()) continue;
            ServerPlayer player = holding(helper, curse);
            tick(player, HeldCurses.CHECK_TICKS);
            MobEffectInstance held = player.getEffect(curse.effect());
            check(helper, held != null && held.isInfiniteDuration() && held.getAmplifier() == curse.heldAmplifier(),
                curse + " should be on while held, was " + held);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            tick(player, HeldCurses.CHECK_TICKS);
            MobEffectInstance after = player.getEffect(curse.effect());
            if (curse.letGoTicks() > 0) {
                check(helper, after != null && !after.isInfiniteDuration() && after.getAmplifier() == curse.heldAmplifier()
                    && after.getDuration() > curse.letGoTicks() - HeldCurses.CHECK_TICKS, curse + " should leave its let-go curse, was " + after);
            } else {
                check(helper, after == null, curse + " should end when let go, was " + after);
            }
        }
        helper.succeed();
    }

    // ---- stacks ----

    @GameTest(template = EMPTY)
    public static void totemsStackAsHighAsTheirLevels(GameTestHelper helper) {
        for (Curse curse : Curse.values()) {
            int max = new ItemStack(TotemRegistries.TOTEMS.get(curse).get()).getMaxStackSize();
            check(helper, max == curse.maxStack(), curse + " stacks to " + max + ", expected " + curse.maxStack());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aBiggerStackIsAStrongerCurse(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.SLOWNESS, 3);
        tick(player, HeldCurses.CHECK_TICKS);
        MobEffectInstance three = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        check(helper, three != null && three.isInfiniteDuration() && three.getAmplifier() == 2, "a stack of 3 should be Slowness III, was " + three);
        player.getMainHandItem().shrink(1);
        tick(player, HeldCurses.CHECK_TICKS);
        MobEffectInstance two = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        check(helper, two != null && two.isInfiniteDuration() && two.getAmplifier() == 1, "one fewer should be Slowness II, was " + two);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aStackSavesOnceAndCursesAtItsLevel(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.RAGE, 2);
        tick(player, HeldCurses.CHECK_TICKS);
        check(helper, player.getEffect(TotemRegistries.RAGE).getAmplifier() == 1, "a stack of 2 should be Rage II while held");
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, player.isAlive() && player.getMainHandItem().getCount() == 1, "the pop should use one of the two");
        MobEffectInstance saved = player.getEffect(TotemRegistries.RAGE);
        check(helper, saved != null && saved.getAmplifier() == 2 && !saved.isInfiniteDuration(), "two totems' save should be Rage III, was " + saved);
        tick(player, HeldCurses.CHECK_TICKS);
        check(helper, player.getEffect(TotemRegistries.RAGE).getAmplifier() == 2, "the stronger after-save curse should run out first");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void lettingGoOfAStackKeepsItsLevel(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.LONELINESS, 3);
        tick(player, HeldCurses.CHECK_TICKS);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        tick(player, HeldCurses.CHECK_TICKS);
        MobEffectInstance after = player.getEffect(TotemRegistries.LONELY);
        check(helper, after != null && !after.isInfiniteDuration() && after.getAmplifier() == 2, "letting go of 3 should leave Lonely III, was " + after);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void offHandCountsAsHeld(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(TotemRegistries.TOTEMS.get(Curse.SLOWNESS).get()));
        tick(player, HeldCurses.CHECK_TICKS);
        check(helper, player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "an off-hand totem curses too");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fatigueIsMiningFatigueOne(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.FATIGUE);
        tick(player, HeldCurses.CHECK_TICKS);
        MobEffectInstance fatigue = player.getEffect(MobEffects.DIG_SLOWDOWN);
        check(helper, fatigue != null && fatigue.getAmplifier() == 0, "fatigue should be Mining Fatigue I, was " + fatigue);
        helper.succeed();
    }

    // ---- rule totems ----

    @GameTest(template = EMPTY)
    public static void hurtingAnythingPopsAHeldPacifismTotem(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.PACIFISM);
        Pig pig = helper.spawn(EntityType.PIG, 2, 1, 2);
        pig.hurt(player.damageSources().playerAttack(player), 1.0F);
        tick(player, 2);
        expectRulePop(helper, player, Curse.PACIFISM);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void eatingBreadPopsAHeldMelonTotem(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.MELON_DIET);
        EventHooks.onItemUseFinish(player, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY);
        tick(player, 2);
        expectRulePop(helper, player, Curse.MELON_DIET);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void openingABarrelPopsAHeldRestraintTotem(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.RESTRAINT);
        openBarrel(helper, player);
        tick(player, 2);
        expectRulePop(helper, player, Curse.RESTRAINT);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void brokenRuleAfterASaveLeavesHalfAHeart(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.RESTRAINT);
        tick(player, 20);
        player.setHealth(20.0F);
        player.invulnerableTime = 0; // the pop's own hit would otherwise swallow the doom
        openBarrel(helper, player);
        tick(player, 2);
        check(helper, player.isAlive(), "a curse can't kill you after a save");
        check(helper, player.getHealth() == 1.0F, "should be on half a heart, was " + player.getHealth());
        MobEffectInstance restrained = player.getEffect(TotemRegistries.RESTRAINED);
        check(helper, restrained != null && restrained.getDuration() >= Curse.RESTRAINT.afterSaveTicks() - 2,
            "half a heart should restart Restrained, it has " + restrained);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void foodOffTheMelonDietDoesNotFillYou(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.MELON_DIET);
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(1.0F);
        ItemStack bread = new ItemStack(Items.BREAD);
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Start(player, bread, 32));
        player.getFoodData().eat(5, 6.0F); // what eating the bread did
        EventHooks.onItemUseFinish(player, bread, 0, ItemStack.EMPTY);
        check(helper, player.getFoodData().getFoodLevel() == 10, "bread should not fill you, food " + player.getFoodData().getFoodLevel());
        check(helper, player.getMainHandItem().isEmpty(), "and the totem still pops");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void brokenRuleWithoutAShieldKills(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.MELON_DIET, 0, 400);
        EventHooks.onItemUseFinish(player, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY);
        tick(player, 2);
        check(helper, !player.isAlive(), "a melon diet with no totem and no save still kills");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void melonAndDrinksKeepTheMelonDiet(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.MELON_DIET, 0, 400);
        EventHooks.onItemUseFinish(player, new ItemStack(Items.MELON_SLICE), 0, ItemStack.EMPTY);
        EventHooks.onItemUseFinish(player, new ItemStack(Items.HONEY_BOTTLE), 0, ItemStack.EMPTY);
        EventHooks.onItemUseFinish(player, new ItemStack(Items.MILK_BUCKET), 0, ItemStack.EMPTY);
        tick(player, 2);
        check(helper, player.isAlive(), "melon and drinks are allowed");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void takingDamageKeepsTheVow(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.PACIFISM);
        Pig pig = helper.spawn(EntityType.PIG, 2, 1, 2);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(pig), 1.0F);
        tick(player, 2);
        check(helper, !player.getMainHandItem().isEmpty(), "being hurt does not break the vow");
        helper.succeed();
    }

    // ---- rage and lonely timers ----

    @GameTest(template = EMPTY)
    public static void timersShortenWithLevel(GameTestHelper helper) {
        check(helper, CurseTimers.seconds(0) == 60 && CurseTimers.seconds(1) == 40 && CurseTimers.seconds(2) == 30,
            "60, 40 and 30 seconds");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void rageHurtsOnceTheTimerRunsOut(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.RAGE, 0, 400);
        overdue(player, TotemRegistries.LAST_HIT, 0);
        tick(player, 1);
        check(helper, player.getHealth() < 20.0F, "rage should hurt once the timer runs out");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aHitStopsRage(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.RAGE, 0, 400);
        overdue(player, TotemRegistries.LAST_HIT, 0);
        Pig pig = helper.spawn(EntityType.PIG, 2, 1, 2);
        pig.hurt(player.damageSources().playerAttack(player), 1.0F);
        tick(player, 2);
        check(helper, player.getHealth() == 20.0F, "a hit should reset the timer, health " + player.getHealth());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void rageAfterASaveCantKill(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.RAGE);
        player.removeAllEffects();
        Curses.apply(player, Curse.RAGE, 1, 400);
        player.setHealth(1.0F);
        overdue(player, TotemRegistries.LAST_HIT, 1);
        player.invulnerableTime = 0;
        tick(player, 1);
        check(helper, player.isAlive() && player.getHealth() == 1.0F, "rage after a save stops at half a heart");
        MobEffectInstance rage = player.getEffect(TotemRegistries.RAGE);
        check(helper, rage != null && rage.getAmplifier() == 1 && rage.getDuration() >= Curse.RAGE.afterSaveTicks() - 2,
            "half a heart should restart Rage II, it has " + rage);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void heldRageTotemSavesYouFromRage(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.RAGE);
        tick(player, HeldCurses.CHECK_TICKS);
        player.setHealth(1.0F);
        player.invulnerableTime = 0;
        overdue(player, TotemRegistries.LAST_HIT, 0);
        tick(player, 1);
        check(helper, player.isAlive(), "the held totem should save you from its own rage");
        check(helper, player.getMainHandItem().isEmpty(), "and be used up");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aTradeIsAGiftForLonely(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.LONELINESS, 0, 400);
        overdue(player, TotemRegistries.LAST_GIFT, 0);
        trade(player, villager(helper));
        check(helper, player.getData(TotemRegistries.LAST_GIFT) == player.level().getGameTime(), "a trade should restart the timer");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aPlayerMobGiftIsAGiftForLonely(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.LONELINESS, 0, 400);
        overdue(player, TotemRegistries.LAST_GIFT, 0);
        PlayerMobGifts.gift(player, UUID.randomUUID(), false);
        check(helper, player.getData(TotemRegistries.LAST_GIFT) != player.level().getGameTime(), "a gift to you is not yours");
        PlayerMobGifts.gift(player, UUID.randomUUID(), true);
        check(helper, player.getData(TotemRegistries.LAST_GIFT) == player.level().getGameTime(), "your gift restarts the timer");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void lonelyHurtsOnceTheTimerRunsOut(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.LONELINESS, 0, 400);
        overdue(player, TotemRegistries.LAST_GIFT, 0);
        tick(player, 1);
        check(helper, player.getHealth() < 20.0F, "lonely should hurt once the timer runs out");
        helper.succeed();
    }

    // ---- breathless ----

    @GameTest(template = EMPTY)
    public static void breathlessDrainsAirOnLand(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.BREATHLESSNESS, 0, 400);
        tick(player, 5);
        check(helper, player.getAirSupply() == player.getMaxAirSupply() - 5, "air should drain by one a tick, was " + player.getAirSupply());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void waterBreathingStopsBreathless(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.BREATHLESSNESS, 0, 400);
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400));
        tick(player, 5);
        check(helper, player.getAirSupply() == player.getMaxAirSupply(), "water breathing keeps your air");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void breathlessDrownsYouOnLand(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.BREATHLESSNESS, 0, 400);
        player.setAirSupply(-19);
        tick(player, 1);
        check(helper, player.getHealth() == 18.0F, "drowning on land should hurt 2, health " + player.getHealth());
        helper.succeed();
    }

    // ---- fire ----

    @GameTest(template = EMPTY)
    public static void fireTotemSavesFromFireAndLava(GameTestHelper helper) {
        ServerPlayer burnt = holding(helper, Curse.FIRE);
        burnt.hurt(burnt.damageSources().onFire(), LETHAL);
        check(helper, burnt.isAlive(), "fire totem should save from burning");
        check(helper, burnt.hasEffect(MobEffects.FIRE_RESISTANCE), "and give Fire Resistance");
        ServerPlayer swimmer = holding(helper, Curse.FIRE);
        swimmer.hurt(swimmer.damageSources().lava(), LETHAL);
        check(helper, swimmer.isAlive(), "fire totem should save from lava");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fireTotemIgnoresOtherDeaths(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.FIRE);
        player.hurt(player.damageSources().fall(), LETHAL);
        check(helper, !player.isAlive(), "only fire and lava are saved");
        helper.succeed();
    }

    // ---- insomnia ----

    @GameTest(template = EMPTY)
    public static void dayPhantomsComeAQuarterOfTheTime(GameTestHelper helper) {
        check(helper, InsomniaPhantoms.DAY_CHANCE == 0.25F && InsomniaPhantoms.NIGHT_CHANCE == 1.0F, "25% by day, 100% at night");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void nightTryAlwaysSpawnsPhantoms(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var level = helper.getLevel();
        long time = level.getDayTime();
        level.setDayTime(18000);
        level.updateSkyBrightness();
        try {
            int spawned = 0;
            for (int i = 0; i < 5 && spawned == 0; i++) {
                spawned = InsomniaPhantoms.trySpawnAt(level, player.blockPosition(), level.getRandom());
            }
            check(helper, spawned > 0, "a night try should spawn phantoms, even below sea level in a flat world");
        } finally {
            level.setDayTime(time);
            level.updateSkyBrightness();
            level.getEntitiesOfClass(Phantom.class, player.getBoundingBox().inflate(64)).forEach(Phantom::discard);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dayPhantomsResistFire(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(2, 3, 2));
        var level = helper.getLevel();
        int spawned = InsomniaPhantoms.spawnGroup(level, at, level.getCurrentDifficultyAt(at), true, level.getRandom());
        List<Phantom> phantoms = level.getEntitiesOfClass(Phantom.class, new net.minecraft.world.phys.AABB(at).inflate(3));
        check(helper, spawned > 0 && phantoms.size() == spawned, "spawned " + spawned + ", found " + phantoms.size());
        check(helper, phantoms.stream().allMatch(p -> p.hasEffect(MobEffects.FIRE_RESISTANCE)), "day phantoms need Fire Resistance");
        phantoms.forEach(Phantom::discard);
        helper.succeed();
    }

    // ---- void ----

    @GameTest(template = EMPTY)
    public static void voidTotemIgnoresOrdinaryDeaths(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.VOID);
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, !player.isAlive(), "void totem should not save from a generic hit");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void voidTotemHopsToTheRescueTarget(GameTestHelper helper) {
        Vec3 target = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        DungeonTotemsApi.setRescueTarget(p -> Optional.of(target));
        try {
            ServerPlayer player = holding(helper, Curse.VOID);
            player.teleportTo(player.getX(), player.level().getMinBuildHeight() - 70, player.getZ());
            player.hurt(player.damageSources().fellOutOfWorld(), LETHAL);
            check(helper, player.isAlive(), "player should survive the void");
            check(helper, player.position().distanceTo(target) < 0.01, "player should be at the target, was " + player.position());
        } finally {
            DungeonTotemsApi.setRescueTarget(p -> Optional.empty());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void voidTotemHopsLikeAChorusFruitNearSafeGround(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.VOID);
        Vec3 safe = player.position();
        VoidRescue.rememberSafeSpot(player, safe);
        player.teleportTo(player.getX(), player.level().getMinBuildHeight() - 70, player.getZ());
        player.hurt(player.damageSources().fellOutOfWorld(), LETHAL);
        check(helper, player.isAlive(), "player should survive the void");
        double dx = player.getX() - safe.x;
        double dz = player.getZ() - safe.z;
        check(helper, Math.abs(dx) <= 8.5 && Math.abs(dz) <= 8.5, "hop should land near safe ground, was " + player.position());
        check(helper, player.getY() > player.level().getMinBuildHeight(), "hop should land in the world");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void voidTotemIgnoresLethalFalls(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.VOID);
        player.hurt(player.damageSources().fall(), LETHAL);
        check(helper, !player.isAlive(), "a lethal fall is not a void death");
        helper.succeed();
    }

    // ---- feather ----

    @GameTest(template = EMPTY)
    public static void featherTotemSavesFromLethalFalls(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.FEATHER);
        player.hurt(player.damageSources().fall(), LETHAL);
        check(helper, player.isAlive(), "player should survive the fall");
        check(helper, player.getMainHandItem().isEmpty(), "totem should be used up");
        check(helper, player.getActiveEffects().stream().noneMatch(e -> Curses.curseOf(e) != null), "no curse from a feather");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void featherTotemIgnoresOtherDeaths(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.FEATHER);
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, !player.isAlive(), "only a fall is saved");
        helper.succeed();
    }

    // ---- boom ----

    @GameTest(template = EMPTY)
    public static void boomTotemSavesFromExplosions(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.BOOM);
        player.hurt(player.damageSources().explosion(null, null), LETHAL);
        check(helper, player.isAlive(), "player should survive the explosion");
        check(helper, !player.hasEffect(MobEffects.FIRE_RESISTANCE), "a single-purpose totem gives no Fire Resistance");
        check(helper, player.hasEffect(MobEffects.REGENERATION), "the pop still heals");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void boomTotemIgnoresOtherDeaths(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.BOOM);
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, !player.isAlive(), "only an explosion is saved");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void featherTotemGivesNoFireResistance(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.FEATHER);
        player.hurt(player.damageSources().fall(), LETHAL);
        check(helper, player.isAlive(), "player should survive the fall");
        check(helper, !player.hasEffect(MobEffects.FIRE_RESISTANCE), "a single-purpose totem gives no Fire Resistance");
        helper.succeed();
    }

    // ---- morph ----

    @GameTest(template = EMPTY)
    public static void morphingTotemGivesAFittingForm(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.MORPHING);
        player.hurt(player.damageSources().generic(), LETHAL);
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        check(helper, form != null, "player should have a form");
        check(helper, Morphs.formData(form).filter(f -> f.pool() == MorphForm.Pool.LAND && f.weight() > 0).isPresent(),
            "form should come from the land pool: " + form);
        check(helper, player.getBbHeight() == form.getDimensions().height(), "hitbox should be the form's");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void morphLastsItsFormsTime(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.MORPHING);
        player.hurt(player.damageSources().generic(), LETHAL);
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        check(helper, form != null, "player should have a form");
        int expected = Morphs.formData(form).map(MorphForm::ticks).orElse(-1);
        MobEffectInstance morphed = player.getEffect(TotemRegistries.MORPHED);
        check(helper, morphed != null && morphed.getDuration() == expected,
            form + " should last " + expected + " ticks, was " + (morphed == null ? "none" : morphed.getDuration()));
        check(helper, Curses.isSaved(player, Curse.MORPHING), "the save should cover the form's time");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void formStatsComeAndGo(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.CHICKEN);
        check(helper, player.getMaxHealth() == 6.0F, "chicken max health floors at 6, was " + player.getMaxHealth());
        Morphs.setForm(player, null);
        check(helper, player.getMaxHealth() == 20.0F, "max health back to 20");
        check(helper, player.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().isEmpty(), "no speed modifier left");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void flyingFormsFlyAndStopWhenTheFormEnds(GameTestHelper helper) {
        for (EntityType<?> flyer : List.of(EntityType.BEE, EntityType.PARROT, EntityType.BAT)) {
            ServerPlayer player = player(helper);
            Morphs.setForm(player, flyer);
            check(helper, player.getAbilities().mayfly && player.getAbilities().flying, flyer + " form should fly");
            player.getAbilities().flying = false; // as vanilla does on touching the ground
            tick(player, 1);
            check(helper, player.getAbilities().flying, flyer + " form should take off again at once");
            Morphs.setForm(player, null);
            check(helper, !player.getAbilities().mayfly && !player.getAbilities().flying, "flight should end with the form");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void beeFormStingsOnceAndMayDie(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.BEE);
        Pig pig = helper.spawn(EntityType.PIG, 2, 1, 2);
        player.attack(pig);
        check(helper, pig.hasEffect(MobEffects.POISON), "a sting should poison");
        check(helper, FormAttacks.hasStung(player), "the bee has lost its stinger");
        Morphs.setForm(player, EntityType.COW);
        check(helper, !FormAttacks.hasStung(player), "turning back in time saves you");
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void formsWithoutAnAttackDoNoDamage(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.COW);
        Pig pig = helper.spawn(EntityType.PIG, 2, 1, 2);
        float before = pig.getHealth();
        player.attack(pig);
        check(helper, pig.getHealth() == before, "a cow can't hurt anything");
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void goatFormRams(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.GOAT);
        Pig pig = helper.spawn(EntityType.PIG, 2, 1, 3);
        player.attack(pig);
        double push = pig.getDeltaMovement().horizontalDistance();
        check(helper, push > 0.5, "a ram should knock the target far, was " + push);
        double charge = player.getDeltaMovement().horizontalDistance();
        check(helper, charge > 0.5 && !player.isSprinting(), "a goat should charge (and not be left sprinting) into the ram, moved " + charge);
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void sheepFormGrazes(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.SHEEP);
        BlockPos grass = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(grass, Blocks.GRASS_BLOCK.defaultBlockState());
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(player, grass, Direction.UP,
            PlayerInteractEvent.LeftClickBlock.Action.START));
        check(helper, helper.getLevel().getBlockState(grass).is(Blocks.DIRT), "a sheep should eat the grass");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dyingUnderwaterGivesAWaterForm(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.MORPHING);
        submerge(helper, player, Blocks.WATER);
        player.hurt(player.damageSources().generic(), LETHAL);
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        check(helper, FormTraits.isWaterForm(form), "an underwater pop should give a water form, gave " + form);
        check(helper, player.hasEffect(MobEffects.DOLPHINS_GRACE), "water forms swim fast");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dyingInLavaGivesALavaForm(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.MORPHING);
        submerge(helper, player, Blocks.LAVA);
        player.hurt(player.damageSources().generic(), LETHAL);
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        check(helper, form != null && Morphs.formData(form).map(MorphForm::pool).orElse(null) == MorphForm.Pool.LAVA,
            "a lava pop should give a lava form, gave " + form);
        check(helper, player.hasEffect(MobEffects.FIRE_RESISTANCE), "a lava form can't burn");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fishFormDriesOutOnLand(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.COD);
        tick(player, 150);
        check(helper, player.getAirSupply() < player.getMaxAirSupply(), "a fish on land runs out of air");
        tick(player, 200);
        DamageSource last = player.getLastDamageSource();
        check(helper, last != null && last.is(DamageTypes.DROWN), "a fish out of water too long takes damage, last was " + last);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void catFormTakesNoFallDamage(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.CAT);
        float before = player.getHealth();
        player.causeFallDamage(20.0F, 1.0F, player.damageSources().fall());
        check(helper, player.getHealth() == before, "a cat lands on its feet");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void camelCantJumpAndHorsesGallop(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.CAMEL);
        check(helper, player.getAttributeValue(Attributes.JUMP_STRENGTH) == 0.0, "camels can't jump");
        Morphs.setForm(player, EntityType.HORSE);
        double walking = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        player.setSprinting(true);
        tick(player, 1);
        check(helper, player.getAttributeValue(Attributes.MOVEMENT_SPEED) > walking * 1.6, "a sprinting horse should gallop");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void chickenLaysAndArmadilloRolls(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.CHICKEN);
        FormUse.use(player);
        check(helper, !helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2),
            item -> item.getItem().is(Items.EGG) && item.hasPickUpDelay()).isEmpty(), "a chicken should lay an egg you see before picking up");
        Morphs.setForm(player, EntityType.ARMADILLO);
        FormUse.use(player);
        check(helper, FormUse.isRolled(player) && player.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.0, "a rolled armadillo can't move");
        Morphs.setForm(player, EntityType.COW);
        check(helper, !FormUse.isRolled(player) && player.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.0, "changing form unrolls");
        helper.succeed();
    }

    /** How each form's attack should land, from its vanilla mob. */
    private enum Hit { NONE, MELEE, POISON, SHOT }

    private static final Map<EntityType<?>, Hit> FORM_HITS = Map.ofEntries(
        Map.entry(EntityType.ALLAY, Hit.NONE), Map.entry(EntityType.ARMADILLO, Hit.NONE), Map.entry(EntityType.BAT, Hit.NONE),
        Map.entry(EntityType.CAMEL, Hit.NONE), Map.entry(EntityType.CHICKEN, Hit.NONE), Map.entry(EntityType.COW, Hit.NONE),
        Map.entry(EntityType.DONKEY, Hit.NONE), Map.entry(EntityType.HORSE, Hit.NONE), Map.entry(EntityType.MULE, Hit.NONE),
        Map.entry(EntityType.MOOSHROOM, Hit.NONE), Map.entry(EntityType.PIG, Hit.NONE), Map.entry(EntityType.SHEEP, Hit.NONE),
        Map.entry(EntityType.SNIFFER, Hit.NONE), Map.entry(EntityType.STRIDER, Hit.NONE), Map.entry(EntityType.TURTLE, Hit.NONE),
        Map.entry(EntityType.SKELETON_HORSE, Hit.NONE), Map.entry(EntityType.ZOMBIE_HORSE, Hit.NONE), Map.entry(EntityType.PARROT, Hit.NONE),
        Map.entry(EntityType.RABBIT, Hit.NONE), Map.entry(EntityType.COD, Hit.NONE), Map.entry(EntityType.SALMON, Hit.NONE),
        Map.entry(EntityType.TROPICAL_FISH, Hit.NONE), Map.entry(EntityType.TADPOLE, Hit.NONE), Map.entry(EntityType.SQUID, Hit.NONE),
        Map.entry(EntityType.GLOW_SQUID, Hit.NONE),
        Map.entry(EntityType.CAT, Hit.MELEE), Map.entry(EntityType.OCELOT, Hit.MELEE), Map.entry(EntityType.FOX, Hit.MELEE),
        Map.entry(EntityType.WOLF, Hit.MELEE), Map.entry(EntityType.PANDA, Hit.MELEE), Map.entry(EntityType.DOLPHIN, Hit.MELEE),
        Map.entry(EntityType.AXOLOTL, Hit.MELEE), Map.entry(EntityType.POLAR_BEAR, Hit.MELEE), Map.entry(EntityType.ZOMBIFIED_PIGLIN, Hit.MELEE),
        Map.entry(EntityType.GOAT, Hit.MELEE), Map.entry(EntityType.IRON_GOLEM, Hit.MELEE), Map.entry(EntityType.FROG, Hit.MELEE),
        Map.entry(EntityType.BEE, Hit.MELEE),
        Map.entry(EntityType.PUFFERFISH, Hit.POISON),
        Map.entry(EntityType.LLAMA, Hit.SHOT), Map.entry(EntityType.TRADER_LLAMA, Hit.SHOT), Map.entry(EntityType.SNOW_GOLEM, Hit.SHOT));

    /** {@code AbstractHorse}'s best roll, and a camel's no-jump. */
    private static final Map<EntityType<?>, Double> FORM_JUMPS = Map.of(EntityType.HORSE, 1.0, EntityType.SKELETON_HORSE, 1.0,
        EntityType.ZOMBIE_HORSE, 1.0, EntityType.CAMEL, 0.0);

    /**
     * Every form in the data map: hitting a pig (living, so poison takes) lands as the mob's attack would (nothing, a melee hit,
     * poison, or a shot), its right-click move runs, horses jump their best, flyers fly, and a second of
     * life in the form goes by cleanly.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    @SuppressWarnings("unchecked")
    public static void everyFormFightsLikeItsMob(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        List<EntityType<?>> forms = BuiltInRegistries.ENTITY_TYPE.stream().filter(type -> Morphs.formData(type).isPresent()).toList();
        Set<EntityType<?>> unlisted = new HashSet<>(forms);
        unlisted.removeAll(FORM_HITS.keySet());
        check(helper, unlisted.isEmpty(), "forms with no expected attack: " + unlisted);
        List<String> wrong = new ArrayList<>();
        for (EntityType<?> form : forms) {
            String name = BuiltInRegistries.ENTITY_TYPE.getKey(form).getPath();
            try {
                Morphs.setForm(player, form);
                Vec3 start = helper.absoluteVec(new Vec3(2.5, 1, 2.5));
                player.teleportTo(start.x, start.y, start.z);
                player.setYRot(0.0F); // facing +z, at the target
                player.setXRot(0.0F);
                tick(player, 20); // a full attack charge
                Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, 2, 1, 4);
                int shotsBefore = shots(helper);
                float before = target.getHealth();
                player.attack(target);
                boolean hurt = target.getHealth() < before;
                boolean poisoned = target.hasEffect(MobEffects.POISON);
                boolean shot = shots(helper) > shotsBefore;
                Hit expected = FORM_HITS.get(form);
                Hit got = shot ? Hit.SHOT : poisoned && !hurt ? Hit.POISON : hurt ? Hit.MELEE : Hit.NONE;
                if (got != expected) wrong.add(name + " attacked as " + got + ", expected " + expected);
                target.discard();

                MobGaits.Gait gait = MobGaits.of(form);
                if (gait != null) {
                    double mobSpeed = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) form).getBaseValue(Attributes.MOVEMENT_SPEED);
                    double chill = form == EntityType.STRIDER ? FormTraits.STRIDER_COLD * FormTraits.STRIDER_COLD : 1.0; // on stone, it's cold
                    double walk = player.getAttributeValue(Attributes.MOVEMENT_SPEED) / chill;
                    player.setSprinting(true);
                    tick(player, 1);
                    double run = player.getAttributeValue(Attributes.MOVEMENT_SPEED) / chill;
                    player.setSprinting(false);
                    tick(player, 1);
                    if (Math.abs(walk - MobGaits.walkSpeed(gait, mobSpeed)) > 1.0E-6) wrong.add(name + " walks at " + walk + ", its mob at " + MobGaits.walkSpeed(gait, mobSpeed));
                    if (Math.abs(run - MobGaits.runSpeed(gait, mobSpeed)) > 1.0E-6) wrong.add(name + " runs at " + run + ", its mob at " + MobGaits.runSpeed(gait, mobSpeed));
                }
                if (FormUse.USABLE.contains(form)) FormUse.use(player);
                Double jump = FORM_JUMPS.get(form);
                double strength = player.getAttributeValue(Attributes.JUMP_STRENGTH);
                if (jump != null && Math.abs(strength - jump) > 1.0E-6) wrong.add(name + " jumps " + strength + ", expected " + jump);
                if (MorphFlight.flies(form) != player.getAbilities().mayfly) wrong.add(name + (player.getAbilities().mayfly ? " flies" : " can't fly"));
                tick(player, 20);
                if (!player.isAlive()) wrong.add(name + " died within a second");
            } catch (RuntimeException e) {
                wrong.add(name + " threw " + e);
            }
            if (!player.isAlive()) player = player(helper);
        }
        Morphs.setForm(player, null);
        check(helper, wrong.isEmpty(), forms.size() + " forms, " + wrong.size() + " wrong: " + String.join("; ", wrong));
        helper.succeed();
    }

    /** Ticks a measured mob walks before timing starts (to reach its pace), and how long it's timed for. */
    private static final int STROLL_WARM_UP = 40;
    /** Long enough that a running fox (about 9 blocks a second) is still on it when timing ends. */
    private static final int LANE_LENGTH = 130;
    /** Far enough apart that lanes don't bother each other (an armadillo curls up near undead mobs). */
    private static final int LANE_SPACING = 12;
    private static final int LANES_PER_BATCH = 10;
    private static final int STROLL_TICKS = 60;
    /** How far a measured mob may be from {@link MobGaits}' prediction. */
    private static final double GAIT_TOLERANCE = 0.1;

    /**
     * Every land form's real mob strolls, and runs, at its own AI's multipliers down its own stone lane
     * high above the grid; its ground speed must match {@link MobGaits}' stroll and run for it (a form walks
     * at twice the stroll), within 10%. The strider walks on lava, where it's warm. Lanes run {@value #LANES_PER_BATCH} at a time
     * on one force-loaded patch. Ridden mobs' runs are the player's input on them, so only their walk is
     * measured.
     */
    @GameTest(template = EMPTY, timeoutTicks = 1600)
    @SuppressWarnings("unchecked")
    public static void mobsWalkAtTheirOwnPace(GameTestHelper helper) {
        List<GaitLane> lanes = new ArrayList<>();
        List<Map.Entry<EntityType<?>, MobGaits.Gait>> gaits = MobGaits.GAITS.entrySet().stream() // the same batches every run
            .sorted(java.util.Comparator.comparing(entry -> BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey()).toString())).toList();
        for (Map.Entry<EntityType<?>, MobGaits.Gait> entry : gaits) {
            MobGaits.Gait gait = entry.getValue();
            double mobSpeed = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) entry.getKey()).getBaseValue(Attributes.MOVEMENT_SPEED);
            lanes.add(new GaitLane(entry.getKey(), false, gait.walk(), MobGaits.toBlocksPerSecond(MobGaits.strollSpeed(gait, mobSpeed))));
            if (!gait.ridden()) lanes.add(new GaitLane(entry.getKey(), true, gait.run(), MobGaits.toBlocksPerSecond(MobGaits.mobRunSpeed(gait, mobSpeed))));
        }
        for (int x = 0; x < LANES_PER_BATCH * LANE_SPACING; x++) {
            for (int z = -1; z <= LANE_LENGTH; z++) {
                helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 58, z)), Blocks.STONE.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 59, z)), Blocks.STONE.defaultBlockState());
            }
        }
        List<net.minecraft.world.level.ChunkPos> forced = forceLanes(helper, LANES_PER_BATCH);
        List<String> rows = new ArrayList<>();
        List<String> wrong = new ArrayList<>();
        List<Mob> mobs = new ArrayList<>();
        List<Vec3> last = new ArrayList<>();
        List<List<Double>> steps = new ArrayList<>();
        int batchTicks = STROLL_WARM_UP + STROLL_TICKS + 1;
        int[] tick = {-1};
        helper.onEachTick(() -> {
            if (tick[0] < 0 && !lanesTicking(helper)) return; // forced chunks take a moment to start ticking
            tick[0]++;
            int batch = tick[0] / batchTicks;
            int t = tick[0] % batchTicks;
            int first = batch * LANES_PER_BATCH;
            if (first >= lanes.size()) return;
            List<GaitLane> current = lanes.subList(first, Math.min(first + LANES_PER_BATCH, lanes.size()));
            if (t == 0) {
                mobs.clear();
                last.clear();
                steps.clear();
                for (int i = 0; i < LANES_PER_BATCH; i++) { // a strider walks on lava, the rest on stone
                    boolean lava = i < current.size() && current.get(i).form() == EntityType.STRIDER;
                    for (int x = i * LANE_SPACING; x < i * LANE_SPACING + 3; x++) {
                        for (int z = 0; z < LANE_LENGTH; z++) {
                            helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 59, z)),
                                (lava ? Blocks.LAVA : Blocks.STONE).defaultBlockState());
                        }
                    }
                }
                for (int i = 0; i < current.size(); i++) {
                    Mob mob = (Mob) current.get(i).form().create(helper.getLevel());
                    Vec3 at = helper.absoluteVec(new Vec3(i * LANE_SPACING + 1.5, 60, 0.5));
                    mob.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
                    mob.goalSelector.removeAllGoals(goal -> true);
                    mob.targetSelector.removeAllGoals(goal -> true);
                    mob.setPersistenceRequired();
                    if (mob.getBrain().checkMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, MemoryStatus.REGISTERED)) {
                        mob.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, Integer.MAX_VALUE); // a goat walks, not leaps
                    }
                    if (mob.getBrain().checkMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.REGISTERED)) {
                        mob.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, Integer.MAX_VALUE);
                    }
                    helper.getLevel().addFreshEntity(mob);
                    mobs.add(mob);
                    last.add(null);
                    steps.add(new ArrayList<>());
                }
            }
            for (int i = 0; i < mobs.size(); i++) {
                Mob mob = mobs.get(i);
                float modifier = (float) current.get(i).modifier();
                Vec3 target = new Vec3(mob.getX(), mob.getY(), mob.getZ() + 20);
                if (mob.getBrain().checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED)) {
                    mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, modifier, 0));
                } else {
                    mob.getNavigation().moveTo(target.x, target.y, target.z, modifier);
                }
                if (t >= STROLL_WARM_UP && last.get(i) != null) steps.get(i).add(mob.position().subtract(last.get(i)).horizontalDistance());
                last.set(i, mob.position());
            }
            if (t != batchTicks - 1) return;
            for (int i = 0; i < mobs.size(); i++) {
                GaitLane lane = current.get(i);
                double measured = steps.get(i).stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 20.0;
                String row = String.format("%s %s %.2f b/s, given %.2f", BuiltInRegistries.ENTITY_TYPE.getKey(lane.form()).getPath(),
                    lane.run() ? "run" : "walk", measured, lane.expected());
                rows.add(row);
                if (Math.abs(measured - lane.expected()) > lane.expected() * GAIT_TOLERANCE) wrong.add(row);
                mobs.get(i).discard();
            }
            if (first + LANES_PER_BATCH < lanes.size()) return;
            for (int x = 0; x < LANES_PER_BATCH * LANE_SPACING; x++) { // no lava left lying about
                for (int z = 0; z < LANE_LENGTH; z++) {
                    helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 59, z)), Blocks.STONE.defaultBlockState());
                }
            }
            forced.forEach(chunk -> helper.getLevel().setChunkForced(chunk.x, chunk.z, false));
            DungeonTotems.LOGGER.info("Mob gaits:\n{}", String.join("\n", rows));
            check(helper, wrong.isEmpty(), wrong.size() + " gaits off: " + String.join("; ", wrong));
            helper.succeed();
        });
    }

    /** One measured stroll or run: which mob, at what multiplier, and the blocks a second the form is given. */
    private record GaitLane(EntityType<?> form, boolean run, double modifier, double expected) {}

    /** Whether every chunk under the lanes ticks its entities yet. */
    private static boolean lanesTicking(GameTestHelper helper) {
        for (int x = 0; x <= LANES_PER_BATCH * LANE_SPACING; x += 16) {
            for (int z = 0; z <= LANE_LENGTH; z += 16) {
                if (!helper.getLevel().isPositionEntityTicking(helper.absolutePos(new BlockPos(x, 60, z)))) return false;
            }
        }
        return true;
    }

    /** Force-loads the chunks under {@code lanes} lanes 4 blocks apart, so every lane's mob ticks. */
    private static List<net.minecraft.world.level.ChunkPos> forceLanes(GameTestHelper helper, int lanes) {
        BlockPos corner = helper.absolutePos(BlockPos.ZERO);
        List<net.minecraft.world.level.ChunkPos> forced = new ArrayList<>();
        for (int cx = (corner.getX() - 2) >> 4; cx <= (corner.getX() + lanes * LANE_SPACING + 2) >> 4; cx++) {
            for (int cz = (corner.getZ() - 2) >> 4; cz <= (corner.getZ() + LANE_LENGTH + 4) >> 4; cz++) {
                helper.getLevel().setChunkForced(cx, cz, true);
                forced.add(new net.minecraft.world.level.ChunkPos(cx, cz));
            }
        }
        return forced;
    }

    private static int shots(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(32);
        return helper.getLevel().getEntitiesOfClass(Projectile.class, area).size();
    }

    @GameTest(template = EMPTY)
    public static void morphCommandStepsThroughForms(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        net.minecraft.commands.CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        net.minecraft.commands.Commands commands = helper.getLevel().getServer().getCommands();
        commands.performPrefixedCommand(source, "morph minecraft:cow");
        check(helper, Morphs.formOf(player).orElse(null) == EntityType.COW, "/morph cow makes a cow");
        commands.performPrefixedCommand(source, "morph next");
        check(helper, Morphs.formOf(player).orElse(null) == EntityType.DOLPHIN, "/morph next after cow is dolphin, was " + Morphs.formOf(player));
        commands.performPrefixedCommand(source, "morph prev");
        commands.performPrefixedCommand(source, "morph prev");
        check(helper, Morphs.formOf(player).orElse(null) == EntityType.COD, "two back from dolphin is cod, was " + Morphs.formOf(player));
        commands.performPrefixedCommand(source, "morph minecraft:zombie");
        check(helper, Morphs.formOf(player).orElse(null) == EntityType.COD, "a zombie isn't a morph form");
        commands.performPrefixedCommand(source, "morph off");
        check(helper, Morphs.formOf(player).isEmpty(), "/morph off turns you back");
        commands.performPrefixedCommand(source, "morph prev");
        check(helper, Morphs.formOf(player).orElse(null) == EntityType.ZOMBIFIED_PIGLIN, "prev from nothing wraps to the last form");
        commands.performPrefixedCommand(player.createCommandSourceStack().withPermission(0).withSuppressedOutput(), "morph off");
        check(helper, Morphs.formOf(player).isPresent(), "only ops can morph");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void camelDashesOnJumpAndSitsOnUse(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.CAMEL);
        player.setOnGround(true);
        FormUse.use(player);
        check(helper, player.getDeltaMovement().horizontalDistance() < 0.1, "right-click sits a camel down, it doesn't dash");
        FormUse.jump(player);
        Vec3 motion = player.getDeltaMovement();
        check(helper, motion.horizontalDistance() > 1.0 && motion.y > 0.0, "a camel's jump is its dash, was " + motion);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void goatChargesAtNothing(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.GOAT);
        FormAttacks.attackAir(player);
        check(helper, player.getDeltaMovement().horizontalDistance() > 0.5, "a goat attacking the air charges into a ram");
        Morphs.setForm(player, EntityType.CAT);
        player.setDeltaMovement(Vec3.ZERO);
        FormAttacks.attackAir(player); // just its noise
        check(helper, player.getDeltaMovement().horizontalDistance() == 0.0, "a cat attacking the air doesn't move");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void waterFormsBreatheLikeTheirMobs(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.COD);
        tick(player, 100);
        check(helper, WaterLife.airLeft(player) == 200, "a cod on land has 15 s of air, " + WaterLife.airLeft(player) + " left after 5 s");
        check(helper, player.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.0, "a cod can't walk on land");
        Morphs.setForm(player, EntityType.AXOLOTL);
        tick(player, 100);
        check(helper, WaterLife.airLeft(player) == 5900, "an axolotl on land has 5 min of air, " + WaterLife.airLeft(player) + " left after 5 s");
        check(helper, player.getAirSupply() < player.getMaxAirSupply(), "an axolotl's air bar runs down on land");
        check(helper, player.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.0, "an axolotl walks on land");
        Morphs.setForm(player, EntityType.DOLPHIN);
        tick(player, 100);
        check(helper, WaterLife.airLeft(player) == 4800, "a dolphin breathes air on land");
        submerge(helper, player, Blocks.WATER);
        tick(player, 99);
        check(helper, WaterLife.airLeft(player) == 4700, "a dolphin holds its breath under water, " + WaterLife.airLeft(player) + " left after 5 s");
        check(helper, player.getAirSupply() < player.getMaxAirSupply(), "a dolphin's air bar runs down under water");
        Morphs.setForm(player, EntityType.COD);
        tick(player, 20);
        check(helper, WaterLife.airLeft(player) == 300 && player.getAirSupply() == player.getMaxAirSupply(), "a cod breathes in water");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void doorsStillOpenAsAForm(GameTestHelper helper) {
        BlockPos door = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos grass = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.getLevel().setBlockAndUpdate(door, Blocks.OAK_DOOR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(grass, Blocks.GRASS_BLOCK.defaultBlockState());
        check(helper, FormUse.isUsable(helper.getLevel(), door), "a door is for using");
        check(helper, !FormUse.isUsable(helper.getLevel(), grass), "grass isn't: right-click does the form's move");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void landFormsHoldTheirBreathLikeTheirMobs(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.COW);
        submerge(helper, player, Blocks.WATER);
        tick(player, 40);
        check(helper, player.getAirSupply() < player.getMaxAirSupply(), "a cow runs out of air under water");
        for (EntityType<?> form : List.of(EntityType.TURTLE, EntityType.FROG, EntityType.IRON_GOLEM, EntityType.ZOMBIFIED_PIGLIN,
                EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE)) {
            Morphs.setForm(player, form);
            player.setAirSupply(player.getMaxAirSupply());
            tick(player, 40);
            check(helper, player.getAirSupply() == player.getMaxAirSupply(), form.getDescriptionId() + " should never run out of air under water");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void waterHurtsStridersAndSnowGolems(GameTestHelper helper) {
        for (EntityType<?> form : List.of(EntityType.STRIDER, EntityType.SNOW_GOLEM)) {
            ServerPlayer player = player(helper);
            Morphs.setForm(player, form);
            submerge(helper, player, Blocks.WATER);
            tick(player, 20);
            DamageSource last = player.getLastDamageSource(); // regeneration on a full stomach heals as fast as it hurts
            check(helper, last != null && last.is(DamageTypes.DROWN), form.getDescriptionId() + " should be hurt by water");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void slowFormsJumpAtTheirOwnPace(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        check(helper, FormAir.paceOverPlayer(player) == 1.0, "a player keeps a player's air speed");
        Morphs.setForm(player, EntityType.TURTLE);
        tick(player, 1);
        double pace = FormAir.paceOverPlayer(player);
        check(helper, pace > 0.0 && pace < 0.2, "a turtle walks at a fraction of a player's pace, got " + pace);
        player.setSprinting(true);
        tick(player, 1);
        player.setDeltaMovement(Vec3.ZERO);
        player.jumpFromGround();
        double push = player.getDeltaMovement().horizontalDistance();
        check(helper, push < 0.2 * 0.2, "a turtle's sprint-jump pushes it " + push + ", a player's 0.2");
        Morphs.setForm(player, EntityType.HORSE);
        player.setSprinting(true);
        tick(player, 1);
        check(helper, FormAir.paceOverPlayer(player) > 1.0, "a galloping horse carries more speed into a jump than a player");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tunedFormsMoveAtTheirSetSpeeds(GameTestHelper helper) {
        Map<EntityType<?>, double[]> expected = Map.of(
            EntityType.HORSE, new double[] {4.86, 9.71},
            EntityType.DONKEY, new double[] {3.78, 7.55},
            EntityType.LLAMA, new double[] {3.78, 7.55},
            EntityType.CAMEL, new double[] {2.85, 8.20},
            EntityType.ZOMBIFIED_PIGLIN, new double[] {3.0, 3.9},
            EntityType.POLAR_BEAR, new double[] {3.3, 4.3},
            EntityType.FOX, new double[] {4.0, 7.0},
            EntityType.WOLF, new double[] {4.0, 7.0});
        List<String> wrong = new ArrayList<>();
        expected.forEach((form, speeds) -> {
            MobGaits.Gait gait = MobGaits.of(form);
            double mobSpeed = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) form).getBaseValue(Attributes.MOVEMENT_SPEED);
            double walk = MobGaits.toBlocksPerSecond(MobGaits.walkSpeed(gait, mobSpeed));
            double run = MobGaits.toBlocksPerSecond(MobGaits.runSpeed(gait, mobSpeed));
            if (Math.abs(walk - speeds[0]) > 0.02 || Math.abs(run - speeds[1]) > 0.02) {
                wrong.add(form.getDescriptionId() + " " + String.format("%.2f/%.2f", walk, run));
            }
        });
        check(helper, wrong.isEmpty(), "speeds off the table: " + wrong);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void squidInkBlindsNearbyPlayers(GameTestHelper helper) {
        ServerPlayer squid = player(helper);
        ServerPlayer other = player(helper);
        Morphs.setForm(squid, EntityType.SQUID);
        FormUse.use(squid);
        check(helper, other.hasEffect(MobEffects.BLINDNESS), "ink should blind a nearby player");
        check(helper, !squid.hasEffect(MobEffects.BLINDNESS), "the squid isn't blinded by its own ink");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void puffedPufferfishPoisonsOnTouch(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.PUFFERFISH);
        FormUse.use(player);
        check(helper, WaterMoves.isPuffed(player), "right-click should puff up");
        Pig pig = helper.spawn(EntityType.PIG, 2, 1, 2);
        tick(player, 1);
        check(helper, pig.hasEffect(MobEffects.POISON), "touching a puffed pufferfish should poison");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void frogEatsSmallSlimes(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.FROG);
        Slime slime = helper.spawn(EntityType.SLIME, 2, 1, 3);
        slime.setSize(1, true);
        NeoForge.EVENT_BUS.post(new AttackEntityEvent(player, slime));
        check(helper, slime.isRemoved(), "a frog's tongue should eat a small slime whole");
        check(helper, player.getAttributeValue(Attributes.JUMP_STRENGTH) > 0.42 * 1.4, "a frog should leap");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void snowGolemThrowsSnowballs(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.SNOW_GOLEM);
        FormUse.use(player);
        check(helper, !helper.getLevel().getEntitiesOfClass(Snowball.class, player.getBoundingBox().inflate(3)).isEmpty(),
            "a snow golem should throw a snowball");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void turtleLaysEggOnSand(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.getLevel().setBlockAndUpdate(player.blockPosition().below(), Blocks.SAND.defaultBlockState());
        Morphs.setForm(player, EntityType.TURTLE);
        check(helper, WaterLife.breathesUnderWater(EntityType.TURTLE), "a turtle breathes underwater");
        FormUse.use(player);
        check(helper, helper.getLevel().getBlockState(player.blockPosition()).is(Blocks.TURTLE_EGG), "a turtle should lay an egg on sand");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void axolotlPlaysDead(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.AXOLOTL);
        FormUse.use(player);
        check(helper, WaterMoves.isPlayingDead(player) && player.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.0,
            "playing dead holds you still");
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 4);
        zombie.setTarget(player);
        check(helper, zombie.getTarget() == null, "mobs ignore an axolotl playing dead");
        Morphs.setForm(player, EntityType.COD);
        check(helper, !WaterMoves.isPlayingDead(player), "changing form wakes you");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void mooshroomCanBeMilkedForStew(GameTestHelper helper) {
        ServerPlayer mooshroom = player(helper);
        ServerPlayer milker = player(helper);
        Morphs.setForm(mooshroom, EntityType.MOOSHROOM);
        milker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOWL));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(milker, InteractionHand.MAIN_HAND, mooshroom));
        check(helper, milker.getMainHandItem().is(Items.MUSHROOM_STEW), "a bowl on a mooshroom should give stew");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void zombifiedPiglinsAreKin(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.ZOMBIFIED_PIGLIN);
        check(helper, player.hasEffect(MobEffects.FIRE_RESISTANCE), "a zombified piglin is fire-proof");
        ZombifiedPiglin piglin = helper.spawn(EntityType.ZOMBIFIED_PIGLIN, 2, 1, 4);
        piglin.setTarget(player);
        check(helper, piglin.getTarget() == null, "zombified piglins don't turn on their own");
        Pig pig = helper.spawn(EntityType.PIG, 4, 1, 2);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(pig), 1.0F);
        check(helper, piglin.getTarget() == pig, "the pack turns on whoever hurts you");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void creepersKeepAwayFromCats(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.CAT);
        Creeper creeper = helper.spawn(EntityType.CREEPER, 2, 1, 4);
        creeper.setTarget(player);
        check(helper, creeper.getTarget() == null, "creepers won't go for a cat");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void striderFloatsOnLava(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.STRIDER);
        check(helper, player.canStandOnFluid(Fluids.LAVA.defaultFluidState()), "a strider stands on lava");
        check(helper, !player.canStandOnFluid(Fluids.WATER.defaultFluidState()), "but not on water");
        submerge(helper, player, Blocks.LAVA);
        player.setDeltaMovement(0.0, -0.5, 0.0);
        tick(player, 10);
        check(helper, player.isAlive() && player.getDeltaMovement().y > 0.0, "a sunk strider should bob up, was " + player.getDeltaMovement());
        check(helper, FormTraits.isWarmStrider(player), "a strider in lava is warm");
        check(helper, !player.isOnFire(), "a strider in lava doesn't burn");
        Morphs.setForm(player, EntityType.PIG);
        check(helper, !player.canStandOnFluid(Fluids.LAVA.defaultFluidState()), "a pig doesn't stand on lava");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void chickenFormFlutters(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.CHICKEN);
        check(helper, player.hasEffect(MobEffects.SLOW_FALLING), "a chicken should fall slowly");
        check(helper, !player.getAbilities().flying, "a chicken walks");
        Morphs.setForm(player, EntityType.COW);
        check(helper, !player.hasEffect(MobEffects.SLOW_FALLING), "slow falling should end with the chicken form");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void monstersIgnoreAMorphedPlayer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.COW);
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        zombie.setTarget(player);
        check(helper, zombie.getTarget() == null, "zombie should not target a cow");
        var pig = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
        check(helper, pig instanceof Pig, "sanity");
        helper.succeed();
    }

    // ---- zombified ----

    @GameTest(template = EMPTY)
    public static void zombifiedIsCuredByAGoldenAppleWhileWeakened(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.ZOMBIFIED);
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, Morphs.formOf(player).orElse(null) == EntityType.ZOMBIE, "player should be a zombie");
        var witch = helper.spawn(EntityType.WITCH, new BlockPos(1, 2, 1));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400), witch); // a splash
        check(helper, player.hasEffect(TotemRegistries.ZOMBIFIED), "weakness alone should not cure");
        eatGoldenApple(player);
        check(helper, !player.hasEffect(TotemRegistries.ZOMBIFIED), "golden apple while weakened should cure");
        check(helper, Morphs.formOf(player).isEmpty(), "form should be gone");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void goldenAppleWithoutWeaknessDoesNotCure(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.ZOMBIFIED);
        player.hurt(player.damageSources().generic(), LETHAL);
        eatGoldenApple(player);
        check(helper, player.hasEffect(TotemRegistries.ZOMBIFIED), "a golden apple alone should not cure");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void weaknessTotemCurseCountsAsWeakness(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.ZOMBIFIED);
        pop(player, Curse.WEAKNESS);
        check(helper, player.hasEffect(TotemRegistries.ZOMBIFIED), "a totem's weakness alone is no cure");
        eatGoldenApple(player);
        check(helper, !player.hasEffect(TotemRegistries.ZOMBIFIED), "golden apple with the Weakness curse should cure");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void snowGolemsHuntAZombifiedPlayer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.ZOMBIFIED);
        SnowGolem golem = helper.spawn(EntityType.SNOW_GOLEM, 2, 1, 2);
        tick(player, 21);
        check(helper, golem.getTarget() == player, "a snow golem should target a zombie player, targets " + golem.getTarget());
        golem.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void zombifiedRunningOutKills(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.addEffect(new MobEffectInstance(TotemRegistries.ZOMBIFIED, 2, 0));
        Morphs.setForm(player, EntityType.ZOMBIE);
        tick(player, 5);
        check(helper, !player.isAlive(), "player should die when zombification runs out");
        helper.succeed();
    }

    // ---- stacking and death ----

    @GameTest(template = EMPTY)
    public static void milkLeavesTheCurses(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.addEffect(new MobEffectInstance(TotemRegistries.RAGE, 400, 0));
        player.addEffect(new MobEffectInstance(TotemRegistries.HEARTBREAK, 400, 0));
        player.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
        check(helper, player.hasEffect(TotemRegistries.RAGE), "milk should not lift rage");
        check(helper, !player.hasEffect(TotemRegistries.HEARTBREAK), "milk does lift heartbreak");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void laterPopsKeepEarlierCurses(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        for (Curse curse : List.of(Curse.FATIGUE, Curse.HEARTBREAK, Curse.WEAKNESS)) pop(player, curse);
        check(helper, player.hasEffect(MobEffects.DIG_SLOWDOWN), "fatigue cleared by a later pop");
        check(helper, player.hasEffect(TotemRegistries.HEARTBREAK), "heartbreak cleared by a later pop");
        check(helper, player.hasEffect(MobEffects.WEAKNESS), "weakness missing");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void secondSaveRefreshesWithoutLevellingUp(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.WEAKNESS);
        tick(player, 20);
        pop(player, Curse.WEAKNESS);
        MobEffectInstance weakness = player.getEffect(MobEffects.WEAKNESS);
        check(helper, weakness != null && weakness.getAmplifier() == 0, "a second save stays Weakness I, was " + weakness);
        expectEffect(helper, player, MobEffects.WEAKNESS, Curse.WEAKNESS.afterSaveTicks());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void heartbreakStopsAtHalfAHeart(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.HEARTBREAK, 4, 400);
        check(helper, player.getMaxHealth() == 1.0F, "max health should stop at half a heart, was " + player.getMaxHealth());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void afterSaveCursesComeBackAfterDeath(GameTestHelper helper) {
        ServerPlayer dead = player(helper);
        pop(dead, Curse.RAGE);
        pop(dead, Curse.HEARTBREAK);
        Curses.apply(dead, Curse.SLOWNESS, 0, MobEffectInstance.INFINITE_DURATION); // as if held
        tick(dead, 20);
        dead.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400)); // a plain effect, not a curse
        ServerPlayer respawned = player(helper);
        CurseMemory.onClone(new PlayerEvent.Clone(respawned, dead, true));
        CurseMemory.onRespawn(new PlayerEvent.PlayerRespawnEvent(respawned, false));
        MobEffectInstance rage = respawned.getEffect(TotemRegistries.RAGE);
        check(helper, rage != null && rage.getAmplifier() == 1, "rage II should come back, was " + rage);
        expectEffect(helper, respawned, TotemRegistries.RAGE, Curse.RAGE.afterSaveTicks());
        check(helper, respawned.hasEffect(TotemRegistries.HEARTBREAK), "heartbreak should come back");
        check(helper, !respawned.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a held curse should not come back");
        check(helper, !respawned.hasEffect(MobEffects.BLINDNESS), "a plain effect should not come back");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void expiredZombifiedStaysGoneAfterDeath(GameTestHelper helper) {
        ServerPlayer dead = player(helper);
        dead.addEffect(new MobEffectInstance(TotemRegistries.ZOMBIFIED, 2, 0));
        tick(dead, 5);
        check(helper, !dead.isAlive(), "zombification should have killed");
        ServerPlayer respawned = player(helper);
        CurseMemory.onClone(new PlayerEvent.Clone(respawned, dead, true));
        CurseMemory.onRespawn(new PlayerEvent.PlayerRespawnEvent(respawned, false));
        check(helper, !respawned.hasEffect(TotemRegistries.ZOMBIFIED), "the curse that killed you should not come back");
        helper.succeed();
    }

    // ---- loot ----

    @GameTest(template = EMPTY)
    public static void evokersDropDungeonTotems(GameTestHelper helper) {
        ServerPlayer killer = player(helper);
        Evoker evoker = helper.spawn(EntityType.EVOKER, 2, 1, 2);
        LootParams params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.THIS_ENTITY, evoker)
            .withParameter(LootContextParams.ORIGIN, evoker.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, killer.damageSources().playerAttack(killer))
            .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
            .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
            .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
            .create(LootContextParamSets.ENTITY);
        LootTable table = helper.getLevel().getServer().reloadableRegistries()
            .getLootTable(EntityType.EVOKER.getDefaultLootTable());
        Set<Item> seen = new HashSet<>();
        boolean emerald = false;
        for (int roll = 0; roll < LOOT_ROLLS; roll++) {
            int totems = 0;
            for (ItemStack stack : table.getRandomItems(params)) {
                check(helper, !stack.is(Items.TOTEM_OF_UNDYING), "evoker dropped a vanilla totem");
                if (stack.is(EvokerTotemLootModifier.EVOKER_TOTEMS)) {
                    totems += stack.getCount();
                    seen.add(stack.getItem());
                }
                emerald |= stack.is(Items.EMERALD);
            }
            check(helper, totems == 1, "roll " + roll + " dropped " + totems + " dungeon totems, expected 1");
        }
        check(helper, seen.size() > 1, "every roll gave the same totem: " + seen);
        check(helper, emerald, "the emerald pool should still drop");
        evoker.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void killedEvokerDropsADungeonTotem(GameTestHelper helper) {
        ServerPlayer killer = player(helper);
        Evoker evoker = helper.spawn(EntityType.EVOKER, 2, 1, 2);
        evoker.hurt(killer.damageSources().playerAttack(killer), LETHAL);
        check(helper, !evoker.isAlive(), "evoker should die");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, evoker.getBoundingBox().inflate(2));
        check(helper, drops.stream().noneMatch(drop -> drop.getItem().is(Items.TOTEM_OF_UNDYING)), "dropped a vanilla totem");
        check(helper, drops.stream().anyMatch(drop -> drop.getItem().is(EvokerTotemLootModifier.EVOKER_TOTEMS)),
            "no dungeon totem among " + drops.stream().map(drop -> drop.getItem().toString()).toList());
        helper.succeed();
    }

    // ---- advancements ----

    @GameTest(template = EMPTY)
    public static void advancementsStayOffUntilLaunch(GameTestHelper helper) {
        check(helper, !TotemAdvancements.isLaunched(false, null) && !TotemAdvancements.isLaunched(false, "false"),
            "before launch, only a dev run's property loads the tab");
        check(helper, TotemAdvancements.isLaunched(true, null), "a launched release loads it");
        Map<ResourceLocation, Resource> files = helper.getLevel().getServer().getResourceManager()
            .listResources("advancement", path -> path.getNamespace().equals(DungeonTotems.MOD_ID) && path.getPath().endsWith(".json"));
        check(helper, files.size() == TabComplete.TAB.size() + 1, "expected every advancement file, found " + files.keySet());
        List<ResourceLocation> ungated = new ArrayList<>();
        files.forEach((path, resource) -> {
            try (var reader = resource.openAsReader()) {
                if (!JsonParser.parseReader(reader).toString().contains("\"dungeontotems:advancements_launched\"")) ungated.add(path);
            } catch (java.io.IOException e) {
                ungated.add(path);
            }
        });
        check(helper, ungated.isEmpty(), "these would show before launch: " + ungated);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void everyTotemAdvancementLoads(GameTestHelper helper) {
        List<ResourceLocation> ids = new ArrayList<>(TabComplete.TAB);
        ids.add(TabComplete.FULFILLED);
        for (ResourceLocation id : ids) check(helper, helper.getLevel().getServer().getAdvancements().get(id) != null, id + " didn't load");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void poppingADungeonTotemAgreesToTheTerms(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.WEAKNESS);
        check(helper, progress(player, "terms_and_conditions").isDone(), "popping a dungeon totem should earn Terms and Conditions");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aTimedCurseRunningOutAliveCounts(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.BLINDNESS);
        Curses.markSaved(player, Curse.BLINDNESS, 0); // its after-save time over (game time stands still in these ticks)
        tick(player, 5);
        check(helper, criterionDone(player, "waiting_it_out", "blindness"), "Blindness lived through should count for Waiting It Out");
        check(helper, !progress(player, "waiting_it_out").isDone(), "one curse of nine isn't Waiting It Out");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dyingEndsACurseTrial(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.WEAKNESS);
        check(helper, player.getData(TotemRegistries.CURSE_TRIALS).contains("weakness"), "a pop starts a trial");
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, !player.isAlive() && player.getData(TotemRegistries.CURSE_TRIALS).isEmpty(), "dying ends every trial");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void zombifiedOnlyCountsWhenCured(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.ZOMBIFIED);
        Curses.markSaved(player, Curse.ZOMBIFIED, 0);
        tick(player, 3);
        check(helper, !criterionDone(player, "promises_kept", "zombified"), "Zombified counts only once cured");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void heldTotemsStillCountOnceTheSaveIsOver(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.RAGE);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(TotemRegistries.TOTEMS.get(Curse.RAGE).get())); // still holding one
        Curses.markSaved(player, Curse.RAGE, 0);
        tick(player, 5);
        check(helper, criterionDone(player, "promises_kept", "rage"), "Rage lived through should count even with a Rage totem held");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aCuredZombieIsBrainsOptional(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.ZOMBIFIED);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
        eatGoldenApple(player);
        check(helper, progress(player, "brains_optional").isDone(), "curing Zombified should earn Brains Optional");
        check(helper, criterionDone(player, "promises_kept", "zombified"), "and count for Promises Kept");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theThreeLifesaversAreCloseCalls(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        for (Curse curse : List.of(Curse.VOID, Curse.FEATHER, Curse.BOOM, Curse.FIRE)) {
            check(helper, !progress(player, "close_calls").isDone(), "Close Calls before " + curse.key());
            CurseTrials.onPop(player, curse);
        }
        check(helper, progress(player, "close_calls").isDone(), "Void, Feather, Boom and Fire should be Close Calls");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void greatAndSmallHasEveryForm(GameTestHelper helper) {
        Set<String> criteria = helper.getLevel().getServer().getAdvancements().get(DungeonTotems.id("great_and_small")).value().criteria().keySet();
        Set<String> forms = new HashSet<>();
        BuiltInRegistries.ENTITY_TYPE.getDataMap(MorphForm.DATA_MAP).keySet().forEach(key -> forms.add(key.location().toString()));
        check(helper, criteria.equals(forms), "Great and Small should list every morph form (run scripts/make_advancements.py): "
            + "missing " + forms.stream().filter(f -> !criteria.contains(f)).toList());
        ServerPlayer player = player(helper);
        Morphs.startRandomMorph(player);
        String form = BuiltInRegistries.ENTITY_TYPE.getKey(Morphs.formOf(player).orElseThrow()).toString();
        check(helper, criterionDone(player, "great_and_small", form), "becoming a " + form + " should tick it off Great and Small");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theWholeTabFulfilsTheTerms(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        for (ResourceLocation id : TabComplete.TAB) {
            check(helper, !progress(player, TabComplete.FULFILLED.getPath()).isDone(), "fulfilled before " + id);
            AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(id);
            holder.value().criteria().keySet().forEach(criterion -> player.getAdvancements().award(holder, criterion));
        }
        check(helper, progress(player, TabComplete.FULFILLED.getPath()).isDone(), "every advancement should fulfil the terms");
        helper.succeed();
    }

    // ---- helpers ----

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 1, 2.5));
        player.teleportTo(at.x, at.y, at.z);
        for (int i = 0; i < SPAWN_INVULNERABLE_TICKS; i++) player.tick(); // only counts down spawn protection
        return player;
    }

    private static ServerPlayer holding(GameTestHelper helper, Curse curse) {
        return holding(helper, curse, 1);
    }

    private static ServerPlayer holding(GameTestHelper helper, Curse curse, int count) {
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TotemRegistries.TOTEMS.get(curse).get(), count));
        return player;
    }

    /** The held rule totem popped: gone from the hand, you alive on half a heart, its after-save curse on. */
    private static void expectRulePop(GameTestHelper helper, ServerPlayer player, Curse curse) {
        check(helper, player.isAlive(), curse + ": breaking the rule should not kill");
        check(helper, player.getMainHandItem().isEmpty(), curse + ": the totem should be used up");
        check(helper, player.getHealth() <= 1.0F + 0.01F || player.hasEffect(MobEffects.REGENERATION), curse + ": revived like a pop");
        check(helper, Curses.isSaved(player, curse), curse + ": its after-save curse should be on");
    }

    private static void openBarrel(GameTestHelper helper, ServerPlayer player) {
        BlockPos barrel = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(barrel, Blocks.BARREL.defaultBlockState());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(barrel), Direction.UP, barrel, false);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, barrel, hit));
    }

    /** Puts the last hit or gift {@code ticksPast} ticks beyond the timer, so it has run out. */
    private static void overdue(ServerPlayer player, java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<Long>> last, int amplifier) {
        player.setData(last, player.level().getGameTime() - CurseTimers.seconds(amplifier) * 20L);
    }

    private static void eatGoldenApple(ServerPlayer player) {
        EventHooks.onItemUseFinish(player, new ItemStack(Items.GOLDEN_APPLE), 0, ItemStack.EMPTY);
    }

    private static Villager villager(GameTestHelper helper) {
        return helper.spawn(EntityType.VILLAGER, 2, 1, 2);
    }

    private static void trade(ServerPlayer player, Villager villager) {
        MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(Items.BREAD), 1, 1, 0.0F);
        NeoForge.EVENT_BUS.post(new TradeWithVillagerEvent(player, offer, villager));
    }

    /** Fills the space around the player with {@code fluid} and lets them notice it. */
    private static void submerge(GameTestHelper helper, ServerPlayer player, Block fluid) {
        BlockPos at = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-1, 0, -1), at.offset(1, 2, 1))) {
            helper.getLevel().setBlockAndUpdate(pos, fluid.defaultBlockState());
        }
        tick(player, 1);
    }

    /** Pops {@code curse}'s totem on the player. */
    private static void pop(ServerPlayer player, Curse curse) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TotemRegistries.TOTEMS.get(curse).get()));
        player.invulnerableTime = 0; // vanilla ignores a second hit this soon
        player.hurt(player.damageSources().generic(), LETHAL);
    }

    private static AdvancementProgress progress(ServerPlayer player, String id) {
        return player.getAdvancements().getOrStartProgress(player.server.getAdvancements().get(DungeonTotems.id(id)));
    }

    private static boolean criterionDone(ServerPlayer player, String id, String criterion) {
        CriterionProgress done = progress(player, id).getCriterion(criterion);
        return done != null && done.isDone();
    }

    private static void tick(ServerPlayer player, int ticks) {
        for (int i = 0; i < ticks && player.isAlive(); i++) player.doTick();
    }

    private static void expectEffect(GameTestHelper helper, ServerPlayer player, Holder<MobEffect> effect, int duration) {
        MobEffectInstance instance = player.getEffect(effect);
        check(helper, instance != null, "missing " + effect.getRegisteredName());
        check(helper, instance.getDuration() == duration,
            effect.getRegisteredName() + " lasts " + instance.getDuration() + ", expected " + duration);
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        if (!ok) helper.fail(message);
    }
}
