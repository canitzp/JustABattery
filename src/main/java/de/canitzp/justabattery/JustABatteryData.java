package de.canitzp.justabattery;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = JustABattery.MODID)
public class JustABatteryData {

    @SubscribeEvent
    public static void runData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper helper = event.getExistingFileHelper();

        // Client
        generator.addProvider(event.includeClient(), new ItemModel(generator.getPackOutput(), helper));

        // Server
        generator.addProvider(event.includeServer(), new ItemTagProvider(generator.getPackOutput(), event.getLookupProvider(), helper));
        event.createProvider(event.includeServer(), Recipe.Runner::new);
    }

    public static class ItemModel extends ItemModelProvider {

        public ItemModel(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, JustABattery.MODID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            ItemModelBuilder batteryModelBuilder = this.singleTexture("battery", this.mcLoc("item/handheld"), "layer0", this.modLoc("item/single/battery_single_empty"));

            String[] quantificationNames = new String[]{"single", "double", "triple", "quad", "quint"};
            String[] levelNames = new String[]{"empty", "10", "20", "30", "40", "50", "60", "70", "80", "90", "full"};
            for(int size = 1; size <= 5; size++){
                for(int level = 0; level <= 10; level++){
                    String quantificationName = quantificationNames[size - 1];
                    String levelName = levelNames[level];
                    ItemModelBuilder batteryStageModel = this.singleTexture("item/" + quantificationName + "/battery_" + quantificationName + "_" + levelName, this.mcLoc("item/handheld"), "layer0", this.modLoc("item/" + quantificationName + "/battery_" + quantificationName + "_" + levelName));

                    batteryModelBuilder.override()
                            .predicate(ResourceLocation.parse("justabattery:size"), size)
                            .predicate(ResourceLocation.parse("justabattery:level"), level / 10F)
                            .model(batteryStageModel);
                }
            }
        }
    }

    public static class Recipe extends RecipeProvider {

        public Recipe(HolderLookup.Provider output, RecipeOutput lookupProvider) {
            super(output, lookupProvider);
        }

        @Override
        protected void buildRecipes() {
            this.shaped(RecipeCategory.TOOLS, JustABattery.BATTERY_ITEM.get())
                    .define('r', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                    .define('l', Tags.Items.GEMS_LAPIS)
                    .define('g', Tags.Items.NUGGETS_GOLD)
                    .define('c', Tags.Items.INGOTS_COPPER)
                    .pattern(" g ")
                    .pattern("lrl")
                    .pattern("ccc")
                    .unlockedBy("has_lapis_gem", has(Tags.Items.GEMS_LAPIS))
                    .save(super.output);
        }

        public static final class Runner extends RecipeProvider.Runner {
            public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
                super(output, lookupProvider);
            }

            @Override
            protected RecipeProvider createRecipeProvider(HolderLookup.Provider lookupProvider, RecipeOutput output) {
                return new Recipe(lookupProvider, output);
            }

            @Override
            public String getName() {
                return "JustABattery recipes";
            }
        }
    }

    public static class ItemTagProvider extends TagsProvider<Item> {


        protected ItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
            super(output, Registries.ITEM, lookupProvider, JustABattery.MODID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookupProvider) {
            this.tag(ItemTags.create(ResourceLocation.parse("curios:curio"))).add(BuiltInRegistries.ITEM.getResourceKey(JustABattery.BATTERY_ITEM.get()).get());
        }
    }

}
