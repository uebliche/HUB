package net.uebliche.hub.fabric.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HubPayload(byte[] data) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("uebliche", "hub");
    public static final Type<HubPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, HubPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.data.length);
                buf.writeBytes(payload.data);
            },
            buf -> {
                int len = buf.readVarInt();
                byte[] data = new byte[len];
                buf.readBytes(data);
                return new HubPayload(data);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
