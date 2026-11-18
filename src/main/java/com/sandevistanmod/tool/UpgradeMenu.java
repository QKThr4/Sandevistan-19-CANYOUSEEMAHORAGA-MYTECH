package com.sandevistanmod.tool;

import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Slot 0 = ferramenta; slots 1 a 4 = encaixes de modulos. Ao tirar um modulo do encaixe ele sai da ferramenta. */
public class UpgradeMenu extends AbstractContainerMenu {
    private static final int INV_FIRST = 5;
    private static final int INV_END = 41;

    private final ContainerLevelAccess access;
    private final Player player;
    private boolean busy;
    private ItemStack lastTool = ItemStack.EMPTY;
    private final SimpleContainer box = new SimpleContainer(1 + ToolMods.SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            UpgradeMenu.this.changed();
        }
    };

    public UpgradeMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public UpgradeMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModRegistries.UPGRADE_MENU.get(), id);
        this.access = access;
        this.player = inv.player;
        this.addSlot(new Slot(box, 0, 80, 20) {
            @Override
            public boolean mayPlace(ItemStack st) {
                return ToolMods.isModTool(st);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int k = 0; k < ToolMods.SLOTS; k++) {
            this.addSlot(new Slot(box, 1 + k, 38 + k * 28, 52) {
                @Override
                public boolean mayPlace(ItemStack st) {
                    return validFor(st, this.getContainerSlot());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) this.addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        }
        for (int c = 0; c < 9; c++) this.addSlot(new Slot(inv, c, 8 + c * 18, 142));
    }

    /** Modulos dos outros encaixes (menos o indicado). */
    private ToolMods.Mod[] othersOf(int skipSlot) {
        ToolMods.Mod[] o = new ToolMods.Mod[ToolMods.SLOTS];
        for (int k = 0; k < ToolMods.SLOTS; k++) o[k] = k + 1 == skipSlot ? ToolMods.Mod.NONE : ToolMods.modOf(box.getItem(k + 1));
        return o;
    }

    /** O item e um modulo que a ferramenta da mesa aceita neste encaixe? */
    private boolean validFor(ItemStack st, int slot) {
        ItemStack tool = box.getItem(0);
        if (tool.isEmpty()) return false;
        return ToolMods.accepts(tool, ToolMods.modOf(st), othersOf(slot), player.level().registryAccess());
    }

    private boolean fitsAnySlot(ItemStack st) {
        for (int k = 1; k <= ToolMods.SLOTS; k++) if (box.getItem(k).isEmpty() && validFor(st, k)) return true;
        return false;
    }

    private void changed() {
        if (busy || player.level().isClientSide) return;
        busy = true;
        try {
            ItemStack tool = box.getItem(0);
            net.minecraft.core.RegistryAccess ra = player.level().registryAccess();
            if (tool.isEmpty()) {
                for (int k = 1; k <= ToolMods.SLOTS; k++) box.setItem(k, ItemStack.EMPTY);
                lastTool = ItemStack.EMPTY;
            } else if (tool != lastTool) {
                // ferramenta nova na mesa: mostra os modulos que ela ja tem
                ToolMods.Mod[] mods = ToolMods.getModsFull(tool);
                for (int k = 0; k < ToolMods.SLOTS; k++) box.setItem(k + 1, ToolMods.modItem(mods[k]));
                lastTool = tool;
            } else {
                ToolMods.Mod[] mods = new ToolMods.Mod[ToolMods.SLOTS];
                for (int k = 0; k < ToolMods.SLOTS; k++) mods[k] = ToolMods.modOf(box.getItem(k + 1));
                ToolMods.setMods(tool, mods, ra);
            }
        } finally {
            busy = false;
        }
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        if (index < INV_FIRST) {
            if (!this.moveItemStackTo(st, INV_FIRST, INV_END, true)) return ItemStack.EMPTY;
        } else if (ToolMods.isModTool(st)) {
            if (!this.moveItemStackTo(st, 0, 1, false)) return ItemStack.EMPTY;
        } else if (fitsAnySlot(st)) {
            if (!this.moveItemStackTo(st, 1, INV_FIRST, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (st.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, st);
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof UpgradeTableBlock
                && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (p.level().isClientSide) return;
        busy = true; // os modulos ja estao gravados na ferramenta: so devolve a ferramenta
        ItemStack tool = box.getItem(0);
        box.setItem(0, ItemStack.EMPTY);
        if (!tool.isEmpty()) p.getInventory().placeItemBackInInventory(tool);
    }
}
