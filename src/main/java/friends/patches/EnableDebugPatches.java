package friends.patches;

import basemod.DevConsole;
import chronoMods.TogetherManager;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import savestate.SaveStateMod;

public class EnableDebugPatches {
    @SpirePatch(clz = TogetherManager.class, method = "receivePostInitialize", optional = true, requiredModId = "chronoMods")
    public static class EnableDebugPatch {

        @SpirePrefixPatch
        public static SpireReturn enableDebug() {
            if (SaveStateMod.shouldGoFast) {
                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }

        @SpirePostfixPatch
        public static void spyAfter() {
            DevConsole.enabled = true;
        }
    }

    @SpirePatch(clz = TogetherManager.ConvenienceDebugPresses.class, method = "Postfix", optional = true, requiredModId = "chronoMods")
    public static class EnableDebugPatch2 {
        @SpirePostfixPatch
        public static void enableDebug() {
            DevConsole.enabled = true;
        }
    }
}
