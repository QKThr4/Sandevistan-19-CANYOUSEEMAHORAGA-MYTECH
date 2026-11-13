package com.sandevistanmod.energy;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Liga as baterias e os blocos de energia ao sistema de FE do NeoForge (outros mods carregam e puxam energia deles). */
@EventBusSubscriber(modid = SandevistanMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class EnergyEvents {
    private EnergyEvents() {}

    @SubscribeEvent
    public static void caps(RegisterCapabilitiesEvent event) {
        Item[] items = ModRegistries.BATERIAS.stream().map(b -> (Item) b.get()).toArray(Item[]::new);
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> new BatteryItem.Storage(stack), items);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModRegistries.ENERGY_BE.get(), (be, side) -> be.storage());
    }
}
