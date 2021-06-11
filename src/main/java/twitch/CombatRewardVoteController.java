package twitch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;

import java.util.HashMap;

public class CombatRewardVoteController implements VoteController {
    private final HashMap<String, RewardItem> messageToCombatRewardItem;
    private final HashMap<String, String> messageToOriginalRewardTextMap;
    private final TwitchController twitchController;

    CombatRewardVoteController(TwitchController twitchController) {
        this.twitchController = twitchController;

        messageToCombatRewardItem = new HashMap<>();
        messageToOriginalRewardTextMap = new HashMap<>();
        for (RewardItem reward : AbstractDungeon.combatRewardScreen.rewards) {
            messageToCombatRewardItem.put(reward.type.name().toLowerCase(), reward);
            messageToOriginalRewardTextMap
                    .put(reward.type.name().toLowerCase(), reward.text);
        }
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

            String message = choice.choiceName;
            if (messageToCombatRewardItem.containsKey(message)) {
                RewardItem rewardItem = messageToCombatRewardItem.get(message);
                String rewardItemMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                rewardItem.text = messageToOriginalRewardTextMap
                        .get(message) + rewardItemMessage;

            } else {
                System.err.println("no card button for " + choice.choiceName);
            }
        }
    }
}
