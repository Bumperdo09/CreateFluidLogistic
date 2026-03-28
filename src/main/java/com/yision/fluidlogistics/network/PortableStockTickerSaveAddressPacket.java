package com.yision.fluidlogistics.network;

import com.yision.fluidlogistics.item.PortableStockTickerItem;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record PortableStockTickerSaveAddressPacket(String address) implements ServerboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableStockTickerSaveAddressPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, PortableStockTickerSaveAddressPacket::address,
                    PortableStockTickerSaveAddressPacket::new
            );

    @Override
    public void handle(ServerPlayer player) {
        ItemStack stack = PortableStockTickerItem.find(player.getInventory());
        if (stack.isEmpty()) {
            return;
        }
        PortableStockTickerItem.saveAddressToStack(stack, address);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.PORTABLE_STOCK_TICKER_SAVE_ADDRESS;
    }
}
