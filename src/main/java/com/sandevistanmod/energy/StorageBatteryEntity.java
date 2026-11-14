package com.sandevistanmod.energy;

import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Guarda ate 10.000 FE. Recebe dos geradores e cabos (ate 400 FE/t) e entrega ate 100 FE/t aos carregadores. */
public class StorageBatteryEntity extends BlockEntity {
    public static final int CAP = 10000;
    private static final int IN = 400;
    private static final int OUT = 100;
    private int energy;

    public StorageBatteryEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.STORAGE_BE.get(), pos, state);
    }

    public int energy() {
        return energy;
    }

    public void serverTick() {
        if (level == null || energy <= 0) return;
        int left = Math.min(energy, OUT);
        CableBlock.fromBattery = true;
        try {
            for (Direction d : Direction.values()) {
                if (left <= 0) break;
                BlockPos np = worldPosition.relative(d);
                if (!level.isLoaded(np) || level.getBlockState(np).getBlock() instanceof StorageBatteryBlock) continue;
                IEnergyStorage t = level.getCapability(Capabilities.EnergyStorage.BLOCK, np, d.getOpposite());
                if (t == null || !t.canReceive()) continue;
                left -= t.receiveEnergy(left, false);
            }
        } finally {
            CableBlock.fromBattery = false;
        }
        int sent = Math.min(energy, OUT) - left;
        if (sent > 0) {
            energy -= sent;
            setChanged();
        }
    }

    public final IEnergyStorage storage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int n = Math.max(0, Math.min(Math.min(toReceive, IN), CAP - energy));
            if (n > 0 && !simulate) {
                energy += n;
                setChanged();
            }
            return n;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return CAP;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("energy", energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        energy = Math.max(0, Math.min(CAP, tag.getInt("energy")));
    }
}
