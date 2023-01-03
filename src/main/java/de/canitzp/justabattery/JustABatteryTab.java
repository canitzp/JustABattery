package de.canitzp.justabattery;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = JustABattery.MODID)
public class JustABatteryTab {

    public static final ResourceLocation NAME = new ResourceLocation(JustABattery.MODID, "tab");
    public static final Component TITLE = Component.translatable("tab." + JustABattery.MODID);
    @SubscribeEvent
    public static void registerCreativeTab(CreativeModeTabEvent.Register event){
        event.registerCreativeModeTab(NAME, builder -> {
            builder.icon(() -> JustABattery.BATTERY_ITEM.get().getDefaultInstance());
            builder.title(TITLE);
            builder.displayItems(JustABatteryTab::addItemsToDisplay);
        });
    }

    private static void addItemsToDisplay(FeatureFlagSet featureFlagSet, CreativeModeTab.Output output, boolean hasOp){
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
