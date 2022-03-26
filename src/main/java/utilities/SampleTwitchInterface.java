package utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javassist.Modifier;
import twitch.BetaArtRequest;
import twitch.PredictionInfo;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SampleTwitchInterface {
    private static final Field MODIFIERS_FIELD;


//  twitchslaysspiretest
//    private static final String BROADCASTER_ID = "777541093";


    //  twitchslaysspire
    private static final String BROADCASTER_ID = "605614377";

    private static final String BETA_ART_REWARD_1_WEEK_ID = "32907622-e992-4088-af09-46c75ad43cfa";
    private static final String BETA_ART_REWARD_2_HOURS_ID = "292e4682-5152-4021-a0d7-14ad5b47a386";
    private static final String CHEESE_RUN_REWARD_ID = "76c937ab-6218-48ec-ae87-d71a5a6b2144";

    private static String token;
    private static String clientId;

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

        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) throws IOException {
        allowMethods("PATCH");

        InputStream in = new FileInputStream("tssconfig.txt");
        Properties properties = new Properties();
        properties.load(in);


        if (containProperLogin(properties)) {
            token = properties.getProperty("token");
            clientId = properties.getProperty("client_id");

            createBetaRedemption();

//            Optional<PredictionInfo> info = createPrediction();
//
//            resolvePrediction(info.get(), false);

        } else {
            System.out.println("no proper login");
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

    public static void makeNonFinal(Field field) {
        int mods = field.getModifiers();
        field.setAccessible(true);

        MODIFIERS_FIELD.setAccessible(true);

        if (Modifier.isFinal(mods)) {
            try {
                MODIFIERS_FIELD.setByte(field, (byte) (mods & ~Modifier.FINAL));
                System.out.println("success");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                System.out.println("failure");
            }
        }
    }

    public static void queryChannel() throws IOException {
        URL url = new URL("https://api.twitch.tv/helix/users?login=twitchslaysspiretest");
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
        }

    }

    public static void queryChannelRewards() throws IOException {
        URL url = new URL("https://api.twitch.tv/helix/channel_points/custom_rewards?broadcaster_id=" + BROADCASTER_ID);
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
        } else {
            System.out.println("failed with code " + http.getResponseCode() + " " + http
                    .getResponseMessage());
        }

    }


    public static void modifyChannel() throws IOException {
        URL url = new URL("https://api.twitch.tv/helix/channels?broadcaster_id=" + BROADCASTER_ID);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("PATCH");
        http.setDoOutput(true);
        http.setRequestProperty("Client-Id", clientId);
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Bearer " + token);


        //            http.setRequestProperty("broadcaster_id", "twitchslaysspire");

        String data = "{\"title\":\"I can still update my stream title via blarb\"}";

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
            System.out.println(response.toString());
        } else {
            System.out.println("failed with code " + responseCode + " " + http
                    .getResponseMessage());
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

    private static void createBetaRedemption() throws IOException {
        URL url = new URL("https://api.twitch.tv/helix/channel_points/custom_rewards?broadcaster_id=" + BROADCASTER_ID);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setRequestProperty("Client-Id", clientId);
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Bearer " + token);

        //            http.setRequestProperty("broadcaster_id", "twitchslaysspire");

        JsonObject dataJson = new JsonObject();

        dataJson.addProperty("title", "Do Thing 2");
        dataJson.addProperty("cost", 15_000);
        dataJson.addProperty("is_user_input_required", true);
        dataJson.addProperty("prompt", "What Beta Art would you like to enable?  You can double check spelling by using the !card command.");

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

            String rewardId = responseJson.get("data").getAsJsonArray().get(0).getAsJsonObject()
                                          .get("id").getAsString();

            System.out.println("rewardId " + rewardId);
        } else {
            System.out.println("failed with code " + responseCode + " " + http
                    .getResponseMessage());
        }
    }

    public static Optional<BetaArtRequest> getBetaArtRedemptions() throws IOException {
        Optional<JsonObject> queryResult = queryRedemptions(BETA_ART_REWARD_1_WEEK_ID);

        if (queryResult.isPresent()) {
            JsonObject redemption = queryResult.get();

            BetaArtRequest result = new BetaArtRequest();

            result.redemptionId = redemption.get("id").getAsString();
            result.userInput = redemption.get("user_input").getAsString();

            return Optional.of(result);
        }

        return Optional.empty();
    }

    public static void fullfillBetaArtReward(String redemptionId) throws IOException {
        fulfillChannelPointReward(BETA_ART_REWARD_1_WEEK_ID, redemptionId);
    }

    public static Optional<JsonObject> queryRedemptions(String rewardId) throws IOException {
        String baseUrl = "https://api.twitch.tv/helix/channel_points/custom_rewards/redemptions";
        String queryUrl = String.format("%s?broadcaster_id=%s&reward_id=%s&status=UNFULFILLED",
                baseUrl, BROADCASTER_ID, rewardId);

        System.out.println(queryUrl);
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

            System.out.println(response.toString());
        } else {
            System.out.println("failed with code " + http.getResponseCode() + " " + http
                    .getResponseMessage());
        }

        return Optional.empty();
    }

    public static void fulfillChannelPointReward(String rewardId, String redemptionId) throws IOException {
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

        dataJson.addProperty("status", "FULFILLED");

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
            System.out.println(response.toString());
        } else {
            System.out.println("failed with code " + responseCode + " " + http
                    .getResponseMessage());
        }
    }

    private static Optional<PredictionInfo> createPrediction() throws IOException {
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

    public static void resolvePrediction(PredictionInfo predictionInfo, boolean win) throws IOException {
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
            System.out.println(response.toString());
        } else {
            System.out.println("failed with code " + responseCode + " " + http
                    .getResponseMessage());
        }
    }
}
