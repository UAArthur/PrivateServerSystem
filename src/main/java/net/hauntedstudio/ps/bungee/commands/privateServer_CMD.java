package net.hauntedstudio.ps.bungee.commands;

import net.hauntedstudio.ps.bungee.PS;
import net.hauntedstudio.ps.bungee.models.Server;
import net.hauntedstudio.ps.bungee.models.Template;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class privateServer_CMD extends Command implements TabExecutor {

    private final PS plugin;

    public privateServer_CMD(PS plugin, String name) {
        super(name, null, "ps", "privateserver", "privateServer");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "template" -> handleTemplateCommand(player, args);
            case "create" -> handleCreateCommand(player, args);
            case "start" -> handleStartCommand(player, args);
            case  "stop" -> handleStopCommand(player, args);
            case "help" -> sendHelp(player);
            default -> player.sendMessage("§cUnknown action. Use §e/privateServer help §cfor a list of commands.");
        }
    }

    private void sendHelp(ProxiedPlayer player) {
        if (!hasPermission(player, "privatserver.player")) return;

        player.sendMessage(new TextComponent("§8§m--------------------------------------------------"));
        player.sendMessage(new TextComponent("§6§lPrivateServer §8§l» §7Commands"));
        player.sendMessage(new TextComponent("§8§m--------------------------------------------------"));

        sendHelpLine(player, "§6• §e/privateServer create <template> <name> §7- Create a new private server", "/privateServer create ", "Click to suggest this command");
        sendHelpLine(player, "§6• §e/privateServer delete <name> §7- Delete a private server", "/privateServer delete ", "Click to suggest this command");
        sendHelpLine(player, "§6• §e/privateServer list §7- List all your private servers", "/privateServer list", "Click to suggest this command");
        sendHelpLine(player, "§6• §e/privateServer join <name> §7- Join a private server", "/privateServer join ", "Click to suggest this command");
        sendHelpLine(player, "§6• §e/privateServer leave §7- Leave your current private server", "/privateServer leave", "Click to suggest this command");
        sendHelpLine(player, "§6• §e/privateServer info <name> §7- Get info about a private server", "/privateServer info ", "Click to suggest this command");
        sendHelpLine(player, "§6• §e/privateServer template <action> [args] §7- Manage templates", "/privateServer template ", "Click to suggest this command");
        sendHelpLine(player, "§6• §e/privateServer help §7- Show this help message", "/privateServer help", "Click to suggest this command");

        player.sendMessage(new TextComponent("§8§m--------------------------------------------------"));
    }

    private void handleStopCommand(ProxiedPlayer player, String[] args) {
        if (!hasPermission(player, "privatserver.command.stop")) return;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /privateServer stop <name>");
            return;
        }

        String serverName = args[1];
        var server = plugin.getServerManager().getServerByName(player, serverName);
        if (server == null) {
            player.sendMessage("§cPrivate server not found: " + serverName);
            return;
        }

        plugin.getServerManager().stopAllServers();
        player.sendMessage("§aPrivate server " + serverName + " stopped successfully!");
    }

    private void handleCreateCommand(ProxiedPlayer player, String[] args) {
        if (!hasPermission(player, "privatserver.command.create")) return;

        String templateName;
        String serverName;
        if (args.length == 2) {
            templateName = this.plugin.getSettingsManager().getSettings().getDefaultTemplateId();
            serverName = args[1];
        } else if (args.length > 2) {
            templateName = args[1];
            serverName = args[2];
        } else {
            player.sendMessage("§cUsage: /privateServer create <template> <name>");
            return;
        }

        Template template = plugin.getTemplateManager().getTemplateByName(templateName);
        if (template == null) {
            template = plugin.getTemplateManager().getTemplateById(templateName);
        }
        if (template == null) {
            player.sendMessage("§cTemplate not found: " + templateName);
            return;
        }

        player.sendMessage("§eCreating server... §7(This may take ~5 seconds)");
        Template finalTemplate = template;
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            var server = plugin.getServerManager().createServer(player, serverName, finalTemplate);
            if (server != null) {
                player.sendMessage("§eStarting server... §7(This may take ~10 seconds)");

                player.sendMessage("§aPrivate server created and started successfully!");
            } else {
                player.sendMessage("§cFailed to create private server.");
            }
            plugin.getServerManager().startServer(player, serverName);
        });
    }

    private void handleStartCommand(ProxiedPlayer player, String[] args) {
        if (!hasPermission(player, "privatserver.command.start")) return;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /privateServer start <name> or /privateServer start <username> <name>");
            return;
        }

        if (args.length == 2) {
            // Starting own server
            String serverName = args[1];
            plugin.getServerManager().startServer(player, serverName);
        } else {
            // Starting someone else's server
            if (!hasPermission(player, "privatserver.command.start.others")) {
                player.sendMessage("§cYou don't have permission to start other players' servers.");
                return;
            }

            String username = args[1];
            String serverName = args[2];

            UUID targetPlayerUuid = plugin.getServerManager().getUUIDFromUsername(username);
            if (targetPlayerUuid == null) {
                player.sendMessage("§cPlayer not found: " + username);
                return;
            }

            if (!plugin.getServerManager().hasPlayerServer(targetPlayerUuid, serverName)) {
                player.sendMessage("§cServer '" + serverName + "' not found for player " + username);
                return;
            }

            plugin.getServerManager().startServer(player, targetPlayerUuid, serverName);
        }
    }

    private void handleTemplateCommand(ProxiedPlayer player, String[] args) {
        if (!hasPermission(player, "privatserver.command.template")) return;

        if (args.length < 2) {
            sendTemplateHelp(player);
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "list" -> {
                if (!hasPermission(player, "privatserver.command.template.list")) return;

                player.sendMessage("§8§m---------------- §6Templates §8§m----------------");
                plugin.getTemplateManager().getTemplates().forEach(template ->
                        sendHelpLine(player,
                                "§6• §e" + template.getName() + " §8- §7" + template.getDescription(),
                                "/privateserver create " + template.getId(),
                                null)
                );
                player.sendMessage("§8§m--------------------------------------------------");
            }

            case "info" -> {
                if (!hasPermission(player, "privatserver.command.template.info")) return;

                if (args.length < 3) {
                    player.sendMessage("§cUsage: /privateServer template info <name>");
                    return;
                }

                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).replaceAll("^\"|\"$", "");
                var template = plugin.getTemplateManager().getTemplateByName(name);
                if (template == null) template = plugin.getTemplateManager().getTemplateById(name);

                if (template == null) {
                    player.sendMessage("§cTemplate not found: " + name);
                    return;
                }

                player.sendMessage("§8§m---------------- §6Template Info §8§m----------------");
                player.sendMessage("§6ID: §e" + template.getId());
                player.sendMessage("§6Name: §e" + template.getName());
                player.sendMessage("§6Description: §e" + template.getDescription());
                player.sendMessage("§6Version: §e" + template.getVersion());
                player.sendMessage("§6Path: §e" + template.getPath());
                player.sendMessage("§8§m--------------------------------------------------");
            }

            default -> player.sendMessage("§cUnknown template action.");
        }
    }

    private void sendTemplateHelp(ProxiedPlayer player) {
        player.sendMessage("§8§m---------------- §6Template Actions §8§m----------------");
        player.sendMessage("§6• §e/privateServer template list §7- List all templates");
        player.sendMessage("§6• §e/privateServer template create <name> <type> §7- Create a new template");
        player.sendMessage("§6• §e/privateServer template delete <name> §7- Delete a template");
        player.sendMessage("§6• §e/privateServer template info <name> §7- Get info about a template");
        player.sendMessage("§8§m--------------------------------------------------");
    }

    private void sendHelpLine(ProxiedPlayer player, String text, String suggestCommand, String hoverText) {
        TextComponent component = new TextComponent(text);
        if (hoverText != null) {
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
        }
        component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestCommand));
        player.sendMessage(component);
    }

    private boolean hasPermission(ProxiedPlayer player, String permission) {
        if (player.hasPermission("privatserver.admin")) return true;
        if (!player.hasPermission(permission)) {
            player.sendMessage("§cYou don't have permission to do this.");
            return false;
        }
        return true;
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof ProxiedPlayer player)) {
            return completions;
        }

        if (!hasPermission(player, "privatserver.player") && !hasPermission(player, "privatserver.admin")) {
            return completions;
        }

        if (args.length == 1) {
            completions = Arrays.asList("create", "delete", "list", "join", "leave", "info", "template", "help", "start", "stop");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("template") && hasPermission(player, "privatserver.command.template")) {
                completions = Arrays.asList("list", "create", "delete", "info");
            } else if (args[0].equalsIgnoreCase("start")) {
                if (hasPermission(player, "privatserver.command.start.others")) {
                    List<String> finalCompletions = completions;
                    plugin.getProxy().getPlayers().forEach(p -> finalCompletions.add(p.getName()));

                    // Also add the user's own server names
                    Server[] servers = plugin.getServerManager().getPlayerServers(player);
                    for (Server server : servers) {
                        completions.add(server.getName());
                    }
                } else {
                    // Only add the user's own server names
                    Server[] servers = plugin.getServerManager().getPlayerServers(player);
                    for (Server server : servers) {
                        completions.add(server.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("start") && hasPermission(player, "privatserver.command.start.others")) {
                String username = args[1];
                UUID targetPlayerUuid = plugin.getServerManager().getUUIDFromUsername(username);
                if (targetPlayerUuid != null) {
                    File serverDir = new File(plugin.getDataFolder(), "servers/" + targetPlayerUuid);
                    if (serverDir.exists() && serverDir.isDirectory()) {
                        File[] serverDirs = serverDir.listFiles(File::isDirectory);
                        if (serverDirs != null) {
                            for (File dir : serverDirs) {
                                completions.add(dir.getName());
                            }
                        }
                    }
                }
            }
        }

        return completions;
    }
}
