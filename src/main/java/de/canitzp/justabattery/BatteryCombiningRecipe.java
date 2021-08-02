package de.canitzp.justabattery;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;

public class BatteryCombiningRecipe extends ShapelessRecipe {
    
    public static final ResourceLocation ID = new ResourceLocation(JustABattery.MODID, "combine_batteries");
    public static final NonNullList<Ingredient> INPUTS = NonNullList.of(Ingredient.EMPTY, Ingredient.of(JustABattery.BATTERY_ITEM.get()), Ingredient.of(JustABattery.BATTERY_ITEM.get()), Ingredient.of(Tags.Items.NUGGETS_GOLD));
    
    public static final BatteryCombiningRecipe INSTANCE = new BatteryCombiningRecipe();
    
    private BatteryCombiningRecipe(){
        super(ID, "", JustABattery.BATTERY_ITEM.get().getDefaultInstance(), INPUTS);
    }
    
    @Override
    public boolean isSpecial(){
        return true;
    }
    
    private ItemStack getFirstBattery(CraftingContainer inv){
        for(int i = 0; i < inv.getContainerSize(); i++){
            ItemStack stack = inv.getItem(i);
            if(stack.getItem() instanceof BatteryItem){
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
    
    private boolean checkIfOnlyValidItemsArePresent(CraftingContainer inv){
        for(int i = 0; i < inv.getContainerSize(); i++){
            ItemStack stack = inv.getItem(i);
            if(!stack.isEmpty() && this.getIngredients().stream().noneMatch(ingredient -> ingredient.test(stack))){
                return false;
            }
        }
        return true;
    }
    
    // needs to be an int, so we don't rely on checking an overflow
    private int getCombinedLevel(CraftingContainer inv){
        int combinedLevel = 0;
        for(int i = 0; i < inv.getContainerSize(); i++){
            ItemStack stack = inv.getItem(i);
            if(!stack.isEmpty() && stack.getItem() instanceof BatteryItem){
                combinedLevel += BatteryItem.getLevel(stack);
            }
        }
        return combinedLevel;
    }
    
    private int getMaxTraceWidth(CraftingContainer inv){
        int combinedTraceWidth = 0;
        for(int i = 0; i < inv.getContainerSize(); i++){
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
    
    private int getCombinedEnergy(CraftingContainer inv){
        int combinedEnergy = 0;
        for(int i = 0; i < inv.getContainerSize(); i++){
            ItemStack stack = inv.getItem(i);
            if(!stack.isEmpty() && stack.getItem() instanceof BatteryItem){
                combinedEnergy += BatteryItem.getStoredEnergy(stack);
            }
        }
        return combinedEnergy;
    }
    
    private int getGoldNuggetAmount(CraftingContainer inv){
        int nuggets = 0;
        for(int i = 0; i < inv.getContainerSize(); i++){
            ItemStack stack = inv.getItem(i);
            if(stack.is(Tags.Items.NUGGETS_GOLD)){
                nuggets++;
            }
        }
        return nuggets;
    }
    
    @Override
    public ItemStack assemble(CraftingContainer inv){
        ItemStack output = super.getResultItem().copy();
        // levels are combined; cast to byte is safe, as long as the matches() method is called
        BatteryItem.setLevel(output, (byte) this.getCombinedLevel(inv));
        // the greatest of the both trace widths is chosen
        BatteryItem.setTraceWidth(output, (byte) (this.getMaxTraceWidth(inv) + this.getGoldNuggetAmount(inv)));
        // stored energy is combined
        BatteryItem.setStoredEnergy(output, this.getCombinedEnergy(inv));
        
        return output;
    }
    
    @Override
    public boolean matches(CraftingContainer inv, Level level1){
        if(!this.checkIfOnlyValidItemsArePresent(inv)){
            return false;
        }
        ItemStack firstBattery = this.getFirstBattery(inv);
        if(firstBattery.isEmpty()){
            return false;
        }
        
        if(this.getCombinedLevel(inv) > BatteryItem.BATTERY_MAX_LEVEL){
            return false;
        }
        if(this.getMaxTraceWidth(inv) + this.getGoldNuggetAmount(inv) > BatteryItem.BATTERY_MAX_TRACE_WIDTH){
            return false;
        }
        
        return true;
    }
    
}