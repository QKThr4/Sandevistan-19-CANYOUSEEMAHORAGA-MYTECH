package com.sandevistanmod.tool;

import com.sandevistanmod.data.Tx;
import com.sandevistanmod.registry.ModRegistries;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

/** Ferramenta 3 em 1 (picareta + machado + pa): serve para qualquer bloco e como arma. Aceita ate 4 modulos na Mesa de Upgrades. */
public class MultiToolItem extends Item {
    public static final int SLOTS = 4;
    private static final String KEY = "sdv_mods";
    /** Ids dos modulos: 1 velocidade, 2 dano, 3 eficiencia, 4 fortuna. */
    public static final int SPEED = 1;
    public static final int DAMAGE = 2;
    public static final int EFFICIENCY = 3;
    public static final int FORTUNE = 4;

    public MultiToolItem(Properties p) {
        super(p);
    }

    public static Properties props() {
        Tool tool = new Tool(List.of(
                Tool.Rule.deniesDrops(BlockTags.INCORRECT_FOR_NETHERITE_TOOL),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, 9.0f),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, 9.0f),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, 9.0f)), 1.0f, 1);
        return new Properties().durability(2031).fireResistant()
                .component(DataComponents.TOOL, tool).attributes(attributes(0));
    }

    /** Dano 8 e velocidade de ataque 1.0, mais 0.4 de velocidade de ataque por ponto de Modulo de Velocidade. */
    private static ItemAttributeModifiers attributes(int speedPower) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.0 + 0.4 * speedPower, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // ---- modulos ----
    /** Codigo do modulo = tipo * 10 + tier (ex.: 32 = Eficiencia II); 0 se o item nao for um modulo. */
    public static int moduleId(ItemStack st) {
        if (st.isEmpty()) return 0;
        for (int t = 0; t < ModRegistries.MODULOS.size(); t++) {
            for (int k = 0; k < 3; k++) if (st.is(ModRegistries.MODULOS.get(t).get(k).get())) return (t + 1) * 10 + k + 1;
        }
        return 0;
    }

    public static ItemStack moduleItem(int code) {
        int t = code / 10;
        int k = code % 10;
        if (t < 1 || t > 4 || k < 1 || k > 3) return ItemStack.EMPTY;
        return new ItemStack(ModRegistries.MODULOS.get(t - 1).get(k - 1).get());
    }

    public static int[] getMods(ItemStack tool) {
        int[] raw = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntArray(KEY);
        int[] out = new int[SLOTS];
        System.arraycopy(raw, 0, out, 0, Math.min(raw.length, SLOTS));
        return out;
    }

    /** Soma dos tiers dos modulos de um tipo (cada modulo vale o seu tier: I = 1, II = 2, III = 3). */
    public static int power(ItemStack tool, int type) {
        int n = 0;
        for (int m : getMods(tool)) if (m / 10 == type) n += m % 10;
        return n;
    }

    private static final int MAX_EFF = 8;
    private static final int MAX_DMG = 10;
    private static final int MAX_FORTUNE = 5;

    /** Grava os modulos na ferramenta e refaz os encantamentos (eficiencia, afiacao e fortuna) que eles dao. */
    public static void setMods(ItemStack tool, int[] ids, RegistryAccess ra) {
        CustomData.update(DataComponents.CUSTOM_DATA, tool, tag -> tag.putIntArray(KEY, ids));
        int eff = 0;
        int dmg = 0;
        int fort = 0;
        int spd = 0;
        for (int m : ids) {
            if (m / 10 == EFFICIENCY) eff += m % 10;
            else if (m / 10 == DAMAGE) dmg += m % 10;
            else if (m / 10 == FORTUNE) fort += m % 10;
            else if (m / 10 == SPEED) spd += m % 10;
        }
        tool.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes(spd));
        var lookup = ra.lookupOrThrow(Registries.ENCHANTMENT);
        int e = Math.min(eff, MAX_EFF);
        int d = Math.min(dmg, MAX_DMG);
        int f = Math.min(fort, MAX_FORTUNE);
        EnchantmentHelper.updateEnchantments(tool, m -> {
            m.set(lookup.getOrThrow(Enchantments.EFFICIENCY), e);
            m.set(lookup.getOrThrow(Enchantments.SHARPNESS), d);
            m.set(lookup.getOrThrow(Enchantments.FORTUNE), f);
        });
    }

    // ---- comportamento ----
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        InteractionResult r = Items.NETHERITE_AXE.useOn(ctx); // descascar troncos
        if (r.consumesAction()) return r;
        return Items.NETHERITE_SHOVEL.useOn(ctx); // fazer caminho de terra
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        return true;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModRegistries.LINGOTE_ACO.get());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Tx.t(ChatFormatting.GRAY, "multi.tip"));
        int[] mods = getMods(stack);
        int used = 0;
        for (int m : mods) if (m > 0) used++;
        tip.add(Tx.t(ChatFormatting.AQUA, "multi.slots", used, SLOTS));
        int[] types = {SPEED, DAMAGE, EFFICIENCY, FORTUNE};
        String[] keys = {"multi.speed", "multi.damage", "multi.eff", "multi.fortune"};
        for (int k = 0; k < types.length; k++) {
            int n = power(stack, types[k]);
            if (n > 0) tip.add(Tx.t(ChatFormatting.GREEN, keys[k], n));
        }
    }
}
