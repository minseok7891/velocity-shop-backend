package me.minseok.shopsystem.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import me.minseok.shopsystem.ShopSystemBackend;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class BackendMessageListener implements PluginMessageListener {

    private final ShopSystemBackend plugin;

    public BackendMessageListener(ShopSystemBackend plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("shopsystem:sync")) {
            return;
        }
        plugin.getLogger().info("DEBUG: Received plugin message on channel " + channel + " from " + player.getName());

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("PRICE_UPDATE")) {
            String item = in.readUTF();
            double buyPrice = in.readDouble();
            double sellPrice = in.readDouble();

            // Check source server to avoid infinite loops/self-updates
            try {
                String sourceServer = in.readUTF();
                String currentServer = plugin.getConfig().getString("server-name", "unknown");
                if (sourceServer.equals(currentServer)) {
                    // Ignore update from self
                    return;
                }
                plugin.getLogger().info("Received price update for " + item + " from " + sourceServer);
            } catch (Exception e) {
                // Legacy message or error reading source server
                plugin.getLogger().info("Received price update for " + item + " (unknown source)");
            }

            plugin.getShopManager().updateItemPrice(item, buyPrice, sellPrice);

        } else if (subChannel.equals("SYNC_REQUEST")) {
            plugin.getLogger().info("Received sync request from Velocity");
            // Request config from Velocity
            plugin.requestSync();
        } else if (subChannel.equals("SEND_CONFIG")) {
            plugin.getLogger().info("Received config data from Velocity");
            int fileCount = in.readInt();

            for (int i = 0; i < fileCount; i++) {
                String fileName = in.readUTF();
                String content = in.readUTF();

                try {
                    java.io.File file = new java.io.File(plugin.getDataFolder(), fileName);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    java.nio.file.Files.writeString(file.toPath(), content);
                    plugin.getLogger().info("Updated file: " + fileName);
                } catch (java.io.IOException e) {
                    plugin.getLogger().severe("Failed to save file: " + fileName);
                    e.printStackTrace();
                }
            }

            plugin.getShopManager().loadShops();
            plugin.getLogger().info("Reloaded shops from synced config");
        }
    }
}
