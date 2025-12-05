package me.minseok.shopsystem.commands;

import me.minseok.shopsystem.economy.VaultEconomy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {

    private final VaultEconomy economy;

    public BalanceCommand(VaultEconomy economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Check own balance
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can check their own balance");
                return true;
            }

            Player player = (Player) sender;
            double balance = economy.getBalance(player);
            sender.sendMessage("§a💰 잔액: §f" + economy.format(balance));

        } else if (args.length == 1) {
            // Check other player's balance
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[0]);
                return true;
            }

            double balance = economy.getBalance(target);
            sender.sendMessage("§a" + target.getName() + "의 잔액: §f" + economy.format(balance));

        } else {
            sender.sendMessage("§c사용법: /balance [플레이어]");
        }

        return true;
    }
}
