package com.yision.fluidlogistics.block;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity.CreativeSmartFluidTank;
import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import com.simibubi.create.content.logistics.packagerLink.WiFiEffectPacket;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase.InterfaceProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.TankManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.yision.fluidlogistics.api.IFluidPackager;
import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.item.CompressedTankItem;
import com.yision.fluidlogistics.registry.AllBlockEntities;
import com.yision.fluidlogistics.registry.AllItems;

import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

public class FluidPackagerBlockEntity extends SmartBlockEntity implements Clearable, IFluidPackager {

    public static final int CYCLE = 20;

    public boolean redstonePowered;
    public int buttonCooldown;
    public String signBasedAddress;

    public TankManipulationBehaviour fluidTarget;
    public InvManipulationBehaviour itemTarget;
    public ItemStack heldBox;
    public ItemStack previouslyUnwrapped;

    public List<BigItemStack> queuedExitingPackages;
    public final FluidPackagerItemHandler inventory;

    public int animationTicks;
    public boolean animationInward;
    public List<FluidStack> pendingFluidsToInsert;

    public AbstractComputerBehaviour computerBehaviour;
    public Boolean hasCustomComputerAddress;
    public String customComputerAddress;

    private InventorySummary availableItems;
    private VersionedInventoryTrackerBehaviour invVersionTracker;
    private AdvancementBehaviour advancements;

    public FluidPackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        redstonePowered = state.getOptionalValue(FluidPackagerBlock.POWERED).orElse(false);
        heldBox = ItemStack.EMPTY;
        previouslyUnwrapped = ItemStack.EMPTY;
        inventory = new FluidPackagerItemHandler(this);
        animationTicks = 0;
        animationInward = true;
        queuedExitingPackages = new LinkedList<>();
        pendingFluidsToInsert = new LinkedList<>();
        signBasedAddress = "";
        customComputerAddress = "";
        hasCustomComputerAddress = false;
        buttonCooldown = 0;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            AllBlockEntities.FLUID_PACKAGER.get(),
            (be, context) -> be.inventory
        );
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(fluidTarget = new TankManipulationBehaviour(this, InterfaceProvider.oppositeOfBlockFacing()));
        behaviours.add(itemTarget = new InvManipulationBehaviour(this, InterfaceProvider.oppositeOfBlockFacing()));
        behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
        behaviours.add(advancements = new AdvancementBehaviour(this, AllAdvancements.PACKAGER));
        behaviours.add(computerBehaviour = ComputerCraftProxy.behaviour(this));
    }

    @Override
    public void initialize() {
        super.initialize();
        recheckIfLinksPresent();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        computerBehaviour.removePeripheral();
    }

    @Override
    public void tick() {
        super.tick();

        if (buttonCooldown > 0)
            buttonCooldown--;

        if (animationTicks == 0) {
            previouslyUnwrapped = ItemStack.EMPTY;

            if (!level.isClientSide() && !queuedExitingPackages.isEmpty() && heldBox.isEmpty()) {
                BigItemStack entry = queuedExitingPackages.get(0);
                heldBox = entry.stack.copy();

                entry.count--;
                if (entry.count <= 0)
                    queuedExitingPackages.remove(0);

                animationInward = false;
                animationTicks = CYCLE;
                notifyUpdate();
            }

            return;
        }

        if (level.isClientSide) {
            if (animationTicks == CYCLE - (animationInward ? 5 : 1))
                AllSoundEvents.PACKAGER.playAt(level, worldPosition, 1, 1, true);
            if (animationTicks == (animationInward ? 1 : 5))
                level.playLocalSound(worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.25f, 0.75f, true);
        }

        animationTicks--;

        if (animationTicks == 0 && !level.isClientSide()) {
            if (animationInward && !pendingFluidsToInsert.isEmpty()) {
                IFluidHandler fluidHandler = fluidTarget.getInventory();
                if (fluidHandler != null) {
                    for (FluidStack fluid : pendingFluidsToInsert) {
                        fluidHandler.fill(fluid, FluidAction.EXECUTE);
                    }
                }
                pendingFluidsToInsert.clear();
                triggerStockCheck();
            }
            wakeTheFrogs();
            setChanged();
        }
    }

    public void triggerStockCheck() {
        getAvailableItems();
    }

    @Override
    public InventorySummary getAvailableItems() {
        IFluidHandler fluidHandler = fluidTarget.getInventory();

        InventorySummary availableItems = new InventorySummary();

        if (fluidHandler != null) {
            boolean isCreativeHandler = fluidHandler instanceof CreativeSmartFluidTank;
            
            List<FluidAccumulator> accumulators = new java.util.ArrayList<>();

            for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
                FluidStack fluid = fluidHandler.getFluidInTank(tank);
                if (!fluid.isEmpty()) {
                    FluidStack template = fluid.copyWithAmount(1);
                    
                    FluidAccumulator acc = null;
                    for (FluidAccumulator existing : accumulators) {
                        if (FluidStack.isSameFluidSameComponents(existing.template, template)) {
                            acc = existing;
                            break;
                        }
                    }
                    
                    if (acc == null) {
                        acc = new FluidAccumulator(template, fluid.getAmount(), isCreativeHandler);
                        accumulators.add(acc);
                    } else {
                        acc.amount += fluid.getAmount();
                        acc.isCreative = acc.isCreative || isCreativeHandler;
                    }
                }
            }

            for (FluidAccumulator acc : accumulators) {
                ItemStack fluidDisplayItem = createFluidDisplayItem(acc.template);
                int displayAmount = acc.isCreative ? BigItemStack.INF : acc.amount;
                availableItems.add(fluidDisplayItem, displayAmount);
            }
        }

        submitNewArrivals(this.availableItems, availableItems);
        this.availableItems = availableItems;
        return availableItems;
    }

    private static class FluidAccumulator {
        final FluidStack template;
        int amount;
        boolean isCreative;

        FluidAccumulator(FluidStack template, int amount, boolean isCreative) {
            this.template = template;
            this.amount = amount;
            this.isCreative = isCreative;
        }
    }

    private ItemStack createFluidDisplayItem(FluidStack fluid) {
        ItemStack tankStack = new ItemStack(AllItems.COMPRESSED_STORAGE_TANK.get());
        CompressedTankItem.setFluidVirtual(tankStack, fluid.copyWithAmount(1));
        return tankStack;
    }

    private void submitNewArrivals(InventorySummary before, InventorySummary after) {
        if (before == null || after.isEmpty())
            return;

        Set<RequestPromiseQueue> promiseQueues = new HashSet<>();

        for (Direction d : Iterate.directions) {
            if (!level.isLoaded(worldPosition.relative(d)))
                continue;

            BlockState adjacentState = level.getBlockState(worldPosition.relative(d));
            if (AllBlocks.FACTORY_GAUGE.has(adjacentState)) {
                if (FactoryPanelBlock.connectedDirection(adjacentState) != d)
                    continue;
                if (!(level.getBlockEntity(worldPosition.relative(d)) instanceof FactoryPanelBlockEntity fpbe))
                    continue;
                if (!fpbe.restocker)
                    continue;
                for (FactoryPanelBehaviour behaviour : fpbe.panels.values()) {
                    if (!behaviour.isActive())
                        continue;
                    promiseQueues.add(behaviour.restockerPromises);
                }
            }

            if (AllBlocks.STOCK_LINK.has(adjacentState)) {
                if (PackagerLinkBlock.getConnectedDirection(adjacentState) != d)
                    continue;
                if (!(level.getBlockEntity(worldPosition.relative(d)) instanceof PackagerLinkBlockEntity plbe))
                    continue;
                UUID freqId = plbe.behaviour.freqId;
                if (!Create.LOGISTICS.hasQueuedPromises(freqId))
                    continue;
                promiseQueues.add(Create.LOGISTICS.getQueuedPromises(freqId));
            }
        }

        if (promiseQueues.isEmpty())
            return;

        for (BigItemStack entry : after.getStacks())
            before.add(entry.stack, -entry.count);
        for (RequestPromiseQueue queue : promiseQueues) {
            for (BigItemStack entry : before.getStacks()) {
                if (entry.count < 0) {
                    queue.itemEnteredSystem(entry.stack, -entry.count);
                }
            }
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level.isClientSide())
            return;
        recheckIfLinksPresent();
        if (!redstonePowered)
            return;
        redstonePowered = getBlockState().getOptionalValue(FluidPackagerBlock.POWERED).orElse(false);
        if (!redstoneModeActive())
            return;
        updateSignAddress();
        attemptToPackageFluid();
    }

    public void recheckIfLinksPresent() {
        if (level.isClientSide())
            return;
        BlockState blockState = getBlockState();
        if (!blockState.hasProperty(FluidPackagerBlock.LINKED))
            return;
        boolean shouldBeLinked = getLinkPos() != null;
        boolean isLinked = blockState.getValue(FluidPackagerBlock.LINKED);
        if (shouldBeLinked == isLinked)
            return;
        level.setBlockAndUpdate(worldPosition, blockState.cycle(FluidPackagerBlock.LINKED));
    }

    public boolean redstoneModeActive() {
        return !getBlockState().getOptionalValue(FluidPackagerBlock.LINKED).orElse(false);
    }

    private BlockPos getLinkPos() {
        for (Direction d : Iterate.directions) {
            BlockState adjacentState = level.getBlockState(worldPosition.relative(d));
            if (!AllBlocks.STOCK_LINK.has(adjacentState))
                continue;
            if (PackagerLinkBlock.getConnectedDirection(adjacentState) != d)
                continue;
            return worldPosition.relative(d);
        }
        return null;
    }

    public void flashLink() {
        for (Direction d : Iterate.directions) {
            BlockState adjacentState = level.getBlockState(worldPosition.relative(d));
            if (!AllBlocks.STOCK_LINK.has(adjacentState))
                continue;
            if (PackagerLinkBlock.getConnectedDirection(adjacentState) != d)
                continue;
            WiFiEffectPacket.send(level, worldPosition.relative(d));
            return;
        }
    }

    public boolean isTooBusyFor(RequestType type) {
        int queue = queuedExitingPackages.size();
        return queue >= switch (type) {
            case PLAYER -> 50;
            case REDSTONE -> 20;
            case RESTOCK -> 10;
        };
    }

    public void activate() {
        redstonePowered = true;
        setChanged();

        recheckIfLinksPresent();
        if (!redstoneModeActive())
            return;

        updateSignAddress();
        attemptToPackageFluid();

        if (buttonCooldown <= 0) {
            buttonCooldown = 40;
        }
    }

    public void attemptToPackageFluid() {
        if (!heldBox.isEmpty() || animationTicks != 0 || buttonCooldown > 0)
            return;

        IFluidHandler fluidHandler = fluidTarget.getInventory();
        if (fluidHandler == null)
            return;

        FluidStack extractedFluid = extractFluidFromTank(fluidHandler, Config.getFluidPerPackage());
        if (extractedFluid.isEmpty())
            return;

        ItemStack fluidPackage = createFluidPackage(extractedFluid);
        if (fluidPackage.isEmpty())
            return;

        computerBehaviour.prepareComputerEvent(new PackageEvent(fluidPackage, "package_created"));

        if (!signBasedAddress.isBlank()) {
            PackageItem.addAddress(fluidPackage, signBasedAddress);
        }

        heldBox = fluidPackage;
        animationInward = false;
        animationTicks = CYCLE;

        advancements.awardPlayer(AllAdvancements.PACKAGER);
        triggerStockCheck();
        notifyUpdate();
    }

    private FluidStack extractFluidFromTank(IFluidHandler handler, int maxAmount) {
        boolean isCreativeHandler = handler instanceof CreativeSmartFluidTank;
        
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluidInTank = handler.getFluidInTank(tank);
            if (fluidInTank.isEmpty())
                continue;

            if (isCreativeHandler) {
                return fluidInTank.copyWithAmount(maxAmount);
            }
            
            int drainAmount = Math.min(maxAmount, fluidInTank.getAmount());
            FluidStack toDrain = fluidInTank.copyWithAmount(drainAmount);
            FluidStack drained = handler.drain(toDrain, FluidAction.EXECUTE);
            if (!drained.isEmpty())
                return drained;
        }
        return FluidStack.EMPTY;
    }

    private ItemStack createFluidPackage(FluidStack fluid) {
        ItemStackHandler packageContents = new ItemStackHandler(PackageItem.SLOTS);
        int tankCapacity = CompressedTankItem.getCapacity();
        int maxTanks = 9;
        int tanksCreated = 0;

        FluidStack remainingFluid = fluid.copy();

        while (!remainingFluid.isEmpty() && tanksCreated < maxTanks) {
            int fluidForTank = Math.min(remainingFluid.getAmount(), tankCapacity);
            FluidStack tankFluid = remainingFluid.copyWithAmount(fluidForTank);

            ItemStack compressedTank = new ItemStack(AllItems.COMPRESSED_STORAGE_TANK.get());
            CompressedTankItem.setFluid(compressedTank, tankFluid);
            ItemHandlerHelper.insertItemStacked(packageContents, compressedTank, false);

            remainingFluid.shrink(fluidForTank);
            tanksCreated++;
        }

        ItemStack fluidPackage = new ItemStack(AllItems.RARE_FLUID_PACKAGE.get());
        fluidPackage.set(com.simibubi.create.AllDataComponents.PACKAGE_CONTENTS, 
            com.simibubi.create.foundation.item.ItemHelper.containerContentsFromHandler(packageContents));
        return fluidPackage;
    }

    public boolean unwrapBox(ItemStack box, boolean simulate) {
        if (animationTicks > 0)
            return false;

        Objects.requireNonNull(this.level);

        ItemStackHandler contents = PackageItem.getContents(box);
        List<ItemStack> items = ItemHelper.getNonEmptyStacks(contents);
        if (items.isEmpty())
            return true;

        IFluidHandler fluidHandler = fluidTarget.getInventory();
        if (fluidHandler == null) {
            return false;
        }

        int totalFluidAmount = 0;
        for (ItemStack item : items) {
            FluidStack fluid = CompressedTankItem.getFluid(item);
            if (!fluid.isEmpty()) {
                totalFluidAmount += fluid.getAmount();
            }
        }

        if (totalFluidAmount > 0) {
            int totalCapacity = 0;
            for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
                FluidStack tankFluid = fluidHandler.getFluidInTank(tank);
                int tankCapacity = fluidHandler.getTankCapacity(tank);
                totalCapacity += (tankCapacity - tankFluid.getAmount());

                if (!tankFluid.isEmpty()) {
                    for (ItemStack item : items) {
                        FluidStack packageFluid = CompressedTankItem.getFluid(item);
                        if (!packageFluid.isEmpty()) {
                            if (!FluidStack.isSameFluidSameComponents(tankFluid, packageFluid)) {
                                return false;
                            }
                        }
                    }
                }
            }

            if (totalCapacity < totalFluidAmount) {
                return false;
            }
        }

        if (simulate) {
            return true;
        }

        pendingFluidsToInsert.clear();
        for (ItemStack item : items) {
            FluidStack fluid = CompressedTankItem.getFluid(item);
            if (!fluid.isEmpty()) {
                pendingFluidsToInsert.add(fluid.copy());
            }
        }

        computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_received"));
        previouslyUnwrapped = box;
        animationInward = true;
        animationTicks = CYCLE;
        notifyUpdate();

        return true;
    }

    private boolean tryStandardUnpack(ItemStack box, boolean simulate, List<ItemStack> items) {
        PackageOrderWithCrafts orderContext = PackageItem.getOrderContext(box);
        Direction facing = getBlockState().getOptionalValue(FluidPackagerBlock.FACING).orElse(Direction.UP);
        BlockPos target = worldPosition.relative(facing.getOpposite());
        BlockState targetState = level.getBlockState(target);

        UnpackingHandler handler = UnpackingHandler.REGISTRY.get(targetState);
        UnpackingHandler toUse = handler != null ? handler : UnpackingHandler.DEFAULT;
        boolean unpacked = toUse.unpack(level, target, targetState, facing, items, orderContext, simulate);

        if (unpacked && !simulate) {
            computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_received"));
            previouslyUnwrapped = box;
            animationInward = true;
            animationTicks = CYCLE;
            notifyUpdate();
        }

        return unpacked;
    }

    public void updateSignAddress() {
        signBasedAddress = "";
        for (Direction side : Iterate.directions) {
            String address = getSign(side);
            if (address == null || address.isBlank())
                continue;
            signBasedAddress = address;
        }
        if (computerBehaviour.hasAttachedComputer() && hasCustomComputerAddress) {
            signBasedAddress = customComputerAddress;
        } else {
            hasCustomComputerAddress = false;
        }
    }

    protected String getSign(Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(side));
        if (!(blockEntity instanceof SignBlockEntity sign))
            return null;
        for (boolean front : Iterate.trueAndFalse) {
            SignText text = sign.getText(front);
            String address = "";
            for (Component component : text.getMessages(false)) {
                String string = component.getString();
                if (!string.isBlank())
                    address += string.trim() + " ";
            }
            if (!address.isBlank())
                return address.trim();
        }
        return null;
    }

    protected void wakeTheFrogs() {
        if (level.getBlockEntity(worldPosition.relative(Direction.UP)) instanceof FrogportBlockEntity port)
            port.tryPullingFromOwnAndAdjacentInventories();
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        redstonePowered = compound.getBoolean("Active");
        animationInward = compound.getBoolean("AnimationInward");
        animationTicks = compound.getInt("AnimationTicks");
        signBasedAddress = compound.getString("SignAddress");
        customComputerAddress = compound.getString("ComputerAddress");
        hasCustomComputerAddress = compound.getBoolean("HasComputerAddress");
        heldBox = ItemStack.parseOptional(registries, compound.getCompound("HeldBox"));
        previouslyUnwrapped = ItemStack.parseOptional(registries, compound.getCompound("InsertedBox"));
        if (clientPacket)
            return;
        queuedExitingPackages = NBTHelper.readCompoundList(compound.getList("QueuedExitingPackages", Tag.TAG_COMPOUND),
                c -> CatnipCodecUtils.decode(BigItemStack.CODEC, registries, c).orElseThrow());
        pendingFluidsToInsert = NBTHelper.readCompoundList(compound.getList("PendingFluids", Tag.TAG_COMPOUND),
                c -> CatnipCodecUtils.decode(FluidStack.OPTIONAL_CODEC, registries, c).orElse(FluidStack.EMPTY));
        if (compound.contains("LastSummary"))
            availableItems = CatnipCodecUtils.decodeOrNull(InventorySummary.CODEC, registries, compound.getCompound("LastSummary"));
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("Active", redstonePowered);
        compound.putBoolean("AnimationInward", animationInward);
        compound.putInt("AnimationTicks", animationTicks);
        compound.putString("SignAddress", signBasedAddress);
        compound.putString("ComputerAddress", customComputerAddress);
        compound.putBoolean("HasComputerAddress", hasCustomComputerAddress);
        compound.put("HeldBox", heldBox.saveOptional(registries));
        compound.put("InsertedBox", previouslyUnwrapped.saveOptional(registries));
        if (clientPacket)
            return;
        compound.put("QueuedExitingPackages", NBTHelper.writeCompoundList(queuedExitingPackages, bis -> {
            if (CatnipCodecUtils.encode(BigItemStack.CODEC, registries, bis).orElse(new CompoundTag()) instanceof CompoundTag ct)
                return ct;
            return new CompoundTag();
        }));
        compound.put("PendingFluids", NBTHelper.writeCompoundList(pendingFluidsToInsert, fs -> {
            if (CatnipCodecUtils.encode(FluidStack.OPTIONAL_CODEC, registries, fs).orElse(new CompoundTag()) instanceof CompoundTag ct)
                return ct;
            return new CompoundTag();
        }));
        if (availableItems != null)
            compound.put("LastSummary", CatnipCodecUtils.encode(InventorySummary.CODEC, registries, availableItems).orElseThrow());
    }

    @Override
    public void clearContent() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        queuedExitingPackages.clear();
        pendingFluidsToInsert.clear();
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, inventory);
        queuedExitingPackages.forEach(bigStack -> {
            for (int i = 0; i < bigStack.count; i++)
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), bigStack.stack.copy());
        });
        queuedExitingPackages.clear();
    }

    public float getTrayOffset(float partialTicks) {
        float tickCycle = animationInward ? animationTicks - partialTicks : animationTicks - 5 - partialTicks;
        float progress = Mth.clamp(tickCycle / (CYCLE - 5) * 2 - 1, -1, 1);
        progress = 1 - progress * progress;
        return progress * progress;
    }

    public ItemStack getRenderedBox() {
        if (animationInward)
            return animationTicks <= CYCLE / 2 ? ItemStack.EMPTY : previouslyUnwrapped;
        return animationTicks >= CYCLE / 2 ? ItemStack.EMPTY : heldBox;
    }

    @Override
    public boolean isTargetingSameInventory(@Nullable com.simibubi.create.content.logistics.packager.IdentifiedInventory inventory) {
        if (inventory == null)
            return false;

        IItemHandler targetHandler = this.itemTarget.getInventory();
        if (targetHandler == null)
            return false;

        if (inventory.identifier() != null) {
            BlockFace face = this.itemTarget.getTarget().getOpposite();
            return inventory.identifier().contains(face);
        } else {
            return isSameInventoryFallback(targetHandler, inventory.handler());
        }
    }

    private static boolean isSameInventoryFallback(IItemHandler first, IItemHandler second) {
        if (first == second)
            return true;

        for (int i = 0; i < second.getSlots(); i++) {
            ItemStack stackInSlot = second.getStackInSlot(i);
            if (stackInSlot.isEmpty())
                continue;
            for (int j = 0; j < first.getSlots(); j++)
                if (stackInSlot == first.getStackInSlot(j))
                    return true;
            break;
        }

        return false;
    }

    @Override
    public net.createmod.catnip.data.Pair<IFluidPackager, com.simibubi.create.content.logistics.packager.PackagingRequest> processFluidRequest(
            ItemStack stack, int amount, String address, int linkIndex,
            org.apache.commons.lang3.mutable.MutableBoolean finalLink, int orderId,
            @Nullable PackageOrderWithCrafts context,
            @Nullable com.simibubi.create.content.logistics.packager.IdentifiedInventory ignoredHandler) {
        
        if (isTargetingSameInventory(ignoredHandler))
            return null;

        if (!(stack.getItem() instanceof CompressedTankItem))
            return null;

        FluidStack requestedFluid = CompressedTankItem.getFluid(stack);
        if (requestedFluid.isEmpty())
            return null;

        IFluidHandler fluidHandler = fluidTarget.getInventory();
        if (fluidHandler == null)
            return null;

        boolean isCreativeHandler = fluidHandler instanceof CreativeSmartFluidTank;

        if (isCreativeHandler) {
            for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
                FluidStack tankFluid = fluidHandler.getFluidInTank(tank);
                if (!tankFluid.isEmpty() && FluidStack.isSameFluidSameComponents(tankFluid, requestedFluid)) {
                    return net.createmod.catnip.data.Pair.of(this,
                        com.simibubi.create.content.logistics.packager.PackagingRequest.create(
                            stack, amount, address, linkIndex, finalLink, 0, orderId, context));
                }
            }
            return null;
        }

        int availableAmount = 0;
        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            FluidStack tankFluid = fluidHandler.getFluidInTank(tank);
            if (!tankFluid.isEmpty() && FluidStack.isSameFluidSameComponents(tankFluid, requestedFluid)) {
                availableAmount += tankFluid.getAmount();
            }
        }

        if (availableAmount == 0)
            return null;

        int toWithdraw = Math.min(amount, availableAmount);
        
        return net.createmod.catnip.data.Pair.of(this,
            com.simibubi.create.content.logistics.packager.PackagingRequest.create(
                stack, toWithdraw, address, linkIndex, finalLink, 0, orderId, context));
    }

    @Override
    public void attemptToSendFluidRequest(java.util.List<com.simibubi.create.content.logistics.packager.PackagingRequest> queuedRequests) {
        if (queuedRequests == null || queuedRequests.isEmpty())
            return;

        IFluidHandler fluidHandler = fluidTarget.getInventory();
        if (fluidHandler == null) {
            queuedRequests.remove(0);
            return;
        }

        com.simibubi.create.content.logistics.packager.PackagingRequest nextRequest = queuedRequests.get(0);
        ItemStack requestedStack = nextRequest.item();
        
        if (!(requestedStack.getItem() instanceof CompressedTankItem)) {
            queuedRequests.remove(0);
            return;
        }

        FluidStack requestedFluid = CompressedTankItem.getFluid(requestedStack);
        if (requestedFluid.isEmpty()) {
            queuedRequests.remove(0);
            return;
        }

        int remainingCount = nextRequest.getCount();
        int totalFluidExtracted = 0;
        String fixedAddress = nextRequest.address();
        int fixedOrderId = nextRequest.orderId();
        int linkIndexInOrder = nextRequest.linkIndex();
        boolean finalLinkInOrder = nextRequest.finalLink().booleanValue();
        int packageIndexAtLink = nextRequest.packageCounter().getAndIncrement();
        boolean finalPackageAtLink = false;
        PackageOrderWithCrafts orderContext = nextRequest.context();

        int toExtract = Math.min(remainingCount, Config.getFluidPerPackage());
        
        FluidStack extractedFluid = extractSpecificFluidFromTank(fluidHandler, requestedFluid, toExtract);
        if (extractedFluid.isEmpty()) {
            queuedRequests.remove(0);
            return;
        }

        ItemStack fluidPackage = createFluidPackage(extractedFluid);
        if (fluidPackage.isEmpty()) {
            queuedRequests.remove(0);
            return;
        }

        computerBehaviour.prepareComputerEvent(new PackageEvent(fluidPackage, "package_created"));

        PackageItem.clearAddress(fluidPackage);
        if (fixedAddress != null)
            PackageItem.addAddress(fluidPackage, fixedAddress);
        
        int extractedAmount = extractedFluid.getAmount();
        totalFluidExtracted += extractedAmount;
        nextRequest.subtract(extractedAmount);

        if (nextRequest.isEmpty()) {
            finalPackageAtLink = true;
            queuedRequests.remove(0);
        }

        PackageItem.setOrder(fluidPackage, fixedOrderId, linkIndexInOrder, finalLinkInOrder, 
            packageIndexAtLink, finalPackageAtLink, orderContext);

        if (!heldBox.isEmpty() || animationTicks != 0) {
            queuedExitingPackages.add(new BigItemStack(fluidPackage, 1));
            return;
        }

        heldBox = fluidPackage;
        animationInward = false;
        animationTicks = CYCLE;

        advancements.awardPlayer(AllAdvancements.PACKAGER);
        triggerStockCheck();
        notifyUpdate();
    }

    private FluidStack extractSpecificFluidFromTank(IFluidHandler handler, FluidStack targetFluid, int maxAmount) {
        boolean isCreativeHandler = handler instanceof CreativeSmartFluidTank;
        
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluidInTank = handler.getFluidInTank(tank);
            if (fluidInTank.isEmpty())
                continue;
            if (!FluidStack.isSameFluidSameComponents(fluidInTank, targetFluid))
                continue;

            if (isCreativeHandler) {
                return fluidInTank.copyWithAmount(maxAmount);
            }
            
            int drainAmount = Math.min(maxAmount, fluidInTank.getAmount());
            FluidStack toDrain = fluidInTank.copyWithAmount(drainAmount);
            FluidStack drained = handler.drain(toDrain, FluidAction.EXECUTE);
            if (!drained.isEmpty())
                return drained;
        }
        return FluidStack.EMPTY;
    }

    @Override
    public void flashFluidLink() {
        flashLink();
    }

    @Override
    public boolean isFluidPackagerTooBusy(RequestType type) {
        return isTooBusyFor(type);
    }

    @Override
    @Nullable
    public com.simibubi.create.content.logistics.packager.IdentifiedInventory getIdentifiedInventory() {
        if (itemTarget == null)
            return null;
        return itemTarget.getIdentifiedInventory();
    }
}
