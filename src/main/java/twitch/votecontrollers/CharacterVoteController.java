package twitch.votecontrollers;

import ThMod.characters.Marisa;
import basemod.BaseMod;
import basemod.CustomCharacterSelectScreen;
import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.characters.Defect;
import com.megacrit.cardcrawl.characters.Ironclad;
import com.megacrit.cardcrawl.characters.TheSilent;
import com.megacrit.cardcrawl.characters.Watcher;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import hermit.characters.hermit;
import theVacant.characters.TheVacant;
import thecursed.TheCursedMod;
import thecursed.characters.TheCursedCharacter;
import twitch.TwitchController;
import twitch.VoteController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class CharacterVoteController extends VoteController {
    private static HashMap<String, Texture> characterPortraits;

    public static final HashMap<String, String> MOD_CHARACTER_EXTRA_INFO = new HashMap<String, String>() {{
        put("hermit", "(!hermit) (!hermitinfo) (!trymodchars)");
        put("marisa", "(!marisa) (!marisainfo) (!trymodchars)");
        put("vacant", "(!vacant) (!vacantinfo) (!trymodchars)");
    }};

    private final JsonObject stateJson;

    private final HashMap<String, CharacterOption> characterOptions;

    public CharacterVoteController(TwitchController twitchController, JsonObject stateJson) {
        super(twitchController);
        this.stateJson = stateJson;

        characterOptions = getCharacterOptions();
    }

    @Override
    public void setUpChoices() {
        ArrayList<TwitchController.Choice> choices = new ArrayList<>();

        twitchController.choices = new ArrayList<>();

        int choiceIndex = 1;

        choices.add(new TwitchController.Choice("ironclad", Integer
                .toString(choiceIndex++), "start ironclad"));
        choices.add(new TwitchController.Choice("silent", Integer
                .toString(choiceIndex++), "start silent"));
        choices.add(new TwitchController.Choice("defect", Integer
                .toString(choiceIndex++), "start defect"));
        choices.add(new TwitchController.Choice("watcher", Integer
                .toString(choiceIndex++), "start watcher"));

        if (BaseMod.hasModID("MarisaState:")) {
            choices.add(new TwitchController.Choice("marisa", Integer
                    .toString(choiceIndex++), "start marisa"));
        }

        if (BaseMod.hasModID("HermitState:")) {
            choices.add(new TwitchController.Choice("hermit", Integer
                    .toString(choiceIndex++), "start hermit"));
        }

        if (BaseMod.hasModID("VacantState:")) {
            choices.add(new TwitchController.Choice("vacant", Integer
                    .toString(choiceIndex++), "start the_vacant"));
        }

        if (BaseMod.hasModID("CursedState:")) {
            choices.add(new TwitchController.Choice("cursed", Integer
                    .toString(choiceIndex++), "start the_cursed"));
        }

        twitchController.choices = choices;
        twitchController.viableChoices = choices;

        twitchController.choicesMap = new HashMap<>();
        for (TwitchController.Choice choice : choices) {
            twitchController.choicesMap.put(choice.voteString, choice);
        }
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        Set<String> winningResults = twitchController.getBestVoteResultKeys();

        // Only take the first winning result, the text blurs together if there are multiple
        // winners.
        boolean firstFound = false;


        int startX = Settings.WIDTH / (twitchController.viableChoices.size() + 1) / 2;
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

            boolean isWinning = (!firstFound) && winningResults.contains(choice.voteString);
            if (isWinning) {
                firstFound = true;
            }

            Color messageColor = isWinning ? new Color(1.f, 1.f, 0, 1.f) : new Color(1.f, 0, 0, 1.f);

            if (isWinning) {
                CardCrawlGame.mainMenuScreen.charSelectScreen.bgCharImg = characterPortraits
                        .get(choice.choiceName);
            }
            characterOptions.get(choice.choiceName).selected = isWinning;

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
                case "cursed":
                    charButton = ImageMaster
                            .loadImage(TheCursedMod.getResourcePath("charSelect/button.png"));
                    break;
            }

//            Settings.WIDTH

            int xpos = startX + 225 * i;
            spriteBatch.draw(charButton, xpos, 50, charButton.getWidth(), charButton.getHeight());

            String voteMessage = String.format("[vote %s] (%s)",
                    choice.voteString,
                    voteFrequencies.getOrDefault(choice.voteString, 0));

            Hitbox hitbox = new Hitbox(xpos, 50, 200, 200);

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

    private HashMap<String, CharacterOption> getCharacterOptions() {
        HashMap<String, CharacterOption> characterOptions = new HashMap<>();

        ArrayList<CharacterOption> options = new ArrayList<>();
        try {
            options = ReflectionHacks
                    .getPrivate(CardCrawlGame.mainMenuScreen.charSelectScreen, CustomCharacterSelectScreen.class, "allOptions");

            if (options == null) {
                options = CardCrawlGame.mainMenuScreen.charSelectScreen.options;
            } else {
                CardCrawlGame.mainMenuScreen.charSelectScreen.options = options;
            }
        } catch (IllegalArgumentException e) {
        }


        for (CharacterOption option : options) {
            if (option.c instanceof Ironclad) {
                characterOptions.put("ironclad", option);
            } else if (option.c instanceof TheSilent) {
                characterOptions.put("silent", option);
            } else if (option.c instanceof Defect) {
                characterOptions.put("defect", option);
            } else if (option.c instanceof Watcher) {
                characterOptions.put("watcher", option);
            } else if (BaseMod.hasModID("MarisaState:") && option.c instanceof Marisa) {
                characterOptions.put("marisa", option);
            } else if (BaseMod.hasModID("HermitState:") && option.c instanceof hermit) {
                characterOptions.put("hermit", option);
            } else if (BaseMod.hasModID("VacantState:") && option.c instanceof TheVacant) {
                characterOptions.put("vacant", option);
            } else if (BaseMod.hasModID("CursedState:") && option.c instanceof TheCursedCharacter) {
                characterOptions.put("cursed", option);
            } else {
                System.err.println("no character option for " + option.c.getClass());
            }
        }

        System.err.println("Returning Character Options " + characterOptions);
        return characterOptions;
    }

    private static String capitalizeFirstLetter(String originalString) {
        return originalString.substring(0, 1).toUpperCase() + originalString.substring(1);
    }

    public static void initializePortraits() {
        if (characterPortraits == null || characterPortraits.isEmpty()) {
            characterPortraits = new HashMap<>();

            characterPortraits.put("ironclad", ImageMaster
                    .loadImage("images/ui/charSelect/ironcladPortrait.jpg"));
            characterPortraits
                    .put("silent", ImageMaster
                            .loadImage("images/ui/charSelect/silentPortrait.jpg"));
            characterPortraits
                    .put("defect", ImageMaster
                            .loadImage("images/ui/charSelect/defectPortrait.jpg"));
            characterPortraits
                    .put("watcher", ImageMaster
                            .loadImage("images/ui/charSelect/watcherPortrait.jpg"));

            if (BaseMod.hasModID("MarisaState:")) {
                characterPortraits
                        .put("marisa", ImageMaster.loadImage("img/charSelect/marisaPortrait.jpg"));
            }

            if (BaseMod.hasModID("HermitState:")) {
                characterPortraits.put("hermit", ImageMaster
                        .loadImage("hermitResources/images/charSelect/hermitSelect.png"));
            }

            if (BaseMod.hasModID("VacantState:")) {
                characterPortraits.put("vacant", ImageMaster
                        .loadImage("theVacantResources/images/charSelect/VacantPortraitBG.png"));
            }

            if (BaseMod.hasModID("CursedState:")) {
                characterPortraits.put("cursed", ImageMaster
                        .loadImage(TheCursedMod.getResourcePath("charSelect/portrait.png")));
            }
        }
    }

    @SpirePatch(clz = UnlockTracker.class, method = "isCharacterLocked")
    public static class AllCharactersUnlockedPatch {
        @SpirePrefixPatch
        public static SpireReturn<Boolean> unlockAll(String key) {
            return SpireReturn.Return(false);
        }
    }
}
