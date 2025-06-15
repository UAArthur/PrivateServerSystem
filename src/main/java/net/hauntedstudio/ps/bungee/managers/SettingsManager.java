package net.hauntedstudio.ps.bungee.managers;

import com.google.gson.Gson;
import lombok.Getter;
import net.hauntedstudio.ps.bungee.PS;
import net.hauntedstudio.ps.bungee.models.Settings;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SettingsManager {
    private final PS plugin;
    @Getter
    private Settings settings;

    public SettingsManager(PS plugin) {
        this.plugin = plugin;

        loadSettings();
    }

    private void loadSettings() {
        File file = new File(plugin.getDataFolder(), "settings.json");
        if (!file.exists()) {
            try (var in = plugin.getClass().getClassLoader().getResourceAsStream("settings.json")) {
                if (in == null) {
                    plugin.getLoger().error("settings.json not found in resources!");
                    return;
                }
                plugin.getDataFolder().mkdirs();
                java.nio.file.Files.copy(in, file.toPath());
                plugin.getLoger().debug("Default settings.json copied from resources.");
            } catch (IOException e) {
                plugin.getLoger().error("Failed to copy default settings.json: " + e.getMessage());
                return;
            }
        }
        try (FileReader reader = new FileReader(file)) {
            this.settings = new Gson().fromJson(reader, Settings.class);
            plugin.getLoger().debug("Settings loaded successfully.");
        } catch (IOException e) {
            plugin.getLoger().error("Failed to load settings.json: " + e.getMessage());
        }
    }

}