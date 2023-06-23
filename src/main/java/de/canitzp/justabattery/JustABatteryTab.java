package de.canitzp.justabattery;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class JustABatteryTab {

    public static final Component TITLE = Component.translatable("tab." + JustABattery.MODID);

    public static CreativeModeTab create() {
        return CreativeModeTab.builder()
                .icon(() -> JustABattery.BATTERY_ITEM.get().getDefaultInstance())
                .title(TITLE)
                .displayItems(JustABatteryTab::addItemsToDisplay)
                .build();
    }

    private static void addItemsToDisplay(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output){
        for(int i = 1; i <= 5; i++){
            JustABatteryTab.addBatteryToCreativeTab(output, i);
        }
        JustABatteryTab.addBatteryToCreativeTab(output, BatteryItem.getBatteryMaxLevel());
    }

    private static void addBatteryToCreativeTab(CreativeModeTab.Output tabStacks, int level) {
        ItemStack empty = JustABattery.BATTERY_ITEM.get().getDefaultInstance();
        BatteryItem.setLevel(empty, level);

        ItemStack full = empty.copy();
        BatteryItem.setStoredEnergy(full, BatteryItem.getCapacity(full));

        ItemStack emptyThickTraces = empty.copy();
        BatteryItem.setTraceWidth(emptyThickTraces, BatteryItem.getBatteryMaxTraceWidth());

        ItemStack fullThickTraces = full.copy();
        BatteryItem.setTraceWidth(fullThickTraces, BatteryItem.getBatteryMaxTraceWidth());

        tabStacks.accept(empty);
        tabStacks.accept(full);
        tabStacks.accept(emptyThickTraces);
        tabStacks.accept(fullThickTraces);
    }

}
