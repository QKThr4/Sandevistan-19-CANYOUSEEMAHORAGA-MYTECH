package com.sandevistanmod.energy;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import com.sandevistanmod.data.Tx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Cabo de energia: liga geradores a carregadores (e a blocos de outros mods) a qualquer distancia, sem perder FE. */
public class CableBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    /** 0 = recebe e entrega, 1 = so recebe (e leva), 2 = so entrega aos blocos encostados. */
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 2);
    private static final int MAX_CABLES = 1024;
    private static boolean routing;
    /** Verdadeiro enquanto uma bateria de armazenamento empurra energia: o cabo nao a devolve para outras baterias. */
    public static boolean fromBattery;

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape[] ARMS = {
        Block.box(5, 0, 5, 11, 5, 11), Block.box(5, 11, 5, 11, 16, 11), Block.box(5, 5, 0, 11, 11, 5),
        Block.box(5, 5, 11, 11, 11, 16), Block.box(0, 5, 5, 5, 11, 11), Block.box(11, 5, 5, 16, 11, 11)};

    public CableBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(SOUTH, false).setValue(EAST, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false).setValue(MODE, 0));
    }

    private static BooleanProperty prop(Direction d) {
        return switch (d) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, MODE);
    }

    /** Liga o braco do cabo a outros cabos, aos blocos de energia do mod e a qualquer bloco que tenha energia (FE), inclusive de outros mods. */
    private static boolean connects(LevelAccessor level, BlockPos npos, Direction d, BlockState n) {
        if (n.getBlock() instanceof CableBlock || n.getBlock() instanceof EnergyBlock || n.getBlock() instanceof StorageBatteryBlock) return true;
        return level instanceof Level l && l.getCapability(Capabilities.EnergyStorage.BLOCK, npos, d.getOpposite()) != null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int m = (state.getValue(MODE) + 1) % 3;
        level.setBlock(pos, state.setValue(MODE, m), 3);
        player.displayClientMessage(Tx.t(m == 0 ? ChatFormatting.YELLOW : m == 1 ? ChatFormatting.AQUA : ChatFormatting.GOLD, "cable.mode." + m), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState();
        for (Direction d : Direction.values()) {
            BlockPos np = ctx.getClickedPos().relative(d);
            s = s.setValue(prop(d), connects(ctx.getLevel(), np, d, ctx.getLevel().getBlockState(np)));
        }
        return s;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos npos) {
        return state.setValue(prop(dir), connects(level, npos, dir, neighbor));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape s = CORE;
        Direction[] order = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int k = 0; k < 6; k++) {
            if (state.getValue(prop(order[k]))) s = Shapes.or(s, ARMS[k]);
        }
        return s;
    }

    /** Capacidade de energia do cabo: tudo que entra e levado a todos os blocos que aceitam energia ao longo da rede de cabos. */
    public static IEnergyStorage storage(Level level, BlockPos start) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int toReceive, boolean simulate) {
                if (routing || toReceive <= 0 || level.getBlockState(start).getValue(MODE) == 2) return 0;
                routing = true;
                try {
                    return route(level, start, toReceive, simulate);
                } finally {
                    routing = false;
                }
            }

            @Override
            public int extractEnergy(int toExtract, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return 0;
            }

            @Override
            public int getMaxEnergyStored() {
                return 0;
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }

    private static int route(Level level, BlockPos start, int amount, boolean simulate) {
        int left = amount;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && left > 0 && seen.size() <= MAX_CABLES) {
            BlockPos p = queue.poll();
            BlockState ps = level.getBlockState(p);
            boolean delivers = !(ps.getBlock() instanceof CableBlock) || ps.getValue(MODE) != 1;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!level.isLoaded(n)) continue;
                if (level.getBlockState(n).getBlock() instanceof CableBlock) {
                    if (seen.add(n)) queue.add(n);
                    continue;
                }
                if (!delivers) continue;
                if (fromBattery && level.getBlockState(n).getBlock() instanceof StorageBatteryBlock) continue;
                IEnergyStorage t = level.getCapability(Capabilities.EnergyStorage.BLOCK, n, d.getOpposite());
                if (t == null || !t.canReceive()) continue;
                left -= t.receiveEnergy(left, simulate);
                if (left <= 0) break;
            }
        }
        return amount - left;
    }
}
