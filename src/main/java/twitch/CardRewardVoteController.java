package twitch;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.ui.buttons.SingingBowlButton;
import com.megacrit.cardcrawl.ui.buttons.SkipCardButton;

import java.util.HashMap;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class CardRewardVoteController implements VoteController {
    // Card reward rendering references
    private final HashMap<String, AbstractCard> messageToCardReward;

    private final TwitchController twitchController;

    CardRewardVoteController(TwitchController twitchController) {
        this.twitchController = twitchController;
        messageToCardReward = new HashMap<>();
        for (AbstractCard card : AbstractDungeon.cardRewardScreen.rewardGroup) {
            messageToCardReward.put(card.name.toLowerCase(), card);
        }
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

            String message = choice.choiceName;
            if (message.equalsIgnoreCase("skip")) {
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
                System.err.println("no card button for " + choice.choiceName);
            }
        }
    }

    private static Hitbox cardRewardAdjust(Hitbox hitbox) {
        return new Hitbox(hitbox.x, hitbox.y - 15.0F * Settings.scale, hitbox.width, hitbox.height + 25 * Settings.scale);
    }
}
