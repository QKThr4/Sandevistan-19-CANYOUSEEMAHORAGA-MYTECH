package com.sandevistanmod.tool;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Regras das ferramentas: obsidiana do Multiuso, bonus de arco/besta, velocidade do arco e Aspectos (Congelante, Venenoso, Wither, Trovejante). */
@EventBusSubscriber(modid = SandevistanMod.MODID)
public final class ToolEvents {
    private ToolEvents() {}

    /** Multiuso quebra obsidiana, mas mais devagar que uma picareta de diamante (~50%). */
    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed e) {
        if (e.getEntity().getMainHandItem().getItem() instanceof MultiToolItem && e.getState().is(Tags.Blocks.OBSIDIANS)) {
            e.setNewSpeed(e.getNewSpeed() * (4.0f / 9.0f));
        }
    }

    private static boolean is(ItemStack st, java.util.List<net.neoforged.neoforge.registries.DeferredItem<Item>> l, int idx) {
        return st.is(l.get(idx).get());
    }

    /** Bigorna: Modulo de Encantamento + livro encantado = modulo que carrega o primeiro encantamento do livro (serve com encantamentos de outros mods). */
    @SubscribeEvent
    public static void anvil(net.neoforged.neoforge.event.AnvilUpdateEvent e) {
        ItemStack left = e.getLeft();
        ItemStack right = e.getRight();
        if (!left.is(ModRegistries.MOD_ENCANTO.get()) || !right.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) return;
        net.minecraft.world.item.enchantment.ItemEnchantments en = right.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for (net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> h : en.keySet()) {
            var key = h.unwrapKey();
            if (key.isEmpty()) continue;
            ItemStack out = ToolMods.modItem(new ToolMods.Mod(ToolMods.ENCH_CODE, key.get().location().toString(), en.getLevel(h)));
            if (out.isEmpty()) continue;
            e.setOutput(out);
            e.setCost(Math.max(1, en.getLevel(h)));
            e.setMaterialCost(1);
            return;
        }
    }

    /** Tridente de Aco/Ciber arremessado: avisa os clientes para usarem a textura certa. */
    @SubscribeEvent
    public static void tridentJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.loadedFromDisk() || !(e.getEntity() instanceof net.minecraft.world.entity.projectile.ThrownTrident t)) return;
        if (!(t.getOwner() instanceof LivingEntity o)) return;
        ItemStack st = o.getUseItem();
        int kind = st.is(ModRegistries.TRIDENTE_ACO.get()) ? 1 : st.is(ModRegistries.TRIDENTE_CIBER.get()) ? 2 : 0;
        if (kind > 0) net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(new com.sandevistanmod.net.TridentSkinPayload(t.getUUID(), kind));
    }

    /** Flechas de arcos/bestas de Aco (+10% de dano) e Ciber (+20%); modulo de Dano na besta (+10% por ponto). */
    @SubscribeEvent
    public static void arrowJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.loadedFromDisk() || !(e.getEntity() instanceof AbstractArrow a)) return;
        ItemStack w = a.getWeaponItem();
        if (w == null || w.isEmpty()) return;
        double mult = 1.0;
        if (is(w, ModRegistries.FERRAMENTAS_ACO, 5) || is(w, ModRegistries.FERRAMENTAS_ACO, 6)) mult = 1.10;
        else if (is(w, ModRegistries.FERRAMENTAS_CIBER, 5) || is(w, ModRegistries.FERRAMENTAS_CIBER, 6)) mult = 1.20;
        if (w.getItem() instanceof net.minecraft.world.item.CrossbowItem) mult *= 1.0 + 0.1 * ToolMods.power(w, ToolMods.DAMAGE);
        if (mult != 1.0) a.setBaseDamage(a.getBaseDamage() * mult);
    }

    /** Modulo de Velocidade no arco: carrega mais rapido. */
    @SubscribeEvent
    public static void bowTick(LivingEntityUseItemEvent.Tick e) {
        ItemStack st = e.getItem();
        if (!(st.getItem() instanceof net.minecraft.world.item.BowItem) || !ToolMods.isModTool(st)) return;
        int p = ToolMods.power(st, ToolMods.SPEED);
        if (p > 0 && e.getEntity().getRandom().nextFloat() < Math.min(0.9f, 0.15f * p)) e.setDuration(e.getDuration() - 1);
    }

    @SubscribeEvent
    public static void aspects(LivingDamageEvent.Post e) {
        DamageSource src = e.getSource();
        ItemStack w = src.getWeaponItem();
        if ((w == null || w.isEmpty()) && src.getEntity() instanceof LivingEntity le && src.getDirectEntity() == le) w = le.getMainHandItem();
        if (w == null || w.isEmpty() || !ToolMods.isModTool(w)) return;
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide || !t.isAlive()) return;
        int fr = ToolMods.maxTier(w, ToolMods.FREEZE);
        if (fr > 0 && !t.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)) {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (2 + fr) * 20, 1));
            t.setTicksFrozen(t.getTicksRequiredToFreeze() + (2 + fr) * 40);
            if (t.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SNOWFLAKE, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        }
        int vn = ToolMods.maxTier(w, ToolMods.VENOM);
        if (vn > 0 && !t.getType().is(EntityTypeTags.UNDEAD)) t.addEffect(new MobEffectInstance(MobEffects.POISON, 5 * 20, vn - 1));
        int wi = ToolMods.maxTier(w, ToolMods.WITHER);
        if (wi > 0) {
            MobEffectInstance eff = new MobEffectInstance(MobEffects.WITHER, 5 * 20, wi - 1);
            if (t.canBeAffected(eff)) t.addEffect(eff);
        }
        int th = ToolMods.maxTier(w, ToolMods.THUNDER);
        if (th > 0 && src.getEntity() instanceof LivingEntity at && t.level() instanceof ServerLevel sl) {
            long now = sl.getGameTime();
            if (now >= at.getPersistentData().getLong("sdv_thunder") && at.getRandom().nextFloat() < new float[] {0.05f, 0.08f, 0.12f}[th - 1]) {
                at.getPersistentData().putLong("sdv_thunder", now + 100);
                // raio seguro: so visual + dano, sem fogo nem transformacoes
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                if (bolt != null) {
                    bolt.moveTo(t.getX(), t.getY(), t.getZ());
                    bolt.setVisualOnly(true);
                    sl.addFreshEntity(bolt);
                }
                t.invulnerableTime = 0;
                t.hurt(sl.damageSources().lightningBolt(), 5.0f);
            }
        }
    }
}
