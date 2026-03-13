package com.yision.fluidlogistics.block.MultiFluidAccessPort;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.fluidlogistics.registry.AllBlockEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.yision.fluidlogistics.block.MultiFluidAccessPort.MultiFluidAccessPortBlock.ATTACHED;

public class MultiFluidAccessPortBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    private final Map<Direction, IFluidHandler> sideCapabilities;
    private Map<Direction, FilteringBehaviour> filters;
    private boolean powered;

    public MultiFluidAccessPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        sideCapabilities = new EnumMap<>(Direction.class);
    }

    @Override
    public void initialize() {
        super.initialize();
        updateConnectedStorage();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (filters == null) {
            filters = new EnumMap<>(Direction.class);
        }
        filters.clear();
        addFilterBehaviour(behaviours, getLeftOutputDirection());
        addFilterBehaviour(behaviours, getRightOutputDirection());
        addFilterBehaviour(behaviours, getBackOutputDirection());
    }

    private void addFilterBehaviour(List<BlockEntityBehaviour> behaviours, Direction side) {
        FilteringBehaviour behaviour = new PortFilteringBehaviour(this, new OutputFilterSlot(side), typeFor(side));
        createFilter(side, behaviour);
        filters.put(side, behaviour);
        behaviours.add(behaviour);
    }

    private FilteringBehaviour createFilter(Direction side, FilteringBehaviour behaviour) {
        behaviour.forFluids();
        behaviour.setLabel(Component.translatable(getFilterLabelKey(side)).copy());
        return behaviour;
    }

    private String getFilterLabelKey(Direction side) {
        if (side == getLeftOutputDirection()) {
            return "block.fluidlogistics.multi_fluid_access_port.filter_left";
        }
        if (side == getRightOutputDirection()) {
            return "block.fluidlogistics.multi_fluid_access_port.filter_right";
        }
        return "block.fluidlogistics.multi_fluid_access_port.filter_back";
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AllBlockEntities.MULTI_FLUID_ACCESS_PORT.get(),
            (be, side) -> be.getFluidCapability(side));
    }

    public IFluidHandler getFluidCapability(Direction side) {
        if (side == null || !isOutputSide(side)) {
            return null;
        }
        return sideCapabilities.computeIfAbsent(side, output -> new PortFluidHandler(this, output));
    }

    public void updateConnectedStorage() {
        boolean previouslyPowered = powered;
        Level level = getLevel();
        if (level == null) {
            return;
        }

        powered = level.hasNeighborSignal(worldPosition);
        boolean attached = !powered && getConnectedFluidHandler() != null;
        if (previouslyPowered != powered || getBlockState().getValue(ATTACHED) != attached) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(ATTACHED, attached));
            notifyUpdate();
        }
    }

    private IFluidHandler getConnectedFluidHandler() {
        if (level == null || powered) {
            return null;
        }
        Direction targetDirection = DirectedDirectionalBlock.getTargetDirection(getBlockState());
        BlockPos targetPos = worldPosition.relative(targetDirection);
        IFluidHandler handler =
            level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, targetDirection.getOpposite());
        if (handler instanceof WrappedPortFluidHandler) {
            return null;
        }
        return handler;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (powered) {
            return false;
        }

        IFluidHandler handler = getConnectedFluidHandler();
        if (handler == null) {
            return false;
        }

        if (!hasAnyFluid(handler)) {
            return false;
        }

        if (hasAnyFilter()) {
            return addFilteredTooltip(tooltip, handler);
        }
        return containedFluidTooltip(tooltip, isPlayerSneaking, handler);
    }

    private boolean hasAnyFilter() {
        return hasFilter(getLeftOutputDirection()) || hasFilter(getRightOutputDirection()) || hasFilter(getBackOutputDirection());
    }

    private boolean hasFilter(Direction side) {
        return !getFilterItem(side).isEmpty();
    }

    private boolean addFilteredTooltip(List<Component> tooltip, IFluidHandler handler) {
        List<DisplayedFluid> merged = new ArrayList<>();
        mergeFilteredFluid(merged, handler, getLeftOutputDirection());
        mergeFilteredFluid(merged, handler, getRightOutputDirection());
        mergeFilteredFluid(merged, handler, getBackOutputDirection());
        if (merged.isEmpty()) {
            return false;
        }

        CreateLang.builder()
            .translate("gui.goggles.fluid_container")
            .forGoggles(tooltip);

        boolean added = false;
        for (DisplayedFluid entry : merged) {
            CreateLang.fluidName(entry.stack)
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);
            CreateLang.builder()
                .add(CreateLang.number(entry.stack.getAmount())
                    .text("mB")
                    .style(ChatFormatting.GOLD))
                .text(ChatFormatting.GRAY, " / ")
                .add(CreateLang.number(entry.capacity)
                    .text("mB")
                    .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);
            added = true;
        }
        return added;
    }

    private void mergeFilteredFluid(List<DisplayedFluid> merged, IFluidHandler handler, Direction side) {
        if (!hasFilter(side)) {
            return;
        }

        DisplayedFluid match = getMatchingDisplayedFluid(side, handler);
        if (match == null) {
            return;
        }

        for (DisplayedFluid existing : merged) {
            if (FluidStack.isSameFluidSameComponents(existing.stack, match.stack)) {
                return;
            }
        }

        merged.add(match);
    }

    private DisplayedFluid getMatchingDisplayedFluid(Direction side, IFluidHandler handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (!fluid.isEmpty() && testFilter(side, fluid)) {
                return new DisplayedFluid(fluid.copy(), handler.getTankCapacity(tank));
            }
        }
        return null;
    }

    private boolean hasAnyFluid(IFluidHandler handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            if (!handler.getFluidInTank(tank).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private record DisplayedFluid(FluidStack stack, int capacity) {
    }

    private boolean isOutputSide(Direction side) {
        return side == getLeftOutputDirection() || side == getRightOutputDirection() || side == getBackOutputDirection();
    }

    Direction getBackOutputDirection() {
        return getBlockState().getValue(MultiFluidAccessPortBlock.FACING).getOpposite();
    }

    Direction getLeftOutputDirection() {
        return getBlockState().getValue(MultiFluidAccessPortBlock.FACING).getCounterClockWise();
    }

    Direction getRightOutputDirection() {
        return getBlockState().getValue(MultiFluidAccessPortBlock.FACING).getClockWise();
    }

    ItemStack getFilterItem(Direction side) {
        if (filters == null) {
            return ItemStack.EMPTY;
        }
        FilteringBehaviour behaviour = filters.get(side);
        return behaviour == null ? ItemStack.EMPTY : behaviour.getFilter();
    }

    private boolean testFilter(Direction side, FluidStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        FilteringBehaviour behaviour = filters.get(normalizeOutputSide(side));
        return behaviour == null || behaviour.getFilter().isEmpty() || behaviour.test(stack);
    }

    private Direction normalizeOutputSide(Direction side) {
        Direction left = getLeftOutputDirection();
        Direction right = getRightOutputDirection();
        Direction back = getBackOutputDirection();
        if (side == left) {
            return left;
        }
        if (side == right) {
            return right;
        }
        return back;
    }

    private BehaviourType<?> typeFor(Direction side) {
        if (side == getLeftOutputDirection()) {
            return PortFilteringBehaviour.LEFT_TYPE;
        }
        if (side == getRightOutputDirection()) {
            return PortFilteringBehaviour.RIGHT_TYPE;
        }
        return PortFilteringBehaviour.BACK_TYPE;
    }

    private FluidStack getMatchingFluid(Direction side, IFluidHandler handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (!fluid.isEmpty() && testFilter(side, fluid)) {
                return fluid.copy();
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        powered = tag.getBoolean("Powered");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Powered", powered);
    }

    private interface WrappedPortFluidHandler {
    }

    private static class PortFluidHandler implements IFluidHandler, WrappedPortFluidHandler {
        private final MultiFluidAccessPortBlockEntity blockEntity;
        private final Direction side;
        private final ThreadLocal<Boolean> recursionGuard = ThreadLocal.withInitial(() -> false);

        private PortFluidHandler(MultiFluidAccessPortBlockEntity blockEntity, Direction side) {
            this.blockEntity = blockEntity;
            this.side = side;
        }

        private <T> T preventRecursion(Supplier<T> value, T fallback) {
            if (recursionGuard.get()) {
                return fallback;
            }
            recursionGuard.set(true);
            try {
                return value.get();
            } finally {
                recursionGuard.set(false);
            }
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0) {
                return FluidStack.EMPTY;
            }
            return preventRecursion(() -> {
                IFluidHandler handler = blockEntity.getConnectedFluidHandler();
                return handler == null ? FluidStack.EMPTY : blockEntity.getMatchingFluid(side, handler);
            }, FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank != 0) {
                return 0;
            }
            return preventRecursion(() -> {
                IFluidHandler handler = blockEntity.getConnectedFluidHandler();
                if (handler == null) {
                    return 0;
                }
                FluidStack match = blockEntity.getMatchingFluid(side, handler);
                if (match.isEmpty()) {
                    return handler.getTanks() > 0 ? handler.getTankCapacity(0) : 0;
                }
                for (int i = 0; i < handler.getTanks(); i++) {
                    FluidStack fluid = handler.getFluidInTank(i);
                    if (FluidStack.isSameFluidSameComponents(fluid, match)) {
                        return handler.getTankCapacity(i);
                    }
                }
                return 0;
            }, 0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && blockEntity.testFilter(side, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return preventRecursion(() -> {
                IFluidHandler handler = blockEntity.getConnectedFluidHandler();
                if (handler == null || resource.isEmpty() || !blockEntity.testFilter(side, resource)) {
                    return 0;
                }
                return handler.fill(resource, action);
            }, 0);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return preventRecursion(() -> {
                IFluidHandler handler = blockEntity.getConnectedFluidHandler();
                if (handler == null || resource.isEmpty() || !blockEntity.testFilter(side, resource)) {
                    return FluidStack.EMPTY;
                }
                return handler.drain(resource, action);
            }, FluidStack.EMPTY);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return preventRecursion(() -> {
                IFluidHandler handler = blockEntity.getConnectedFluidHandler();
                if (handler == null || maxDrain <= 0) {
                    return FluidStack.EMPTY;
                }
                FluidStack matching = blockEntity.getMatchingFluid(side, handler);
                if (matching.isEmpty()) {
                    return FluidStack.EMPTY;
                }
                return handler.drain(matching.copyWithAmount(maxDrain), action);
            }, FluidStack.EMPTY);
        }
    }

    private static class OutputFilterSlot extends ValueBoxTransform {
        private final Direction outputSide;

        private OutputFilterSlot(Direction outputSide) {
            this.outputSide = outputSide;
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction facing = state.getValue(MultiFluidAccessPortBlock.FACING);
            AttachFace target = state.getValue(MultiFluidAccessPortBlock.TARGET);
            if (target == AttachFace.WALL) {
                Vec3 base = baseTopLocation(facing);
                return base == null ? null : VecHelper.rotateCentered(base, wallRotation(facing), Axis.Y);
            }

            Vec3 base = baseVerticalLocation(facing, target);
            return base == null ? null : VecHelper.rotateCentered(base, verticalRotation(facing), Axis.Y);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            Direction facing = state.getValue(MultiFluidAccessPortBlock.FACING);
            AttachFace target = state.getValue(MultiFluidAccessPortBlock.TARGET);
            if (target == AttachFace.WALL) {
                TransformStack transform = TransformStack.of(ms)
                    .rotateYDegrees(wallRotation(facing))
                    .rotateXDegrees(90);
                if (outputSide == facing.getCounterClockWise()) {
                    transform.rotateZDegrees(90);
                } else if (outputSide == facing.getClockWise()) {
                    transform.rotateZDegrees(-90);
                }
                return;
            }

            TransformStack.of(ms).rotateYDegrees(verticalRotation(facing) + 180);
        }

        @Override
        public float getScale() {
            return super.getScale() * 0.95f;
        }

        @Override
        public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
            Vec3 offset = getLocalOffset(level, pos, state);
            if (offset == null) {
                return false;
            }

            AttachFace target = state.getValue(MultiFluidAccessPortBlock.TARGET);
            if (target == AttachFace.WALL) {
                return Math.abs(localHit.y - offset.y) < 3 / 16f
                    && Math.abs(localHit.x - offset.x) < 3 / 16f
                    && Math.abs(localHit.z - offset.z) < 3 / 16f;
            }

            Direction face = state.getValue(MultiFluidAccessPortBlock.FACING);
            if (Math.abs(distanceToFace(localHit, offset, face)) >= 3 / 16f) {
                return false;
            }
            return Math.abs(localHit.x - offset.x) < 3 / 16f
                && Math.abs(localHit.y - offset.y) < 3 / 16f
                && Math.abs(localHit.z - offset.z) < 3 / 16f;
        }

        private Vec3 baseTopLocation(Direction facing) {
            if (outputSide == facing.getCounterClockWise()) {
                return VecHelper.voxelSpace(13, 15.75, 8);
            }
            if (outputSide == facing.getClockWise()) {
                return VecHelper.voxelSpace(3, 15.75, 8);
            }
            if (outputSide == facing.getOpposite()) {
                return VecHelper.voxelSpace(8, 15.75, 3);
            }
            return null;
        }

        private Vec3 baseVerticalLocation(Direction facing, AttachFace target) {
            if (outputSide == facing.getCounterClockWise()) {
                return VecHelper.voxelSpace(3, 8, 15.75);
            }
            if (outputSide == facing.getClockWise()) {
                return VecHelper.voxelSpace(13, 8, 15.75);
            }
            if (outputSide == facing.getOpposite()) {
                return VecHelper.voxelSpace(8, target == AttachFace.FLOOR ? 13 : 3, 15.75);
            }
            return null;
        }

        private float rotationFromFacing(Direction facing) {
            return switch (facing) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
        }

        private float wallRotation(Direction facing) {
            float rotation = rotationFromFacing(facing);
            if (facing.getAxis() == Axis.Z) {
                rotation += 180;
            }
            return rotation;
        }

        private float verticalRotation(Direction facing) {
            float rotation = rotationFromFacing(facing);
            if (facing.getAxis() == Axis.Z) {
                rotation += 180;
            }
            return rotation;
        }

        private double distanceToFace(Vec3 hit, Vec3 offset, Direction face) {
            return switch (face) {
                case NORTH, SOUTH -> hit.z - offset.z;
                case EAST, WEST -> hit.x - offset.x;
                default -> 0;
            };
        }

    }

    static ValueBoxTransform createSlotTransform(Direction side) {
        return new OutputFilterSlot(side);
    }

    private static class PortFilteringBehaviour extends FilteringBehaviour {
        private static final BehaviourType<PortFilteringBehaviour> LEFT_TYPE = new BehaviourType<>();
        private static final BehaviourType<PortFilteringBehaviour> RIGHT_TYPE = new BehaviourType<>();
        private static final BehaviourType<PortFilteringBehaviour> BACK_TYPE = new BehaviourType<>();

        private final BehaviourType<?> type;
        private final String key;
        private final int netId;

        private PortFilteringBehaviour(SmartBlockEntity be, ValueBoxTransform slot, BehaviourType<?> type) {
            super(be, slot);
            this.type = type;
            if (type == LEFT_TYPE) {
                this.key = "FilteringLeft";
                this.netId = 21;
            } else if (type == RIGHT_TYPE) {
                this.key = "FilteringRight";
                this.netId = 22;
            } else {
                this.key = "FilteringBack";
                this.netId = 23;
            }
        }

        @Override
        public BehaviourType<?> getType() {
            return type;
        }

        @Override
        public String getClipboardKey() {
            return key;
        }

        @Override
        public int netId() {
            return netId;
        }

        @Override
        public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
            CompoundTag data = new CompoundTag();
            super.write(data, registries, clientPacket);
            nbt.put(key, data);
        }

        @Override
        public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
            if (nbt.contains(key)) {
                super.read(nbt.getCompound(key), registries, clientPacket);
            }
        }

        @Override
        public void writeSafe(CompoundTag nbt, HolderLookup.Provider registries) {
            CompoundTag data = new CompoundTag();
            super.writeSafe(data, registries);
            nbt.put(key, data);
        }
    }
}
