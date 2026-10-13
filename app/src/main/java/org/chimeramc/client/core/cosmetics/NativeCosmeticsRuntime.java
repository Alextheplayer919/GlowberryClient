package org.chimeramc.client.core.cosmetics;

import android.content.Context;

import org.chimeramc.client.preloader.PreloaderInput;

/**
 * Boots the native cosmetics system and keeps it in step with the equipped set.
 *
 * <p>There is exactly one place that initialises cosmetics now: this class. It publishes the
 * equipped cape/accessory/pet into the native registry and installs the sink that renders a peer's
 * advertised cosmetics, so the renderer always has the current data with no resource pack and no
 * world reload. {@code LauncherApplication} calls {@link #init} once at process start, and
 * {@link #syncEquipped} is called before every launch and on every equip.
 *
 * <p>Fail-closed throughout: without the native library every call is a no-op and the preview still
 * shows the equipped set.
 */
public final class NativeCosmeticsRuntime {

    private static volatile boolean initialized;
    private static CosmeticStore store;
    /** The application context, kept so the per-frame VFX preference read has a context. */
    private static volatile Context appContext;

    private NativeCosmeticsRuntime() {
    }

    /**
     * Initialises the native cosmetics system: publishes the equipped set and installs the
     * peer-advert sink. Idempotent, so it is safe to call from both {@code Application.onCreate} and
     * a game session start.
     */
    public static void init(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        if (store == null) store = new CosmeticStore(context);
        // Install the render sink even if a previous init ran: the sync module clears nothing here,
        // and re-installing is cheap.
        CosmeticSyncModule.setAdvertListener(new CosmeticSyncModule.AdvertListener() {
            @Override
            public void onAdvert(CosmeticSyncProtocol.Advert advert) {
                renderPeerAdvert(advert);
            }

            @Override
            public void onPeerLeft(String peerId) {
                // Drop a departed peer's cape/hat/pet so their cosmetics do not linger on our client.
                try {
                    NativeCosmeticsBridge.publishPeer(peerId, null, null, null);
                } catch (Throwable ignored) {
                }
            }
        });
        if (initialized) return;
        initialized = true;
        syncEquipped(context);
    }

    /**
     * Publishes the currently equipped cosmetics into the native registry. Called at startup, before
     * every launch, and whenever the player changes a cosmetic.
     */
    public static void syncEquipped(Context context) {
        try {
            CosmeticStore s = store != null ? store : (context == null ? null
                    : (store = new CosmeticStore(context)));
            if (s == null) return;
            // The Context-aware publish carries each piece's authored geometry and its own texture,
            // which is what the native renderer actually reads to draw the cape, hat and pet.
            NativeCosmeticsBridge.publishLocal(context, s.getEquippedCapeForDisplay(),
                    s.getEquippedAccessory(), s.getEquippedPet());
        } catch (Throwable ignored) {
            // A cosmetic must never take the process down; the preview still shows the selection.
        }
    }

    /**
     * Renders a received peer advertisement: resolves the advertised catalogue ids against the local
     * catalogue and uploads the pixels, keyed by the peer's id, so the renderer can draw their
     * cosmetic. Runs on the sync receive thread.
     */
    private static void renderPeerAdvert(CosmeticSyncProtocol.Advert advert) {
        if (advert == null || advert.peerId.isEmpty()) return;
        try {
            NativeCosmeticsBridge.publishPeer(advert.peerId,
                    CosmeticCatalog.equippedCape(advert.capeId),
                    CosmeticCatalog.equippedAccessory(advert.accessoryId),
                    CosmeticCatalog.equippedPet(advert.petId));
        } catch (Throwable ignored) {
            // A bad payload must not stop the receive loop.
        }
    }

    /** True once {@link #init} has run. */
    public static boolean isInitialized() {
        return initialized;
    }

    // --- Per-frame transform push (the Java half of the native render path) -------------------

    private static final PetPose FRAME_POSE = new PetPose();
    private static CosmeticCatalog.PetLocomotion frameGait = CosmeticCatalog.PetLocomotion.WALK;
    private static float framePetPhase;
    private static long lastFrameMs;
    private static double frameDistanceMoved;
    private static float[] lastPosition;
    private static float lastYaw;

    /** The gait the in-game pet animates with; set from the Mod Menu / instance settings. */
    public static void setFrameGait(CosmeticCatalog.PetLocomotion gait) {
        frameGait = gait == null ? CosmeticCatalog.PetLocomotion.WALK : gait;
    }

    /**
     * Whether the particle/aura VFX is enabled, read from the inbuilt-mod preferences.
     *
     * <p>Fail-closed: an unreadable preference or a null context reads as off, so the VFX is opt-in
     * and never spawns unless the player turned it on.
     */
    private static boolean isVfxEnabled() {
        try {
            android.content.Context context = appContext;
            if (context == null) return false;
            return org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager
                    .getInstance(context).isCosmeticVfxEnabled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Computes and pushes one frame of cosmetic transforms, called from the game's own frame tick.
     *
     * <p>This is where the launcher's animation state ({@link PetPose}, {@link CapeAnimationCurve})
     * is turned into the byte buffer the native render hook draws from. It reads the local player's
     * position/rotation (the same feed the nametag icons use) to derive speed, distance moved and
     * yaw, advances the pet gait phase, computes the pose, packs a {@link CosmeticFrame} and pushes
     * it. Fail-closed: with no cosmetics equipped the frame is empty, and a build without the native
     * symbol is a no-op.
     */
    public static void tickFrame(Context context, long nowMs) {
        try {
            CosmeticStore s = store != null ? store : (context == null ? null
                    : (store = new CosmeticStore(context)));
            if (s == null) return;
            CosmeticCatalog.Cape cape = s.getEquippedCapeForDisplay();
            CosmeticCatalog.Accessory accessory = s.getEquippedAccessory();
            CosmeticCatalog.Pet pet = s.getEquippedPet();
            if (cape == null && accessory == null && pet == null) {
                PreloaderInput.pushCosmeticFrame(CosmeticFrame.empty().toBytes());
                return;
            }

            // Derive motion from the local-player feed. A missing read leaves the player still, so
            // the cape hangs and the pet idles rather than jittering on stale values.
            float moveSpeed = 0f;
            float verticalSpeed = 0f;
            float yaw = lastYaw;
            float[] pos = PreloaderInput.readLocalPlayerPosition();
            if (pos != null && lastPosition != null && lastFrameMs != 0L) {
                float dt = (nowMs - lastFrameMs) / 1000f;
                if (dt > 0f && dt < 0.5f) {
                    float dx = pos[0] - lastPosition[0];
                    float dy = pos[1] - lastPosition[1];
                    float dz = pos[2] - lastPosition[2];
                    double horizontal = Math.sqrt(dx * dx + dz * dz);
                    frameDistanceMoved += horizontal;
                    moveSpeed = (float) Math.min(1.0, horizontal / dt / 5.0);
                    verticalSpeed = (float) Math.max(-1.5, Math.min(1.5, dy / dt));
                }
            }
            if (pos != null) lastPosition = new float[]{pos[0], pos[1], pos[2]};
            float[] rot = PreloaderInput.readLocalPlayerRotation();
            if (rot != null) {
                yaw = rot[0];
            }
            lastYaw = yaw;
            lastFrameMs = nowMs;

            boolean jumping = pos != null && lastPosition != null && verticalSpeed > 0.1f;

            PetPose pose = null;
            if (pet != null) {
                float dt = 1f / 60f;
                framePetPhase += PetPose.cyclesPerSecond(frameGait) * dt;
                if (framePetPhase > 1f) framePetPhase -= (float) Math.floor(framePetPhase);
                FRAME_POSE.compute(pet.species, frameGait, framePetPhase);
                pose = FRAME_POSE;
            }

            CosmeticFrame frame = CosmeticFrame.build(cape, accessory, pet, pose,
                    moveSpeed, jumping, verticalSpeed, frameDistanceMoved, 0.0, yaw,
                    0f, 0f, CapeGeometry.SEGMENT_COUNT, isVfxEnabled());
            PreloaderInput.pushCosmeticFrame(frame.toBytes());
        } catch (Throwable ignored) {
            // A cosmetic frame must never take the game down; the next tick retries.
        }
    }
}
