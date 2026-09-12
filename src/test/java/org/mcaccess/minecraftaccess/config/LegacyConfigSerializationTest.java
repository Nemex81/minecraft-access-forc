package org.mcaccess.minecraftaccess.config;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.utils.config.IdentifierAdapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Legacy Config Serialization Round-Trip Tests (Contratto S2)")
class LegacyConfigSerializationTest {

    @TempDir
    Path tempDir;

    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new GsonBuilder()
                .setFormattingStyle(FormattingStyle.PRETTY.withIndent("    "))
                .registerTypeAdapter(Identifier.class, new IdentifierAdapter())
                .create();
    }

    @Test
    @DisplayName("1. Profilo A: Valori personalizzati reali round-trip su @TempDir")
    void testProfileARoundTrip() throws IOException {
        String inputJson = """
                {
                    "fallDetector": {
                        "range": 6,
                        "depth": 5,
                        "delay": 2500
                    }
                }
                """;

        Config config = gson.fromJson(inputJson, Config.class);
        assertNotNull(config.fallDetector);
        assertEquals(6, config.fallDetector.range);
        assertEquals(5, config.fallDetector.depth);
        assertEquals(2500, config.fallDetector.delay);

        Path file = tempDir.resolve("profile_a.json");
        Files.writeString(file, gson.toJson(config), StandardCharsets.UTF_8);

        String reloadedJson = Files.readString(file, StandardCharsets.UTF_8);
        Config reloaded = gson.fromJson(reloadedJson, Config.class);

        assertEquals(6, reloaded.fallDetector.range);
        assertEquals(5, reloaded.fallDetector.depth);
        assertEquals(2500, reloaded.fallDetector.delay);
    }

    @Test
    @DisplayName("2. Profilo B: Valori personalizzati alternativi round-trip su @TempDir")
    void testProfileBRoundTrip() throws IOException {
        String inputJson = """
                {
                    "fallDetector": {
                        "range": 7,
                        "depth": 3,
                        "delay": 2500
                    }
                }
                """;

        Config config = gson.fromJson(inputJson, Config.class);
        assertNotNull(config.fallDetector);
        assertEquals(7, config.fallDetector.range);
        assertEquals(3, config.fallDetector.depth);
        assertEquals(2500, config.fallDetector.delay);

        Path file = tempDir.resolve("profile_b.json");
        Files.writeString(file, gson.toJson(config), StandardCharsets.UTF_8);

        String reloadedJson = Files.readString(file, StandardCharsets.UTF_8);
        Config reloaded = gson.fromJson(reloadedJson, Config.class);

        assertEquals(7, reloaded.fallDetector.range);
        assertEquals(3, reloaded.fallDetector.depth);
        assertEquals(2500, reloaded.fallDetector.delay);
    }

    @Test
    @DisplayName("3. Profilo C: Valori limite zero round-trip su @TempDir")
    void testProfileCRoundTrip() throws IOException {
        String inputJson = """
                {
                    "fallDetector": {
                        "range": 0,
                        "depth": 0,
                        "delay": 0
                    }
                }
                """;

        Config config = gson.fromJson(inputJson, Config.class);
        assertNotNull(config.fallDetector);
        assertEquals(0, config.fallDetector.range);
        assertEquals(0, config.fallDetector.depth);
        assertEquals(0, config.fallDetector.delay);

        Path file = tempDir.resolve("profile_c.json");
        Files.writeString(file, gson.toJson(config), StandardCharsets.UTF_8);

        String reloadedJson = Files.readString(file, StandardCharsets.UTF_8);
        Config reloaded = gson.fromJson(reloadedJson, Config.class);

        assertEquals(0, reloaded.fallDetector.range);
        assertEquals(0, reloaded.fallDetector.depth);
        assertEquals(0, reloaded.fallDetector.delay);
    }

    @Test
    @DisplayName("4. Profilo D: Assenza campi legacy e campi duali personalizzati round-trip su @TempDir")
    void testProfileDRoundTrip() throws IOException {
        String inputJson = """
                {
                    "fallDetector": {
                        "proximityMaxRange": 4,
                        "longRangeScanInterval": 4000
                    }
                }
                """;

        Config config = gson.fromJson(inputJson, Config.class);
        assertNotNull(config.fallDetector);
        // Default di classe per i campi legacy
        assertEquals(6, config.fallDetector.range);
        assertEquals(4, config.fallDetector.depth);
        assertEquals(2500, config.fallDetector.delay);
        // Campi duali personalizzati
        assertEquals(4, config.fallDetector.proximityMaxRange);
        assertEquals(4000, config.fallDetector.longRangeScanInterval);

        Path file = tempDir.resolve("profile_d.json");
        Files.writeString(file, gson.toJson(config), StandardCharsets.UTF_8);

        String reloadedJson = Files.readString(file, StandardCharsets.UTF_8);
        Config reloaded = gson.fromJson(reloadedJson, Config.class);

        // Verifica congiunta post-riscrittura su disco
        assertEquals(6, reloaded.fallDetector.range);
        assertEquals(4, reloaded.fallDetector.depth);
        assertEquals(2500, reloaded.fallDetector.delay);
        assertEquals(4, reloaded.fallDetector.proximityMaxRange);
        assertEquals(4000, reloaded.fallDetector.longRangeScanInterval);
    }

    @Test
    @DisplayName("5. Verifica riflessiva annotazione @ConfigEntry.Gui.Excluded e assenza modificatore transient")
    void testReflectionExcludedAndNotTransient() throws NoSuchFieldException {
        Class<Config.FallDetector> clazz = Config.FallDetector.class;

        String[] fields = {"range", "depth", "delay"};
        for (String fieldName : fields) {
            Field field = clazz.getField(fieldName);
            assertNotNull(field, "Il campo " + fieldName + " deve esistere");
            assertTrue(field.isAnnotationPresent(ConfigEntry.Gui.Excluded.class),
                    "Il campo " + fieldName + " deve avere @ConfigEntry.Gui.Excluded");
            assertFalse(Modifier.isTransient(field.getModifiers()),
                    "Il campo " + fieldName + " NON deve essere transient");
        }
    }
}
