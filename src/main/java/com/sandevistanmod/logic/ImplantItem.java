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

/** Implante simples (Braco de Ferro, Link Neural): so se instala com o Ripperdoc. */
public class ImplantItem extends Item {
    private final String[] tips;
    private final int slot;
    private final int tier;

    public ImplantItem(Properties properties, int slot, int tier, String... tips) {
        super(properties);
        this.slot = slot;
        this.tier = SandevistanData.clampTier(tier);
        this.tips = tips;
    }

    /** Encaixe do implante (1 braco, 3 link, 4 osso, 5 nervos, 6 coracao, 7 perna). */
    public int slot() {
        return slot;
    }

    /** Tier deste item (1 a 3). */
    public int tier() {
        return tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Tx.t(ChatFormatting.AQUA, "clinic.hint"));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Tx.t(ChatFormatting.GREEN, "tooltip.1"));
        if (SandevistanData.slotHasTiers(slot)) tooltip.add(Tx.t(ChatFormatting.AQUA, "tooltip.tier", tier));
        Object[] args = SandevistanData.statArgs(slot, tier);
        for (String t : tips) tooltip.add(Tx.t(ChatFormatting.GRAY, t, args));
        tooltip.add(Tx.t(ChatFormatting.GOLD, "tooltip.load", (int) SandevistanData.loadOf(slot, tier)));
    }
}
