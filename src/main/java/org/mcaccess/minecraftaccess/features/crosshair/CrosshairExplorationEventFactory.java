package org.mcaccess.minecraftaccess.features.crosshair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SourceDomain;
import org.mcaccess.minecraftaccess.features.cognitive.SpatialDirection;
import org.mcaccess.minecraftaccess.features.cognitive.StateSignature;

/**
 * Pure, deterministic factory creating CognitiveEvent instances for continuous crosshair exploration.
 * Enforces canonical ID derivation from block/entity types, sets PASSIVE priority,
 * and maintains canChain = false to prevent interference with Phase 3B specialized obstacle compositions.
 */
public final class CrosshairExplorationEventFactory {
    public static final String SEMANTIC_KEY_TARGET = "exploration.crosshair.target";
    public static final String SEMANTIC_KEY_DISTANCE = "exploration.crosshair.distance";
    public static final long PASSIVE_TTL_MS = 2000L;

    private CrosshairExplorationEventFactory() {
    }

    /**
     * Pure factory method creating an exploration crosshair CognitiveEvent.
     */
    public static @Nullable CognitiveEvent createEvent(
            @NotNull String semanticKey,
            @Nullable String canonicalId,
            @Nullable BlockPos targetPos,
            double distance,
            int distanceBucket,
            @NotNull String narrationText,
            long now
    ) {
        if (narrationText.isBlank()) {
            return null;
        }

        StateSignature signature = StateSignature.of(distanceBucket, 0, canonicalId);

        return new CognitiveEvent(
                SourceDomain.EXPLORATION,
                CognitivePriority.PASSIVE,
                semanticKey,
                signature,
                narrationText,
                targetPos,
                distance,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                PASSIVE_TTL_MS,
                false, // canChain = false: obstacle compositions from Phase 3B must dominate
                now
        );
    }

    /**
     * Extracts canonical registry identifier string (e.g. "minecraft:stone" or "minecraft:cow").
     */
    public static @Nullable String extractCanonicalId(@Nullable HitResult rayCast, @Nullable Level level) {
        if (rayCast == null) {
            return null;
        }
        try {
            if (rayCast instanceof BlockHitResult blockHit && level != null) {
                Block block = level.getBlockState(blockHit.getBlockPos()).getBlock();
                Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                return id.toString();
            } else if (rayCast instanceof EntityHitResult entityHit) {
                Identifier id = EntityType.getKey(entityHit.getEntity().getType());
                return id.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Extracts target BlockPos when available.
     */
    public static @Nullable BlockPos extractTargetPos(@Nullable HitResult rayCast) {
        if (rayCast == null) {
            return null;
        }
        if (rayCast instanceof BlockHitResult blockHit) {
            return blockHit.getBlockPos();
        } else if (rayCast instanceof EntityHitResult entityHit) {
            return entityHit.getEntity().blockPosition();
        }
        return null;
    }
}
