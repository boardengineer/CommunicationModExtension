package utilities;

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
import java.util.List;

public class SamplePoster {
    private static final String USER_AGENT = "Mozilla/5.0";
    //    private static final String URL_BASE = "http://54.221.53.86:8000";
    private static final String URL_BASE = "http://127.0.0.1:8000";

    private static final String RUNS_URL = URL_BASE + "/runhistory/runs/";
    private static final String FLOORS_URL = URL_BASE + "/runhistory/floor_results/";
    private static final String COMMANDS_URL = URL_BASE + "/runhistory/battle_commands/";
    private static final String VOTES_URL = URL_BASE + "/runhistory/vote_results/";
//    private static final String POST_PARAMS = "victory=true&score=Pass@123";

    private static final String FLOOR_QUERY_URL = URL_BASE + "/runhistory/floor_result_query";
    private static final String BATTLE_COMMAND_QUERY_URL = URL_BASE + "/runhistory/battle_command_query";
    private static final String VOTE_QUERY_URL = URL_BASE + "/runhistory/vote_result_query";

    public static void main(String[] args) {
        try {
//            System.out.println(queryBattleCommandResult(queryFloorResult(1, 211).get(0)));

//            postVoteResult(222, 5, 0, "1'");

            postScore();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void postVoteResult(int run, int floorNum, int index, String winningVote) throws IOException {
        URL url = new URL(VOTES_URL);
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
        System.out.println(jsonInputString);

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
            System.out.println("response " + response.toString());
        }
    }

    private static void postBattleCommands() throws IOException {
        URL url = new URL(COMMANDS_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("index", 1);
        requestBody.addProperty("command_string", "kill the thing");
        requestBody.addProperty("floor_result", 17);

        String jsonInputString = requestBody.toString();
        System.out.println(jsonInputString);

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
            System.out.println("response " + response.toString());
        }
    }

    private static void postFloorResult() throws IOException {
        URL url = new URL(FLOORS_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("floor_num", 1);
        requestBody.addProperty("hp_change", 3);
        requestBody.addProperty("run", 61);

        String jsonInputString = requestBody.toString();
        System.out.println(jsonInputString);

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
            System.out.println("response " + response.toString());
        }
    }

    private static void postScore() throws IOException {
        URL url = new URL(RUNS_URL + "278/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("seed_string", "hello");

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
        System.out.println(jsonInputString);

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

    private static List<Integer> queryFloorResult(int floorNum, int runId) throws IOException {
        URL url = new URL(String.format(FLOOR_QUERY_URL + "?run=%d&floor_num=%d", runId, floorNum));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        String jsonInputString = requestBody.toString();
        System.out.println(jsonInputString);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println("response " + response.toString());

            JsonArray floorResults = new JsonParser().parse(response.toString()).getAsJsonArray();

            ArrayList<Integer> result = new ArrayList<>();
            floorResults.forEach(ele -> result.add(ele.getAsJsonObject().get("id").getAsInt()));
            return result;
        }
    }

    private static List<Command> queryBattleCommandResult(int floor_result) throws IOException {
        URL url = new URL(String
                .format(BATTLE_COMMAND_QUERY_URL + "?floor_result=%d", floor_result));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        String jsonInputString = requestBody.toString();
        System.out.println(jsonInputString);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println("response " + response.toString());

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
}
