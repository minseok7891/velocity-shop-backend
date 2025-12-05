package me.minseok.shopsystem.commands;

import me.minseok.shopsystem.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShopGiveCommand implements CommandExecutor, TabCompleter {

    private final ShopManager shopManager;

    public ShopGiveCommand(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("shopsystem.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c사용법: /shopgive <section> <item_id> [player] [amount]");
            return true;
        }

        String sectionId = args[0].toLowerCase();
        String itemId = args[1].toUpperCase();

        ShopManager.ShopCategory category = shopManager.getCategory(sectionId);
        if (category == null) {
            sender.sendMessage("§c카테고리를 찾을 수 없습니다: " + sectionId);
            return true;
        }

        // Try to find by ID first
        ShopManager.ShopItem shopItem = shopManager.getItemById(itemId);

        // Fallback to Material if not found
        if (shopItem == null) {
            Material material = Material.getMaterial(itemId);
            if (material != null) {
                shopItem = shopManager.getItemByMaterial(material);
            }
        }

        if (shopItem == null) {
            sender.sendMessage("§c상점에서 해당 아이템을 찾을 수 없습니다: " + itemId);
            return true;
        }

        // Determine target player
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[2]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c플레이어 이름을 명시해야 합니다.");
                return true;
            }
            target = (Player) sender;
        }

        // Determine amount
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage("§c수량은 1-64 사이여야 합니다.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c잘못된 숫자 형식입니다.");
                return true;
            }
        }

        // Create and give item
        ItemStack item = new ItemStack(shopItem.getMaterial(), amount);

        // Apply enchantments if any
        if (!shopItem.getEnchantments().isEmpty()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
                org.bukkit.inventory.meta.EnchantmentStorageMeta esm = (org.bukkit.inventory.meta.EnchantmentStorageMeta) meta;
                shopItem.getEnchantments().forEach((ench, level) -> {
                    esm.addStoredEnchant(ench, level, true);
                });
            } else {
                shopItem.getEnchantments().forEach((ench, level) -> {
                    meta.addEnchant(ench, level, true);
                });
            }
            item.setItemMeta(meta);
        }

        target.getInventory().addItem(item);

        sender.sendMessage("§a✓ " + target.getName() + "에게 " + shopItem.getId() + " x" + amount + "을(를) 지급했습니다!");
        if (!sender.equals(target)) {
            target.sendMessage("§a✓ " + shopItem.getId() + " x" + amount + "을(를) 받았습니다!");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Section names
            for (ShopManager.ShopCategory category : shopManager.getCategories().values()) {
                completions.add(category.getId());
            }
        } else if (args.length == 2) {
            // Materials
            for (Material material : Material.values()) {
                if (material.isItem()) {
                    completions.add(material.name().toLowerCase());
                }
            }
        } else if (args.length == 3) {
            // Online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        return completions;
    }
}
