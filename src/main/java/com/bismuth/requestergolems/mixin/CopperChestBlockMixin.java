package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CopperChestBlock.class)
public abstract class CopperChestBlockMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void requestergolems$preventRequesterPairing(
            BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState placedState = cir.getReturnValue();
        if (placedState == null || placedState.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return;
        }

        BlockPos connectedPos = ChestBlock.getConnectedBlockPos(context.getClickedPos(), placedState);
        if (requestergolems$isRequester(context.getLevel(), connectedPos)) {
            cir.setReturnValue(placedState.setValue(ChestBlock.TYPE, ChestType.SINGLE));
        }
    }

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void requestergolems$preventRequesterPairingAfterNeighborUpdate(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            net.minecraft.core.Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random,
            CallbackInfoReturnable<BlockState> cir
    ) {
        if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            if (requestergolems$isRequester(level, pos)
                    && cir.getReturnValue().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                cir.setReturnValue(cir.getReturnValue().setValue(ChestBlock.TYPE, ChestType.SINGLE));
            }
            return;
        }

        if (requestergolems$isRequester(level, pos)
                || requestergolems$isRequester(level, neighbourPos)) {
            cir.setReturnValue(cir.getReturnValue().setValue(ChestBlock.TYPE, ChestType.SINGLE));
        }
    }

    private static boolean requestergolems$isRequester(LevelReader level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof RequesterChestAccess requester
                && requester.requestergolems$isRequester();
    }
}
