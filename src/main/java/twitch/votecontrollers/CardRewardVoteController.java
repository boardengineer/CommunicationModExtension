package twitch.votecontrollers;

import basemod.BaseMod;
import basemod.ReflectionHacks;
import chronoMods.coop.CoopCourierRoom;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.relics.TinyHouse;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.ui.buttons.SingingBowlButton;
import com.megacrit.cardcrawl.ui.buttons.SkipCardButton;
import twitch.CommandChoice;
import twitch.TwitchController;
import twitch.VoteController;

import java.util.HashMap;
import java.util.Set;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class CardRewardVoteController extends VoteController {
    private static boolean inTinyHouse = false;

    private static final String CARD_REWARD_LONG_KEY = "card_select_long";
    private static final String CARD_REWARD_SHORT_KEY = "card_select_short";

    private static final int DEFAULT_LONG_VOTE_TIME_MILLIS = 30_000;
    private static final int DEFAULT_SHORT_VOTE_TIME_MILLIS = 20_000;

    // Card reward rendering references
    private final HashMap<String, AbstractCard> messageToCardReward;
    private final JsonObject stateJson;

    public CardRewardVoteController(TwitchController twitchController, JsonObject stateJson) {
        super(twitchController);
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
                if (inTinyHouse) {
                    inTinyHouse = false;

                    TwitchController.viableChoices
                            .add(new CommandChoice("Skip", "0", "skip", "cancel", "leave"));
                } else {
                    boolean backToRoom = false;

                    if (BaseMod.hasModID("chronoMods:")) {
                        if (AbstractDungeon.getCurrRoom() instanceof CoopCourierRoom) {
                            backToRoom = true;
                        }
                    }

                    if (AbstractDungeon.getCurrRoom() instanceof ShopRoom) {
                        backToRoom = true;
                    }

                    String skipCommand = backToRoom ? "cancel" : "proceed";
                    TwitchController.viableChoices
                            .add(new CommandChoice("Skip", "0", "skip", skipCommand));
                }
            } else {
                TwitchController.viableChoices
                        .add(new CommandChoice("Skip", "0", "skip"));
            }
        }

        HashMap<String, Integer> optionsMap = TwitchController.optionsMap;

        optionsMap.putIfAbsent(CARD_REWARD_LONG_KEY, DEFAULT_LONG_VOTE_TIME_MILLIS);
        optionsMap.putIfAbsent(CARD_REWARD_SHORT_KEY, DEFAULT_SHORT_VOTE_TIME_MILLIS);
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = TwitchController.getVoteFrequencies();
        Set<String> winningResults = TwitchController.getBestVoteResultKeys();

        for (int i = 0; i < TwitchController.viableChoices.size(); i++) {
            if (TwitchController.viableChoices.get(i) instanceof CommandChoice) {
                CommandChoice choice = (CommandChoice) TwitchController.viableChoices.get(i);

                Color messageColor = winningResults
                        .contains(choice.voteString) ? new Color(1.f, 1.f, 0, 1.f) : new Color(1.f, 0, 0, 1.f);

                String message = choice.choiceName;
                if (message.equalsIgnoreCase("skip")) {
                    String skipMessage = String.format("[vote %s] (%s)",
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

                    SkipCardButton skipCardButton = ReflectionHacks
                            .getPrivate(AbstractDungeon.cardRewardScreen, CardRewardScreen.class, "skipButton");

                    renderTextBelowHitbox(spriteBatch, skipMessage, cardRewardAdjust(skipCardButton.hb), messageColor);
                } else if (message.equalsIgnoreCase("bowl")) {
                    String bowlMessage = String.format("[vote %s] (%s)",
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

                    SingingBowlButton bowlButton = ReflectionHacks
                            .getPrivate(AbstractDungeon.cardRewardScreen, CardRewardScreen.class, "bowlButton");

                    renderTextBelowHitbox(spriteBatch, bowlMessage, cardRewardAdjust(bowlButton.hb), messageColor);
                } else if (messageToCardReward.containsKey(message)) {
                    AbstractCard card = messageToCardReward.get(message);
                    Hitbox cardHitbox = card.hb;
                    String cardMessage = String.format("[vote %s] (%s)",
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

                    renderTextBelowHitbox(spriteBatch, cardMessage, cardRewardAdjust(cardHitbox), messageColor);
                } else {
//                System.err.println("no card button for " + choice.choiceName);
                }
            }
        }
    }

    @Override
    public long getVoteTimerMillis() {
        if (AbstractDungeon.floorNum == 1) {
            return TwitchController.optionsMap.get(CARD_REWARD_LONG_KEY);
        } else {
            return TwitchController.optionsMap.get(CARD_REWARD_SHORT_KEY);
        }
    }

    private static Hitbox cardRewardAdjust(Hitbox hitbox) {
        return new Hitbox(hitbox.x, hitbox.y - 15.0F * Settings.scale, hitbox.width, hitbox.height + 25 * Settings.scale);
    }

    @SpirePatch(clz = TinyHouse.class, method = "onEquip")
    public static class toggleTinyHouseBooleanPatch {
        @SpirePostfixPatch
        public static void toggleBoolean(TinyHouse tinyHouse) {
            inTinyHouse = true;
        }
    }
}
