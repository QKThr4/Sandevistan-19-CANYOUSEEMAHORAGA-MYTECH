package com.sandevistanmod.energy;

import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Menu do carregador (um encaixe de bateria) e do gerador a combustivel (um encaixe de combustivel, como uma fornalha). */
public class EnergyMenu extends AbstractContainerMenu {
    /** Dados sincronizados: 0 energia no bloco, 1 geracao ou fluxo (FE/t), 2 queima restante, 3 queima total, 4 bateria em milesimos. */
    private final ContainerData data;
    private final int kind;
    private final EnergyBlockEntity be;

    public EnergyMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, buf.readVarInt());
    }

    public EnergyMenu(int id, Inventory inv, EnergyBlockEntity be, int kind) {
        super(ModRegistries.ENERGY_MENU.get(), id);
        this.be = be;
        this.kind = kind;
        Container c = be == null ? new SimpleContainer(1) : new Container() {
            @Override public int getContainerSize() { return 1; }
            @Override public boolean isEmpty() { return be.battery().isEmpty(); }
            @Override public ItemStack getItem(int i) { return be.battery(); }
            @Override public ItemStack removeItem(int i, int n) {
                ItemStack r = be.battery().split(n);
                be.setChanged();
                return r;
            }
            @Override public ItemStack removeItemNoUpdate(int i) {
                ItemStack r = be.battery();
                be.setBattery(ItemStack.EMPTY);
                return r;
            }
            @Override public void setItem(int i, ItemStack st) { be.setBattery(st); }
            @Override public void setChanged() { be.setChanged(); }
            @Override public boolean stillValid(Player p) { return !be.isRemoved() && p.distanceToSqr(be.getBlockPos().getCenter()) < 64; }
            @Override public void clearContent() { be.setBattery(ItemStack.EMPTY); }
        };
        this.data = be == null ? new SimpleContainerData(5) : new ContainerData() {
            @Override
            public int get(int i) {
                switch (i) {
                    case 0: return kind == EnergyBlock.CHARGER ? be.energy() : be.generating();
                    case 1: return kind == EnergyBlock.CHARGER ? be.intake() : be.flow();
                    case 2: return be.burn();
                    case 3: return be.burnMax();
                    default:
                        ItemStack b = be.battery();
                        int cap = b.isEmpty() ? 0 : b.getMaxDamage();
                        return cap <= 0 ? 0 : (int) ((long) BatteryItem.energyOf(b) * 1000L / cap);
                }
            }

            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 5; }
        };
        this.addSlot(new Slot(c, 0, 80, 28) {
            @Override
            public boolean mayPlace(ItemStack st) {
                return kind == EnergyBlock.CHARGER ? st.getCapability(Capabilities.EnergyStorage.ITEM) != null
                        : st.getBurnTime(RecipeType.SMELTING) > 0;
            }

            @Override
            public int getMaxStackSize() {
                return kind == EnergyBlock.CHARGER ? 1 : 64;
            }
        });
        for (int r = 0; r < 3; r++) {
            for (int col = 0; col < 9; col++) this.addSlot(new Slot(inv, col + r * 9 + 9, 8 + col * 18, 84 + r * 18));
        }
        for (int col = 0; col < 9; col++) this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public int kind() { return kind; }
    public int energy() { return data.get(0); }
    public int rate() { return data.get(1); }
    public int burn() { return data.get(2); }
    public int burnMax() { return data.get(3); }
    public int batteryPermille() { return data.get(4); }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack out = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack st = slot.getItem();
            out = st.copy();
            if (index == 0) {
                if (!moveItemStackTo(st, 1, 37, true)) return ItemStack.EMPTY;
            } else if (!slots.get(0).mayPlace(st) || !moveItemStackTo(st, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return out;
    }

    @Override
    public boolean stillValid(Player p) {
        return be == null || (!be.isRemoved() && p.distanceToSqr(be.getBlockPos().getCenter()) < 64);
    }
}
