package twitch;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.ui.buttons.ProceedButton;

import java.util.HashMap;

public class CombatRewardVoteController implements VoteController {
    private final HashMap<String, RewardItem> messageToCombatRewardItem;
    private final HashMap<String, String> messageToOriginalRewardTextMap;
    private final TwitchController twitchController;
    private final JsonObject stateJson;

    CombatRewardVoteController(TwitchController twitchController, JsonObject stateJson) {
        this.twitchController = twitchController;
        this.stateJson = stateJson;

        JsonObject gameState = stateJson.get("game_state").getAsJsonObject();
        JsonArray choicesJson = gameState.get("choice_list").getAsJsonArray();

        messageToCombatRewardItem = new HashMap<>();
        messageToOriginalRewardTextMap = new HashMap<>();

        for (int i = 0; i < choicesJson.size(); i++) {
            String voteString = Integer.toString(i + 1);

            RewardItem reward = AbstractDungeon.combatRewardScreen.rewards.get(i);

            messageToCombatRewardItem.put(voteString, reward);
            messageToOriginalRewardTextMap.put(voteString, reward.text);
        }
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

            String message = choice.choiceName;
            if (message.equals("leave")) {
                String leaveMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                RenderHelpers
                        .renderTextBelowHitbox(spriteBatch, leaveMessage, ReflectionHacks
                                .getPrivate(AbstractDungeon.overlayMenu.proceedButton, ProceedButton.class, "hb"));
            } else if (messageToCombatRewardItem.containsKey(choice.voteString)) {
                RewardItem rewardItem = messageToCombatRewardItem.get(choice.voteString);
                String rewardItemMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                rewardItem.text = messageToOriginalRewardTextMap
                        .get(choice.voteString) + rewardItemMessage;
            } else {
                System.err.println("no card button for " + choice.choiceName);
            }
        }
    }

    @Override
    public void endVote() {
        // Reset the text to avoid duped vote strings
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);
            if (messageToCombatRewardItem.containsKey(choice.voteString)) {
                RewardItem rewardItem = messageToCombatRewardItem.get(choice.voteString);
                rewardItem.text = messageToOriginalRewardTextMap.get(choice.voteString);
            }
        }
    }
}
