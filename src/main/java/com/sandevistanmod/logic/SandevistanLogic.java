package com.sandevistanmod.logic;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.SandevistanVariant;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.net.Net;
import com.sandevistanmod.net.StatePayload;
import com.sandevistanmod.registry.ModRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Toda a logica do Sandevistan no servidor: implante, Mente, tick rate, atributos e efeitos. */
public final class SandevistanLogic {
    private SandevistanLogic() {}

    /** Dano instantaneo ao implantar (6 = 3 coracoes) e vida minima exigida para nao morrer. */
    public static final float IMPLANT_DAMAGE = 6f;
    public static final int IMPLANT_HEARTS = 3;
    /** Tempo, em segundos reais, para confirmar o implante com o segundo clique. */
    public static final long CONFIRM_MS = 10_000L;

    // ---- potencia do Sandevistan: cada modelo (SandevistanVariant) calcula a sua a partir do tick rate ----
    private record Boost(Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {}

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, path);
    }

    private static final Boost[][] BOOSTS = new Boost[SandevistanVariant.values().length][];

    private static Boost[] boosts(SandevistanVariant v) {
        Boost[] b = BOOSTS[v.ordinal()];
        if (b == null) {
            b = new Boost[] {
                    new Boost(Attributes.JUMP_STRENGTH, rl("jump"), v.jumpAdd(), AttributeModifier.Operation.ADD_VALUE),
                    new Boost(Attributes.GRAVITY, rl("gravity"), v.gravityAdd(), AttributeModifier.Operation.ADD_VALUE),
                    new Boost(Attributes.ATTACK_SPEED, rl("attack_speed"), v.attackSpeedAdd(),
                            AttributeModifier.Operation.ADD_VALUE),
                    new Boost(Attributes.BLOCK_BREAK_SPEED, rl("break_speed"), v.speedMult() - 1.0,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                    // rapido tambem na agua, debaixo dagua, agachado e em blocos que atrasam (alma de areia, mel)
                    new Boost(Attributes.WATER_MOVEMENT_EFFICIENCY, rl("water_move"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                    new Boost(Attributes.MOVEMENT_EFFICIENCY, rl("move_eff"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                    new Boost(Attributes.SUBMERGED_MINING_SPEED, rl("sub_mine"), 0.8, AttributeModifier.Operation.ADD_VALUE),
                    new Boost(Attributes.SNEAKING_SPEED, rl("sneak"), 0.7, AttributeModifier.Operation.ADD_VALUE),
                    // sem dano de queda enquanto esta ligado
                    new Boost(Attributes.FALL_DAMAGE_MULTIPLIER, rl("no_fall"), -1.0,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
            };
            BOOSTS[v.ordinal()] = b;
        }
        return b;
    }

    private static final Map<UUID, Long> ARMED = new HashMap<>();
    private static final Map<UUID, Long> LAST_TOGGLE = new HashMap<>();
    private static final Map<UUID, Integer> LAST_CODE = new HashMap<>();
    private static final Map<UUID, Float> LAST_SENT = new HashMap<>();
    /** Tick rate aplicado por nos (20 = normal). */
    private static float appliedRate = 20f;

    // ---------- utilidades ----------
    static void chat(ServerPlayer p, Component c) {
        p.sendSystemMessage(c);
        // avisos em vermelho no chat tocam o som de alerta (so para o proprio jogador)
        net.minecraft.network.chat.TextColor col = c.getStyle().getColor();
        if (col != null && (col.equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.RED))
                || col.equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.DARK_RED)))) {
            playOnly(p, ModRegistries.ALERT.get(), 0.7f, 1.0f);
        }
    }

    /** Toca um som so para este jogador, junto dele. */
    private static void playOnly(ServerPlayer p, SoundEvent ev, float volume, float pitch) {
        // som preso ao jogador (segue o jogador enquanto ele anda), enviado so para ele
        p.connection.send(new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ev),
                SoundSource.PLAYERS, p, volume, pitch, p.getRandom().nextLong()));
    }

    static void bar(ServerPlayer p, Component c) {
        p.displayClientMessage(c, true);
    }

    static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    static void send(ServerPlayer p, SandevistanData d) {
        LAST_SENT.put(p.getUUID(), d.mind);
        Net.sync(p);
    }

    // ---------- implante ----------
    /** Esmeraldas que o Ripperdoc cobra para implantar o Sandevistan. */
    public static final int CLINIC_EMERALDS = 4;

    static int countEmeralds(ServerPlayer p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(net.minecraft.world.item.Items.EMERALD)) n += s.getCount();
        for (ItemStack s : p.getInventory().offhand) if (s.is(net.minecraft.world.item.Items.EMERALD)) n += s.getCount();
        return n;
    }

    static void takeEmeralds(ServerPlayer p, int amount) {
        int left = amount;
        for (ItemStack s : p.getInventory().items) {
            if (left <= 0) break;
            if (s.is(net.minecraft.world.item.Items.EMERALD)) {
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
        for (ItemStack s : p.getInventory().offhand) {
            if (left <= 0) break;
            if (s.is(net.minecraft.world.item.Items.EMERALD)) {
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
    }

    /** Item do implante de cada encaixe. */
    public static net.minecraft.world.item.Item implantItem(int slot) {
        return implantItem(slot, 0);
    }

    /** Item do implante do encaixe no tier dado (1 a 3); nas costas e no Link Neural nao ha tiers. */
    public static net.minecraft.world.item.Item implantItem(int slot, int variant, int tier) {
        net.minecraft.world.item.Item t = ModRegistries.tierImplant(slot, tier);
        return t != null ? t : implantItem(slot, variant);
    }

    /** Tier de um item de implante (1 se nao for um implante com tiers). */
    public static int tierOfStack(ItemStack st) {
        return st.getItem() instanceof ImplantItem ii ? ii.tier() : st.getItem() instanceof ChipItem ci ? ci.tier() : 1;
    }

    /** Item do implante do encaixe (tier I); nas costas depende do modelo do Sandevistan (0 Apogee ... 4 QianT). */
    public static net.minecraft.world.item.Item implantItem(int slot, int variant) {
        return switch (slot) {
            case SandevistanData.SLOT_BACK -> ModRegistries.SANDEVISTANS.get(Math.max(0, Math.min(4, variant))).get();
            case SandevistanData.SLOT_ARM_R, SandevistanData.SLOT_ARM_L -> ModRegistries.BRACO_FERRO.get();
            case SandevistanData.SLOT_BONE -> ModRegistries.ESTRUTURA_OSSEA.get();
            case SandevistanData.SLOT_NERVE -> ModRegistries.SISTEMA_NERVOSO.get();
            case SandevistanData.SLOT_HEART -> ModRegistries.CORACAO_FERRO.get();
            case SandevistanData.SLOT_LEG_R, SandevistanData.SLOT_LEG_L -> ModRegistries.PERNAS.get();
            default -> ModRegistries.LINK_NEURAL.get();
        };
    }

    /** Modelo do Sandevistan de um item (0 a 4) ou -1 se nao for um Sandevistan. */
    public static int variantOf(ItemStack st) {
        return st.getItem() instanceof SandevistanItem si ? si.variant() : -1;
    }

    static boolean hasItem(ServerPlayer p, net.minecraft.world.item.Item item) {
        for (ItemStack s : p.getInventory().items) if (s.is(item)) return true;
        for (ItemStack s : p.getInventory().offhand) if (s.is(item)) return true;
        return false;
    }

    static void takeItem(ServerPlayer p, net.minecraft.world.item.Item item) {
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) {
                s.shrink(1);
                return;
            }
        }
        for (ItemStack s : p.getInventory().offhand) {
            if (s.is(item)) {
                s.shrink(1);
                return;
            }
        }
    }

    /** Ha um Ripperdoc (aldeao doutor) perto do jogador? */
    public static boolean nearDoctor(ServerPlayer p) {
        return !p.level().getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,
                p.getBoundingBox().inflate(8.0),
                v -> v.isAlive() && v.getVillagerData().getProfession() == ModRegistries.RIPPERDOC.get()).isEmpty();
    }

    /** Ha o bloco da Clinica de Ripperdoc perto do jogador? */
    public static boolean nearClinicBlock(ServerPlayer p) {
        net.minecraft.core.BlockPos c = p.blockPosition();
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(c.offset(-6, -3, -6), c.offset(6, 3, 6))) {
            if (p.level().getBlockState(pos).is(ModRegistries.CLINICA.get())) return true;
        }
        return false;
    }

    /** Acoes vindas da tela do Ripperdoc: 1 implantar, 2 remover, 3 comprar Estabilizador, 4 comprar Imunossupressor. */
    public static final int STABILIZER_PRICE = 10;
    public static final int IMMUNO_PRICE = 8;

    /** Acoes: 1/2 implantar/remover o Sandevistan, 3/4 loja, 10+encaixe implantar, 20+encaixe remover. */
    public static void doctorAction(ServerPlayer p, int action) {
        if (!nearDoctor(p) && !nearClinicBlock(p)) {
            bar(p, Tx.t(ChatFormatting.RED, "doc.far"));
            return;
        }
        if (action < 0) {
            ExtLogic.doctor(p, action);
            return;
        }
        switch (action) {
            case 1 -> implantAt(p, SandevistanData.SLOT_BACK, 0);
            case 2 -> removeAt(p, SandevistanData.SLOT_BACK);
            case 3 -> buy(p, ModRegistries.ESTABILIZADOR.get(), STABILIZER_PRICE);
            case 4 -> buy(p, ModRegistries.IMUNOSSUPRESSOR.get(), IMMUNO_PRICE);
            default -> {
                if (action >= 100) {
                    // varios implantes de uma vez: (acao - 100) = tiers (2 bits por encaixe) << 12 | mascara de encaixes << 3 | modelo
                    int v = action - 100;
                    implantMany(p, (v >> 3) & 0x1FF, v & 7, v >> 12);
                } else if (action >= 30 && action < 30 + SandevistanVariant.values().length) {
                    implantAt(p, SandevistanData.SLOT_BACK, action - 30);
                } else if (action >= 10 && action < 10 + SandevistanData.SLOT_COUNT) implantAt(p, action - 10, 0);
                else if (action >= 20 && action < 20 + SandevistanData.SLOT_COUNT) removeAt(p, action - 20);
            }
        }
    }

    private static void buy(ServerPlayer p, net.minecraft.world.item.Item item, int price) {
        boolean creative = p.getAbilities().instabuild;
        if (!creative && countEmeralds(p) < price) {
            bar(p, Tx.t(ChatFormatting.RED, "shop.noemerald", price));
            return;
        }
        if (!creative) takeEmeralds(p, price);
        give(p, new ItemStack(item));
        bar(p, Tx.t(ChatFormatting.GREEN, "shop.bought"));
    }

    /** O Ripperdoc implanta: consome o item e 8 esmeraldas e causa 3 coracoes de dano. */
    public static boolean implantAt(ServerPlayer p, int slot, int variant) {
        return implantAt(p, slot, variant, 1);
    }

    public static boolean implantAt(ServerPlayer p, int slot, int variant, int tier) {
        SandevistanData d = SandevistanData.get(p);
        if (slot < 0 || slot >= SandevistanData.SLOT_COUNT) return false;
        if (d.has(slot)) {
            bar(p, Tx.t(ChatFormatting.RED, "already"));
            return false;
        }
        boolean creative = p.getAbilities().instabuild;
        net.minecraft.world.item.Item item = implantItem(slot, variant, tier);
        if (!creative && !hasItem(p, item)) {
            bar(p, Tx.t(ChatFormatting.RED, "clinic.noitem"));
            return false;
        }
        if (!creative && p.getHealth() <= IMPLANT_DAMAGE + 1f) {
            bar(p, Tx.t(ChatFormatting.RED, "lowhp", IMPLANT_HEARTS));
            return false;
        }
        if (!creative && countEmeralds(p) < CLINIC_EMERALDS) {
            bar(p, Tx.t(ChatFormatting.RED, "clinic.noemerald", CLINIC_EMERALDS));
            return false;
        }
        if (!creative) {
            takeEmeralds(p, CLINIC_EMERALDS);
            takeItem(p, item);
        }
        p.hurt(p.damageSources().magic(), IMPLANT_DAMAGE);
        d.set(slot, true, tier);
        if (slot == SandevistanData.SLOT_BACK) {
            d.active = false;
            d.introSeen = false;
            d.variant = Math.max(0, Math.min(4, variant));
            d.mind = d.maxMind();
        }
        send(p, d);
        broadcastState(p, d);
        bar(p, Tx.t(ChatFormatting.GREEN, "implanted.short"));
        RipperdocTalk.afterImplant(p, d);
        return true;
    }

    /**
     * Varios implantes numa cirurgia so: a mascara tem um bit por encaixe. Cada implante gasta o seu item e 8 esmeraldas,
     * e a cirurgia inteira causa o dano uma vez. Se faltar item, esmeralda ou vida, nada e implantado.
     */
    public static boolean implantMany(ServerPlayer p, int mask, int variant) {
        return implantMany(p, mask, variant, 0);
    }

    /** tiers: 2 bits por encaixe (0 = tier I, 1 = tier II, 2 = tier III). */
    public static boolean implantMany(ServerPlayer p, int mask, int variant, int tiers) {
        SandevistanData d = SandevistanData.get(p);
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) {
            if ((mask & (1 << s)) != 0 && !d.has(s)) slots.add(s);
        }
        if (slots.isEmpty()) {
            bar(p, Tx.t(ChatFormatting.RED, "already"));
            return false;
        }
        boolean creative = p.getAbilities().instabuild;
        if (!creative) {
            // quantos itens de cada tipo a cirurgia consome (dois bracos ou duas pernas = dois itens)
            java.util.Map<net.minecraft.world.item.Item, Integer> need = new java.util.HashMap<>();
            for (int s : slots) need.merge(implantItem(s, variant, tierIn(tiers, s)), 1, Integer::sum);
            for (java.util.Map.Entry<net.minecraft.world.item.Item, Integer> en : need.entrySet()) {
                int have = 0;
                for (ItemStack st : p.getInventory().items) if (st.is(en.getKey())) have += st.getCount();
                for (ItemStack st : p.getInventory().offhand) if (st.is(en.getKey())) have += st.getCount();
                if (have < en.getValue()) {
                    bar(p, Tx.t(ChatFormatting.RED, "clinic.noitem"));
                    return false;
                }
            }
            if (p.getHealth() <= IMPLANT_DAMAGE + 1f) {
                bar(p, Tx.t(ChatFormatting.RED, "lowhp", IMPLANT_HEARTS));
                return false;
            }
            int price = CLINIC_EMERALDS * slots.size();
            if (countEmeralds(p) < price) {
                bar(p, Tx.t(ChatFormatting.RED, "clinic.noemerald", price));
                return false;
            }
            takeEmeralds(p, price);
            for (int s : slots) takeItem(p, implantItem(s, variant, tierIn(tiers, s)));
        }
        p.hurt(p.damageSources().magic(), IMPLANT_DAMAGE);
        for (int s : slots) {
            d.set(s, true, tierIn(tiers, s));
            if (s == SandevistanData.SLOT_BACK) {
                d.active = false;
                d.introSeen = false;
                d.variant = Math.max(0, Math.min(4, variant));
                d.mind = d.maxMind();
            }
        }
        send(p, d);
        broadcastState(p, d);
        bar(p, Tx.t(ChatFormatting.GREEN, "implanted.short"));
        RipperdocTalk.afterImplant(p, d);
        return true;
    }

    /** Tier (1 a 3) do encaixe dentro dos tiers empacotados (2 bits por encaixe). */
    public static int tierIn(int packed, int slot) {
        return SandevistanData.slotHasTiers(slot) ? SandevistanData.clampTier(((packed >> (2 * slot)) & 3) + 1) : 1;
    }

    /** Remover um implante so e possivel com o Ripperdoc. */
    public static void removeAt(ServerPlayer p, int slot) {
        SandevistanData d = SandevistanData.get(p);
        if (slot < 0 || slot >= SandevistanData.SLOT_COUNT || !d.has(slot)) {
            bar(p, Tx.t(ChatFormatting.RED, "noimplant"));
            return;
        }
        if (slot == SandevistanData.SLOT_BACK && d.active) {
            bar(p, Tx.t(ChatFormatting.RED, "remove.active"));
            return;
        }
        if (slot == SandevistanData.SLOT_NEURAL && d.chips() > 0) {
            bar(p, Tx.t(ChatFormatting.RED, "remove.chips"));
            return;
        }
        int oldTier = d.tierOf(slot);
        d.set(slot, false);
        if (slot == SandevistanData.SLOT_BACK) d.introSeen = false;
        give(p, new ItemStack(implantItem(slot, d.variant, oldTier)));
        send(p, d);
        broadcastState(p, d);
        bar(p, Tx.t(ChatFormatting.YELLOW, "removed"));
        RipperdocTalk.afterRemove(p);
    }

    /** Ciberpsicose a partir da qual o jogador nao controla mais o implante. */
    public static final float LOSS_OF_CONTROL = 100f;
    /** Dano de usar sem Mente: 2 coracoes a cada 8 segundos (o tempo acumulado se mantem entre usos). */
    private static final float ZERO_DMG_PERIOD = 8f;
    private static final float ZERO_DMG_AMOUNT = 4f;
    /** Ticks extras por tick ao usar itens e nas recargas do jogador (depende do modelo ligado). */
    public static int extraTicks(ServerPlayer p) {
        return SandevistanData.get(p).variant().extraTicks();
    }

    /** Resistencia ao knockback pela ciberpsicose: 0 ate 30%, sobe ate 1 (nenhum) em 100%. */
    public static float psyResist(SandevistanData d) {
        if (!d.psychotic) return 0f;
        return Math.max(0f, Math.min(1f, (d.psy - 30f) / 70f));
    }

    /** Acima de 100% de ciberpsicose o jogador nao usa itens, nao dropa nem abre inventarios. */
    public static boolean locked(SandevistanData d) {
        return d.psychotic && d.psy >= LOSS_OF_CONTROL;
    }

    private static final Map<UUID, Long> DODGE_CLEAR = new HashMap<>();

    /** Mostra "Desviou!" e apaga logo depois (a mensagem da barra de acao dura mais do que queremos). */
    public static void dodged(ServerPlayer p) {
        bar(p, Tx.t(ChatFormatting.RED, "dodge"));
        DODGE_CLEAR.put(p.getUUID(), System.currentTimeMillis() + 600L);
    }

    /** Mente gasta ao ligar o Sandevistan. */
    private static final float ACTIVATE_COST = 5f;
    /** Recarga (s) dos 3 coracoes de dano que vem quando a Mente chega a 0. */
    private static final float ZERO_HIT_COOLDOWN = 1.5f;

    // ---------- ligar / desligar ----------
    public static void handleAction(ServerPlayer p, int action) {
        SandevistanData d = SandevistanData.get(p);
        switch (action) {
            case 0 -> toggle(p, d);
            case 1 -> chat(p, Tx.t(ChatFormatting.YELLOW, "remove.doctor"));
            case 2, 3 -> removeChip(p, d, action - 2);
            case 4 -> {
                d.armsVisible = !d.armsVisible;
                broadcastState(p, d);
                send(p, d);
            }
            case 5 -> {
                d.legsVisible = !d.legsVisible;
                broadcastState(p, d);
                send(p, d);
            }
            case 6 -> removeBattery(p, d);
            case 20 -> ExtLogic.xray(p, d);
            case 21 -> ExtLogic.eye(p, d);
            case 22 -> ExtLogic.toggleInjector(p, d);
            case 23 -> ExtLogic.toggleMantis(p, d);
            default -> {
                // 30 a 65: instala a bateria que esta nesse espaco do inventario
                if (action >= 30 && action < 30 + 36) {
                    ItemStack st = p.getInventory().items.get(action - 30);
                    if (st.getItem() instanceof com.sandevistanmod.energy.BatteryItem bi) insertBattery(p, st, bi.type());
                }
            }
        }
    }

    // ---------- energia (bateria implantada) ----------
    /** Insere a bateria (botao direito no item). Se ja houver uma, ela volta para o inventario. */
    public static boolean insertBattery(ServerPlayer p, ItemStack stack, int type) {
        SandevistanData d = SandevistanData.get(p);
        ItemStack old = d.batType != 0 ? batteryStack(d) : ItemStack.EMPTY;
        d.batType = type;
        d.batEnergy = Math.max(0, Math.min(SandevistanData.BAT_CAP[type], stack.getMaxDamage() - stack.getDamageValue()));
        d.batFrac = 0f;
        stack.shrink(1);
        if (!old.isEmpty()) p.getInventory().placeItemBackInInventory(old);
        chat(p, Tx.t(ChatFormatting.AQUA, "battery.inserted"));
        send(p, d);
        return true;
    }

    public static void removeBattery(ServerPlayer p, SandevistanData d) {
        if (d.batType == 0) return;
        ItemStack st = batteryStack(d);
        d.batType = 0;
        d.batEnergy = 0;
        d.batFrac = 0f;
        p.getInventory().placeItemBackInInventory(st);
        send(p, d);
    }

    /** O item da bateria implantada, com a energia restante como "durabilidade" restante. */
    public static ItemStack batteryStack(SandevistanData d) {
        if (d.batType == 0) return ItemStack.EMPTY;
        ItemStack st = new ItemStack(ModRegistries.battery(d.batType));
        st.setDamageValue(Math.max(0, st.getMaxDamage() - d.batEnergy));
        return st;
    }

    /** Consumo extra por implante de energia: cada um aumenta o gasto dos outros em 10%. */
    public static float energyMult(int users) {
        return 1f + 0.1f * Math.max(0, users - 1);
    }

    /** Recarrega a bateria implantada (fe pode ser fracionado). */
    public static void addEnergy(SandevistanData d, float fe) {
        if (d.batType == 0 || fe <= 0f) return;
        d.solarFrac += fe;
        int whole = (int) d.solarFrac;
        d.solarFrac -= whole;
        d.batEnergy = Math.min(d.batCap(), d.batEnergy + whole);
    }

    /** Gasta energia da bateria (fe pode ser fracionado). Devolve false se nao ha energia. */
    public static boolean drainEnergy(SandevistanData d, float fe) {
        if (!d.hasEnergy()) return false;
        d.batFrac += fe;
        int whole = (int) d.batFrac;
        d.batFrac -= whole;
        d.batEnergy = Math.max(0, d.batEnergy - whole);
        return true;
    }

    // ---------- chips do Link Neural ----------
    private static final ResourceLocation ARM_DMG_R = rl("iron_arm_damage_r");
    private static final ResourceLocation ARM_DMG_L = rl("iron_arm_damage_l");
    private static final ResourceLocation BONE_ARMOR = rl("bone_armor");
    private static final ResourceLocation BONE_KB = rl("bone_knockback");
    private static final ResourceLocation BONE_FALL = rl("bone_fall");
    private static final ResourceLocation HEART_HP = rl("heart_health");
    private static final ResourceLocation LEG_SPEED_R = rl("leg_speed_r");
    private static final ResourceLocation LEG_SPEED_L = rl("leg_speed_l");
    private static final ResourceLocation LEG_JUMP_R = rl("leg_jump_r");
    private static final ResourceLocation LEG_JUMP_L = rl("leg_jump_l");
    private static final ResourceLocation LEG_FALL_R = rl("leg_fall_r");
    private static final ResourceLocation LEG_FALL_L = rl("leg_fall_l");
    private static final ResourceLocation NERVE_SPEED = rl("nerve_speed");
    private static final ResourceLocation NERVE_ATTACK = rl("nerve_attack");
    private static final ResourceLocation MARTIAL_DMG = rl("martial_damage");
    private static final ResourceLocation MARTIAL_SPEED = rl("martial_speed");
    private static final ResourceLocation MINING_SPEED = rl("mining_chip");

    /** Insere um chip (botao direito no item): precisa do Link Neural, sem chips repetidos, 2 no maximo. */
    public static boolean insertChip(ServerPlayer p, ItemStack stack, int type, int tier) {
        SandevistanData d = SandevistanData.get(p);
        if (locked(d)) {
            bar(p, Tx.t(ChatFormatting.DARK_RED, "psy.nocontrol"));
            return false;
        }
        if (!d.neural) {
            bar(p, Tx.t(ChatFormatting.RED, "chip.noneural"));
            return false;
        }
        if (d.chipSlot(type) >= 0) {
            bar(p, Tx.t(ChatFormatting.RED, "chip.dup"));
            return false;
        }
        int free = d.chip[0] == 0 ? 0 : d.chip[1] == 0 ? 1 : -1;
        if (free < 0) {
            bar(p, Tx.t(ChatFormatting.RED, "chip.full"));
            return false;
        }
        if (stack.getDamageValue() >= SandevistanData.chipMax(type, tier)) {
            bar(p, Tx.t(ChatFormatting.RED, "chip.needrepair"));
            return false;
        }
        d.chip[free] = type;
        d.chipTier[free] = SandevistanData.chipHasTiers(type) ? SandevistanData.clampTier(tier) : 1;
        d.chipDmg[free] = stack.getDamageValue();
        d.chipLoad[free] = SandevistanData.CHIP_LOAD_SECONDS;
        d.mind = Math.min(d.mind, d.maxMind());
        stack.shrink(1);
        bar(p, Tx.t(ChatFormatting.AQUA, "chip.inserted", (int) SandevistanData.chipMindFor(type, d.chipTier[free])));
        send(p, d);
        broadcastState(p, d);
        return true;
    }

    /** Tira o chip do encaixe (0 ou 1) e devolve o item com a durabilidade que tinha. */
    public static void removeChip(ServerPlayer p, SandevistanData d, int slot) {
        if (slot < 0 || slot > 1 || d.chip[slot] == 0) return;
        if (locked(d)) {
            bar(p, Tx.t(ChatFormatting.DARK_RED, "psy.nocontrol"));
            return;
        }
        ItemStack out = new ItemStack(ModRegistries.chipItem(d.chip[slot], d.chipTier[slot]));
        out.setDamageValue(d.chipDmg[slot]);
        d.chip[slot] = 0;
        d.chipTier[slot] = 1;
        d.chipDmg[slot] = 0;
        d.chipLoad[slot] = 0f;
        give(p, out);
        bar(p, Tx.t(ChatFormatting.YELLOW, "chip.removed"));
        send(p, d);
        broadcastState(p, d);
    }

    /** O chip ativou uma habilidade: perde 1 de durabilidade. */
    public static void useChip(ServerPlayer p, int type) {
        SandevistanData d = SandevistanData.get(p);
        int i = d.chipSlot(type);
        if (i < 0 || !d.chipActive(type)) return;
        d.chipDmg[i]++;
        if (d.chipDmg[i] >= SandevistanData.chipMax(type, d.chipTier[i])) chat(p, Tx.t(ChatFormatting.RED, "chip.broken"));
    }

    static void setMod(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount,
                               AttributeModifier.Operation op, boolean want) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        boolean has = cur != null;
        if (want && has && cur.amount() != amount) {
            inst.removeModifier(id);
            has = false;
        }
        if (want && !has) inst.addTransientModifier(new AttributeModifier(id, amount, op));
        else if (!want && has) inst.removeModifier(id);
    }

    /** Efeito discreto dos chips: so mexe nos que nos mesmos demos (ambiente, sem particulas, mesmo nivel). */
    private static void chipEffect(ServerPlayer p, Holder<net.minecraft.world.effect.MobEffect> eff, int amp, boolean want) {
        MobEffectInstance cur = p.getEffect(eff);
        if (want) {
            if (cur == null || (cur.getAmplifier() <= amp && (cur.getAmplifier() < amp || cur.getDuration() < 60))) {
                p.addEffect(new MobEffectInstance(eff, 100, amp, true, false, false));
            }
        } else if (cur != null && cur.isAmbient() && !cur.isVisible() && cur.getAmplifier() == amp
                && cur.getDuration() <= 100) {
            p.removeEffect(eff);
        }
    }

    /** Baixa dos chips, psicose da Carga Cibernetica e bonus passivos (bracos e chips). dt = segundos reais. */
    private static void cyberTick(ServerPlayer p, SandevistanData d, float dt) {
        for (int i = 0; i < 2; i++) {
            if (d.chip[i] != 0 && d.chipLoad[i] > 0f) {
                d.chipLoad[i] -= dt;
                if (d.chipLoad[i] <= 0f) {
                    d.chipLoad[i] = 0f;
                    bar(p, Tx.t(ChatFormatting.GREEN, "chip.ready"));
                }
            }
        }
        // Carga Cibernetica: a partir de 70%, a cada 5 s ha chance de ganhar ciberpsicose (cresce ate 50% de chance de +10 em 100%)
        float cap = d.capacity();
        float thr = d.loadThreshold();
        // o Chip de RAM gasta durabilidade (1 a cada 5 s) so enquanto a carga esta na faixa que ele protege
        if (cap >= SandevistanData.BASE_LOAD_THRESHOLD && d.chipActive(SandevistanData.CHIP_RAM)) {
            d.ramT += dt;
            while (d.ramT >= 5f) {
                d.ramT -= 5f;
                useChip(p, SandevistanData.CHIP_RAM);
            }
        } else {
            d.ramT = 0f;
        }
        if (cap >= thr) {
            d.loadT += dt;
            while (d.loadT >= 5f) {
                d.loadT -= 5f;
                float k = Math.min(1f, (cap - thr) / (100f - thr));
                if (p.getRandom().nextFloat() < 0.05f + 0.45f * k) gainIns(p, d, Math.max(1, Math.round(1f + 9f * k)));
            }
        } else {
            d.loadT = 0f;
        }
        boolean martial = d.chipActive(SandevistanData.CHIP_MARTIAL);
        int tb = d.tierOf(SandevistanData.SLOT_BONE);
        int tr = d.tierOf(SandevistanData.SLOT_LEG_R);
        int tl = d.tierOf(SandevistanData.SLOT_LEG_L);
        int tn = d.tierOf(SandevistanData.SLOT_NERVE);
        setMod(p, Attributes.ATTACK_DAMAGE, ARM_DMG_R, SandevistanData.at(SandevistanData.ARM_DAMAGE, d.tierOf(SandevistanData.SLOT_ARM_R)), AttributeModifier.Operation.ADD_VALUE, d.armR);
        setMod(p, Attributes.ATTACK_DAMAGE, ARM_DMG_L, SandevistanData.at(SandevistanData.ARM_DAMAGE, d.tierOf(SandevistanData.SLOT_ARM_L)), AttributeModifier.Operation.ADD_VALUE, d.armL);
        // Estrutura Ossea Reforcada: mais armadura, menos empurrao e menos dano de queda (atributos, nao efeitos de pocao)
        setMod(p, Attributes.ARMOR, BONE_ARMOR, SandevistanData.at(SandevistanData.BONE_ARMOR, tb), AttributeModifier.Operation.ADD_VALUE, d.bone);
        setMod(p, Attributes.KNOCKBACK_RESISTANCE, BONE_KB, SandevistanData.at(SandevistanData.BONE_KB, tb), AttributeModifier.Operation.ADD_VALUE, d.bone);
        setMod(p, Attributes.FALL_DAMAGE_MULTIPLIER, BONE_FALL, -SandevistanData.at(SandevistanData.BONE_FALL, tb), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, d.bone);
        // Coracao de Ferro: +5 coracoes de vida maxima (a regeneracao +10% esta no evento de cura)
        setMod(p, Attributes.MAX_HEALTH, HEART_HP, SandevistanData.at(SandevistanData.HEART_HP, d.tierOf(SandevistanData.SLOT_HEART)), AttributeModifier.Operation.ADD_VALUE, d.heart);
        // Pernas Cibernéticas (cada perna): +15% de velocidade, mais pulo e -15% de dano de queda
        setMod(p, Attributes.MOVEMENT_SPEED, LEG_SPEED_R, SandevistanData.at(SandevistanData.LEG_SPEED, tr), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, d.legR);
        setMod(p, Attributes.MOVEMENT_SPEED, LEG_SPEED_L, SandevistanData.at(SandevistanData.LEG_SPEED, tl), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, d.legL);
        setMod(p, Attributes.JUMP_STRENGTH, LEG_JUMP_R, SandevistanData.at(SandevistanData.LEG_JUMP, tr), AttributeModifier.Operation.ADD_VALUE, d.legR);
        setMod(p, Attributes.JUMP_STRENGTH, LEG_JUMP_L, SandevistanData.at(SandevistanData.LEG_JUMP, tl), AttributeModifier.Operation.ADD_VALUE, d.legL);
        setMod(p, Attributes.FALL_DAMAGE_MULTIPLIER, LEG_FALL_R, -SandevistanData.at(SandevistanData.LEG_FALL, tr), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, d.legR);
        setMod(p, Attributes.FALL_DAMAGE_MULTIPLIER, LEG_FALL_L, -SandevistanData.at(SandevistanData.LEG_FALL, tl), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, d.legL);
        // Sistema Nervoso Aprimorado: reflexos um pouco mais rapidos
        setMod(p, Attributes.MOVEMENT_SPEED, NERVE_SPEED, SandevistanData.at(SandevistanData.NERVE_SPEED, tn), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, d.nerve);
        setMod(p, Attributes.ATTACK_SPEED, NERVE_ATTACK, SandevistanData.at(SandevistanData.NERVE_ATTACK, tn), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, d.nerve);
        setMod(p, Attributes.ATTACK_DAMAGE, MARTIAL_DMG, 3.0, AttributeModifier.Operation.ADD_VALUE, martial);
        setMod(p, Attributes.MOVEMENT_SPEED, MARTIAL_SPEED, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, martial);
        setMod(p, Attributes.BLOCK_BREAK_SPEED, MINING_SPEED, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                d.chipActive(SandevistanData.CHIP_MINING));
        if (p.tickCount % 10 == 0) {
            chipEffect(p, MobEffects.DAMAGE_RESISTANCE, 0, d.chipActive(SandevistanData.CHIP_PAIN) && !d.active);
            boolean walker = d.chipActive(SandevistanData.CHIP_WALKER);
            chipEffect(p, MobEffects.JUMP, 0, walker);
            chipEffect(p, MobEffects.DOLPHINS_GRACE, 0, walker);
        }
    }

    private static void toggle(ServerPlayer p, SandevistanData d) {
        if (!d.implanted) {
            bar(p, Tx.t(ChatFormatting.RED, "needimplant"));
            return;
        }
        if (d.psy >= LOSS_OF_CONTROL) {
            // acima de 100% o jogador perde o controle: o implante liga e desliga sozinho
            bar(p, Tx.t(ChatFormatting.DARK_RED, "psy.nocontrol"));
            return;
        }
        if (d.active) {
            deactivate(p, d, true);
        } else {
            activate(p, d);
        }
    }

    private static void activate(ServerPlayer p, SandevistanData d) {
        d.active = true;
        if (!d.introSeen) {
            d.introSeen = true;
            // o video da primeira vez e so do Militech Apogee
            if (d.variant == 0) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new com.sandevistanmod.net.IntroPayload());
            }
        }
        // ligar gasta 5 de Mente (a atual, nao a permanente)
        float spent = Math.min(ACTIVATE_COST, d.mind);
        d.mind -= spent;
        d.useAcc += spent;
        while (d.useAcc >= SandevistanData.PSY_USE_STEP) {
            d.useAcc -= SandevistanData.PSY_USE_STEP;
            gainIns(p, d, SandevistanData.PSY_PER_STEP);
        }
        // ligar com a Mente em 0 tambem custa os 3 coracoes (o tick aplica ao ver a Mente zerada)
        if (d.mind <= 0f) d.zeroWarned = false;
        applyBoost(p);
        updateTickRate(p.server);
        broadcastState(p, d);
        send(p, d);
    }

    private static void deactivate(ServerPlayer p, SandevistanData d, boolean sound) {
        d.active = false;
        removeBoost(p);
        p.fallDistance = 0;
        // para o jogador no lugar: sem isso ele sairia disparado com a velocidade que tinha
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        updateTickRate(p.server);
        broadcastState(p, d);
        send(p, d);
    }

    // ---------- atributos e efeitos ----------
    private static void applyBoost(ServerPlayer p) {
        SandevistanVariant v = SandevistanData.get(p).variant();
        for (Boost b : boosts(v)) {
            AttributeInstance inst = p.getAttribute(b.attr());
            if (inst != null && !inst.hasModifier(b.id())) {
                inst.addTransientModifier(new AttributeModifier(b.id(), b.amount(), b.op()));
            }
        }
        ensure(p, MobEffects.MOVEMENT_SPEED, 100, 200, v.speedAmp(), true);
        if (v.resist >= 0) ensure(p, MobEffects.DAMAGE_RESISTANCE, 100, 200, v.resist, true);
        ensure(p, MobEffects.NIGHT_VISION, 300, 400, 0);
        if (v.strength >= 0) ensure(p, MobEffects.DAMAGE_BOOST, 100, 200, v.strength);
        if (v.haste >= 0) ensure(p, MobEffects.DIG_SPEED, 100, 200, v.haste);
    }

    private static void ensure(ServerPlayer p, Holder<net.minecraft.world.effect.MobEffect> eff, int minDur, int dur, int amp) {
        ensure(p, eff, minDur, dur, amp, false);
    }

    private static void ensure(ServerPlayer p, Holder<net.minecraft.world.effect.MobEffect> eff, int minDur, int dur,
                               int amp, boolean icon) {
        MobEffectInstance cur = p.getEffect(eff);
        if (cur == null || cur.getAmplifier() != amp || cur.getDuration() < minDur) {
            p.addEffect(new MobEffectInstance(eff, dur, amp, true, false, icon));
        }
    }

    private static void dropEffect(ServerPlayer p, Holder<net.minecraft.world.effect.MobEffect> eff) {
        MobEffectInstance cur = p.getEffect(eff);
        if (cur != null && cur.isAmbient()) p.removeEffect(eff);
    }

    private static void removeBoost(ServerPlayer p) {
        for (SandevistanVariant v : SandevistanVariant.values()) {
            for (Boost b : boosts(v)) {
                AttributeInstance inst = p.getAttribute(b.attr());
                if (inst != null) inst.removeModifier(b.id());
            }
        }
        dropEffect(p, MobEffects.NIGHT_VISION);
        // os efeitos do Sandevistan saem mesmo se outro efeito (pocao, outro mod) os fundiu e tirou a marca de "ambiente"
        dropBoostEffect(p, MobEffects.MOVEMENT_SPEED, 0);
        dropBoostEffect(p, MobEffects.DAMAGE_RESISTANCE, 1);
        dropBoostEffect(p, MobEffects.DAMAGE_BOOST, 2);
        dropBoostEffect(p, MobEffects.DIG_SPEED, 3);
    }

    /** Tira o efeito se for ambiente ou se tiver exatamente o nivel que algum modelo do Sandevistan aplica. kind: 0 velocidade, 1 resistencia, 2 forca, 3 pressa. */
    private static void dropBoostEffect(ServerPlayer p, Holder<net.minecraft.world.effect.MobEffect> eff, int kind) {
        MobEffectInstance cur = p.getEffect(eff);
        if (cur == null) return;
        boolean match = cur.isAmbient();
        for (SandevistanVariant v : SandevistanVariant.values()) {
            int amp = kind == 0 ? v.speedAmp() : kind == 1 ? v.resist : kind == 2 ? v.strength : v.haste;
            if (amp >= 0 && cur.getAmplifier() == amp) match = true;
        }
        if (match) p.removeEffect(eff);
    }

    // ---------- tick rate ----------
    /**
     * Enquanto algum jogador estiver com o Sandevistan ligado, o jogo inteiro roda a 2 ticks por segundo,
     * exatamente como "/tick rate 2". Ao desligar, volta a 20.
     */
    public static void updateTickRate(MinecraftServer server) {
        float want = 20f;
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            SandevistanData sd = SandevistanData.get(sp);
            // vale o modelo mais lento (o que mais desacelera) entre os jogadores com o Sandevistan ligado
            if (sp.isAlive() && sd.active) want = Math.min(want, sd.variant().rate());
        }
        // le a taxa REAL do servidor a cada tick: outro mod (ex.: Sharingan) pode ter mudado por cima da nossa
        float cur = server.tickRateManager().tickrate();
        if (want < 20f) {
            // so desacelera mais; se outro mod ja deixou mais lento, nao mexe
            if (cur > want + 0.01f) {
                appliedRate = want;
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput(), "tick rate " + (int) want);
            }
        } else if (appliedRate < 20f) {
            // desligou: so devolve a 20 se a taxa ainda e a que NOS aplicamos (senao e de outro mod)
            if (Math.abs(cur - appliedRate) < 0.01f) {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput(), "tick rate 20");
            }
            appliedRate = 20f;
        }
    }

    // ---------- estado visivel para todos ----------
    private static void broadcastState(ServerPlayer p, SandevistanData d) {
        int code = d.stateCode();
        if (LAST_CODE.getOrDefault(p.getUUID(), -1) != code) {
            LAST_CODE.put(p.getUUID(), code);
            PacketDistributor.sendToAllPlayers(new StatePayload(p.getUUID(), code));
        }
    }

    public static void sendStatesTo(ServerPlayer joiner) {
        for (Map.Entry<UUID, Integer> en : LAST_CODE.entrySet()) {
            if (en.getValue() > 0 && !en.getKey().equals(joiner.getUUID())) {
                PacketDistributor.sendToPlayer(joiner, new StatePayload(en.getKey(), en.getValue()));
            }
        }
    }

    // ---------- tick do jogador ----------
    public static void tick(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        broadcastState(p, d);
        Long clear = DODGE_CLEAR.get(p.getUUID());
        if (clear != null && System.currentTimeMillis() >= clear) {
            DODGE_CLEAR.remove(p.getUUID());
            bar(p, Component.empty());
        }
        if (!d.implanted && d.active) deactivate(p, d, false);
        // sem controle: nao abre inventarios (so o menu)
        if (locked(d) && p.containerMenu != p.inventoryMenu) p.closeContainer();
        // com o jogo mais lento cada tick dura mais: divide pelo tick rate para o tempo real continuar o mesmo
        // dt vem da taxa REAL do servidor (nao do cache): assim a Mente nunca gasta rapido demais
        float rate = Math.max(1f, p.server.tickRateManager().tickrate());
        if (d.zeroHitCd > 0f) d.zeroHitCd = Math.max(0f, d.zeroHitCd - 1f / rate);
        cyberTick(p, d, 1f / rate);
        LowMind.tick(p, d, 1f / rate);
        if (!d.implanted) {
            // sem o Sandevistan so ha a Mente (que os chips reduzem) e os efeitos dos outros implantes
            if (d.mind < d.maxMind()) {
                d.mind = Math.min(d.maxMind(),
                        d.mind + (SandevistanData.MAX_MIND / SandevistanData.REGEN_SECONDS) / rate);
            } else if (d.mind > d.maxMind()) {
                d.mind = d.maxMind();
            }
        } else if (d.active) {
            // a Mente chega a 0 e o jogador pode continuar usando: cada 50 de Mente gasta soma 5 de ciberpsicose
            float cost = (SandevistanData.MAX_MIND / d.variant().seconds) / rate;
            if (d.mind > 0f) {
                d.mind = Math.max(0f, d.mind - cost);
                d.useAcc += cost;
                while (d.useAcc >= SandevistanData.PSY_USE_STEP) {
                    d.useAcc -= SandevistanData.PSY_USE_STEP;
                    gainIns(p, d, SandevistanData.PSY_PER_STEP);
                }
            } else {
                // Mente em 0: +2 de ciberpsicose por segundo de uso
                d.zeroT += 1f / rate;
                while (d.zeroT >= 1f) {
                    d.zeroT -= 1f;
                    gainIns(p, d, 2f);
                }
            }
            boolean forced = d.psy >= LOSS_OF_CONTROL;
            if (d.mind <= 0f && !d.zeroWarned) {
                d.zeroWarned = true;
                if (!forced && d.zeroHitCd <= 0f) {
                    chat(p, Tx.t(ChatFormatting.RED, "mindzero"));
                    // chegou em 0: 3 coracoes de dano, uma vez (so de novo depois de recuperar Mente e zerar outra vez)
                    // e com recarga de 1,5 s entre um e outro
                    realDamage(p, 6f);
                    d.zeroHitCd = ZERO_HIT_COOLDOWN;
                }
            }
            if (d.mind <= 0f) {
                // usando com a Mente em 0: 2 coracoes a cada 8 s de uso (o tempo acumulado nao zera ao parar)
                // sem o controle do implante (acima de 100%) nao ha esse dano
                if (!forced) {
                    d.zeroDmgT += 1f / rate;
                    while (d.zeroDmgT >= ZERO_DMG_PERIOD) {
                        d.zeroDmgT -= ZERO_DMG_PERIOD;
                        realDamage(p, ZERO_DMG_AMOUNT);
                    }
                }
            } else {
                d.zeroWarned = false;
            }
            applyBoost(p);
        } else {
            if (d.mind > 0f) d.zeroWarned = false;
            d.zeroT = 0f;
            if (d.mind < d.maxMind()) {
                d.mind = Math.min(d.maxMind(),
                        d.mind + (SandevistanData.MAX_MIND / d.variant().regenSeconds()) / rate);
            }
        }
        // a ciberpsicose vale para qualquer jogador, com ou sem implante (o Sandevistan e so mais um implante)
        psychosisTick(p, d, 1f / rate);
        insanityTick(p, d, 1f / rate);
        ExtLogic.tick(p, d, 1f / rate);
        if (d.implanted) forcedToggle(p, d, 1f / rate);
        Float last = LAST_SENT.get(p.getUUID());
        long sig = d.syncSig();
        Long lastSig = LAST_SIG.get(p.getUUID());
        if (last == null || Math.abs(d.mind - last) >= 1f || (d.mind >= d.maxMind() && last < d.mind)
                || lastSig == null || lastSig != sig) {
            LAST_SIG.put(p.getUUID(), sig);
            send(p, d);
        }
    }

    /** Sem controle (ciberpsicose acima de 100%): liga e desliga sozinho, 5 a 10 segundos ligado. */
    private static void forcedToggle(ServerPlayer p, SandevistanData d, float dt) {
        if (d.psy < LOSS_OF_CONTROL) {
            d.forceT = 0f;
            return;
        }
        d.forceT -= dt;
        if (d.forceT > 0f) return;
        if (d.active) {
            deactivate(p, d, true);
            d.forceT = 3f + p.getRandom().nextFloat() * 5f;
        } else {
            activate(p, d);
            d.forceT = 5f + p.getRandom().nextFloat() * 5f;
        }
    }

    /** Dano que nao some com a Resistencia do proprio Sandevistan: compensa a reducao do efeito. */
    private static void realDamage(ServerPlayer p, float amount) {
        MobEffectInstance res = p.getEffect(MobEffects.DAMAGE_RESISTANCE);
        float factor = 1f;
        if (res != null) factor = Math.max(0.05f, 1f - 0.2f * (res.getAmplifier() + 1));
        p.invulnerableTime = 0;
        p.hurt(p.damageSources().magic(), amount / factor);
    }

    // ---------- ciberpsicose ----------
    /** Fase 1 a 5: 1-25, 26-50, 51-75, 76-99, 100+. */
    public static int phase(float psy) {
        return psy < 26f ? 1 : psy < 51f ? 2 : psy < 76f ? 3 : psy < 100f ? 4 : 5;
    }

    /** Segundos para baixar 1 de ciberpsicose esperando com o Sandevistan desligado, por fase. */
    private static final float[] DECAY_SECONDS = {0f, 5f, 10f, 15f, 20f, 60f};
    /** Segundos para perder 1 de Mente durante os sintomas, por fase. */
    private static final float[] DRAIN_SECONDS = {0f, 30f, 15f, 7f, 5f, 1f};
    /** Forca, Pressa e Velocidade dos sintomas por fase (valor = nivel - 1; -1 = sem o efeito). */
    private static final int[] PSY_STRENGTH = {-1, 0, 0, 1, 1, 2};
    private static final int[] PSY_HASTE = {-1, -1, 0, 1, 1, 2};
    private static final int[] PSY_SPEED = {-1, -1, -1, 0, 0, 2};
    private static final SoundEvent[] STRANGE_SOUNDS = {
            SoundEvents.WARDEN_HEARTBEAT, SoundEvents.ENDERMAN_STARE, SoundEvents.ENDER_DRAGON_GROWL,
            SoundEvents.ELDER_GUARDIAN_CURSE, SoundEvents.WITHER_AMBIENT, SoundEvents.PHANTOM_AMBIENT
    };

    private static final Map<UUID, Long> LAST_SIG = new HashMap<>();

    /** Sobe a Insanidade (a carga alta e o uso do Sandevistan caem aqui; ela e que sobe a ciberpsicose). */
    private static void gainIns(ServerPlayer p, SandevistanData d, float amount) {
        if (!p.isAlive()) return;
        // com a Mente fragil a Insanidade sobe um pouco mais facil
        if (d.ins < 100f) amount *= LowMind.gainFactor(d);
        d.ins = Math.min(100f, d.ins + amount);
    }

    /** Remedio em pilula: baixa 1 de Insanidade por segundo durante 30 s. Nao gasta se nao ha Insanidade ou se ja esta agindo. */
    /** Valores por tier (1 a 3). Estabilizador: Mente maxima recuperada; Pilula: Insanidade por segundo; Imunossupressor: psicose tirada e Mente atual. */
    public static final float[] REMEDY_MIND = {8f, 12f, 16f};
    public static final float[] PILL_RATE = {1f, 1.5f, 2f};
    public static final float[] IMMUNO_TAKE = {40f, 40f, 40f};
    /** Fracao da ciberpsicose tirada que volta quando a dose acaba: tier I 2/3, tier II 1/3, tier III nada. */
    public static final float[] IMMUNO_BACK = {2f / 3f, 1f / 3f, 0f};
    public static final float[] IMMUNO_MIND = {20f, 30f, 40f};

    public static boolean takePill(ServerPlayer p, int tier) {
        SandevistanData d = SandevistanData.get(p);
        if (d.insCalm > 0f) {
            chat(p, Tx.t(ChatFormatting.RED, "pill.active"));
            return false;
        }
        if (d.ins <= 0f) {
            chat(p, Tx.t(ChatFormatting.RED, "pill.noins"));
            return false;
        }
        d.insCalm = PILL_SECONDS;
        d.insRate = PILL_RATE[Math.max(1, Math.min(3, tier)) - 1];
        chat(p, Tx.t(ChatFormatting.AQUA, "pill.used", (int) PILL_SECONDS));
        send(p, d);
        return true;
    }

    private static final float PILL_SECONDS = 30f;
    /** Segundos para perder 1 de Insanidade parado, por fase da propria Insanidade (igual a ciberpsicose). */
    private static final float INS_START_PERIOD = 7f;
    private static final float INS_END_PERIOD = 1f;

    /** Insanidade: efeito da pilula, subida da ciberpsicose a partir de 20% e descida igual a da ciberpsicose. dt = segundos reais. */
    private static void insanityTick(ServerPlayer p, SandevistanData d, float dt) {
        if (!p.isAlive()) return;
        if (d.insCalm > 0f) {
            float use = Math.min(dt, d.insCalm);
            d.insCalm -= use;
            d.ins = Math.max(0f, d.ins - use * d.insRate);
        }
        // a Insanidade sobe a ciberpsicose: de +1 a cada 10 s (em 20%) ate +1 a cada 2 s (em 100%); acima de 100% de ciberpsicose ela ja sobe sozinha
        if (d.ins >= SandevistanData.INS_START && d.psy < 100f) {
            float t = (d.ins - SandevistanData.INS_START) / (100f - SandevistanData.INS_START);
            float period = INS_START_PERIOD + (INS_END_PERIOD - INS_START_PERIOD) * Math.min(1f, t);
            d.insT += dt;
            while (d.insT >= period) {
                d.insT -= period;
                gainPsy(p, d, 1f);
            }
        } else {
            d.insT = 0f;
        }
        // descida: parado e com a carga abaixo do limite, no mesmo ritmo da ciberpsicose (por fase)
        boolean loadHigh = d.capacity() >= d.loadThreshold();
        if (d.ins > 0f && !d.active && !loadHigh) {
            d.insDecayT += dt;
            float period = DECAY_SECONDS[phase(d.ins)];
            while (d.ins > 0f && d.insDecayT >= period) {
                d.insDecayT -= period;
                d.ins = Math.max(0f, d.ins - 1f);
                period = DECAY_SECONDS[phase(d.ins)];
            }
        } else {
            d.insDecayT = 0f;
        }
    }

    private static void gainPsy(ServerPlayer p, SandevistanData d, float amount) {
        float old = d.psy;
        d.psy = Math.min(SandevistanData.MAX_PSY, d.psy + amount);
        if (old < 100f && d.psy >= 100f) chat(p, Tx.t(ChatFormatting.DARK_RED, "psy.limit"));
    }

    /** Comando (OP): mode 0 soma, 1 tira, 2 define a Mente permanente (0 a 100). */
    public static void adjustMind(ServerPlayer p, int mode, int value) {
        SandevistanData d = SandevistanData.get(p);
        float cur = SandevistanData.MAX_MIND - d.mindLost;
        float now = mode == 0 ? cur + value : mode == 1 ? cur - value : value;
        now = Math.max(0f, Math.min(SandevistanData.MAX_MIND, now));
        d.mindLost = SandevistanData.MAX_MIND - now;
        d.mind = Math.min(d.mind, d.maxMind());
        send(p, d);
    }

    /** Comando (OP): mode 0 soma, 1 tira, 2 define a Mente ATUAL (a que regenera), sem passar da Mente maxima. */
    public static void adjustCurrentMind(ServerPlayer p, int mode, int value) {
        SandevistanData d = SandevistanData.get(p);
        float now = mode == 0 ? d.mind + value : mode == 1 ? d.mind - value : value;
        d.mind = Math.max(0f, Math.min(d.maxMind(), now));
        if (d.mind > 0f) d.zeroWarned = false;
        send(p, d);
    }

    /** Comando de teste: define a Insanidade (0 a 100). */
    public static void setIns(ServerPlayer p, float value) {
        SandevistanData d = SandevistanData.get(p);
        d.ins = Math.max(0f, Math.min(100f, value));
        send(p, d);
    }

    /** Comando de teste: define a ciberpsicose. */
    public static void setPsy(ServerPlayer p, float value) {
        SandevistanData d = SandevistanData.get(p);
        d.psy = Math.max(0f, Math.min(SandevistanData.MAX_PSY, value));
        send(p, d);
    }

    /** Cura Mente: recupera Mente perdida de vez (maxima sobe) e a Mente atual, sem passar de 100. */
    private static void healMind(SandevistanData d, float amount) {
        float fix = Math.min(amount, d.mindLost);
        d.mindLost -= fix;
        d.mind = Math.min(d.maxMind(), d.mind + amount);
    }

    /** Toma o Estabilizador Neural: recupera Mente maxima (8/12/16) na hora e fica 1 minuto com Mente Boa (outro refaz o tempo e cura de novo). */
    public static boolean takeRemedy(ServerPlayer p, int tier) {
        SandevistanData d = SandevistanData.get(p);
        d.goodMind = SandevistanData.GOOD_MIND_SECONDS;
        d.goodT = 0f;
        d.goodHealT = 0f;
        float amt = REMEDY_MIND[Math.max(1, Math.min(3, tier)) - 1];
        healMind(d, amt);
        syncGoodEffect(p, d);
        chat(p, Tx.t(ChatFormatting.GREEN, "remedy.used", (int) amt));
        send(p, d);
        return true;
    }

    /** Imunossupressor tomado pelo proprio jogador. */
    public static boolean takeImmuno(ServerPlayer p, int tier) {
        return applyImmuno(p, p, tier);
    }

    /** Segundos sem usar para a sequencia de Imunossupressores quebrar. */
    private static final float IMMUNO_SEQ_SECONDS = 180f;

    /** Quanto da ciberpsicose o Imunossupressor tira, pela posicao na sequencia (1 a 4 normal; 5-8 metade; 9-12 um quarto; depois metade a cada um). */
    private static float immunoFactor(int n) {
        if (n <= 4) return 1f;
        if (n <= 8) return 0.5f;
        if (n <= 12) return 0.25f;
        return 0.25f * (float) Math.pow(0.5, n - 12);
    }

    /**
     * Imunossupressor em 'target' (ele mesmo ou outro jogador, por injecao): so funciona com ciberpsicose acima de 0.
     * Restaura 20 de Mente ATUAL e tira ciberpsicose (ate 40 x fator da sequencia). Cada dose tem o seu proprio minuto:
     * ao acabar a dose volta uma parte (2/3, 1/3 ou nada, conforme o tier); nao tira Mente maxima.
     */
    public static boolean applyImmuno(ServerPlayer target, ServerPlayer giver, int tier) {
        SandevistanData d = SandevistanData.get(target);
        if (d.psy <= 0f) {
            chat(giver, Tx.t(ChatFormatting.RED, "immuno.nopsy"));
            return false;
        }
        float before = d.mind;
        int ti = Math.max(1, Math.min(3, tier)) - 1;
        d.mind = Math.min(d.maxMind(), d.mind + IMMUNO_MIND[ti]);
        float gain = d.mind - before;
        int n = d.immuSeq + 1;
        float took = Math.min(IMMUNO_TAKE[ti] * immunoFactor(n) * LowMind.immunoFactor(d), d.psy);
        if (took < 0.5f) took = 0f;
        d.psy -= took;
        d.doses.add(new float[] {SandevistanData.DOSE_SECONDS, took * IMMUNO_BACK[ti]});
        d.immuSeq = n;
        d.immuSeqT = IMMUNO_SEQ_SECONDS;
        chat(target, Tx.t(ChatFormatting.AQUA, "immuno.used", Math.round(gain), Math.round(took)));
        send(target, d);
        return true;
    }

    /** Fim de UMA dose: volta a ciberpsicose guardada dela (nao tira Mente maxima). */
    private static void endDose(ServerPlayer p, SandevistanData d, float psyBack, boolean notify) {
        d.psy = Math.min(SandevistanData.MAX_PSY, d.psy + psyBack);
        if (notify && psyBack >= 0.5f) chat(p, Tx.t(ChatFormatting.RED, "immuno.end", Math.round(psyBack)));
    }

    private static void syncGoodEffect(ServerPlayer p, SandevistanData d) {
        MobEffectInstance cur = p.getEffect(ModRegistries.GOOD_MIND);
        if (d.goodMind > 0f) {
            int want = (int) (d.goodMind * 20f) + 10;
            if (cur == null || Math.abs(cur.getDuration() - want) > 60) {
                p.addEffect(new MobEffectInstance(ModRegistries.GOOD_MIND, want, 0, true, true, true));
            }
        } else if (cur != null) {
            p.removeEffect(ModRegistries.GOOD_MIND);
        }
    }

    private static void dropPsyBuffs(ServerPlayer p) {
        dropEffect(p, MobEffects.DAMAGE_BOOST);
        dropEffect(p, MobEffects.DIG_SPEED);
        dropEffect(p, MobEffects.MOVEMENT_SPEED);
    }

    /** dt = segundos reais deste tick. */
    private static void psychosisTick(ServerPlayer p, SandevistanData d, float dt) {
        if (d.immuSeqT > 0f) {
            d.immuSeqT -= dt;
            if (d.immuSeqT <= 0f) d.immuSeq = 0;
        }
        if (!d.doses.isEmpty()) {
            java.util.Iterator<float[]> it = d.doses.iterator();
            while (it.hasNext()) {
                float[] ds = it.next();
                ds[0] -= dt;
                if (ds[0] <= 0f) {
                    it.remove();
                    endDose(p, d, ds[1], true);
                }
            }
        }
        // Mente Boa: 1% de chance por segundo de curar 1 de Mente e -1 de ciberpsicose a cada 5 s (com o implante desligado)
        if (d.goodMind > 0f) {
            d.goodMind = Math.max(0f, d.goodMind - dt);
            // a cada segundo com o efeito ha 5% de chance de recuperar 1 de Mente maxima
            d.goodHealT += dt;
            while (d.goodHealT >= 1f) {
                d.goodHealT -= 1f;
                if (p.getRandom().nextFloat() < 0.05f) healMind(d, 1f);
            }
            if (!d.active && d.psy > LowMind.creepCap(d) && d.psy < 100f) {
                d.goodT += dt;
                while (d.goodT >= 5f) {
                    d.goodT -= 5f;
                    d.psy = Math.max(LowMind.creepCap(d), d.psy - 1f);
                }
            }
        }
        if (p.tickCount % 10 == 0) syncGoodEffect(p, d);

        // os sintomas valem usando o Sandevistan; parado, so comecam 5 s depois de desligar
        if (d.active) {
            d.idle = 0f;
            d.decayT = 0f;
        } else {
            d.idle += dt;
        }

        // a partir de 100% ela so sobe: +1 a cada 1,5 s, e esperar nao adianta (so o Imunossupressor alivia)
        if (d.psy >= 100f) {
            d.riseT += dt;
            while (d.riseT >= 1.5f) {
                d.riseT -= 1.5f;
                gainPsy(p, d, 1f);
            }
        } else {
            d.riseT = 0f;
        }

        // esperar com o implante desligado baixa a ciberpsicose
        if (!d.active && d.psy > LowMind.creepCap(d) && d.psy < 100f) {
            d.decayT += dt;
            float period = DECAY_SECONDS[phase(d.psy)];
            while (d.psy > LowMind.creepCap(d) && d.decayT >= period) {
                d.decayT -= period;
                d.psy = Math.max(LowMind.creepCap(d), d.psy - 1f);
                period = DECAY_SECONDS[phase(d.psy)];
            }
        } else {
            d.decayT = 0f;
        }

        // os efeitos negativos so comecam 5 s depois de parar de usar
        boolean state = d.psy > 0f && (d.active || d.idle >= SandevistanData.PSY_IDLE_SECONDS);
        if (state != d.psychotic) {
            d.psychotic = state;
            if (state) {
                bar(p, Tx.t(ChatFormatting.DARK_RED, "psy.on"));
            } else {
                if (!d.active) dropPsyBuffs(p);
            }
        }
        if (!state) {
            d.blind = false;
            d.blindT = 0f;
            return;
        }

        int ph = phase(d.psy);
        // a partir de 100% o jogador nunca mais controla: fica sempre em apagao (o cliente deixa ver as vezes)
        if (d.psy >= 100f) {
            d.blind = true;
            d.blindT = 5f;
        }
        // apagao: a partir de 70% o jogador fica sem enxergar por alguns segundos (o cliente o move e faz bater)
        if (d.blind) {
            d.blindT -= dt;
            if (d.blindT <= 0f || d.psy < 70f) {
                d.blind = false;
                d.blindT = 0f;
            }
        }
        // com o Sandevistan ligado os efeitos dele ja sao bem mais fortes: os da fase so valem parado
        if (!d.active && d.psy >= d.effectStart() && p.tickCount % 10 == 0) {
            if (PSY_STRENGTH[ph] >= 0) ensure(p, MobEffects.DAMAGE_BOOST, 60, 100, PSY_STRENGTH[ph]);
            if (PSY_HASTE[ph] >= 0) ensure(p, MobEffects.DIG_SPEED, 60, 100, PSY_HASTE[ph]);
            if (PSY_SPEED[ph] >= 0) ensure(p, MobEffects.MOVEMENT_SPEED, 60, 100, PSY_SPEED[ph]);
        }

        // perde Mente PARA SEMPRE: a Mente maxima cai 1 (100 vira 99) a cada intervalo da fase
        d.drainT += dt;
        float drain = DRAIN_SECONDS[ph];
        while (d.drainT >= drain) {
            d.drainT -= drain;
            d.mindLost = Math.min(SandevistanData.MAX_MIND, d.mindLost + 1f);
            d.mind = Math.min(d.mind, d.maxMind());
        }

        // uma vez por segundo real: nausea, apagao, sons estranhos e dano
        d.secT += dt;
        while (d.secT >= 1f) {
            d.secT -= 1f;
            perSecond(p, d, ph);
        }
    }

    /** Chance por segundo do apagao: so a partir de 70%; rara aos 70%, incomum aos 80%, comum aos 90% (100% ja nao controla). */
    private static float blackoutChance(float psy) {
        if (psy < 70f) return 0f;
        if (psy < 80f) return 0.003f + (psy - 70f) / 10f * 0.007f;
        if (psy < 90f) return 0.02f + (psy - 80f) / 10f * 0.02f;
        return 0.07f + Math.min(9f, psy - 90f) / 9f * 0.06f;
    }

    private static void perSecond(ServerPlayer p, SandevistanData d, int ph) {
        float psy = d.psy;
        // nausea curta de vez em quando
        if (psy >= d.effectStart() && p.getRandom().nextFloat() < Math.min(0.6f, 0.04f + psy * 0.004f)) {
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false, false));
        }
        // apagao (a partir de 70%): chance por segundo e duracao crescem com a ciberpsicose
        if (!d.blind && psy >= 70f && p.getRandom().nextFloat() < blackoutChance(psy)) {
            d.blind = true;
            d.blindT = Math.min(30f, 10f + (psy - 70f) * 0.3f);
        }
        // sons estranhos (so depois de 25%), so para o proprio jogador
        // dos 75% aos 100% os sons vao baixando ate ficarem mudos; de 100% em diante ficam mudos (so a musica toca)
        float soundFade = psy < 75f ? 1f : psy >= 100f ? 0f : (100f - psy) / 25f;
        if (soundFade > 0f && psy > 25f && p.getRandom().nextFloat() < Math.min(0.5f, 0.05f + psy * 0.004f)) {
            // os sons de Minecraft continuam; os dois sons de psicose do mod entram no sorteio
            if (p.getRandom().nextFloat() < 0.4f) {
                playOnly(p, (p.getRandom().nextBoolean() ? ModRegistries.PSY_SND_1 : ModRegistries.PSY_SND_2).get(), 0.4f * soundFade, 1.0f);
            } else {
                SoundEvent ev = STRANGE_SOUNDS[p.getRandom().nextInt(STRANGE_SOUNDS.length)];
                p.connection.send(new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ev),
                        SoundSource.PLAYERS, p, 0.45f * soundFade, 0.6f + p.getRandom().nextFloat() * 0.8f,
                        p.getRandom().nextLong()));
            }
        }
        // dano por segundo: so a partir de 175% (1 coracao); de 225% em diante, 3 coracoes
        float dmg = 0f;
        if (psy >= 225f) dmg = 6f;
        else if (psy >= 175f) dmg = 2f;
        if (dmg > 0f) {
            realDamage(p, dmg);
        }
    }

    // ---------- morte, login, saida ----------
    public static void resetOnDeath(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        d.active = false;
        for (float[] ds : d.doses) endDose(p, d, ds[1], false);
        d.doses.clear();
        d.mind = d.maxMind();
        d.psy = 0f;
        d.ins = 0f;
        d.insCalm = 0f;
        d.immuSeq = 0;
        d.immuSeqT = 0f;
        d.blind = false;
        d.goodMind = 0f;
        d.useAcc = 0f;
        d.psychotic = false;
        d.zeroDmgT = 0f;
        d.forceT = 0f;
        removeBoost(p);
        ARMED.remove(p.getUUID());
    }

    public static void onRespawn(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        d.active = false;
        removeBoost(p);
        LAST_CODE.remove(p.getUUID());
        broadcastState(p, d);
        send(p, d);
    }

    public static void onLogin(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        // os atributos temporarios nao sao salvos: se saiu ligado, volta desligado
        d.active = false;
        // efeitos salvos de uma sessao que fechou com o Sandevistan ligado
        removeBoost(p);
        LAST_CODE.remove(p.getUUID());
        sendStatesTo(p);
        broadcastState(p, d);
        send(p, d);
    }

    public static void onLogout(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        if (d.active) {
            d.active = false;
            removeBoost(p);
        }
        updateTickRate(p.server);
        LAST_CODE.remove(p.getUUID());
        LAST_SENT.remove(p.getUUID());
        LAST_SIG.remove(p.getUUID());
        LAST_TOGGLE.remove(p.getUUID());
        DODGE_CLEAR.remove(p.getUUID());
        ARMED.remove(p.getUUID());
        PacketDistributor.sendToAllPlayers(new StatePayload(p.getUUID(), 0));
    }

    public static void clearAll() {
        ARMED.clear();
        LAST_TOGGLE.clear();
        LAST_CODE.clear();
        LAST_SENT.clear();
        LAST_SIG.clear();
        appliedRate = 20f;
    }
}
