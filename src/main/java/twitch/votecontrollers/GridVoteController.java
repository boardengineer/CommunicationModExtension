package twitch.votecontrollers;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import twitch.CommandChoice;
import twitch.TwitchController;
import twitch.VoteController;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class GridVoteController extends VoteController {
    private final HashMap<String, AbstractCard> voteStringToCardMap;
    private final JsonObject stateJson;

    public GridVoteController(TwitchController twitchController, JsonObject stateJson) {
        super(twitchController);

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
    public JsonArray getVoteChoicesJson() {
        JsonArray result = new JsonArray();

        for (int i = 0; i < TwitchController.viableChoices.size(); i++) {
            if (TwitchController.viableChoices.get(i) instanceof CommandChoice) {
                CommandChoice choice = (CommandChoice) TwitchController.viableChoices.get(i);
                JsonObject optionJson = new JsonObject();

                String message = choice.voteString;
                if (voteStringToCardMap.containsKey(message)) {
                    AbstractCard card = voteStringToCardMap.get(message);

                    float width = card.hb.width * card.drawScale;
                    float height = card.hb.height * card.drawScale;

                    Hitbox hitbox = new Hitbox(card.current_x - width / 2.f, card.current_y - height / 2.f, width, height);

                    optionJson.addProperty("value", message);
                    optionJson.addProperty("x_pos", hitbox.x);
                    optionJson.addProperty("y_pos", hitbox.y);
                    optionJson.addProperty("height", hitbox.height);
                    optionJson.addProperty("width", hitbox.width);
                }

                result.add(optionJson);
            }
        }

        return result;
    }

    @Override
    public Optional<String> getTipString() {
        String tipMsg = ReflectionHacks
                .getPrivate(AbstractDungeon.gridSelectScreen, GridCardSelectScreen.class, "tipMsg");

        return Optional.of(tipMsg);
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

                String message = choice.voteString;
                if (voteStringToCardMap.containsKey(message)) {
                    AbstractCard card = voteStringToCardMap.get(message);

                    Hitbox hitbox = new Hitbox(card.current_x - 25, card.current_y - 110, 50, 50);

                    String voteMessage = String.format("[vote %s](%s)",
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

                    renderTextBelowHitbox(spriteBatch, voteMessage, hitbox, messageColor);
                } else {
                    System.err.println("no event button for " + choice.choiceName);
                }
            }
        }
    }

    @Override
    public void endVote() {

    }
}
