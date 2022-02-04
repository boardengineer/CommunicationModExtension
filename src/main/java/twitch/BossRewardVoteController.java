package twitch;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;

import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class BossRewardVoteController extends VoteController {
    private final TwitchController twitchController;
    private final HashMap<String, AbstractRelic> messageToBossRelicMap;
    private final JsonObject stateJson;

    BossRewardVoteController(TwitchController twitchController, JsonObject stateJson) {
        this.twitchController = twitchController;

        messageToBossRelicMap = new HashMap<>();

        for (AbstractRelic relic : AbstractDungeon.bossRelicScreen.relics) {
            messageToBossRelicMap.put(relic.name.toLowerCase(), relic);
        }

        this.stateJson = stateJson;
    }

    @Override
    public void setUpChoices() {
        twitchController.setUpDefaultVoteOptions(stateJson);

        twitchController.viableChoices
                .add(new TwitchController.Choice("Skip", "0", "skip", "proceed"));
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        Set<String> winningResults = twitchController.getBestVoteResultKeys();

        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

            Color messageColor = winningResults
                    .contains(choice.voteString) ? Color.YELLOW : Color.RED;

            String message = choice.choiceName.toLowerCase(Locale.ROOT);
            if (messageToBossRelicMap.containsKey(message)) {
                AbstractRelic rewardItem = messageToBossRelicMap.get(message);
                Hitbox rewardItemHitbox = rewardItem.hb;
                String rewardItemMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                renderTextBelowHitbox(spriteBatch, rewardItemMessage, rewardItemHitbox, messageColor);
            } else if (message.equalsIgnoreCase("skip")) {
                String skipMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                MenuCancelButton cancelButton = ReflectionHacks
                        .getPrivate(AbstractDungeon.bossRelicScreen, BossRelicSelectScreen.class, "cancelButton");

                renderTextBelowHitbox(spriteBatch, skipMessage, cancelButton.hb, messageColor);
            } else {
                System.err.println("no boss relic button for " + choice.choiceName);
            }
        }
    }

    @Override
    public void endVote() {

    }
}
