package me.minseok.shopsystem.discount;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class DiscountManager {

    private final Map<String, DiscountTier> discountTiers;

    public DiscountManager() {
        this.discountTiers = new HashMap<>();
    }

    public void loadDiscounts(FileConfiguration config) {
        discountTiers.clear();

        if (!config.contains("discounts")) {
            return;
        }

        for (String tierName : config.getConfigurationSection("discounts").getKeys(false)) {
            String permission = config.getString("discounts." + tierName + ".permission");
            double buyDiscount = config.getDouble("discounts." + tierName + ".buy_discount", 0);
            double sellMultiplier = config.getDouble("discounts." + tierName + ".sell_multiplier", 1.0);
            int priority = config.getInt("discounts." + tierName + ".priority", 0);

            discountTiers.put(tierName, new DiscountTier(tierName, permission, buyDiscount, sellMultiplier, priority));
        }
    }

    public double getBuyDiscount(Player player) {
        DiscountTier bestTier = getBestTier(player);
        return bestTier != null ? bestTier.getBuyDiscount() : 0;
    }

    public double getSellMultiplier(Player player) {
        DiscountTier bestTier = getBestTier(player);
        return bestTier != null ? bestTier.getSellMultiplier() : 1.0;
    }

    private DiscountTier getBestTier(Player player) {
        DiscountTier best = null;

        for (DiscountTier tier : discountTiers.values()) {
            if (player.hasPermission(tier.getPermission())) {
                if (best == null || tier.getPriority() > best.getPriority()) {
                    best = tier;
                }
            }
        }

        return best;
    }

    public static class DiscountTier {
        private final String name;
        private final String permission;
        private final double buyDiscount;
        private final double sellMultiplier;
        private final int priority;

        public DiscountTier(String name, String permission, double buyDiscount, double sellMultiplier, int priority) {
            this.name = name;
            this.permission = permission;
            this.buyDiscount = buyDiscount;
            this.sellMultiplier = sellMultiplier;
            this.priority = priority;
        }

        public String getName() {
            return name;
        }

        public String getPermission() {
            return permission;
        }

        public double getBuyDiscount() {
            return buyDiscount;
        }

        public double getSellMultiplier() {
            return sellMultiplier;
        }

        public int getPriority() {
            return priority;
        }
    }
}
