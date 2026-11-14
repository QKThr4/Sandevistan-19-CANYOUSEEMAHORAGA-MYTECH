package com.sandevistanmod.tool;

import com.sandevistanmod.SandevistanMod;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Modulo de velocidade: +40% de velocidade de mineracao por modulo, em qualquer bloco. */
@EventBusSubscriber(modid = SandevistanMod.MODID)
public final class MultiToolEvents {
    private MultiToolEvents() {}

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed e) {
        ItemStack st = e.getEntity().getMainHandItem();
        if (!(st.getItem() instanceof MultiToolItem)) return;
        int n = MultiToolItem.power(st, MultiToolItem.SPEED);
        if (n > 0) e.setNewSpeed(e.getNewSpeed() * (1f + 0.4f * n));
    }
}
