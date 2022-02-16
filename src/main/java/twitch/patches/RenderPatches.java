package twitch.patches;

import basemod.CustomCharacterSelectScreen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;

public class RenderPatches {
    @SpirePatch(clz = CharacterOption.class, method = "renderOptionButton")
    public static class NoButtonRenderPatch {
        @SpirePrefixPatch
        public static SpireReturn doNothingAtRender(CharacterOption option, SpriteBatch sb) {
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "renderAscensionMode")
    @SpirePatch(clz = CharacterSelectScreen.class, method = "renderSeedSettings")
    public static class NoRenderAscension {
        @SpirePrefixPatch
        public static SpireReturn doNothingAtRender(CharacterSelectScreen option, SpriteBatch sb) {
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch(clz = CustomCharacterSelectScreen.class, method = "render")
    public static class NoRenderSelectdoScreen {
        @SpireInsertPatch(loc = 69)
        public static SpireReturn noRenderArrows(CustomCharacterSelectScreen scree) {
            return SpireReturn.Return(null);
        }
    }
}
