package com.sandevistanmod.bench;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Bancada do mod: 0 Montagem, 1 Medicinal (plantas), 2 Reciclador de Sucata. Abre um menu 3x3 so com itens das receitas dela. */
public class BenchBlock extends Block {
    public static final int ASSEMBLY = 0;
    public static final int MEDICINAL = 1;
    public static final int RECYCLER = 2;

    private final int kind;

    public BenchBlock(Properties properties, int kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new BenchMenu(id, inv, ContainerLevelAccess.create(level, pos), kind),
                Component.translatable(this.getDescriptionId())), buf -> buf.writeVarInt(kind));
        return InteractionResult.CONSUME;
    }
}
