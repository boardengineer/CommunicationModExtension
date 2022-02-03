package twitch;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;

import java.util.HashMap;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class CharacterVoteController extends VoteController {
    private final TwitchController twitchController;
    private final JsonObject stateJson;

    public CharacterVoteController(TwitchController twitchController, JsonObject stateJson) {
        this.twitchController = twitchController;
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
            renderTextBelowHitbox(spriteBatch, voteMessage, hitbox);
        }
    }

    @Override
    public void endVote() {

    }
}
