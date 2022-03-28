package twitch;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.DeadBranch;
import com.megacrit.cardcrawl.relics.PrismaticShard;
import com.megacrit.cardcrawl.relics.Tingsha;
import tssrelics.relics.FestivuePole;
import tssrelics.relics.JadeMysticKnot;

import java.io.IOException;
import java.util.HashMap;
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
    public static HashMap<String, CheeseConfig> availableCheeses;

    public static SpireConfig cheeseConfig;

    public CheeseController(TwitchController twitchController) {
        this.twitchController = twitchController;


        availableCheeses = new HashMap<>();

        // Prismatic Shard Cheese
        availableCheeses.put("rainbow", new CheeseConfig("rainbow", () -> {
            AbstractRelic shard = new PrismaticShard().makeCopy();

            AbstractDungeon.getCurrRoom()
                           .spawnRelicAndObtain((float) (Settings.WIDTH / 2), (float) (Settings.HEIGHT / 2), shard);

            AbstractDungeon.shopRelicPool.remove(shard.relicId);
        }, false));

        availableCheeses.put("serenity", new CheeseConfig("serenity", () -> {
            AbstractRelic pole = new FestivuePole().makeCopy();

            AbstractDungeon.getCurrRoom()
                           .spawnRelicAndObtain((float) (Settings.WIDTH / 2), (float) (Settings.HEIGHT / 2), pole);

            AbstractDungeon.rareRelicPool.remove(pole.relicId);
        }, true));

        availableCheeses.put("tingting", new CheeseConfig("tingting", () -> {
            AbstractRelic tingsha = new Tingsha().makeCopy();

            AbstractDungeon.getCurrRoom()
                           .spawnRelicAndObtain((float) (Settings.WIDTH / 2), (float) (Settings.HEIGHT / 2), tingsha);

            AbstractDungeon.rareRelicPool.remove(tingsha.relicId);
        }, true));

        availableCheeses.put("uglystick", new CheeseConfig("uglystick", () -> {
            AbstractRelic branch = new DeadBranch().makeCopy();

            AbstractDungeon.getCurrRoom()
                           .spawnRelicAndObtain((float) (Settings.WIDTH / 2), (float) (Settings.HEIGHT / 2), branch);

            AbstractDungeon.rareRelicPool.remove(branch.relicId);
        }, true));

        availableCheeses.put("alltiedup", new CheeseConfig("alltiedup", () -> {
            AbstractRelic knot = new JadeMysticKnot().makeCopy();

            AbstractDungeon.getCurrRoom()
                           .spawnRelicAndObtain((float) (Settings.WIDTH / 2), (float) (Settings.HEIGHT / 2), knot);

            AbstractDungeon.rareRelicPool.remove(knot.relicId);
        }, true));

        // Start a background thread to read the SpireConfig containing any pending cheese
        // request.
        requestedCheeseConfig = Optional.empty();
        new Thread(() -> {
            try {
                cheeseConfig = new SpireConfig("CommModExtension", "pending_cheese");

                if (cheeseConfig.has("cheese_id")) {
                    requestedCheeseConfig = Optional
                            .of(availableCheeses.get(cheeseConfig.getString("cheese_id")));
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
                    System.err.println("cheese controller querried");
                    if (cheeseRequestOptional.isPresent()) {
                        System.err.println("cheese found");
                        CheeseRequest cheeseRequest = cheeseRequestOptional.get();
                        String queryName = cheeseRequest.userInput.replace(" ", "").toLowerCase();

                        if (requestedCheeseConfig.isPresent()) {
                            twitchController.apiController
                                    .completePointRedemption(cheeseRequest.rewardId, cheeseRequest.redemptionId, "CANCELED");
                            TwitchController.twirk
                                    .channelMessage("[Bot] Cheese Redemption Cancelled, a cheese run is already queued up; Try again later.");
                        }

                        if (availableCheeses.containsKey(queryName)) {
                            requestedCheeseConfig = Optional.of(availableCheeses.get(queryName));

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
