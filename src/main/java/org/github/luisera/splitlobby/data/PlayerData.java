package org.github.luisera.splitlobby.data;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private final String name;

    // Dados do Jogador
    private double coins;
    private boolean flyMode;
    private boolean hidePlayers;

    // Controle de alteração (Para saber se precisa salvar no banco)
    private boolean dirty;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.coins = 0;
        this.flyMode = false;
        this.hidePlayers = false;
        this.dirty = false;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }

    public double getCoins() { return coins; }

    public void setCoins(double coins) {
        this.coins = coins;
        this.dirty = true; // Marca que houve mudança
    }

    public void addCoins(double amount) {
        setCoins(this.coins + amount);
    }

    public boolean isFlyMode() { return flyMode; }
    public void setFlyMode(boolean flyMode) { this.flyMode = flyMode; }

    public boolean isHidePlayers() { return hidePlayers; }
    public void setHidePlayers(boolean hidePlayers) { this.hidePlayers = hidePlayers; }

    // Métodos para verificar se precisamos salvar no MySQL
    public boolean isDirty() { return dirty; }
    public void setClean() { this.dirty = false; }
}