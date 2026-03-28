package com.yision.fluidlogistics.portableticker;

import com.yision.fluidlogistics.item.PortableStockTickerItem;
import com.yision.fluidlogistics.registry.AllMenuTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PortableStockTickerMenu extends AbstractContainerMenu {

    public final Player player;
    public final Inventory playerInventory;
    public Object screenReference;

    public PortableStockTickerMenu(MenuType<?> type, int id, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(type, id, playerInventory);
    }

    public PortableStockTickerMenu(MenuType<?> type, int id, Inventory playerInventory) {
        super(type, id);
        this.playerInventory = playerInventory;
        this.player = playerInventory.player;
        addPlayerSlots(-1000, 0);
    }

    public PortableStockTickerMenu(int id, Inventory playerInventory) {
        this(AllMenuTypes.PORTABLE_STOCK_TICKER.get(), id, playerInventory);
    }

    private void addPlayerSlots(int x, int y) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot) {
            addSlot(new Slot(playerInventory, hotbarSlot, x + hotbarSlot * 18, y + 58));
        }
    }

    public ItemStack getTickerStack() {
        return PortableStockTickerItem.find(playerInventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return !getTickerStack().isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
