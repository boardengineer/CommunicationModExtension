package friends;

import chronoMods.coop.hubris.DuctTapeCard;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.cards.AbstractCard;
import savestate.CardState;
import savestate.SaveStateMod;
import savestate.StateFactories;

import java.util.Arrays;
import java.util.stream.Collectors;

public class DuctTapeCardState extends CardState {
    private final CardState[] cards;

    public DuctTapeCardState(AbstractCard card) {
        super(card);

        cards = ((DuctTapeCard) card).cards.stream().map(CardState::forCard)
                                           .toArray(CardState[]::new);
    }

    public DuctTapeCardState(String jsonString) {
        super(jsonString);

        cards = null;
    }

    public DuctTapeCardState(JsonObject jsonObject) {
        super(jsonObject);

        JsonArray cardsArray = jsonObject.get("cards").getAsJsonArray();

        cards = new CardState[cardsArray.size()];
        for (int i = 0; i < cards.length; i++) {
            cards[i] = new CardState(cardsArray.get(i).getAsJsonObject());
        }
    }

    @Override
    public AbstractCard loadCard() {
        return new DuctTapeCard(Arrays.stream(cards).map(CardState::loadCard)
                                      .collect(Collectors.toList()));
    }

    @Override
    public JsonObject jsonEncode() {
        JsonObject result = super.jsonEncode();

        JsonArray cardsArray = new JsonArray();
        for (int i = 0; i < cards.length; i++) {
            cardsArray.add(cards[i].jsonEncode());
        }
        result.add("cards", cardsArray);

        return result;
    }

    @Override
    public String encode() {
        return jsonEncode().toString();
    }

    @SpirePatch(clz = SaveStateMod.class, method = "receivePostInitialize", requiredModId = "chronoMods")
    public static class AddToMapPatch {
        @SpirePostfixPatch
        public static void addThing(SaveStateMod mod) {
            CardState.CardFactories reloadFactories = new CardState.CardFactories(card -> new DuctTapeCardState(card), json -> new DuctTapeCardState(json), jsonObject -> new DuctTapeCardState(jsonObject));

            StateFactories.cardFactoriesByType.put(DuctTapeCard.class, reloadFactories);
            StateFactories.cardFactoriesByCardId.put(DuctTapeCard.ID, reloadFactories);

            String[] extendedArray = new String[StateFactories.cardIds.length + 1];
            for (int i = 0; i < StateFactories.cardIds.length; i++) {
                extendedArray[i] = StateFactories.cardIds[i];
            }
            StateFactories.cardIds = extendedArray;

            int lastIndex = extendedArray.length - 1;
            extendedArray[lastIndex] = DuctTapeCard.ID;
            StateFactories.cardIdToIndexMap.put(DuctTapeCard.ID, lastIndex);
        }
    }
}
