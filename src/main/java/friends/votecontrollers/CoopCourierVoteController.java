package friends.votecontrollers;

import chronoMods.TogetherManager;
import chronoMods.coop.CoopCourierRecipient;
import chronoMods.coop.CoopCourierScreen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import friends.patches.CoopChoicePatches;
import twitch.CommandChoice;
import twitch.RenderHelpers;
import twitch.TwitchController;
import twitch.VoteController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class CoopCourierVoteController extends VoteController {
    private final HashMap<String, CoopChoicePatches.CourChoice> voteStringToCourierItemMap;
    private final JsonObject stateJson;

    public CoopCourierVoteController(TwitchController twitchController, JsonObject stateJson) {
        super(twitchController);

        ArrayList<CoopChoicePatches.CourChoice> choices = CoopChoicePatches
                .getCourierScreenChoices();

        voteStringToCourierItemMap = new HashMap<>();
        for (int i = 0; i < choices.size(); i++) {
            String voteString = Integer.toString(i + 1);

            voteStringToCourierItemMap.put(voteString, choices.get(i));
        }

        this.stateJson = stateJson;
    }

    @Override
    public void setUpChoices() {
        twitchController.setUpDefaultVoteOptions(stateJson);
    }

    @Override
    public void sendVoteMessage() {
        CoopCourierScreen screen = TogetherManager.courierScreen;

        boolean hasRecipient = false;
        for (CoopCourierRecipient player : screen.players) {
            if (player.selected) {
                hasRecipient = true;
                break;
            }
        }

        if (!hasRecipient) {
            TwitchController.twirk
                    .channelMessage("[BOT] Choose a recipient then choose what to send");
        }

        super.sendVoteMessage();
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
                String voteString = choice.voteString;

                if (message.equals("leave")) {
                    String leaveMessage = String.format("[vote %s] (%s)",
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

                    RenderHelpers
                            .renderTextBelowHitbox(spriteBatch, leaveMessage, AbstractDungeon.overlayMenu.cancelButton.hb, messageColor);
                } else if (voteStringToCourierItemMap.containsKey(voteString)) {
                    Hitbox hitbox = addGoldHitbox(getCourierHitbox(voteStringToCourierItemMap
                            .get(voteString)), 1);

                    if (hitbox != null) {
                        String courMessage = String.format("[vote %s] (%s)",
                                choice.voteString,
                                voteFrequencies.getOrDefault(choice.voteString, 0));

                        RenderHelpers
                                .renderTextBelowHitbox(spriteBatch, courMessage, hitbox, messageColor);
                    } else {
                        System.err.println("missing hit box for " + message + " " + voteString);
                    }
                }
            }
        }
    }

    private static Hitbox getCourierHitbox(CoopChoicePatches.CourChoice choice) {
        if (choice instanceof CoopChoicePatches.RelicCourChoice) {
            return ((CoopChoicePatches.RelicCourChoice) choice).relic.relic.hb;
        } else if (choice instanceof CoopChoicePatches.CardCourChoice) {
            return ((CoopChoicePatches.CardCourChoice) choice).card.hb;
        } else if (choice instanceof CoopChoicePatches.PotionCourChoice) {
            return ((CoopChoicePatches.PotionCourChoice) choice).potion.potion.hb;
        } else if (choice instanceof CoopChoicePatches.BoosterCourChoice) {
            return TogetherManager.courierScreen.boosterHBs[((CoopChoicePatches.BoosterCourChoice) choice).boosterindex];
        }
        return null;
    }

    private static Hitbox addGoldHitbox(Hitbox hitbox, int slot) {
        if (hitbox == null) {
            return null;
        }
        return new Hitbox(hitbox.x + (slot - 1) * 50, hitbox.y - 62.0F * Settings.scale, hitbox.width, hitbox.height + 62.F * Settings.scale);
    }
}
