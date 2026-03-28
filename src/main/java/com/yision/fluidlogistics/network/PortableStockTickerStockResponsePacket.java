package com.yision.fluidlogistics.network;

import java.util.List;

import com.simibubi.create.content.logistics.BigItemStack;
import com.yision.fluidlogistics.portableticker.PortableStockTickerClientStorage;

import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record PortableStockTickerStockResponsePacket(boolean lastPacket, List<BigItemStack> items)
        implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableStockTickerStockResponsePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, PortableStockTickerStockResponsePacket::lastPacket,
                    CatnipStreamCodecBuilders.list(BigItemStack.STREAM_CODEC), PortableStockTickerStockResponsePacket::items,
                    PortableStockTickerStockResponsePacket::new
            );

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        PortableStockTickerClientStorage.receiveChunk(items, lastPacket);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.PORTABLE_STOCK_TICKER_STOCK_RESPONSE;
    }
}
