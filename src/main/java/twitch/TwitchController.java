package twitch;

import basemod.ReflectionHacks;
import basemod.interfaces.PostRenderSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import battleaimod.BattleAiMod;
import battleaimod.networking.AiClient;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.types.users.TwitchUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.relics.CursedKey;
import com.megacrit.cardcrawl.relics.FrozenEye;
import com.megacrit.cardcrawl.relics.RunicDome;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.screens.GameOverScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.ui.buttons.ReturnToMenuButton;
import communicationmod.CommunicationMod;
import ludicrousspeed.LudicrousSpeedMod;
import ludicrousspeed.simulator.commands.Command;
import savestate.SaveState;
import twitch.games.ClimbGameController;
import twitch.votecontrollers.*;

import java.io.IOException;
import java.util.*;

public class TwitchController implements PostUpdateSubscriber, PostRenderSubscriber {
    private static final long NO_VOTE_TIME_MILLIS = 1_000;
    private static final long RECALL_VOTE_TIME_MILLIS = 2_500;
    private static final long FAST_VOTE_TIME_MILLIS = 3_000;

    public static int runId = 0;

    private static Queue<Integer> recallQueue;

    public enum VoteType {
        // THe first vote in each dungeon
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

    private String screenType = null;
    public static VoteController voteController;

    private final BetaArtController betaArtController;
    private final CheeseController cheeseController;
    public static GameController gameController;
    public QueryController queryController;

    public static HashMap<String, Integer> optionsMap;

    private boolean inVote = false;
    private long voteEndTimeMillis;

    public ArrayList<Choice> choices;
    public ArrayList<Choice> viableChoices;
    public HashMap<String, Choice> choicesMap;

    public static Twirk twirk;

    private boolean shouldStartClientOnUpdate = false;
    public static boolean inBattle = false;
    private boolean fastMode = false;
    int consecutiveNoVotes = 0;
    public boolean skipAfterCard = true;

    private int previousLevel = -1;
    private static boolean isActive = false;

    public TwitchApiController apiController;

    public SpireConfig optionsConfig;

    public Optional<PredictionInfo> currentPrediction = Optional.empty();

    public TwitchController(Twirk twirk) {
        this.betaArtController = new BetaArtController(this);
        this.cheeseController = new CheeseController(this);
        try {
            optionsConfig = new SpireConfig("CommModExtension", "options");

            TwitchController.twirk = twirk;
            try {
                apiController = new TwitchApiController();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.queryController = new QueryController(this);
            twitch.votecontrollers.CharacterVoteController.initializePortraits();

            optionsMap = new HashMap<>();

            optionsMap.put("recall", 0);
            optionsMap.put("turns", 15_000);
            optionsMap.put("verbose", 1);

            for (VoteType voteType : VoteType.values()) {
                optionsMap.put(voteType.optionName, voteType.defaultTime);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        gameController = new ClimbGameController(this);
    }


    @Override
    public void receivePostUpdate() {
        if (shouldStartClientOnUpdate) {
            shouldStartClientOnUpdate = false;
            inBattle = true;
            startAiClient();
        }

        betaArtController.update();
        cheeseController.update();

        // The Ai Client has stopped simulation.  Hand control back to the Twitch interface.
        if (BattleAiMod.rerunController != null || LudicrousSpeedMod.mustRestart) {
            if (BattleAiMod.rerunController.isDone || LudicrousSpeedMod.mustRestart) {
                LudicrousSpeedMod.controller = BattleAiMod.rerunController = null;
                inBattle = false;

                if (LudicrousSpeedMod.mustRestart) {
                    System.err.println("Desync detected, rerunning simluation");
                    LudicrousSpeedMod.mustRestart = false;
//                    startAiClient();
                }
            }
        }

        try {
            if (voteByUsernameMap != null && inVote && isVoteOver()) {
                inVote = false;
                resolveVote();
            }
        } catch (ConcurrentModificationException | NullPointerException e) {
            System.err.println("Null pointer caught, clean up this crap");
            e.printStackTrace();
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
                CommunicationMod.queueCommand(command);
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
                    saveOptionsConfig();
                } else if (tokens[1].equals("disable")) {
                    voteByUsernameMap = null;
                    inBattle = false;
                    isActive = false;
                } else if (tokens[1].equals("enable")) {
                    isActive = true;
                } else if (tokens[1].equals("recall")) {
                    System.err.println("starting recall");
                    voteByUsernameMap = null;
                    inBattle = false;
                    optionsMap.put("recall", 1);
                    previousLevel = 0;

                    if (tokens.length >= 3) {
                        new Thread(() -> {
                            try {
                                recallQueue = new LinkedList<>();
                                Arrays.stream(tokens[2].split(","))
                                      .forEach(runId -> recallQueue.add(Integer.parseInt(runId)));
                                runId = recallQueue.poll();
                                String command = Slayboard.queryRunCommand(runId);
                                CommunicationMod.queueCommand(command);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }


                }
            }
        }

        queryController.maybeRunQuery(tokens);

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
                    try {
                        voteByUsernameMap.put(userName, voteValue);
                    } catch (ConcurrentModificationException e) {
                        System.err.println("Skipping user vote");
                    }
                }
            }
        }
    }

    public void startVote(String stateMessage) {
        if (!isActive) {
            return;
        }

        JsonObject stateJson = new JsonParser().parse(stateMessage).getAsJsonObject();
        if (stateJson.has("available_commands")) {
            JsonArray availableCommandsArray = stateJson.get("available_commands").getAsJsonArray();

            Set<String> availableCommands = new HashSet<>();
            availableCommandsArray.forEach(command -> availableCommands.add(command.getAsString()));

            if (!inBattle) {
                if (stateJson.has("game_state")) {
                    JsonObject gameState = stateJson.get("game_state").getAsJsonObject();
                    screenType = gameState.get("screen_type").getAsString();
                    if (screenType != null) {

                        if (screenType.equalsIgnoreCase("COMBAT_REWARD")) {
                            if (AbstractDungeon
                                    .getCurrRoom() instanceof ShopRoom && AbstractDungeon.combatRewardScreen.rewards
                                    .isEmpty()) {
                                CommunicationMod.queueCommand("cancel");
                            }
                        }
                    }
                }

                if (availableCommands.contains("choose")) {
                    startChooseVote(stateJson);
                } else if (availableCommands.contains("play")) {
                    // BATTLE STARTS HERE
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        shouldStartClientOnUpdate = true;
                    }).start();
                } else if (availableCommands.contains("start")) {
                    startCharacterVote(new JsonParser().parse(stateMessage).getAsJsonObject());
                } else if (availableCommands.contains("proceed")) {
                    String screenType = stateJson.get("game_state").getAsJsonObject()
                                                 .get("screen_type").getAsString();
                    delayProceed(screenType, stateMessage);
                } else if (availableCommands.contains("confirm")) {
                    System.err.println("choosing confirm");
                    CommunicationMod.queueCommand("confirm");
                } else if (availableCommands.contains("leave")) {
                    // exit shop hell
                    CommunicationMod.queueCommand("leave");
                    CommunicationMod.queueCommand("proceed");
                }
            }
        }
    }

    public void startChooseVote(JsonObject stateJson) {
        if (stateJson.has("game_state")) {
            JsonObject gameState = stateJson.get("game_state").getAsJsonObject();
            screenType = gameState.get("screen_type").getAsString();

            if (screenType != null) {
                if (screenType.equalsIgnoreCase("EVENT")) {
                    voteController = new EventVoteController(this, stateJson);
                } else if (screenType.equalsIgnoreCase("MAP")) {
                    voteController = new MapVoteController(this, stateJson);
                } else if (screenType.equalsIgnoreCase("SHOP_SCREEN")) {
                    voteController = new ShopScreenVoteController(this, stateJson);
                } else if (screenType.equalsIgnoreCase("CARD_REWARD")) {
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

            long voteTime = optionsMap.get("other");
            if (voteController != null) {
                voteController.setUpChoices();
                voteTime = voteController.getVoteTimerMillis();
            } else {
                setUpDefaultVoteOptions(stateJson);
            }

            choicesMap = new HashMap<>();
            for (Choice choice : viableChoices) {
                choicesMap.put(choice.voteString, choice);
            }

            startVote(voteTime, stateJson.toString());
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


        if (screenType.equals("GAME_OVER")) {
            JsonObject gameState = new JsonParser().parse(stateMessage).getAsJsonObject()
                                                   .get("game_state").getAsJsonObject();

            gameController.reportGameOver(gameState);

            boolean reportedVictory = gameState.get("screen_state").getAsJsonObject()
                                               .get("victory").getAsBoolean();
            int floor = gameState.get("floor").getAsInt();
            boolean didClimb = reportedVictory || floor > 51;

            if (currentPrediction.isPresent()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(3_000);
                        apiController.resolvePrediction(currentPrediction.get(), didClimb);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
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
        } else {
            System.err.println("unknown screen type proceed timer " + screenType);
        }

        startVote(optionsMap.get("skip"), true, "");
    }

    public void startCharacterVote(JsonObject stateJson) {
        voteController = new CharacterVoteController(this, stateJson);

        voteController.setUpChoices();

        voteFrequencies = new HashMap<>();

        startVote(voteController.getVoteTimerMillis(), "");

        CardCrawlGame.mode = CardCrawlGame.GameMode.CHAR_SELECT;
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.CHAR_SELECT;
    }

    private void startVote(long voteTimeMillis, boolean forceWait, String stateString) {
        voteByUsernameMap = new HashMap<>();
        voteEndTimeMillis = System.currentTimeMillis();
        long voteStart = System.currentTimeMillis();

        if (viableChoices.isEmpty()) {
            viableChoices.add(new Choice("proceed", "proceed", "proceed"));
        }

        if (!shouldRecall() && optionsMap.getOrDefault("verbose", 0) > 0) {
            if (voteController != null) {
                Optional<String> message = voteController.getTipString();

                if (message.isPresent()) {
                    twirk.channelMessage("[BOT] " + message.get());
                }
            }

            if (voteController != null) {
                voteController.sendVoteMessage();
            }
        }

        if (shouldRecall()) {
            voteStart += viableChoices.size() > 1 ? RECALL_VOTE_TIME_MILLIS : 250L;
        } else {
            if (viableChoices.size() > 1 || forceWait) {
                voteStart += fastMode ? FAST_VOTE_TIME_MILLIS : voteTimeMillis;
            } else {
                voteStart += NO_VOTE_TIME_MILLIS;
            }
        }
        voteEndTimeMillis = voteStart;
        inVote = true;
    }

    private void startVote(long voteTimeMillis, String stateString) {
        startVote(voteTimeMillis, false, stateString);
    }

    @Override
    public void receivePostRender(SpriteBatch spriteBatch) {
        String topMessage = "";
        if (voteByUsernameMap != null && viableChoices != null && viableChoices
                .size() > 1 && !isVoteOver()) {
            if (voteController != null) {
                try {
                    voteController.render(spriteBatch);
                } catch (ConcurrentModificationException e) {
                    System.err.println("Error: Skipping rendering because of concurrent error");
                }
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

        if (!topMessage.isEmpty() && !shouldRecall()) {
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
        if (!shouldRecall()) {
            if (BattleAiMod.aiClient == null) {
                try {
                    BattleAiMod.aiClient = new AiClient();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            if (BattleAiMod.aiClient != null) {
                int numTurns = optionsMap.get("turns");
                if (AbstractDungeon.player.hasRelic(RunicDome.ID)) {
                    numTurns /= 2;
                }

                if (AbstractDungeon.player.hasRelic(FrozenEye.ID)) {
                    numTurns = numTurns + numTurns / 2;
                }

                BattleAiMod.aiClient.sendState(numTurns);
                SaveState toSend = new SaveState();
                // send game over stats to slayboard in another thread
//                new Thread(() -> {
//                    try {
//                        Slayboard.postBattleState(toSend.encode(), runId);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }).start();
            }
        } else {
            try {
                int floorResultId = Slayboard.queryFloorResult(AbstractDungeon.floorNum, runId)
                                             .get(0);
                List<Command> commands = Slayboard.queryBattleCommandResult(floorResultId);

                BattleAiMod.aiClient = new AiClient(false);
                BattleAiMod.aiClient.runQueriedCommands(commands);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Choice {
        public String choiceName;
        public String voteString;
        public Optional<RewardInfo> rewardInfo = Optional.empty();
        public final ArrayList<String> resultCommands;

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
        public final String rewardType;
        public String potionName = null;
        public String relicName = null;

        public RewardInfo(JsonObject rewardJson) {
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

    public static HashSet<Integer> NO_OPT_REST_SITE_FLOORS = new HashSet<Integer>() {{
        add(14);
        add(31);
        add(48);
    }};

    public static HashSet<Integer> BOSS_CHEST_FLOOR_NUMS = new HashSet<Integer>() {{
        add(17);
        add(34);
    }};

    public HashMap<String, Integer> getVoteFrequencies() {
        if (voteByUsernameMap == null) {
            return new HashMap<>();
        }

        HashMap<String, Integer> frequencies = new HashMap<>();

        // Concurrency error here
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
        Set<String> bestResults = getBestVoteResultKeys();

        if (bestResults.size() == 0) {
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

        Iterator<String> resultFinder = bestResults.iterator();
        int resultIndex = new Random().nextInt(bestResults.size());
        for (int i = 0; i < resultIndex; i++) {
            resultFinder.next();
        }
        String bestResult = resultFinder.next();

        if (!choicesMap.containsKey(bestResult.toLowerCase())) {
            System.err.println("choosing random for invalid votes " + bestResult);
            int randomResult = new Random().nextInt(viableChoices.size());
            return viableChoices.get(randomResult);
        }

        return choicesMap.get(bestResult.toLowerCase());
    }

    public Set<String> getBestVoteResultKeys() {
        HashMap<String, Integer> frequencies = getVoteFrequencies();
        HashSet<String> result = new HashSet<>();

        Set<Map.Entry<String, Integer>> entries = frequencies.entrySet();
        int bestRate = 0;

        for (Map.Entry<String, Integer> entry : entries) {
            if (entry.getValue() > bestRate) {
                result = new HashSet<>();
                result.add(entry.getKey());
                bestRate = entry.getValue();
            } else if (bestRate > 0 && entry.getValue() == bestRate) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    public void setUpDefaultVoteOptions(JsonObject stateJson) {
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

        // TODO separate into a separate voting controller class
        if (!isBossFloor() && screenType != null && screenType
                .equals("CHEST") && AbstractDungeon.player != null && AbstractDungeon.player
                .hasRelic(CursedKey.ID)) {
            twirk.channelMessage("[BOT] Cursed Key allows skipping relics, [vote 0] to skip, [vote 1] to open");
            viableChoices.add(new Choice("leave", "0", "leave", "proceed"));
        }
    }

    private static boolean isBossFloor() {
        return BOSS_CHEST_FLOOR_NUMS.contains(AbstractDungeon.floorNum);
    }

    public static boolean shouldRecall() {
        return optionsMap.get("recall") != 0;
    }

    public static void enable() {
        isActive = true;
    }

    public static void disable() {
        isActive = false;
    }

    public static void battleRestart() {
        boolean wasInBattle = inBattle;
        inBattle = false;
        if (wasInBattle) {
            CommunicationMod.mustSendGameState = true;
        }
    }

    /**
     * Writes the beta table back info the spire config
     */
    public void saveOptionsConfig() {
        new Thread(() -> {
            JsonObject toWrite = new JsonObject();
            for (Map.Entry<String, Integer> entry : optionsMap.entrySet()) {
                optionsConfig.setString(entry.getKey(), Integer.toString(entry.getValue()));
            }

            try {
                optionsConfig.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private boolean isVoteOver() {
        return System.currentTimeMillis() > voteEndTimeMillis;
    }

    private void resolveVote() {
        if (AbstractDungeon.floorNum != previousLevel) {
            previousLevel = AbstractDungeon.floorNum;
        }

        voteByUsernameMap.keySet().forEach(userName -> {
            if (!voteFrequencies.containsKey(userName)) {
                voteFrequencies.put(userName, 0);
            }
            voteFrequencies.put(userName, voteFrequencies.get(userName) + 1);
        });

        Choice result;

        result = getVoteResult();

        if (voteController != null) {
            voteController.endVote(result);
        }

        boolean shouldChannelMessageForRecall = viableChoices
                .size() > 1 && shouldRecall();
        if (!voteByUsernameMap.isEmpty() || shouldChannelMessageForRecall) {
            twirk.channelMessage(String
                    .format("[BOT] selected %s | %s", result.voteString, result.choiceName));
        }

        for (String command : result.resultCommands) {
            boolean isCharacterVote = voteController != null &&
                    voteController instanceof CharacterVoteController;

            if (isCharacterVote && result.resultCommands.size() == 1) {
                int ascension = gameController.getAscension();

                if (ascension > 0) {
                    String seedString = SeedHelper.getString(new Random().nextLong());

                    command += String
                            .format(" %d %s", gameController.getAscension(), seedString);
                }
            }
            CommunicationMod.queueCommand(command);
        }

        voteByUsernameMap = null;
        voteController = null;
        screenType = null;
    }
}
