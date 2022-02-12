package utilities;

import javassist.Modifier;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class SampleTwitchInterface {
    private static final Field MODIFIERS_FIELD;
    private static String BROADCASTER_ID = "605614377";
    private static String token;
    private static String clientId;

    static {
        try {

            Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
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

            modifyChannel();

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
        System.out.println("we back?");
    }

    public static void queryChannel() throws IOException {
        URL url = new URL("https://api.twitch.tv/helix/users?login=twitchslaysspire");
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
}
