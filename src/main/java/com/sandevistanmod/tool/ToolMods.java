package com.sandevistanmod.tool;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.registry.ModRegistries;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Modulos de ferramenta: leitura/gravacao nos itens (Multiuso e ferramentas Ciber) e regras do que cada ferramenta aceita. */
public final class ToolMods {
    private ToolMods() {}

    public static final int SLOTS = 4;
    private static final String KEY = "sdv_mods";
    /** Tipos de modulo (codigo = tipo * 10 + tier). */
    public static final int SPEED = 1, DAMAGE = 2, EFFICIENCY = 3, FORTUNE = 4, UNBREAK = 5, FREEZE = 6, VENOM = 7, WITHER = 8, THUNDER = 9;
    /** Classes de ferramenta. */
    public static final int M = 1, P = 2, B = 4, X = 8, T = 16;

    /** Modulos de encantamento: codigo = 100 + indice * 10 + nivel. */
    public record Ench(ResourceKey<Enchantment> key, int max, int mask) {}

    public static final List<Ench> ENCH = List.of(
            new Ench(Enchantments.FIRE_ASPECT, 2, M),
            new Ench(Enchantments.KNOCKBACK, 2, M),
            new Ench(Enchantments.LOOTING, 3, M),
            new Ench(Enchantments.SWEEPING_EDGE, 3, M),
            new Ench(Enchantments.SILK_TOUCH, 1, P),
            new Ench(Enchantments.PUNCH, 2, B),
            new Ench(Enchantments.FLAME, 1, B),
            new Ench(Enchantments.INFINITY, 1, B),
            new Ench(Enchantments.MULTISHOT, 1, X),
            new Ench(Enchantments.PIERCING, 4, X),
            new Ench(Enchantments.QUICK_CHARGE, 3, X),
            new Ench(Enchantments.LOYALTY, 3, T),
            new Ench(Enchantments.CHANNELING, 1, T),
            new Ench(Enchantments.RIPTIDE, 3, T),
            new Ench(Enchantments.IMPALING, 5, T),
            new Ench(Enchantments.MENDING, 1, M | P | B | X | T));

    public static boolean isModTool(ItemStack st) {
        return !st.isEmpty() && (st.getItem() instanceof MultiToolItem || st.getItem() instanceof ModTools.Cyber);
    }

    public static int mask(Item it) {
        if (it instanceof MultiToolItem) return M | P;
        if (it instanceof SwordItem || it instanceof AxeItem) return M;
        if (it instanceof PickaxeItem || it instanceof ShovelItem || it instanceof HoeItem) return P;
        if (it instanceof BowItem) return B;
        if (it instanceof CrossbowItem) return X;
        if (it instanceof TridentItem) return T;
        return 0;
    }

    // ---- codigos ----
    public static int type(int code) {
        return code > 0 && code < 100 ? code / 10 : 0;
    }

    public static int tier(int code) {
        return code % 10;
    }

    public static boolean isEnchCode(int code) {
        return code >= 100;
    }

    public static int enchIndex(int code) {
        return (code - 100) / 10;
    }

    public static int enchCode(int idx, int lvl) {
        return 100 + idx * 10 + lvl;
    }

    /** Codigo do modulo (item) ou 0. Modulo de encantamento le o que guarda. */
    public static int moduleId(ItemStack st) {
        if (st.isEmpty()) return 0;
        if (st.is(ModRegistries.MOD_ENCANTO.get())) {
            CompoundTag tag = st.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            String id = tag.getString("e");
            int lvl = tag.getInt("l");
            for (int i = 0; i < ENCH.size(); i++) {
                if (ENCH.get(i).key().location().getPath().equals(id) && lvl >= 1 && lvl <= 5) return enchCode(i, lvl);
            }
            return 0;
        }
        for (int t = 0; t < ModRegistries.MODULOS.size(); t++) {
            for (int k = 0; k < 3; k++) if (st.is(ModRegistries.MODULOS.get(t).get(k).get())) return (t + 1) * 10 + k + 1;
        }
        return 0;
    }

    public static ItemStack moduleItem(int code) {
        if (code <= 0) return ItemStack.EMPTY;
        if (isEnchCode(code)) {
            int idx = enchIndex(code);
            if (idx < 0 || idx >= ENCH.size()) return ItemStack.EMPTY;
            ItemStack st = new ItemStack(ModRegistries.MOD_ENCANTO.get());
            String id = ENCH.get(idx).key().location().getPath();
            int lvl = tier(code);
            CustomData.update(DataComponents.CUSTOM_DATA, st, t -> {
                t.putString("e", id);
                t.putInt("l", lvl);
            });
            return st;
        }
        int t = code / 10;
        int k = code % 10;
        if (t < 1 || t > ModRegistries.MODULOS.size() || k < 1 || k > 3) return ItemStack.EMPTY;
        return new ItemStack(ModRegistries.MODULOS.get(t - 1).get(k - 1).get());
    }

    /** Um livro encantado vira modulo: devolve o codigo do primeiro encantamento da lista que este livro tem (e que a ferramenta aceita), ou 0. */
    public static int bookCode(ItemStack tool, ItemStack book, int[] others, RegistryAccess ra) {
        if (!book.is(Items.ENCHANTED_BOOK)) return 0;
        ItemEnchantments en = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (int i = 0; i < ENCH.size(); i++) {
            for (Holder<Enchantment> h : en.keySet()) {
                if (!h.is(ENCH.get(i).key())) continue;
                int lvl = Math.min(en.getLevel(h), ENCH.get(i).max());
                if (lvl < 1) continue;
                int code = enchCode(i, lvl);
                if (accepts(tool, code, others, ra)) return code;
            }
        }
        return 0;
    }

    public static boolean isConvertible(ItemStack st) {
        return st.is(Items.ENCHANTED_BOOK);
    }

    // ---- dados na ferramenta ----
    public static int[] getMods(ItemStack tool) {
        int[] raw = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntArray(KEY);
        int[] out = new int[SLOTS];
        System.arraycopy(raw, 0, out, 0, Math.min(raw.length, SLOTS));
        return out;
    }

    /** Soma dos tiers dos modulos de um tipo. */
    public static int power(ItemStack tool, int type) {
        int n = 0;
        if (tool.isEmpty() || !isModTool(tool)) return 0;
        for (int m : getMods(tool)) if (type(m) == type) n += m % 10;
        return n;
    }

    /** Maior tier de um tipo (usado pelos Aspectos). */
    public static int maxTier(ItemStack tool, int type) {
        int n = 0;
        if (tool.isEmpty() || !isModTool(tool)) return 0;
        for (int m : getMods(tool)) if (type(m) == type) n = Math.max(n, m % 10);
        return n;
    }

    // ---- regras ----
    private static Holder<Enchantment> h(RegistryAccess ra, ResourceKey<Enchantment> k) {
        return ra.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(k);
    }

    /** Encantamentos (vanilla) que um modulo causa nesta ferramenta. */
    private static void enchantsOf(ItemStack tool, int code, Map<ResourceKey<Enchantment>, Integer> want) {
        int mask = mask(tool.getItem());
        int t = type(code);
        int tr = tier(code);
        if (t == 0) {
            int idx = enchIndex(code);
            if (idx >= 0 && idx < ENCH.size()) want.merge(ENCH.get(idx).key(), tr, Math::max);
            return;
        }
        switch (t) {
            case EFFICIENCY -> {
                if ((mask & P) != 0) want.merge(Enchantments.EFFICIENCY, tr, Integer::sum);
            }
            case FORTUNE -> {
                if ((mask & P) != 0) want.merge(Enchantments.FORTUNE, tr, Integer::sum);
            }
            case DAMAGE -> {
                if ((mask & (M | T)) != 0) want.merge(Enchantments.SHARPNESS, tr, Integer::sum);
                else if ((mask & B) != 0) want.merge(Enchantments.POWER, tr, Integer::sum);
            }
            case UNBREAK -> want.merge(Enchantments.UNBREAKING, tr, Integer::sum);
            case SPEED -> {
                if ((mask & X) != 0) want.merge(Enchantments.QUICK_CHARGE, tr, Integer::sum);
            }
            default -> {}
        }
    }

    private static int cap(ResourceKey<Enchantment> k) {
        if (k == Enchantments.EFFICIENCY) return 8;
        if (k == Enchantments.SHARPNESS) return 10;
        if (k == Enchantments.POWER) return 8;
        if (k == Enchantments.FORTUNE) return 5;
        if (k == Enchantments.UNBREAKING) return 5;
        if (k == Enchantments.QUICK_CHARGE) return 4;
        return 10;
    }

    /** A ferramenta aceita este modulo (considerando os modulos que ja tem)? */
    public static boolean accepts(ItemStack tool, int code, int[] others, RegistryAccess ra) {
        if (code <= 0 || !isModTool(tool)) return false;
        int mask = mask(tool.getItem());
        int t = type(code);
        switch (t) {
            case SPEED, DAMAGE, FREEZE, VENOM, WITHER, THUNDER -> {
                if ((mask & (M | B | X | T)) == 0) return false;
            }
            case EFFICIENCY, FORTUNE -> {
                if ((mask & P) == 0) return false;
            }
            case UNBREAK -> {}
            case 0 -> {
                int idx = enchIndex(code);
                if (idx < 0 || idx >= ENCH.size()) return false;
                Ench e = ENCH.get(idx);
                if ((e.mask() & mask) == 0) return false;
                if (e.key() == Enchantments.SWEEPING_EDGE && !(tool.getItem() instanceof SwordItem)) return false;
                if (tier(code) > e.max()) return false;
            }
            default -> {
                return false;
            }
        }
        // incompatibilidades do vanilla entre os encantamentos que os modulos dao
        Map<ResourceKey<Enchantment>, Integer> mine = new LinkedHashMap<>();
        enchantsOf(tool, code, mine);
        for (int o : others) {
            if (o <= 0) continue;
            if (t == 0 && o == code) return false; // mesmo encantamento duas vezes
            if (t == 0 && isEnchCode(o) && enchIndex(o) == enchIndex(code)) return false;
            Map<ResourceKey<Enchantment>, Integer> theirs = new LinkedHashMap<>();
            enchantsOf(tool, o, theirs);
            for (ResourceKey<Enchantment> a : mine.keySet()) {
                for (ResourceKey<Enchantment> b : theirs.keySet()) {
                    if (a != b && !Enchantment.areCompatible(h(ra, a), h(ra, b))) return false;
                }
            }
        }
        return true;
    }

    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "speed_module");

    /** Grava os modulos na ferramenta e refaz encantamentos e velocidade de ataque que eles dao. */
    public static void setMods(ItemStack tool, int[] ids, RegistryAccess ra) {
        CustomData.update(DataComponents.CUSTOM_DATA, tool, tag -> tag.putIntArray(KEY, ids));
        int mask = mask(tool.getItem());
        int spd = 0;
        Map<ResourceKey<Enchantment>, Integer> want = new LinkedHashMap<>();
        for (int m : ids) {
            if (m <= 0) continue;
            if (type(m) == SPEED) spd += m % 10;
            enchantsOf(tool, m, want);
        }
        // velocidade de ataque (modulo de Velocidade) nas armas de corpo a corpo
        ItemAttributeModifiers base = tool.getItem().components().get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (base != null) {
            ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
            for (ItemAttributeModifiers.Entry e : base.modifiers()) b.add(e.attribute(), e.modifier(), e.slot());
            if (spd > 0 && (mask & (M | T)) != 0) {
                b.add(Attributes.ATTACK_SPEED, new AttributeModifier(SPEED_ID, 0.4 * spd, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            }
            tool.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());
        }
        Map<ResourceKey<Enchantment>, Integer> fin = new LinkedHashMap<>();
        for (Map.Entry<ResourceKey<Enchantment>, Integer> e : want.entrySet()) fin.put(e.getKey(), Math.min(e.getValue(), cap(e.getKey())));
        EnchantmentHelper.updateEnchantments(tool, m -> {
            m.removeIf(x -> true);
            for (Map.Entry<ResourceKey<Enchantment>, Integer> e : fin.entrySet()) m.set(h(ra, e.getKey()), e.getValue());
        });
    }

    /** Linhas para o tooltip: ranhuras usadas e o poder de cada tipo. */
    public static List<String> summary(ItemStack tool) {
        List<String> out = new ArrayList<>();
        int[] mods = getMods(tool);
        int used = 0;
        for (int m : mods) if (m > 0) used++;
        out.add("slots:" + used);
        for (int t = 1; t <= 9; t++) {
            int n = t >= FREEZE ? maxTier(tool, t) : power(tool, t);
            if (n > 0) out.add(t + ":" + n);
        }
        return out;
    }
}
