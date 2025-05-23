package de.canitzp.justabattery;

import com.mojang.serialization.Codec;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@EventBusSubscriber
@Mod(JustABattery.MODID)
public class JustABattery {
    
    public static final String MODID = "justabattery";
    
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final Holder<CreativeModeTab> TAB = TABS.register("tab", JustABatteryTab::create);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);
    public static final Supplier<BatteryItem> BATTERY_ITEM = ITEMS.register("battery", BatteryItem::new);
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPE = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final Supplier<DataComponentType<Integer>> DC_ENERGY = DATA_COMPONENT_TYPE.registerComponentType("energy", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).cacheEncoding());
    public static final Supplier<DataComponentType<Integer>> DC_LEVEL = DATA_COMPONENT_TYPE.registerComponentType("level", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).cacheEncoding());
    public static final Supplier<DataComponentType<Integer>> DC_TRACE_WIDTH = DATA_COMPONENT_TYPE.registerComponentType("trace_width", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).cacheEncoding());
    public static final Supplier<DataComponentType<Byte>> DC_MODE = DATA_COMPONENT_TYPE.registerComponentType("mode", builder -> builder.persistent(Codec.BYTE).networkSynchronized(ByteBufCodecs.BYTE).cacheEncoding());

    public JustABattery(IEventBus modEventBus, ModContainer modContainer){
        LOGGER.info("[JustABattery]: Starting. Thanks for using :+1:. Also many thanks to markygnlg, who suggested this mod idea in the first place!");

        JustAConfig.load();

        DATA_COMPONENT_TYPE.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);

        modEventBus.addListener(this::registerCapabilities);
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents{
        @SubscribeEvent
        public static void registerRegisterRangeSelectItemModelProperties(RegisterRangeSelectItemModelPropertyEvent event){
            event.register(ResourceLocation.fromNamespaceAndPath(MODID, "battery_charge"), BatteryItem.ChargeProperty.MAP_CODEC);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addReloadListenerEvent(AddServerReloadListenersEvent event) {
        event.addListener(ResourceLocation.fromNamespaceAndPath(MODID, "combination_recipe"), new SimplePreparableReloadListener<RecipeManager>() {
            @Override
            protected RecipeManager prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return event.getServerResources().getRecipeManager();
            }

            @Override
            protected void apply(RecipeManager recipeManager, ResourceManager resourceManager, ProfilerFiller profiler) {
                 List<RecipeHolder<?>> allRecipes = new ArrayList<>(recipeManager.getRecipes());
                if (allRecipes.stream().noneMatch(recipe -> recipe.id().equals(BatteryCombiningRecipe.ID))) {
                    allRecipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, BatteryCombiningRecipe.ID), BatteryCombiningRecipe.INSTANCE));
                } else {
                    LOGGER.info("[JustABattery]: Battery recipe aborted! The recipe id does already exist.");
                }
                Arrays.stream(RecipeManager.class.getDeclaredFields()).filter(field -> field.getType().equals(RecipeMap.class)).findFirst().ifPresent(field -> {
                    field.setAccessible(true);
                    try {
                        field.set(recipeManager, RecipeMap.create(allRecipes));
                        LOGGER.info("[JustABattery]: Battery recipe injected.");
                    } catch (IllegalAccessException e) {
                        LOGGER.error("[JustABattery]: Battery recipe injection failed.", e);
                    }
                });
            }
        });
    }

    @SubscribeEvent
    public static void onEntityStruckByLightning(EntityStruckByLightningEvent event){
        if(event.getEntity() instanceof Player player){
            player.getInventory().items.stream().filter(itemStack -> itemStack.is(BATTERY_ITEM.get())).forEach(batteryStack -> {
                BATTERY_ITEM.get().struckByLightning(event.getLightning(), batteryStack);
            });
        } else if(event.getEntity() instanceof ItemEntity entity){
            if (entity.getItem().is(BATTERY_ITEM.get())) {
                BATTERY_ITEM.get().struckByLightning(event.getLightning(), entity.getItem());
                event.setCanceled(true); // we don't want the item entity to be destroyed by the lightning bolt
            }
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event){
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, unused) -> new BatteryItem.StackEnergyStorage(stack), BATTERY_ITEM.get());
    }

    @SubscribeEvent
    public static void playerJoin(PlayerEvent.PlayerLoggedInEvent event){
        Player player = event.getEntity();
        NonNullList<ItemStack> armorInventory = player.getInventory().armor;
        NonNullList<ItemStack> mainInventory = player.getInventory().items;
        NonNullList<ItemStack> offHandInventory = player.getInventory().offhand;

        NonNullList<ItemStack> mergedInventory = NonNullList.create();
        mergedInventory.addAll(armorInventory);
        mergedInventory.addAll(mainInventory);
        mergedInventory.addAll(offHandInventory);

        for(ItemStack stack : mergedInventory){
            if(stack.has(DataComponents.CUSTOM_DATA)){
                CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
                // update from pre 1.20.6 versions
                if(tag.contains("Energy", Tag.TAG_INT)){
                    int energy = tag.getInt("Energy");
                    tag.remove("Energy");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    stack.set(DC_ENERGY, energy);
                }
                if(tag.contains("Mode", Tag.TAG_BYTE)){
                    byte mode = tag.getByte("Mode");
                    tag.remove("Mode");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    stack.set(DC_MODE, mode);
                }
                if(tag.contains("Level", Tag.TAG_INT)){
                    int level = tag.getInt("Level");
                    tag.remove("level");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    stack.set(DC_LEVEL, level);
                }
                if(tag.contains("TraceWidth", Tag.TAG_INT)){
                    int traceWidth = tag.getInt("TraceWidth");
                    tag.remove("TraceWidth");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    stack.set(DC_TRACE_WIDTH, traceWidth);
                }
            }
        }
    }
}