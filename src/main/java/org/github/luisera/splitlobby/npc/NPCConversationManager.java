package org.github.luisera.splitlobby.npc;

import org.bukkit.entity.Player;
import org.github.luisera.splitlobby.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCConversationManager {

    private final Main plugin;
    private final Map<UUID, ConversationData> conversations = new HashMap<>();

    public NPCConversationManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicia conversa para criar NPC
     */
    public void startCreateConversation(Player player) {
        ConversationData data = new ConversationData(ConversationType.CREATE);
        data.setLocation(player.getLocation());
        conversations.put(player.getUniqueId(), data);

        player.sendMessage("");
        player.sendMessage("§6§l⚑ CRIAR NPC");
        player.sendMessage("");
        player.sendMessage("§e➤ Qual o nome do NPC?");
        player.sendMessage("§7Use & para cores (ex: &bNome &aAqui)");
        player.sendMessage("");
        player.sendMessage("§cDigite 'cancelar' para cancelar");
        player.sendMessage("");
    }

    /**
     * Inicia conversa para editar NPC completo
     */
    public void startFullEditConversation(Player player, int npcId) {
        NPCData npc = plugin.getNPCManager().getNPC(npcId);
        if (npc == null) {
            player.sendMessage("§cNPC não encontrado!");
            return;
        }

        ConversationData data = new ConversationData(ConversationType.FULL_EDIT);
        data.setNpcId(npcId);
        data.setLocation(npc.getLocation());
        conversations.put(player.getUniqueId(), data);

        player.sendMessage("");
        player.sendMessage("§6§l⚑ EDITAR NPC #" + npcId);
        player.sendMessage("");
        player.sendMessage("§e  Nome atual: " + npc.getName());
        if (npc.getDescription() != null && !npc.getDescription().isEmpty()) {
            player.sendMessage("§e  Descrição atual:");
            for (String line : npc.getDescription()) {
                player.sendMessage("    " + line);
            }
        } else {
            player.sendMessage("§e  Descrição: §7Nenhuma");
        }
        player.sendMessage("");
        player.sendMessage("§e➤ Digite o novo nome do NPC:");
        player.sendMessage("§7Use & para cores");
        player.sendMessage("");
        player.sendMessage("§cDigite 'cancelar' para cancelar");
        player.sendMessage("");
    }

    /**
     * Inicia conversa para editar NPC
     */
    public void startEditConversation(Player player, int npcId, EditType editType) {
        NPCData npc = plugin.getNPCManager().getNPC(npcId);
        if (npc == null) {
            player.sendMessage("§cNPC #" + npcId + " não encontrado!");
            player.sendMessage("§7Use §e/slnpc list §7para ver todos os NPCs");
            return;
        }

        ConversationData data = new ConversationData(ConversationType.EDIT);
        data.setNpcId(npcId);
        data.setEditType(editType);
        conversations.put(player.getUniqueId(), data);

        player.sendMessage("");
        player.sendMessage("§a§l✓ NPC #" + npcId + " encontrado: §f" + npc.getName());
        player.sendMessage("");

        switch (editType) {
            case COMMAND:
                player.sendMessage("§6§l⚑ DEFINIR COMANDO");
                player.sendMessage("");
                player.sendMessage("§e➤ Digite o comando para o NPC:");
                player.sendMessage("§7Variáveis: {player}, {uuid}");
                player.sendMessage("§7Use [CONSOLE] no início para executar como console");
                player.sendMessage("");
                player.sendMessage("§7Exemplo: §fwarp pvp");
                player.sendMessage("§7Exemplo: §f[CONSOLE] give {player} diamond 1");
                break;

            case DESCRIPTION:
                player.sendMessage("§6§l⚑ DEFINIR DESCRIÇÃO");
                player.sendMessage("");
                player.sendMessage("§e➤ Digite a descrição do NPC:");
                player.sendMessage("§7Use | para separar linhas");
                player.sendMessage("§7Use & para cores");
                player.sendMessage("");
                player.sendMessage("§7Exemplo: §f&bBedwars | &7Clique com direito");
                break;

            case SKIN:
                player.sendMessage("§6§l⚑ DEFINIR SKIN");
                player.sendMessage("");
                player.sendMessage("§e➤ Digite o nick do jogador para copiar a skin:");
                player.sendMessage("");
                player.sendMessage("§7Exemplo: §fNotch");
                break;
        }
        player.sendMessage("");
        player.sendMessage("§cDigite 'cancelar' para cancelar");
        player.sendMessage("");
    }

    /**
     * Processa mensagem do jogador
     */
    public boolean handleMessage(Player player, String message) {
        ConversationData data = conversations.get(player.getUniqueId());
        if (data == null) return false;

        if (message.equalsIgnoreCase("cancelar") || message.equalsIgnoreCase("cancel")) {
            conversations.remove(player.getUniqueId());
            player.sendMessage("§cOperação cancelada!");
            return true;
        }

        switch (data.getType()) {
            case CREATE:
                return handleCreateConversation(player, data, message);
            case EDIT:
                return handleEditConversation(player, data, message);
            case FULL_EDIT:
                return handleFullEditConversation(player, data, message);
        }

        return false;
    }

    /**
     * Processa conversa de criação de NPC
     */
    private boolean handleCreateConversation(Player player, ConversationData data, String message) {
        switch (data.getStep()) {
            case 0: // Nome
                data.setName(message);
                data.nextStep();

                player.sendMessage("");
                player.sendMessage("§a✓ Nome definido: " + org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
                player.sendMessage("");
                player.sendMessage("§e➤ Deseja adicionar uma descrição?");
                player.sendMessage("§7Digite §aSIM §7ou §cNÃO");
                player.sendMessage("");
                return true;

            case 1: // Descrição (Sim/Não)
                if (message.equalsIgnoreCase("sim") || message.equalsIgnoreCase("yes") || message.equalsIgnoreCase("s")) {
                    data.nextStep();
                    player.sendMessage("");
                    player.sendMessage("§e➤ Digite a descrição:");
                    player.sendMessage("§7Use | para separar linhas");
                    player.sendMessage("§7Use & para cores");
                    player.sendMessage("");
                    player.sendMessage("§7Exemplo: §f&bBedwars | &7Clique aqui");
                    player.sendMessage("");
                    return true;
                } else if (message.equalsIgnoreCase("não") || message.equalsIgnoreCase("nao") || message.equalsIgnoreCase("no") || message.equalsIgnoreCase("n")) {
                    // Criar NPC sem descrição
                    createNPC(player, data);
                    return true;
                } else {
                    player.sendMessage("§cResposta inválida! Digite §aSIM §cou §cNÃO");
                    return true;
                }

            case 2: // Descrição (Texto)
                data.setDescription(message);
                createNPC(player, data);
                return true;
        }

        return false;
    }

    /**
     * Processa conversa de edição completa de NPC
     */
    private boolean handleFullEditConversation(Player player, ConversationData data, String message) {
        NPCData npc = plugin.getNPCManager().getNPC(data.getNpcId());
        if (npc == null) {
            player.sendMessage("§cNPC não encontrado!");
            conversations.remove(player.getUniqueId());
            return true;
        }

        switch (data.getStep()) {
            case 0: // Nome
                data.setName(message);
                npc.setName(message);
                data.nextStep();

                player.sendMessage("");
                player.sendMessage("§a✓ Nome atualizado: " + org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
                player.sendMessage("");
                player.sendMessage("§e➤ Deseja atualizar a descrição?");
                player.sendMessage("§7Digite §aSIM §7ou §cNÃO");
                player.sendMessage("");
                return true;

            case 1: // Descrição (Sim/Não)
                if (message.equalsIgnoreCase("sim") || message.equalsIgnoreCase("yes") || message.equalsIgnoreCase("s")) {
                    data.nextStep();
                    player.sendMessage("");
                    player.sendMessage("§e➤ Digite a nova descrição:");
                    player.sendMessage("§7Use | para separar linhas");
                    player.sendMessage("§7Use & para cores");
                    player.sendMessage("");
                    return true;
                } else if (message.equalsIgnoreCase("não") || message.equalsIgnoreCase("nao") || message.equalsIgnoreCase("no") || message.equalsIgnoreCase("n")) {
                    // Salvar sem atualizar descrição
                    updateNPC(player, data, npc, null);
                    return true;
                } else {
                    player.sendMessage("§cResposta inválida! Digite §aSIM §cou §cNÃO");
                    return true;
                }

            case 2: // Descrição (Texto)
                updateNPC(player, data, npc, message);
                return true;
        }

        return false;
    }

    /**
     * Atualiza o NPC
     */
    private void updateNPC(Player player, ConversationData data, NPCData npc, String description) {
        if (description != null && !description.isEmpty()) {
            java.util.List<String> lines = java.util.Arrays.asList(description.split("\\|"));
            plugin.getNPCManager().setDescription(data.getNpcId(), lines);
        } else {
            // Apenas salva o nome
            plugin.getNPCManager().setDescription(data.getNpcId(), npc.getDescription());
        }

        player.sendMessage("");
        player.sendMessage("§a§l✓ NPC ATUALIZADO!");
        player.sendMessage("§e  ID: §f#" + data.getNpcId());
        player.sendMessage("§e  Nome: " + npc.getName());
        player.sendMessage("");

        conversations.remove(player.getUniqueId());
    }

    /**
     * Processa conversa de edição de NPC
     */
    private boolean handleEditConversation(Player player, ConversationData data, String message) {
        NPCData npc = plugin.getNPCManager().getNPC(data.getNpcId());
        if (npc == null) {
            player.sendMessage("§cNPC não encontrado!");
            conversations.remove(player.getUniqueId());
            return true;
        }

        switch (data.getEditType()) {
            case COMMAND:
                plugin.getNPCManager().setCommand(data.getNpcId(), message);
                player.sendMessage("");
                player.sendMessage("§a§l✓ COMANDO DEFINIDO!");
                player.sendMessage("§e  NPC: §f" + npc.getName() + " §7(#" + data.getNpcId() + ")");
                player.sendMessage("§e  Comando: §f" + message);
                player.sendMessage("");
                break;

            case DESCRIPTION:
                java.util.List<String> lines = java.util.Arrays.asList(message.split("\\|"));
                plugin.getNPCManager().setDescription(data.getNpcId(), lines);
                player.sendMessage("");
                player.sendMessage("§a§l✓ DESCRIÇÃO DEFINIDA!");
                player.sendMessage("§e  NPC: §f" + npc.getName() + " §7(#" + data.getNpcId() + ")");
                player.sendMessage("§e  Linhas:");
                for (String line : lines) {
                    player.sendMessage("    " + org.bukkit.ChatColor.translateAlternateColorCodes('&', line.trim()));
                }
                player.sendMessage("");
                break;

            case SKIN:
                plugin.getNPCManager().setSkin(data.getNpcId(), message);
                player.sendMessage("");
                player.sendMessage("§e§l⌛ APLICANDO SKIN...");
                player.sendMessage("§7Buscando skin de: §f" + message);
                player.sendMessage("§7Aguarde...");
                player.sendMessage("");
                break;
        }

        conversations.remove(player.getUniqueId());
        return true;
    }

    /**
     * Cria o NPC
     */
    private void createNPC(Player player, ConversationData data) {
        NPCData npc = plugin.getNPCManager().createNPC(data.getName(), data.getLocation());

        if (data.getDescription() != null && !data.getDescription().isEmpty()) {
            java.util.List<String> lines = java.util.Arrays.asList(data.getDescription().split("\\|"));
            plugin.getNPCManager().setDescription(npc.getId(), lines);
        }

        player.sendMessage("");
        player.sendMessage("§a§l✓ NPC CRIADO!");
        player.sendMessage("§e  ID: §f#" + npc.getId());
        player.sendMessage("§e  Nome: " + npc.getName());
        player.sendMessage("§e  Localização: §f" + formatLocation(data.getLocation()));
        player.sendMessage("");
        player.sendMessage("§7Use §e/slnpc setskin " + npc.getId() + " §7para definir a skin");
        player.sendMessage("§7Use §e/slnpc setcmd " + npc.getId() + " §7para definir comando");
        player.sendMessage("");

        conversations.remove(player.getUniqueId());
    }

    /**
     * Verifica se jogador está em conversa
     */
    public boolean isInConversation(Player player) {
        return conversations.containsKey(player.getUniqueId());
    }

    /**
     * Cancela conversa
     */
    public void cancelConversation(Player player) {
        conversations.remove(player.getUniqueId());
    }

    /**
     * Formata localização
     */
    private String formatLocation(org.bukkit.Location loc) {
        return String.format("%s (%.1f, %.1f, %.1f)",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    // Enums
    public enum ConversationType {
        CREATE, EDIT, FULL_EDIT
    }

    public enum EditType {
        COMMAND, DESCRIPTION, SKIN
    }

    // Classe interna para dados da conversa
    private static class ConversationData {
        private final ConversationType type;
        private int step = 0;
        private org.bukkit.Location location;
        private String name;
        private String description;
        private int npcId;
        private EditType editType;

        public ConversationData(ConversationType type) {
            this.type = type;
        }

        public ConversationType getType() { return type; }
        public int getStep() { return step; }
        public void nextStep() { step++; }
        public org.bukkit.Location getLocation() { return location; }
        public void setLocation(org.bukkit.Location location) { this.location = location; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public int getNpcId() { return npcId; }
        public void setNpcId(int npcId) { this.npcId = npcId; }
        public EditType getEditType() { return editType; }
        public void setEditType(EditType editType) { this.editType = editType; }
    }
}