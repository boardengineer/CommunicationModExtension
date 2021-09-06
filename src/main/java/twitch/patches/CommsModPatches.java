package twitch.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import communicationmod.ChoiceScreenUtils;

public class CommsModPatches {
    @SpirePatch(clz = ChoiceScreenUtils.class, method = "isCancelButtonAvailable", paramtypez = {ChoiceScreenUtils.ChoiceType.class})
    public static class CheckIfCardRewardScreenAvailable {
        @SpirePrefixPatch
        public static SpireReturn returnTrueValue(ChoiceScreenUtils.ChoiceType choiceType) {
            if (choiceType == ChoiceScreenUtils.ChoiceType.COMBAT_REWARD) {
                boolean result = !AbstractDungeon.overlayMenu.cancelButton.isHidden;
                return SpireReturn.Return(result);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = ChoiceScreenUtils.class, method = "pressCancelButton", paramtypez = {ChoiceScreenUtils.ChoiceType.class})
    public static class PressCancelButtonPatch {
        @SpirePrefixPatch
        public static SpireReturn returnTrueValue(ChoiceScreenUtils.ChoiceType choiceType) {
            if (choiceType == ChoiceScreenUtils.ChoiceType.COMBAT_REWARD) {
                AbstractDungeon.combatRewardScreen.rewards.clear();

                AbstractDungeon.overlayMenu.cancelButton.hb.clicked = true;
                SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }
}
