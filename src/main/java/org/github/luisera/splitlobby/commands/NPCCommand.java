package org.github.luisera.splitlobby.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.github.luisera.splitlobby.Main;
import org.github.luisera.splitlobby.npc.NPCData;

import java.util.Arrays;
import java.util.Collection;

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

        try {
            switch (args[0].toLowerCase()) {
                case "create":
                case "criar":
                    plugin.getNPCConversationManager().startCreateConversation(player);
                    break;

                case "edit":
                case "editar":
                    if (args.length < 2) {
                        player.sendMessage("§cUso: /slnpc edit <id>");
                        return true;
                    }
                    plugin.getNPCConversationManager().startFullEditConversation(player, Integer.parseInt(args[1]));
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
                    if (args.length < 2) {
                        player.sendMessage("§cUso: /slnpc setskin <id>");
                        return true;
                    }
                    plugin.getNPCConversationManager().startEditConversation(
                            player,
                            Integer.parseInt(args[1]),
                            org.github.luisera.splitlobby.npc.NPCConversationManager.EditType.SKIN
                    );
                    break;

                case "setcmd":
                case "setcommand":
                    if (args.length < 2) {
                        player.sendMessage("§cUso: /slnpc setcmd <id>");
                        return true;
                    }
                    plugin.getNPCConversationManager().startEditConversation(
                            player,
                            Integer.parseInt(args[1]),
                            org.github.luisera.splitlobby.npc.NPCConversationManager.EditType.COMMAND
                    );
                    break;

                case "setdesc":
                case "setdescription":
                    if (args.length < 2) {
                        player.sendMessage("§cUso: /slnpc setdesc <id>");
                        return true;
                    }
                    plugin.getNPCConversationManager().startEditConversation(
                            player,
                            Integer.parseInt(args[1]),
                            org.github.luisera.splitlobby.npc.NPCConversationManager.EditType.DESCRIPTION
                    );
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
        } catch (NumberFormatException e) {
            player.sendMessage("§cID inválido! Use um número.");
        }

        return true;
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
        player.sendMessage("§e  /slnpc create §7- Cria um NPC (modo conversa)");
        player.sendMessage("§e  /slnpc edit <id> §7- Edita nome e descrição");
        player.sendMessage("§e  /slnpc delete <id> §7- Deleta um NPC");
        player.sendMessage("§e  /slnpc setskin <id> §7- Define a skin");
        player.sendMessage("§e  /slnpc setcmd <id> §7- Define comando");
        player.sendMessage("§e  /slnpc setdesc <id> §7- Define descrição");
        player.sendMessage("§e  /slnpc list §7- Lista todos os NPCs");
        player.sendMessage("§e  /slnpc tp <id> §7- Teleporta para um NPC");
        player.sendMessage("");
    }

    private String formatLocation(org.bukkit.Location loc) {
        return String.format("%s (%.1f, %.1f, %.1f)",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}