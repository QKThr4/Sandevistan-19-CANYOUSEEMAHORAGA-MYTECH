package com.sandevistanmod.tool;

import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Ferramentas de Aco (nivel entre ferro e diamante, mais perto do ferro) e Ciber (nivel de diamante, 4 modulos, sem encantar). */
public final class ModTools {
    private ModTools() {}

    /** Marca as ferramentas Ciber: aceitam 4 modulos na Mesa de Upgrades e nao podem ser encantadas. */
    public interface Cyber {}

    public static final class ModTier implements Tier {
        private final int uses;
        private final float speed;
        private final float damage;
        private final int ench;
        private final TagKey<Block> incorrect;

        ModTier(int uses, float speed, float damage, int ench, TagKey<Block> incorrect) {
            this.uses = uses;
            this.speed = speed;
            this.damage = damage;
            this.ench = ench;
            this.incorrect = incorrect;
        }

        @Override
        public int getUses() {
            return uses;
        }

        @Override
        public float getSpeed() {
            return speed;
        }

        @Override
        public float getAttackDamageBonus() {
            return damage;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return incorrect;
        }

        @Override
        public int getEnchantmentValue() {
            return ench;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModRegistries.LINGOTE_ACO.get());
        }
    }

    // aco nao quebra obsidiana (como o ferro); Ciber quebra (como o diamante)
    public static final ModTier STEEL = new ModTier(450, 6.5f, 2.5f, 12, BlockTags.INCORRECT_FOR_IRON_TOOL);
    public static final ModTier STEEL_MACE = new ModTier(600, 6.5f, 2.5f, 12, BlockTags.INCORRECT_FOR_IRON_TOOL);
    public static final ModTier CYBER = new ModTier(1561, 8.0f, 3.0f, 0, BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
    public static final ModTier CYBER_MACE = new ModTier(2000, 8.0f, 3.0f, 0, BlockTags.INCORRECT_FOR_DIAMOND_TOOL);

    private static Item.Properties sword(Tier t) {
        return new Item.Properties().attributes(SwordItem.createAttributes(t, 3, -2.4f));
    }

    private static Item.Properties pick(Tier t) {
        return new Item.Properties().attributes(DiggerItem.createAttributes(t, 1.0f, -2.8f));
    }

    private static Item.Properties axe(Tier t) {
        return new Item.Properties().attributes(DiggerItem.createAttributes(t, 6.0f, -3.1f));
    }

    private static Item.Properties shovel(Tier t) {
        return new Item.Properties().attributes(DiggerItem.createAttributes(t, 1.5f, -3.0f));
    }

    private static Item.Properties hoe(Tier t) {
        return new Item.Properties().attributes(DiggerItem.createAttributes(t, -2.0f, -1.0f));
    }

    /** Maca: age como um machado (sem o golpe esmagador), quebra escudos e e mais duravel. */
    private static Item.Properties mace(Tier t) {
        return new Item.Properties().attributes(DiggerItem.createAttributes(t, 7.0f, -3.2f));
    }

    private static Item.Properties trident(int uses, double dmg) {
        return new Item.Properties().durability(uses).rarity(net.minecraft.world.item.Rarity.UNCOMMON)
                .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                        .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, dmg,
                                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                        .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.9,
                                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                        .build())
                .component(DataComponents.TOOL, TridentItem.createToolProperties());
    }

    // ---------------- Aco ----------------
    public static Item steelSword() { return new SwordItem(STEEL, sword(STEEL)); }
    public static Item steelPick() { return new PickaxeItem(STEEL, pick(STEEL)); }
    public static Item steelAxe() { return new AxeItem(STEEL, axe(STEEL)); }
    public static Item steelShovel() { return new ShovelItem(STEEL, shovel(STEEL)); }
    public static Item steelHoe() { return new HoeItem(STEEL, hoe(STEEL)); }
    public static Item steelBow() { return new BowItem(new Item.Properties().durability(440)); }
    public static Item steelCrossbow() { return new CrossbowItem(new Item.Properties().durability(540)); }
    // vanilla: arco 384, besta 465, tridente 250 (maca/tridente ~10% mais dano base)
    public static Item steelTrident() { return new TridentItem(trident(400, 7.8)); }
    public static Item steelMace() { return new Mace(STEEL_MACE); }

    /** Flecha de aco: +25% de dano, atravessa 1 mob e serve em qualquer arco/besta. */
    public static class SteelArrow extends ArrowItem {
        public SteelArrow(Item.Properties p) {
            super(p);
        }

        @Override
        public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, ItemStack weapon) {
            AbstractArrow a = super.createArrow(level, ammo, shooter, weapon);
            a.setBaseDamage(a.getBaseDamage() * 1.25);
            a.setPierceLevel((byte) 1);
            return a;
        }
    }

    public static class Mace extends AxeItem {
        public Mace(ModTier t) {
            super(t, mace(t));
        }
    }

    private static boolean no(Holder<Enchantment> e) {
        return false;
    }

    // ---------------- Ciber (nao encantaveis, 4 modulos) ----------------
    public static class CySword extends SwordItem implements Cyber {
        public CySword() { super(CYBER, sword(CYBER)); }
        @Override public boolean isEnchantable(ItemStack s) { return false; }
        public boolean supportsEnchantment(ItemStack s, Holder<Enchantment> e) { return no(e); }
    }

    public static class CyPick extends PickaxeItem implements Cyber {
        public CyPick() { super(CYBER, pick(CYBER)); }
        @Override public boolean isEnchantable(ItemStack s) { return false; }
        public boolean supportsEnchantment(ItemStack s, Holder<Enchantment> e) { return no(e); }
    }

    public static class CyAxe extends AxeItem implements Cyber {
        public CyAxe() { super(CYBER, axe(CYBER)); }
        @Override public boolean isEnchantable(ItemStack s) { return false; }
        public boolean supportsEnchantment(ItemStack s, Holder<Enchantment> e) { return no(e); }
    }

    public static class CyShovel extends ShovelItem implements Cyber {
        public CyShovel() { super(CYBER, shovel(CYBER)); }
        @Override public boolean isEnchantable(ItemStack s) { return false; }
        public boolean supportsEnchantment(ItemStack s, Holder<Enchantment> e) { return no(e); }
    }

    public static class CyHoe extends HoeItem implements Cyber {
        public CyHoe() { super(CYBER, hoe(CYBER)); }
        @Override public boolean isEnchantable(ItemStack s) { return false; }
        public boolean supportsEnchantment(ItemStack s, Holder<Enchantment> e) { return no(e); }
    }

    public static class CyBow extends BowItem implements Cyber {
        public CyBow() { super(new Item.Properties().durability(900)); }
        @Override public boolean isEnchantable(ItemStack s) { return false; }
        public boolean supportsEnchantment(ItemStack s, Holder<Enchantment> e) { return no(e); }
    }

    public static class CyCrossbow extends CrossbowItem implements Cyber {
        public CyCrossbow() { super(new Item.Properties().durability(900)); }
        @Override public boolean isEnchantable(ItemStack s) { return false; }
        public boolean supportsEnchantment(ItemStack s, Holder<Enchantment> e) { return no(e); }
    }

    public static class CyTrident extends TridentItem implements Cyber {
        public CyTrident() { super(trident(900, 8.4)); }
        @Override public boolean isEnchantable(ItemStack s) { return false; }
        public boolean supportsEnchantment(ItemStack s, Holder<Enchantment> e) { return no(e); }
    }

    public static class CyMace extends Mace implements Cyber {
        public CyMace() { super(CYBER_MACE); }
        @Override public boolean isEnchantable(ItemStack s) { return false; }
        public boolean supportsEnchantment(ItemStack s, Holder<Enchantment> e) { return no(e); }
    }
}
