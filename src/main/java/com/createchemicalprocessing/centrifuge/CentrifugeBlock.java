package com.createchemicalprocessing.centrifuge;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CentrifugeBlock extends Block {
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    private static final int RECHECK_TICKS = 20;

    public CentrifugeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ASSEMBLED, false).setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ASSEMBLED, RUNNING);
    }

    private static boolean hasSufficientSpeed(Level level, BlockPos bearingPos) {
        BlockState bearing = level.getBlockState(bearingPos);
        BlockState shaft = level.getBlockState(bearingPos.below());
        return bearing.hasProperty(BlockStateProperties.FACING)
                && bearing.getValue(BlockStateProperties.FACING) == Direction.UP
                && AllBlocks.SHAFT.has(shaft)
                && shaft.hasProperty(BlockStateProperties.AXIS)
                && shaft.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y
                && level.getBlockEntity(bearingPos) instanceof KineticBlockEntity kinetic
                && Math.abs(kinetic.getSpeed()) >= CentrifugeConfig.MINIMUM_RPM.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!AllItems.WRENCH.isIn(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && CentrifugeStructure.isAreaLoaded(level, pos)
                && CentrifugeStructure.findBearing(level, pos) == null) {
            ServerLevel server = (ServerLevel) level;
            DustParticleOptions red = new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.5f);
            for (BlockPos invalid : CentrifugeStructure.diagnose(level, pos).stream().limit(128).toList()) {
                server.sendParticles(red, invalid.getX() + 0.5, invalid.getY() + 0.5, invalid.getZ() + 0.5,
                        24, 0.35, 0.35, 0.35, 0.0);
            }
        }
        useWithoutItem(state, level, pos, player, hit);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (!CentrifugeStructure.isAreaLoaded(level, pos)) {
                player.displayClientMessage(Component.translatable("message.create_chemical_processing.centrifuge.unloaded"), true);
                return InteractionResult.CONSUME;
            }
            BlockPos bearing = CentrifugeStructure.findBearing(level, pos);
            boolean valid = bearing != null;
            boolean running = valid && hasSufficientSpeed(level, bearing);
            level.setBlock(pos, state.setValue(ASSEMBLED, valid).setValue(RUNNING, running), Block.UPDATE_ALL);
            if (valid) level.scheduleTick(pos, this, RECHECK_TICKS);
            player.displayClientMessage(valid
                    ? Component.translatable(running ? "message.create_chemical_processing.centrifuge.running" : "message.create_chemical_processing.centrifuge.slow", CentrifugeConfig.MINIMUM_RPM.get())
                    : Component.translatable("message.create_chemical_processing.centrifuge.invalid"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ASSEMBLED)) return;
        if (!CentrifugeStructure.isAreaLoaded(level, pos)) {
            level.scheduleTick(pos, this, RECHECK_TICKS);
            return;
        }
        BlockPos bearing = CentrifugeStructure.findBearing(level, pos);
        boolean valid = bearing != null;
        boolean running = valid && hasSufficientSpeed(level, bearing);
        BlockState updated = state.setValue(ASSEMBLED, valid).setValue(RUNNING, running);
        if (updated != state) level.setBlock(pos, updated, Block.UPDATE_ALL);
        if (valid) level.scheduleTick(pos, this, RECHECK_TICKS);
    }
}
