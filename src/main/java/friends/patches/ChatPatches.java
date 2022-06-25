package friends.patches;

import chronoMods.chat.ChatScreen;
import chronoMods.ui.hud.InfoPopupPatches;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import savestate.SaveStateMod;

public class ChatPatches {
    @SpirePatch(clz = ChatScreen.IsJustPressedFix.class, method = "Prefix", optional = true, requiredModId = "chronoMods")
    public static class DisablePress {
        @SpirePrefixPatch
        public static SpireReturn<SpireReturn<Boolean>> spyBefore() {
            if (SaveStateMod.shouldGoFast) {
                return SpireReturn.Return(SpireReturn.Continue());
            }

            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = InfoPopupPatches.infoDungeonUpdate.class, method = "Insert", optional = true, requiredModId = "chronoMods")
    @SpirePatch(clz = InfoPopupPatches.infoRender.class, method = "Insert", optional = true, requiredModId = "chronoMods")
    public static class DisableChatBox {
        @SpirePrefixPatch
        public static SpireReturn spyBefore() {
            if (SaveStateMod.shouldGoFast) {
                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }
    }
}
