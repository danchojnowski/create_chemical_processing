package com.createchemicalprocessing.centrifuge;

import com.createchemicalprocessing.CreateChemicalProcessing;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Validates a stationary centrifuge shell. The origin is the north-west interior floor position. */
public final class CentrifugeStructure {
    public static final TagKey<Block> WALLS = tag("centrifuge_walls");
    public static final TagKey<Block> FLOORS = tag("centrifuge_floor_slabs");
    public static final TagKey<Block> ROOFS = tag("centrifuge_roof_stairs");
    private static final ResourceLocation BEARING = ResourceLocation.fromNamespaceAndPath("create", "mechanical_bearing");

    private CentrifugeStructure() {}

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("create_chemical_processing", name));
    }

    /** Check all chunks that could contain a shell, without loading any of them. */
    public static boolean isAreaLoaded(LevelReader level, BlockPos controller) {
        for (int x = -2; x <= 3; x++) {
            for (int z = -2; z <= 3; z++) {
                if (!level.hasChunkAt(controller.offset(x, 0, z))) return false;
            }
        }
        return true;
    }

    /** Search downward for a bearing and test each supported interior width. */
    public static BlockPos findBearing(LevelReader level, BlockPos controller) {
        if (!isAreaLoaded(level, controller)) return null;
        Block bearing = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(BEARING);
        for (int bearingY = controller.getY() - 4; bearingY >= level.getMinBuildHeight(); bearingY--) {
            BlockPos bearingPos = new BlockPos(controller.getX(), bearingY, controller.getZ());
            if (!level.getBlockState(bearingPos).is(bearing)) continue;
            int height = controller.getY() - bearingY - 3;
            for (int width = 1; width <= 3; width++) {
                int offset = (width - 1) / 2;
                BlockPos origin = bearingPos.offset(-offset, 1, -offset);
                if (isValid(level, origin, width, height)) return bearingPos;
            }
        }
        return null;
    }

    public static boolean find(LevelReader level, BlockPos controller) {
        return findBearing(level, controller) != null;
    }

    /** Best-effort diagnosis: choose the bearing and chamber size with the fewest errors. */
    public static List<BlockPos> diagnose(LevelReader level, BlockPos controller) {
        List<BlockPos> best = null;
        Block bearing = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(BEARING);
        for (int y = controller.getY() - 4; y >= level.getMinBuildHeight(); y--) {
            BlockPos base = new BlockPos(controller.getX(), y, controller.getZ());
            if (!level.getBlockState(base).is(bearing)) continue;
            int height = controller.getY() - y - 3;
            for (int width = 1; width <= 3; width++) {
                int offset = (width - 1) / 2;
                List<BlockPos> errors = invalidBlocks(level, base.offset(-offset, 1, -offset), width, height);
                if (best == null || errors.size() < best.size()) best = errors;
            }
        }
        // Without a bearing, there is no unambiguous way to infer the intended height.
        return best == null ? List.of() : best;
    }

    private static List<BlockPos> invalidBlocks(LevelReader level, BlockPos origin, int width, int height) {
        List<BlockPos> errors = new ArrayList<>();
        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= width; z++) {
                boolean edge = x == -1 || z == -1 || x == width || z == width;
                BlockPos floor = origin.offset(x, 0, z);
                BlockPos roof = origin.offset(x, height + 1, z);
                BlockState floorState = level.getBlockState(floor);
                BlockState roofState = level.getBlockState(roof);
                if (!floorState.is(FLOORS) || !(floorState.getBlock() instanceof SlabBlock) || floorState.getValue(SlabBlock.TYPE) != SlabType.BOTTOM) errors.add(floor);
                if (!roofState.is(ROOFS) || !(roofState.getBlock() instanceof StairBlock)) errors.add(roof);
                for (int y = 1; y <= height; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (edge ? !state.is(WALLS) : !state.isAir()) errors.add(pos);
                }
            }
        }
        return errors;
    }

    /**
     * Checks a square chamber, including its one-block-thick shell. Floor is at origin Y;
     * height counts empty interior blocks above it. Returns false for unloaded positions.
     * The bearing is centered below the floor, rounded toward north-west for even sizes.
     */
    public static boolean isValid(LevelReader level, BlockPos origin, int width, int height) {
        if (width < 1 || width > 3 || height < 1 || height > level.getMaxBuildHeight() - level.getMinBuildHeight()) return false;
        long roofY = (long) origin.getY() + height + 1;
        if (origin.getY() <= level.getMinBuildHeight() || roofY + 1 >= level.getMaxBuildHeight()) return false;
        BlockPos controllerPos = origin.offset((width - 1) / 2, height + 2, (width - 1) / 2);
        if (!level.hasChunkAt(controllerPos) || !level.getBlockState(controllerPos).is(CreateChemicalProcessing.CENTRIFUGE.get())) return false;
        BlockPos bearingPos = origin.offset((width - 1) / 2, -1, (width - 1) / 2);
        if (!level.hasChunkAt(bearingPos) || !level.getBlockState(bearingPos).is(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(BEARING))) return false;
        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= width; z++) {
                if (!level.hasChunkAt(origin.offset(x, 0, z))) return false;
            }
        }
        return invalidBlocks(level, origin, width, height).isEmpty();
    }
}
