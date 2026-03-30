package com.yision.fluidlogistics.network;

import com.yision.fluidlogistics.item.PortableStockTickerItem;
import com.yision.fluidlogistics.portableticker.PortableStockTickerMenu;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

public record PortableStockTickerOpenPacket() implements ServerboundPacketPayload {

    public static final PortableStockTickerOpenPacket INSTANCE = new PortableStockTickerOpenPacket();
    public static final StreamCodec<RegistryFriendlyByteBuf, PortableStockTickerOpenPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    public static void send() {
        CatnipServices.NETWORK.sendToServer(INSTANCE);
    }

    @Override
    public void handle(ServerPlayer player) {
        ItemStack stack = PortableStockTickerItem.find(player.getInventory());
        if (stack.isEmpty()) {
            return;
        }

        if (!PortableStockTickerItem.isTuned(stack)) {
            player.displayClientMessage(Component.translatable("item.fluidlogistics.portable_stock_ticker.not_linked"), true);
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new PortableStockTickerMenu(id, inv),
                Component.translatable("item.fluidlogistics.portable_stock_ticker")
        ));
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.PORTABLE_STOCK_TICKER_OPEN;
    }
}
