package twitch.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.events.city.DrugDealer;
import com.megacrit.cardcrawl.events.exordium.LivingWall;
import com.megacrit.cardcrawl.events.shrines.Designer;
import com.megacrit.cardcrawl.events.shrines.Transmogrifier;
import com.megacrit.cardcrawl.neow.NeowReward;
import com.megacrit.cardcrawl.relics.Astrolabe;
import com.megacrit.cardcrawl.relics.PandorasBox;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import tssrelics.relics.FestivuePole;
import twitch.TwitchController;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Transform results don't always stay on screen long enough to see, write results in chat.
 * <p>
 * Transforms will be printed in common locations but will only be enabled during transforms via
 * shouldPrintTransformResults.
 */
public class TransformResultPatches {
    public static boolean shouldPrintTransformResults = false;

    @SpirePatch(clz = ShowCardAndObtainEffect.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {AbstractCard.class, float.class, float.class, boolean.class})
    public static class DisplayTransformResultsPatch {
        @SpirePrefixPatch
        public static void showTransformResult(ShowCardAndObtainEffect effect, AbstractCard card, float x, float y, boolean convergeCards) {
            if (shouldPrintTransformResults && TwitchController.twirk != null) {
                TwitchController.twirk.channelMessage("[BOT] Transform result: " + card.name);
            }
        }
    }

    @SpirePatch(clz = Astrolabe.class, method = "giveCards")
    public static class EnableMessageForAstrolabePatch {
        @SpirePrefixPatch
        public static void enableMessaging(Astrolabe astrolabe, ArrayList<AbstractCard> group) {
            shouldPrintTransformResults = true;
        }

        @SpirePostfixPatch
        public static void disableMessaging(Astrolabe astrolabe, ArrayList<AbstractCard> group) {
            shouldPrintTransformResults = false;
        }
    }

    @SpirePatch(clz = NeowReward.class, method = "update")
    public static class EnableOnNeowReward {
        @SpirePrefixPatch
        public static void enableMessaging(NeowReward neowReward) {
            shouldPrintTransformResults = true;
        }

        @SpirePostfixPatch
        public static void disableMessaging(NeowReward neowReward) {
            shouldPrintTransformResults = false;
        }
    }

    @SpirePatch(clz = DrugDealer.class, method = "update")
    public static class EnableOnDrugDealer {
        @SpirePrefixPatch
        public static void enableMessaging(DrugDealer drugDealer) {
            shouldPrintTransformResults = true;
        }

        @SpirePostfixPatch
        public static void disableMessaging(DrugDealer drugDealer) {
            shouldPrintTransformResults = false;
        }
    }

    @SpirePatch(clz = LivingWall.class, method = "update")
    public static class EnableOnLivingWall {
        @SpirePrefixPatch
        public static void enableMessaging(LivingWall livingWall) {
            shouldPrintTransformResults = true;
        }

        @SpirePostfixPatch
        public static void disableMessaging(LivingWall livingWall) {
            shouldPrintTransformResults = false;
        }
    }

    @SpirePatch(clz = FestivuePole.AirGrievencesEffect.class, method = "update", requiredModId = "TSSRelics")
    public static class EnableOnFestivusPole {
        @SpirePrefixPatch
        public static void enableMessaging(FestivuePole.AirGrievencesEffect airGrievencesEffect) {
            shouldPrintTransformResults = true;
        }

        @SpirePostfixPatch
        public static void disableMessaging(FestivuePole.AirGrievencesEffect airGrievencesEffect) {
            shouldPrintTransformResults = false;
        }
    }

    @SpirePatch(clz = Designer.class, method = "update")
    public static class EnableOnDesigner {
        @SpirePrefixPatch
        public static void enableMessaging(Designer designer) {
            shouldPrintTransformResults = true;
        }

        @SpirePostfixPatch
        public static void disableMessaging(Designer designer) {
            shouldPrintTransformResults = false;
        }
    }

    @SpirePatch(clz = Transmogrifier.class, method = "update")
    public static class EnableOnTransmogrifier {
        @SpirePrefixPatch
        public static void enableMessaging(Transmogrifier transmogrifier) {
            shouldPrintTransformResults = true;
        }

        @SpirePostfixPatch
        public static void disableMessaging(Transmogrifier transmogrifier) {
            shouldPrintTransformResults = false;
        }
    }

    // Pandora's box uses a different transform mechanism and a different mechanism for adding the
    // cards to the deck

    @SpirePatch(clz = GridCardSelectScreen.class, method = "openConfirmationGrid")
    public static class DisplayMessageFromConfirmationScreen {

        @SpirePrefixPatch
        public static void displayMessage(GridCardSelectScreen gridCardSelectScreen, CardGroup group, String tipMsg) {
            if (shouldPrintTransformResults && TwitchController.twirk != null) {
                TwitchController.twirk.channelMessage(String
                        .format("[BOT] Transform Results: [%s] ", group.group.stream()
                                                                             .map(card -> card.name)
                                                                             .collect(Collectors
                                                                                     .joining(", "))));
            }
        }
    }

    @SpirePatch(clz = PandorasBox.class, method = "onEquip")
    public static class onEquipPandorasBox {
        @SpirePrefixPatch
        public static void enableMessaging(PandorasBox pandorasBox) {
            shouldPrintTransformResults = true;
        }

        @SpirePostfixPatch
        public static void disableMessaging(PandorasBox pandorasBox) {
            shouldPrintTransformResults = false;
        }
    }
}
