package com.yision.fluidlogistics.network;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.yision.fluidlogistics.item.PortableStockTickerItem;

import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record PortableStockTickerHiddenCategoriesPacket(List<Integer> indices) implements ServerboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableStockTickerHiddenCategoriesPacket> STREAM_CODEC =
            StreamCodec.composite(
                    CatnipStreamCodecBuilders.list(ByteBufCodecs.INT), PortableStockTickerHiddenCategoriesPacket::indices,
                    PortableStockTickerHiddenCategoriesPacket::new
            );

    @Override
    public void handle(ServerPlayer player) {
        ItemStack stack = PortableStockTickerItem.find(player.getInventory());
        if (stack.isEmpty()) {
            return;
        }

        Map<UUID, List<Integer>> hidden = PortableStockTickerItem.loadHiddenCategoriesFromStack(stack);
        if (indices.isEmpty()) {
            hidden.remove(player.getUUID());
        } else {
            hidden.put(player.getUUID(), List.copyOf(indices));
        }
        PortableStockTickerItem.saveHiddenCategoriesToStack(stack, hidden);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.PORTABLE_STOCK_TICKER_HIDDEN_CATEGORIES;
    }
}
