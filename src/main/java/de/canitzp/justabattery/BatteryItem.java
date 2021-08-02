package de.canitzp.justabattery;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BatteryItem extends Item {
    
    public static final int BATTERY_INITIAL_CAPACITY = JustAConfig.get().battery_capacity;
    public static final int BATTERY_STAGED_TRANSFER = JustAConfig.get().battery_transfer;
    public static final int BATTERY_MAX_LEVEL = JustAConfig.get().battery_max_level;
    public static final int BATTERY_MAX_TRACE_WIDTH = JustAConfig.get().battery_max_trace_width;
    
    public static final byte MODE_NONE = 0;
    public static final byte MODE_FIRST_FOUND = 1;
    public static final byte MODE_ALL = 2;
    public static final byte MODE_RANDOM = 3;
    
    public static int getStoredEnergy(ItemStack stack){
        return stack.getOrCreateTag().getInt("Energy");
    }
    
    public static void setStoredEnergy(ItemStack stack, int energy){
        stack.getOrCreateTag().putInt("Energy", energy);
    }
    
    public static int getCapacity(ItemStack stack){
        return BATTERY_INITIAL_CAPACITY * getLevel(stack);
    }
    
    public static int getMaxTransfer(ItemStack stack){
        return BATTERY_STAGED_TRANSFER * getTraceWidth(stack);
    }
    
    public static byte getMode(ItemStack stack){
        return stack.getOrCreateTag().getByte("Mode");
    }
    
    public static void setMode(ItemStack stack, byte mode){
        stack.getOrCreateTag().putByte("Mode", mode);
    }
    
    public static int getLevel(ItemStack stack){
        int level = stack.getOrCreateTag().getInt("Level");
        return level <= 0 ? 1 : level;
    }
    
    public static void setLevel(ItemStack stack, int level){
        stack.getOrCreateTag().putInt("Level", level);
    }
    
    public static int getTraceWidth(ItemStack stack){
        int traceWidth = stack.getOrCreateTag().getInt("TraceWidth");
        return traceWidth <= 0 ? 1 : traceWidth;
    }
    
    public static void setTraceWidth(ItemStack stack, int traceWidth){
        stack.getOrCreateTag().putInt("TraceWidth", traceWidth);
    }
    
    public BatteryItem(){
        super(new Properties().stacksTo(1).tab(JustABattery.TAB));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip, ITooltipFlag flag){
        super.appendHoverText(stack, level, tooltip, flag);
    
        tooltip.add(new TranslationTextComponent("item.justabattery.desc.level", getLevel(stack)).withStyle(TextFormatting.DARK_PURPLE));
        tooltip.add(new TranslationTextComponent("item.justabattery.desc.tracewidth", getTraceWidth(stack), getMaxTransfer(stack)).withStyle(TextFormatting.DARK_PURPLE));
    
        byte mode = getMode(stack);
        tooltip.add(new TranslationTextComponent("item.justabattery.prefix.mode").append(" ").append(new TranslationTextComponent("item.justabattery.name.mode." + mode)).withStyle(TextFormatting.DARK_PURPLE, TextFormatting.ITALIC));
        tooltip.add(new TranslationTextComponent("item.justabattery.desc.mode." + mode).withStyle(TextFormatting.DARK_PURPLE, TextFormatting.ITALIC));
        
        tooltip.add(new TranslationTextComponent("item.justabattery.desc.energy", getStoredEnergy(stack),getCapacity(stack)).withStyle(TextFormatting.RED));
    }
    
    @Override
    public ITextComponent getName(ItemStack stack){
        TextComponent primary = null;
        TextComponent secondary = new TranslationTextComponent(this.getDescriptionId(stack));
        
        int level = getLevel(stack);
        if(level >= 2 && level <= 5){
            primary = new TranslationTextComponent("item.justabattery.name.prefix." + level);
        } else if (level > 5){
            primary = new TranslationTextComponent("item.justabattery.name.prefix", level);
        }
        
        return primary != null ? primary.append(" ").append(secondary) : secondary;
    }
    
    @Override
    public ITextComponent getHighlightTip(ItemStack stack, ITextComponent displayName){
        byte mode = getMode(stack);
        if(mode >= MODE_FIRST_FOUND && mode <= MODE_RANDOM){
            if(displayName instanceof TextComponent){
                TextComponent mutableComponent = (TextComponent) displayName;
                mutableComponent.append(" - ").append(new TranslationTextComponent("item.justabattery.prefix.mode")).append(" ").append(new TranslationTextComponent("item.justabattery.name.mode." + mode));
            }
        }
        return super.getHighlightTip(stack, displayName);
    }
    
    @Override
    public boolean isFoil(ItemStack stack){
        return getMode(stack) > MODE_NONE;
    }
    
    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context){
        if(context.getPlayer() != null && context.getPlayer().isCrouching()){
            if(!context.getLevel().isClientSide()){
                byte mode = getMode(stack);
                mode++;
                if(mode > MODE_RANDOM){
                    mode = MODE_NONE;
                }
                setMode(stack, mode);
            
            }
            return ActionResultType.SUCCESS;
        }
        return super.onItemUseFirst(stack, context);
    }
    
    @Override
    public void fillItemCategory(ItemGroup tab, NonNullList<ItemStack> stacks){
        if(this.allowdedIn(tab)){
            for(byte i = 1; i <= 5; i++){
                ItemStack empty = this.getDefaultInstance();
                setLevel(empty, i);
            
                ItemStack full = empty.copy();
                setStoredEnergy(full, getCapacity(full));
            
                ItemStack emptyThickTraces = empty.copy();
                setTraceWidth(emptyThickTraces, BATTERY_MAX_TRACE_WIDTH);
            
                ItemStack fullThickTraces = full.copy();
                setTraceWidth(fullThickTraces, BATTERY_MAX_TRACE_WIDTH);
            
                stacks.add(empty);
                stacks.add(full);
                stacks.add(emptyThickTraces);
                stacks.add(fullThickTraces);
                // fill remaining row with empty stacks
                for(int j = 5; j <= 9; j++){
                    stacks.add(ItemStack.EMPTY);
                }
            }
        }
    }
    
    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt){
        return new StackEnergyStorage(stack);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World level, Entity entity, int p_41407_, boolean p_41408_){
        if(level.isClientSide()){
            return;
        }
        if(entity instanceof PlayerEntity){
            PlayerEntity player = (PlayerEntity) entity;
            int mode = getMode(stack);
            if(mode == 0){
                return;
            }
            
            List<ItemStack> energyItems = player.inventory.items
                .stream()
                .filter(itemStack -> !ItemStack.matches(itemStack, stack))
                .filter(itemStack -> itemStack.getCapability(CapabilityEnergy.ENERGY).isPresent())
                .collect(Collectors.toList());
            if(energyItems.size() > 0){
                int storedEnergy = getStoredEnergy(stack);
                int maxTransferableEnergy = Math.min(storedEnergy, getMaxTransfer(stack));
                int actualTransferredEnergy = 0;
                switch(mode){
                    case MODE_FIRST_FOUND: {
                        actualTransferredEnergy = this.transferEnergyToFirstItem(maxTransferableEnergy, energyItems);
                        break;
                    }
                    case MODE_ALL: {
                        actualTransferredEnergy = this.transferEnergyToAll(maxTransferableEnergy, energyItems);
                        break;
                    }
                    case MODE_RANDOM: {
                        actualTransferredEnergy = this.transferEnergyRandom(maxTransferableEnergy, energyItems);
                        break;
                    }
                }
                if(actualTransferredEnergy > 0){
                    BatteryItem.setStoredEnergy(stack, storedEnergy - actualTransferredEnergy);
                }
            }
        }
    }
    
    // return transferred energy
    private int transferEnergyToFirstItem(int energy, List<ItemStack> storages){
        AtomicInteger energyTransferred = new AtomicInteger(0);
        int index = 0;
        while(energyTransferred.get() == 0 && index < storages.size()){
            ItemStack energyReceiverStack = storages.get(index++);
            energyReceiverStack.getCapability(CapabilityEnergy.ENERGY).ifPresent(energyReceiverStorage -> {
                energyTransferred.set(energyReceiverStorage.receiveEnergy(energy, false));
            });
        }
        return energyTransferred.get();
    }
    
    // return transferred energy
    private int transferEnergyToAll(int energy, List<ItemStack> storages){
        AtomicInteger energyTransferred = new AtomicInteger(0);
        List<ItemStack> stacksThatReallyWantSomeEnergy = storages.stream().filter(stack -> stack.getCapability(CapabilityEnergy.ENERGY).resolve().get().receiveEnergy(1, true) > 0).collect(Collectors.toList());
        if(stacksThatReallyWantSomeEnergy.size() > 0){
            int energyPerStack = (int) (energy / (stacksThatReallyWantSomeEnergy.size() * 1.0F));
            stacksThatReallyWantSomeEnergy.forEach(itemStack -> {
                itemStack.getCapability(CapabilityEnergy.ENERGY).ifPresent(energyReceiverStorage -> {
                    energyTransferred.addAndGet(energyReceiverStorage.receiveEnergy(energyPerStack, false));
                });
            });
        }
        return energyTransferred.get();
    }
    
    // return transferred energy
    private int transferEnergyRandom(int energy, List<ItemStack> storages){
        AtomicInteger energyTransferred = new AtomicInteger(0);
        int index = new Random().nextInt(storages.size());
        storages.get(index).getCapability(CapabilityEnergy.ENERGY).ifPresent(energyReceiverStorage -> {
            energyTransferred.set(energyReceiverStorage.receiveEnergy(energy, false));
        });
        return energyTransferred.get();
    }
    
    public static class StackEnergyStorage implements IEnergyStorage, ICapabilityProvider {
    
        private final LazyOptional<IEnergyStorage> holder = LazyOptional.of(() -> this);
        private final ItemStack stack;
        
        public StackEnergyStorage(ItemStack stack){
            this.stack = stack;
        }
        
        public void setEnergyStored(int energy){
            BatteryItem.setStoredEnergy(this.stack, energy);
        }
    
        @Override
        public int getEnergyStored(){
            return BatteryItem.getStoredEnergy(this.stack);
        }
    
        @Override
        public int getMaxEnergyStored(){
            return BatteryItem.getCapacity(this.stack);
        }
    
        @Override
        public boolean canExtract(){
            return this.getEnergyStored() > 0;
        }
    
        @Override
        public boolean canReceive(){
            return this.getEnergyStored() < this.getMaxEnergyStored();
        }
    
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate){
            if (!canReceive()){
                return 0;
            }
        
            int stored = this.getEnergyStored();
            int energyReceived = Math.min(this.getMaxEnergyStored() - stored, Math.min(BatteryItem.getMaxTransfer(this.stack), maxReceive));
            if (!simulate){
                this.setEnergyStored(stored + energyReceived);
            }
            return energyReceived;
        }
    
        @Override
        public int extractEnergy(int maxExtract, boolean simulate){
            if (!canExtract()){
                return 0;
            }
        
            int stored = this.getEnergyStored();
            int energyExtracted = Math.min(stored, Math.min(BatteryItem.getMaxTransfer(this.stack), maxExtract));
            if (!simulate){
                this.setEnergyStored(stored - energyExtracted);
            }
            return energyExtracted;
        }
        
        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side){
            return CapabilityEnergy.ENERGY.orEmpty(cap, holder);
        }
    }
}
