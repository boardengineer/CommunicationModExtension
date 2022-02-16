package twitch.patches;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import twitch.votecontrollers.GridVoteController;
import twitch.TwitchController;

import java.util.ArrayList;

public class GridSelectionPatch {
    @SpirePatch(clz = GridCardSelectScreen.class, method = "updateCardPositionsAndHoverLogic")
    public static class GridRenderPatch {
        @SpirePrefixPatch
        public static SpireReturn messWithGridSelect(GridCardSelectScreen gridCardSelectScreen) {
            if (TwitchController.voteController != null && TwitchController.voteController instanceof GridVoteController) {
                ArrayList<AbstractCard> cards = gridCardSelectScreen.targetGroup.group;

                int lineNum = 0;
                for (int i = 0; i < cards.size(); i++) {
                    int mod = i % 8;
                    if (mod == 0 && i != 0) {
                        ++lineNum;
                    }

                    AbstractCard card = cards.get(i);

                    float drawStartX = ReflectionHacks
                            .getPrivate(gridCardSelectScreen, GridCardSelectScreen.class, "drawStartX");
                    float drawStartY = ReflectionHacks
                            .getPrivate(gridCardSelectScreen, GridCardSelectScreen.class, "drawStartY");
                    float currentDiffY = ReflectionHacks
                            .getPrivate(gridCardSelectScreen, GridCardSelectScreen.class, "currentDiffY");

                    float padX = ReflectionHacks
                            .getPrivate(gridCardSelectScreen, GridCardSelectScreen.class, "padX");
                    float padY = ReflectionHacks
                            .getPrivate(gridCardSelectScreen, GridCardSelectScreen.class, "padY");


                    card.drawScale = .45F;

                    // TODO make this adjust with scale
//                    card.target_x = card.current_x = drawStartX + (float) mod * (padX / 2.F * 1.5F) - 200;
                    card.target_x = card.current_x = drawStartX + (float) mod * (padX / 2.F * 1.5F) - 300;
                    card.target_y = card.current_y = drawStartY + currentDiffY - (float) lineNum * (padY / 2.F * 1.4F) + 75;

                    AbstractDungeon.overlayMenu.cancelButton.hide();
                }

                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }
}
