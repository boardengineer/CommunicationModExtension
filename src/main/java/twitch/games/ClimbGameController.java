package twitch.games;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.ui.panels.TopPanel;
import twitch.GameController;
import twitch.TwitchController;

import java.util.HashMap;

public class ClimbGameController implements GameController {
    private static final Texture LIVES_IMAGE = new Texture("img/ClimbLives.png");

    private static final String ASCENSION_KEY = "climb_asc";
    private static final String LIVES_KEY = "climb_lives";

    private static final int DEFAULT_LIVES = 20;
    private static final int DEFAULT_ASCENSION = 1;

    private final TwitchController twitchController;
    private final HashMap<String, Integer> optionsMap;
    private final SpireConfig optionsConfig;

    public ClimbGameController(TwitchController twitchController) {
        this.twitchController = twitchController;
        this.optionsMap = TwitchController.optionsMap;
        this.optionsConfig = twitchController.optionsConfig;

        if (optionsConfig.has(ASCENSION_KEY)) {
            optionsMap.put(ASCENSION_KEY, optionsConfig.getInt(ASCENSION_KEY));
        } else {
            optionsMap.put(ASCENSION_KEY, DEFAULT_ASCENSION);
        }

        if (optionsConfig.has(LIVES_KEY)) {
            optionsMap.put(LIVES_KEY, optionsConfig.getInt(LIVES_KEY));
        } else {
            optionsMap.put(LIVES_KEY, DEFAULT_LIVES);
        }
    }

    @Override
    public int getAscension() {
        return TwitchController.optionsMap.get(ASCENSION_KEY);
    }

    @Override
    public void reportGameOver(JsonObject gameState) {
        boolean reportedVictory = gameState.get("screen_state").getAsJsonObject()
                                           .get("victory").getAsBoolean();
        int floor = gameState.get("floor").getAsInt();
        boolean didClimb = reportedVictory || floor > 51;
        if (didClimb) {
            int newAsc = optionsMap.getOrDefault(ASCENSION_KEY, 0) + 1;
            newAsc = Math.min(20, newAsc);
            optionsMap.put(ASCENSION_KEY, newAsc);
            if (reportedVictory && floor > 51) {
                // Heart kills get an extra life
                int newLives = optionsMap.getOrDefault(LIVES_KEY, 0) + 1;
                optionsMap.put(LIVES_KEY, newLives);
            }
            twitchController.saveOptionsConfig();
        } else {
            int newLives = optionsMap.getOrDefault(LIVES_KEY, 0) - 1;

            if (newLives <= 0) {
                newLives = DEFAULT_LIVES;

                int newAsc = 1;
                optionsMap.put(ASCENSION_KEY, newAsc);
            }

            optionsMap.put(LIVES_KEY, newLives);
            twitchController.saveOptionsConfig();
        }
    }

    public int getlives() {
        return optionsMap.get(LIVES_KEY);
    }

    @SpirePatch(clz = TopPanel.class, method = "renderDungeonInfo")
    public static class ClimbLivesRenderPatch {
        @SpirePostfixPatch
        public static void renderLives(TopPanel topPanel, SpriteBatch sb) {
            if (TwitchController.gameController != null && TwitchController.gameController instanceof ClimbGameController) {
                ClimbGameController gameController = (ClimbGameController) TwitchController.gameController;
                if (gameController.getAscension() > 0) {

                    float floorX = ReflectionHacks.getPrivateStatic(TopPanel.class, "floorX");
                    float ICON_Y = ReflectionHacks.getPrivateStatic(TopPanel.class, "ICON_Y");
                    float ICON_W = ReflectionHacks.getPrivateStatic(TopPanel.class, "ICON_W");
                    float INFO_TEXT_Y = ReflectionHacks
                            .getPrivateStatic(TopPanel.class, "INFO_TEXT_Y");

                    float drawX = floorX + 212.0F * Settings.scale;

                    sb.draw(LIVES_IMAGE, floorX + 212.0F * Settings.scale, ICON_Y, ICON_W, ICON_W);

                    FontHelper
                            .renderFontLeftTopAligned(sb, FontHelper.topPanelInfoFont, Integer
                                    .toString(gameController
                                            .getlives()), drawX + ICON_W + 10, INFO_TEXT_Y, Settings.GREEN_TEXT_COLOR);
                }
            }
        }
    }
}
