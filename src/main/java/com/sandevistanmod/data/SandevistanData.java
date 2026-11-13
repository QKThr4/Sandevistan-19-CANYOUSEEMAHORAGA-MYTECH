package com.sandevistanmod.data;

import com.sandevistanmod.registry.ModRegistries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Estado dos implantes de um jogador (salvo no jogador e copiado ao morrer). */
public class SandevistanData implements INBTSerializable<CompoundTag> {
    public static final float MAX_MIND = 100f;
    /** Tempo, em segundos reais, que a barra de Mente cheia dura com o Sandevistan ligado. */
    public static final float USE_SECONDS = 30f;
    /** Tempo, em segundos reais, para a barra encher do zero (1 segundo de uso a cada 3 segundos parado) com o Sandevistan desligado. */
    public static final float REGEN_SECONDS = 90f;
    /** Mente minima para conseguir ligar o Sandevistan. */
    public static final float MIN_TO_START = 10f;
    /** Tick rate do jogo inteiro enquanto o Sandevistan esta ligado. */
    public static final int SLOW_TICK_RATE = 3;

    /** Ciberpsicose maxima guardada (a barra mostra 100%, mas ela pode passar disso). */
    public static final float MAX_PSY = 300f;
    /** Insanidade (%) a partir da qual a ciberpsicose comeca a subir. */
    public static final float INS_START = 20f;
    /** A cada 50 de Mente gasta (50% de uso) o jogador ganha 5 de ciberpsicose. */
    public static final float PSY_USE_STEP = 50f;
    public static final float PSY_PER_STEP = 5f;
    /** Segundos parado (sem ligar o Sandevistan) ate os efeitos negativos comecarem. */
    public static final float PSY_IDLE_SECONDS = 0f;
    /** Duracao do efeito Mente Boa do remedio e tempo (s) para regenerar 1 de Mente com ele. */
    public static final float GOOD_MIND_SECONDS = 60f;
    public static final float GOOD_MIND_REGEN_SECONDS = 30f;

    // ---- implantes: encaixes 0 costas (Sandevistan), 1 braco direito, 2 braco esquerdo, 3 link neural ----
    public static final int SLOT_BACK = 0;
    public static final int SLOT_ARM_R = 1;
    public static final int SLOT_ARM_L = 2;
    public static final int SLOT_NEURAL = 3;
    public static final int SLOT_BONE = 4;
    public static final int SLOT_NERVE = 5;
    public static final int SLOT_HEART = 6;
    public static final int SLOT_LEG_R = 7;
    public static final int SLOT_LEG_L = 8;
    public static final int SLOT_COUNT = 9;

    /** Quanto cada implante soma na Carga Cibernetica (%) e cada chip. */
    public static final float LOAD_SANDEVISTAN = 30f;
    public static final float LOAD_ARM = 10f;
    public static final float LOAD_NEURAL = 0f;
    /** Estrutura Ossea Reforcada e Sistema Nervoso Aprimorado. */
    public static final float LOAD_BONE = 5f;
    public static final float LOAD_NERVE = 5f;
    /** Coracao de Ferro (5%) e cada Perna Cibernetica (10%). */
    public static final float LOAD_HEART = 5f;
    public static final float LOAD_LEG = 10f;
    public static final float LOAD_CHIP = 5f;
    /** Mente permanente que cada chip inserido tira e segundos para ele baixar antes de funcionar. */
    public static final float CHIP_MIND = 5f;
    public static final float CHIP_LOAD_SECONDS = 10f;
    /** Duracao de cada Imunossupressor (s). */
    public static final float DOSE_SECONDS = 60f;

    public static final int CHIP_MARTIAL = 1;
    public static final int CHIP_PAIN = 2;
    public static final int CHIP_MINING = 3;
    public static final int CHIP_WALKER = 4;
    public static final int CHIP_RAM = 5;
    public static final int CHIP_TYPES = 5;
    /** Carga a partir da qual a ciberpsicose por carga comeca (%) e quanto o Chip de RAM (tier I/II/III) soma a ela. */
    public static final float BASE_LOAD_THRESHOLD = 70f;
    public static final float[] RAM_BONUS = {5f, 7f, 10f};
    /** Durabilidade de cada chip por tipo e tier (1 uso = 1 de durabilidade); o de Mineracao nao tem tiers. */
    private static final int[][] CHIP_DURABILITY = {
            {0, 0, 0}, {800, 900, 1000}, {500, 600, 700}, {2000, 2000, 2000}, {400, 500, 600}, {800, 800, 800}};
    /** Mente permanente que cada chip tira, por tier. */
    private static final float[] CHIP_MIND_TIER = {5f, 4f, 3f};

    public static final int MAX_TIER = 3;

    public static int clampTier(int t) {
        return Math.max(1, Math.min(MAX_TIER, t));
    }

    /** Este tipo de chip tem tiers? (so o de Mineracao nao tem) */
    public static boolean chipHasTiers(int type) {
        return type == CHIP_MARTIAL || type == CHIP_PAIN || type == CHIP_WALKER || type == CHIP_RAM;
    }

    public static int chipMax(int type) {
        return chipMax(type, 1);
    }

    public static int chipMax(int type, int tier) {
        return type >= 1 && type <= CHIP_TYPES ? CHIP_DURABILITY[type][clampTier(tier) - 1] : 0;
    }

    public static float chipMindOf(int tier) {
        return CHIP_MIND_TIER[clampTier(tier) - 1];
    }

    /** Mente permanente que o chip tira: os tiers baixam o custo, menos no Chip de RAM (sempre 5). */
    public static float chipMindFor(int type, int tier) {
        return type == CHIP_RAM ? CHIP_MIND : chipMindOf(tier);
    }

    /** Carga Cibernetica que o chip soma: o Chip de RAM nao soma nada (senao anularia o proprio bonus). */
    public static float chipLoadOf(int type) {
        return type == CHIP_RAM ? 0f : LOAD_CHIP;
    }

    /** Bonus (%) que o Chip de RAM deste tier da ao limite de carga. */
    public static float ramBonus(int tier) {
        return RAM_BONUS[clampTier(tier) - 1];
    }

    /** Este encaixe de implante tem tiers? (Sandevistan e Link Neural nao tem) */
    public static boolean slotHasTiers(int slot) {
        return slot != SLOT_BACK && slot != SLOT_NEURAL;
    }

    // ---- valores por tier (indice 0 = tier I) ----
    public static final double[] ARM_DAMAGE = {2.0, 2.5, 3.0};
    public static final double[] LEG_SPEED = {0.15, 0.18, 0.20};
    public static final double[] LEG_JUMP = {0.04, 0.05, 0.06};
    public static final double[] LEG_FALL = {0.15, 0.18, 0.21};
    public static final double[] BONE_ARMOR = {3.0, 4.0, 5.0};
    public static final double[] BONE_KB = {0.25, 0.30, 0.35};
    public static final double[] BONE_FALL = {0.30, 0.35, 0.40};
    public static final double[] NERVE_SPEED = {0.08, 0.10, 0.12};
    public static final double[] NERVE_ATTACK = {0.12, 0.15, 0.18};
    public static final double[] HEART_HP = {10.0, 12.0, 14.0};
    public static final double[] HEART_HEAL = {0.10, 0.15, 0.20};
    private static final float[] LOAD_BIG_T = {9f, 11f, 13f};
    private static final float[] LOAD_SMALL_T = {4f, 5f, 6f};

    public static double at(double[] arr, int tier) {
        return arr[clampTier(tier) - 1];
    }

    /** Texto de um numero para a interface: 2, 2.5, 3. */
    public static String num(double v) {
        return v == Math.rint(v) ? Integer.toString((int) v) : Double.toString(v);
    }

    /** Percentual inteiro de uma fracao (0.18 -> 18). */
    public static int pct(double v) {
        return (int) Math.round(v * 100.0);
    }

    public static float loadOf(int slot) {
        return loadOf(slot, 1);
    }

    public static float loadOf(int slot, int tier) {
        int i = clampTier(tier) - 1;
        return switch (slot) {
            case SLOT_BACK -> LOAD_SANDEVISTAN;
            case SLOT_ARM_R, SLOT_ARM_L, SLOT_LEG_R, SLOT_LEG_L -> LOAD_BIG_T[i];
            case SLOT_BONE, SLOT_NERVE, SLOT_HEART -> LOAD_SMALL_T[i];
            default -> LOAD_NEURAL;
        };
    }

    /** Argumentos dos textos de efeito de cada implante neste tier (para tooltips e aba de efeitos). */
    public static Object[] statArgs(int slot, int tier) {
        switch (slot) {
            case SLOT_ARM_R, SLOT_ARM_L:
                return new Object[] {num(at(ARM_DAMAGE, tier)), num(at(ARM_DAMAGE, tier) * 2)};
            case SLOT_LEG_R, SLOT_LEG_L:
                return new Object[] {pct(at(LEG_SPEED, tier)), num(at(LEG_JUMP, tier)), pct(at(LEG_FALL, tier))};
            case SLOT_BONE:
                return new Object[] {(int) at(BONE_ARMOR, tier), pct(at(BONE_KB, tier)), pct(at(BONE_FALL, tier))};
            case SLOT_NERVE:
                return new Object[] {pct(at(NERVE_SPEED, tier)), pct(at(NERVE_ATTACK, tier))};
            case SLOT_HEART:
                return new Object[] {(int) (at(HEART_HP, tier) / 2), pct(at(HEART_HEAL, tier))};
            default:
                return new Object[0];
        }
    }

    /** Carga que o Sandevistan soma, por modelo (0 Apogee ... 4 QianT). */
    public static float loadOfVariant(int variant) {
        return SandevistanVariant.of(variant).load;
    }

    /** True depois que o video da primeira vez ja tocou para este implante das costas. */
    public boolean introSeen;
    public boolean implanted;
    public boolean armR;
    public boolean armL;
    public boolean neural;
    public boolean bone;
    public boolean nerve;
    public boolean heart;
    public boolean legR;
    public boolean legL;
    /** Modelo do Sandevistan implantado: 0 Apogee, 1 Falcon, 2 Dynalar, 3 Zetatech, 4 QianT. */
    public int variant;
    /** Os bracos de ferro aparecem no modelo do jogador. */
    public boolean armsVisible = true;
    /** As pernas de ferro aparecem no modelo do jogador. */
    public boolean legsVisible = true;
    /** Dois encaixes de chip no Link Neural: tipo (0 vazio), dano de durabilidade e segundos que faltam para baixar. */
    public final int[] chip = new int[2];
    public final int[] chipDmg = new int[2];
    /** Tier (1 a 3) de cada chip inserido e de cada implante implantado (por encaixe). */
    public final int[] chipTier = {1, 1};
    public final int[] tier = {1, 1, 1, 1, 1, 1, 1, 1, 1};
    public final float[] chipLoad = new float[2];

    public boolean active;
    public float mind = MAX_MIND;
    /** Ciberpsicose (0 a 300; 100 = limite da barra) e Mente Boa restante (segundos reais). */
    public float psy;
    /** Insanidade (0 a 100): sobe com a Carga Cibernetica alta e com o uso do Sandevistan; a partir de 20% ela sobe a ciberpsicose. */
    public float ins;
    /** Segundos que faltam do efeito da Pilula (baixa 1 de Insanidade por segundo). */
    public float insCalm;
    /** Insanidade que a pilula em uso tira por segundo (1 / 1,5 / 2 conforme o tier). */
    public float insRate = 1f;
    public float goodMind;
    /** Mente perdida PARA SEMPRE pela ciberpsicose (a Mente maxima cai); so o Imunossupressor recupera. */
    public float mindLost;
    /** Doses de Imunossupressor ativas: {segundos restantes, ciberpsicose que volta quando acabar}. Cada uma tem o seu tempo. */
    public final List<float[]> doses = new ArrayList<>();
    /** Apagao da ciberpsicose (a partir de 70%): o jogador nao ve nada e age sozinho. Enviado ao cliente. */
    public boolean blind;
    /** Sequencia de Imunossupressores: quantos seguidos e segundos restantes para a sequencia quebrar. */
    public int immuSeq;
    public float immuSeqT;
    /** Mente gasta ainda nao convertida em ciberpsicose. */
    public float useAcc;
    /** Sintomas ativos agora (parou de usar ha mais de 5 s): enviado ao cliente para os efeitos de tela. */
    public boolean psychotic;

    // temporizadores so do servidor (nao salvos)
    public transient float idle;
    public transient float decayT;
    public transient float drainT;
    public transient float secT;
    public transient float goodT;
    public transient boolean zeroWarned;
    public transient float zeroT;
    public transient float zeroDmgT;
    public transient float riseT;
    public transient float goodHealT;
    public transient float blindT;
    public transient float forceT;
    public transient float zeroHitCd;
    public transient float loadT;
    public transient float insT;
    public transient float insDecayT;
    public transient float ramT;
    /** Temporizadores dos sintomas da Mente baixa (so do servidor). */
    public final transient float[] lowT = new float[8];
    public transient int swapA = -1;
    public transient int swapB = -1;
    public transient ItemStack swapRefA = ItemStack.EMPTY;
    public transient ItemStack swapRefB = ItemStack.EMPTY;

    public static SandevistanData get(Player p) {
        return p.getData(ModRegistries.SANDEVISTAN);
    }

    public boolean has(int slot) {
        return switch (slot) {
            case SLOT_BACK -> implanted;
            case SLOT_ARM_R -> armR;
            case SLOT_ARM_L -> armL;
            case SLOT_BONE -> bone;
            case SLOT_NERVE -> nerve;
            case SLOT_HEART -> heart;
            case SLOT_LEG_R -> legR;
            case SLOT_LEG_L -> legL;
            default -> neural;
        };
    }

    public int tierOf(int slot) {
        return slot >= 0 && slot < SLOT_COUNT && slotHasTiers(slot) ? clampTier(tier[slot]) : 1;
    }

    public void set(int slot, boolean value, int newTier) {
        set(slot, value);
        tier[slot] = value && slotHasTiers(slot) ? clampTier(newTier) : 1;
    }

    public void set(int slot, boolean value) {
        switch (slot) {
            case SLOT_BACK -> implanted = value;
            case SLOT_ARM_R -> armR = value;
            case SLOT_ARM_L -> armL = value;
            case SLOT_BONE -> bone = value;
            case SLOT_NERVE -> nerve = value;
            case SLOT_HEART -> heart = value;
            case SLOT_LEG_R -> legR = value;
            case SLOT_LEG_L -> legL = value;
            default -> neural = value;
        }
    }

    /** Mente maxima PERMANENTE (100 menos o que a ciberpsicose tirou; os chips sao removiveis e nao contam). */
    public float permMind() {
        return Math.max(0f, MAX_MIND - mindLost);
    }

    /** Sintomas da Mente baixa: 0 nenhum, 1 abaixo de 60%, 2 abaixo de 30%, 3 abaixo de 10%. */
    public int lowTier() {
        float m = permMind();
        return m < 10f ? 3 : m < 30f ? 2 : m < 60f ? 1 : 0;
    }

    /** Ciberpsicose a partir da qual forca, nausea e tela tremendo comecam (15%, ou 10% com a Mente muito baixa). */
    public float effectStart() {
        return lowTier() >= 2 ? 10f : 15f;
    }

    public boolean hasAny() {
        return implanted || armR || armL || neural || bone || nerve || heart || legR || legL;
    }

    public boolean hasExt() {
        for (int v : ext) if (v != 0) return true;
        return false;
    }

    public int arms() {
        return (armR ? 1 : 0) + (armL ? 1 : 0);
    }

    public int legs() {
        return (legR ? 1 : 0) + (legL ? 1 : 0);
    }

    public int chips() {
        return (chip[0] != 0 ? 1 : 0) + (chip[1] != 0 ? 1 : 0);
    }

    public SandevistanVariant variant() {
        return SandevistanVariant.of(variant);
    }

    /** Carga Cibernetica (%): Sandevistan 10 a 30 (pelo modelo), cada braco 10, cada chip 5, osso e nervos 5, Link Neural 0. */
    public float capacity() {
        float sum = implanted ? variant().load : 0f;
        for (int i = 0; i < 2; i++) if (chip[i] != 0) sum += chipLoadOf(chip[i]);
        for (int s = 1; s < SLOT_COUNT; s++) if (has(s)) sum += loadOf(s, tierOf(s));
        for (int s = 0; s < EXT_COUNT; s++) if (ext[s] != 0) sum += extLoad(s, ext[s], eyeType);
        return sum;
    }

    /** Carga (%) a partir da qual a ciberpsicose por carga comeca: 70, mais o bonus do Chip de RAM ativo. */
    public float loadThreshold() {
        for (int i = 0; i < 2; i++) {
            if (chip[i] == CHIP_RAM && chipActive(CHIP_RAM)) return BASE_LOAD_THRESHOLD + ramBonus(chipTier[i]);
        }
        return BASE_LOAD_THRESHOLD;
    }

    /** Este tipo de chip esta inserido, ja baixou e ainda tem durabilidade? */
    public boolean chipActive(int type) {
        for (int i = 0; i < 2; i++) {
            if (chip[i] == type && chipLoad[i] <= 0f && chipDmg[i] < chipMax(type, chipTier[i])) return true;
        }
        return false;
    }

    /** Encaixe (0 ou 1) onde esta o chip deste tipo, ou -1. */
    public int chipSlot(int type) {
        for (int i = 0; i < 2; i++) if (chip[i] == type) return i;
        return -1;
    }

    /** Mente permanente que os chips inseridos tiram. */
    public float chipMind() {
        float sum = 0f;
        for (int i = 0; i < 2; i++) if (chip[i] != 0) sum += chipMindFor(chip[i], chipTier[i]);
        return sum;
    }

    /** Mente maxima atual (100 menos o que a ciberpsicose e os chips tiraram). */
    public float maxMind() {
        return Math.max(0f, Math.min(MAX_MIND, MAX_MIND - mindLost - chipMind()));
    }

    /** Segundos reais restantes de uso com a Mente atual. */
    public int secondsLeft() {
        return Math.max(0, (int) Math.ceil(mind / (MAX_MIND / variant().seconds)));
    }

    /**
     * Codigo enviado aos outros jogadores: bit 1 Sandevistan, bit 2 ligado, bit 4 com sintomas, bits 3 a 9 ciberpsicose (0 a 100),
     * bit 10 braco direito, bit 11 braco esquerdo, bit 12 Link Neural, bit 13 bracos visiveis, bits 14 a 16 modelo do Sandevistan, bit 17 perna direita, bit 18 perna esquerda.
     */
    public int stateCode() {
        int level = Math.max(0, Math.min(100, (int) Math.floor(psy)));
        boolean sick = psychotic && psy > 0f;
        return (implanted ? 1 : 0) | (active && implanted ? 2 : 0) | (sick ? 4 : 0) | ((sick ? level : 0) << 3)
                | (armR ? 1 << 10 : 0) | (armL ? 1 << 11 : 0) | (neural ? 1 << 12 : 0) | (armsVisible ? 1 << 13 : 0)
                | ((variant & 7) << 14) | (legR ? 1 << 17 : 0) | (legL ? 1 << 18 : 0) | (legsVisible ? 1 << 19 : 0)
                | (ext[EXT_JET] != 0 ? 1 << 20 : 0) | (ext[EXT_BOOT_R] != 0 ? 1 << 21 : 0) | (ext[EXT_BOOT_L] != 0 ? 1 << 22 : 0)
                | (ext[EXT_SOLAR] != 0 ? 1 << 23 : 0) | (ext[EXT_REACTOR] != 0 ? 1 << 24 : 0) | (mantisOk(0) && mantisOn ? 1 << 25 : 0)
                | (mantisOk(1) && mantisOn ? 1 << 26 : 0) | (ext[EXT_INJECTOR] != 0 ? 1 << 27 : 0) | (flyingNow ? 1 << 28 : 0);
    }

    /** Muda quando algo que o cliente precisa ver mudou (usado para decidir se reenvia os dados). */
    public long syncSig() {
        long h = 17L;
        h = h * 31L + (long) Math.floor(psy);
        h = h * 31L + (long) Math.floor(ins);
        h = h * 31L + (long) Math.ceil(insCalm / 5f);
        h = h * 31L + (long) Math.ceil(goodMind);
        h = h * 31L + (psychotic ? 1 : 0);
        h = h * 31L + (blind ? 1 : 0);
        h = h * 31L + (long) Math.floor(mindLost);
        h = h * 31L + (implanted ? 1 : 0) + (armR ? 2 : 0) + (armL ? 4 : 0) + (neural ? 8 : 0) + (armsVisible ? 16 : 0)
                + (bone ? 32 : 0) + (nerve ? 64 : 0) + (heart ? 128 : 0) + (legR ? 256 : 0) + (legL ? 512 : 0) + (legsVisible ? 2048 : 0) + variant * 4096L;
        for (int i = 0; i < 2; i++) {
            h = h * 31L + chip[i];
            h = h * 31L + chipDmg[i];
            h = h * 31L + chipTier[i];
            h = h * 31L + (long) Math.ceil(chipLoad[i]);
        }
        for (int s = 0; s < SLOT_COUNT; s++) h = h * 31L + tier[s];
        for (int i = 0; i < EXT_COUNT; i++) h = h * 31L + ext[i];
        h = h * 31L + eyeType;
        h = h * 31L + (injOn ? 1 : 0);
        h = h * 31L + (mantisOn ? 1 : 0);
        h = h * 31L + mantisDur[0] * 2003L + mantisDur[1];
        h = h * 31L + licence * 2L + (qOn ? 1 : 0);
        for (int i = 0; i < qp.length; i++) h = h * 31L + qp[i];
        h = h * 31L + (eyeOn ? 1 : 0);
        h = h * 31L + (long) Math.ceil(xrayOn);
        h = h * 31L + batType;
        h = h * 31L + (batType == 0 ? 0 : batEnergy * 200L / BAT_CAP[batType]);
        h = h * 31L + doses.size();
        for (float[] ds : doses) h = h * 31L + (long) Math.ceil(ds[0]);
        return h;
    }

    // ---- Implantes extras (instalados com botao direito no item) ----
    public static final int EXT_BOOT_R = 0;
    public static final int EXT_BOOT_L = 1;
    public static final int EXT_JET = 2;
    public static final int EXT_XP = 3;
    public static final int EXT_XRAY = 4;
    public static final int EXT_EYE = 5;
    public static final int EXT_SOLAR = 6;
    public static final int EXT_REACTOR = 7;
    public static final int EXT_MANTIS_R = 8;
    public static final int EXT_MANTIS_L = 9;
    public static final int EXT_INJECTOR = 10;
    public static final int EXT_COUNT = 11;
    /** 0 = vazio; senao o tier (1 a 4) do implante naquele encaixe. */
    public final int[] ext = new int[EXT_COUNT];
    /** Tipo do par de olhos: 1 Zoom, 2 Tatico, 3 Laser, 4 Visao Noturna/Termica (0 sem olhos). */
    public int eyeType;
    /** Olho tatico/termico ligado. */
    public boolean eyeOn;
    /** Segundos restantes do Scanner de Minerios ativo; recargas so no servidor. */
    public float xrayOn;
    public transient float xrayCd;
    public transient float laserCd;
    /** Segundos restantes de carga do laser (0 = nao esta carregando). */
    public transient float laserCharge;
    /** O mod deu voo (habilidade mayfly) a este jogador. */
    public boolean flightGranted;
    /** Voando agora com jetpack/botas (so para o som e a chama dos modelos). */
    public transient boolean flyingNow;

    /** Tipo de olhos instalado (0 se nao ha). */
    public int eye() {
        return ext[EXT_EYE] != 0 ? eyeType : 0;
    }

    public int extTier(int slot) {
        return ext[slot] == 0 ? 0 : clampExtTier(ext[slot]);
    }

    /** Tier maximo dos implantes extras: so Jetpack, Reator Arc e Laminas Mantis chegam ao tier IV. */
    public static final int MAX_EXT_TIER = 4;

    public static int clampExtTier(int t) {
        return Math.max(1, Math.min(MAX_EXT_TIER, t));
    }

    public static int maxExtTier(int slot) {
        return slot == EXT_JET || slot == EXT_REACTOR || slot == EXT_MANTIS_R || slot == EXT_MANTIS_L ? 4 : 3;
    }

    public static final double[] BOOT_FALL_ONE = {0.5, 0.4, 0.3};
    public static final double[] BOOT_FALL_TWO = {0.25, 0.2, 0.15};
    public static final float[] BOOT_FLY_SPEED = {0.035f, 0.04f, 0.045f};
    public static final int[] BOOT_ALTITUDE = {4, 5, 6};
    public static final float[] BOOT_GLIDE_COST = {6f, 5f, 4f};
    public static final float[] JET_SPEED = {0.05f, 0.06f, 0.07f, 0.09f};
    public static final float[] JET_COST = {14f, 12f, 10f, 8f};
    public static final float[] XP_BONUS = {0.2f, 0.3f, 0.4f};
    public static final int[] XRAY_RADIUS = {8, 12, 16};
    public static final float[] XRAY_COOLDOWN = {35f, 30f, 25f};
    public static final float[] ZOOM_MAX = {8f, 12f, 16f};
    public static final int[] TACTICAL_RANGE = {24, 32, 48};
    public static final float[] LASER_DAMAGE = {6f, 8f, 10f};
    public static final float[] LASER_CHARGE = {1.5f, 1.2f, 0.9f};
    public static final int[] VISION_RANGE = {16, 24, 32};
    private static final float[][] EXT_LOAD = {{4f, 5f, 6f, 6f}, {4f, 5f, 6f, 6f}, {8f, 10f, 12f, 15f}, {3f, 4f, 5f, 5f}, {5f, 6f, 7f, 7f},
            {0f, 0f, 0f, 0f}, {3f, 4f, 5f, 5f}, {8f, 10f, 12f, 15f}, {7f, 8f, 9f, 11f}, {7f, 8f, 9f, 11f}, {3f, 4f, 5f, 5f}};
    private static final float[][] EYE_LOAD = {{3f, 3f, 4f, 4f}, {5f, 5f, 5f, 5f}, {5f, 6f, 7f, 7f}, {4f, 5f, 5f, 5f}};
    private static final int[][] EXT_COST = {{10, 14, 18, 18}, {10, 14, 18, 18}, {16, 22, 28, 64}, {8, 12, 16, 16}, {10, 14, 18, 18},
            {0, 0, 0, 0}, {10, 14, 18, 18}, {30, 40, 50, 120}, {14, 18, 22, 48}, {14, 18, 22, 48}, {10, 14, 18, 18}};
    private static final int[][] EYE_COST = {{8, 12, 16, 16}, {12, 16, 20, 20}, {14, 18, 24, 24}, {10, 14, 18, 18}};
    /** Energia (FE por segundo) que o Implante Solar (so de dia, ao ar livre) e o Reator Arc (sempre) recarregam na bateria. */
    public static final float[] SOLAR_REGEN = {4f, 7f, 10f};
    public static final float[] REACTOR_REGEN = {20f, 35f, 55f, 90f};
    /** Dano extra das Laminas Mantis (como uma espada de ferro, de diamante e de netherite). */
    public static final float[] MANTIS_DAMAGE = {5f, 6f, 7f, 9f};
    /** Injetor automatico: Insanidade a partir da qual toma a Pilula e ciberpsicose a partir da qual aplica o Imunossupressor. */
    public static final float[] INJ_INS = {70f, 60f, 50f};
    public static final float[] INJ_PSY = {40f, 35f, 30f};
    /** Injetor automatico ligado (o jogador liga e desliga na roleta de habilidades). */
    public boolean injOn = true;
    /** Laminas Mantis ligadas (para fora) ou recolhidas. */
    public boolean mantisOn = true;
    /** Durabilidade de cada Lamina Mantis (0 direita, 1 esquerda); perde 1 por golpe e o Ripperdoc repara. */
    public static final int MANTIS_MAX = 1000;
    public final int[] mantisDur = {MANTIS_MAX, MANTIS_MAX};

    /** A lamina (0 direita, 1 esquerda) esta instalada e inteira? */
    public boolean mantisOk(int side) {
        return ext[side == 0 ? EXT_MANTIS_R : EXT_MANTIS_L] != 0 && mantisDur[side] > 0;
    }

    // ---- Licencas do Ripperdoc (missoes que liberam os crafts de tier II, III e IV) ----
    /** Maior tier cujos crafts estao liberados (1 = so tier I). */
    public int licence = 1;
    /** Missao da proxima licenca aceita? */
    public boolean qOn;
    /** Progresso de cada objetivo da missao atual. */
    public final int[] qp = new int[6];
    public transient float qActiveAcc;
    public transient float injT;
    public transient float injPillCd;
    public transient float injImmunoCd;
    public transient boolean lowWarned;
    public transient float solarFrac;

    /** Carga Cibernetica de um implante extra (o par de olhos depende do tipo). */
    public static float extLoad(int slot, int tier, int eyeType) {
        int i = clampExtTier(tier) - 1;
        if (slot == EXT_EYE) return EYE_LOAD[Math.max(1, Math.min(4, eyeType)) - 1][i];
        return EXT_LOAD[Math.max(0, Math.min(EXT_COUNT - 1, slot))][i];
    }

    /** Preco em esmeraldas para o Ripperdoc implantar. */
    public static int extCost(int slot, int tier, int eyeType) {
        int i = clampExtTier(tier) - 1;
        int base = slot == EXT_EYE ? EYE_COST[Math.max(1, Math.min(4, eyeType)) - 1][i]
                : EXT_COST[Math.max(0, Math.min(EXT_COUNT - 1, slot))][i];
        // metade do preco antigo, e nada custa mais de 10 esmeraldas
        return Math.min(10, Math.max(1, (base + 1) / 2));
    }

    /** Quantos implantes que gastam energia estao instalados (cada um aumenta o consumo dos outros em 10%). */
    public int energyUsers() {
        int n = 0;
        for (int i = 0; i <= EXT_XRAY; i++) if (ext[i] != 0) n++;
        if (ext[EXT_EYE] != 0 && eyeType >= 2) n++;
        if (mantisOn && (mantisOk(0) || mantisOk(1))) n++;
        return n;
    }

    // ---- Energia (bateria implantada) ----
    /** Capacidade (FE) dos 5 tipos de bateria; o indice 0 e "sem bateria". */
    public static final int[] BAT_CAP = {0, 2000, 6000, 15000, 40000, 100000};
    /** Maximo de FE aceito por chamada de carga (quanto maior a bateria, mais devagar carrega). */
    public static final int[] BAT_RATE = {0, 200, 150, 100, 60, 30};
    public static final int BAT_TYPES = 5;
    /** Tipo da bateria no encaixe de energia (0 = nenhuma) e energia guardada nela. */
    public int batType;
    public int batEnergy;
    public transient float batFrac;

    public int batCap() {
        return BAT_CAP[Math.max(0, Math.min(BAT_TYPES, batType))];
    }

    public boolean hasEnergy() {
        return batType != 0 && batEnergy > 0;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("implanted", implanted);
        t.putBoolean("intro", introSeen);
        t.putBoolean("armR", armR);
        t.putBoolean("armL", armL);
        t.putBoolean("neural", neural);
        t.putBoolean("bone", bone);
        t.putBoolean("nerve", nerve);
        t.putBoolean("heart", heart);
        t.putBoolean("legR", legR);
        t.putBoolean("legL", legL);
        t.putInt("var", variant);
        t.putBoolean("armvis", armsVisible);
        t.putBoolean("legvis", legsVisible);
        t.putIntArray("chip", new int[] {chip[0], chip[1]});
        t.putIntArray("chipd", new int[] {chipDmg[0], chipDmg[1]});
        t.putIntArray("chipt", new int[] {chipTier[0], chipTier[1]});
        t.putIntArray("tiers", tier.clone());
        t.putFloat("chipl0", chipLoad[0]);
        t.putFloat("chipl1", chipLoad[1]);
        t.putBoolean("active", active);
        t.putFloat("mind", mind);
        t.putFloat("psy", psy);
        t.putFloat("ins", ins);
        t.putFloat("inscalm", insCalm);
        t.putFloat("insr", insRate);
        t.putFloat("lost", mindLost);
        t.putInt("bat", batType);
        t.putInt("bate", batEnergy);
        t.putIntArray("ext", ext.clone());
        t.putInt("eyet", eyeType);
        t.putBoolean("injon", injOn);
        t.putBoolean("mantison", mantisOn);
        t.putIntArray("mantisdur", mantisDur.clone());
        t.putInt("lic", licence);
        t.putBoolean("qon", qOn);
        t.putIntArray("qp", qp.clone());
        t.putBoolean("eyeon", eyeOn);
        t.putFloat("xrayon", xrayOn);
        t.putBoolean("flg", flightGranted);
        ListTag list = new ListTag();
        for (float[] ds : doses) {
            CompoundTag c = new CompoundTag();
            c.putFloat("t", ds[0]);
            c.putFloat("p", ds[1]);
            list.add(c);
        }
        t.put("doses", list);
        t.putBoolean("blind", blind);
        t.putInt("is", immuSeq);
        t.putFloat("ist", immuSeqT);
        t.putFloat("good", goodMind);
        t.putFloat("acc", useAcc);
        t.putBoolean("psychotic", psychotic);
        return t;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag t) {
        implanted = t.getBoolean("implanted");
        introSeen = !t.contains("intro") || t.getBoolean("intro");
        armR = t.getBoolean("armR");
        armL = t.getBoolean("armL");
        neural = t.getBoolean("neural");
        bone = t.getBoolean("bone");
        nerve = t.getBoolean("nerve");
        heart = t.getBoolean("heart");
        legR = t.getBoolean("legR");
        legL = t.getBoolean("legL");
        variant = Math.max(0, Math.min(SandevistanVariant.values().length - 1, t.getInt("var")));
        armsVisible = !t.contains("armvis") || t.getBoolean("armvis");
        legsVisible = !t.contains("legvis") || t.getBoolean("legvis");
        int[] c = t.getIntArray("chip");
        int[] cd = t.getIntArray("chipd");
        int[] ct = t.getIntArray("chipt");
        int[] tr = t.getIntArray("tiers");
        for (int s = 0; s < SLOT_COUNT; s++) tier[s] = s < tr.length && slotHasTiers(s) ? clampTier(tr[s]) : 1;
        for (int i = 0; i < 2; i++) {
            chip[i] = i < c.length && c[i] >= 1 && c[i] <= CHIP_TYPES ? c[i] : 0;
            chipTier[i] = chip[i] != 0 && i < ct.length && chipHasTiers(chip[i]) ? clampTier(ct[i]) : 1;
            chipDmg[i] = chip[i] != 0 && i < cd.length ? Math.max(0, Math.min(chipMax(chip[i], chipTier[i]), cd[i])) : 0;
            chipLoad[i] = chip[i] != 0 ? Math.max(0f, t.getFloat("chipl" + i)) : 0f;
        }
        active = t.getBoolean("active");
        mind = t.contains("mind") ? t.getFloat("mind") : MAX_MIND;
        psy = t.getFloat("psy");
        ins = Math.max(0f, Math.min(100f, t.getFloat("ins")));
        insCalm = Math.max(0f, t.getFloat("inscalm"));
        insRate = t.contains("insr") ? Math.max(1f, t.getFloat("insr")) : 1f;
        mindLost = Math.max(0f, Math.min(MAX_MIND, t.getFloat("lost")));
        batType = Math.max(0, Math.min(BAT_TYPES, t.getInt("bat")));
        batEnergy = batType == 0 ? 0 : Math.max(0, Math.min(BAT_CAP[batType], t.getInt("bate")));
        int[] ex = t.getIntArray("ext");
        for (int i = 0; i < EXT_COUNT; i++) ext[i] = i < ex.length ? Math.max(0, Math.min(maxExtTier(i), ex[i])) : 0;
        eyeType = ext[EXT_EYE] == 0 ? 0 : Math.max(1, Math.min(4, t.getInt("eyet")));
        eyeOn = t.getBoolean("eyeon") && eyeType >= 2;
        injOn = !t.contains("injon") || t.getBoolean("injon");
        mantisOn = !t.contains("mantison") || t.getBoolean("mantison");
        int[] md = t.getIntArray("mantisdur");
        for (int i = 0; i < 2; i++) mantisDur[i] = i < md.length ? Math.max(0, Math.min(MANTIS_MAX, md[i])) : MANTIS_MAX;
        licence = t.contains("lic") ? Math.max(1, Math.min(4, t.getInt("lic"))) : 1;
        qOn = t.getBoolean("qon");
        int[] qq = t.getIntArray("qp");
        for (int i = 0; i < qp.length; i++) qp[i] = i < qq.length ? Math.max(0, qq[i]) : 0;
        xrayOn = Math.max(0f, t.getFloat("xrayon"));
        flightGranted = t.getBoolean("flg");
        doses.clear();
        ListTag list = t.getList("doses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag d = list.getCompound(i);
            float left = d.getFloat("t");
            if (left > 0f) doses.add(new float[] {left, Math.max(0f, d.getFloat("p"))});
        }
        blind = t.getBoolean("blind");
        immuSeq = Math.max(0, t.getInt("is"));
        immuSeqT = Math.max(0f, t.getFloat("ist"));
        goodMind = t.getFloat("good");
        useAcc = t.getFloat("acc");
        psychotic = t.getBoolean("psychotic");
    }
}
