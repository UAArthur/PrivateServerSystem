package net.hauntedstudio.ps.bungee.managers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.hauntedstudio.ps.bungee.PS;
import net.hauntedstudio.ps.bungee.models.Template;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class TemplateManager {
    private final PS plugin;
    private final List<Template> templates = new ArrayList<>();
    private final Gson gson = new Gson();

    public TemplateManager(PS plugin) {
        this.plugin = plugin;
        this.plugin.getLoger().debug("TemplateManager initialized.");

        File templatesDir = new File(plugin.getDataFolder(), "templates");
        if (!templatesDir.exists()) {
            if (templatesDir.mkdirs()) {
                plugin.getLoger().debug("Template directory created successfully.");
            } else {
                plugin.getLoger().error("Failed to create template directory.");
            }
        }

        loadTemplates();
    }

    private void loadTemplates() {
        File templatesDir = new File(plugin.getDataFolder(), "templates");
        if (!templatesDir.exists() || !templatesDir.isDirectory()) {
            plugin.getLoger().error("Templates directory does not exist or is not a directory.");
            return;
        }

        File[] files = templatesDir.listFiles();
        if (files == null || files.length == 0) {
            createDefaultTemplate();
            plugin.getLoger().debug("No templates found in the directory.");
            return;
        }

        for (File dir : files) {
            if (dir.isDirectory()) {
                File templateFile = new File(dir, "template.json");
                if (templateFile.exists() && templateFile.isFile()) {
                    try (Reader reader = new FileReader(templateFile)) {
                        Template template = gson.fromJson(reader, Template.class);
                        if (template != null) {
                            template.setPath(dir.getAbsolutePath());
                            template.setServerFile(new File(dir, "spigot.jar"));
                            template.setServerPropertiesFile(new File(dir, "server.properties"));
                            templates.add(template);
                            plugin.getLoger().debug("Loaded template: " + template.getName());
                        } else {
                            plugin.getLoger().error("Template file was empty or invalid: " + templateFile.getName());
                        }
                    } catch (JsonSyntaxException | IOException e) {
                        plugin.getLoger().error("Failed to load template: " + templateFile.getName() + " - " + e.getMessage());
                    }
                }
            }
        }
    }

    private void createDefaultTemplate() {
        File defaultDir = new File(plugin.getDataFolder(), "templates/default");
        File placeholderFile = new File(defaultDir, "ADD-SPIGOT-JAR-HERE");
        File templateJson = new File(defaultDir, "template.json");

        if (!defaultDir.exists()) {
            if (defaultDir.mkdirs()) {
                plugin.getLoger().debug("Default template directory created successfully.");
                try {
                    placeholderFile.createNewFile();
                    try (InputStream in = plugin.getClass().getClassLoader().getResourceAsStream("template.json")) {
                        if (in == null) {
                            plugin.getLoger().error("Resource template.json not found in JAR.");
                        } else {
                            Files.copy(
                                    in,
                                    templateJson.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING
                            );
                            plugin.getLoger().debug("template.json copied to default template directory.");
                        }
                    }
                } catch (IOException e) {
                    plugin.getLoger().error("Error creating default template: " + e.getMessage());
                }
            } else {
                plugin.getLoger()
                        .error("Failed to create default template directory.");
            }
        }
    }

    public List<Template> getTemplates() {
        return new ArrayList<>(templates);
    }

    public Template getTemplateById(String id) {
        return templates.stream()
                .filter(template -> template.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    public Template getTemplateByName(String name) {
        return templates.stream()
                .filter(template -> template.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
