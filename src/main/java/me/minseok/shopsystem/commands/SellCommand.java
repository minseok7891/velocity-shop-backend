package me.minseok.shopsystem.commands;

import me.minseok.shopsystem.economy.VaultEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SellCommand implements CommandExecutor {

    private final VaultEconomy economy;

    public SellCommand(VaultEconomy economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (handItem == null || handItem.getType() == Material.AIR) {
            player.sendMessage("§c손에 아이템을 들고 있어야 합니다!");
            return true;
        }

        int amount = handItem.getAmount();
        String itemName = handItem.getType().name();

        // Simple sell price calculation (you can customize this)
        double basePrice = 10.0;
        double totalPrice = basePrice * amount;

        // Remove items
        player.getInventory().setItemInMainHand(null);

        // Give money
        EconomyResponse response = economy.depositPlayer(player, totalPrice);

        if (response.transactionSuccess()) {
            player.sendMessage("§a✓ " + itemName + " x" + amount + "을(를) " +
                    economy.format(totalPrice) + "에 판매했습니다!");
            player.sendMessage("§7잔액: " + economy.format(response.balance));
        } else {
            // Refund items if transaction failed
            player.getInventory().setItemInMainHand(handItem);
            player.sendMessage("§c판매 실패: " + response.errorMessage);
        }

        return true;
    }
}
