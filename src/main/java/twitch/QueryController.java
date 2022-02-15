package twitch;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DescriptionLine;
import com.megacrit.cardcrawl.helpers.CardLibrary;

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class QueryController {
    private final TwitchController twitchController;

    private final HashMap<String, String> cardsToDescriptionMap;
    private final HashMap<String, String> cardNamesToIdMap;

    private HashMap<String, String> keywordDescriptionMap;
    private HashMap<String, String> relicDescriptionMap;

    public QueryController(TwitchController twitchController) {
        this.twitchController = twitchController;

        cardNamesToIdMap = new HashMap<>();
        cardsToDescriptionMap = new HashMap<>();

        populateCardMaps();

    }

    public Optional<String> getDescriptionForCard(String rawCardName) {
        String key = prepNameString(rawCardName);

        if (cardsToDescriptionMap.containsKey(key)) {
            return Optional.of(cardsToDescriptionMap.get(key));
        }

        return Optional.empty();
    }

    public Optional<String> getIdForCard(String rawCardName) {
        String key = prepNameString(rawCardName);

        if (cardNamesToIdMap.containsKey(key)) {
            return Optional.of(cardNamesToIdMap.get(key));
        }

        return Optional.empty();
    }

    private void populateCardMaps() {
        CardLibrary.cards.values().stream()
                         .forEach(card -> {
                             String name = prepNameString(card.name);
                             String description = card.description
                                     .stream()
                                     .map(line -> replaceStringSegmentsForCard(line, card))
                                     .collect(Collectors
                                             .joining(" "));

                             String descriptionResult = String
                                     .format("%s: %s", card.name, description);
                             cardsToDescriptionMap.put(name, descriptionResult);
                             cardNamesToIdMap.put(name, card.cardID);

                             try {
                                 card.upgrade();
                                 String upgradedName = name + "+";
                                 String upgradedDescription = card.description
                                         .stream()
                                         .map(line -> replaceStringSegmentsForCard(line, card))
                                         .collect(Collectors
                                                 .joining(" "));
                                 String upgradedDescriptionResult = String
                                         .format("%s: %s", card.name, upgradedDescription);
                                 cardsToDescriptionMap.put(upgradedName, upgradedDescriptionResult);
                             } catch (NullPointerException e) {
                                 // upgrading sometimes nulls out, hopefully just for curses.
                             }
                         });
    }

    private static String replaceStringSegmentsForCard(DescriptionLine line, AbstractCard card) {
        String result = line.text;

        result = result.replace("!B!", Integer.toString(card.baseBlock));
        result = result.replace("!D!", Integer.toString(card.baseDamage));
        result = result.replace("!M!", Integer.toString(card.baseMagicNumber));

        return result;
    }

    private String prepNameString(String name) {
        return name.toLowerCase().replace(" ", "");
    }
}
