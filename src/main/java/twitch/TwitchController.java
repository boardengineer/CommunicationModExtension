package twitch;

import basemod.interfaces.PostRenderSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import battleaimod.BattleAiMod;
import battleaimod.networking.AiClient;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.potions.PotionSlot;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class TwitchController implements PostUpdateSubscriber, PostRenderSubscriber {
    private static final long NO_VOTE_TIME_MILLIS = 1_000;
    private static final long FAST_VOTE_TIME_MILLIS = 3_000;
    private static final long NORMAL_VOTE_TIME_MILLIS = 15_000;


    /**
     * Used to count user votes during
     */
    private HashMap<String, String> voteByUsernameMap = null;

    private long voteEndTimeMillis;

    private ArrayList<Choice> choices;
    private ArrayList<Choice> viableChoices;
    private HashMap<String, Choice> choicesMap;

    LinkedBlockingQueue<String> readQueue;

    private boolean shouldStartClientOnUpdate = false;
    private boolean inBattle = false;
    private boolean fastMode = true;
    int consecutiveNoVotes = 0;

    public TwitchController(LinkedBlockingQueue<String> readQueue) {
        this.readQueue = readQueue;
    }

    @Override
    public void receivePostUpdate() {
        if (shouldStartClientOnUpdate) {
            shouldStartClientOnUpdate = false;
            inBattle = true;
            startAiClient();
        }

        if (BattleAiMod.battleAiController != null) {
            if (BattleAiMod.battleAiController.isDone) {
                BattleAiMod.battleAiController = null;
                inBattle = false;
            }
        }

        if (voteByUsernameMap != null) {
            long timeRemaining = voteEndTimeMillis - System.currentTimeMillis();

            if (timeRemaining <= 0) {
                Choice result = getVoteResult();

                for (String command : result.resultCommands) {
                    readQueue.add(command);
                }

                voteByUsernameMap = null;
            }
        }
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
            }
            int randomResult = new Random().nextInt(viableChoices.size());
            return viableChoices.get(randomResult);
        } else {
            consecutiveNoVotes = 0;
        }

        String bestResult = "";
        int bestRate = 0;

        for (Map.Entry<String, Integer> entry : entries) {
            if (entry.getValue() > bestRate) {
                bestResult = entry.getKey();
                bestRate = entry.getValue();
            }
        }

        if (!choicesMap.containsKey(bestResult)) {
            int randomResult = new Random().nextInt(viableChoices.size());
            return viableChoices.get(randomResult);
        }

        return choicesMap.get(bestResult);
    }

    private HashMap<String, Integer> getVoteFrequencies() {
        if (voteByUsernameMap == null) {
            return new HashMap<>();
        }

        HashMap<String, Integer> frequenceies = new HashMap<>();

        voteByUsernameMap.entrySet().forEach(entry -> {
            String choice = entry.getValue();
            if (!frequenceies.containsKey(choice)) {
                frequenceies.put(choice, 0);
            }

            frequenceies.put(choice, frequenceies.get(choice) + 1);
        });

        return frequenceies;
    }

    public void receiveMessage(String userName, String message) {
        String[] tokens = message.split(" ");
        if (tokens.length >= 2 && tokens[0].equals("!set")) {
            if (tokens[1].equals("slow")) {
                fastMode = false;
                consecutiveNoVotes = 0;
            }
        }

        if (userName.equals("boardengineer")) {
            System.err.println("pasha wants something");
            if (tokens.length >= 2 && tokens[0].equals("!admin")) {
                String command = message.substring(message.indexOf(' ') + 1);
                readQueue.add(command);

                System.err.println(command);
            }
        }

        if (voteByUsernameMap != null) {
            if (tokens.length >= 2 && tokens[0].equals("!vote")) {
                voteByUsernameMap.put(userName, tokens[1]);
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
                    readQueue.add("start ironclad");
                } else if (availableCommands.contains("proceed")) {
                    readQueue.add("proceed");
                } else if (availableCommands.contains("confirm")) {
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
            JsonArray choicesJson = stateJson.get("game_state").getAsJsonObject().get("choice_list")
                                             .getAsJsonArray();
            choices = new ArrayList<>();
            choicesJson.forEach(choice -> {
                String choiceString = choice.getAsString();
                String choiceCommand = String.format("choose %s", choices.size());
                Choice toAdd = new Choice(choiceString, Integer
                        .toString(choices.size()), choiceCommand);
                choices.add(toAdd);

            });
            viableChoices = getTrueChoices();

            choicesMap = new HashMap<>();
            for (Choice choice : viableChoices) {
                choicesMap.put(choice.voteString, choice);
            }

            voteByUsernameMap = new HashMap<>();
            voteEndTimeMillis = System.currentTimeMillis();
            if (viableChoices.isEmpty()) {
                viableChoices.add(new Choice("proceed", "proceed", "proceed"));
            }

            if (viableChoices.size() > 1) {
                voteEndTimeMillis += fastMode ? FAST_VOTE_TIME_MILLIS : NORMAL_VOTE_TIME_MILLIS;
            } else {
                voteEndTimeMillis += NO_VOTE_TIME_MILLIS;
            }
        } else {
            System.err.println("ERROR Missing game state");
        }
    }

    @Override
    public void receivePostRender(SpriteBatch spriteBatch) {
        if (voteByUsernameMap != null && viableChoices != null && viableChoices.size() > 1) {
            int boxWidth = Settings.WIDTH / 3;
            int boxHeight = viableChoices.size() * 50;

            int boxX = Settings.WIDTH - boxWidth - 75;

            spriteBatch.draw(ImageMaster.WHITE_SQUARE_IMG, boxX, 10, boxWidth, boxHeight);
            FontHelper
                    .renderFont(spriteBatch, FontHelper.menuBannerFont, buildDisplayString(), boxX, boxHeight, Color.RED);

            long remainingTime = voteEndTimeMillis - System.currentTimeMillis();

            String timeMessage = String
                    .format("Vote Time Remaining: %s", remainingTime / 1000 + 1);
            if(fastMode) {
                timeMessage += "\nFast Mode active [!set slow] for more time";
            }

            FontHelper
                    .renderFont(spriteBatch, FontHelper.menuBannerFont, timeMessage, Settings.WIDTH * 3 / 5, Settings.HEIGHT - 37, Color.RED);
        }
    }

    private String buildDisplayString() {
        String result = "";
        HashMap<String, Integer> voteFrequencies = getVoteFrequencies();

        for (int i = 0; i < viableChoices.size(); i++) {
            Choice choice = viableChoices.get(i);

            result += String
                    .format("%s [!vote %s] (%s)",
                            choice.choiceName,
                            choice.voteString,
                            voteFrequencies.getOrDefault(Integer.toString(i), 0));

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
            BattleAiMod.aiClient.sendState();
        }
    }

    private ArrayList<Choice> getTrueChoices() {
        ArrayList<Choice> result = new ArrayList<>();

        boolean canTakePotion = AbstractDungeon.player.potions.stream()
                                                              .anyMatch(potion -> potion instanceof PotionSlot);

        choices.stream().filter(choice -> canTakePotion || !choice.choiceName.toLowerCase()
                                                                             .contains("potion"))
               .forEach(choice -> result.add(choice));

        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SHOP) {
            result.add(new Choice("leave", "leave", "leave", "proceed"));
        } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD) {
            Optional<Choice> goldChoice = result.stream()
                                                .filter(choice -> choice.choiceName.equals("gold"))
                                                .findAny();
            if (goldChoice.isPresent()) {
                ArrayList<Choice> onlyGold = new ArrayList<>();
                onlyGold.add(goldChoice.get());

                // In the reward screen, always take the gold first if the option exists
                return onlyGold;
            }

            Optional<Choice> potionChoice = result.stream()
                                                  .filter(choice -> choice.choiceName.toLowerCase()
                                                                                     .contains("potion"))
                                                  .findAny();

            if (potionChoice.isPresent()) {
                ArrayList<Choice> onlyPotion = new ArrayList<>();
                onlyPotion.add(potionChoice.get());

                // Then the potion
                return onlyPotion;
            }
        }

        return result;
    }

    public static class Choice {
        final String choiceName;
        final String voteString;
        final ArrayList<String> resultCommands;

        public Choice(String choiceName, String voteString, String... resultCommands) {
            this.choiceName = choiceName;
            this.voteString = voteString;

            this.resultCommands = new ArrayList<>();
            for (String resultCommand : resultCommands) {
                this.resultCommands.add(resultCommand);
            }
        }
    }
}
