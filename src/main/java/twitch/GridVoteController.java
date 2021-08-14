package twitch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;

import java.util.HashMap;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class GridVoteController extends VoteController {
    private final TwitchController twitchController;
    private final HashMap<String, AbstractCard> voteStringToCardMap;
    private final JsonObject stateJson;

    GridVoteController(TwitchController twitchController, JsonObject stateJson) {
        this.twitchController = twitchController;

        voteStringToCardMap = new HashMap<>();
        for (int i = 0; i < AbstractDungeon.gridSelectScreen.targetGroup.group.size(); i++) {
            String voteString = Integer.toString(i + 1);

            voteStringToCardMap
                    .put(voteString, AbstractDungeon.gridSelectScreen.targetGroup.group.get(i));
        }

        this.stateJson = stateJson;
    }

    @Override
    public void setUpChoices() {
        twitchController.setUpDefaultVoteOptions(stateJson);
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);
            String message = choice.voteString;
            if (voteStringToCardMap.containsKey(message)) {
                AbstractCard card = voteStringToCardMap.get(message);
                Hitbox hitbox = new Hitbox(card.current_x - 25, card.current_y - 110, 50, 50);

                String voteMessage = String.format("[vote %s](%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                renderTextBelowHitbox(spriteBatch, voteMessage, hitbox);
            } else {
                System.err.println("no event button for " + choice.choiceName);
            }
        }
    }

    @Override
    public void endVote() {

    }
}
