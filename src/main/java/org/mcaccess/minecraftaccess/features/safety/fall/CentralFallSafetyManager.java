package org.mcaccess.minecraftaccess.features.safety.fall;

import java.time.Clock;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.autowalk.MovementCoordinator;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.ModifierUtils;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

@Slf4j
public class CentralFallSafetyManager implements BalmClientModule {

    @Getter
    private final ProximityFallDetector proximityDetector;
    @Getter
    private final LongRangeFallDetector longRangeDetector;
    private final Clock clock;
    private final java.util.function.Supplier<Config.FallDetector> configSupplier;
    private long proximitySuppressionUntilMs = 0;

    // Seam per testabilità headless di AutoWalk
    static java.util.function.BooleanSupplier autoWalkStateSupplier = MovementCoordinator::isAutoWalkActive;

    public static void setAutoWalkStateSupplier(java.util.function.BooleanSupplier supplier) {
        autoWalkStateSupplier = supplier;
    }

    public static void resetTestSeams() {
        autoWalkStateSupplier = MovementCoordinator::isAutoWalkActive;
        ProximityFallDetector.resetTestSeams();
        LongRangeFallDetector.resetTestSeams();
    }

    public CentralFallSafetyManager() {
        this(Clock.systemDefaultZone(), new ProximityFallDetector(), new LongRangeFallDetector());
    }

    public CentralFallSafetyManager(Clock clock, ProximityFallDetector proximityDetector, LongRangeFallDetector longRangeDetector) {
        this(clock, proximityDetector, longRangeDetector, () -> {
            Config cfg = Config.getInstance();
            return cfg != null && cfg.fallDetector != null ? cfg.fallDetector : new Config.FallDetector();
        });
    }

    public CentralFallSafetyManager(
            Clock clock,
            ProximityFallDetector proximityDetector,
            LongRangeFallDetector longRangeDetector,
            java.util.function.Supplier<Config.FallDetector> configSupplier
    ) {
        this.clock = clock;
        this.proximityDetector = proximityDetector;
        this.longRangeDetector = longRangeDetector;
        this.configSupplier = configSupplier;
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "fall_detector");
    }

    @Override
    public void initialize() {
        ClientPlayingTick.AFTER.register(this::tick);

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "fall_detector.inspect_fall"))
                .withDefault(InputBinding.key(InputConstants.KEY_F, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    if (!ModifierUtils.hasAltOnly()) return false;
                    proximityDetector.inspectNearbyFalls();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "fall_detector.toggle_auto_sneak"))
                .withDefault(InputBinding.key(InputConstants.KEY_F, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    if (!ModifierUtils.hasControlAndAlt()) return false;
                    proximityDetector.toggleAutoSneak();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "other.repeat_last_narration"))
                .withDefault(InputBinding.key(InputConstants.KEY_G, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    if (!ModifierUtils.hasAltOnly()) return false;
                    MainClass.repeatLastNarration();
                    return true;
                })
                .build();
    }

    public void tick(Minecraft client, Player player, Level level) {
        Config cfg = Config.getInstance();
        Config.FallDetector config = configSupplier != null ? configSupplier.get() : null;
        if (config == null) {
            config = cfg != null && cfg.fallDetector != null ? cfg.fallDetector : new Config.FallDetector();
        }

        if (!config.enabled) {
            proximityDetector.resetSafetyState();
            return;
        }

        if (client.gui != null && client.gui.screen() != null) {
            proximityDetector.resetSafetyStateForGui();
            return;
        }

        if (player.isUnderWater() || player.isInWater() || player.isInWaterOrRain() || player.isSwimming() || player.isVisuallySwimming() || player.isEyeInFluid(FluidTags.WATER)) {
            proximityDetector.resetSafetyState();
            return;
        }

        boolean autoWalkActive = autoWalkStateSupplier.getAsBoolean();
        boolean silenceFallVoice = cfg != null && cfg.autoWalk != null && cfg.autoWalk.silenceFallWarningsDuringWalk;

        // 1. Esecuzione del Corto Raggio (Prossimità cinetica ad alta frequenza)
        ProximityFallDetector.ProximityStatus proxStatus = proximityDetector.tick(
                client,
                player,
                level,
                autoWalkActive,
                silenceFallVoice
        );
        proximityDetector.getMovementGuard().reconcileCrouchState();

        // 2. Regola di Quiete Reciproca: allarme o discesa in corso silenziano il lungo raggio per 2000 ms
        long now = clock.millis();
        if (proxStatus.isAlertOrActiveDescent()) {
            proximitySuppressionUntilMs = now + 2000L;
        }
        boolean isSuppressedByProximity = (now < proximitySuppressionUntilMs);

        // 3. Esecuzione del Lungo Raggio (Radar orografico a bassa frequenza)
        longRangeDetector.tick(
                client,
                player,
                level,
                isSuppressedByProximity,
                autoWalkActive
        );
    }
}
