package org.github.luisera.splitlobby.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.github.luisera.splitlobby.Main;
import org.github.luisera.splitlobby.npc.NPCData;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class NPCCommand implements CommandExecutor {

    private final Main plugin;

    public NPCCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("splitlobby.admin")) {
            player.sendMessage("§cVocê não tem permissão!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
            case "criar":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /slnpc create <nome>");
                    return true;
                }
                createNPC(player, args[1]);
                break;

            case "delete":
            case "deletar":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /slnpc delete <id>");
                    return true;
                }
                deleteNPC(player, args[1]);
                break;

            case "setskin":
                if (args.length < 3) {
                    player.sendMessage("§cUso: /slnpc setskin <id> <nick>");
                    return true;
                }
                setSkin(player, args[1], args[2]);
                break;

            case "setcmd":
            case "setcommand":
                if (args.length < 3) {
                    player.sendMessage("§cUso: /slnpc setcmd <id> <comando>");
                    return true;
                }
                setCommand(player, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                break;

            case "setdesc":
            case "setdescription":
                if (args.length < 3) {
                    player.sendMessage("§cUso: /slnpc setdesc <id> <linha1> | <linha2> | ...");
                    player.sendMessage("§7Exemplo: /slnpc setdesc 1 &bBedwars | &7Clique com direito");
                    return true;
                }
                setDescription(player, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                break;

            case "list":
            case "lista":
                listNPCs(player);
                break;

            case "tp":
            case "teleport":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /slnpc tp <id>");
                    return true;
                }
                teleportToNPC(player, args[1]);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void createNPC(Player player, String name) {
        NPCData npc = plugin.getNPCManager().createNPC(name, player.getLocation());

        player.sendMessage("");
        player.sendMessage("§a§l✓ NPC CRIADO!");
        player.sendMessage("§e  ID: §f#" + npc.getId());
        player.sendMessage("§e  Nome: §f" + npc.getName());
        player.sendMessage("§e  Localização: §f" + formatLocation(npc.getLocation()));
        player.sendMessage("");
        player.sendMessage("§7Use §e/slnpc setskin " + npc.getId() + " <nick> §7para definir a skin");
        player.sendMessage("");
    }

    private void deleteNPC(Player player, String idStr) {
        try {
            int id = Integer.parseInt(idStr);
            NPCData npc = plugin.getNPCManager().getNPC(id);

            if (npc == null) {
                player.sendMessage("§cNPC #" + id + " não encontrado!");
                return;
            }

            plugin.getNPCManager().deleteNPC(id);

            player.sendMessage("");
            player.sendMessage("§c§l✗ NPC DELETADO!");
            player.sendMessage("§e  ID: §f#" + id);
            player.sendMessage("§e  Nome: §f" + npc.getName());
            player.sendMessage("");

        } catch (NumberFormatException e) {
            player.sendMessage("§cID inválido! Use um número.");
        }
    }

    private void setSkin(Player player, String idStr, String skinName) {
        try {
            int id = Integer.parseInt(idStr);
            NPCData npc = plugin.getNPCManager().getNPC(id);

            if (npc == null) {
                player.sendMessage("§cNPC #" + id + " não encontrado!");
                return;
            }

            player.sendMessage("");
            player.sendMessage("§e§l⌛ APLICANDO SKIN...");
            player.sendMessage("§7Buscando skin de: §f" + skinName);
            player.sendMessage("§7Aguarde...");
            player.sendMessage("");

            plugin.getNPCManager().setSkin(id, skinName);

            // Feedback após 2 segundos
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("");
                player.sendMessage("§a§l✓ SKIN APLICADA!");
                player.sendMessage("§e  NPC: §f" + npc.getName() + " §7(#" + id + ")");
                player.sendMessage("§e  Skin: §f" + skinName);
                player.sendMessage("");
            }, 40L);

        } catch (NumberFormatException e) {
            player.sendMessage("§cID inválido! Use um número.");
        }
    }

    private void setCommand(Player player, String idStr, String command) {
        try {
            int id = Integer.parseInt(idStr);
            NPCData npc = plugin.getNPCManager().getNPC(id);

            if (npc == null) {
                player.sendMessage("§cNPC #" + id + " não encontrado!");
                return;
            }

            plugin.getNPCManager().setCommand(id, command);

            player.sendMessage("");
            player.sendMessage("§a§l✓ COMANDO DEFINIDO!");
            player.sendMessage("§e  NPC: §f" + npc.getName() + " §7(#" + id + ")");
            player.sendMessage("§e  Comando: §f" + command);
            player.sendMessage("");
            player.sendMessage("§7Variáveis disponíveis:");
            player.sendMessage("§7  {player} §f- Nome do jogador");
            player.sendMessage("§7  {uuid} §f- UUID do jogador");
            player.sendMessage("§7Use [CONSOLE] no início para executar como console");
            player.sendMessage("");

        } catch (NumberFormatException e) {
            player.sendMessage("§cID inválido! Use um número.");
        }
    }

    private void setDescription(Player player, String idStr, String description) {
        try {
            int id = Integer.parseInt(idStr);
            NPCData npc = plugin.getNPCManager().getNPC(id);

            if (npc == null) {
                player.sendMessage("§cNPC #" + id + " não encontrado!");
                return;
            }

            List<String> lines = Arrays.asList(description.split("\\|"));
            plugin.getNPCManager().setDescription(id, lines);

            player.sendMessage("");
            player.sendMessage("§a§l✓ DESCRIÇÃO DEFINIDA!");
            player.sendMessage("§e  NPC: §f" + npc.getName() + " §7(#" + id + ")");
            player.sendMessage("§e  Linhas:");
            for (String line : lines) {
                player.sendMessage("    " + org.bukkit.ChatColor.translateAlternateColorCodes('&', line.trim()));
            }
            player.sendMessage("");

        } catch (NumberFormatException e) {
            player.sendMessage("§cID inválido! Use um número.");
        }
    }

    private void listNPCs(Player player) {
        Collection<NPCData> npcs = plugin.getNPCManager().getAllNPCs();

        if (npcs.isEmpty()) {
            player.sendMessage("§cNenhum NPC foi criado ainda!");
            return;
        }

        player.sendMessage("");
        player.sendMessage("§6§l⚑ NPCS DO LOBBY §7(" + npcs.size() + ")");
        player.sendMessage("");

        for (NPCData npc : npcs) {
            player.sendMessage("§e  #" + npc.getId() + " §f" + npc.getName());
            player.sendMessage("    §7Skin: §f" + (npc.getSkinName() != null ? npc.getSkinName() : "Nenhuma"));
            player.sendMessage("    §7Comando: §f" + (npc.getCommand() != null ? npc.getCommand() : "Nenhum"));
            player.sendMessage("    §7Local: §f" + formatLocation(npc.getLocation()));
            player.sendMessage("");
        }
    }

    private void teleportToNPC(Player player, String idStr) {
        try {
            int id = Integer.parseInt(idStr);
            NPCData npc = plugin.getNPCManager().getNPC(id);

            if (npc == null) {
                player.sendMessage("§cNPC #" + id + " não encontrado!");
                return;
            }

            player.teleport(npc.getLocation());
            player.sendMessage("§a§l✓ §aTeleportado para o NPC: §f" + npc.getName());

        } catch (NumberFormatException e) {
            player.sendMessage("§cID inválido! Use um número.");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l⚑ SISTEMA DE NPCS");
        player.sendMessage("");
        player.sendMessage("§e  /slnpc create <nome> §7- Cria um NPC");
        player.sendMessage("§e  /slnpc delete <id> §7- Deleta um NPC");
        player.sendMessage("§e  /slnpc setskin <id> <nick> §7- Define a skin");
        player.sendMessage("§e  /slnpc setcmd <id> <comando> §7- Define comando");
        player.sendMessage("§e  /slnpc setdesc <id> <desc> §7- Define descrição");
        player.sendMessage("§e  /slnpc list §7- Lista todos os NPCs");
        player.sendMessage("§e  /slnpc tp <id> §7- Teleporta para um NPC");
        player.sendMessage("");
    }

    private String formatLocation(org.bukkit.Location loc) {
        return String.format("%s (%.1f, %.1f, %.1f)",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}