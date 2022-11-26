package friends.patches;

import chronoMods.coop.hubris.DuctTapeCard;
import chronoMods.coop.infusions.Infusion;
import chronoMods.coop.infusions.InfusionHelper;
import chronoMods.coop.infusions.InfusionSet;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.red.Whirlwind;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class DoNotSubmitPatch {
    @SpirePatch(clz = CardCrawlGame.class, method = "update")
    public static class AlwaysDebugButtonsPatch {
        @SpirePostfixPatch
        public static void Debug(CardCrawlGame __instance) {
            if (InputActionSet.selectCard_1.isJustPressed()) {
                String rareRelicList = AbstractDungeon.rareRelicPool.stream().collect(Collectors
                        .joining(" "));
                System.err.println(rareRelicList);
                System.err.println(AbstractDungeon.rareRelicPool.size());
            }
        }
    }

    @SpirePatch(clz = AbstractDungeon.class, method = "update", requiredModId = "chronoMods")
    public static class ConvenienceDebugPresses {
        @SpirePostfixPatch
        public static void SpireWithFriendsDebugButtons(AbstractDungeon __instance) {
            if (InputActionSet.selectCard_1.isJustPressed()) {
                System.err.println("we got a hook into a button");
            }

            if (InputActionSet.selectCard_2.isJustPressed()) {
                InfusionSet iSet = InfusionHelper
                        .getInfusionSet(AbstractPlayer.PlayerClass.IRONCLAD);
                AbstractCard c = CardLibrary.getAnyColorCard(AbstractCard.CardRarity.COMMON)
                                            .makeCopy();
                Infusion i = iSet.getUnshuffledValidInfusion(c);
                if (i != null) {
                    i.ApplyInfusion(c);
                }
                AbstractDungeon.effectList
                        .add(new ShowCardAndObtainEffect(c, Settings.WIDTH / 2.0f, Settings.HEIGHT / 2.0f));
            }


            if (InputActionSet.selectCard_3.isJustPressed()) {
                ArrayList<AbstractCard> cards = new ArrayList();
                cards.add(new Whirlwind());
                cards.add(new Whirlwind());

                AbstractDungeon.effectList
                        .add(new ShowCardAndObtainEffect(new DuctTapeCard(cards), Settings.WIDTH / 2.0f, Settings.HEIGHT / 2.0f));
            }
        }
    }
}
