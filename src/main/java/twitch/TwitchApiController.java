package twitch;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javassist.Modifier;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TwitchApiController {
    private static final Field MODIFIERS_FIELD;

    private static final String STREAM_TITLE_PREFIX = "Viewers + AI Slay the Spire (!climb)";

    // TODO don't hardcode this
//  twitchslaysspiretest
//    private static final String BROADCASTER_ID = "777541093";

    // twitchslaysspire
    private static final String BROADCASTER_ID = "605614377";

    private static final String BETA_ART_REWARD_1_WEEK_ID = "32907622-e992-4088-af09-46c75ad43cfa";
    private static final String BETA_ART_REWARD_2_HOURS_ID = "8b0dcb01-8f0a-47bc-8be6-c1834898fbe6";
    private static final String CHEESE_RUN_REWARD_ID = "4126d9e9-c2bf-4a28-8ca4-71e17f6dbc18";

    private String token;
    private String clientId;

    private static final long ONE_WEEK_MILLILS = 1_000 * 60 * 60 * 24 * 7;
    private static final long TWO_HOURS_MILLIS = 1_000 * 60 * 60 * 2;

    public TwitchApiController() throws IOException {
        InputStream in = new FileInputStream("tssconfig.txt");
        Properties properties = new Properties();
        properties.load(in);

        if (containProperLogin(properties)) {
            token = properties.getProperty("token");
            clientId = properties.getProperty("client_id");
        } else {
            System.out.println("no proper login");
        }
    }

    public void setStreamTitle(String title) throws IOException {
        URL url = new URL("https://api.twitch.tv/helix/channels?broadcaster_id=" + BROADCASTER_ID);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("PATCH");
        http.setDoOutput(true);
        http.setRequestProperty("Client-Id", clientId);
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Bearer " + token);

        JsonObject dataJson = new JsonObject();

        dataJson.addProperty("title", STREAM_TITLE_PREFIX + title);

        String data = dataJson.toString();

        byte[] out = data.getBytes(StandardCharsets.UTF_8);

        OutputStream stream = http.getOutputStream();
        stream.write(out);

        int responseCode = http.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    http.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } else {
            System.out.println("failed with code " + responseCode + " " + http
                    .getResponseMessage());
        }

    }


    public Optional<BetaArtRequest> queryBetaArtRequests() throws IOException {
        Optional<JsonObject> queryResult = queryRedemptions(BETA_ART_REWARD_1_WEEK_ID);

        BetaArtRequest result = new BetaArtRequest();
        if (queryResult.isPresent()) {
            result.duration = ONE_WEEK_MILLILS;
            result.rewardId = BETA_ART_REWARD_1_WEEK_ID;
        } else {
            queryResult = queryRedemptions(BETA_ART_REWARD_2_HOURS_ID);
            result.duration = TWO_HOURS_MILLIS;
            result.rewardId = BETA_ART_REWARD_2_HOURS_ID;
        }

        if (queryResult.isPresent()) {
            JsonObject redemption = queryResult.get();
            result.redemptionId = redemption.get("id").getAsString();
            result.userInput = redemption.get("user_input").getAsString();
            return Optional.of(result);
        }

        return Optional.empty();
    }

    public Optional<CheeseRequest> queryCheeseRequests() throws IOException {
        Optional<JsonObject> queryResult = queryRedemptions(CHEESE_RUN_REWARD_ID);

        if (queryResult.isPresent()) {
            CheeseRequest result = new CheeseRequest();

            JsonObject redemption = queryResult.get();
            result.rewardId = CHEESE_RUN_REWARD_ID;
            result.redemptionId = redemption.get("id").getAsString();
            result.userInput = redemption.get("user_input").getAsString();

            return Optional.of(result);
        }

        return Optional.empty();
    }

    public void fulfillChannelPointReward(BetaArtRequest betaArtRequest) throws IOException {
        completePointRedemption(betaArtRequest.rewardId, betaArtRequest.redemptionId, "FULFILLED");
    }

    public void cancelBetaArtReward(BetaArtRequest betaArtRequest) throws IOException {
        completePointRedemption(betaArtRequest.rewardId, betaArtRequest.redemptionId, "CANCELED");
    }

    public void completePointRedemption(String rewardId, String redemptionId, String status) throws IOException{
        String urlBuilder = String
                .format("https://api.twitch.tv/helix/channel_points/custom_rewards/redemptions?broadcaster_id=%s&reward_id=%s&id=%s", BROADCASTER_ID, rewardId, redemptionId);

        URL url = new URL(urlBuilder);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("PATCH");
        http.setDoOutput(true);
        http.setRequestProperty("Client-Id", clientId);
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Bearer " + token);


        //            http.setRequestProperty("broadcaster_id", "twitchslaysspire");
        JsonObject dataJson = new JsonObject();

        dataJson.addProperty("status", status);

        String data = dataJson.toString();

        byte[] out = data.getBytes(StandardCharsets.UTF_8);

        OutputStream stream = http.getOutputStream();
        stream.write(out);

        int responseCode = http.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    http.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } else {
//            System.out.println("failed with code " + responseCode + " " + http
//                    .getResponseMessage());
        }
    }

    private static boolean containProperLogin(Properties properties) {
        boolean hasAllRequiredFields = true;

        if (properties.get("client_id") == null) {
            hasAllRequiredFields = false;
        } else if (properties.get("token") == null) {
            hasAllRequiredFields = false;
        }

        return hasAllRequiredFields;
    }

    public ArrayList<String> queryChannelSubscribers() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        boolean isDone = false;
        boolean isFirst = true;
        String cursor = "";

        while (!isDone) {
            isDone = true;

            String urlString = "https://api.twitch.tv/helix/subscriptions?broadcaster_id=" + BROADCASTER_ID;

            if(!isFirst) {
                urlString += "&after="  + cursor;
            }

            isFirst = false;

            URL url = new URL(urlString);

            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.setDoOutput(true);
            http.setRequestProperty("Client-Id", clientId);
            http.setRequestProperty("Authorization", "Bearer " + token);

            if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        http.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // print result
                System.out.println(response.toString());

                JsonObject jsonObject = new JsonParser().parse(response.toString())
                                                        .getAsJsonObject();
                JsonArray array = jsonObject.getAsJsonArray("data");

                array.forEach(jsonElement -> result
                        .add(jsonElement.getAsJsonObject().get("user_name").getAsString()));

                JsonObject pagination = jsonObject.get("pagination").getAsJsonObject();
                if (pagination.has("cursor")) {
                    isDone = false;
                    cursor = pagination.get("cursor").getAsString();
                }
            } else {
                System.out.println("failed with code " + http.getResponseCode() + " " + http
                        .getResponseMessage());
                result.add("ERROR QUERYING SUBS, contact dev");
            }
        }

        return result;
    }

    public Optional<JsonObject> queryRedemptions(String rewardId) throws IOException {
        String baseUrl = "https://api.twitch.tv/helix/channel_points/custom_rewards/redemptions";
        String queryUrl = String.format("%s?broadcaster_id=%s&reward_id=%s&status=UNFULFILLED",
                baseUrl, BROADCASTER_ID, rewardId);

        URL url = new URL(queryUrl);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("GET");
        http.setDoOutput(true);
        http.setRequestProperty("Client-Id", clientId);
        http.setRequestProperty("Authorization", "Bearer " + token);

        if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    http.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
            JsonObject responseJson = new JsonParser().parse(response.toString()).getAsJsonObject();
            JsonArray dataArray = responseJson.get("data").getAsJsonArray();

            if (dataArray.size() > 0) {
                return Optional.of(dataArray.get(0).getAsJsonObject());
            }

//            System.out.println(response.toString());
        } else {
//            System.out.println("failed with code " + http.getResponseCode() + " " + http
//                    .getResponseMessage());
        }

        return Optional.empty();
    }

    public Optional<PredictionInfo> createPrediction() throws IOException {
        URL url = new URL("https://api.twitch.tv/helix/predictions");
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setRequestProperty("Client-Id", clientId);
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Bearer " + token);

        //            http.setRequestProperty("broadcaster_id", "twitchslaysspire");

        JsonObject dataJson = new JsonObject();

        dataJson.addProperty("broadcaster_id", BROADCASTER_ID);
        dataJson.addProperty("title", "Will we beat Act 3?");

        JsonArray outcomes = new JsonArray();

        JsonObject winOutcome = new JsonObject();
        winOutcome.addProperty("title", "YES! win win win win");

        JsonObject loseOutcome = new JsonObject();
        loseOutcome.addProperty("title", "NO! only mostly perfect");

        outcomes.add(winOutcome);
        outcomes.add(loseOutcome);

        dataJson.add("outcomes", outcomes);

        dataJson.addProperty("prediction_window", 300);

        String data = dataJson.toString();

        byte[] out = data.getBytes(StandardCharsets.UTF_8);

        OutputStream stream = http.getOutputStream();
        stream.write(out);

        int responseCode = http.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    http.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result

            JsonObject responseJson = new JsonParser().parse(response.toString()).getAsJsonObject();

            JsonObject predictionJson = responseJson.get("data").getAsJsonArray().get(0)
                                                    .getAsJsonObject();

            PredictionInfo result = new PredictionInfo();
            result.predictionId = predictionJson.get("id").getAsString();

            JsonArray outcomeResults = predictionJson.get("outcomes").getAsJsonArray();
            result.winningId = outcomeResults.get(0).getAsJsonObject().get("id").getAsString();
            result.losingId = outcomeResults.get(1).getAsJsonObject().get("id").getAsString();

            return Optional.of(result);
        } else {
            System.out.println("failed with code " + responseCode + " " + http
                    .getResponseMessage());
        }

        return Optional.empty();
    }

    public void resolvePrediction(PredictionInfo predictionInfo, boolean win) throws IOException {
        URL url = new URL("https://api.twitch.tv/helix/predictions");
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("PATCH");
        http.setDoOutput(true);
        http.setRequestProperty("Client-Id", clientId);
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Bearer " + token);

        JsonObject dataJson = new JsonObject();

        dataJson.addProperty("broadcaster_id", BROADCASTER_ID);
        dataJson.addProperty("id", predictionInfo.predictionId);
        dataJson.addProperty("status", "RESOLVED");
        dataJson.addProperty("winning_outcome_id", win ? predictionInfo.winningId : predictionInfo.losingId);

        String data = dataJson.toString();

        byte[] out = data.getBytes(StandardCharsets.UTF_8);

        OutputStream stream = http.getOutputStream();
        stream.write(out);

        int responseCode = http.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    http.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
//            System.out.println(response.toString());
        } else {
            System.out.println("failed with code " + responseCode + " " + http
                    .getResponseMessage());
        }
    }

    static {
        try {

            Method getDeclaredFields0 = Class.class
                    .getDeclaredMethod("getDeclaredFields0", boolean.class);
            getDeclaredFields0.setAccessible(true);
            Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
            Field modifiers = null;
            for (Field each : fields) {
                if ("modifiers".equals(each.getName())) {
                    modifiers = each;
                    break;
                }
            }

            MODIFIERS_FIELD = modifiers;
            allowMethods("PATCH");
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void allowMethods(String... methods) {
        try {
            Field methodsField = HttpURLConnection.class.getDeclaredField("methods");

            makeNonFinal(methodsField);

            methodsField.setAccessible(true);

            String[] oldMethods = (String[]) methodsField.get(null);
            Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
            methodsSet.addAll(Arrays.asList(methods));
            String[] newMethods = methodsSet.toArray(new String[0]);

            methodsField.set(null/*static field*/, newMethods);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void makeNonFinal(Field field) {
        int mods = field.getModifiers();
        field.setAccessible(true);

        MODIFIERS_FIELD.setAccessible(true);

        if (Modifier.isFinal(mods)) {
            try {
                MODIFIERS_FIELD.setByte(field, (byte) (mods & ~Modifier.FINAL));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
