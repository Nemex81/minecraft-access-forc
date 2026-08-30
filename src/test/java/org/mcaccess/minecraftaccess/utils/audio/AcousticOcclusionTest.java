package org.mcaccess.minecraftaccess.utils.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcousticOcclusionTest {

    @Test
    void testFloorVolumeClamp() {
        assertEquals(0.01f, AcousticOcclusion.MIN_VOLUME_FLOOR, 0.0001f);
        assertEquals(0.20f, AcousticOcclusion.OCCLUSION_THRESHOLD, 0.0001f);
    }

    @Test
    void testNullLevelSafety() {
        assertEquals(0.0f, AcousticOcclusion.calculateTotalOcclusion(null, null, null));
        assertEquals(1.0f, AcousticOcclusion.getVolumeMultiplier(null, null, null));
        assertFalse(AcousticOcclusion.isOccluded(null, null, null));
    }

    @Test
    void testNullBlockState() {
        assertEquals(0.0f, AcousticOcclusion.calculateBlockAttenuation(null));
    }
}
