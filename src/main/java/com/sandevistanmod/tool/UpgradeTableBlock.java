package com.sandevistanmod.tool;

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

/** Mesa de Upgrades: encaixa modulos na Ferramenta Multiuso. */
public class UpgradeTableBlock extends Block {
    public UpgradeTableBlock(Properties p) {
        super(p);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new UpgradeMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable(this.getDescriptionId())));
        return InteractionResult.CONSUME;
    }
}
