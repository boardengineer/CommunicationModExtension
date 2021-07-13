package utilities;

import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SamplePoster {
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String GET_URL = "http://54.221.53.86:8000/runhistory/runs/";
//    private static final String POST_PARAMS = "victory=true&score=Pass@123";

    public static void main(String[] args) {
        try {
            postScore();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void postScore() throws IOException {
        URL url = new URL ("http://54.221.53.86:8000/runhistory/runs/");
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("victory", true);
        requestBody.addProperty("score", 123);

        String jsonInputString = requestBody.toString();

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
