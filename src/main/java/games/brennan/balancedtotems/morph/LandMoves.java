package games.brennan.balancedtotems.morph;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Empty-hand "use" moves for land forms, each the mob's own. */
final class LandMoves {

    /** {@code Camel#DASH_COOLDOWN_TICKS}. */
    private static final int DASH_COOLDOWN = 55;
    private static final double DASH_SPEED = 1.4;
    private static final double DASH_LIFT = 0.35;
    private static final int SNOWBALL_COOLDOWN = 10;
    private static final int DIG_COOLDOWN = 1200;
    /** {@code Sniffer}: the seed comes up this long after the dig starts. */
    private static final int DIG_SEED_DELAY = 120;
    private static final int TURTLE_EGG_COOLDOWN = 1200;
    private static final int IMITATE_COOLDOWN = 20;

    private LandMoves() {}

    /** A camel's dash: a long lunge where you're looking. */
    static void dash(ServerPlayer player) {
        if (!player.onGround() || !FormTimers.ready(player, "dash", DASH_COOLDOWN)) return;
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        player.setDeltaMovement(look.scale(DASH_SPEED).add(0.0, DASH_LIFT, 0.0));
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.level().playSound(null, player.blockPosition(), SoundEvents.CAMEL_DASH, SoundSource.PLAYERS, 1.0F, 1.0F);
        FormTimers.show(player, FormAction.DASH);
    }

    /** A cat sits (and stands again when you move). */
    static void sit(ServerPlayer player) {
        FormTimers.show(player, FormAction.SIT);
    }

    /** A bat hangs upside down (and lets go when you move). */
    static void hang(ServerPlayer player) {
        FormTimers.show(player, FormAction.HANG);
    }

    static void croak(ServerPlayer player) {
        if (!FormTimers.ready(player, "croak", 60)) return;
        player.level().playSound(null, player.blockPosition(), SoundEvents.FROG_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);
        FormTimers.show(player, FormAction.CROAK);
    }

    /** {@code Sniffer}: dig in soft ground and, a moment later, bring up an ancient seed. */
    static void dig(ServerPlayer player) {
        BlockPos below = player.blockPosition().below();
        if (!player.level().getBlockState(below).is(BlockTags.SNIFFER_DIGGABLE_BLOCK)) return;
        if (!FormTimers.ready(player, "dig", DIG_COOLDOWN)) return;
        player.level().playSound(null, player.blockPosition(), SoundEvents.SNIFFER_DIGGING, SoundSource.PLAYERS, 1.0F, 1.0F);
        FormTimers.show(player, FormAction.DIG);
        FormTimers.later(player, DIG_SEED_DELAY, LandMoves::dropSeed);
    }

    private static void dropSeed(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SNIFFER_DIGGING);
        LootParams params = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, player.position())
            .withParameter(LootContextParams.THIS_ENTITY, player)
            .create(LootContextParamSets.GIFT);
        for (ItemStack stack : table.getRandomItems(params)) {
            level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SNIFFER_DROP_SEED, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** {@code SnowGolem#performRangedAttack}, aimed where you look. */
    static void throwSnowball(ServerPlayer player) {
        if (!FormTimers.ready(player, "snowball", SNOWBALL_COOLDOWN)) return;
        Snowball snowball = new Snowball(player.level(), player);
        snowball.setItem(new ItemStack(Items.SNOWBALL));
        snowball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 1.0F);
        player.level().addFreshEntity(snowball);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.PLAYERS, 1.0F, 0.4F);
    }

    /** A turtle lays an egg on sand. */
    static void layTurtleEgg(ServerPlayer player) {
        BlockPos at = player.blockPosition();
        if (!player.level().getBlockState(at.below()).is(BlockTags.SAND) || !player.level().getBlockState(at).isAir()) return;
        if (!FormTimers.ready(player, "turtle_egg", TURTLE_EGG_COOLDOWN)) return;
        player.level().setBlockAndUpdate(at, Blocks.TURTLE_EGG.defaultBlockState());
        player.level().playSound(null, at, SoundEvents.TURTLE_LAY_EGG, SoundSource.PLAYERS, 0.3F, 0.9F);
    }

    /** {@code Parrot#getAmbient}: a nearby hostile mob's voice, or a parrot's own. */
    static void imitate(ServerPlayer player) {
        if (!FormTimers.ready(player, "imitate", IMITATE_COOLDOWN)) return;
        SoundEvent sound = Parrot.getAmbient(player.level(), player.getRandom());
        float pitch = (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F;
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.7F, pitch);
    }
}
