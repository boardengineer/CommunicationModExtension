package friends.patches;

import chronoMods.TogetherManager;
import chronoMods.coop.infusions.Infusion;
import chronoMods.coop.infusions.LinkedInfusions;
import chronoMods.network.NetworkHelper;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.cards.AbstractCard;
import ludicrousspeed.LudicrousSpeedMod;
import savestate.CardState;

import java.util.Collection;
import java.util.HashMap;

public class InfusionStatePatches {
    public static HashMap<String, Runnable> infusionsRunnableMap;

    @SpirePatch(clz = CardState.class, method = SpirePatch.CLASS, requiredModId = "chronoMods")
    public static class InfusionStateField {
        public static SpireField<String> infusionId = new SpireField<>(() -> null);
    }

    @SpirePatch(clz = CardState.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {AbstractCard.class}, requiredModId = "chronoMods")
    public static class MainConstructorFoilPatch {
        @SpirePostfixPatch
        public static void addFoilParam(CardState cardState, AbstractCard cardParam) {
            if (Infusion.infusionField.infusion.get(cardParam) != null) {
                InfusionStateField.infusionId
                        .set(cardState, Infusion.infusionField.infusion.get(cardParam).description);
            }
        }
    }

    @SpirePatch(clz = CardState.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {JsonObject.class}, requiredModId = "chronoMods")
    public static class JsonObjectConstructorFoilPatch {
        @SpirePostfixPatch
        public static void addFoilParam(CardState cardState, JsonObject cardJson) {
            if (cardJson.has("infusion_description")) {
                InfusionStateField.infusionId
                        .set(cardState, cardJson.get("infusion_description").getAsString());
            }
        }
    }

    @SpirePatch(clz = CardState.class, method = "loadCard", requiredModId = "chronoMods")
    public static class LoadCardInfusionPatch {
        private static boolean disable = true;

        @SpirePrefixPatch
        public static SpireReturn addInfusion(CardState cardState) {
            if (disable) {
                disable = false;
                AbstractCard result = cardState.loadCard();
                String description = InfusionStateField.infusionId.get(cardState);

                Infusion.infusionField.infusion
                        .set(result, new Infusion(description, getMapInstance()
                                .get(description)));

                disable = true;
                return SpireReturn.Return(result);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = CardState.class, method = "jsonEncode", requiredModId = "chronoMods")
    public static class EncodeFoilPatch {
        private static boolean disable = true;

        @SpirePrefixPatch
        public static SpireReturn maybeDoNothing(CardState cardState) {
            if (disable && InfusionStateField.infusionId.get(cardState) != null) {
                disable = false;
                JsonObject result = cardState.jsonEncode();
                result.addProperty("infusion_description", InfusionStateField.infusionId
                        .get(cardState));
                disable = true;
                return SpireReturn.Return(result);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = TogetherManager.class, method = "receivePostInitialize", requiredModId = "chronoMods")
    public static class Thingthing {
        @SpirePrefixPatch
        public static SpireReturn populateMap() {
            if (LudicrousSpeedMod.plaidMode) {
                LinkedInfusions.setupInfusions();
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = NetworkHelper.class, method = "initialize", requiredModId = "chronoMods")
    public static class NoNetworkHelperPatch {
        @SpirePrefixPatch
        public static SpireReturn noInit() {
            if (LudicrousSpeedMod.plaidMode) {
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    public static HashMap<String, Runnable> getMapInstance() {
        if (infusionsRunnableMap == null) {
            infusionsRunnableMap = new HashMap<>();

            LinkedInfusions.setupInfusions();
            LinkedInfusions.characterInfusionMasterList.values().stream()
                                                       .flatMap(Collection::stream)
                                                       .flatMap(set -> set.infusions.stream())
                                                       .forEach(infusion -> infusionsRunnableMap
                                                               .put(infusion.description, infusion.actions));


            LinkedInfusions.defaultInfusions.infusions.stream()
                                                      .forEach(infusion -> infusionsRunnableMap
                                                              .put(infusion.description, infusion.actions));
        }

        return infusionsRunnableMap;
    }
}
