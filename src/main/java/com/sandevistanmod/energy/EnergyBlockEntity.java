package com.sandevistanmod.energy;

import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Guarda a energia dos blocos de energia, gera (geradores), empurra FE para os vizinhos e carrega a bateria (carregador). */
public class EnergyBlockEntity extends BlockEntity {
    public static final int BUFFER = 20000;
    public static final int MAX_BURN = 40000;
    private static final int PUSH = 400;

    private final int kind;
    private int energy;
    private int burn;
    private int gen;
    private ItemStack battery = ItemStack.EMPTY;
    private int burnMax;
    private int flow;

    public EnergyBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ENERGY_BE.get(), pos, state);
        this.kind = state.getBlock() instanceof EnergyBlock b ? b.kind() : 0;
    }

    public int energy() {
        return energy;
    }

    public int burn() {
        return burn;
    }

    public int generating() {
        return gen;
    }

    public int burnMax() {
        return burnMax;
    }

    public int flow() {
        return flow;
    }

    public ItemStack battery() {
        return battery;
    }

    public void setBattery(ItemStack st) {
        battery = st;
        setChanged();
    }

    public void addBurn(int t) {
        burn += t;
        setChanged();
    }

    /** FE por tick que este bloco gera agora. */
    private int produce() {
        Level lv = level;
        BlockPos pos = getBlockPos();
        switch (kind) {
            case EnergyBlock.COMBUSTION:
                if (burn <= 0 && !battery.isEmpty() && energy + 20 <= BUFFER) {
                    int t = battery.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
                    if (t > 0) {
                        ItemStack rest = battery.getCraftingRemainingItem();
                        battery.shrink(1);
                        if (battery.isEmpty()) battery = rest;
                        burn += t;
                        burnMax = t;
                        setChanged();
                    }
                }
                if (burn > 0 && energy + 20 <= BUFFER) {
                    burn--;
                    return 20;
                }
                return 0;
            case EnergyBlock.SOLAR:
                if (!lv.isDay() || !lv.canSeeSky(pos.above())) return 0;
                return lv.isRaining() ? 4 : 10;
            case EnergyBlock.WIND:
                if (!lv.canSeeSky(pos.above())) return 0;
                int g = 3 + Math.min(17, Math.max(0, pos.getY() - 60) / 5);
                return lv.isRaining() ? g + g / 3 : g;
            case EnergyBlock.THERMAL:
                int n = 0;
                for (Direction d : Direction.values()) {
                    if (lv.getFluidState(pos.relative(d)).is(FluidTags.LAVA)) n++;
                }
                return n * 15;
            default:
                return 0;
        }
    }

    public void serverTick() {
        if (level == null) return;
        int before = gen;
        if (kind != EnergyBlock.CHARGER) {
            gen = produce();
            energy = Math.min(BUFFER, energy + gen);
            if (energy > 0) push();
        } else if (battery.isEmpty() || energy <= 0) {
            flow = 0;
        } else {
            IEnergyStorage st = battery.getCapability(Capabilities.EnergyStorage.ITEM);
            if (st != null) {
                int acc = st.receiveEnergy(energy, false);
                flow = acc;
                if (acc > 0) {
                    energy -= acc;
                    setChanged();
                }
            }
        }
        if (before != gen) setChanged();
    }

    private void push() {
        for (Direction d : Direction.values()) {
            if (energy <= 0) return;
            BlockPos np = getBlockPos().relative(d);
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, np, d.getOpposite());
            if (target == null || !target.canReceive()) continue;
            int sent = target.receiveEnergy(Math.min(energy, PUSH), false);
            energy -= sent;
        }
        setChanged();
    }

    /** Capacidade de energia vista pelos blocos vizinhos e por outros mods. */
    public IEnergyStorage storage() {
        return storage;
    }

    private final IEnergyStorage storage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (kind != EnergyBlock.CHARGER) return 0;
            int n = Math.max(0, Math.min(toReceive, BUFFER - energy));
            if (n > 0 && !simulate) {
                energy += n;
                setChanged();
            }
            return n;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (kind == EnergyBlock.CHARGER) return 0;
            int n = Math.max(0, Math.min(toExtract, energy));
            if (n > 0 && !simulate) {
                energy -= n;
                setChanged();
            }
            return n;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return BUFFER;
        }

        @Override
        public boolean canExtract() {
            return kind != EnergyBlock.CHARGER;
        }

        @Override
        public boolean canReceive() {
            return kind == EnergyBlock.CHARGER;
        }
    };

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("energy", energy);
        tag.putInt("burn", burn);
        tag.putInt("burnMax", burnMax);
        if (!battery.isEmpty()) tag.put("bat", battery.saveOptional(provider));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        energy = Math.max(0, Math.min(BUFFER, tag.getInt("energy")));
        burn = Math.max(0, Math.min(MAX_BURN, tag.getInt("burn")));
        burnMax = Math.max(0, tag.getInt("burnMax"));
        battery = tag.contains("bat") ? ItemStack.parseOptional(provider, tag.getCompound("bat")) : ItemStack.EMPTY;
    }
}
