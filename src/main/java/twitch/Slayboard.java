package twitch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Slayboard {
    private static final String URL = "http://54.221.53.86:8000";

    public static void postScore(String stateMessage) throws IOException {
        URL url = new URL(URL + "/runhistory/runs/");

        JsonObject parsed = new JsonParser().parse(stateMessage).getAsJsonObject();
        JsonObject screenState = parsed.get("game_state").getAsJsonObject().get("screen_state")
                                       .getAsJsonObject();

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("victory", screenState.get("victory").getAsBoolean());
        requestBody.addProperty("score", screenState.get("score").getAsInt());

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
}
