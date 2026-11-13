package com.sandevistanmod.energy;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.logic.SandevistanLogic;
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
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Bateria: a energia guardada e (durabilidade maxima - dano). Botao direito instala no jogador; tambem expoe FE para outros mods. */
public class BatteryItem extends Item {
    private final int type;

    public BatteryItem(Properties properties, int type) {
        super(properties);
        this.type = type;
    }

    public int type() {
        return type;
    }

    public static int energyOf(ItemStack st) {
        return Math.max(0, st.getMaxDamage() - st.getDamageValue());
    }

    /** Bateria vazia do tipo (1 a 5), como sai das bancadas. */
    public static ItemStack empty(Item item) {
        ItemStack st = new ItemStack(item);
        st.setDamageValue(st.getMaxDamage());
        return st;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            ItemStack old = SandevistanLogic.swapBattery(sp, stack, type);
            if (!old.isEmpty()) {
                // a bateria antiga volta para a MAO (se a trocada acabou) ou para o inventario; nunca se perde
                if (stack.isEmpty()) return InteractionResultHolder.sidedSuccess(old, false);
                sp.getInventory().placeItemBackInInventory(old);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x33CCFF;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Tx.t(ChatFormatting.AQUA, "battery.energy", energyOf(stack), stack.getMaxDamage()));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "battery.rate", SandevistanData.BAT_RATE[type]));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "battery.use"));
    }

    /** Armazenamento FE da bateria (capacidade de item), limitado pela velocidade de carga do tipo. */
    public static final class Storage implements IEnergyStorage {
        private final ItemStack stack;
        private final int rate;

        public Storage(ItemStack stack) {
            this.stack = stack;
            this.rate = stack.getItem() instanceof BatteryItem b ? SandevistanData.BAT_RATE[b.type] : 100;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int room = stack.getMaxDamage() - energyOf(stack);
            int n = Math.max(0, Math.min(Math.min(toReceive, rate), room));
            if (n > 0 && !simulate) stack.setDamageValue(stack.getDamageValue() - n);
            return n;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int n = Math.max(0, Math.min(toExtract, energyOf(stack)));
            if (n > 0 && !simulate) stack.setDamageValue(stack.getDamageValue() + n);
            return n;
        }

        @Override
        public int getEnergyStored() {
            return energyOf(stack);
        }

        @Override
        public int getMaxEnergyStored() {
            return stack.getMaxDamage();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
