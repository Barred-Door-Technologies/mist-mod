package com.mistsmp.mod.client.relic;

import com.mistsmp.mod.client.config.MistSettings;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Mist wisps and the occasional gold spark around relics that players are holding or wearing.
 * Client-side only, like cosmetic particles: every client spawns what it sees, with no packets.
 *
 * Same skip rules as cosmetic particles: players further than 48 blocks, invisible players,
 * spectators, other players while the K toggle is off, and your own relics in first person.
 * Vanilla's Particles setting still applies on top. A Bag of Holding only shows them while collecting.
 */
public final class RelicParticles {

    public static final SimpleParticleType WISP = FabricParticleTypes.simple();
    public static final SimpleParticleType SPARK = FabricParticleTypes.simple();

    private static final double MAX_DISTANCE = 48.0;
    private static final float WISPS_PER_SECOND = 1.2F;
    private static final float SPARKS_PER_SECOND = 0.25F;

    /** Height of each worn slot as a fraction of the player's height, and how far from the center it sits. */
    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final double[] WORN_HEIGHT = {0.92, 0.62, 0.38, 0.06};
    private static final double[] WORN_RADIUS = {0.3, 0.32, 0.28, 0.26};

    private RelicParticles() {
    }

    public static void init() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("mistsmp", "relic_wisp"), WISP);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("mistsmp", "relic_spark"), SPARK);
        ParticleProviderRegistry.getInstance().register(WISP, RelicParticle::wisp);
        ParticleProviderRegistry.getInstance().register(SPARK, RelicParticle::spark);
    }

    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) return;

        Vec3 camera = minecraft.gameRenderer.mainCamera().position();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        boolean showOthers = MistSettings.get().showOtherPlayersParticles();
        RandomSource random = level.getRandom();

        for (AbstractClientPlayer player : level.players()) {
            boolean self = player == minecraft.player;
            if (self ? firstPerson : !showOthers) continue;
            if (player.isInvisible() || player.isSpectator()) continue;
            if (player.position().distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) continue;

            double yaw = Math.toRadians(player.yBodyRot);
            // The player's right-hand side, and the direction they face.
            double rightX = -Math.cos(yaw);
            double rightZ = -Math.sin(yaw);
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            double height = player.getBbHeight();
            double mainSide = player.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0;

            emitHand(level, random, player.getMainHandItem(), player, mainSide, rightX, rightZ, forwardX, forwardZ, height);
            emitHand(level, random, player.getOffhandItem(), player, -mainSide, rightX, rightZ, forwardX, forwardZ, height);

            for (int i = 0; i < WORN.length; i++) {
                ItemStack worn = player.getItemBySlot(WORN[i]);
                if (!RelicItems.showsEffects(worn)) continue;
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = WORN_RADIUS[i];
                emit(level, random,
                        player.getX() + Math.cos(angle) * radius,
                        player.getY() + height * WORN_HEIGHT[i] + (random.nextDouble() - 0.5) * 0.15,
                        player.getZ() + Math.sin(angle) * radius);
            }
        }
    }

    private static void emitHand(ClientLevel level, RandomSource random, ItemStack stack, AbstractClientPlayer player, double side,
                                 double rightX, double rightZ, double forwardX, double forwardZ, double height) {
        if (!RelicItems.showsEffects(stack)) return;
        double x = player.getX() + rightX * 0.38 * side + forwardX * 0.12;
        double y = player.getY() + height * 0.42;
        double z = player.getZ() + rightZ * 0.38 * side + forwardZ * 0.12;
        emit(level, random,
                x + (random.nextDouble() - 0.5) * 0.15,
                y + (random.nextDouble() - 0.5) * 0.25,
                z + (random.nextDouble() - 0.5) * 0.15);
    }

    private static void emit(ClientLevel level, RandomSource random, double x, double y, double z) {
        if (roll(WISPS_PER_SECOND, random)) {
            level.addParticle(WISP, x, y, z,
                    (random.nextDouble() - 0.5) * 0.01, 0.008 + random.nextDouble() * 0.008, (random.nextDouble() - 0.5) * 0.01);
        }
        if (roll(SPARKS_PER_SECOND, random)) {
            level.addParticle(SPARK, x, y, z,
                    (random.nextDouble() - 0.5) * 0.06, 0.03 + random.nextDouble() * 0.04, (random.nextDouble() - 0.5) * 0.06);
        }
    }

    /** True on average perSecond times a second. */
    private static boolean roll(float perSecond, RandomSource random) {
        return random.nextFloat() < perSecond / 20F;
    }
}