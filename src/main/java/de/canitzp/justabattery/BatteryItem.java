package de.canitzp.justabattery;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class BatteryItem extends Item {
    
    public static final int BATTERY_INITIAL_CAPACITY = 20_000;
    
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
    
    public static byte getMode(ItemStack stack){
        return stack.getOrCreateTag().getByte("Mode");
    }
    
    public static void setMode(ItemStack stack, byte mode){
        stack.getOrCreateTag().putByte("Mode", mode);
    }
    
    public static byte getLevel(ItemStack stack){
        byte level = stack.getOrCreateTag().getByte("Level");
        return level <= 0 ? 1 : level;
    }
    
    public static void setLevel(ItemStack stack, byte level){
        stack.getOrCreateTag().putByte("Level", level);
    }
    
    public BatteryItem(){
        super(new Properties().stacksTo(1).tab(CreativeModeTab.TAB_TOOLS));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag){
        super.appendHoverText(stack, level, tooltip, flag);
    
        tooltip.add(new TranslatableComponent("item.justabattery.desc.level", getLevel(stack)).withStyle(ChatFormatting.DARK_PURPLE));
        
        tooltip.add(new TranslatableComponent("item.justabattery.desc.mode." + getMode(stack)).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        
        tooltip.add(new TranslatableComponent("item.justabattery.desc.energy", getStoredEnergy(stack),getCapacity(stack)).withStyle(ChatFormatting.RED));
    }
    
    @Override
    public boolean isFoil(ItemStack stack){
        return getMode(stack) > MODE_NONE;
    }
    
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context){
        if(context.getPlayer() != null && context.getPlayer().isCrouching()){
            if(!context.getLevel().isClientSide()){
                byte mode = getMode(stack);
                mode++;
                if(mode > MODE_RANDOM){
                    mode = MODE_NONE;
                }
                setMode(stack, mode);
            }
            return InteractionResult.SUCCESS;
        }
        return super.onItemUseFirst(stack, context);
    }
    
    @Override
    public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> stacks){
        if(this.allowdedIn(tab)){
            for(byte i = 1; i < 3; i++){
                ItemStack empty = this.getDefaultInstance();
                setLevel(empty, i);
                
                ItemStack full = empty.copy();
                setStoredEnergy(full, getCapacity(full));
                
                stacks.add(empty);
                stacks.add(full);
            }
        }
    }
    
    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt){
        return new StackEnergyStorage(getCapacity(stack), stack);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int p_41407_, boolean p_41408_){
        if(level.isClientSide()){
            return;
        }
        if(entity instanceof Player player){
            int mode = getMode(stack);
            if(mode == 0){
                return;
            }
            
            int storedEnergy = getStoredEnergy(stack);
            List<ItemStack> energyItems = player.getInventory().items
                .stream()
                .filter(itemStack -> !ItemStack.isSameItemSameTags(itemStack, stack))
                .filter(itemStack -> itemStack.getCapability(CapabilityEnergy.ENERGY).isPresent())
                .collect(Collectors.toList());
            if(energyItems.size() > 0){
                switch(mode){
                    case MODE_FIRST_FOUND -> this.transferEnergyToFirstItem(storedEnergy, energyItems);
                    case MODE_ALL -> this.transferEnergyToAll(storedEnergy, energyItems);
                    case MODE_RANDOM -> this.transferEnergyRandom(storedEnergy, energyItems);
                }
            }
        }
    }
    
    private void transferEnergyToFirstItem(int energy, List<ItemStack> storages){
        AtomicBoolean energyTransferred = new AtomicBoolean(false);
        int index = 0;
        while(!energyTransferred.get() && index < storages.size()){
            ItemStack energyReceiverStack = storages.get(index++);
            energyReceiverStack.getCapability(CapabilityEnergy.ENERGY).ifPresent(energyReceiverStorage -> {
                int transferredEnergy = energyReceiverStorage.receiveEnergy(energy, false);
                if(transferredEnergy >= 0){
                    energyTransferred.set(true);
                }
            });
        }
    }
    
    private void transferEnergyToAll(int energy, List<ItemStack> storages){
        List<ItemStack> stacksThatReallyWantSomeEnergy = storages.stream().filter(stack -> stack.getCapability(CapabilityEnergy.ENERGY).resolve().get().receiveEnergy(1, true) > 0).collect(Collectors.toList());
        if(stacksThatReallyWantSomeEnergy.size() > 0){
            int energyPerStack = (int) (energy / (stacksThatReallyWantSomeEnergy.size() * 1.0F));
            stacksThatReallyWantSomeEnergy.forEach(itemStack -> {
                itemStack.getCapability(CapabilityEnergy.ENERGY).ifPresent(energyReceiverStorage -> {
                    energyReceiverStorage.receiveEnergy(energyPerStack, false);
                });
            });
        }
    }
    
    private void transferEnergyRandom(int energy, List<ItemStack> storages){
        int index = new Random().nextInt(storages.size());
        storages.get(index).getCapability(CapabilityEnergy.ENERGY).ifPresent(energyReceiverStorage -> {
            energyReceiverStorage.receiveEnergy(energy, false);
        });
    }
    
    public static class StackEnergyStorage extends EnergyStorage implements ICapabilityProvider {
    
        private final LazyOptional<IEnergyStorage> holder = LazyOptional.of(() -> this);
        private final ItemStack stack;
        
        public StackEnergyStorage(int capacity, ItemStack stack){
            super(capacity);
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
        public int receiveEnergy(int maxReceive, boolean simulate){
            if (!canReceive()){
                return 0;
            }
        
            int stored = this.getEnergyStored();
            int energyReceived = Math.min(this.capacity - stored, Math.min(this.maxReceive, maxReceive));
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
            int energyExtracted = Math.min(stored, Math.min(this.maxExtract, maxExtract));
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
