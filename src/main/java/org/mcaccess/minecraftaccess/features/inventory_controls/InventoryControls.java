package org.mcaccess.minecraftaccess.features.inventory_controls;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.mixin.AbstractContainerScreenAccessor;
import org.mcaccess.minecraftaccess.mixin.AbstractRecipeBookScreenAccessor;
import org.mcaccess.minecraftaccess.mixin.AnvilScreenAccessor;
import org.mcaccess.minecraftaccess.mixin.CreativeModeInventoryScreenAccessor;
import org.mcaccess.minecraftaccess.mixin.EditBoxAccessor;
import org.mcaccess.minecraftaccess.mixin.RecipeBookComponentAccessor;
import org.mcaccess.minecraftaccess.mixin.RecipeBookPageAccessor;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.condition.Interval;
import org.mcaccess.minecraftaccess.utils.system.MouseUtils;

/**
 * This features lets us use keyboard in inventory screens. Works with all default minecraft screens.
 *
 * <p>Key binds and combinations:
 * (all key binds are re-mappable(except two keys) from the game's controls menu and these key binds do not interrupt with any other key with same key.)<br>
 * 1) Up Key (default: I) = Focus to slot above.<br>
 * 2) Right Key (default: L) = Focus to slot right.<br>
 * 3) Down Key (default: K) = Focus to slot down.<br>
 * 4) Left Key (default: J) = Focus to slot left.<br>
 * 5) Group Key (default: C) = Select next group.<br>
 * 6) Left Shift + Group Key = Select previous group.<br>
 * 7) Switch Tab Key (default: V) = Select next tab (only for creative inventory screen and inventory/crafting screen).<br>
 * 8) Left Shift + Switch Tab Key = Select previous tab (only for creative inventory screen and inventory/crafting screen).<br>
 * 9) Toggle Craftable Key (default: R) = Toggle between show all and show only craftable recipes in inventory/crafting screen.<br>
 * 10) T Key (not re-mappable) = Select the search box.<br>
 * 11) Enter Key (not re-mappable) = Deselect the search box.
 */
@Slf4j
public class InventoryControls implements BalmClientModule {
    private Config.InventoryControls config;
    private final Interval interval = Interval.defaultDelay();

    private AbstractContainerScreenAccessor previousScreen = null;
    private AbstractContainerScreenAccessor currentScreen = null;

    private List<SlotsGroup> currentSlotsGroupList = null;
    private SlotsGroup currentGroup = null;
    private int currentGroupIndex = 0;
    private SlotItem currentSlotItem = null;
    private RecipeBookComponent<?> currentRecipeBookWidget = null;
    private String previousSlotText = "";
    private int previousQueuedCrafts = 0;
    private int previousCarriedCount = 0;
    private int previousStonecutterOptionsCount = 0;
    private ItemStack previousResultStack = ItemStack.EMPTY;
    private static long lastActionNarrationTime = 0;

    public static boolean isActionRecentlyNarrated() {
        return System.currentTimeMillis() - lastActionNarrationTime < 1500;
    }

    public InventoryControls() {
        loadConfig();
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls");
    }

    @Override
    public void initialize() {
        ClientTickCallback.AFTER.register(this::tick);

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.change_group/previous"))
                .withDefault(InputBinding.key(InputConstants.KEY_C, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    log.debug("Group key pressed");
                    changeGroup(false);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.change_group/next"))
                .withDefault(InputBinding.key(InputConstants.KEY_C))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    log.debug("Group key pressed");
                    changeGroup(true);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.switch_tab/previous"))
                .withDefault(InputBinding.key(InputConstants.KEY_V, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    log.debug("Switch Tab key pressed");
                    if (currentScreen instanceof AbstractRecipeBookScreen<?>) {
                        changeRecipeTab(false);
                        return true;
                    } else if (currentScreen instanceof CreativeModeInventoryScreen) {
                        changeCreativeInventoryTab(false);
                        return true;
                    }
                    return false;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.switch_tab/next"))
                .withDefault(InputBinding.key(InputConstants.KEY_V))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    log.debug("Switch Tab key pressed");
                    if (currentScreen instanceof AbstractRecipeBookScreen<?>) {
                        changeRecipeTab(true);
                        return true;
                    } else if (currentScreen instanceof CreativeModeInventoryScreen) {
                        changeCreativeInventoryTab(true);
                        return true;
                    }
                    return false;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/left"))
                .withDefault(InputBinding.key(InputConstants.KEY_J))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (isSearchBoxFocused()) return false;
                    log.debug("Left key pressed");
                    focusSlotItemAt(FocusDirection.LEFT);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/left_arrow"))
                .withDefault(InputBinding.key(InputConstants.KEY_LEFT))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (isSearchBoxFocused()) return false;
                    log.debug("Left arrow key pressed");
                    focusSlotItemAt(FocusDirection.LEFT);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/right"))
                .withDefault(InputBinding.key(InputConstants.KEY_L))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (isSearchBoxFocused()) return false;
                    log.debug("Right key pressed");
                    focusSlotItemAt(FocusDirection.RIGHT);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/right_arrow"))
                .withDefault(InputBinding.key(InputConstants.KEY_RIGHT))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (isSearchBoxFocused()) return false;
                    log.debug("Right arrow key pressed");
                    focusSlotItemAt(FocusDirection.RIGHT);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/up"))
                .withDefault(InputBinding.key(InputConstants.KEY_I))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (isSearchBoxFocused()) return false;
                    log.debug("Up key pressed");
                    focusSlotItemAt(FocusDirection.UP);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/up_arrow"))
                .withDefault(InputBinding.key(InputConstants.KEY_UP))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (isSearchBoxFocused()) return false;
                    log.debug("Up arrow key pressed");
                    focusSlotItemAt(FocusDirection.UP);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.switch_recipe_book_page/previous"))
                .withDefault(InputBinding.key(InputConstants.KEY_I, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (isSearchBoxFocused()) return false;
                    if (currentGroup != null && currentGroup.isScrollable) {
                        log.debug("Previous Recipe Book page key pressed");
                        if (isRecipeBookOpen()) {
                            clickPreviousRecipeBookPage();
                        } else {
                            MouseUtils.Wheel.UP.scroll();
                        }
                        return true;
                    }
                    return false;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/down"))
                .withDefault(InputBinding.key(InputConstants.KEY_K))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (isSearchBoxFocused()) return false;
                    log.debug("Down key pressed");
                    focusSlotItemAt(FocusDirection.DOWN);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/down_arrow"))
                .withDefault(InputBinding.key(InputConstants.KEY_DOWN))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (isSearchBoxFocused()) return false;
                    log.debug("Down arrow key pressed");
                    focusSlotItemAt(FocusDirection.DOWN);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.switch_recipe_book_page/next"))
                .withDefault(InputBinding.key(InputConstants.KEY_K, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (isSearchBoxFocused()) return false;
                    if (currentGroup != null && currentGroup.isScrollable) {
                        log.debug("Next Recipe Book page key pressed");
                        if (isRecipeBookOpen()) {
                            clickNextRecipeBookPage();
                        } else {
                            MouseUtils.Wheel.DOWN.scroll();
                        }
                        return true;
                    }
                    return false;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.jump_to_textbox"))
                .withDefault(InputBinding.key(InputConstants.KEY_T))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (CreativeModeInventoryScreenAccessor.getSelectedTab().getType() == CreativeModeTab.Type.SEARCH
                            && currentScreen instanceof CreativeModeInventoryScreen creativeInventoryScreen) {
                        setSearchBoxFocus(((CreativeModeInventoryScreenAccessor) creativeInventoryScreen).getSearchBox(), true);
                        return true;
                    } else if (currentScreen instanceof AnvilScreen anvilScreen) {
                        setSearchBoxFocus(((AnvilScreenAccessor) anvilScreen).getName(), true);
                        return true;
                    } else if (isRecipeBookOpen()) {
                        // resolve can-not-enter-characters-issue https://github.com/minecraft-access/minecraft-access/issues/67
                        Minecraft.getInstance().gui.screen().setFocused(currentRecipeBookWidget);
                        setSearchBoxFocus(((RecipeBookComponentAccessor) currentRecipeBookWidget).getSearchBox(), true);
                        return true;
                    }
                    return false;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.toggle_craftable"))
                .withDefault(InputBinding.key(InputConstants.KEY_R))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (currentRecipeBookWidget == null) return false;
                    if (!currentRecipeBookWidget.isVisible()) return false;

                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
                    ((RecipeBookComponentAccessor) currentRecipeBookWidget).getFilterButton().onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));

                    String filterName = ((RecipeBookComponentAccessor) currentRecipeBookWidget).getFilterButton().getValue()
                            ? ((RecipeBookComponentAccessor) currentRecipeBookWidget).callGetRecipeFilterName().getString()
                            : I18n.get("gui.recipebook.toggleRecipes.all");

                    currentSlotsGroupList = GroupGenerator.generateGroupsFromSlots(currentScreen);
                    SlotsGroup recipesGroup = null;
                    if (currentSlotsGroupList != null) {
                        for (int i = 0; i < currentSlotsGroupList.size(); i++) {
                            if ("recipes".equals(currentSlotsGroupList.get(i).getGroupKey())) {
                                currentGroupIndex = i;
                                recipesGroup = currentSlotsGroupList.get(i);
                                currentGroup = recipesGroup;
                                break;
                            }
                        }
                    }

                    RecipeBookComponentAccessor compAccessor = (RecipeBookComponentAccessor) currentRecipeBookWidget;
                    RecipeBookPageAccessor pageAccessor = (RecipeBookPageAccessor) compAccessor.getRecipeBookPage();
                    RecipePageStats stats = getPageStats(pageAccessor);
                    String statsSummary = stats.formatSummary();
                    String prefix = statsSummary.isEmpty() ? filterName : filterName + ", " + statsSummary;

                    if (recipesGroup != null && !recipesGroup.slotItems.isEmpty()) {
                        currentSlotItem = recipesGroup.getFirstGroupItem();
                        moveToSlotItem(currentSlotItem, 100);
                        List<RecipeButton> buttons = pageAccessor.getButtons();
                        if (!buttons.isEmpty() && buttons.getFirst().visible && buttons.getFirst().getCollection() != null) {
                            RecipeButton firstButton = buttons.getFirst();
                            ItemStack displayStack = firstButton.getDisplayStack();
                            String itemName = displayStack.getHoverName().getString();
                            String craftable = firstButton.getCollection().hasCraftable() ? I18n.get("minecraft_access.other.craftable") : I18n.get("minecraft_access.other.not_craftable");
                            String firstRecipeText = "%s %d %s".formatted(craftable, displayStack.getCount(), itemName);
                            String narration = prefix + ". " + firstRecipeText;
                            lastActionNarrationTime = System.currentTimeMillis();
                            previousSlotText = firstRecipeText;
                            MainClass.narrate(narration, true);
                            return true;
                        }
                    }

                    lastActionNarrationTime = System.currentTimeMillis();
                    MainClass.narrate(prefix, true);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.fuel_status"))
                .withDefault(InputBinding.key(InputConstants.KEY_U))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .handleScreenInput(_ -> {
                    if (currentScreen.getMenu() instanceof AbstractFurnaceMenu furnace) {
                        MainClass.narrate(I18n.get("minecraft_access.inventory_controls.fuel_status",
                                Math.round(furnace.getLitProgress() * 100),
                                Math.round(furnace.getBurnProgress() * 100)), true);
                        return true;
                    } else if (currentScreen instanceof BrewingStandScreen brewingStand) {
                        BrewingStandMenu menu = brewingStand.getMenu();
                        MainClass.narrate(I18n.get("minecraft_access.inventory_controls.fuel_status",
                                (menu.getFuel() * 100) / BrewingStandBlockEntity.FUEL_USES,
                                (menu.getBrewingTicks() * 100) / PotionBrewing.BREWING_TIME_SECONDS * 20), true);
                        return true;
                    }
                    return false;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.recipe_info"))
                .withDefault(InputBinding.key(InputConstants.KEY_X))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(_ -> {
                    log.debug("Recipe info key pressed");
                    return narrateRecipeInfo();
                })
                .build();
    }

    private void tick(Minecraft client) {
        if (!interval.isReady()) return;

        if (client.player == null) return;
        if (client.gui.screen() == null) {
            previousScreen = null;
            currentScreen = null;
            currentGroupIndex = 0;
            currentGroup = null;
            currentRecipeBookWidget = null;
            previousQueuedCrafts = 0;
            previousCarriedCount = 0;
            previousStonecutterOptionsCount = 0;
            previousResultStack = ItemStack.EMPTY;
            return;
        }
        if (!(client.gui.screen() instanceof AbstractContainerScreen)) return;
        if (!config.enabled) {
            return;
        }

        loadConfig();
        currentScreen = (AbstractContainerScreenAccessor) client.gui.screen();
        currentRecipeBookWidget = getRecipeBookWidget(client.gui.screen());
        currentSlotsGroupList = GroupGenerator.generateGroupsFromSlots(currentScreen);

        if (!isSearchBoxFocused() && client.gui.screen() != null && client.gui.screen().getFocused() != null) {
            client.gui.screen().setFocused(null);
        }

        interval.adjustNextReadyTimeBy(keyListener());

        // On screen open
        if (previousScreen != currentScreen) {
            previousScreen = currentScreen;
            previousQueuedCrafts = 0;
            previousCarriedCount = 0;
            previousStonecutterOptionsCount = 0;
            previousResultStack = ItemStack.EMPTY;
            if (currentScreen instanceof AnvilScreen anvilScreen) {
                setSearchBoxFocus(((AnvilScreenAccessor) anvilScreen).getName(), false);
            }
            if (currentScreen instanceof CreativeModeInventoryScreen creativeInventoryScreen) {
                EditBox searchBox = ((CreativeModeInventoryScreenAccessor) creativeInventoryScreen).getSearchBox();
                if (searchBox.canConsumeInput()) {
                    setSearchBoxFocus(searchBox, false);
                }
            }

            //<editor-fold desc="Always open recipe book on screen open">
            if (config.autoOpenRecipeBook && currentRecipeBookWidget != null) {
                if (!currentRecipeBookWidget.isVisible()) currentRecipeBookWidget.toggleVisibility();
                setSearchBoxFocus(((RecipeBookComponentAccessor) currentRecipeBookWidget).getSearchBox(), false);
            }
            //</editor-fold>

            refreshGroupListAndSelectFirstGroup(false); // Interrupt is false to let it narrate the screen's name
        }

        if (currentScreen instanceof StonecutterScreen stonecutterScreen) {
            int visibleCount = stonecutterScreen.getMenu().getNumberOfVisibleRecipes();
            if (visibleCount > 0 && previousStonecutterOptionsCount == 0) {
                MainClass.narrate(I18n.get("minecraft_access.inventory_controls.stonecutter_options", visibleCount), false);
            }
            previousStonecutterOptionsCount = visibleCount;
        } else {
            previousStonecutterOptionsCount = 0;
        }

        if (currentSlotsGroupList == null || currentSlotsGroupList.isEmpty()) return;

        int currentQueuedCrafts = getQueuedCraftsCount(currentScreen);
        ItemStack currentResultStack = getResultSlotItem(currentScreen);
        ItemStack currentCarried = currentScreen.getMenu().getCarried();
        int currentCarriedCount = currentCarried.getCount();

        boolean isRecipesGroup = currentGroup != null && "recipes".equals(currentGroup.getGroupKey());
        boolean isCraftingOutputGroup = currentGroup != null && "crafting_output".equals(currentGroup.getGroupKey());
        boolean customNarrationHandled = false;

        if (isRecipesGroup && currentQueuedCrafts > 0 && currentQueuedCrafts != previousQueuedCrafts) {
            if (!currentResultStack.isEmpty()) {
                int totalItems = currentQueuedCrafts * currentResultStack.getCount();
                String itemName = currentResultStack.getHoverName().getString();
                String narration = I18n.get("minecraft_access.inventory_controls.crafting_queued", currentQueuedCrafts, totalItems, itemName);
                previousSlotText = getCurrentSlotNarrationText();
                MainClass.narrate(narration, true);
                customNarrationHandled = true;
            }
        } else if (isCraftingOutputGroup) {
            if (currentCarriedCount > previousCarriedCount && !currentCarried.isEmpty()) {
                int craftedAmount = currentCarriedCount - previousCarriedCount;
                String itemName = currentCarried.getHoverName().getString();
                String narration = I18n.get("minecraft_access.inventory_controls.crafted_items", craftedAmount, itemName, currentCarriedCount);
                previousSlotText = getCurrentSlotNarrationText();
                MainClass.narrate(narration, true);
                customNarrationHandled = true;
            } else if (currentQueuedCrafts < previousQueuedCrafts && currentCarriedCount == previousCarriedCount) {
                int batchesCrafted = previousQueuedCrafts - currentQueuedCrafts;
                int perBatchCount = !previousResultStack.isEmpty() ? previousResultStack.getCount()
                        : (!currentResultStack.isEmpty() ? currentResultStack.getCount() : 1);
                int craftedAmount = batchesCrafted * perBatchCount;
                String itemName = !previousResultStack.isEmpty() ? previousResultStack.getHoverName().getString()
                        : (!currentResultStack.isEmpty() ? currentResultStack.getHoverName().getString() : "");
                if (!itemName.isEmpty()) {
                    String narration = I18n.get("minecraft_access.inventory_controls.crafted_items_to_inventory", craftedAmount, itemName);
                    previousSlotText = getCurrentSlotNarrationText();
                    MainClass.narrate(narration, true);
                    customNarrationHandled = true;
                }
            }
        }

        if (customNarrationHandled) {
            lastActionNarrationTime = System.currentTimeMillis();
        }

        previousQueuedCrafts = currentQueuedCrafts;
        previousCarriedCount = currentCarriedCount;
        if (!currentResultStack.isEmpty()) {
            previousResultStack = currentResultStack.copy();
        } else if (currentQueuedCrafts == 0) {
            previousResultStack = ItemStack.EMPTY;
        }

        if (!customNarrationHandled && config.narrateFocusedSlotChanges) {
            String slotNarrationText = getCurrentSlotNarrationText();
            if (!previousSlotText.equals(slotNarrationText)) {
                previousSlotText = slotNarrationText;
                MainClass.narrate(previousSlotText, true);
            }
        }
    }

    private int getQueuedCraftsCount(@NotNull AbstractContainerScreenAccessor screen) {
        int minCount = Integer.MAX_VALUE;
        boolean hasIngredients = false;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof TransientCraftingContainer) {
                if (slot.hasItem()) {
                    hasIngredients = true;
                    int count = slot.getItem().getCount();
                    if (count < minCount) {
                        minCount = count;
                    }
                }
            }
        }
        return hasIngredients ? minCount : 0;
    }

    private @NotNull ItemStack getResultSlotItem(@NotNull AbstractContainerScreenAccessor screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof ResultContainer && !(slot instanceof FurnaceResultSlot)) {
                if (slot.hasItem()) {
                    return slot.getItem();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private @Nullable RecipeBookComponent<?> getRecipeBookWidget(Screen screen) {
        if (screen instanceof AbstractRecipeBookScreen<?> recipeBookScreen) {
            return ((AbstractRecipeBookScreenAccessor) recipeBookScreen).getRecipeBookComponent();
        }
        return null;
    }

    /**
     * Load configs from config.json.
     */
    private void loadConfig() {
        config = Config.getInstance().inventoryControls;
        interval.setDelay(config.delayMilliseconds, Interval.Unit.MILLISECOND);
    }

    /**
     * Handles the key inputs.
     */
    private boolean keyListener() {
        Minecraft client = Minecraft.getInstance();
        boolean isEnterPressed = InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_RETURN)
                || InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_NUMPADENTER);

        //<editor-fold desc="When using a search box">
        //<editor-fold desc="When using a search box">
        if (currentScreen instanceof CreativeModeInventoryScreen creativeInventoryScreen) {
            EditBox searchBox = ((CreativeModeInventoryScreenAccessor) creativeInventoryScreen).getSearchBox();
            if (searchBox.canConsumeInput()) {
                if (isEnterPressed) {
                    setSearchBoxFocus(searchBox, false);
                    refreshGroupListAndSelectFirstGroup(true);
                    return true;
                }
            }
        }

        if (currentScreen instanceof AnvilScreen anvilScreen) {
            EditBox searchBox = ((AnvilScreenAccessor) anvilScreen).getName();
            if (searchBox.canConsumeInput()) {
                if (isEnterPressed) {
                    setSearchBoxFocus(searchBox, false);
                    previousSlotText = "";
                    return true;
                }
            }
        }

        if (isRecipeBookOpen()) {
            EditBox searchBox = ((RecipeBookComponentAccessor) currentRecipeBookWidget).getSearchBox();
            if (searchBox.canConsumeInput()) {
                if (isEnterPressed) {
                    setSearchBoxFocus(searchBox, false);
                    previousSlotText = "";
                    return true;
                }
            }
        }
        //</editor-fold>

        return false;
    }

    private boolean isRecipeBookOpen() {
        return currentRecipeBookWidget != null && currentRecipeBookWidget.isVisible();
    }

    private boolean isSearchBoxFocused() {
        if (isRecipeBookOpen()) {
            EditBox searchBox = ((RecipeBookComponentAccessor) currentRecipeBookWidget).getSearchBox();
            if (searchBox.canConsumeInput()) {
                return true;
            }
        }
        if (currentScreen instanceof CreativeModeInventoryScreen creativeInventoryScreen) {
            EditBox searchBox = ((CreativeModeInventoryScreenAccessor) creativeInventoryScreen).getSearchBox();
            if (searchBox.canConsumeInput()) {
                return true;
            }
        }
        if (currentScreen instanceof AnvilScreen anvilScreen) {
            EditBox searchBox = ((AnvilScreenAccessor) anvilScreen).getName();
            if (searchBox.canConsumeInput()) {
                return true;
            }
        }
        return false;
    }

    private void clickPreviousRecipeBookPage() {
        if (currentRecipeBookWidget == null || !currentRecipeBookWidget.isVisible()) return;
        RecipeBookComponentAccessor compAccessor = (RecipeBookComponentAccessor) currentRecipeBookWidget;
        RecipeBookPageAccessor pageAccessor = (RecipeBookPageAccessor) compAccessor.getRecipeBookPage();
        int currentPage = pageAccessor.getCurrentPage();
        int totalPages = pageAccessor.getTotalPages();

        if (currentPage <= 0) {
            RecipePageStats stats = getPageStats(pageAccessor);
            String statsSummary = stats.formatSummary();
            String prefix = (totalPages <= 1)
                    ? I18n.get("minecraft_access.inventory_controls.recipe_page_single")
                    : I18n.get("minecraft_access.inventory_controls.recipe_page_first");
            String boundaryInfo = statsSummary.isEmpty() ? prefix : prefix + ", " + statsSummary;

            currentSlotsGroupList = GroupGenerator.generateGroupsFromSlots(currentScreen);
            SlotsGroup recipesGroup = null;
            if (currentSlotsGroupList != null) {
                for (int i = 0; i < currentSlotsGroupList.size(); i++) {
                    if ("recipes".equals(currentSlotsGroupList.get(i).getGroupKey())) {
                        currentGroupIndex = i;
                        recipesGroup = currentSlotsGroupList.get(i);
                        currentGroup = recipesGroup;
                        break;
                    }
                }
            }

            if (recipesGroup != null && !recipesGroup.slotItems.isEmpty()) {
                currentSlotItem = recipesGroup.getFirstGroupItem();
                moveToSlotItem(currentSlotItem, 100);
                List<RecipeButton> buttons = pageAccessor.getButtons();
                if (!buttons.isEmpty() && buttons.getFirst().visible && buttons.getFirst().getCollection() != null) {
                    RecipeButton firstButton = buttons.getFirst();
                    ItemStack displayStack = firstButton.getDisplayStack();
                    String itemName = displayStack.getHoverName().getString();
                    String craftable = firstButton.getCollection().hasCraftable() ? I18n.get("minecraft_access.other.craftable") : I18n.get("minecraft_access.other.not_craftable");
                    String firstRecipeText = "%s %d %s".formatted(craftable, displayStack.getCount(), itemName);
                    String narration = boundaryInfo + ". " + firstRecipeText;
                    lastActionNarrationTime = System.currentTimeMillis();
                    previousSlotText = firstRecipeText;
                    MainClass.narrate(narration, true);
                    return;
                }
            }

            lastActionNarrationTime = System.currentTimeMillis();
            MainClass.narrate(boundaryInfo, true);
            return;
        }

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
        int x = pageAccessor.getBackButton().getX() + 3;
        int y = pageAccessor.getBackButton().getY() + 3;
        MouseUtils.Coordinates p = MouseUtils.calcRealPositionOfWidget(x, y);
        MouseUtils.moveAndLeftClick(p.x(), p.y());

        currentSlotsGroupList = GroupGenerator.generateGroupsFromSlots(currentScreen);
        SlotsGroup recipesGroup = null;
        if (currentSlotsGroupList != null) {
            for (int i = 0; i < currentSlotsGroupList.size(); i++) {
                if ("recipes".equals(currentSlotsGroupList.get(i).getGroupKey())) {
                    currentGroupIndex = i;
                    recipesGroup = currentSlotsGroupList.get(i);
                    currentGroup = recipesGroup;
                    break;
                }
            }
        }

        int newPage = Math.max(0, currentPage - 1);
        RecipePageStats stats = getPageStats(pageAccessor);
        String statsSummary = stats.formatSummary();
        String basePageInfo = I18n.get("minecraft_access.inventory_controls.recipe_page", newPage + 1, totalPages);
        String pageInfo = statsSummary.isEmpty() ? basePageInfo : basePageInfo + ", " + statsSummary;

        if (recipesGroup != null && !recipesGroup.slotItems.isEmpty()) {
            currentSlotItem = recipesGroup.getFirstGroupItem();
            moveToSlotItem(currentSlotItem, 100);
            List<RecipeButton> buttons = pageAccessor.getButtons();
            if (!buttons.isEmpty() && buttons.getFirst().visible && buttons.getFirst().getCollection() != null) {
                RecipeButton firstButton = buttons.getFirst();
                ItemStack displayStack = firstButton.getDisplayStack();
                String itemName = displayStack.getHoverName().getString();
                String craftable = firstButton.getCollection().hasCraftable() ? I18n.get("minecraft_access.other.craftable") : I18n.get("minecraft_access.other.not_craftable");
                String firstRecipeText = "%s %d %s".formatted(craftable, displayStack.getCount(), itemName);
                String narration = pageInfo + ". " + firstRecipeText;
                lastActionNarrationTime = System.currentTimeMillis();
                previousSlotText = firstRecipeText;
                MainClass.narrate(narration, true);
                return;
            }
        }

        lastActionNarrationTime = System.currentTimeMillis();
        MainClass.narrate(pageInfo, true);
    }

    private void clickNextRecipeBookPage() {
        if (currentRecipeBookWidget == null || !currentRecipeBookWidget.isVisible()) return;
        RecipeBookComponentAccessor compAccessor = (RecipeBookComponentAccessor) currentRecipeBookWidget;
        RecipeBookPageAccessor pageAccessor = (RecipeBookPageAccessor) compAccessor.getRecipeBookPage();
        int currentPage = pageAccessor.getCurrentPage();
        int totalPages = pageAccessor.getTotalPages();

        if (currentPage >= totalPages - 1) {
            RecipePageStats stats = getPageStats(pageAccessor);
            String statsSummary = stats.formatSummary();
            String prefix = (totalPages <= 1)
                    ? I18n.get("minecraft_access.inventory_controls.recipe_page_single")
                    : I18n.get("minecraft_access.inventory_controls.recipe_page_last");
            String boundaryInfo = statsSummary.isEmpty() ? prefix : prefix + ", " + statsSummary;

            currentSlotsGroupList = GroupGenerator.generateGroupsFromSlots(currentScreen);
            SlotsGroup recipesGroup = null;
            if (currentSlotsGroupList != null) {
                for (int i = 0; i < currentSlotsGroupList.size(); i++) {
                    if ("recipes".equals(currentSlotsGroupList.get(i).getGroupKey())) {
                        currentGroupIndex = i;
                        recipesGroup = currentSlotsGroupList.get(i);
                        currentGroup = recipesGroup;
                        break;
                    }
                }
            }

            if (recipesGroup != null && !recipesGroup.slotItems.isEmpty()) {
                currentSlotItem = recipesGroup.getFirstGroupItem();
                moveToSlotItem(currentSlotItem, 100);
                List<RecipeButton> buttons = pageAccessor.getButtons();
                if (!buttons.isEmpty() && buttons.getFirst().visible && buttons.getFirst().getCollection() != null) {
                    RecipeButton firstButton = buttons.getFirst();
                    ItemStack displayStack = firstButton.getDisplayStack();
                    String itemName = displayStack.getHoverName().getString();
                    String craftable = firstButton.getCollection().hasCraftable() ? I18n.get("minecraft_access.other.craftable") : I18n.get("minecraft_access.other.not_craftable");
                    String firstRecipeText = "%s %d %s".formatted(craftable, displayStack.getCount(), itemName);
                    String narration = boundaryInfo + ". " + firstRecipeText;
                    lastActionNarrationTime = System.currentTimeMillis();
                    previousSlotText = firstRecipeText;
                    MainClass.narrate(narration, true);
                    return;
                }
            }

            lastActionNarrationTime = System.currentTimeMillis();
            MainClass.narrate(boundaryInfo, true);
            return;
        }

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
        int x = pageAccessor.getForwardButton().getX() + 3;
        int y = pageAccessor.getForwardButton().getY() + 3;
        MouseUtils.Coordinates p = MouseUtils.calcRealPositionOfWidget(x, y);
        MouseUtils.moveAndLeftClick(p.x(), p.y());

        currentSlotsGroupList = GroupGenerator.generateGroupsFromSlots(currentScreen);
        SlotsGroup recipesGroup = null;
        if (currentSlotsGroupList != null) {
            for (int i = 0; i < currentSlotsGroupList.size(); i++) {
                if ("recipes".equals(currentSlotsGroupList.get(i).getGroupKey())) {
                    currentGroupIndex = i;
                    recipesGroup = currentSlotsGroupList.get(i);
                    currentGroup = recipesGroup;
                    break;
                }
            }
        }

        int newPage = Math.min(totalPages - 1, currentPage + 1);
        RecipePageStats stats = getPageStats(pageAccessor);
        String statsSummary = stats.formatSummary();
        String basePageInfo = I18n.get("minecraft_access.inventory_controls.recipe_page", newPage + 1, totalPages);
        String pageInfo = statsSummary.isEmpty() ? basePageInfo : basePageInfo + ", " + statsSummary;

        if (recipesGroup != null && !recipesGroup.slotItems.isEmpty()) {
            currentSlotItem = recipesGroup.getFirstGroupItem();
            moveToSlotItem(currentSlotItem, 100);
            List<RecipeButton> buttons = pageAccessor.getButtons();
            if (!buttons.isEmpty() && buttons.getFirst().visible && buttons.getFirst().getCollection() != null) {
                RecipeButton firstButton = buttons.getFirst();
                ItemStack displayStack = firstButton.getDisplayStack();
                String itemName = displayStack.getHoverName().getString();
                String craftable = firstButton.getCollection().hasCraftable() ? I18n.get("minecraft_access.other.craftable") : I18n.get("minecraft_access.other.not_craftable");
                String firstRecipeText = "%s %d %s".formatted(craftable, displayStack.getCount(), itemName);
                String narration = pageInfo + ". " + firstRecipeText;
                lastActionNarrationTime = System.currentTimeMillis();
                previousSlotText = firstRecipeText;
                MainClass.narrate(narration, true);
                return;
            }
        }

        lastActionNarrationTime = System.currentTimeMillis();
        MainClass.narrate(pageInfo, true);
    }

    /**
     * Focuses a slot item in the specified direction if available.
     *
     * @param focusDirection The direction of the slot item to focus.
     */
    private void focusSlotItemAt(FocusDirection focusDirection) {
        if (currentGroup == null) {
            changeGroup(true);
            return;
        }
        if (currentSlotItem == null) {
            focusSlotItem(currentGroup.getFirstGroupItem(), true);
            return;
        }

        SlotItem slotItem = getGroupItemInDirection(focusDirection);
        if (slotItem == null) {
            MainClass.narrate(I18n.get("minecraft_access.inventory_controls.no_slot_in_direction", I18n.get(focusDirection.getString())), true);
            return;
        }

        focusSlotItem(slotItem, true);
    }

    /**
     * Returns the slot item in the specified direction if available.
     *
     * @param focusDirection The direction of the slot item.
     * @return The object of the slot item if found else null.
     */
    private SlotItem getGroupItemInDirection(FocusDirection focusDirection) {
        switch (focusDirection) {
            case UP -> {
                if (!currentGroup.hasSlotItemAbove(currentSlotItem)) return null;

                if (currentSlotItem.upSlotItem != null) return currentSlotItem.upSlotItem;

                for (SlotItem item : currentGroup.slotItems) {
                    if (item.x == currentSlotItem.x && item.y == currentSlotItem.y - 18) {
                        return item;
                    }
                }
            }
            case RIGHT -> {
                if (!currentGroup.hasSlotItemRight(currentSlotItem)) return null;

                if (currentSlotItem.rightSlotItem != null) return currentSlotItem.rightSlotItem;

                for (SlotItem item : currentGroup.slotItems) {
                    if (item.x == currentSlotItem.x + 18 && item.y == currentSlotItem.y) {
                        return item;
                    }
                }
            }
            case DOWN -> {
                if (!currentGroup.hasSlotItemBelow(currentSlotItem)) return null;

                if (currentSlotItem.downSlotItem != null) return currentSlotItem.downSlotItem;

                for (SlotItem item : currentGroup.slotItems) {
                    if (item.x == currentSlotItem.x && item.y == currentSlotItem.y + 18) {
                        return item;
                    }
                }
            }
            case LEFT -> {
                if (!currentGroup.hasSlotItemLeft(currentSlotItem)) return null;

                if (currentSlotItem.leftSlotItem != null) return currentSlotItem.leftSlotItem;

                for (SlotItem item : currentGroup.slotItems) {
                    if (item.x == currentSlotItem.x - 18 && item.y == currentSlotItem.y) {
                        return item;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Focuses at the specified slot item in the current group and narrate its details.
     *
     * @param slotItem  The object of the slot item to focus.
     * @param interrupt Whether to stop the narrator from narrating the previous message or not.
     */
    private void focusSlotItem(@NotNull SlotItem slotItem, boolean interrupt) {
        currentSlotItem = slotItem;
        moveToSlotItem(currentSlotItem);

        String narration = getCurrentSlotNarrationText();
        if (!narration.isEmpty()) {
            previousSlotText = narration;
            MainClass.narrate(narration, interrupt);
        }
    }

    /**
     * Moves the mouse cursor over to the slot item specified.
     *
     * @param slotItem The object of the slot item to move the mouse cursor over to.
     */
    private void moveToSlotItem(SlotItem slotItem) {
        if (slotItem == null) return;

        int x = slotItem.x;
        int y = slotItem.y;

        MouseUtils.Coordinates p = MouseUtils.calcRealPositionOfWidget(currentScreen.getLeftPos() + x, currentScreen.getTopPos() + y);
        MouseUtils.move(p.x(), p.y());
    }

    /**
     * Moves the mouse cursor over to the specified slot item after some delay.
     *
     * @param slotItem The object of the slot item to move the mouse cursor over to.
     * @param delay    The delay in milliseconds.
     */
    @SuppressWarnings("SameParameterValue")
    private void moveToSlotItem(SlotItem slotItem, int delay) {
        if (slotItem == null) return;

        int x = slotItem.x;
        int y = slotItem.y;

        MouseUtils.Coordinates p = MouseUtils.calcRealPositionOfWidget(currentScreen.getLeftPos() + x, currentScreen.getTopPos() + y);
        MouseUtils.moveAfterDelay(p.x(), p.y(), delay);
    }

    /**
     * Get the details of the current slot item to narrate.
     *
     * @return The details of the current slot item.
     */
    private String getCurrentSlotNarrationText() {
        if (currentSlotItem == null) return "";

        Slot slot = currentSlotItem.slot;
        if (slot == null) {
            return Objects.requireNonNullElse(currentSlotItem.getNarratableText(), I18n.get("minecraft_access.inventory_controls.Unknown"));
        }
        if (!slot.hasItem()) {
            return I18n.get("minecraft_access.inventory_controls.empty_slot", currentGroup.getSlotPrefix(slot));
        }

        ItemStack itemStack = slot.getItem();
        // <slot row col prefix> <count>
        String info = "%s %s".formatted(currentGroup.getSlotPrefix(slot),
                (itemStack.getCount() != 1 && !itemStack.isEmpty()) ? String.valueOf(itemStack.getCount()) : "");

        // <name> <description>
        StringBuilder toolTipString = new StringBuilder();
        List<Component> toolTipList = itemStack.getTooltipLines(TooltipContext.EMPTY, Minecraft.getInstance().player, TooltipFlag.NORMAL);
        for (Component line : toolTipList) {
            toolTipString.append(line.getString()).append(' ');
        }

        Optional.ofNullable(itemStack.get(DataComponents.JUKEBOX_PLAYABLE))
                .flatMap(jukeboxPlayable -> jukeboxPlayable.song().unwrapKey())
                .ifPresent(discNumber -> toolTipString.append(' ').append(I18n.get("jukebox_song.minecraft." + discNumber.identifier().getPath())));

        // <slot row col prefix> <count> <name> <description>
        return "%s %s".formatted(info, toolTipString.toString());
    }

    /**
     * Change the selected group.
     *
     * @param goForward Whether to switch to next group or previous group.
     */
    private void changeGroup(boolean goForward) {
        if (currentSlotsGroupList == null || currentSlotsGroupList.isEmpty()) {
            return;
        }
        int nextGroupIndex = currentGroupIndex + (goForward ? 1 : -1);
        nextGroupIndex = Mth.clamp(nextGroupIndex, 0, currentSlotsGroupList.size() - 1);

        if (nextGroupIndex == currentGroupIndex) return; // Skip if already at the first or last group
        currentGroupIndex = nextGroupIndex;
        selectGroup(true);
    }

    /**
     * Refreshes the current group list and selects the first group.
     *
     * @param interrupt Whether to stop the narrator from narrating the previous message or not.
     */
    private void refreshGroupListAndSelectFirstGroup(boolean interrupt) {
        currentSlotsGroupList = GroupGenerator.generateGroupsFromSlots(currentScreen);
        if (currentSlotsGroupList == null || currentSlotsGroupList.isEmpty()) return;
        currentGroupIndex = 0;
        selectGroup(interrupt);
    }

    private void selectGroup(boolean interrupt) {
        if (currentSlotsGroupList == null || currentSlotsGroupList.isEmpty()) {
            return;
        }
        if (!isSearchBoxFocused() && Minecraft.getInstance().gui.screen() != null && Minecraft.getInstance().gui.screen().getFocused() != null) {
            Minecraft.getInstance().gui.screen().setFocused(null);
        }
        currentGroup = currentSlotsGroupList.get(currentGroupIndex);
        log.debug("Group(name:{}) {}/{} selected", currentGroup.getGroupName(), currentGroupIndex + 1, currentSlotsGroupList.size());
        MainClass.narrate(I18n.get("minecraft_access.inventory_controls.group_selected",
                currentGroup.isScrollable ? I18n.get("minecraft_access.inventory_controls.scrollable") : "",
                currentGroup.getGroupName()), interrupt);
        focusSlotItem(currentGroup.getFirstGroupItem(), false);
    }

    /**
     * Changes the selected tab for creative inventory screen.
     *
     * @param goForward Whether to switch to next tab or previous tab.
     */
    private void changeCreativeInventoryTab(boolean goForward) {
        if (!(currentScreen instanceof CreativeModeInventoryScreen creativeInventoryScreen)) return;

        int tab = CreativeModeTabs.tabs().indexOf(CreativeModeInventoryScreenAccessor.getSelectedTab());

        if (goForward && tab + 1 < CreativeModeTabs.tabs().size()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
            ((CreativeModeInventoryScreenAccessor) creativeInventoryScreen).invokeSelectTab(CreativeModeTabs.tabs().get(tab + 1));
            refreshGroupListAndSelectFirstGroup(false);
        } else if (!goForward && tab - 1 >= 0) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
            ((CreativeModeInventoryScreenAccessor) creativeInventoryScreen).invokeSelectTab(CreativeModeTabs.tabs().get(tab - 1));
            refreshGroupListAndSelectFirstGroup(false);
        }
    }

    /**
     * Changes the selected tab for inventory/crafting screen.
     *
     * @param goForward Whether to switch to next tab or previous tab.
     */
    private void changeRecipeTab(boolean goForward) {
        if (currentRecipeBookWidget == null || !currentRecipeBookWidget.isVisible()) return;

        RecipeBookComponentAccessor recipeBookComponentAccessor = (RecipeBookComponentAccessor) currentRecipeBookWidget;
        var tabButtons = recipeBookComponentAccessor.getTabButtons();
        if (tabButtons == null || tabButtons.isEmpty()) return;

        int currentTabIndex = tabButtons.indexOf(recipeBookComponentAccessor.getSelectedTab());
        int nextTabIndex = currentTabIndex + (goForward ? 1 : -1);
        nextTabIndex = Mth.clamp(nextTabIndex, 0, tabButtons.size() - 1);

        var targetTab = tabButtons.get(nextTabIndex);

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));

        int x = targetTab.getX() + 9;
        int y = targetTab.getY() + 9;

        MouseUtils.Coordinates p = MouseUtils.calcRealPositionOfWidget(x, y);
        MouseUtils.moveAndLeftClick(p.x(), p.y());

        String categoryName = getRecipeCategoryName(targetTab.getCategory());

        // Refresh groups and select recipes group
        currentSlotsGroupList = GroupGenerator.generateGroupsFromSlots(currentScreen);
        SlotsGroup recipesGroup = null;
        if (currentSlotsGroupList != null) {
            for (int i = 0; i < currentSlotsGroupList.size(); i++) {
                if ("recipes".equals(currentSlotsGroupList.get(i).getGroupKey())) {
                    currentGroupIndex = i;
                    recipesGroup = currentSlotsGroupList.get(i);
                    currentGroup = recipesGroup;
                    break;
                }
            }
        }

        RecipeBookPageAccessor pageAccessor = (RecipeBookPageAccessor) recipeBookComponentAccessor.getRecipeBookPage();
        RecipePageStats stats = getPageStats(pageAccessor);
        String statsSummary = stats.formatSummary();
        String baseCat = I18n.get("minecraft_access.inventory_controls.recipe_category", categoryName);
        String categoryPrefix = statsSummary.isEmpty() ? baseCat : baseCat + ", " + statsSummary;

        if (recipesGroup != null && !recipesGroup.slotItems.isEmpty()) {
            currentSlotItem = recipesGroup.getFirstGroupItem();
            moveToSlotItem(currentSlotItem, 100);

            List<RecipeButton> buttons = pageAccessor.getButtons();
            if (!buttons.isEmpty() && buttons.getFirst().visible && buttons.getFirst().getCollection() != null) {
                RecipeButton firstButton = buttons.getFirst();
                ItemStack displayStack = firstButton.getDisplayStack();
                String itemName = displayStack.getHoverName().getString();
                String craftable = firstButton.getCollection().hasCraftable() ? I18n.get("minecraft_access.other.craftable") : I18n.get("minecraft_access.other.not_craftable");
                String firstRecipeText = "%s %d %s".formatted(craftable, displayStack.getCount(), itemName);
                String narration = categoryPrefix + ". " + firstRecipeText;
                lastActionNarrationTime = System.currentTimeMillis();
                previousSlotText = firstRecipeText;
                MainClass.narrate(narration, true);
                return;
            }
        }

        String narration = (recipesGroup == null || recipesGroup.slotItems.isEmpty())
                ? I18n.get("minecraft_access.inventory_controls.recipe_category_empty", categoryName)
                : categoryPrefix;
        lastActionNarrationTime = System.currentTimeMillis();
        MainClass.narrate(narration, true);
    }

    public record RecipePageStats(int total, int craftable, int notCraftable) {
        public String formatSummary() {
            if (total == 0) return "";
            if (notCraftable == 0) {
                return (total == 1)
                        ? I18n.get("minecraft_access.inventory_controls.recipe_page_stats_single_craftable")
                        : I18n.get("minecraft_access.inventory_controls.recipe_page_stats_all_craftable", total);
            }
            if (craftable == 0) {
                return (total == 1)
                        ? I18n.get("minecraft_access.inventory_controls.recipe_page_stats_single_not_craftable")
                        : I18n.get("minecraft_access.inventory_controls.recipe_page_stats_none_craftable", total);
            }
            String craftText = (craftable == 1)
                    ? I18n.get("minecraft_access.inventory_controls.recipe_page_stats_craftable_one")
                    : I18n.get("minecraft_access.inventory_controls.recipe_page_stats_craftable_count", craftable);
            String notCraftText = (notCraftable == 1)
                    ? I18n.get("minecraft_access.inventory_controls.recipe_page_stats_not_craftable_one")
                    : I18n.get("minecraft_access.inventory_controls.recipe_page_stats_not_craftable_count", notCraftable);
            return I18n.get("minecraft_access.inventory_controls.recipe_page_stats_mixed", total, craftText, notCraftText);
        }
    }

    private RecipePageStats getPageStats(RecipeBookPageAccessor pageAccessor) {
        int total = 0;
        int craftable = 0;
        int notCraftable = 0;
        for (RecipeButton button : pageAccessor.getButtons()) {
            if (button.visible && button.getCollection() != null) {
                total++;
                if (button.getCollection().hasCraftable()) {
                    craftable++;
                } else {
                    notCraftable++;
                }
            }
        }
        return new RecipePageStats(total, craftable, notCraftable);
    }

    private String getRecipeCategoryName(@Nullable ExtendedRecipeBookCategory category) {
        if (category == null) return I18n.get("minecraft_access.inventory_controls.Unknown");

        String path = null;
        if (category instanceof SearchRecipeBookCategory searchCat) {
            path = searchCat.name().toLowerCase(Locale.ROOT) + "_search";
        } else if (category instanceof RecipeBookCategory recipeCat) {
            Identifier id = BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey(recipeCat);
            if (id != null) {
                path = id.getPath();
            }
        }

        if (path != null) {
            String transKey = "minecraft_access.recipe_category." + path;
            if (Language.getInstance().has(transKey)) {
                return I18n.get(transKey);
            }
            String clean = path.replace('_', ' ').replace("crafting ", "").replace("furnace ", "");
            if (!clean.isEmpty()) {
                return Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
            }
        }

        String fallback = category.toString();
        if (fallback.contains("@")) {
            return I18n.get("minecraft_access.inventory_controls.Unknown");
        }
        return fallback;
    }

    private boolean narrateRecipeInfo() {
        if (currentRecipeBookWidget == null || !currentRecipeBookWidget.isVisible()) {
            return false;
        }
        if (currentGroup == null || !"recipes".equals(currentGroup.getGroupKey())) {
            return false;
        }

        RecipeBookComponentAccessor recipeBookAccessor = (RecipeBookComponentAccessor) currentRecipeBookWidget;
        RecipeBookPageAccessor pageAccessor = (RecipeBookPageAccessor) recipeBookAccessor.getRecipeBookPage();
        List<RecipeButton> buttons = pageAccessor.getButtons();
        int focusIndex = currentGroup.slotItems.indexOf(currentSlotItem);
        if (focusIndex < 0 || focusIndex >= buttons.size()) {
            return false;
        }

        RecipeButton button = buttons.get(focusIndex);
        if (!button.visible) {
            return false;
        }

        RecipeCollection collection = button.getCollection();
        if (collection == null) {
            return false;
        }

        ItemStack displayStack = button.getDisplayStack();
        String productName = displayStack.getHoverName().getString();

        var recipes = collection.getRecipes();
        if (recipes.isEmpty()) {
            return false;
        }

        ContextMap context = SlotDisplayContext.fromLevel(Minecraft.getInstance().level);
        RecipeDisplay display = recipes.getFirst().display();
        for (var entry : recipes) {
            ItemStack resultStack = entry.display().result().resolveForFirstStack(context);
            if (ItemStack.isSameItem(resultStack, displayStack)) {
                display = entry.display();
                break;
            }
        }

        List<SlotDisplay> ingredients = new ArrayList<>();
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            ingredients.addAll(shaped.ingredients());
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            ingredients.addAll(shapeless.ingredients());
        } else if (display instanceof FurnaceRecipeDisplay furnace) {
            ingredients.add(furnace.ingredient());
        } else if (display instanceof StonecutterRecipeDisplay stonecutter) {
            ingredients.add(stonecutter.input());
        } else if (display instanceof SmithingRecipeDisplay smithing) {
            ingredients.add(smithing.template());
            ingredients.add(smithing.base());
            ingredients.add(smithing.addition());
        }
        Map<String, Integer> ingredientCounts = new LinkedHashMap<>();
        for (SlotDisplay slotDisplay : ingredients) {
            if (slotDisplay == null) {
                continue;
            }
            ItemStack stack = slotDisplay.resolveForFirstStack(context);
            if (!stack.isEmpty()) {
                String ingredientName = stack.getHoverName().getString();
                ingredientCounts.put(ingredientName, ingredientCounts.getOrDefault(ingredientName, 0) + 1);
            }
        }

        if (ingredientCounts.isEmpty()) {
            return false;
        }

        List<String> formattedIngredients = new ArrayList<>();
        for (Map.Entry<String, Integer> entryItem : ingredientCounts.entrySet()) {
            formattedIngredients.add("%d %s".formatted(entryItem.getValue(), entryItem.getKey()));
        }

        String ingredientsString = String.join(", ", formattedIngredients);
        String narration = I18n.get("minecraft_access.inventory_controls.recipe_requirements", productName, ingredientsString);
        MainClass.narrate(narration, true);
        return true;
    }

    /**
     * Encapsulate the changes against the vanilla code here.
     * Correspond to the vanilla code after 1.20.x
     */
    private void setSearchBoxFocus(EditBox w, boolean focus) {
        if (focus) {
            log.debug("T key pressed, selecting the search box.");
            w.setFocused(true);
        } else {
            log.debug("Enter key pressed, deselecting the search box.");
            boolean origin = ((EditBoxAccessor) w).getCanLoseFocus();
            w.setCanLoseFocus(true);
            w.setFocused(false);
            w.setCanLoseFocus(origin);
        }
    }

    private enum FocusDirection {
        UP("gui.up"),
        DOWN("gui.down"),
        LEFT("minecraft_access.inventory_controls.direction_left"),
        RIGHT("minecraft_access.inventory_controls.direction_right");

        private final String value;

        FocusDirection(String value) {
            this.value = value;
        }

        String getString() {
            return value;
        }
    }
}
