package utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SamplePoster {
    private static final String USER_AGENT = "Mozilla/5.0";
//    private static final String URL = "http://54.221.53.86:8000/runhistory/runs/";
    private static final String URL = "http://127.0.0.1:8000/runhistory/runs/";
//    private static final String POST_PARAMS = "victory=true&score=Pass@123";

    public static void main(String[] args) {
        try {
            postScore();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void postScore() throws IOException {
        URL url = new URL (URL);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("victory", true);
        requestBody.addProperty("score", 123);

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

        try(OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println(response.toString());
        }
    }
}
