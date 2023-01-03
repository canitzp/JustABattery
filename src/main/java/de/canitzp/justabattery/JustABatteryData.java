package de.canitzp.justabattery;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = JustABattery.MODID)
public class JustABatteryData {

    @SubscribeEvent
    public static void runData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper helper = event.getExistingFileHelper();

        // Client
        generator.addProvider(event.includeClient(), new ItemModel(generator.getPackOutput(), helper));

        // Server
        generator.addProvider(event.includeServer(), new ItemTagProvider(generator.getPackOutput(), event.getLookupProvider(), helper));
        generator.addProvider(event.includeServer(), new Recipe(generator.getPackOutput()));
    }

    public static class ItemModel extends ItemModelProvider {

        public ItemModel(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, JustABattery.MODID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            ItemModelBuilder batteryModelBuilder = singleTexture("battery", mcLoc("item/handheld"), "layer0", modLoc("item/single/battery_single_empty"));

            String[] quantificationNames = new String[]{"single", "double", "triple", "quad", "quint"};
            String[] levelNames = new String[]{"empty", "10", "20", "30", "40", "50", "60", "70", "80", "90", "full"};
            for(int size = 1; size <= 5; size++){
                for(int level = 0; level <= 10; level++){
                    String quantificationName = quantificationNames[size - 1];
                    String levelName = levelNames[level];
                    ItemModelBuilder batteryStageModel = singleTexture("item/" + quantificationName + "/battery_" + quantificationName + "_" + levelName, mcLoc("item/handheld"), "layer0", modLoc("item/" + quantificationName + "/battery_" + quantificationName + "_" + levelName));

                    batteryModelBuilder.override()
                            .predicate(new ResourceLocation("justabattery:size"), size)
                            .predicate(new ResourceLocation("justabattery:level"), level / 10F)
                            .model(batteryStageModel);
                }
            }
        }
    }

    public static class Recipe extends RecipeProvider {

        public Recipe(PackOutput output) {
            super(output);
        }

        @Override
        protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, JustABattery.BATTERY_ITEM.get())
                    .define('r', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                    .define('l', Tags.Items.GEMS_LAPIS)
                    .define('g', Tags.Items.NUGGETS_GOLD)
                    .define('c', Tags.Items.INGOTS_COPPER)
                    .pattern(" g ")
                    .pattern("lrl")
                    .pattern("ccc")
                    .unlockedBy("has_lapis_gem", has(Tags.Items.GEMS_LAPIS))
                    .save(consumer);
        }
    }

    public static class ItemTagProvider extends TagsProvider<Item> {


        protected ItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
            super(output, Registries.ITEM, lookupProvider, JustABattery.MODID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookupProvider) {
            this.tag(ItemTags.create(new ResourceLocation("curios:curio"))).add(ForgeRegistries.ITEMS.getResourceKey(JustABattery.BATTERY_ITEM.get()).get());
        }
    }

}
