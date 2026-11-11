package com.sandevistanmod.logic;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Implante extra (botas, jetpack, XP, scanner, olhos): so o Ripperdoc instala. */
public class ExtItem extends Item {
    private final int slot;
    private final int tier;
    private final int eyeType;
    private final String tip;
    private final Object[] tipArgs;

    public ExtItem(Properties properties, int slot, int tier, int eyeType, String tip, Object... tipArgs) {
        super(properties);
        this.slot = slot;
        this.tier = tier;
        this.eyeType = eyeType;
        this.tip = tip;
        this.tipArgs = tipArgs;
    }

    public int slot() {
        return slot;
    }

    public int tier() {
        return tier;
    }

    public int eyeType() {
        return eyeType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) player.displayClientMessage(Tx.t(ChatFormatting.YELLOW, "ext.toDoctor"), true);
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Tx.t(ChatFormatting.AQUA, tip, tipArgs));
        boolean energy = slot <= SandevistanData.EXT_XRAY || (slot == SandevistanData.EXT_EYE && eyeType >= 2);
        tooltip.add(Tx.t(ChatFormatting.GRAY, energy ? "tooltip.ext.energy" : "tooltip.ext.noenergy"));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "tooltip.ext.load", Math.round(SandevistanData.extLoad(slot, tier, eyeType))));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "tooltip.ext.use"));
    }
}
