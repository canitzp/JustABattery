package de.canitzp.justabattery;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.function.Supplier;

@Mod.EventBusSubscriber
@Mod(JustABattery.MODID)
public class JustABattery {
    
    public static final String MODID = "justabattery";
    
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final Holder<CreativeModeTab> TAB = TABS.register("tab", JustABatteryTab::create);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);
    public static final Supplier<BatteryItem> BATTERY_ITEM = ITEMS.register("battery", BatteryItem::new);
    
    public JustABattery(){
        LOGGER.info("[JustABattery]: Starting. Thanks for using :+1:. Also many thanks to markygnlg, who suggested this mod idea in the first place!");

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerCapabilities);

        TABS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        
        JustAConfig.load();

        if(FMLEnvironment.dist.isClient()){
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadClient);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void loadClient(FMLClientSetupEvent event){
        ItemProperties.register(BATTERY_ITEM.get(), new ResourceLocation(MODID, "level"), (stack, level, entity, i) -> {
            return BatteryItem.getStoredEnergy(stack) / (BatteryItem.getCapacity(stack) * 1.0F);
        });
        ItemProperties.register(BATTERY_ITEM.get(), new ResourceLocation(MODID, "size"), (stack, level, entity, i) -> {
            int batLevel = BatteryItem.getLevel(stack);
            return batLevel <= 0 ? 1 : Math.min(batLevel, 5);
        });
        LOGGER.info("[JustABattery]: Client setup done.");
    }
    
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event){
        LevelAccessor levelAccessor = event.getLevel();
        if(levelAccessor instanceof Level level){
            if(level.dimension() != Level.OVERWORLD){
                return;
            }
    
            RecipeManager recipeManager = level.getRecipeManager();
            Collection<RecipeHolder<?>> allRecipes = recipeManager.getRecipes();
            if(allRecipes.stream().noneMatch(recipe -> recipe.id().equals(BatteryCombiningRecipe.ID))){
                // only add the recipe, if there isn't anyone
                allRecipes.add(new RecipeHolder<>(BatteryCombiningRecipe.ID, BatteryCombiningRecipe.INSTANCE));
                recipeManager.replaceRecipes(allRecipes);
                LOGGER.info("[JustABattery]: Battery recipe injected.");
            } else {
                LOGGER.info("[JustABattery]: Battery recipe aborted! The recipe id does already exist.");
            }
        }
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
}