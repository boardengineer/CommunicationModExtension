package twitch.patches;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.ui.panels.TopPanel;
import twitch.TwitchController;

public class TopPanelPatches {
    private static final Texture livesImage = new Texture("img/ClimbLives.png");

    @SpirePatch(clz = TopPanel.class, method = "renderDungeonInfo")
    public static class ClimbLivesRenderPatch {
        @SpirePostfixPatch
        public static void renderLives(TopPanel topPanel, SpriteBatch sb) {
            if (TwitchController.optionsMap != null &&
                    TwitchController.optionsMap.getOrDefault("lives", 0) > 0) {

                float floorX = ReflectionHacks.getPrivateStatic(TopPanel.class, "floorX");
                float ICON_Y = ReflectionHacks.getPrivateStatic(TopPanel.class, "ICON_Y");
                float ICON_W = ReflectionHacks.getPrivateStatic(TopPanel.class, "ICON_W");
                float INFO_TEXT_Y = ReflectionHacks.getPrivateStatic(TopPanel.class, "INFO_TEXT_Y");

                float drawX = floorX + 212.0F * Settings.scale;

                sb.draw(livesImage, floorX + 212.0F * Settings.scale, ICON_Y, ICON_W, ICON_W);

                FontHelper
                        .renderFontLeftTopAligned(sb, FontHelper.topPanelInfoFont, Integer
                                .toString(TwitchController.optionsMap
                                        .get("lives")), drawX + ICON_W + 10, INFO_TEXT_Y, Settings.GREEN_TEXT_COLOR);
            }
        }
    }
}
