package com.noloverme.yopayment.config;

import com.noloverme.yopayment.model.DonateItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;

/**
 * Обёртка над donates.yml.
 */
public class DonatesConfig {
    private final Map<String, DonateItem> items = new HashMap<>();

    public DonatesConfig(FileConfiguration config) {
        load(config);
    }

    private void load(FileConfiguration config) {
        items.clear();
        ConfigurationSection donates = config.getConfigurationSection("donates");
        if (donates == null) return;

        for (String itemId : donates.getKeys(false)) {
            ConfigurationSection section = donates.getConfigurationSection(itemId);
            if (section == null) continue;

            String displayName = section.getString("display-name", itemId);
            double price = section.getDouble("price", 0.0);
            String description = section.getString("description");
            List<String> commands = section.getStringList("commands");

            items.put(itemId, new DonateItem(itemId, displayName, price, description, commands));
        }
    }

    public Optional<DonateItem> getItem(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public Set<String> getItemIds() {
        return items.keySet();
    }

    public Collection<DonateItem> getAllItems() {
        return items.values();
    }

    public void reload(FileConfiguration config) {
        load(config);
    }
}
