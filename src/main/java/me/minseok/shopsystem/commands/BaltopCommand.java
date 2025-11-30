package me.minseok.shopsystem.commands;

import me.minseok.shopsystem.database.DatabaseManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BaltopCommand implements CommandExecutor {

    private final DatabaseManager database;
    private static final int PER_PAGE = 10;

    public BaltopCommand(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int page = 1;

        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1)
                    page = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage("§c유효하지 않은 페이지 번호입니다");
                return true;
            }
        }

        List<BalanceEntry> entries = getTopBalances(page);
        if (entries.isEmpty()) {
            sender.sendMessage("§c데이터가 없습니다");
            return true;
        }

        sender.sendMessage("§e§l=== 💰 부자 순위 (" + page + "페이지) ===");

        int rank = (page - 1) * PER_PAGE + 1;
        for (BalanceEntry entry : entries) {
            String medal = getRankMedal(rank);
            sender.sendMessage(String.format("§a%d. %s%s §f- §e%.2f원",
                    rank, medal, entry.playerName, entry.balance));
            rank++;
        }

        return true;
    }

    private String getRankMedal(int rank) {
        return switch (rank) {
            case 1 -> "🥇 ";
            case 2 -> "🥈 ";
            case 3 -> "🥉 ";
            default -> "";
        };
    }

    private List<BalanceEntry> getTopBalances(int page) {
        List<BalanceEntry> entries = new ArrayList<>();
        int offset = (page - 1) * PER_PAGE;

        String sql = """
                    SELECT b.uuid, b.balance, p.name
                    FROM player_balances b
                    LEFT JOIN (
                        SELECT DISTINCT uuid, name
                        FROM transactions
                        WHERE name IS NOT NULL
                    ) p ON b.uuid = p.uuid
                    ORDER BY b.balance DESC
                    LIMIT ? OFFSET ?
                """;

        try (Connection conn = database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, PER_PAGE);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    double balance = rs.getDouble("balance");
                    String name = rs.getString("name");

                    if (name == null) {
                        name = uuid.substring(0, 8); // Fallback to UUID prefix
                    }

                    entries.add(new BalanceEntry(name, balance));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return entries;
    }

    private static class BalanceEntry {
        String playerName;
        double balance;

        BalanceEntry(String playerName, double balance) {
            this.playerName = playerName;
            this.balance = balance;
        }
    }
}
