package com.sandevistanmod.logic;

import com.sandevistanmod.data.Tx;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Pilula de Sanidade: baixa 1 de Insanidade por segundo durante 30 s. */
public class PillItem extends Item implements MedTier {
    private final int tier;

    public PillItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 16;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp) {
            if (!SandevistanLogic.takePill(sp, tier)) return stack;
            level.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1f, 1.3f);
            if (!sp.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Tx.t(ChatFormatting.GREEN, "tooltip.pill.1", SandevistanLogic.PILL_RATE[tier - 1], (int) (SandevistanLogic.PILL_RATE[tier - 1] * 30)));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "tooltip.pill.2"));
    }
}
