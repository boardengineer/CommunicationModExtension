package twitch.patches;

import basemod.CustomCharacterSelectScreen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import twitch.TwitchController;
import twitch.votecontrollers.CharacterVoteController;

public class RenderPatches {
    @SpirePatch(clz = CharacterOption.class, method = "renderOptionButton")
    public static class NoButtonRenderPatch {
        @SpirePrefixPatch
        public static SpireReturn doNothingAtRender(CharacterOption option, SpriteBatch sb) {
            if (inCharacterVote()) {
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "renderAscensionMode")
    @SpirePatch(clz = CharacterSelectScreen.class, method = "renderSeedSettings")
    public static class NoRenderAscension {
        @SpirePrefixPatch
        public static SpireReturn doNothingAtRender(CharacterSelectScreen option, SpriteBatch sb) {
            if (inCharacterVote()) {
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = CustomCharacterSelectScreen.class, method = "render")
    public static class NoRenderSelectdoScreen {
        @SpireInsertPatch(loc = 69)
        public static SpireReturn noRenderArrows(CustomCharacterSelectScreen screen) {
            if (inCharacterVote()) {
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    private static boolean inCharacterVote() {
        return TwitchController.voteController != null && TwitchController.voteController instanceof CharacterVoteController;
    }
}
