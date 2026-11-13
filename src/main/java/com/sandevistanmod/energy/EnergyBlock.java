package com.sandevistanmod.energy;

import com.sandevistanmod.data.Tx;
import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Blocos de energia: carregador de baterias e geradores (combustivel, solar, eolico, termico). */
public class EnergyBlock extends Block implements EntityBlock {
    public static final int CHARGER = 0;
    public static final int COMBUSTION = 1;
    public static final int SOLAR = 2;
    public static final int WIND = 3;
    public static final int THERMAL = 4;

    private final int kind;

    public EnergyBlock(Properties properties, int kind) {
        super(properties);
        this.kind = kind;
    }

    public int kind() {
        return kind;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModRegistries.ENERGY_BE.get()) return null;
        return (lv, pos, st, be) -> ((EnergyBlockEntity) be).serverTick();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EnergyBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof EnergyBlockEntity be)) return InteractionResult.PASS;
        if (kind == CHARGER || kind == COMBUSTION) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new EnergyMenu(id, inv, be, kind),
                    net.minecraft.network.chat.Component.translatable(getDescriptionId())), buf -> buf.writeVarInt(kind));
        } else {
            player.displayClientMessage(Tx.t(ChatFormatting.AQUA, "energy.gen", be.generating(), be.energy(), EnergyBlockEntity.BUFFER), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof EnergyBlockEntity be && !be.battery().isEmpty()) {
            Block.popResource(level, pos, be.battery());
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
