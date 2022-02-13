package twitch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class CharacterVoteController extends VoteController {
    public static final HashMap<String, String> MOD_CHARACTER_EXTRA_INFO = new HashMap<String, String>() {{
        put("hermit", "(!hermit) (!hermitinfo) (!trymodchars)");
        put("marisa", "(!marisa) (!marisainfo) (!trymodchars)");
        put("vacant", "(!vacant) (!vacantinfo) (!trymodchars)");
    }};

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

        // Only take the first winning result, the text blurs together if there are multiple
        // winners.
        boolean firstFound = false;

        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

            boolean isWinning = (!firstFound) && winningResults.contains(choice.voteString);
            if (isWinning) {
                firstFound = true;
            }

            Color messageColor = isWinning ? new Color(1.f, 1.f, 0, 1.f) : new Color(1.f, 0, 0, 1.f);

            CardCrawlGame.mode = CardCrawlGame.GameMode.CHAR_SELECT;
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.CHAR_SELECT;

            if (isWinning) {
                CardCrawlGame.mainMenuScreen.charSelectScreen.bgCharImg = twitchController.characterPortrats
                        .get(choice.choiceName);

            }
            twitchController.characterOptions.get(choice.choiceName).selected = isWinning;

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
                case "vacant":
                    charButton = ImageMaster
                            .loadImage("theVacantResources/images/charSelect/TheVacantButton.png");
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
    public void endVote(TwitchController.Choice result) {
        int ascension = TwitchController.optionsMap.get("asc");
        int lives = TwitchController.optionsMap.get("lives");

        String titleSuffix = String
                .format(" Ascension: %d\t Lives: %d Currently Playing %s", ascension, lives, capitalizeFirstLetter(result.choiceName));

        if (MOD_CHARACTER_EXTRA_INFO.containsKey(result.choiceName)) {
            titleSuffix = titleSuffix + " " + MOD_CHARACTER_EXTRA_INFO.get(result.choiceName);
        }

        final String finalSuffix = titleSuffix;
        new Thread(() -> {
            try {
                twitchController.apiController.setStreamTitle(finalSuffix);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                twitchController.currentPrediction = twitchController.apiController
                        .createPrediction();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    private static String capitalizeFirstLetter(String originalString) {
        return originalString.substring(0, 1).toUpperCase() + originalString.substring(1);
    }
}
