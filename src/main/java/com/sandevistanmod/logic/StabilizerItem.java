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

/** Estabilizador Neural: seringa. Segure o botao direito para se injetar (nao e de beber). */
public class StabilizerItem extends Item implements MedTier {
    private final int tier;

    public StabilizerItem(Properties properties, int tier) {
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
        return UseAnim.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 30;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp) {
            if (!SandevistanLogic.takeRemedy(sp, tier)) return stack;
            level.playSound(null, sp.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1f, 1.4f);
            if (!sp.getAbilities().instabuild) {
                // so sobra a seringa vazia
                ItemStack empty = new ItemStack(com.sandevistanmod.registry.ModRegistries.SERINGA.get());
                stack.shrink(1);
                if (stack.isEmpty()) return empty;
                if (!sp.getInventory().add(empty)) sp.drop(empty, false);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Tx.t(ChatFormatting.GREEN, "tooltip.stabilizer.1", SandevistanLogic.REMEDY_MIND[tier - 1]));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "tooltip.stabilizer.2"));
    }
}
