package de.canitzp.justabattery;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BatteryItem extends Item {

    public static final byte MODE_NONE = 0;
    public static final byte MODE_FIRST_FOUND = 1;
    public static final byte MODE_ALL = 2;
    public static final byte MODE_RANDOM = 3;
    public static final byte MODE_CHARGE_SURROUNDING_BLOCKS = 4;

    public static int getBatteryInitialCapacity(){
        return Math.max(1, Math.min(100_000, JustAConfig.get().battery_capacity));
    }

    public static int getBatteryStagedTransfer(){
        return Math.max(0, Math.min(100_000, JustAConfig.get().battery_transfer));
    }

    public static int getBatteryMaxLevel(){
        return Math.max(1, Math.min(20_000, JustAConfig.get().battery_max_level));
    }

    public static int getBatteryMaxTraceWidth(){
        return Math.max(1, Math.min(20_000, JustAConfig.get().battery_max_trace_width));
    }

    public static int getStoredEnergy(ItemStack stack){
        return stack.getOrDefault(JustABattery.DC_ENERGY, 0);
    }
    
    public static void setStoredEnergy(ItemStack stack, int energy){
        stack.set(JustABattery.DC_ENERGY, energy);
    }
    
    public static int getCapacity(ItemStack stack){
        return getBatteryInitialCapacity() * getLevel(stack);
    }
    
    public static int getMaxTransfer(ItemStack stack){
        return getBatteryStagedTransfer() * getTraceWidth(stack);
    }
    
    public static byte getMode(ItemStack stack){
        return stack.getOrDefault(JustABattery.DC_MODE, (byte) 0);
    }
    
    public static void setMode(ItemStack stack, byte mode){
        stack.set(JustABattery.DC_MODE, mode);
    }
    
    public static int getLevel(ItemStack stack){
        int level = stack.getOrDefault(JustABattery.DC_LEVEL, 0);
        return level <= 0 ? 1 : level;
    }
    
    public static void setLevel(ItemStack stack, int level){
        stack.set(JustABattery.DC_LEVEL, level);
    }
    
    public static int getTraceWidth(ItemStack stack){
        int traceWidth = stack.getOrDefault(JustABattery.DC_TRACE_WIDTH, 0);
        return traceWidth <= 0 ? 1 : traceWidth;
    }
    
    public static void setTraceWidth(ItemStack stack, int traceWidth){
        stack.set(JustABattery.DC_TRACE_WIDTH, traceWidth);
    }
    
    public BatteryItem(){
        super(new Properties().stacksTo(1).fireResistant());
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag){
        super.appendHoverText(stack, context, tooltip, flag);
    
        tooltip.add(Component.translatable("item.justabattery.desc.level", getLevel(stack)).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.justabattery.desc.tracewidth", getTraceWidth(stack), getMaxTransfer(stack)).withStyle(ChatFormatting.DARK_PURPLE));

        String gold_nugget_translated = Language.getInstance().getOrDefault(BuiltInRegistries.ITEM.getKey(Items.GOLD_NUGGET).toLanguageKey("item"), "gold nugget");
        tooltip.add(Component.translatable("item.justabattery.desc.upgrade_guide", gold_nugget_translated).withStyle(ChatFormatting.GRAY));

        byte mode = getMode(stack);
        tooltip.add(Component.translatable("item.justabattery.prefix.mode").append(" ").append(Component.translatable("item.justabattery.name.mode." + mode)).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.justabattery.desc.mode." + mode).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        
        tooltip.add(Component.translatable("item.justabattery.desc.energy", getStoredEnergy(stack),getCapacity(stack)).withStyle(ChatFormatting.RED));
    }
    
    @Override
    public Component getName(ItemStack stack){
        MutableComponent primary = null;
        MutableComponent secondary = Component.translatable(this.getDescriptionId(stack));
    
        int level = getLevel(stack);
        if(level >= 2 && level <= 5){
            primary = Component.translatable("item.justabattery.name.prefix." + level);
        } else if (level > 5){
            primary = Component.translatable("item.justabattery.name.prefix", level);
        }
        
        return primary != null ? primary.append(" ").append(secondary) : secondary;
    }
    
    @Override
    public Component getHighlightTip(ItemStack stack, Component displayName){
        byte mode = getMode(stack);
        if(mode >= MODE_FIRST_FOUND && mode <= MODE_CHARGE_SURROUNDING_BLOCKS){
            if(displayName instanceof MutableComponent mutableComponent){
                mutableComponent.append(" - ").append(Component.translatable("item.justabattery.prefix.mode")).append(" ").append(Component.translatable("item.justabattery.name.mode." + mode));
            }
        }
        return super.getHighlightTip(stack, displayName);
    }
    
    @Override
    public boolean isFoil(ItemStack stack){
        return getMode(stack) > MODE_NONE;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(!JustAConfig.get().allow_block_discharge){
            return super.useOn(context);
        }
        if(context.getPlayer() == null){
            return super.useOn(context);
        }
        if(context.getLevel().isClientSide){
            return super.useOn(context);
        }
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (blockEntity == null) {
            return super.useOn(context);
        }
        IEnergyStorage cap = context.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, context.getClickedPos(), context.getClickedFace());
        if (cap == null) {
            return super.useOn(context);
        }
        ItemStack battery = context.getItemInHand();
        int maxReceivableEnergy = Math.min(BatteryItem.getMaxTransfer(battery), BatteryItem.getCapacity(battery) - BatteryItem.getStoredEnergy(battery));
        if(maxReceivableEnergy > 0){
            int extractedEnergy = cap.extractEnergy(maxReceivableEnergy, false);
            if(extractedEnergy > 0){
                BatteryItem.setStoredEnergy(context.getItemInHand(), BatteryItem.getStoredEnergy(battery) + extractedEnergy);
                context.getPlayer().sendSystemMessage(Component.translatable("item.justabattery.desc.energy_received_from_block", extractedEnergy, context.getLevel().getBlockState(context.getClickedPos()).getBlock().getName()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if(victim.level().isClientSide){
            return super.hurtEnemy(stack, victim, attacker);
        }
        int chargupEnergy = JustAConfig.get().chargeup_creeper_energy_required;
        if(chargupEnergy >= 0){
            if(attacker instanceof Player){
                if(victim instanceof Creeper creeper){
                    if(!creeper.isPowered()){
                        int storedEnergy = BatteryItem.getStoredEnergy(stack);
                        if(storedEnergy >= chargupEnergy){
                            CompoundTag tag = new CompoundTag();
                            creeper.addAdditionalSaveData(tag);
                            tag.putBoolean("powered", true);
                            creeper.readAdditionalSaveData(tag);
                            BatteryItem.setStoredEnergy(stack, storedEnergy - chargupEnergy);
                            attacker.sendSystemMessage(Component.translatable("item.justabattery.desc.charged_creeper"));
                        }
                    }
                }
            }
        }
        return super.hurtEnemy(stack, victim, attacker);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context){
        if(context.getPlayer() != null && context.getPlayer().isCrouching()){
            if(!context.getLevel().isClientSide()){
                byte mode = getMode(stack);
                mode++;
                if(mode > MODE_CHARGE_SURROUNDING_BLOCKS){
                    mode = MODE_NONE;
                }
                setMode(stack, mode);
            }
            return InteractionResult.SUCCESS;
        }
        return super.onItemUseFirst(stack, context);
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        if(BatteryItem.getLevel(stack) == 1 && BatteryItem.getTraceWidth(stack) == 1){
            if(JustAConfig.get().charged_up_battery_on_craft){
                BatteryItem.setStoredEnergy(stack, BatteryItem.getCapacity(stack));
            }
        }
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

            List<ItemStack> energyItems = player.getInventory().items
                .stream()
                .filter(itemStack -> !itemStack.equals(stack)) // skip if the found stack is myself
                .filter(itemStack -> itemStack.getCapability(Capabilities.EnergyStorage.ITEM) != null)
                .collect(Collectors.toList());
            int storedEnergy = getStoredEnergy(stack);
            int maxTransferableEnergy = Math.min(storedEnergy, getMaxTransfer(stack));
            int actualTransferredEnergy = 0;
            switch(mode){
                case MODE_FIRST_FOUND -> actualTransferredEnergy = this.transferEnergyToFirstItem(maxTransferableEnergy, energyItems);
                case MODE_ALL -> actualTransferredEnergy = this.transferEnergyToAll(maxTransferableEnergy, energyItems);
                case MODE_RANDOM -> actualTransferredEnergy = this.transferEnergyRandom(maxTransferableEnergy, energyItems);
                case MODE_CHARGE_SURROUNDING_BLOCKS -> actualTransferredEnergy = this.transferEnergyToBlocks(maxTransferableEnergy, player);
            }
            if(actualTransferredEnergy > 0){
                BatteryItem.setStoredEnergy(stack, storedEnergy - actualTransferredEnergy);
            }
        }
    }
    
    // return transferred energy
    private int transferEnergyToFirstItem(int energy, List<ItemStack> storages){
        if(storages.isEmpty()){
            return 0;
        }
        AtomicInteger energyTransferred = new AtomicInteger(0);
        int index = 0;
        while(energyTransferred.get() == 0 && index < storages.size()){
            ItemStack energyReceiverStack = storages.get(index++);
            IEnergyStorage energyReceiverStorage = energyReceiverStack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (energyReceiverStorage != null) {
                energyTransferred.set(energyReceiverStorage.receiveEnergy(energy, false));
            }
        }
        return energyTransferred.get();
    }
    
    // return transferred energy
    private int transferEnergyToAll(int energy, List<ItemStack> storages){
        if(storages.isEmpty()){
            return 0;
        }
        AtomicInteger energyTransferred = new AtomicInteger(0);
        List<ItemStack> stacksThatReallyWantSomeEnergy = storages.stream().filter(stack -> stack.getCapability(Capabilities.EnergyStorage.ITEM).receiveEnergy(1, true) > 0).toList();
        if(!stacksThatReallyWantSomeEnergy.isEmpty()){
            int energyPerStack = (int) (energy / (stacksThatReallyWantSomeEnergy.size() * 1.0F));
            stacksThatReallyWantSomeEnergy.forEach(itemStack -> {
                IEnergyStorage energyReceiverStorage = itemStack.getCapability(Capabilities.EnergyStorage.ITEM);
                if(energyReceiverStorage != null){
                    energyTransferred.addAndGet(energyReceiverStorage.receiveEnergy(energyPerStack, false));
                }
            });
        }
        return energyTransferred.get();
    }
    
    // return transferred energy
    private int transferEnergyRandom(int energy, List<ItemStack> storages){
        if(storages.isEmpty()){
            return 0;
        }
        AtomicInteger energyTransferred = new AtomicInteger(0);
        int index = new Random().nextInt(storages.size());
        IEnergyStorage energyReceiverStorage = storages.get(index).getCapability(Capabilities.EnergyStorage.ITEM);
        if(energyReceiverStorage != null){
            energyTransferred.set(energyReceiverStorage.receiveEnergy(energy, false));
        }
        return energyTransferred.get();
    }

    private int transferEnergyToBlocks(int energy, Player player){
        AtomicInteger energyAvailable = new AtomicInteger(energy);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for(int xOffset = -2; xOffset <= 2; xOffset++){
            for(int yOffset = -2; yOffset <= 2; yOffset++){
                for(int zOffset = -2; zOffset <= 2; zOffset++){
                    if(xOffset == 0 && yOffset == 0 && zOffset == 0){
                        continue;
                    }
                    position.setX(player.blockPosition().getX() + xOffset);
                    position.setY(player.blockPosition().getY() + yOffset);
                    position.setZ(player.blockPosition().getZ() + zOffset);

                    BlockEntity tile = player.level().getBlockEntity(position);
                    if(tile != null){
                        for (Direction side : Direction.values()) {
                            IEnergyStorage energyStorage = player.level().getCapability(Capabilities.EnergyStorage.BLOCK, position, side);
                            if(energyStorage != null){
                                energyAvailable.set(energyAvailable.get() - energyStorage.receiveEnergy(energyAvailable.get(), false));
                            }
                            if(energyAvailable.get() <= 0){
                                return energy;
                            }
                        }
                    }
                }
            }
        }
        return energy - energyAvailable.get();
    }

    public void struckByLightning(LightningBolt lightningBolt, ItemStack batteryStack){
        int storedEnergy = BatteryItem.getStoredEnergy(batteryStack);
        int capacity = BatteryItem.getCapacity(batteryStack);

        int newEnergyLevel = Math.min(capacity, storedEnergy + 100_000);
        BatteryItem.setStoredEnergy(batteryStack, newEnergyLevel);
    }
    
    public static class StackEnergyStorage implements IEnergyStorage, ICapabilityProvider<ItemStack, Void, IEnergyStorage> {
    
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

        @Override
        public @Nonnull IEnergyStorage getCapability(ItemStack stack, Void unused) {
            return this;
        }
    }
}
