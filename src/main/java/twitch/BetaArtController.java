package twitch;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BetaArtController {
    // The timestamp of the next time the controller should poll to check for any beta art requests.
    // The first poll will be INITIAL_POLL_DELAY milliseconds after startup and will then poll every
    // POLL_DELAY milliseconds.
    private long pollBetaArtTimestamp;

    // Parent controller
    private final TwitchController twitchController;

    private static final long INITIAL_POLL_DELAY = 10_000L;
    private static final long POLL_DELAY = 10_000L;

    HashMap<String, Long> betaExpirationsMap;
    SpireConfig betaArtConfig;

    public BetaArtController(TwitchController twitchController) {
        this.twitchController = twitchController;

        // Start a background thread to read the SpireConfig containing the current beta art
        // requests and fulfills them for the current launch.  Writes the config file back
        // excluding any expired elements thereby cleaning up the beta list and respecting
        // expirations
        betaExpirationsMap = new HashMap<>();
        new Thread(() -> {
            try {
                betaArtConfig = new SpireConfig("CommModExtension", "beta_redemptions");

                JsonObject betaMapJson = new JsonParser()
                        .parse(betaArtConfig.getString("beta_timestamps")).getAsJsonObject();

                long now = System.currentTimeMillis();
                for (Map.Entry<String, JsonElement> entry : betaMapJson.entrySet()) {
                    String key = entry.getKey();
                    long expiration = betaMapJson.get(key).getAsLong();
                    if (expiration > now) {
                        betaExpirationsMap.put(key, expiration);
                        UnlockTracker.betaCardPref.putBoolean(key, true);
                    }
                }
                saveBetaConfig();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        pollBetaArtTimestamp = System.currentTimeMillis() + INITIAL_POLL_DELAY;
    }

    public void update() {
        if (pollBetaArtTimestamp < System.currentTimeMillis()) {
            pollBetaArtTimestamp = System.currentTimeMillis() + 5_000;
            new Thread(() -> {
                try {
                    Optional<BetaArtRequest> betaArtRequestOptional = twitchController.apiController
                            .getBetaArtRedemptions();
                    if (betaArtRequestOptional.isPresent()) {
                        BetaArtRequest betaArtRequest = betaArtRequestOptional.get();

                        String queryName = betaArtRequest.userInput.replace(" ", "").toLowerCase();

                        if (twitchController.cardNamesToIdMap.containsKey(queryName)) {
                            String cardId = twitchController.cardNamesToIdMap.get(queryName);

                            UnlockTracker.betaCardPref.putBoolean(cardId, true);

                            twitchController.apiController
                                    .fullfillBetaArtReward(betaArtRequest.redemptionId);

                            long inAWeek = System.currentTimeMillis() + 1_000 * 60 * 60 * 24 * 7;

                            betaExpirationsMap.put(cardId, inAWeek);
                            saveBetaConfig();
                            TwitchController.twirk
                                    .channelMessage("[Bot] Beta art set successfully for " + betaArtRequest.userInput);
                        } else {
                            twitchController.apiController
                                    .cancelBetaArtReward(betaArtRequest.redemptionId);
                            TwitchController.twirk
                                    .channelMessage("[Bot] Redemption Cancelled, no card matching " + betaArtRequest.userInput);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Writes the beta table back info the spire config
     */
    private void saveBetaConfig() throws IOException {
        JsonObject toWrite = new JsonObject();
        for (Map.Entry<String, Long> entry : betaExpirationsMap.entrySet()) {
            toWrite.addProperty(entry.getKey(), entry.getValue());
        }

        betaArtConfig.setString("beta_timestamps", toWrite.toString());
        betaArtConfig.save();
    }
}
