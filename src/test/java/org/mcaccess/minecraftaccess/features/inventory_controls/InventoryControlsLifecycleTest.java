package org.mcaccess.minecraftaccess.features.inventory_controls;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.mixin.AbstractContainerScreenAccessor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("InventoryControls: lifecycle, screen identity and anti-race guards")
class InventoryControlsLifecycleTest {

    @Test
    @DisplayName("isActiveContainerScreen returns false when currentScreen is null")
    void whenCurrentScreenIsNull_isActiveContainerScreenReturnsFalse() {
        InventoryControls controls = new InventoryControls();
        controls.setCurrentScreenForTesting(null);

        AbstractContainerScreen<?> activeScreen = mock(AbstractContainerScreen.class);
        assertFalse(controls.isActiveContainerScreen(activeScreen));
    }

    @Test
    @DisplayName("isActiveContainerScreen returns false when active screen is null")
    void whenActiveScreenIsNull_isActiveContainerScreenReturnsFalse() {
        InventoryControls controls = new InventoryControls();
        AbstractContainerScreenAccessor mockAccessor = mock(AbstractContainerScreenAccessor.class);
        controls.setCurrentScreenForTesting(mockAccessor);

        assertFalse(controls.isActiveContainerScreen(null));
    }

    @Test
    @DisplayName("isActiveContainerScreen returns false when active screen is not a container screen")
    void whenActiveScreenIsNotContainerScreen_isActiveContainerScreenReturnsFalse() {
        InventoryControls controls = new InventoryControls();
        AbstractContainerScreenAccessor mockAccessor = mock(AbstractContainerScreenAccessor.class);
        controls.setCurrentScreenForTesting(mockAccessor);

        Screen nonContainerScreen = mock(Screen.class);
        assertFalse(controls.isActiveContainerScreen(nonContainerScreen));
    }

    @Test
    @DisplayName("isActiveContainerScreen returns false when active screen differs from currentScreen instance")
    void whenActiveScreenDiffersFromCurrentScreen_isActiveContainerScreenReturnsFalse() {
        InventoryControls controls = new InventoryControls();
        AbstractContainerScreen<?> screen1 = mock(AbstractContainerScreen.class, withSettings().extraInterfaces(AbstractContainerScreenAccessor.class));
        AbstractContainerScreen<?> screen2 = mock(AbstractContainerScreen.class, withSettings().extraInterfaces(AbstractContainerScreenAccessor.class));

        controls.setCurrentScreenForTesting((AbstractContainerScreenAccessor) screen1);

        assertFalse(controls.isActiveContainerScreen(screen2), "Different screen instance must be rejected");
    }

    @Test
    @DisplayName("isActiveContainerScreen returns true only when active screen is the exact matching container instance")
    void whenActiveScreenMatchesCurrentScreen_isActiveContainerScreenReturnsTrue() {
        InventoryControls controls = new InventoryControls();
        AbstractContainerScreen<?> screen = mock(AbstractContainerScreen.class, withSettings().extraInterfaces(AbstractContainerScreenAccessor.class));

        controls.setCurrentScreenForTesting((AbstractContainerScreenAccessor) screen);

        assertTrue(controls.isActiveContainerScreen(screen), "Same container screen instance must be accepted");
    }

    @Test
    @DisplayName("clearNavigationState resets tracked screens, slot items and invalidates screen activity")
    void clearNavigationState_resetsAllTrackingState() {
        InventoryControls controls = new InventoryControls();
        AbstractContainerScreen<?> screen = mock(AbstractContainerScreen.class, withSettings().extraInterfaces(AbstractContainerScreenAccessor.class));

        controls.setCurrentScreenForTesting((AbstractContainerScreenAccessor) screen);
        assertTrue(controls.isActiveContainerScreen(screen));

        controls.clearNavigationState();

        assertNull(controls.getCurrentScreenForTesting());
        assertNull(controls.getCurrentSlotItemForTesting());
        assertFalse(controls.isActiveContainerScreen(screen), "Screen must no longer be considered active after clear");
    }
}
