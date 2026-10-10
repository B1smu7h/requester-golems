package com.bismuth.requestergolems;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World-persistent allocator for the public sequential IDs shown by Requester Golems.
 *
 * <p>The allocator lives in overworld saved data so every dimension shares one
 * sequence. IDs are allocated only on the logical server and are never reused.</p>
 */
public final class RequesterGolemIdData extends SavedData {
    private static final Codec<RequesterGolemIdData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("next_id", 1)
                            .forGetter(data -> data.nextId)
            ).apply(instance, RequesterGolemIdData::new)
    );

    private static final SavedDataType<RequesterGolemIdData> TYPE =
            new SavedDataType<>(RequesterGolems.id("requester_golem_ids"),
                    RequesterGolemIdData::new, CODEC);

    private int nextId;

    public RequesterGolemIdData() {
        this(1);
    }

    private RequesterGolemIdData(int nextId) {
        this.nextId = Math.max(1, nextId);
    }

    public static int allocate(ServerLevel level) {
        RequesterGolemIdData data = level.getServer().overworld()
                .getDataStorage().computeIfAbsent(TYPE);
        int allocated = data.nextId;
        if (allocated < Integer.MAX_VALUE) {
            data.nextId = allocated + 1;
        }
        data.setDirty();
        return allocated;
    }
}
