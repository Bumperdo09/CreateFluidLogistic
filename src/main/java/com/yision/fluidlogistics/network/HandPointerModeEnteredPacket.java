package com.yision.fluidlogistics.network;

import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.advancement.AllTriggers;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public record HandPointerModeEnteredPacket() implements ServerboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, HandPointerModeEnteredPacket> STREAM_CODEC =
            StreamCodec.of(
                    HandPointerModeEnteredPacket::encode,
                    HandPointerModeEnteredPacket::decode
            );

    private static void encode(RegistryFriendlyByteBuf buf, HandPointerModeEnteredPacket packet) {
    }

    private static HandPointerModeEnteredPacket decode(RegistryFriendlyByteBuf buf) {
        return new HandPointerModeEnteredPacket();
    }

    public static void send() {
        CatnipServices.NETWORK.sendToServer(new HandPointerModeEnteredPacket());
    }

    @Override
    public void handle(ServerPlayer player) {
        AllTriggers.HAND_POINTER_MODE.trigger(player);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.HAND_POINTER_MODE_ENTERED;
    }
}
