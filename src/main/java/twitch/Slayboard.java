package twitch;

import battleaimod.networking.AiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ludicrousspeed.simulator.commands.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Slayboard {
    //      private static final String URL = "http://tss.boardengineer.net";
    private static final String URL = "http://127.0.0.1:8000";

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

    public static int postFloorResult(int floorNum, int hpChange, int runId) throws IOException {
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
            return new JsonParser().parse(response.toString()).getAsJsonObject().get("id")
                                   .getAsInt();
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

    public static void updateRunSeedAndAscension(int run, String seed, int ascension, String characterClass) throws IOException {
        URL url = new URL(URL + "/runhistory/runs/" + run + "/");

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("seed_string", seed);
        requestBody.addProperty("ascension", ascension);
        requestBody.addProperty("character_class", characterClass);

        // Add players
        JsonArray players = new JsonArray();
        JsonObject player1 = new JsonObject();
        player1.addProperty("screen_name", "george");
        player1.addProperty("votes", 548);
        players.add(player1);
        requestBody.add("players", players);

        // Add deck
        JsonArray deck = new JsonArray();
        JsonObject card1 = new JsonObject();
        card1.addProperty("card_id", "Strike");
        deck.add(card1);
        requestBody.add("deck", deck);

        // Add relics
        JsonArray relics = new JsonArray();
        JsonObject relic1 = new JsonObject();
        relic1.addProperty("relic_id", "burning blood");
        relics.add(relic1);
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

    public static void postCommands(int floorResult, List<Command> commands) throws IOException {
        URL url = new URL(URL + "/runhistory/battle_commands/");

        JsonObject requestBody = new JsonObject();

        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i) == null) {
                continue;
            }

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            requestBody.addProperty("index", i);
            requestBody.addProperty("floor_result", floorResult);

            requestBody.addProperty("command_string", commands.get(i).encode());

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

    public static void postVoteResult(int run, int floorNum, int index, String winningVote) throws IOException {
        URL url = new URL(URL + "/runhistory/vote_results/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("run", run);
        requestBody.addProperty("floor_num", floorNum);
        requestBody.addProperty("index", index);
        requestBody.addProperty("winning_vote", winningVote);

        String jsonInputString = requestBody.toString();

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
    }

    public static List<Integer> queryFloorResult(int floorNum, int runId) throws IOException {
        URL url = new URL(String
                .format(URL + "/runhistory/floor_result_query?run=%d&floor_num=%d", runId, floorNum));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            JsonArray floorResults = new JsonParser().parse(response.toString()).getAsJsonArray();

            ArrayList<Integer> result = new ArrayList<>();
            floorResults.forEach(ele -> result.add(ele.getAsJsonObject().get("id").getAsInt()));
            return result;
        }
    }

    public static String queryVoteResult(int floorNum, int runId, int index) throws IOException {
        URL url = new URL(String
                .format(URL + "/runhistory/vote_result_query?run=%d&floor_num=%d&index=%d", runId, floorNum, index));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            System.err.println(response.toString());

            JsonArray voteResults = new JsonParser().parse(response.toString()).getAsJsonArray();
            return voteResults.get(0).getAsJsonObject().get("winning_vote").getAsString();
        }
    }

    public static List<Command> queryBattleCommandResult(int floorResult) throws IOException {
        URL url = new URL(String
                .format(URL + "/runhistory/battle_command_query?floor_result=%d", floorResult));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        System.err.println("here?");

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;

            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }


            JsonArray commandResults = new JsonParser().parse(response.toString()).getAsJsonArray();

            ArrayList<JsonObject> commandsObjects = new ArrayList<>();
            commandResults.forEach(ele -> commandsObjects.add(ele.getAsJsonObject()));

            ArrayList<Command> result = new ArrayList<>(commandsObjects.size());

            commandsObjects.forEach(jsonObject -> result
                    .add(jsonObject.get("index").getAsInt() - 1, AiClient
                            .toCommand(jsonObject.get("command_string").getAsString())));

            return result;
        }
    }

    public static String queryRunCommand(int runId) throws IOException {
        URL url = new URL(String
                .format(URL + "/runhistory/runs/%d/", runId));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;

            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            JsonObject runInfo = new JsonParser().parse(response.toString()).getAsJsonObject();

            String runClass = runInfo.get("character_class").getAsString().toLowerCase();
            int ascension = runInfo.get("ascension").getAsInt();
            String seed = runInfo.get("seed_string").getAsString();

            String runCommand = String.format("start %s %d %s", runClass, ascension, seed);

            return runCommand;
        }
    }
}
