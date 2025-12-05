package me.minseok.shopsystem.listeners;

import me.minseok.shopsystem.shop.ShopManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final ShopManager shopManager;

    public PlayerJoinListener(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Refresh prices from database when a player joins
        // This ensures that if the server was empty and missed plugin messages,
        // it will now have the latest prices.
        shopManager.refreshPrices();
    }
}
