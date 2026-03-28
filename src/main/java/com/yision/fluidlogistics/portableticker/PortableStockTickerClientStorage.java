package com.yision.fluidlogistics.portableticker;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.logistics.BigItemStack;
import com.yision.fluidlogistics.network.PortableStockTickerStockRequestPacket;

import net.createmod.catnip.platform.CatnipServices;

public class PortableStockTickerClientStorage {

    private static final int TICKS_BETWEEN_UPDATES = 100;

    private static int ticks;
    private static int version;
    private static List<BigItemStack> stacks = List.of();
    private static List<BigItemStack> collectionBuffer = new ArrayList<>();

    private PortableStockTickerClientStorage() {
    }

    public static void tick() {
        if (++ticks > TICKS_BETWEEN_UPDATES) {
            manualUpdate();
        }
    }

    public static void manualUpdate() {
        ticks = 0;
        collectionBuffer = new ArrayList<>();
        CatnipServices.NETWORK.sendToServer(PortableStockTickerStockRequestPacket.INSTANCE);
    }

    public static void receiveChunk(List<BigItemStack> chunk, boolean last) {
        collectionBuffer.addAll(chunk);
        if (!last) {
            return;
        }

        stacks = List.copyOf(collectionBuffer);
        collectionBuffer = new ArrayList<>();
        version++;
    }

    public static List<BigItemStack> getStacks() {
        return stacks;
    }

    public static int getVersion() {
        return version;
    }
}
