package twitch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;

import java.util.HashMap;
import java.util.Set;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class CharacterVoteController extends VoteController {
    private final TwitchController twitchController;
    private final JsonObject stateJson;

    public CharacterVoteController(TwitchController twitchController, JsonObject stateJson) {
        this.twitchController = twitchController;
        this.stateJson = stateJson;

        twitchController.populateCharacterOptions();
    }

    @Override
    public void setUpChoices() {
        twitchController.setUpDefaultVoteOptions(stateJson);
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        Set<String> winningResults = twitchController.getBestVoteResultKeys();

        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

            Color messageColor = winningResults
                    .contains(choice.voteString) ? new Color(1.f, 1.f, 0, 1.f) : new Color(1.f, 0, 0, 1.f);

            CardCrawlGame.mode = CardCrawlGame.GameMode.CHAR_SELECT;
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.CHAR_SELECT;

            if (winningResults.contains(choice.voteString)) {
                CardCrawlGame.mainMenuScreen.charSelectScreen.bgCharImg = twitchController.characterPortrats
                        .get(choice.choiceName);

            }
            twitchController.characterOptions.get(choice.choiceName).selected = winningResults
                    .contains(choice.voteString);

            Texture charButton = null;
            switch (choice.choiceName) {
                case "ironclad":
                    charButton = ImageMaster.CHAR_SELECT_IRONCLAD;
                    break;
                case "silent":
                    charButton = ImageMaster.CHAR_SELECT_SILENT;
                    break;
                case "defect":
                    charButton = ImageMaster.CHAR_SELECT_DEFECT;
                    break;
                case "watcher":
                    charButton = ImageMaster.CHAR_SELECT_WATCHER;
                    break;
                case "marisa":
                    charButton = ImageMaster.loadImage("img/charSelect/MarisaButton.png");
                    break;
                case "hermit":
                    charButton = ImageMaster
                            .loadImage("hermitResources/images/charSelect/HermitButton.png");
                    break;
            }

            spriteBatch.draw(charButton, 300 + 225 * i, 50, 200, 200);

            String voteMessage = String.format("[vote %s] (%s)",
                    choice.voteString,
                    voteFrequencies.getOrDefault(choice.voteString, 0));

            Hitbox hitbox = new Hitbox(300 + 225 * i, 50, 200, 200);

            renderTextBelowHitbox(spriteBatch, voteMessage, hitbox, messageColor);
        }
    }

    @Override
    public void endVote() {

    }
}
