package twitch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import java.util.HashMap;
import java.util.Locale;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class BossRewardVoteController implements VoteController {
    private final TwitchController twitchController;
    private final HashMap<String, AbstractRelic> messageToBossRelicMap;

    BossRewardVoteController(TwitchController twitchController) {
        this.twitchController = twitchController;

        messageToBossRelicMap = new HashMap<>();

        for (AbstractRelic relic : AbstractDungeon.bossRelicScreen.relics) {
            messageToBossRelicMap.put(relic.name.toLowerCase(), relic);
        }
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

            String message = choice.choiceName.toLowerCase(Locale.ROOT);
            if (messageToBossRelicMap.containsKey(message)) {
                AbstractRelic rewardItem = messageToBossRelicMap.get(message);
                Hitbox rewardItemHitbox = rewardItem.hb;
                String rewardItemMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                renderTextBelowHitbox(spriteBatch, rewardItemMessage, rewardItemHitbox);
            } else {
                System.err.println("no boss relic button for " + choice.choiceName);
            }
        }
    }
}
