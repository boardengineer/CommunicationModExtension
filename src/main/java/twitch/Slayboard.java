package twitch;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Slayboard {
      private static final String URL = "http://tss.boardengineer.net";
//    private static final String URL = "http://127.0.0.1:8000";

    public static void postBattleState(String state, int runId) throws IOException {
        URL url = new URL(URL + "/runhistory/battles/");

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("run", runId);
        requestBody.addProperty("start_state", state);

        String jsonInputString = requestBody.toString();

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
    }

    public static void postFloorResult(int floorNum, int hpChange, int runId) throws IOException {
        URL url = new URL(URL + "/runhistory/floor_results/");

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("floor_num", floorNum);
        requestBody.addProperty("hp_change", hpChange);
        requestBody.addProperty("run", runId);

        String jsonInputString = requestBody.toString();

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println(response.toString());
        }
    }

    public static int startRun() throws IOException {
        URL url = new URL(URL + "/runhistory/runs/");

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("victory", false);
        requestBody.addProperty("score", 0);

        requestBody.addProperty("character_class", "TODO");
        requestBody.addProperty("ascension", 0);

        JsonArray players = new JsonArray();
        requestBody.add("players", players);

        JsonArray deck = new JsonArray();
        requestBody.add("deck", deck);

        JsonArray relics = new JsonArray();
        requestBody.add("relics", relics);

        String jsonInputString = requestBody.toString();

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.err.println("Started new run " + response.toString());

            return new JsonParser().parse(response.toString()).getAsJsonObject().get("id")
                                   .getAsInt();
        }
    }

    public static void postScore(String stateMessage, HashMap<String, Integer> voteFrequencies)
            throws IOException {
        URL url = new URL(URL + "/runhistory/runs/");

        JsonObject parsed = new JsonParser().parse(stateMessage).getAsJsonObject();
        JsonObject gameState = parsed.get("game_state").getAsJsonObject();
        JsonObject screenState = gameState.get("screen_state")
                                          .getAsJsonObject();

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("victory", screenState.get("victory").getAsBoolean());
        requestBody.addProperty("score", screenState.get("score").getAsInt());

        requestBody.addProperty("character_class", gameState.get("class").getAsString());
        requestBody.addProperty("ascension", gameState.get("ascension_level").getAsInt());

        JsonArray players = new JsonArray();
        voteFrequencies.entrySet().forEach(entry -> {
            JsonObject player = new JsonObject();
            player.addProperty("screen_name", entry.getKey());
            player.addProperty("votes", entry.getValue());
            players.add(player);
        });
        requestBody.add("players", players);

        JsonArray deck = new JsonArray();
        JsonArray stateDeck = gameState.get("deck").getAsJsonArray();
        stateDeck.forEach(deckElement -> {
            JsonObject cardToAdd = new JsonObject();
            cardToAdd.addProperty("card_id", deckElement.getAsJsonObject().get("id").getAsString());
            deck.add(cardToAdd);
        });
        requestBody.add("deck", deck);

        JsonArray relics = new JsonArray();
        JsonArray relicsJson = gameState.get("relics").getAsJsonArray();
        relicsJson.forEach(relicElement -> {
            JsonObject relicToAdd = new JsonObject();
            relicToAdd.addProperty("relic_id", relicElement.getAsJsonObject().get("id")
                                                           .getAsString());
            relics.add(relicToAdd);
        });
        requestBody.add("relics", relics);

        String jsonInputString = requestBody.toString();

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
    }
}
