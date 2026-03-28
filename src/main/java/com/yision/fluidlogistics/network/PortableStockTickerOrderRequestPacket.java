package com.yision.fluidlogistics.network;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.WiFiEffectPacket;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.yision.fluidlogistics.item.PortableStockTickerItem;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record PortableStockTickerOrderRequestPacket(PackageOrderWithCrafts order, String address) implements ServerboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableStockTickerOrderRequestPacket> STREAM_CODEC =
            StreamCodec.composite(
                    PackageOrderWithCrafts.STREAM_CODEC, PortableStockTickerOrderRequestPacket::order,
                    ByteBufCodecs.STRING_UTF8, PortableStockTickerOrderRequestPacket::address,
                    PortableStockTickerOrderRequestPacket::new
            );

    @Override
    public void handle(ServerPlayer player) {
        if (player.isSpectator() || AdventureUtil.isAdventure(player)) {
            return;
        }

        ItemStack stack = PortableStockTickerItem.find(player.getInventory());
        if (stack.isEmpty()) {
            return;
        }

        if (!order.isEmpty()) {
            AllSoundEvents.STOCK_TICKER_REQUEST.playOnServer(player.level(), player.blockPosition());
            AllAdvancements.STOCK_TICKER.awardTo(player);
            WiFiEffectPacket.send(player.level(), player.blockPosition());
        }

        ((PortableStockTickerItem) stack.getItem()).broadcastPackageRequest(stack,
                LogisticallyLinkedBehaviour.RequestType.PLAYER, order, address, player);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.PORTABLE_STOCK_TICKER_ORDER_REQUEST;
    }
}
