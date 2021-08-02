package de.canitzp.justabattery;


import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
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
    
    public static final ItemGroup TAB = new ItemGroup(MODID) {
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
        ItemModelsProperties.register(BATTERY_ITEM.get(), new ResourceLocation(MODID, "level"), (stack, level, entity) -> {
            return BatteryItem.getStoredEnergy(stack) / (BatteryItem.getCapacity(stack) * 1.0F);
        });
        ItemModelsProperties.register(BATTERY_ITEM.get(), new ResourceLocation(MODID, "size"), (stack, level, entity) -> {
            int batLevel = BatteryItem.getLevel(stack);
            return batLevel <= 0 ? 1 : Math.min(batLevel, 5);
        });
        LOGGER.info("[JustABattery]: Client setup done.");
    }
    
    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event){
        IWorldReader levelAccessor = event.getWorld();
        if(levelAccessor instanceof World){
            World level = (World) levelAccessor;
            if(level.dimension() != World.OVERWORLD){
                return;
            }
    
            RecipeManager recipeManager = level.getRecipeManager();
            Collection<IRecipe<?>> allRecipes = recipeManager.getRecipes();
            allRecipes.add(BatteryCombiningRecipe.INSTANCE);
            recipeManager.replaceRecipes(allRecipes);
            LOGGER.info("[JustABattery]: Battery recipe injected.");
        }
    }
    
}
