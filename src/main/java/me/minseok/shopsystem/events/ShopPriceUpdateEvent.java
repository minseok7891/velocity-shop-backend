package me.minseok.shopsystem.events;

import me.minseok.shopsystem.shop.ShopManager.ShopItem;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ShopPriceUpdateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final ShopItem item;

    public ShopPriceUpdateEvent(ShopItem item) {
        this.item = item;
    }

    public ShopItem getItem() {
        return item;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
