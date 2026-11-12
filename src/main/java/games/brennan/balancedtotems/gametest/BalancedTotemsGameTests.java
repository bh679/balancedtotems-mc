package games.brennan.balancedtotems.gametest;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.Curse;
import games.brennan.balancedtotems.api.BalancedTotemsApi;
import games.brennan.balancedtotems.curse.CurseMemory;
import games.brennan.balancedtotems.compat.PlayerMobGifts;
import games.brennan.balancedtotems.curse.Curses;
import games.brennan.balancedtotems.curse.Tallies;
import games.brennan.balancedtotems.loot.EvokerTotemLootModifier;
import games.brennan.balancedtotems.morph.FormAttacks;
import games.brennan.balancedtotems.morph.FormTraits;
import games.brennan.balancedtotems.morph.FormUse;
import games.brennan.balancedtotems.morph.MorphForm;
import games.brennan.balancedtotems.morph.Morphs;
import games.brennan.balancedtotems.morph.WaterMoves;
import games.brennan.balancedtotems.registry.BtRegistries;
import games.brennan.balancedtotems.voidtotem.VoidRescue;
import net.minecraft.core.registries.BuiltInRegistries;
import games.brennan.balancedtotems.morph.MorphFlight;
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
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Evoker;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
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
@GameTestHolder(BalancedTotems.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BalancedTotemsGameTests {

    private static final String EMPTY = "empty";
    private static final float LETHAL = 1000.0F;
    private static final int SPAWN_INVULNERABLE_TICKS = 61;
    private static final int LOOT_ROLLS = 50;

    private BalancedTotemsGameTests() {}

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
        expectEffect(helper, player, MobEffects.WEAKNESS, Curse.WEAKNESS.durationTicks());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void everyCurseTotemAppliesItsEffect(GameTestHelper helper) {
        for (Curse curse : Curse.values()) {
            if (curse.effect() == null) continue;
            ServerPlayer player = holding(helper, curse);
            player.hurt(player.damageSources().generic(), LETHAL);
            check(helper, player.isAlive(), curse + ": player should survive");
            expectEffect(helper, player, curse.effect(), curse.durationTicks());
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
        check(helper, !player.hasEffect(MobEffects.WEAKNESS), "no curse from a vanilla pop");
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
    public static void heartbreakTakesTwoHearts(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.HEARTBREAK);
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, player.getMaxHealth() == 16.0F, "max health should be 16, was " + player.getMaxHealth());
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
        BalancedTotemsApi.setRescueTarget(p -> Optional.of(target));
        try {
            ServerPlayer player = holding(helper, Curse.VOID);
            player.teleportTo(player.getX(), player.level().getMinBuildHeight() - 70, player.getZ());
            player.hurt(player.damageSources().fellOutOfWorld(), LETHAL);
            check(helper, player.isAlive(), "player should survive the void");
            check(helper, player.position().distanceTo(target) < 0.01, "player should be at the target, was " + player.position());
        } finally {
            BalancedTotemsApi.setRescueTarget(p -> Optional.empty());
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
        check(helper, charge > 0.5 && player.isSprinting(), "a goat should charge into the ram, moved " + charge);
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
            item -> item.getItem().is(Items.EGG)).isEmpty(), "a chicken should lay an egg");
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

    private static int shots(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(32);
        return helper.getLevel().getEntitiesOfClass(Projectile.class, area).size();
    }

    @GameTest(template = EMPTY)
    public static void camelDashesForward(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Morphs.setForm(player, EntityType.CAMEL);
        player.setOnGround(true);
        FormUse.use(player);
        Vec3 motion = player.getDeltaMovement();
        check(helper, motion.horizontalDistance() > 1.0 && motion.y > 0.0, "a camel's dash should lunge, was " + motion);
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
        check(helper, player.hasEffect(MobEffects.WATER_BREATHING), "a turtle breathes underwater");
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
        submerge(helper, player, Blocks.LAVA);
        player.setDeltaMovement(0.0, -0.5, 0.0);
        tick(player, 1);
        check(helper, player.isAlive() && player.getDeltaMovement().y > 0.0, "a strider should rise in lava, was " + player.getDeltaMovement());
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
        check(helper, player.hasEffect(BtRegistries.ZOMBIFIED), "weakness alone should not cure");
        eatGoldenApple(player);
        check(helper, !player.hasEffect(BtRegistries.ZOMBIFIED), "golden apple while weakened should cure");
        check(helper, Morphs.formOf(player).isEmpty(), "form should be gone");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void goldenAppleWithoutWeaknessDoesNotCure(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.ZOMBIFIED);
        player.hurt(player.damageSources().generic(), LETHAL);
        eatGoldenApple(player);
        check(helper, player.hasEffect(BtRegistries.ZOMBIFIED), "a golden apple alone should not cure");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void weaknessTotemCurseCountsAsWeakness(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.ZOMBIFIED);
        pop(player, Curse.WEAKNESS);
        check(helper, player.hasEffect(BtRegistries.ZOMBIFIED), "a totem's weakness alone is no cure");
        eatGoldenApple(player);
        check(helper, !player.hasEffect(BtRegistries.ZOMBIFIED), "golden apple with the Weakness curse should cure");
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
        player.addEffect(new MobEffectInstance(BtRegistries.ZOMBIFIED, 2, 0));
        Morphs.setForm(player, EntityType.ZOMBIE);
        tick(player, 5);
        check(helper, !player.isAlive(), "player should die when zombification runs out");
        helper.succeed();
    }

    // ---- rampage ----

    @GameTest(template = EMPTY)
    public static void rampageIsLiftedByThreeKills(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.RAMPAGE);
        player.hurt(player.damageSources().generic(), LETHAL);
        for (int i = 0; i < BtRegistries.RAMPAGE_KILLS_NEEDED; i++) {
            check(helper, player.hasEffect(BtRegistries.RAMPAGE), "rampage active before kill " + (i + 1));
            // The GameTest server has PvP off, so fire the kill's death event directly.
            ServerPlayer victim = player(helper);
            CommonHooks.onLivingDeath(victim, victim.damageSources().playerAttack(player));
        }
        check(helper, !player.hasEffect(BtRegistries.RAMPAGE), "three kills should lift the rampage");
        tick(player, 3);
        check(helper, player.isAlive(), "a lifted rampage never kills");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void rampageRunningOutKills(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.addEffect(new MobEffectInstance(BtRegistries.RAMPAGE, 2, 0));
        tick(player, 5);
        check(helper, !player.isAlive(), "player should die when the rampage runs out");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void milkLeavesTheDeadlineCurses(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.addEffect(new MobEffectInstance(BtRegistries.RAMPAGE, 400, 0));
        player.addEffect(new MobEffectInstance(BtRegistries.HEARTBREAK, 400, 0));
        player.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
        check(helper, player.hasEffect(BtRegistries.RAMPAGE), "milk should not lift rampage");
        check(helper, !player.hasEffect(BtRegistries.HEARTBREAK), "milk does lift heartbreak");
        helper.succeed();
    }

    // ---- stacking, levels and death ----

    @GameTest(template = EMPTY)
    public static void fatigueIsMiningFatigueTwo(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.FATIGUE);
        player.hurt(player.damageSources().generic(), LETHAL);
        MobEffectInstance fatigue = player.getEffect(MobEffects.DIG_SLOWDOWN);
        check(helper, fatigue != null && fatigue.getAmplifier() == 1, "fatigue should be Mining Fatigue II, was " + fatigue);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void laterPopsKeepEarlierCurses(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        for (Curse curse : List.of(Curse.FATIGUE, Curse.HEARTBREAK, Curse.WEAKNESS)) pop(player, curse);
        check(helper, player.hasEffect(MobEffects.DIG_SLOWDOWN), "fatigue cleared by a later pop");
        check(helper, player.hasEffect(BtRegistries.HEARTBREAK), "heartbreak cleared by a later pop");
        check(helper, player.hasEffect(MobEffects.WEAKNESS), "weakness missing");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void sameCurseLevelsUpWithAFullTimer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.WEAKNESS);
        tick(player, 20);
        pop(player, Curse.WEAKNESS);
        MobEffectInstance weakness = player.getEffect(MobEffects.WEAKNESS);
        check(helper, weakness != null && weakness.getAmplifier() == 1, "second pop should give Weakness II, was " + weakness);
        expectEffect(helper, player, MobEffects.WEAKNESS, Curse.WEAKNESS.durationTicks());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fatigueAddsTimeNotLevels(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.FATIGUE);
        tick(player, 20);
        pop(player, Curse.FATIGUE);
        MobEffectInstance fatigue = player.getEffect(MobEffects.DIG_SLOWDOWN);
        check(helper, fatigue != null && fatigue.getAmplifier() == 1, "fatigue should stay Mining Fatigue II, was " + fatigue);
        expectEffect(helper, player, MobEffects.DIG_SLOWDOWN, 2 * Curse.FATIGUE.durationTicks() - 20);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void heartbreakStopsAtHalfAHeart(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        for (int i = 0; i < 6; i++) pop(player, Curse.HEARTBREAK);
        check(helper, player.getMaxHealth() == 1.0F, "max health should stop at half a heart, was " + player.getMaxHealth());
        MobEffectInstance heartbreak = player.getEffect(BtRegistries.HEARTBREAK);
        check(helper, heartbreak != null && heartbreak.getAmplifier() == 4, "level should stop at V, was " + heartbreak);
        expectEffect(helper, player, BtRegistries.HEARTBREAK, Curse.HEARTBREAK.durationTicks());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void secondRampageNeedsSixKills(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.RAMPAGE);
        pop(player, Curse.RAMPAGE);
        for (int i = 0; i < 2 * BtRegistries.RAMPAGE_KILLS_NEEDED; i++) {
            check(helper, player.hasEffect(BtRegistries.RAMPAGE), "rampage II lifted after only " + i + " kills");
            ServerPlayer victim = player(helper);
            CommonHooks.onLivingDeath(victim, victim.damageSources().playerAttack(player));
        }
        check(helper, !player.hasEffect(BtRegistries.RAMPAGE), "six kills should lift rampage II");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void cursesComeBackAfterDeathWithAFullTimer(GameTestHelper helper) {
        ServerPlayer dead = player(helper);
        pop(dead, Curse.WEAKNESS);
        pop(dead, Curse.WEAKNESS);
        pop(dead, Curse.HEARTBREAK);
        tick(dead, 20);
        dead.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400)); // a plain effect, not a curse
        ServerPlayer respawned = player(helper);
        CurseMemory.onClone(new PlayerEvent.Clone(respawned, dead, true));
        CurseMemory.onRespawn(new PlayerEvent.PlayerRespawnEvent(respawned, false));
        MobEffectInstance weakness = respawned.getEffect(MobEffects.WEAKNESS);
        check(helper, weakness != null && weakness.getAmplifier() == 1, "weakness II should come back, was " + weakness);
        expectEffect(helper, respawned, MobEffects.WEAKNESS, Curse.WEAKNESS.durationTicks());
        check(helper, respawned.hasEffect(BtRegistries.HEARTBREAK), "heartbreak should come back");
        check(helper, !respawned.hasEffect(MobEffects.BLINDNESS), "a plain effect should not come back");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void expiredZombifiedStaysGoneAfterDeath(GameTestHelper helper) {
        ServerPlayer dead = player(helper);
        dead.addEffect(new MobEffectInstance(BtRegistries.ZOMBIFIED, 2, 0));
        tick(dead, 5);
        check(helper, !dead.isAlive(), "zombification should have killed");
        ServerPlayer respawned = player(helper);
        CurseMemory.onClone(new PlayerEvent.Clone(respawned, dead, true));
        CurseMemory.onRespawn(new PlayerEvent.PlayerRespawnEvent(respawned, false));
        check(helper, !respawned.hasEffect(BtRegistries.ZOMBIFIED), "the curse that killed you should not come back");
        helper.succeed();
    }

    // ---- loneliness ----

    @GameTest(template = EMPTY)
    public static void lonelyIsLiftedByThreeVillagerFriends(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.LONELINESS);
        expectEffect(helper, player, BtRegistries.LONELY, Curse.LONELINESS.durationTicks());
        Villager first = villager(helper);
        trade(player, first);
        trade(player, first); // the same villager counts once
        check(helper, Tallies.count(player, Curse.LONELINESS) == 1, "one villager should be one friend");
        trade(player, villager(helper));
        trade(player, villager(helper));
        check(helper, !player.hasEffect(BtRegistries.LONELY), "three friends should lift Lonely");
        tick(player, 3);
        check(helper, player.isAlive(), "a lifted Lonely never kills");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void secondLonelyNeedsSixFriends(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.LONELINESS);
        pop(player, Curse.LONELINESS);
        for (int i = 0; i < 2 * BtRegistries.RAMPAGE_KILLS_NEEDED; i++) {
            check(helper, player.hasEffect(BtRegistries.LONELY), "Lonely II lifted after only " + i + " friends");
            trade(player, villager(helper));
        }
        check(helper, !player.hasEffect(BtRegistries.LONELY), "six friends should lift Lonely II");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void lonelyRunningOutKills(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.addEffect(new MobEffectInstance(BtRegistries.LONELY, 2, 0));
        tick(player, 5);
        check(helper, !player.isAlive(), "player should die when Lonely runs out");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void playerMobFriendNeedsGiftsBothWays(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.LONELINESS);
        UUID mob = UUID.randomUUID();
        PlayerMobGifts.gift(player, mob, true);
        check(helper, Tallies.count(player, Curse.LONELINESS) == 0, "a gift one way is not a friend yet");
        PlayerMobGifts.gift(player, mob, false);
        check(helper, Tallies.count(player, Curse.LONELINESS) == 1, "gifts both ways make a friend");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void rageCountsVillagersWhenTheyStandIn(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.RAMPAGE);
        Villager villager = villager(helper);
        CommonHooks.onLivingDeath(villager, villager.damageSources().playerAttack(player));
        check(helper, Tallies.count(player, Curse.RAMPAGE) == 1, "a villager kill should count without PlayerMob");
        helper.succeed();
    }

    // ---- pacifism ----

    @GameTest(template = EMPTY)
    public static void hurtingAnythingBreaksTheVow(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.PACIFISM);
        Pig pig = helper.spawn(EntityType.PIG, 2, 1, 2);
        pig.hurt(player.damageSources().playerAttack(player), 1.0F);
        tick(player, 2);
        check(helper, !player.isAlive(), "hurting a pig under the vow should kill");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void takingDamageKeepsTheVow(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.PACIFISM);
        Pig pig = helper.spawn(EntityType.PIG, 2, 1, 2);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(pig), 1.0F);
        tick(player, 2);
        check(helper, player.isAlive(), "being hurt does not break the vow");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void pacifismAddsTimeNotLevels(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.PACIFISM);
        tick(player, 20);
        pop(player, Curse.PACIFISM);
        MobEffectInstance vow = player.getEffect(BtRegistries.VOW_OF_PEACE);
        check(helper, vow != null && vow.getAmplifier() == 0, "the vow has no levels, was " + vow);
        expectEffect(helper, player, BtRegistries.VOW_OF_PEACE, 2 * Curse.PACIFISM.durationTicks() - 20);
        helper.succeed();
    }

    // ---- melon diet and restraint ----

    @GameTest(template = EMPTY)
    public static void eatingBreadBreaksTheMelonDiet(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.MELON);
        EventHooks.onItemUseFinish(player, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY);
        tick(player, 2);
        check(helper, !player.isAlive(), "bread under the melon diet should kill");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void melonAndDrinksKeepTheMelonDiet(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.MELON);
        EventHooks.onItemUseFinish(player, new ItemStack(Items.MELON_SLICE), 0, ItemStack.EMPTY);
        EventHooks.onItemUseFinish(player, new ItemStack(Items.HONEY_BOTTLE), 0, ItemStack.EMPTY);
        EventHooks.onItemUseFinish(player, new ItemStack(Items.MILK_BUCKET), 0, ItemStack.EMPTY);
        tick(player, 2);
        check(helper, player.isAlive(), "melon and drinks are allowed");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void openingABarrelBreaksRestraint(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.RESTRAINT);
        BlockPos barrel = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(barrel, Blocks.BARREL.defaultBlockState());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(barrel), Direction.UP, barrel, false);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, barrel, hit));
        tick(player, 2);
        check(helper, !player.isAlive(), "opening a barrel while restrained should kill");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void restraintAddsTimeNotLevels(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        pop(player, Curse.RESTRAINT);
        tick(player, 20);
        pop(player, Curse.RESTRAINT);
        expectEffect(helper, player, BtRegistries.RESTRAINED, 2 * Curse.RESTRAINT.durationTicks() - 20);
        helper.succeed();
    }

    // ---- loot ----

    @GameTest(template = EMPTY)
    public static void evokersDropBalancedTotems(GameTestHelper helper) {
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
            check(helper, totems == 1, "roll " + roll + " dropped " + totems + " balanced totems, expected 1");
        }
        check(helper, seen.size() > 1, "every roll gave the same totem: " + seen);
        check(helper, emerald, "the emerald pool should still drop");
        evoker.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void killedEvokerDropsABalancedTotem(GameTestHelper helper) {
        ServerPlayer killer = player(helper);
        Evoker evoker = helper.spawn(EntityType.EVOKER, 2, 1, 2);
        evoker.hurt(killer.damageSources().playerAttack(killer), LETHAL);
        check(helper, !evoker.isAlive(), "evoker should die");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, evoker.getBoundingBox().inflate(2));
        check(helper, drops.stream().noneMatch(drop -> drop.getItem().is(Items.TOTEM_OF_UNDYING)), "dropped a vanilla totem");
        check(helper, drops.stream().anyMatch(drop -> drop.getItem().is(EvokerTotemLootModifier.EVOKER_TOTEMS)),
            "no balanced totem among " + drops.stream().map(drop -> drop.getItem().toString()).toList());
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
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BtRegistries.TOTEMS.get(curse).get()));
        return player;
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
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BtRegistries.TOTEMS.get(curse).get()));
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
