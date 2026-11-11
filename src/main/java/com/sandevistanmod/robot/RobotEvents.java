package com.sandevistanmod.robot;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = SandevistanMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class RobotEvents {
    private RobotEvents() {}

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(ModRegistries.ROBO_ZUMBI.get(), RoboZombie.attrs().build());
        e.put(ModRegistries.ROBO_CREEPER.get(), RoboCreeper.attrs().build());
        e.put(ModRegistries.ROBO_ESQUELETO.get(), RoboSkeleton.attrs().build());
        e.put(ModRegistries.ROBO_CAO.get(), RoboDog.attrs().build());
        e.put(ModRegistries.ROBO_SEGURANCA.get(), RoboSecurity.attrs().build());
        e.put(ModRegistries.ROBO_DRONE.get(), RoboDrone.attrs().build());
    }

    @SubscribeEvent
    public static void placements(RegisterSpawnPlacementsEvent e) {
        e.register(ModRegistries.ROBO_ZUMBI.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        e.register(ModRegistries.ROBO_CREEPER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        e.register(ModRegistries.ROBO_ESQUELETO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        e.register(ModRegistries.ROBO_CAO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        e.register(ModRegistries.ROBO_SEGURANCA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        e.register(ModRegistries.ROBO_DRONE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
    }
}
