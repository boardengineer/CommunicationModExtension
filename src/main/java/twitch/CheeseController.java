package twitch;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import twitch.cheese.CheeseOptions;

import java.io.IOException;
import java.util.Optional;

public class CheeseController {
    // The timestamp of the next time the controller should poll to check for any cheese requests.
    // The first poll will be INITIAL_POLL_DELAY milliseconds after startup and will then poll every
    // POLL_DELAY milliseconds.
    private long pollCheeseTimestamp;

    // Parent controller
    private final TwitchController twitchController;

    private static final long INITIAL_POLL_DELAY = 10_000L;
    private static final long POLL_DELAY = 5_000L;

    public static Optional<CheeseConfig> requestedCheeseConfig;

    public static SpireConfig cheeseConfig;

    public CheeseController(TwitchController twitchController) {
        this.twitchController = twitchController;

        // Start a background thread to read the SpireConfig containing any pending cheese
        // request.
        requestedCheeseConfig = Optional.empty();
        new Thread(() -> {
            try {
                cheeseConfig = new SpireConfig("CommModExtension", "pending_cheese");

                if (cheeseConfig.has("cheese_id")) {
                    requestedCheeseConfig = Optional
                            .of(CheeseOptions.AVAILABLE_CHEESES
                                    .get(cheeseConfig.getString("cheese_id")));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        pollCheeseTimestamp = System.currentTimeMillis() + INITIAL_POLL_DELAY;
    }

    public void update() {
        if (pollCheeseTimestamp < System.currentTimeMillis()) {
            pollCheeseTimestamp = System.currentTimeMillis() + POLL_DELAY;
            new Thread(() -> {
                try {
                    Optional<CheeseRequest> cheeseRequestOptional = twitchController.apiController
                            .queryCheeseRequests();

                    if (cheeseRequestOptional.isPresent()) {
                        CheeseRequest cheeseRequest = cheeseRequestOptional.get();
                        String queryName = cheeseRequest.userInput.replace(" ", "").toLowerCase();

                        if (requestedCheeseConfig.isPresent()) {
                            twitchController.apiController
                                    .completePointRedemption(cheeseRequest.rewardId, cheeseRequest.redemptionId, "CANCELED");
                            TwitchController.twirk
                                    .channelMessage("[Bot] Cheese Redemption Cancelled, a cheese run is already queued up; Try again later.");
                        }

                        if (CheeseOptions.AVAILABLE_CHEESES.containsKey(queryName)) {
                            requestedCheeseConfig = Optional.of(CheeseOptions.AVAILABLE_CHEESES.get(queryName));

                            cheeseConfig.setString("cheese_id", queryName);
                            cheeseConfig.save();

                            twitchController.apiController
                                    .completePointRedemption(cheeseRequest.rewardId, cheeseRequest.redemptionId, "FULFILLED");
                            cheeseConfig.setString("cheese_id", queryName);
                            cheeseConfig.save();

                            TwitchController.twirk
                                    .channelMessage("[Bot] Cheese Run Successfully Queued");
                        } else {
                            twitchController.apiController
                                    .completePointRedemption(cheeseRequest.rewardId, cheeseRequest.redemptionId, "CANCELED");
                            TwitchController.twirk
                                    .channelMessage("[Bot] Redemption Cancelled, no cheese id matching " + cheeseRequest.userInput);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public static class CheeseConfig {
        public final Runnable cheeseEffect;
        public final String name;
        public final boolean replaceNeow;

        public CheeseConfig(String name, Runnable cheeseEffect, boolean replaceNeow) {
            this.name = name;
            this.cheeseEffect = cheeseEffect;
            this.replaceNeow = replaceNeow;
        }
    }
}
