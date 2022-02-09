package de.canitzp.justabattery;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

@Mod.EventBusSubscriber
@Mod(JustABattery.MODID)
public class JustABattery {
    
    public static final String MODID = "justabattery";
    
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<BatteryItem> BATTERY_ITEM = ITEMS.register("battery", BatteryItem::new);
    
    public static final CreativeModeTab TAB = new CreativeModeTab(MODID) {
        @Override
        public ItemStack makeIcon(){
            return BATTERY_ITEM.get().getDefaultInstance();
        }
    };
    
    public JustABattery(){
        LOGGER.info("[JustABattery]: Starting. Thanks for using :+1:. Also many thanks to markygnlg, who suggested this mod idea in the first place!");
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        
        JustAConfig.load();
        
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadClient);
        });
    }
    
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
    public static void onWorldLoad(WorldEvent.Load event){
        LevelAccessor levelAccessor = event.getWorld();
        if(levelAccessor instanceof Level level){
            if(level.dimension() != Level.OVERWORLD){
                return;
            }
    
            RecipeManager recipeManager = level.getRecipeManager();
            Collection<Recipe<?>> allRecipes = recipeManager.getRecipes();
            if(allRecipes.stream().noneMatch(recipe -> recipe.getId().equals(BatteryCombiningRecipe.ID))){
                // only add the recipe, if there isn't anyone
                allRecipes.add(BatteryCombiningRecipe.INSTANCE);
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
}