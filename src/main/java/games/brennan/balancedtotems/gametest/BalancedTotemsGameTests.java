package games.brennan.balancedtotems.gametest;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.Curse;
import games.brennan.balancedtotems.api.BalancedTotemsApi;
import games.brennan.balancedtotems.morph.Morphs;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

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
    public static void voidTotemSavesFromLethalFalls(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.VOID);
        player.hurt(player.damageSources().fall(), LETHAL);
        check(helper, player.isAlive(), "player should survive the fall");
        helper.succeed();
    }

    // ---- morph ----

    @GameTest(template = EMPTY)
    public static void morphingTotemGivesAFittingForm(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.MORPHING);
        player.hurt(player.damageSources().generic(), LETHAL);
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        check(helper, form != null, "player should have a form");
        check(helper, form.is(BtRegistries.MORPH_POOL), "form should come from the pool: " + form);
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
    public static void zombifiedIsCuredByAGoldenApple(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.ZOMBIFIED);
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, Morphs.formOf(player).orElse(null) == EntityType.ZOMBIE, "player should be a zombie");
        ItemStack apple = new ItemStack(Items.GOLDEN_APPLE);
        EventHooks.onItemUseFinish(player, apple, 0, ItemStack.EMPTY);
        check(helper, !player.hasEffect(BtRegistries.ZOMBIFIED), "golden apple should cure");
        check(helper, Morphs.formOf(player).isEmpty(), "form should be gone");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void zombifiedIsCuredBySplashWeakness(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.ZOMBIFIED);
        player.hurt(player.damageSources().generic(), LETHAL);
        var witch = helper.spawn(EntityType.WITCH, new BlockPos(1, 2, 1));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400), witch);
        check(helper, !player.hasEffect(BtRegistries.ZOMBIFIED), "splashed weakness should cure");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void weaknessTotemDoesNotCureZombified(GameTestHelper helper) {
        ServerPlayer player = holding(helper, Curse.ZOMBIFIED);
        player.hurt(player.damageSources().generic(), LETHAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BtRegistries.TOTEMS.get(Curse.WEAKNESS).get()));
        player.invulnerableTime = 0; // vanilla ignores a second hit this soon
        player.hurt(player.damageSources().generic(), LETHAL);
        check(helper, player.hasEffect(BtRegistries.ZOMBIFIED), "a totem's weakness is no cure");
        check(helper, player.hasEffect(MobEffects.WEAKNESS), "weakness applied");
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
