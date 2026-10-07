package com.bismuth.requestergolems;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public final class RequesterJob {
    public enum State {
        WAITING,
        IN_PROGRESS,
        CANCELLED
    }

    private final UUID id;
    private final UUID requestId;
    private final ItemStack stack;
    private final int originalCount;
    private int consecutiveFailures;
    private State state;
    private BlockPos sourceChestPos;

    public RequesterJob(UUID id, UUID requestId, ItemStack stack, State state) {
        this(id, requestId, stack, stack.getCount(), 0, state, null);
    }

    public RequesterJob(UUID id, UUID requestId, ItemStack stack, int originalCount, State state) {
        this(id, requestId, stack, originalCount, 0, state, null);
    }

    public RequesterJob(
            UUID id,
            UUID requestId,
            ItemStack stack,
            int originalCount,
            int consecutiveFailures,
            State state
    ) {
        this(id, requestId, stack, originalCount, consecutiveFailures, state, null);
    }

    public RequesterJob(
            UUID id,
            UUID requestId,
            ItemStack stack,
            int originalCount,
            int consecutiveFailures,
            State state,
            BlockPos sourceChestPos
    ) {
        this.id = id;
        this.requestId = requestId;
        this.stack = stack.copy();
        this.originalCount = Math.max(this.stack.getCount(), originalCount);
        this.consecutiveFailures = Math.max(0, consecutiveFailures);
        this.state = state;
        this.sourceChestPos = sourceChestPos == null ? null : sourceChestPos.immutable();
    }

    public static RequesterJob create(UUID requestId, ItemStack stack) {
        return new RequesterJob(UUID.randomUUID(), requestId, stack, State.WAITING);
    }

    public UUID id() {
        return id;
    }

    public UUID requestId() {
        return requestId;
    }

    public ItemStack stack() {
        return stack;
    }

    public int originalCount() {
        return originalCount;
    }

    public int deliveredCount() {
        return originalCount - stack.getCount();
    }

    public State state() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public BlockPos sourceChestPos() {
        return sourceChestPos;
    }

    public void setSourceChestPos(BlockPos sourceChestPos) {
        this.sourceChestPos = sourceChestPos == null ? null : sourceChestPos.immutable();
    }

    public boolean isComplete() {
        return stack.isEmpty();
    }

    public int consecutiveFailures() {
        return consecutiveFailures;
    }

    public int recordConsecutiveFailure() {
        this.consecutiveFailures++;
        return this.consecutiveFailures;
    }

    public void resetConsecutiveFailures() {
        this.consecutiveFailures = 0;
    }

    public RequesterJob copy() {
        return new RequesterJob(
                this.id,
                this.requestId,
                this.stack,
                this.originalCount,
                this.consecutiveFailures,
                this.state,
                this.sourceChestPos
        );
    }
}
