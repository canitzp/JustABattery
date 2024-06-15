package de.canitzp.justabattery;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

public class BatteryCombiningRecipe extends ShapelessRecipe {
    
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JustABattery.MODID, "combine_batteries");
    public static final NonNullList<Ingredient> INPUTS = NonNullList.of(Ingredient.EMPTY, Ingredient.of(JustABattery.BATTERY_ITEM.get()), Ingredient.of(JustABattery.BATTERY_ITEM.get()), Ingredient.of(Tags.Items.NUGGETS_GOLD));
    
    public static final BatteryCombiningRecipe INSTANCE = new BatteryCombiningRecipe();
    
    private BatteryCombiningRecipe(){
        super("", CraftingBookCategory.EQUIPMENT, JustABattery.BATTERY_ITEM.get().getDefaultInstance(), INPUTS);
    }
    
    @Override
    public boolean isSpecial(){
        return true;
    }
    
    private ItemStack getFirstBattery(CraftingInput inv){
        for(int i = 0; i < inv.size(); i++){
            ItemStack stack = inv.getItem(i);
            if(stack.getItem() instanceof BatteryItem){
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
    
    private boolean checkIfOnlyValidItemsArePresent(CraftingInput inv){
        for(int i = 0; i < inv.size(); i++){
            ItemStack stack = inv.getItem(i);
            if(!stack.isEmpty() && this.getIngredients().stream().noneMatch(ingredient -> ingredient.test(stack))){
                return false;
            }
        }
        return true;
    }
    
    // needs to be an int, so we don't rely on checking an overflow
    private int getCombinedLevel(CraftingInput inv){
        int combinedLevel = 0;
        for(int i = 0; i < inv.size(); i++){
            ItemStack stack = inv.getItem(i);
            if(!stack.isEmpty() && stack.getItem() instanceof BatteryItem){
                combinedLevel += BatteryItem.getLevel(stack);
            }
        }
        return combinedLevel;
    }
    
    private int getMaxTraceWidth(CraftingInput inv){
        int combinedTraceWidth = 0;
        for(int i = 0; i < inv.size(); i++){
            ItemStack stack = inv.getItem(i);
            if(!stack.isEmpty() && stack.getItem() instanceof BatteryItem){
                int traceWidth = BatteryItem.getTraceWidth(stack);
                if(traceWidth > combinedTraceWidth){
                    combinedTraceWidth = traceWidth;
                }
            }
        }
        return combinedTraceWidth;
    }
    
    private int getCombinedEnergy(CraftingInput inv){
        int combinedEnergy = 0;
        for(int i = 0; i < inv.size(); i++){
            ItemStack stack = inv.getItem(i);
            if(!stack.isEmpty() && stack.getItem() instanceof BatteryItem){
                combinedEnergy += BatteryItem.getStoredEnergy(stack);
            }
        }
        return combinedEnergy;
    }
    
    private int getGoldNuggetAmount(CraftingInput inv){
        int nuggets = 0;
        for(int i = 0; i < inv.size(); i++){
            ItemStack stack = inv.getItem(i);
            if(stack.is(Tags.Items.NUGGETS_GOLD)){
                nuggets++;
            }
        }
        return nuggets;
    }
    
    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider access){
        ItemStack output = super.getResultItem(access).copy();
        // levels are combined
        BatteryItem.setLevel(output, this.getCombinedLevel(inv));
        // the greatest of the both trace widths is chosen
        BatteryItem.setTraceWidth(output, (this.getMaxTraceWidth(inv) + this.getGoldNuggetAmount(inv)));
        // stored energy is combined
        BatteryItem.setStoredEnergy(output, this.getCombinedEnergy(inv));
        
        return output;
    }
    
    @Override
    public boolean matches(CraftingInput inv, Level level1){
        if(!this.checkIfOnlyValidItemsArePresent(inv)){
            return false;
        }
        ItemStack firstBattery = this.getFirstBattery(inv);
        if(firstBattery.isEmpty()){
            return false;
        }
        
        if(this.getCombinedLevel(inv) > BatteryItem.getBatteryMaxLevel()){
            return false;
        }
        if(this.getMaxTraceWidth(inv) + this.getGoldNuggetAmount(inv) > BatteryItem.getBatteryMaxTraceWidth()){
            return false;
        }

        return true;
    }
    
}