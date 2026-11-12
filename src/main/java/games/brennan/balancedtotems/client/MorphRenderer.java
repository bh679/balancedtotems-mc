package games.brennan.balancedtotems.client;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.morph.Morphs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws a morphed player as their form: the player's submit is cancelled and a client-only dummy of
 * the mob is extracted to a render state and submitted in its place, posed from the player every frame
 * and animated every tick.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID, value = Dist.CLIENT)
public final class MorphRenderer {

    /** One dummy per morphed player, rebuilt when their form changes. */
    private static final Map<UUID, LivingEntity> DUMMIES = new ConcurrentHashMap<>();

    private MorphRenderer() {}

    static void forget(Player player) {
        DUMMIES.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        AvatarRenderState playerState = event.getRenderState();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(playerState.id) instanceof Player player)) return;
        LivingEntity dummy = dummyFor(player);
        if (dummy == null) return;

        event.setCanceled(true);
        poseLike(dummy, player);
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderState formState = dispatcher.extractEntity(dummy, event.getPartialTick());
        if (formState instanceof LivingEntityRenderState living) {
            // The player's state, not the entity: the inventory preview turns the body in the state only.
            living.bodyRot = playerState.bodyRot;
            living.yRot = playerState.yRot;
            living.xRot = playerState.xRot;
        }
        formState.lightCoords = playerState.lightCoords;
        formState.outlineColor = playerState.outlineColor;
        formState.shadowPieces.clear();
        CameraRenderState camera = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        dispatcher.getRenderer(formState).submit(formState, event.getPoseStack(), event.getSubmitNodeCollector(), camera);
    }

    /** A morphed player has no hands to show in first person. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && Morphs.formOf(player).isPresent()) event.setCanceled(true);
    }

    /** Walk animation needs movement per tick, which only a tick sees. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            DUMMIES.clear();
            return;
        }
        for (Player player : level.players()) {
            LivingEntity dummy = DUMMIES.get(player.getUUID());
            if (dummy == null) continue;
            dummy.setOldPosAndRot();
            dummy.setPos(player.getX(), player.getY(), player.getZ());
            dummy.calculateEntityAnimation(false);
            dummy.tickCount = player.tickCount;
        }
        DUMMIES.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
    }

    private static LivingEntity dummyFor(Player player) {
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == null) {
            DUMMIES.remove(player.getUUID());
            return null;
        }
        LivingEntity existing = DUMMIES.get(player.getUUID());
        if (existing != null && existing.getType() == form) return existing;
        if (!(form.create(player.level(), EntitySpawnReason.LOAD) instanceof LivingEntity created)) return null;
        // 26.x renderers read the entity id; real ids are positive, so a negative one never clashes.
        created.setId(-1 - player.getId());
        created.setPos(player.getX(), player.getY(), player.getZ());
        created.setOldPosAndRot();
        DUMMIES.put(player.getUUID(), created);
        return created;
    }

    /** Copies look, body turn, motion state and held/worn items from the player. */
    private static void poseLike(LivingEntity dummy, Player player) {
        dummy.setYRot(player.getYRot());
        dummy.yRotO = player.yRotO;
        dummy.setXRot(player.getXRot());
        dummy.xRotO = player.xRotO;
        dummy.yBodyRot = player.yBodyRot;
        dummy.yBodyRotO = player.yBodyRotO;
        dummy.yHeadRot = player.yHeadRot;
        dummy.yHeadRotO = player.yHeadRotO;
        dummy.setOnGround(player.onGround());
        dummy.setShiftKeyDown(player.isShiftKeyDown());
        dummy.setInvisible(player.isInvisible());
        dummy.hurtTime = player.hurtTime;
        dummy.deathTime = player.deathTime;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ANIMAL_ARMOR) continue;
            dummy.setItemSlot(slot, player.getItemBySlot(slot));
        }
    }
}
