package org.github.luisera.splitlobby.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.github.luisera.splitlobby.Main;

import java.io.File;
import java.io.IOException;

public class CustomConfig {

    private final Main plugin;
    private final String fileName;
    private File file;
    private FileConfiguration config;

    public CustomConfig(Main plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        createFile();
    }

    public void createFile() {
        file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(fileName, false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}