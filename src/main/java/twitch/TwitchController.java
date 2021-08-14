package twitch;

import basemod.ReflectionHacks;
import basemod.interfaces.PostRenderSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import battleaimod.BattleAiMod;
import battleaimod.networking.AiClient;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.types.users.TwitchUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.relics.CursedKey;
import com.megacrit.cardcrawl.relics.WingBoots;
import com.megacrit.cardcrawl.screens.GameOverScreen;
import com.megacrit.cardcrawl.ui.buttons.ReturnToMenuButton;
import ludicrousspeed.LudicrousSpeedMod;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class TwitchController implements PostUpdateSubscriber, PostRenderSubscriber {
    private static final Texture HEART_IMAGE = new Texture("heart.png");

    private static final long DECK_DISPLAY_TIMEOUT = 60_000;
    private static final long RELIC_DISPLAY_TIMEOUT = 60_000;
    private static final long BOSS_DISPLAY_TIMEOUT = 30_000;

    private static final long NO_VOTE_TIME_MILLIS = 1_000;
    private static final long FAST_VOTE_TIME_MILLIS = 3_000;
    private static final long NORMAL_VOTE_TIME_MILLIS = 20_000;

    public enum VoteType {
        // THe first vote in each dungeon
        CHARACTER("character", 25_000),
        MAP_LONG("map_long", 30_000),
        MAP_SHORT("map_short", 15_000),
        CARD_SELECT_LONG("card_select_long", 30_000),
        CARD_SELECT_SHORT("card_select_short", 20_000),
        GAME_OVER("game_over", 15_000),
        OTHER("other", 25_000),
        REST("rest", 1_000),
        SKIP("skip", 1_000);

        String optionName;
        int defaultTime;

        VoteType(String optionName, int defaultTime) {
            this.optionName = optionName;
            this.defaultTime = defaultTime;
        }
    }

    /**
     * Used to count user votes during
     */
    private HashMap<String, String> voteByUsernameMap = null;

    /**
     * Tallies the votes by user for a given run. Increments at the end of each vote and gets
     * reset when the character vote starts.
     */
    private HashMap<String, Integer> voteFrequencies = new HashMap<>();

    private VoteType currentVote = null;
    private String stateString = "";

    private String screenType = null;
    public static VoteController voteController;

    public static HashMap<String, Integer> optionsMap;

    private long voteEndTimeMillis;

    ArrayList<Choice> choices;
    ArrayList<Choice> viableChoices;
    private HashMap<String, Choice> choicesMap;

    private final LinkedBlockingQueue<String> readQueue;
    private final Twirk twirk;

    private boolean shouldStartClientOnUpdate = false;
    private boolean inBattle = false;
    private boolean fastMode = true;
    int consecutiveNoVotes = 0;
    boolean skipAfterCard = true;

    public static long lastDeckDisplayTimestamp = 0L;
    public static long lastRelicDisplayTimestamp = 0L;
    public static long lastBossDisplayTimestamp = 0L;

    public TwitchController(LinkedBlockingQueue<String> readQueue, Twirk twirk) {
        this.readQueue = readQueue;
        this.twirk = twirk;

        optionsMap = new HashMap<>();
        optionsMap.put("asc", 0);
        optionsMap.put("lives", 0);
        optionsMap.put("turns", 10_000);

        for (VoteType voteType : VoteType.values()) {
            optionsMap.put(voteType.optionName, voteType.defaultTime);
        }
    }

    @Override
    public void receivePostUpdate() {
        if (shouldStartClientOnUpdate) {
            shouldStartClientOnUpdate = false;
            inBattle = true;
            startAiClient();
        }

        if (BattleAiMod.rerunController != null || LudicrousSpeedMod.mustRestart) {
            if (BattleAiMod.rerunController.isDone || LudicrousSpeedMod.mustRestart) {
                LudicrousSpeedMod.controller = BattleAiMod.rerunController = null;
                inBattle = false;
                if (LudicrousSpeedMod.mustRestart) {
                    System.err.println("Desync detected, rerunning simluation");
                    LudicrousSpeedMod.mustRestart = false;
                    startAiClient();
                }
            }
        }

        try {
            if (voteByUsernameMap != null) {
                long timeRemaining = voteEndTimeMillis - System.currentTimeMillis();

                if (timeRemaining <= 0) {
                    if (voteController != null) {
                        voteController.endVote();
                    }

                    voteByUsernameMap.keySet().forEach(userName -> {
                        if (!voteFrequencies.containsKey(userName)) {
                            voteFrequencies.put(userName, 0);
                        }
                        voteFrequencies.put(userName, voteFrequencies.get(userName) + 1);
                    });

                    Choice result = getVoteResult();

                    System.err.println("selected " + result);

                    for (String command : result.resultCommands) {
                        if (currentVote == VoteType.CHARACTER &&
                                optionsMap.getOrDefault("asc", 0) > 0 &&
                                result.resultCommands.size() == 1) {
                            command += String.format(" %d", optionsMap.get("asc"));
                        }
                        readQueue.add(command);
                    }

                    if (!voteByUsernameMap.isEmpty()) {
                        String fileName = String
                                .format("votelogs/%s.txt", System.currentTimeMillis());
                        FileWriter writer = new FileWriter(fileName);
                        writer.write(voteByUsernameMap.toString() + " " + stateString);
                        writer.close();
                    }

                    voteByUsernameMap = null;
                    voteController = null;
                    currentVote = null;
                    screenType = null;
                }
            }
        } catch (ConcurrentModificationException | NullPointerException | IOException e) {
            System.err.println("Null pointer caught, clean up this crap");
        }
    }

    public void receiveMessage(TwitchUser user, String message) {
        String userName = user.getDisplayName();
        String[] tokens = message.split(" ");

        if (tokens.length == 1 && tokens[0].equals("07734")) {
            fastMode = false;
            consecutiveNoVotes = 0;
        }

        if (userName.equalsIgnoreCase("twitchslaysspire")) {
            // admin direct command override
            if (tokens.length >= 2 && tokens[0].equals("!sudo")) {
                String command = message.substring(message.indexOf(' ') + 1);
                readQueue.add(command);
            } else if (tokens.length >= 2 && tokens[0].equals("!admin")) {
                if (tokens[1].equals("set")) {
                    if (tokens.length >= 4) {
                        String optionName = tokens[2];
                        if (optionsMap.containsKey(optionName)) {
                            try {
                                int optionValue = Integer.parseInt(tokens[3]);
                                optionsMap.put(optionName, optionValue);
                                System.err
                                        .format("%s successfully set to %d\n", optionName, optionValue);
                            } catch (NumberFormatException e) {

                            }
                        }
                    }
                } else if (tokens[1].equals("disable")) {
                    voteByUsernameMap = null;
                    inBattle = false;
                }
            }
        }

        if (tokens.length == 1) {
            try {
                if (tokens[0].equals("!deck")) {
                    long now = System.currentTimeMillis();
                    if (now > lastDeckDisplayTimestamp + DECK_DISPLAY_TIMEOUT) {
                        lastDeckDisplayTimestamp = now;
                        HashMap<String, Integer> cards = new HashMap<>();
                        AbstractDungeon.player.masterDeck.group
                                .forEach(c -> cards.merge(c.name, 1, Integer::sum));

                        StringBuilder sb = new StringBuilder("[BOT] ");
                        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                            if (cards.containsKey(c.name)) {
                                sb.append(c.name);
                                int amt = cards.get(c.name);
                                if (amt > 1) {
                                    sb.append(" x").append(amt);
                                }
                                sb.append(";");
                                cards.remove(c.name);
                            }
                        }
                        if (sb.length() > 0) {
                            sb.deleteCharAt(sb.length() - 1);
                        }

                        twirk.channelMessage(sb.toString());
                    }
                }

                if (tokens[0].equals("!boss")) {
                    long now = System.currentTimeMillis();
                    if (now > lastBossDisplayTimestamp + BOSS_DISPLAY_TIMEOUT) {
                        lastBossDisplayTimestamp = now;
                        twirk.channelMessage("[BOT] " + AbstractDungeon.bossKey);
                    }
                }

                if (tokens[0].equals("!relics")) {
                    long now = System.currentTimeMillis();
                    if (now > lastRelicDisplayTimestamp + RELIC_DISPLAY_TIMEOUT) {
                        lastRelicDisplayTimestamp = now;

                        String relics = AbstractDungeon.player.relics.stream()
                                                                     .map(relic -> relic.relicId)
                                                                     .collect(Collectors
                                                                             .joining(";"));

                        twirk.channelMessage("[BOT] " + relics);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (voteByUsernameMap != null) {
            if (tokens.length == 1 || (tokens.length >= 2 && VOTE_PREFIXES.contains(tokens[0]))) {
                String voteValue = tokens[0].toLowerCase();
                if (tokens.length >= 2 && VOTE_PREFIXES.contains(tokens[0])) {
                    voteValue = tokens[1].toLowerCase();
                }

                // remove leading 0s
                try {
                    voteValue = Integer.toString(Integer.parseInt(voteValue));
                } catch (NumberFormatException e) {
                }

                if (choicesMap.containsKey(voteValue)) {
                    voteByUsernameMap.put(userName, voteValue);
                }
            }
        }
    }

    public void startVote(String stateMessage) {
        JsonObject stateJson = new JsonParser().parse(stateMessage).getAsJsonObject();
        if (stateJson.has("available_commands")) {
            JsonArray availableCommandsArray = stateJson.get("available_commands").getAsJsonArray();

            Set<String> availableCommands = new HashSet<>();
            availableCommandsArray.forEach(command -> availableCommands.add(command.getAsString()));

            if (!inBattle) {
                if (availableCommands.contains("choose")) {
                    startChooseVote(stateJson);
                } else if (availableCommands.contains("play")) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    shouldStartClientOnUpdate = true;
                } else if (availableCommands.contains("start")) {
                    startCharacterVote(new JsonParser().parse(stateMessage).getAsJsonObject());
                } else if (availableCommands.contains("proceed")) {
                    String screenType = stateJson.get("game_state").getAsJsonObject()
                                                 .get("screen_type").getAsString();
                    delayProceed(screenType, stateMessage);
                } else if (availableCommands.contains("confirm")) {
                    System.err.println("choosing confirm");
                    readQueue.add("confirm");
                } else if (availableCommands.contains("leave")) {
                    // exit shop hell
                    readQueue.add("leave");
                    readQueue.add("proceed");
                }
            }
        }
    }

    public void startChooseVote(JsonObject stateJson) {
        if (stateJson.has("game_state")) {
            VoteType voteType = VoteType.OTHER;

            JsonObject gameState = stateJson.get("game_state").getAsJsonObject();
            String screenType = gameState.get("screen_type").getAsString();

            if (screenType != null) {
                if (screenType.equalsIgnoreCase("EVENT")) {
                    voteController = new EventVoteController(this, stateJson);
                } else if (screenType.equalsIgnoreCase("MAP")) {
                    if (FIRST_FLOOR_NUMS.contains(AbstractDungeon.floorNum)) {
                        voteType = VoteType.MAP_LONG;
                    } else if (NO_OPT_REST_SITE
                            .contains(AbstractDungeon.floorNum) && !AbstractDungeon.player.relics
                            .stream()
                            .anyMatch(relic -> relic instanceof WingBoots && relic.counter > 0)) {
                        voteType = VoteType.SKIP;
                    } else {
                        voteType = VoteType.MAP_SHORT;
                    }

                    voteController = new MapVoteController(this, stateJson);
                } else if (screenType.equalsIgnoreCase("SHOP_SCREEN")) {
                    voteController = new ShopScreenVoteController(this, stateJson);
                } else if (screenType.equalsIgnoreCase("CARD_REWARD")) {
                    if (AbstractDungeon.floorNum == 1) {
                        voteType = VoteType.CARD_SELECT_LONG;
                    } else {
                        voteType = VoteType.CARD_SELECT_SHORT;
                    }

                    voteController = new CardRewardVoteController(this, stateJson);
                } else if (screenType.equalsIgnoreCase("COMBAT_REWARD")) {
                    voteController = new CombatRewardVoteController(this, stateJson);
                } else if (screenType.equalsIgnoreCase("REST")) {
                    voteController = new RestVoteController(this, stateJson);
                } else if (screenType.equalsIgnoreCase("BOSS_REWARD")) {
                    voteController = new BossRewardVoteController(this, stateJson);
                } else if (screenType.equals("GRID")) {
                    voteController = new GridVoteController(this, stateJson);
                } else {
                    System.err.println("Starting generic vote for " + screenType);
                }
            }

            if (voteController != null) {
                voteController.setUpChoices();
            } else {
                setUpDefaultVoteOptions(stateJson);
            }

            choicesMap = new HashMap<>();
            for (Choice choice : viableChoices) {
                choicesMap.put(choice.voteString, choice);
            }

            startVote(voteType, stateJson.toString());
        } else {
            System.err.println("ERROR Missing game state");
        }
    }

    public void delayProceed(String screenType, String stateMessage) {
        choices = new ArrayList<>();

        choices.add(new Choice("proceed", "proceed", "proceed"));

        viableChoices = choices;

        choicesMap = new HashMap<>();
        for (Choice choice : viableChoices) {
            choicesMap.put(choice.voteString, choice);
        }

        VoteType voteType = VoteType.SKIP;

        if (screenType.equals("REST")) {
            voteType = VoteType.REST;
        } else if (screenType.equals("COMBAT_REWARD")) {
            voteType = VoteType.SKIP;
        } else if (screenType.equals("GAME_OVER")) {

            try {
                String fileName = String
                        .format("votelogs/gameover-%s.txt", System.currentTimeMillis());
                FileWriter writer = new FileWriter(fileName);
                writer.write(stateMessage);
                writer.close();

                // send game over stats to slayboard in another thread
                new Thread(() -> {
                    try {
                        Slayboard.postScore(stateMessage, voteFrequencies);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();


                JsonObject gameState = new JsonParser().parse(stateMessage).getAsJsonObject()
                                                       .get("game_state").getAsJsonObject();
                boolean reportedVictory = gameState.get("screen_state").getAsJsonObject()
                                                   .get("victory").getAsBoolean();
                int floor = gameState.get("floor").getAsInt();
                if (reportedVictory || floor > 51) {
                    optionsMap.put("asc", optionsMap.getOrDefault("asc", 0) + 1);
                    if (reportedVictory && floor > 51) {
                        // Heart kills get an extra life
                        optionsMap.put("lives", optionsMap.getOrDefault("lives", 0) + 1);
                    }
                } else {
                    optionsMap.put("lives", optionsMap.getOrDefault("lives", 0) - 1);
                }


                // Changes lives/ascension level
                if (optionsMap.getOrDefault("lives", 0) > 0) {
                    int lives = optionsMap.get("lives");
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

            switch (AbstractDungeon.screen) {
                case DEATH:
                    ReturnToMenuButton deathReturnButton = ReflectionHacks
                            .getPrivate(AbstractDungeon.deathScreen, GameOverScreen.class, "returnButton");
                    deathReturnButton.hb.clicked = true;
                    break;
                case VICTORY:
                    ReturnToMenuButton victoryReturnButton = ReflectionHacks
                            .getPrivate(AbstractDungeon.victoryScreen, GameOverScreen.class, "returnButton");
                    victoryReturnButton.hb.clicked = true;
                    break;
            }
            voteType = VoteType.GAME_OVER;
        } else {
            System.err.println("unknown screen type proceed timer " + screenType);
        }

        System.err.println("delaying for " + screenType + " " + voteType);

        startVote(voteType, true, "");
    }

    public void startCharacterVote(JsonObject stateJson) {
        choices = new ArrayList<>();

        choices.add(new Choice("ironclad", "1", "start ironclad"));
        choices.add(new Choice("silent", "2", "start silent"));
        choices.add(new Choice("defect", "3", "start defect"));
        choices.add(new Choice("watcher", "4", "start watcher"));
        choices.add(new Choice("marisa", "5", "start marisa"));

        viableChoices = choices;

        choicesMap = new HashMap<>();
        for (Choice choice : viableChoices) {
            choicesMap.put(choice.voteString, choice);
        }

        voteController = new CharacterVoteController(this, stateJson);

        voteFrequencies = new HashMap<>();

        startVote(VoteType.CHARACTER, "");
    }

    private void startVote(VoteType voteType, boolean forceWait, String stateString) {
        voteByUsernameMap = new HashMap<>();
        currentVote = voteType;
        voteEndTimeMillis = System.currentTimeMillis();
        this.stateString = stateString;

        if (viableChoices.isEmpty()) {
            viableChoices.add(new Choice("proceed", "proceed", "proceed"));
        }

        if (viableChoices.size() > 1 || forceWait) {
            voteEndTimeMillis += fastMode ? FAST_VOTE_TIME_MILLIS : optionsMap
                    .get(voteType.optionName);
        } else {
            voteEndTimeMillis += NO_VOTE_TIME_MILLIS;
        }
    }

    private void startVote(VoteType voteType, String stateString) {
        startVote(voteType, false, stateString);
    }

    @Override
    public void receivePostRender(SpriteBatch spriteBatch) {
        String topMessage = "";
        if (voteByUsernameMap != null && viableChoices != null && viableChoices.size() > 1) {
            if (voteController != null) {
                voteController.render(spriteBatch);
            } else {
                BitmapFont font = FontHelper.buttonLabelFont;
                String displayString = buildDisplayString();

                float timerMessageHeight = FontHelper.getHeight(font) * 5;

                FontHelper
                        .renderFont(spriteBatch, font, displayString, 15, Settings.HEIGHT * 7 / 8 - timerMessageHeight, Color.RED);
            }

            long remainingTime = voteEndTimeMillis - System.currentTimeMillis();

            topMessage += String
                    .format("Vote Time Remaining: %s", remainingTime / 1000 + 1);

        }
        if (fastMode) {
            topMessage += "\nDemo Mode (Random picks) type 07734 in chat to start playing";
        }

        if (!topMessage.isEmpty()) {
            BitmapFont font = FontHelper.buttonLabelFont;
            FontHelper
                    .renderFont(spriteBatch, font, topMessage, 15, Settings.HEIGHT * 7 / 8, Color.RED);
        }
    }

    private String buildDisplayString() {
        String result = "";
        HashMap<String, Integer> voteFrequencies = getVoteFrequencies();

        for (int i = 0; i < viableChoices.size(); i++) {
            Choice choice = viableChoices.get(i);

            result += String
                    .format("%s [vote %s] (%s)",
                            choice.choiceName,
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

            if (i < viableChoices.size() - 1) {
                result += "\n";
            }
        }

        return result;
    }

    private void startAiClient() {
        if (BattleAiMod.aiClient == null) {
            try {
                BattleAiMod.aiClient = new AiClient();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (BattleAiMod.aiClient != null) {
            BattleAiMod.aiClient.sendState(optionsMap.get("turns"));
        }
    }

    public static class Choice {
        final String choiceName;
        String voteString;
        Optional<RewardInfo> rewardInfo = Optional.empty();
        final ArrayList<String> resultCommands;

        public Choice(String choiceName, String voteString, String... resultCommands) {
            this.choiceName = choiceName;
            this.voteString = voteString;

            this.resultCommands = new ArrayList<>();
            for (String resultCommand : resultCommands) {
                this.resultCommands.add(resultCommand);
            }
        }

        @Override
        public String toString() {
            return "Choice{" +
                    "choiceName='" + choiceName + '\'' +
                    ", voteString='" + voteString + '\'' +
                    ", resultCommands=" + resultCommands +
                    '}';
        }
    }

    public static class RewardInfo {
        final String rewardType;
        String potionName;
        String relicName;

        RewardInfo(JsonObject rewardJson) {
            rewardType = rewardJson.get("reward_type").getAsString();
            if (rewardType.equals("POTION")) {
                potionName = rewardJson.get("potion").getAsJsonObject().get("name").getAsString();
            } else if (rewardType.equals("RELIC")) {
                relicName = rewardJson.get("relic").getAsJsonObject().get("name").getAsString();
            }
        }
    }

    public static HashSet<String> VOTE_PREFIXES = new HashSet<String>() {{
        add("!vote");
        add("vote");
    }};

    public static HashSet<Integer> FIRST_FLOOR_NUMS = new HashSet<Integer>() {{
        add(0);
        add(17);
        add(34);
    }};

    public static HashSet<Integer> NO_OPT_REST_SITE = new HashSet<Integer>() {{
        add(14);
        add(31);
        add(48);
    }};

    HashMap<String, Integer> getVoteFrequencies() {
        if (voteByUsernameMap == null) {
            return new HashMap<>();
        }

        HashMap<String, Integer> frequencies = new HashMap<>();

        voteByUsernameMap.entrySet().forEach(entry -> {
            String choice = entry.getValue();
            if (!frequencies.containsKey(choice)) {
                frequencies.put(choice, 0);
            }

            frequencies.put(choice, frequencies.get(choice) + 1);
        });

        return frequencies;
    }

    private Choice getVoteResult() {
        HashMap<String, Integer> frequencies = getVoteFrequencies();

        Set<Map.Entry<String, Integer>> entries = frequencies.entrySet();
        if (voteByUsernameMap.size() == 0) {
            if (viableChoices.size() > 1) {
                consecutiveNoVotes++;
                if (consecutiveNoVotes >= 5) {
                    fastMode = true;
                }

                System.err.println("choosing random for no votes");
            }

            int randomResult = new Random().nextInt(viableChoices.size());

            return viableChoices.get(randomResult);
        } else {
            consecutiveNoVotes = 0;
        }

        ArrayList<String> bestResults = new ArrayList<>();
        int bestRate = 0;

        for (Map.Entry<String, Integer> entry : entries) {
            if (entry.getValue() > bestRate) {
                bestResults = new ArrayList<>();
                bestResults.add(entry.getKey());
                bestRate = entry.getValue();
            } else if (entry.getValue() == bestRate) {
                bestResults.add(entry.getKey());
            }
        }
        String bestResult = bestResults.get(new Random().nextInt(bestResults.size()));

        if (!choicesMap.containsKey(bestResult.toLowerCase())) {
            System.err.println("choosing random for invalid votes " + bestResult);
            int randomResult = new Random().nextInt(viableChoices.size());
            return viableChoices.get(randomResult);
        }

        return choicesMap.get(bestResult.toLowerCase());
    }

    void setUpDefaultVoteOptions(JsonObject stateJson) {
        JsonObject gameState = stateJson.get("game_state").getAsJsonObject();
        JsonArray choicesJson = gameState.get("choice_list").getAsJsonArray();

        choices = new ArrayList<>();
        choicesJson.forEach(choice -> {
            String choiceString = choice.getAsString();
            String choiceCommand = String.format("choose %s", choices.size());

            // the voteString will start at 1
            String voteString = Integer.toString(choices.size() + 1);

            Choice toAdd = new Choice(choiceString, voteString, choiceCommand);
            choices.add(toAdd);
        });

        viableChoices = choices;

        if (screenType != null && screenType
                .equals("CHEST") && AbstractDungeon.player != null && AbstractDungeon.player
                .hasRelic(CursedKey.ID)) {
            twirk.channelMessage("[BOT] Cursed Key allows skipping relics, [vote 0] to skip, [vote 1] to open");
            viableChoices.add(new Choice("leave", "0", "leave", "proceed"));
        }
    }
}
