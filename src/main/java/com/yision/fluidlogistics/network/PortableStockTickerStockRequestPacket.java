package com.yision.fluidlogistics.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.yision.fluidlogistics.item.PortableStockTickerItem;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class PortableStockTickerStockRequestPacket implements ServerboundPacketPayload {

    public static final PortableStockTickerStockRequestPacket INSTANCE = new PortableStockTickerStockRequestPacket();
    public static final StreamCodec<RegistryFriendlyByteBuf, PortableStockTickerStockRequestPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    private static final int MAX_ITEMS_PER_PACKET = 100;

    @Override
    public void handle(ServerPlayer player) {
        ItemStack stack = PortableStockTickerItem.find(player.getInventory());
        if (stack.isEmpty()) {
            return;
        }

        UUID freq = PortableStockTickerItem.networkFromStack(stack);
        InventorySummary summary = freq == null ? InventorySummary.EMPTY : LogisticsManager.getSummaryOfNetwork(freq, true);
        List<BigItemStack> entries = summary.getStacksByCount();

        if (entries.isEmpty()) {
            CatnipServices.NETWORK.sendToClient(player, new PortableStockTickerStockResponsePacket(true, Collections.emptyList()));
            return;
        }

        for (int i = 0; i < entries.size(); i += MAX_ITEMS_PER_PACKET) {
            int end = Math.min(i + MAX_ITEMS_PER_PACKET, entries.size());
            List<BigItemStack> chunk = new ArrayList<>(entries.subList(i, end));
            boolean last = end == entries.size();
            CatnipServices.NETWORK.sendToClient(player, new PortableStockTickerStockResponsePacket(last, chunk));
        }
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.PORTABLE_STOCK_TICKER_STOCK_REQUEST;
    }
}
