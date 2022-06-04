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
import com.megacrit.cardcrawl.characters.Defect;
import com.megacrit.cardcrawl.characters.Ironclad;
import com.megacrit.cardcrawl.characters.TheSilent;
import com.megacrit.cardcrawl.characters.Watcher;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import hermit.characters.hermit;
import theVacant.characters.TheVacant;
import thecursed.TheCursedMod;
import thecursed.characters.TheCursedCharacter;
import twitch.Command;
import twitch.CommandChoice;
import twitch.TwitchController;
import twitch.VoteController;
import twitch.games.ClimbGameController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class CharacterVoteController extends VoteController {
    private static final String VOTE_TIME_KEY = "character";
    private static final int DEFAULT_VOTE_TIME = 25_000;

    private static HashMap<String, Texture> characterPortraits;

    public static final HashMap<String, String> MOD_CHARACTER_EXTRA_INFO = new HashMap<String, String>() {{
        put("hermit", "(!hermit) (!hermitinfo) (!trymodchars)");
        put("marisa", "(!marisa) (!marisainfo) (!trymodchars)");
        put("vacant", "(!vacant) (!vacantinfo) (!trymodchars)");
    }};

    private final HashMap<String, CharacterOption> characterOptions;

    public CharacterVoteController(TwitchController twitchController) {
        super(twitchController);

        characterOptions = getCharacterOptions();
    }

    @Override
    public void setUpChoices() {
        ArrayList<Command> choices = new ArrayList<>();

        twitchController.choices = new ArrayList<>();

        int choiceIndex = 1;

        choices.add(new CommandChoice("ironclad", Integer
                .toString(choiceIndex++), "start ironclad"));
        choices.add(new CommandChoice("silent", Integer
                .toString(choiceIndex++), "start silent"));
        choices.add(new CommandChoice("defect", Integer
                .toString(choiceIndex++), "start defect"));
        choices.add(new CommandChoice("watcher", Integer
                .toString(choiceIndex++), "start watcher"));

        if (BaseMod.hasModID("MarisaState:")) {
            choices.add(new CommandChoice("marisa", Integer
                    .toString(choiceIndex++), "start marisa"));
        }

        if (BaseMod.hasModID("HermitState:")) {
            choices.add(new CommandChoice("hermit", Integer
                    .toString(choiceIndex++), "start hermit"));
        }

        if (BaseMod.hasModID("VacantState:")) {
            choices.add(new CommandChoice("vacant", Integer
                    .toString(choiceIndex++), "start the_vacant"));
        }

        if (BaseMod.hasModID("CursedState:")) {
            choices.add(new CommandChoice("cursed", Integer
                    .toString(choiceIndex++), "start the_cursed"));
        }

        twitchController.choices = choices;
        TwitchController.viableChoices = choices;

        twitchController.choicesMap = new HashMap<>();
        for (Command choice : choices) {
            twitchController.choicesMap.put(choice.getVoteString(), choice);
        }

        HashMap<String, Integer> optionsMap = TwitchController.optionsMap;

        optionsMap.putIfAbsent(VOTE_TIME_KEY, DEFAULT_VOTE_TIME);
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        Set<String> winningResults = twitchController.getBestVoteResultKeys();

        // Only take the first winning result, the text blurs together if there are multiple
        // winners.
        boolean firstFound = false;


        int startX = Settings.WIDTH / (TwitchController.viableChoices.size() + 1) / 2;
        for (int i = 0; i < TwitchController.viableChoices.size(); i++) {
            CommandChoice choice = (CommandChoice) TwitchController.viableChoices.get(i);

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
    public void endVote(Command result) {
        if (TwitchController.gameController != null && TwitchController.gameController instanceof ClimbGameController) {
            ClimbGameController gameController = (ClimbGameController) TwitchController.gameController;

            int ascension = gameController.getAscension();
            int lives = gameController.getlives();

            String titleSuffix = "";
            if (result instanceof CommandChoice) {
                CommandChoice choice = (CommandChoice) result;

                titleSuffix = String
                        .format(" Ascension: %d\t Lives: %d Currently Playing %s", ascension, lives, capitalizeFirstLetter(choice.choiceName));

                if (MOD_CHARACTER_EXTRA_INFO.containsKey(choice.choiceName)) {
                    titleSuffix = titleSuffix + " " + MOD_CHARACTER_EXTRA_INFO
                            .get(choice.choiceName);
                }
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

    @Override
    public long getVoteTimerMillis() {
        return TwitchController.optionsMap.get(VOTE_TIME_KEY);
    }

    @SpirePatch(clz = UnlockTracker.class, method = "isCharacterLocked")
    public static class AllCharactersUnlockedPatch {
        @SpirePrefixPatch
        public static SpireReturn<Boolean> unlockAll(String key) {
            return SpireReturn.Return(false);
        }
    }

    @SpirePatch(clz = MainMenuScreen.class, method = "render")
    public static class NoRenderPatch {
        @SpirePrefixPatch
        public static SpireReturn noRender(MainMenuScreen screen, SpriteBatch sb) {
            if (screen.isFadingOut || TwitchController.voteController != null &&
                    TwitchController.voteController instanceof CharacterVoteController) {
                float yOffset = ReflectionHacks
                        .getPrivate(screen.charSelectScreen, CharacterSelectScreen.class, "bg_y_offset");
                if (screen.charSelectScreen.bgCharImg != null) {
                    if (Settings.isSixteenByTen) {
                        sb.draw(screen.charSelectScreen.bgCharImg, (float) Settings.WIDTH / 2.0F - 960.0F, (float) Settings.HEIGHT / 2.0F - 600.0F, 960.0F, 600.0F, 1920.0F, 1200.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 1920, 1200, false, false);
                    } else if (Settings.isFourByThree) {
                        sb.draw(screen.charSelectScreen.bgCharImg, (float) Settings.WIDTH / 2.0F - 960.0F, (float) Settings.HEIGHT / 2.0F - 600.0F + yOffset, 960.0F, 600.0F, 1920.0F, 1200.0F, Settings.yScale, Settings.yScale, 0.0F, 0, 0, 1920, 1200, false, false);
                    } else if (Settings.isLetterbox) {
                        sb.draw(screen.charSelectScreen.bgCharImg, (float) Settings.WIDTH / 2.0F - 960.0F, (float) Settings.HEIGHT / 2.0F - 600.0F + yOffset, 960.0F, 600.0F, 1920.0F, 1200.0F, Settings.xScale, Settings.xScale, 0.0F, 0, 0, 1920, 1200, false, false);
                    } else {
                        sb.draw(screen.charSelectScreen.bgCharImg, (float) Settings.WIDTH / 2.0F - 960.0F, (float) Settings.HEIGHT / 2.0F - 600.0F + yOffset, 960.0F, 600.0F, 1920.0F, 1200.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 1920, 1200, false, false);
                    }
                } else {
                    screen.bg.render(sb);
                }

                if (!screen.isFadingOut) {
                    for (CharacterOption option : screen.charSelectScreen.options) {
                        if (option.selected) {
                            option.render(sb);
                        }
                    }
                }

                Color overlayColor = ReflectionHacks
                        .getPrivate(screen, MainMenuScreen.class, "overlayColor");
                sb.setColor(overlayColor);
                sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0F, 0.0F, (float) Settings.WIDTH, (float) Settings.HEIGHT);

                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }
}
