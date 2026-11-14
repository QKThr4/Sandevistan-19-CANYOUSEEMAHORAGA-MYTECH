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
    private final SimpleContainer box = new SimpleContainer(1 + MultiToolItem.SLOTS) {
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
                return st.getItem() instanceof MultiToolItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int k = 0; k < MultiToolItem.SLOTS; k++) {
            this.addSlot(new Slot(box, 1 + k, 38 + k * 28, 52) {
                @Override
                public boolean mayPlace(ItemStack st) {
                    return !box.getItem(0).isEmpty() && MultiToolItem.moduleId(st) > 0;
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

    private void changed() {
        if (busy || player.level().isClientSide) return;
        busy = true;
        try {
            ItemStack tool = box.getItem(0);
            if (tool.isEmpty()) {
                for (int k = 1; k <= MultiToolItem.SLOTS; k++) box.setItem(k, ItemStack.EMPTY);
                lastTool = ItemStack.EMPTY;
            } else if (tool != lastTool) {
                // ferramenta nova na mesa: mostra os modulos que ela ja tem
                int[] mods = MultiToolItem.getMods(tool);
                for (int k = 0; k < MultiToolItem.SLOTS; k++) box.setItem(k + 1, MultiToolItem.moduleItem(mods[k]));
                lastTool = tool;
            } else {
                int[] ids = new int[MultiToolItem.SLOTS];
                for (int k = 0; k < MultiToolItem.SLOTS; k++) ids[k] = MultiToolItem.moduleId(box.getItem(k + 1));
                MultiToolItem.setMods(tool, ids, player.level().registryAccess());
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
        } else if (st.getItem() instanceof MultiToolItem) {
            if (!this.moveItemStackTo(st, 0, 1, false)) return ItemStack.EMPTY;
        } else if (MultiToolItem.moduleId(st) > 0) {
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
