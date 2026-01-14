package org.github.luisera.splitlobby.npc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SkinFetcher {

    /**
     * Busca skin de um jogador na API do Mojang
     * @return [texture, signature] ou null se falhar
     */
    public static String[] fetchSkin(String playerName) {
        try {
            // 1. Busca UUID do jogador
            URL uuidUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection connection = (HttpURLConnection) uuidUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                return null;
            }

            JsonObject uuidJson = new JsonParser()
                    .parse(new InputStreamReader(connection.getInputStream()))
                    .getAsJsonObject();

            String uuid = uuidJson.get("id").getAsString();

            // 2. Busca dados da skin
            URL skinUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            connection = (HttpURLConnection) skinUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                return null;
            }

            JsonObject skinJson = new JsonParser()
                    .parse(new InputStreamReader(connection.getInputStream()))
                    .getAsJsonObject();

            JsonObject properties = skinJson.getAsJsonArray("properties")
                    .get(0).getAsJsonObject();

            String texture = properties.get("value").getAsString();
            String signature = properties.get("signature").getAsString();

            return new String[]{texture, signature};

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}