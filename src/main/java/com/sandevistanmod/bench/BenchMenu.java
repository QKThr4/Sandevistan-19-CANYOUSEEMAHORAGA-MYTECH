package com.sandevistanmod.bench;

import com.sandevistanmod.registry.ModRegistries;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Menu 3x3 das bancadas: so aceita itens das receitas do tipo; o resultado sai quando os itens batem exatamente com uma receita. */
public class BenchMenu extends AbstractContainerMenu {
    private static final int RESULT = 0;
    private static final int GRID_FIRST = 1;
    private static final int INV_FIRST = 10;
    private static final int INV_END = 37;
    private static final int HOTBAR_END = 46;

    private final ContainerLevelAccess access;
    private final Player player;
    private final int kind;
    private final Container result = new SimpleContainer(1);
    private final Container grid = new SimpleContainer(9) {
        @Override
        public void setChanged() {
            super.setChanged();
            BenchMenu.this.slotsChanged(this);
        }
    };

    /** Lado do cliente: o tipo vem no pacote de abertura. */
    public BenchMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, ContainerLevelAccess.NULL, buf.readVarInt());
    }

    public BenchMenu(int id, Inventory inv, ContainerLevelAccess access, int kind) {
        super(ModRegistries.BENCH_MENU.get(), id);
        this.access = access;
        this.player = inv.player;
        this.kind = kind;
        this.addSlot(new Slot(result, 0, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player p, ItemStack stack) {
                craft(p);
            }
        });
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                this.addSlot(new Slot(grid, c + r * 3, 30 + c * 18, 17 + r * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return BenchRecipes.accepts(BenchMenu.this.kind, stack);
                    }
                });
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) this.addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        }
        for (int c = 0; c < 9; c++) this.addSlot(new Slot(inv, c, 8 + c * 18, 142));
    }

    public int kind() {
        return kind;
    }

    // ---------- receita ----------
    private BenchRecipes.Recipe match() {
        int lic = com.sandevistanmod.data.SandevistanData.get(player).licence;
        for (BenchRecipes.Recipe r : BenchRecipes.of(kind)) {
            if (matches(r) && com.sandevistanmod.logic.Quests.needed(r.out()) <= lic) return r;
        }
        return null;
    }

    /** Receita que casa com a bancada mas ainda esta bloqueada (precisa da missao do Ripperdoc). */
    private BenchRecipes.Recipe lockedMatch() {
        int lic = com.sandevistanmod.data.SandevistanData.get(player).licence;
        for (BenchRecipes.Recipe r : BenchRecipes.of(kind)) {
            if (matches(r) && com.sandevistanmod.logic.Quests.needed(r.out()) > lic) return r;
        }
        return null;
    }

    private boolean matches(BenchRecipes.Recipe r) {
        int[] left = new int[r.in().size()];
        for (int j = 0; j < left.length; j++) left[j] = r.in().get(j).count();
        boolean any = false;
        for (int s = 0; s < 9; s++) {
            ItemStack st = grid.getItem(s);
            if (st.isEmpty()) continue;
            any = true;
            for (int u = 0; u < st.getCount(); u++) {
                int k = -1;
                for (int j = 0; j < left.length; j++) {
                    if (left[j] > 0 && r.in().get(j).test().test(st)) {
                        k = j;
                        break;
                    }
                }
                if (k < 0) return false;
                left[k]--;
            }
        }
        if (!any) return false;
        for (int l : left) if (l > 0) return false;
        return true;
    }

    private void updateResult() {
        if (player.level().isClientSide) return;
        BenchRecipes.Recipe r = match();
        ItemStack out = r == null ? ItemStack.EMPTY : r.out().copy();
        if (r == null && player instanceof ServerPlayer sp0) {
            BenchRecipes.Recipe lk = lockedMatch();
            if (lk != null) {
                sp0.displayClientMessage(com.sandevistanmod.data.Tx.t(net.minecraft.ChatFormatting.RED, "bench.locked",
                        com.sandevistanmod.logic.Quests.needed(lk.out())), true);
            }
        }
        result.setItem(0, out);
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT, out));
        }
    }

    @Override
    public void slotsChanged(Container c) {
        if (c == grid) updateResult();
        super.slotsChanged(c);
    }

    /** Gasta os ingredientes da receita atual (o resultado ja foi tirado do slot). */
    private void craft(Player p) {
        BenchRecipes.Recipe r = match();
        if (r == null) return;
        for (BenchRecipes.Req q : r.in()) {
            int need = q.count();
            for (int s = 0; s < 9 && need > 0; s++) {
                ItemStack st = grid.getItem(s);
                while (need > 0 && !st.isEmpty() && q.test().test(st)) {
                    ItemStack rem = st.getCraftingRemainingItem();
                    st.shrink(1);
                    need--;
                    if (!rem.isEmpty() && !p.getInventory().add(rem)) p.drop(rem, false);
                }
                if (st.isEmpty()) grid.setItem(s, ItemStack.EMPTY);
            }
        }
        grid.setChanged();
    }

    /** Clique em uma receita do livro: devolve o que estava na bancada e poe os ingredientes que o jogador tiver. */
    public void fill(Player p, int index) {
        List<BenchRecipes.Recipe> list = BenchRecipes.of(kind);
        if (index < 0 || index >= list.size()) return;
        clearContainer(p, grid);
        BenchRecipes.Recipe r = list.get(index);
        int reqTier = com.sandevistanmod.logic.Quests.needed(r.out());
        if (reqTier > com.sandevistanmod.data.SandevistanData.get(p).licence) {
            if (p instanceof ServerPlayer sp1) sp1.displayClientMessage(com.sandevistanmod.data.Tx.t(net.minecraft.ChatFormatting.RED, "bench.locked", reqTier), true);
            return;
        }
        List<ItemStack> inv = p.getInventory().items;
        for (BenchRecipes.Req q : r.in()) {
            int need = q.count();
            for (int i = 0; i < inv.size() && need > 0; i++) {
                ItemStack st = inv.get(i);
                if (st.isEmpty() || !q.test().test(st)) continue;
                int n = Math.min(need, st.getCount());
                int target = -1;
                for (int s = 0; s < 9; s++) {
                    ItemStack g = grid.getItem(s);
                    if (g.isEmpty() || (ItemStack.isSameItemSameComponents(g, st) && g.getCount() + n <= g.getMaxStackSize())) {
                        target = s;
                        break;
                    }
                }
                if (target < 0) break;
                ItemStack moved = st.split(n);
                ItemStack g = grid.getItem(target);
                if (g.isEmpty()) grid.setItem(target, moved);
                else g.grow(n);
                need -= n;
            }
        }
        grid.setChanged();
        broadcastChanges();
    }

    // ---------- interface ----------
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        if (index == RESULT) {
            if (!this.moveItemStackTo(st, INV_FIRST, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(st, copy);
        } else if (index >= INV_FIRST) {
            if (!this.moveItemStackTo(st, GRID_FIRST, INV_FIRST, false)) {
                if (index < INV_END) {
                    if (!this.moveItemStackTo(st, INV_END, HOTBAR_END, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(st, INV_FIRST, INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (!this.moveItemStackTo(st, INV_FIRST, HOTBAR_END, false)) {
            return ItemStack.EMPTY;
        }
        if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (st.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, st);
        if (index == RESULT) p.drop(st, false);
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof BenchBlock
                && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        access.execute((level, pos) -> clearContainer(p, grid));
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }
}
