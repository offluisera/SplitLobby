package org.github.luisera.splitlobby.npc;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NPCData {

    private final int id;
    private final UUID uuid;
    private String name;
    private final Location location;

    private String skinName;
    private String skinTexture;
    private String skinSignature;
    private String command;
    private List<String> description;

    public NPCData(int id, UUID uuid, String name, Location location) {
        this.id = id;
        this.uuid = uuid;
        this.name = ChatColor.translateAlternateColorCodes('&', name);
        this.location = location;
        this.description = new ArrayList<>();
    }

    // Getters
    public int getId() { return id; }
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public Location getLocation() { return location; }
    public String getSkinName() { return skinName; }
    public String getSkinTexture() { return skinTexture; }
    public String getSkinSignature() { return skinSignature; }
    public String getCommand() { return command; }
    public List<String> getDescription() { return description; }

    // Setters
    public void setName(String name) {
        this.name = ChatColor.translateAlternateColorCodes('&', name);
    }
    public void setSkinName(String skinName) { this.skinName = skinName; }
    public void setSkinTexture(String skinTexture) { this.skinTexture = skinTexture; }
    public void setSkinSignature(String skinSignature) { this.skinSignature = skinSignature; }
    public void setCommand(String command) { this.command = command; }
    public void setDescription(List<String> description) {
        this.description = new ArrayList<>();
        for (String line : description) {
            this.description.add(ChatColor.translateAlternateColorCodes('&', line));
        }
    }
}