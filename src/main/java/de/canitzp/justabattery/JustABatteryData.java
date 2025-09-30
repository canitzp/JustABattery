package de.canitzp.justabattery;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.tags.KeyTagProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = JustABattery.MODID)
public class JustABatteryData {

    @SubscribeEvent
    public static void runData(GatherDataEvent.Client event){
        DataGenerator generator = event.getGenerator();

        event.createProvider(JustABatteryItemModelProvider::new);
        event.createProvider(ItemTagProvider::new);
        event.createProvider(Recipe.Runner::new);
    }

    public static class JustABatteryItemModelProvider extends ModelProvider {

        public JustABatteryItemModelProvider(PackOutput output) {
            super(output, JustABattery.MODID);
        }

        @Override
        protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
            String[] quantificationNames = new String[]{"single", "double", "triple", "quad", "quint"};
            String[] levelNames = new String[]{"empty", "10", "20", "30", "40", "50", "60", "70", "80", "90", "full"};
            List<RangeSelectItemModel.Entry> list = new ArrayList<>();
            for(int size = 1; size <= 5; size++){
                for(int level = 0; level <= 10; level++){
                    String quantificationName = quantificationNames[size - 1];
                    String levelName = levelNames[level];
                    ItemModel.Unbaked unbaked = ItemModelUtils.plainModel(itemModels.createFlatItemModel(JustABattery.BATTERY_ITEM.get(), "/" + quantificationName + "/battery_" + quantificationName + "_" + levelName, ModelTemplates.FLAT_ITEM));

                    list.add(ItemModelUtils.override(unbaked, size * 11 + level - 11));
                }
            }
            itemModels.itemModelOutput.accept(JustABattery.BATTERY_ITEM.get(), ItemModelUtils.rangeSelect(new BatteryItem.ChargeProperty(1, 0), list));
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

    public static class ItemTagProvider extends KeyTagProvider<Item> {

        protected ItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, Registries.ITEM, lookupProvider, JustABattery.MODID);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookupProvider) {
            super.tag(ItemTags.create(ResourceLocation.parse("curios:curio"))).add(BuiltInRegistries.ITEM.getResourceKey(JustABattery.BATTERY_ITEM.get()).get());
        }
    }

}
