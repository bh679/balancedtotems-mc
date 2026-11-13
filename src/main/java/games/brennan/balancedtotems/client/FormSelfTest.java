package games.brennan.balancedtotems.client;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.morph.FormAction;
import games.brennan.balancedtotems.morph.FormUse;
import games.brennan.balancedtotems.morph.MobGaits;
import games.brennan.balancedtotems.morph.MorphForm;
import games.brennan.balancedtotems.morph.Morphs;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dev-only check of every morph form in a real client ({@code ./gradlew runSelfTest}; does nothing unless
 * the {@code balancedtotems.selftest} system property is set). In a singleplayer world it builds a test
 * area, then for each form idles, walks, sprints, jumps, attacks a pig and right-clicks, pressing the real
 * keys, saving screenshots to {@code screenshots/selftest_*} and measuring each stand-in's walk-cycle
 * rate (a real armadillo walks alongside the armadillo form for comparison). The report goes to
 * {@code selftest/report.txt}, then the client quits.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID, value = Dist.CLIENT)
public final class FormSelfTest {

    private static final boolean ENABLED = System.getProperty("balancedtotems.selftest") != null;
    private static final int WARM_UP_TICKS = 100;
    private static final int TICKS_PER_FORM = 140;
    /** The test area: a grass platform, a water pool to its south and a lava pool to its north, high up. */
    private static final BlockPos LAND = new BlockPos(0, 200, 0);
    private static final BlockPos WATER = new BlockPos(0, 199, -20);
    private static final BlockPos LAVA = new BlockPos(0, 200, 20);
    /** A strip far enough off that the player's sprinting doesn't scare the real armadillo into a ball. */
    private static final BlockPos FAR = new BlockPos(0, 200, 60);

    private static int tick;
    private static List<EntityType<?>> forms;
    private static final List<String> REPORT = new ArrayList<>();
    private static final List<Float> walkRates = new ArrayList<>();
    private static final List<Float> realRates = new ArrayList<>();
    private static final List<Double> walkSteps = new ArrayList<>();
    private static final List<Double> sprintSteps = new ArrayList<>();
    private static Vec3 lastPlayerPosition;
    private static float lastPosition = Float.NaN;
    /** Times the form left the ground while walking and sprinting (a rabbit's hops). */
    private static int hops;
    private static boolean wasOnGround = true;
    private static float lastRealPosition = Float.NaN;
    private static int pigId = -1;
    private static int realArmadilloId = -1;

    private FormSelfTest() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft minecraft = Minecraft.getInstance();
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (minecraft.player == null || server == null) return;
        if (minecraft.screen != null) minecraft.setScreen(null);
        tick++;
        if (tick == WARM_UP_TICKS) setUp(server);
        if (tick == WARM_UP_TICKS + 10) build(server); // once the area's chunks are loaded
        if (tick <= WARM_UP_TICKS + 30) return;
        int t = tick - WARM_UP_TICKS - 31; // from 0
        int index = t / TICKS_PER_FORM;
        if (index >= forms.size()) {
            finish(minecraft);
            return;
        }
        step(minecraft, server, forms.get(index), t % TICKS_PER_FORM);
    }

    private static void setUp(MinecraftServer server) {
        String only = System.getProperty("balancedtotems.selftest.forms", ""); // e.g. "polar_bear,armadillo"
        forms = BuiltInRegistries.ENTITY_TYPE.stream().filter(type -> Morphs.formData(type).isPresent())
            .filter(type -> only.isEmpty() || List.of(only.split(",")).contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath()))
            .toList();
        REPORT.add("forms: " + forms.size());
        run(server, "effect clear @a"); // a curse left in the world (an ending Morphed) would undo the forms
        run(server, "clear @a"); // an empty hand: right-click is the form's move, not the item's
        run(server, "difficulty peaceful");
        run(server, "gamemode survival @a");
        run(server, "time set day");
        run(server, "weather clear");
        run(server, "gamerule doDaylightCycle false");
        run(server, "gamerule doWeatherCycle false");
        run(server, "gamerule doMobSpawning false");
        run(server, "effect give @a minecraft:resistance infinite 255 true");
        run(server, "effect give @a minecraft:saturation infinite 255 true");
        run(server, String.format("tp @a %d %d %d", LAND.getX(), LAND.getY() + 2, LAND.getZ()));
        Minecraft.getInstance().options.pauseOnLostFocus = false;
    }

    private static void build(MinecraftServer server) {
        fill(server, LAND.offset(-30, -1, -10), LAND.offset(10, -1, 10), "grass_block");
        fill(server, LAND.offset(-30, 0, -10), LAND.offset(10, 6, 10), "air");
        fill(server, WATER.offset(-30, -4, -6), WATER.offset(10, 1, 6), "water");
        fill(server, WATER.offset(-31, -5, -7), WATER.offset(11, -5, 7), "stone");
        fill(server, LAVA.offset(-30, -2, -6), LAVA.offset(10, -1, 6), "lava");
        fill(server, LAVA.offset(-31, -3, -7), LAVA.offset(11, -3, 7), "stone");
        fill(server, FAR.offset(-30, -1, -2), FAR.offset(10, -1, 2), "grass_block");
    }

    /** One form's run: idle, walk, sprint, jump, attack, use (twice, for toggles like the armadillo). */
    private static void step(Minecraft minecraft, MinecraftServer server, EntityType<?> form, int t) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(form).getPath();
        Player player = minecraft.player;
        if (t == 0) begin(server, player, form);
        if (t == 5) {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT); // its face, and its legs as it walks past
            minecraft.options.hideGui = true;
        }
        if (t == 15) {
            shot(minecraft, name, "1_idle");
            if (Morphs.formOf(player).orElse(null) != form) REPORT.add(name + ": client sees form " + Morphs.formOf(player).orElse(null));
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
            if (serverPlayer != null && Morphs.formOf(serverPlayer).orElse(null) != form) {
                REPORT.add(name + ": server has form " + Morphs.formOf(serverPlayer).orElse(null) + ", effects " + serverPlayer.getActiveEffects());
            }
        }
        if (t == 16) face(player, 90.0F, 10.0F); // walk west, facing the camera
        if (t >= 16 && t < 56) minecraft.options.keyUp.setDown(true);
        if (t >= 36 && t < 56) minecraft.options.keySprint.setDown(true);
        if (t >= 20 && t < 56) {
            measure(minecraft, form);
            if (wasOnGround && !player.onGround()) hops++;
            wasOnGround = player.onGround();
        }
        if (t == 20 || t == 55) ageAt(minecraft, form, t == 20);
        if (t == 25 || t == 45) lastPlayerPosition = null; // past the speed-up into each gait
        if (t >= 25 && t < 36) step(player, walkSteps);
        if (t >= 45 && t < 56) step(player, sprintSteps);
        if (form == EntityType.ARMADILLO && t % 10 == 0) server.execute(() -> steerRealArmadillo(server));
        if (t == 30) shot(minecraft, name, "2_walk");
        if (t == 50) shot(minecraft, name, "3_sprint");
        if (t == 56) {
            release(minecraft);
            report(name, form);
            server.execute(() -> home(server, player.getUUID(), form));
        }
        if (t == 70) {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK); // over its shoulder, at the pig
            server.execute(() -> spawnPig(server, form));
        }
        if (t == 62) minecraft.options.keyJump.setDown(true);
        if (t == 63) minecraft.options.keyJump.setDown(false);
        if (t == 68) shot(minecraft, name, "4_jump");
        if (t == 76) aimAtPig(minecraft);
        if (t == 78) bodyTurn(minecraft, name);
        if (t == 80) {
            REPORT.add(name + ": attack aims at " + (minecraft.hitResult == null ? "nothing" : minecraft.hitResult.getType())
                + ", pig health " + pigHealth(minecraft));
            KeyMapping.click(minecraft.options.keyAttack.getKey());
        }
        if (t == 90) REPORT.add(name + ": pig health after attack " + pigHealth(minecraft));
        if (t == 84) {
            FormAnimator.takeLastAction(player.getUUID());
            face(player, player.getYRot(), -80.0F); // the sky: an attack on nothing
        }
        if (t == 85) KeyMapping.click(minecraft.options.keyAttack.getKey());
        if (t == 89) {
            FormAction shown = FormAnimator.takeLastAction(player.getUUID());
            REPORT.add(name + ": attack on air shows " + shown + String.format(", tilt %.0f deg", FormAnimator.tiltDegrees(player.getUUID(), 0.0F)));
        }
        if (t == 91) aimAtPig(minecraft);
        if (t == 83) shot(minecraft, name, "5_attack");
        if (t == 90) shot(minecraft, name, "6_attack_after");
        if (t == 95) KeyMapping.click(minecraft.options.keyUse.getKey());
        if (t == 98) shot(minecraft, name, "7_use");
        if (t == 112) shot(minecraft, name, "8_use_after");
        if (t == 118 && form != EntityType.ARMADILLO) KeyMapping.click(minecraft.options.keyUse.getKey());
        if (form == EntityType.ARMADILLO && t >= 118 && t < 124) minecraft.options.keyUp.setDown(true); // pushing to move unrolls it
        if (form == EntityType.ARMADILLO && t == 124) minecraft.options.keyUp.setDown(false);
        if (t == 112) {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
            if (MorphRenderer.standIn(player) instanceof net.minecraft.world.entity.animal.camel.Camel camel) REPORT.add(name + ": " + (camel.isCamelSitting() ? "sits on right-click" : "DIDN'T sit"));
            if (MorphRenderer.standIn(player) instanceof net.minecraft.world.entity.animal.Cat cat) REPORT.add(name + ": " + (cat.isInSittingPose() ? "sits on right-click" : "DIDN'T sit"));
            if (form == EntityType.ARMADILLO) REPORT.add(name + ": " + (serverPlayer != null && FormUse.isRolled(serverPlayer) ? "rolled up on right-click" : "DIDN'T roll up"));
        }
        if (form == EntityType.ARMADILLO && t == 136) {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
            REPORT.add(name + ": " + (serverPlayer != null && FormUse.isRolled(serverPlayer) ? "still rolled after moving" : "unrolled when moved"));
        }
        if (t == 121) shot(minecraft, name, "9_use_again");
        if (t == 136) shot(minecraft, name, "10_use_again_after");
        if (t == TICKS_PER_FORM - 1) server.execute(() -> discardTestMobs(server));
    }

    private static void begin(MinecraftServer server, Player player, EntityType<?> form) {
        walkRates.clear();
        realRates.clear();
        walkSteps.clear();
        hops = 0;
        sprintSteps.clear();
        lastPlayerPosition = null;
        lastPosition = Float.NaN;
        lastRealPosition = Float.NaN;
        server.execute(() -> {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
            if (serverPlayer == null || !serverPlayer.isAlive()) {
                REPORT.add(BuiltInRegistries.ENTITY_TYPE.getKey(form).getPath() + ": no living server player to morph");
                return;
            }
            Morphs.setForm(serverPlayer, form);
            home(server, player.getUUID(), form);
            if (form == EntityType.ARMADILLO) spawnRealArmadillo(server);
        });
    }

    /** Back to the form's start, facing south. */
    private static void home(MinecraftServer server, UUID id, EntityType<?> form) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null) return;
        BlockPos start = start(form);
        player.teleportTo(player.serverLevel(), start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0.0F, 15.0F);
    }

    /** Water forms start in the pool, the strider on the lava, the rest on the grass. */
    private static BlockPos start(EntityType<?> form) {
        MorphForm.Pool pool = Morphs.formData(form).map(MorphForm::pool).orElse(MorphForm.Pool.LAND);
        if (pool == MorphForm.Pool.WATER) return WATER.below();
        return form == EntityType.STRIDER ? LAVA : LAND;
    }

    /** A fresh, still pig 2.5 blocks south of the form's start, to attack. */
    private static void spawnPig(MinecraftServer server, EntityType<?> form) {
        ServerLevel level = server.overworld();
        Mob pig = EntityType.PIG.create(level);
        if (pig == null) return;
        pig.setNoAi(true);
        BlockPos start = start(form);
        pig.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 3.0, 180.0F, 0.0F);
        level.addFreshEntity(pig);
        pigId = pig.getId();
    }

    /** A real armadillo walking west beside the form, for the walk-cycle comparison. */
    private static void spawnRealArmadillo(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Armadillo armadillo = EntityType.ARMADILLO.create(level);
        if (armadillo == null) return;
        armadillo.moveTo(FAR.getX() + 8.5, FAR.getY(), FAR.getZ() + 0.5, 90.0F, 0.0F);
        level.addFreshEntity(armadillo);
        realArmadilloId = armadillo.getId();
        steerRealArmadillo(server);
    }

    /** Keeps the real armadillo walking west at its own walk speed. */
    private static void steerRealArmadillo(MinecraftServer server) {
        if (!(server.overworld().getEntity(realArmadilloId) instanceof Armadillo armadillo)) return;
        armadillo.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(FAR.offset(-28, 0, 0), 1.0F, 0));
    }

    private static void discardTestMobs(MinecraftServer server) {
        Entity armadillo = server.overworld().getEntity(realArmadilloId);
        if (armadillo != null) armadillo.discard();
        realArmadilloId = -1;
        Entity pig = server.overworld().getEntity(pigId);
        if (pig != null) pig.discard();
        pigId = -1;
    }

    /** Walk-cycle advance per tick, the rate a mob's legs move at. */
    private static void measure(Minecraft minecraft, EntityType<?> form) {
        LivingEntity standIn = MorphRenderer.standIn(minecraft.player);
        if (standIn != null && standIn.getType() == form) {
            float position = standIn.walkAnimation.position();
            if (!Float.isNaN(lastPosition)) walkRates.add(position - lastPosition);
            lastPosition = position;
        }
        if (minecraft.level != null && minecraft.level.getEntity(realArmadilloId) instanceof LivingEntity real) {
            float position = real.walkAnimation.position();
            if (!Float.isNaN(lastRealPosition)) realRates.add(position - lastRealPosition);
            lastRealPosition = position;
        }
    }

    /** Ground covered since last tick (the first tick of a phase only sets the start). */
    private static void step(Player player, List<Double> steps) {
        Vec3 now = player.position();
        if (lastPlayerPosition != null) steps.add(now.subtract(lastPlayerPosition).horizontalDistance());
        lastPlayerPosition = now;
    }

    private static void report(String name, EntityType<?> form) {
        StringBuilder line = new StringBuilder(name).append(": walk-cycle/tick ").append(stats(walkRates));
        if (!realRates.isEmpty()) line.append(", real armadillo ").append(stats(realRates));
        line.append(String.format(", walks %.2f b/s, sprints %.2f b/s, leaves the ground %d times", perSecond(walkSteps), perSecond(sprintSteps), hops));
        MobGaits.Gait gait = MobGaits.of(form);
        if (gait != null && form.create(Minecraft.getInstance().level) instanceof LivingEntity mob) {
            double mobSpeed = mob.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            line.append(String.format(" (its mob: %.2f, %.2f)", MobGaits.toBlocksPerSecond(MobGaits.walkSpeed(gait, mobSpeed)),
                MobGaits.toBlocksPerSecond(MobGaits.runSpeed(gait, mobSpeed))));
        }
        REPORT.add(line.toString());
    }

    private static double perSecond(List<Double> steps) {
        return steps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 20.0;
    }

    /** Mean and the largest tick-to-tick jump in rate (a jumpy rate reads as shaking). */
    private static String stats(List<Float> rates) {
        if (rates.isEmpty()) return "none";
        float sum = 0.0F;
        float jerk = 0.0F;
        for (int i = 0; i < rates.size(); i++) {
            sum += rates.get(i);
            if (i > 0) jerk = Math.max(jerk, Math.abs(rates.get(i) - rates.get(i - 1)));
        }
        return String.format("mean %.3f, max jerk %.3f, n %d", sum / rates.size(), jerk, rates.size());
    }

    private static void finish(Minecraft minecraft) {
        release(minecraft);
        REPORT.add("animation failures: " + FormAnimator.failures());
        try {
            Path out = minecraft.gameDirectory.toPath().resolve("selftest");
            Files.createDirectories(out);
            Files.write(out.resolve("report.txt"), REPORT);
        } catch (IOException e) {
            BalancedTotems.LOGGER.error("Self-test report couldn't be written", e);
        }
        BalancedTotems.LOGGER.info("Self-test done:\n{}", String.join("\n", REPORT));
        minecraft.stop();
    }

    private static int ageStart;

    /** A stand-in ages one tick a tick, as mobs in a world do (models animate on it). */
    private static void ageAt(Minecraft minecraft, EntityType<?> form, boolean start) {
        LivingEntity standIn = MorphRenderer.standIn(minecraft.player);
        if (standIn == null || standIn.getType() != form) return;
        if (start) ageStart = standIn.tickCount;
        else REPORT.add(BuiltInRegistries.ENTITY_TYPE.getKey(form).getPath() + ": ages " + (standIn.tickCount - ageStart) / 35.0 + " a tick");
    }

    /** After turning to face the pig, the body faces the same way. */
    private static void bodyTurn(Minecraft minecraft, String name) {
        LivingEntity standIn = MorphRenderer.standIn(minecraft.player);
        if (standIn == null) return;
        float off = Math.abs(net.minecraft.util.Mth.wrapDegrees(standIn.yBodyRot - minecraft.player.getYRot()));
        REPORT.add(String.format("%s: body %.0f degrees off where it faces", name, off));
    }

    private static String pigHealth(Minecraft minecraft) {
        return minecraft.level != null && minecraft.level.getEntity(pigId) instanceof LivingEntity pig ? String.valueOf(pig.getHealth()) : "gone";
    }

    /** Turns the player's eyes to the pig's middle, so the attack lands on it if it's in reach. */
    private static void aimAtPig(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || !(minecraft.level.getEntity(pigId) instanceof LivingEntity pig)) return;
        Vec3 to = pig.getBoundingBox().getCenter().subtract(player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(to.y, to.horizontalDistance()));
        face(player, yaw, pitch);
    }

    private static void face(Player player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yHeadRot = yaw;
    }

    private static void release(Minecraft minecraft) {
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keySprint.setDown(false);
        minecraft.options.keyJump.setDown(false);
    }

    private static void shot(Minecraft minecraft, String form, String moment) {
        Screenshot.grab(minecraft.gameDirectory, "selftest_" + form + "_" + moment + ".png", minecraft.getMainRenderTarget(), message -> {});
    }

    private static void run(MinecraftServer server, String command) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command));
    }

    private static void fill(MinecraftServer server, BlockPos from, BlockPos to, String block) {
        run(server, String.format("fill %d %d %d %d %d %d minecraft:%s", from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), block));
    }
}
