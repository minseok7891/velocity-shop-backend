package me.minseok.shopsystem.event;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import me.minseok.shopsystem.ShopCore;
import me.minseok.shopsystem.shop.ShopGUI;
import me.minseok.shopsystem.shop.ShopManager;
import org.bukkit.inventory.Inventory;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 모든 상점 관련 이벤트를 중앙에서 관리하는 매니저
 * 기존의 분산된 리스너들(BackendMessageListener, PlayerEventListener, InventoryListener)을
 * 통합
 */
public class ShopEventManager implements Listener {
    private final ShopCore plugin;
    private final Logger logger;

    public ShopEventManager(ShopCore plugin, ShopGUI shopGUI, ShopManager shopManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        registerListener();
        logger.log(Level.INFO, "ShopEventManager initialized and registered");
    }

    /**
     * 플러그인에 리스너 등록
     */
    private void registerListener() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logger.log(Level.INFO, "ShopEventManager listener registered with PluginManager");
    }

    /**
     * 플레이어 상점 인벤토리 클릭 이벤트 처리
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            Inventory clickedInventory = event.getClickedInventory();
            if (clickedInventory == null)
                return;

            String title = event.getView().getTitle();
            if (!title.contains("§2§l")) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            logger.log(Level.INFO, "Player " + player.getName() + " clicked inventory in shop");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling inventory click event", e);
        }
    }

    /**
     * 플레이어 상점 인벤토리 닫기 이벤트 처리
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        try {
            Inventory inventory = event.getInventory();
            String title = event.getView().getTitle();

            if (title.contains("§2§l") || title.contains("§6§l")) {
                Player player = (Player) event.getPlayer();
                logger.log(Level.INFO, "Player " + player.getName() + " closed shop inventory");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling inventory close event", e);
        }
    }

    /**
     * 플레이어 입장 이벤트 처리
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            Player player = event.getPlayer();
            logger.log(Level.INFO, "Player " + player.getName() + " joined the server");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling player join event", e);
        }
    }

    /**
     * 플레이어 퇴장 이벤트 처리
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            Player player = event.getPlayer();
            logger.log(Level.INFO, "Player " + player.getName() + " left the server");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling player quit event", e);
        }
    }

    /**
     * 리스너 해제
     */
    public void unregister() {
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        logger.log(Level.INFO, "ShopEventManager unregistered from PluginManager");
    }

    /**
     * 이벤트 매니저 상태 확인
     */
    public boolean isActive() {
        return true;
    }
}
