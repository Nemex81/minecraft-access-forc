package org.mcaccess.minecraftaccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Config Cognitive Settings Binding & I18N Tests (ASTRALIS v2.5.5 Phase 2)")
class ConfigCognitiveSettingsTest {

    @BeforeEach
    void setUp() {
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.setChainedNarrationEnabled(true);
        CognitiveCoordinator.setDeduplicationWindowMs(1500);
    }

    @AfterEach
    void tearDown() {
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.resetDelegates();
    }

    @Test
    @DisplayName("1. Binding applies supported config parameters to CognitiveCoordinator")
    void testBindingAppliesSupportedConfigParameters() {
        Config.CognitiveSettings cfg = new Config.CognitiveSettings();
        cfg.cognitiveCoordinatorEnabled = false;
        cfg.explorationCognitiveRoutingEnabled = false;
        cfg.chainedNarrationEnabled = false;
        cfg.deduplicationWindowMs = 2500;

        Config.applyCognitiveSettings(cfg);

        assertFalse(CognitiveCoordinator.isCoordinatorEnabled(), "Coordinator enabled flag must be false");
        assertFalse(CognitiveCoordinator.isExplorationRoutingEnabled(), "Exploration routing flag must be false");
        assertFalse(CognitiveCoordinator.isExplorationRoutingActive(), "Exploration routing active helper must be false");
        assertFalse(CognitiveCoordinator.isChainedNarrationEnabled(), "Chained narration flag must be false");
        assertEquals(2500, CognitiveCoordinator.getDeduplicationWindowMs(), "Deduplication window must be 2500ms");

        // Re-enable
        cfg.cognitiveCoordinatorEnabled = true;
        cfg.explorationCognitiveRoutingEnabled = true;
        cfg.chainedNarrationEnabled = true;
        cfg.deduplicationWindowMs = 1200;

        Config.applyCognitiveSettings(cfg);

        assertTrue(CognitiveCoordinator.isCoordinatorEnabled());
        assertTrue(CognitiveCoordinator.isExplorationRoutingEnabled());
        assertTrue(CognitiveCoordinator.isExplorationRoutingActive());
        assertTrue(CognitiveCoordinator.isChainedNarrationEnabled());
        assertEquals(1200, CognitiveCoordinator.getDeduplicationWindowMs());
    }

    @Test
    @DisplayName("2. Deduplication window is clamped and normalized between 500ms and 5000ms")
    void testDeduplicationWindowIsNormalized() {
        Config.CognitiveSettings cfg = new Config.CognitiveSettings();

        // Test lower bound clamp
        cfg.deduplicationWindowMs = 100;
        Config.applyCognitiveSettings(cfg);
        assertEquals(500, CognitiveCoordinator.getDeduplicationWindowMs(), "Values below 500ms must clamp to 500ms");

        // Test upper bound clamp
        cfg.deduplicationWindowMs = 8000;
        Config.applyCognitiveSettings(cfg);
        assertEquals(5000, CognitiveCoordinator.getDeduplicationWindowMs(), "Values above 5000ms must clamp to 5000ms");
    }

    @Test
    @DisplayName("3. All cognitiveCoordinator I18N keys exist and appear in strict alphabetical order in JSON files")
    void testI18nKeysAlphabeticalAndComplete() throws IOException {
        List<String> expectedKeys = List.of(
                "text.autoconfig.minecraft-access.category.cognitiveCoordinator",
                "text.autoconfig.minecraft-access.option.cognitiveCoordinator.chainedNarrationEnabled",
                "text.autoconfig.minecraft-access.option.cognitiveCoordinator.chainedNarrationEnabled.@Tooltip",
                "text.autoconfig.minecraft-access.option.cognitiveCoordinator.cognitiveCoordinatorEnabled",
                "text.autoconfig.minecraft-access.option.cognitiveCoordinator.cognitiveCoordinatorEnabled.@Tooltip",
                "text.autoconfig.minecraft-access.option.cognitiveCoordinator.deduplicationWindowMs",
                "text.autoconfig.minecraft-access.option.cognitiveCoordinator.deduplicationWindowMs.@Tooltip",
                "text.autoconfig.minecraft-access.option.cognitiveCoordinator.explorationCognitiveRoutingEnabled",
                "text.autoconfig.minecraft-access.option.cognitiveCoordinator.explorationCognitiveRoutingEnabled.@Tooltip"
        );

        Path itJsonPath = Path.of("src/main/resources/assets/minecraft_access/lang/it_it.json");
        Path enJsonPath = Path.of("src/main/resources/assets/minecraft_access/lang/en_us.json");

        assertI18nFileHasKeysInAlphabeticalOrder(itJsonPath, expectedKeys);
        assertI18nFileHasKeysInAlphabeticalOrder(enJsonPath, expectedKeys);
    }

    private void assertI18nFileHasKeysInAlphabeticalOrder(Path path, List<String> expectedKeys) throws IOException {
        String content = Files.readString(path);

        // Verify presence and find indices in file content
        int lastIndex = -1;
        for (String key : expectedKeys) {
            String searchPattern = "\"" + key + "\":";
            int index = content.indexOf(searchPattern);
            assertTrue(index > 0, "Missing required key in " + path.getFileName() + ": " + key);
            assertTrue(index > lastIndex, "Key is out of alphabetical order in " + path.getFileName() + ": " + key);
            lastIndex = index;
        }
    }

    @Test
    @DisplayName("4. Sequential rollout gate: exploration routing active only when both coordinator and exploration flag are true")
    void testExplorationRoutingSequentialGate() {
        // 1. Both true -> active
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.setExplorationRoutingEnabled(true);
        assertTrue(CognitiveCoordinator.isExplorationRoutingActive());

        // 2. Exploration false -> inactive
        CognitiveCoordinator.setExplorationRoutingEnabled(false);
        assertFalse(CognitiveCoordinator.isExplorationRoutingActive());

        // 3. Coordinator false, exploration true -> inactive
        CognitiveCoordinator.setCoordinatorEnabled(false);
        CognitiveCoordinator.setExplorationRoutingEnabled(true);
        assertFalse(CognitiveCoordinator.isExplorationRoutingActive());

        // 4. Both false -> inactive
        CognitiveCoordinator.setCoordinatorEnabled(false);
        CognitiveCoordinator.setExplorationRoutingEnabled(false);
        assertFalse(CognitiveCoordinator.isExplorationRoutingActive());
    }

    @Test
    @DisplayName("5. Lifecycle guard: null player or level skips flush safely")
    void testLifecycleGuardSkipsNull() {
        // When player or level is null, tick handling must gracefully return without throwing or flushing
        assertDoesNotThrow(() -> CognitiveCoordinator.handleClientTick(null, null, 1000L));
    }
}
