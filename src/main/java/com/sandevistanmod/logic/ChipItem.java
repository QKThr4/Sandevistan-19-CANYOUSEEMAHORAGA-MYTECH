package com.sandevistanmod.logic;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Chip do Link Neural: botao direito para inserir (sem o doutor). Tem durabilidade, consertavel na bigorna com redstone ou ferro. */
public class ChipItem extends Item {
    private final int type;
    private final int tier;
    private final String effectKey;

    public ChipItem(Properties properties, int type, int tier, String effectKey) {
        super(properties);
        this.type = type;
        this.tier = SandevistanData.clampTier(tier);
        this.effectKey = effectKey;
    }

    public int tier() {
        return tier;
    }

    public int type() {
        return type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!SandevistanLogic.insertChip(sp, stack, type, tier)) return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.REDSTONE) || repair.is(net.minecraft.world.item.Items.IRON_INGOT);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int ramBonus = (int) SandevistanData.ramBonus(tier);
        tooltip.add(Tx.t(ChatFormatting.AQUA, effectKey, ramBonus, 70 + ramBonus));
        if (SandevistanData.chipHasTiers(type)) tooltip.add(Tx.t(ChatFormatting.AQUA, "tooltip.tier", tier));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "tooltip.chip.1"));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "tooltip.chip.2", (int) SandevistanData.chipMindFor(type, tier)));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "tooltip.chip.3", (int) SandevistanData.chipLoadOf(type)));
        int max = SandevistanData.chipMax(type, tier);
        tooltip.add(Tx.t(ChatFormatting.DARK_GRAY, "chip.dur", Math.max(0, max - stack.getDamageValue()), max));
    }
}
