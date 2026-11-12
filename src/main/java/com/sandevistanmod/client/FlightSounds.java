package com.sandevistanmod.client;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.registry.ModRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Som do jetpack e das botas: ao comecar a voar toca o "ligando" (cerca de 1 s) e em seguida o som constante,
 * que repete sem parar enquanto o jogador voar. As botas usam o mesmo som, mais agudo e mais baixo.
 */
@EventBusSubscriber(modid = SandevistanMod.MODID, value = Dist.CLIENT)
public final class FlightSounds {
    private FlightSounds() {}

    private static final int START_TICKS = 20;

    private static final class Entry {
        long startTick;
        SoundInstance start;
        SoundInstance loop;
    }

    private static final Map<UUID, Entry> MAP = new HashMap<>();

    /** Som constante em repeticao preso ao jogador. */
    private static final class Loop extends AbstractTickableSoundInstance {
        private final AbstractClientPlayer player;

        Loop(AbstractClientPlayer player, boolean jet) {
            super(ModRegistries.SND_JET_LOOP.get(), SoundSource.PLAYERS, RandomSource.create());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = jet ? 0.7f : 0.5f;
            this.pitch = jet ? 1.0f : 1.25f;
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }

        @Override
        public void tick() {
            if (player.isRemoved() || !ClientState.bit(player.getUUID(), 28)) {
                stop();
                return;
            }
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            MAP.clear();
            return;
        }
        try {
            long now = mc.level.getGameTime();
            for (AbstractClientPlayer pl : mc.level.players()) {
                UUID id = pl.getUUID();
                boolean fly = ClientState.bit(id, 28);
                Entry en = MAP.get(id);
                boolean jet = ClientState.bit(id, 20);
                if (fly && en == null) {
                    en = new Entry();
                    en.startTick = now;
                    en.start = new EntityBoundSoundInstance(ModRegistries.SND_JET_START.get(), SoundSource.PLAYERS,
                            jet ? 0.8f : 0.6f, jet ? 1.0f : 1.25f, pl, pl.getRandom().nextLong());
                    mc.getSoundManager().play(en.start);
                    MAP.put(id, en);
                } else if (fly && en.loop == null && now - en.startTick >= START_TICKS) {
                    en.loop = new Loop(pl, jet);
                    mc.getSoundManager().play(en.loop);
                } else if (!fly && en != null) {
                    if (en.start != null) mc.getSoundManager().stop(en.start);
                    if (en.loop != null) mc.getSoundManager().stop(en.loop);
                    MAP.remove(id);
                }
            }
        } catch (RuntimeException ignored) {
            // som e so enfeite
        }
    }

    public static void clear() {
        MAP.clear();
    }
}
