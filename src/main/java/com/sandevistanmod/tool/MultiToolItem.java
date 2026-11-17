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
    public static final int SLOTS = ToolMods.SLOTS;
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
                .component(DataComponents.TOOL, tool).attributes(attributes());
    }

    /** Dano 8 e velocidade de ataque 0.7 (o Modulo de Velocidade soma 0.4 por ponto, via ToolMods). */
    private static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.3, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
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
    }
}
