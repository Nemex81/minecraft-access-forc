package org.mcaccess.minecraftaccess.features.point_of_interest.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WaypointTest {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    @DisplayName("Verify Waypoint record creation and properties")
    void testWaypointRecord() {
        BlockPos pos = new BlockPos(100, 64, -200);
        Identifier dim = Identifier.fromNamespaceAndPath("minecraft", "overworld");
        long now = System.currentTimeMillis();

        Waypoint wp = new Waypoint("wp-1", "Main Base", pos, dim, WaypointType.CUSTOM, now);

        assertEquals("wp-1", wp.id());
        assertEquals("Main Base", wp.name());
        assertEquals(pos, wp.pos());
        assertEquals(dim, wp.dimension());
        assertEquals(WaypointType.CUSTOM, wp.type());
        assertEquals(now, wp.timestamp());
    }

    @Test
    @DisplayName("Verify WaypointType enum values")
    void testWaypointTypes() {
        assertEquals(3, WaypointType.values().length);
        assertNotNull(WaypointType.valueOf("CUSTOM"));
        assertNotNull(WaypointType.valueOf("DEATH"));
        assertNotNull(WaypointType.valueOf("BED"));
    }

    @Test
    @DisplayName("Verify JSON Serialization and Deserialization roundtrip")
    void testJsonSerializationRoundtrip() {
        BlockPos pos = new BlockPos(-500, 72, 1250);
        Identifier dim = Identifier.fromNamespaceAndPath("minecraft", "the_nether");
        long timestamp = 1700000000000L;

        Waypoint original = new Waypoint("test-uuid-123", "Nether Portal", pos, dim, WaypointType.DEATH, timestamp);

        // Serialize to DTO
        WaypointManager.WaypointDTO dto = new WaypointManager.WaypointDTO();
        dto.id = original.id();
        dto.name = original.name();
        dto.x = original.pos().getX();
        dto.y = original.pos().getY();
        dto.z = original.pos().getZ();
        dto.dimension = original.dimension().toString();
        dto.type = original.type().name();
        dto.timestamp = original.timestamp();

        String json = gson.toJson(dto);
        assertNotNull(json);
        assertTrue(json.contains("Nether Portal"));
        assertTrue(json.contains("the_nether"));

        // Deserialize from DTO
        WaypointManager.WaypointDTO parsedDto = gson.fromJson(json, WaypointManager.WaypointDTO.class);
        assertNotNull(parsedDto);
        assertEquals(original.id(), parsedDto.id);
        assertEquals(original.name(), parsedDto.name);
        assertEquals(original.pos().getX(), parsedDto.x);
        assertEquals(original.pos().getY(), parsedDto.y);
        assertEquals(original.pos().getZ(), parsedDto.z);
        assertEquals(original.dimension().toString(), parsedDto.dimension);
        assertEquals(original.type().name(), parsedDto.type);
        assertEquals(original.timestamp(), parsedDto.timestamp);
    }
}
