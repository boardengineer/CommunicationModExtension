package twitch;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.ui.buttons.SingingBowlButton;
import com.megacrit.cardcrawl.ui.buttons.SkipCardButton;

import java.util.HashMap;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class CardRewardVoteController extends VoteController {
    // Card reward rendering references
    private final HashMap<String, AbstractCard> messageToCardReward;
    private final TwitchController twitchController;
    private final JsonObject stateJson;

    CardRewardVoteController(TwitchController twitchController, JsonObject stateJson) {
        this.twitchController = twitchController;
        messageToCardReward = new HashMap<>();
        for (AbstractCard card : AbstractDungeon.cardRewardScreen.rewardGroup) {
            messageToCardReward.put(card.name.toLowerCase(), card);
        }

        this.stateJson = stateJson;
    }

    @Override
    public void setUpChoices() {
        twitchController.setUpDefaultVoteOptions(stateJson);

        boolean skippable = ReflectionHacks
                .getPrivate(AbstractDungeon.cardRewardScreen, CardRewardScreen.class, "skippable");

        if (skippable) {
            if (twitchController.skipAfterCard) {
                twitchController.viableChoices
                        .add(new TwitchController.Choice("Skip", "0", "skip", "proceed"));
            } else {
                twitchController.viableChoices
                        .add(new TwitchController.Choice("Skip", "0", "skip"));
            }
        }
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

            String message = choice.choiceName;
            if (message.equalsIgnoreCase("skip") && !AbstractDungeon.player
                    .hasRelic("PrismaticBranch")) {
                String skipMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                SkipCardButton skipCardButton = ReflectionHacks
                        .getPrivate(AbstractDungeon.cardRewardScreen, CardRewardScreen.class, "skipButton");

                renderTextBelowHitbox(spriteBatch, skipMessage, cardRewardAdjust(skipCardButton.hb));
            } else if (message.equalsIgnoreCase("bowl")) {
                String bowlMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                SingingBowlButton bowlButton = ReflectionHacks
                        .getPrivate(AbstractDungeon.cardRewardScreen, CardRewardScreen.class, "bowlButton");

                renderTextBelowHitbox(spriteBatch, bowlMessage, cardRewardAdjust(bowlButton.hb));
            } else if (messageToCardReward.containsKey(message)) {
                AbstractCard card = messageToCardReward.get(message);
                Hitbox cardHitbox = card.hb;
                String cardMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                renderTextBelowHitbox(spriteBatch, cardMessage, cardRewardAdjust(cardHitbox));
            } else {
//                System.err.println("no card button for " + choice.choiceName);
            }
        }
    }

    @Override
    public void endVote() {

    }

    private static Hitbox cardRewardAdjust(Hitbox hitbox) {
        return new Hitbox(hitbox.x, hitbox.y - 15.0F * Settings.scale, hitbox.width, hitbox.height + 25 * Settings.scale);
    }
}
