package games.brennan.dungeontotems.gametest;

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
import games.brennan.dungeontotems.morph.Morphs;
import games.brennan.dungeontotems.registry.TotemRegistries;
import games.brennan.dungeontotems.voidtotem.VoidRescue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
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
            expectEffect(helper, player, curse.effect(), curse.afterSaveTicks());
            MobEffectInstance instance = player.getEffect(curse.effect());
            check(helper, instance.getAmplifier() == curse.afterSaveAmplifier(), curse + " level " + instance.getAmplifier());
            check(helper, Curses.isSaved(player, curse), curse + " should be shielded after the save");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void afterSaveTimesMatchTheTable(GameTestHelper helper) {
        check(helper, Curse.MELON.afterSaveTicks() == 10 * 60 * 20, "melon diet lasts 10 minutes");
        check(helper, Curse.RAMPAGE.afterSaveTicks() == 5 * 60 * 20 && Curse.RAMPAGE.afterSaveAmplifier() == 1, "rage II for 5 minutes");
        check(helper, Curse.LONELINESS.letGoTicks() == 60 * 20 && Curse.LONELINESS.letGoAmplifier() == 1, "lonely II for 1 minute");
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
                check(helper, after != null && !after.isInfiniteDuration() && after.getAmplifier() == curse.letGoAmplifier()
                    && after.getDuration() > curse.letGoTicks() - HeldCurses.CHECK_TICKS, curse + " should leave its let-go curse, was " + after);
            } else {
                check(helper, after == null, curse + " should end when let go, was " + after);
            }
        }
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
        ServerPlayer player = holding(helper, Curse.MELON);
        EventHooks.onItemUseFinish(player, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY);
        tick(player, 2);
        expectRulePop(helper, player, Curse.MELON);
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
        player.setHealth(20.0F);
        player.invulnerableTime = 0; // the pop's own hit would otherwise swallow the doom
        openBarrel(helper, player);
        tick(player, 2);
        check(helper, player.isAlive(), "a curse can't kill you after a save");
        check(helper, player.getHealth() == 1.0F, "should be on half a heart, was " + player.getHealth());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void brokenRuleWithoutAShieldKills(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.MELON, 0, 400);
        EventHooks.onItemUseFinish(player, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY);
        tick(player, 2);
        check(helper, !player.isAlive(), "a melon diet with no totem and no save still kills");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void melonAndDrinksKeepTheMelonDiet(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.MELON, 0, 400);
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
        Curses.apply(player, Curse.RAMPAGE, 0, 400);
        overdue(player, TotemRegistries.LAST_HIT, 0);
        tick(player, 1);
        check(helper, player.getHealth() < 20.0F, "rage should hurt once the timer runs out");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aHitStopsRage(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Curses.apply(player, Curse.RAMPAGE, 0, 400);
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
        pop(player, Curse.RAMPAGE);
        player.removeAllEffects();
        Curses.apply(player, Curse.RAMPAGE, 1, 400);
        player.setHealth(1.0F);
        overdue(player, TotemRegistries.LAST_HIT, 1);
        player.invulnerableTime = 0;
        tick(player, 1);
        check(helper, player.isAlive() && player.getHealth() == 1.0F, "rage after a save stops at half a heart");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void heldRageTotemSavesYouFromRage(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.RAMPAGE);
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
        check(helper, form.is(TotemRegistries.MORPH_POOL), "form should come from the pool: " + form);
        check(helper, player.getBbHeight() == form.getDimensions().height(), "hitbox should be the form's");
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
        player.addEffect(new MobEffectInstance(TotemRegistries.RAMPAGE, 400, 0));
        player.addEffect(new MobEffectInstance(TotemRegistries.HEARTBREAK, 400, 0));
        player.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
        check(helper, player.hasEffect(TotemRegistries.RAMPAGE), "milk should not lift rage");
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
        pop(dead, Curse.RAMPAGE);
        pop(dead, Curse.HEARTBREAK);
        Curses.apply(dead, Curse.SLOWNESS, 0, MobEffectInstance.INFINITE_DURATION); // as if held
        tick(dead, 20);
        dead.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400)); // a plain effect, not a curse
        ServerPlayer respawned = player(helper);
        CurseMemory.onClone(new PlayerEvent.Clone(respawned, dead, true));
        CurseMemory.onRespawn(new PlayerEvent.PlayerRespawnEvent(respawned, false));
        MobEffectInstance rage = respawned.getEffect(TotemRegistries.RAMPAGE);
        check(helper, rage != null && rage.getAmplifier() == 1, "rage II should come back, was " + rage);
        expectEffect(helper, respawned, TotemRegistries.RAMPAGE, Curse.RAMPAGE.afterSaveTicks());
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
            check(helper, totems == 1, "roll " + roll + " dropped " + totems + " Dungeon Totems, expected 1");
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
            "no Dungeon Totem among " + drops.stream().map(drop -> drop.getItem().toString()).toList());
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
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TotemRegistries.TOTEMS.get(curse).get()));
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

    /** Pops {@code curse}'s totem on the player. */
    private static void pop(ServerPlayer player, Curse curse) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TotemRegistries.TOTEMS.get(curse).get()));
        player.invulnerableTime = 0; // vanilla ignores a second hit this soon
        player.hurt(player.damageSources().generic(), LETHAL);
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
