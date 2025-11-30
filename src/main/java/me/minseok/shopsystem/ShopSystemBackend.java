package me.minseok.shopsystem;

import me.minseok.shopsystem.database.DatabaseManager;
import me.minseok.shopsystem.economy.VaultEconomy;
import me.minseok.shopsystem.commands.*;
import me.minseok.shopsystem.shop.ShopGUI;
import me.minseok.shopsystem.shop.ShopManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ShopSystemBackend extends JavaPlugin {

    private DatabaseManager database;
    private VaultEconomy economy;
    private ShopManager shopManager;
    private ShopGUI shopGUI;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        getLogger().info("Server Name: " + getConfig().getString("server-name", "unknown"));

        // Initialize database
        FileConfiguration config = getConfig();
        database = new DatabaseManager(
                config.getString("database.host", "localhost"),
                config.getInt("database.port", 3306),
                config.getString("database.database", "minecraft"),
                config.getString("database.username", "root"),
                config.getString("database.password", ""),
                getLogger());

        database.initialize();

        // Initialize economy
        economy = new VaultEconomy(database, getLogger());

        // Register Vault economy provider
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            getServer().getServicesManager().register(
                    Economy.class,
                    economy,
                    this,
                    ServicePriority.Highest);
            getLogger().info("Registered Vault economy provider");
        } else {
            getLogger().warning("Vault not found! Economy features may not work");
        }

        // Initialize shop system
        shopManager = new ShopManager(getDataFolder(), getLogger(), database, this);
        shopManager.loadConfig(getConfig());
        shopManager.loadShops();

        // Register messaging
        getServer().getMessenger().registerIncomingPluginChannel(this, "shopsystem:sync",
                new me.minseok.shopsystem.messaging.BackendMessageListener(this));
        getServer().getMessenger().registerOutgoingPluginChannel(this, "shopsystem:sync");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Register economy commands
        getCommand("balance").setExecutor(new BalanceCommand(economy));
        getCommand("pay").setExecutor(new PayCommand(economy));
        getCommand("baltop").setExecutor(new BaltopCommand(database));
        getCommand("eco").setExecutor(new EcoCommand(economy));

        shopGUI = new ShopGUI(shopManager, economy);
        ShopCommand shopCmd = new ShopCommand(shopGUI, shopManager);
        getCommand("shop").setExecutor(shopCmd);
        getCommand("shop").setTabCompleter(shopCmd);

        // Register listeners
        getServer().getPluginManager().registerEvents(shopGUI, this);
        getServer().getPluginManager()
                .registerEvents(new me.minseok.shopsystem.listeners.PlayerJoinListener(shopManager), this);

        // Schedule auto-refresh task
        int refreshInterval = getConfig().getInt("dynamic-pricing.auto-refresh-interval", 10);
        if (refreshInterval > 0) {
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                shopManager.refreshPrices();
            }, 20L * refreshInterval, 20L * refreshInterval);
            getLogger().info("Auto-refresh task scheduled every " + refreshInterval + " seconds.");
        }

        getCommand("sell").setExecutor(new SellCommand(economy));

        SellAllCommand sellAllCmd = new SellAllCommand(economy, shopManager);
        SellGUICommand sellGUICmd = new SellGUICommand(economy, shopManager);
        getCommand("sellall").setExecutor(sellAllCmd);
        getCommand("sellall").setTabCompleter(sellAllCmd);
        getCommand("sellgui").setExecutor(sellGUICmd);

        // Admin commands
        EShopCommand eshopCmd = new EShopCommand(shopManager, new File(getDataFolder(), "shops"));
        ShopGiveCommand shopGiveCmd = new ShopGiveCommand(shopManager);
        getCommand("eshop").setExecutor(eshopCmd);
        getCommand("eshop").setTabCompleter(eshopCmd);
        getCommand("shopgive").setExecutor(shopGiveCmd);
        getCommand("shopgive").setTabCompleter(shopGiveCmd);

        // Register listeners
        getServer().getPluginManager().registerEvents(sellGUICmd, this);

        // Register sync listener and force refresh on join
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                // Force refresh prices on join
                getServer().getScheduler().runTaskAsynchronously(ShopSystemBackend.this, () -> {
                    shopManager.refreshPrices();
                });

                if (getServer().getOnlinePlayers().size() == 1) {
                    sendSyncRequest(event.getPlayer());
                }
            }
        }, this);

        getLogger().info("ShopSystemBackend enabled successfully");
    }

    public void requestSync() {
        if (getServer().getOnlinePlayers().isEmpty()) {
            return;
        }

        org.bukkit.entity.Player player = getServer().getOnlinePlayers().iterator().next();
        sendSyncRequest(player);
    }

    private void sendSyncRequest(org.bukkit.entity.Player player) {
        com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
        out.writeUTF("REQUEST_CONFIG");
        player.sendPluginMessage(this, "shopsystem:sync", out.toByteArray());
        getLogger().info("Sent config request to Velocity via " + player.getName());
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("ShopSystemBackend disabled");
    }

    // ... existing methods ...

    public DatabaseManager getDatabase() {
        return database;
    }

    public VaultEconomy getEconomy() {
        return economy;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }
}
