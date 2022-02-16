package twitch;

import basemod.BaseMod;
import basemod.ReflectionHacks;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DescriptionLine;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import hermit.HermitMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class QueryController {
    private final HashMap<String, String> cardsToDescriptionMap;
    private final HashMap<String, String> cardNamesToIdMap;

    private final HashMap<String, String> keywordDescriptionMap;
    private HashMap<String, String> relicDescriptionMap;

    public QueryController() {
        cardNamesToIdMap = new HashMap<>();
        cardsToDescriptionMap = new HashMap<>();
        populateCardMaps();

        keywordDescriptionMap = new HashMap<>();
        populateKeywordMap();

        relicDescriptionMap = new HashMap<>();
        populateRelicMap();
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

    public Optional<String> getDescriptionForRelic(String rawRelicName) {
        String key = prepNameString(rawRelicName);

        if (relicDescriptionMap.containsKey(key)) {
            return Optional.of(relicDescriptionMap.get(key));
        }

        return Optional.empty();
    }

    public Optional<String> getDefinitionForKeyword(String rawKeyword) {
        String key = prepNameString(rawKeyword);

        if (keywordDescriptionMap.containsKey(key)) {
            return Optional.of(keywordDescriptionMap.get(key));
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

    private void populateKeywordMap() {
        GameDictionary.keywords.entrySet().forEach(entry -> {
            String key = entry.getKey();

            key = key.replace("thevacant:", "");

            if (BaseMod.hasModID("HermitState:")) {
                key = key.replace(HermitMod.getModID() + ":", "");
            }

            key = key.toLowerCase().replace(" ", "");

            String description = entry.getValue();

            description = description.replace("#y", "");
            description = description.replace("#b", "");
            description = description.replace("NL", "");

            keywordDescriptionMap.put(key, key + " : " + description);
        });
    }

    private void populateRelicMap() {
        getAllRelics().forEach(relic -> {
            String key = relic.name;

            key = key.replace("thevacant:", "");

            if (BaseMod.hasModID("HermitState:")) {
                key = key.replace(HermitMod.getModID() + ":", "");
            }

            key = key.toLowerCase().replace(" ", "");

            String description = relic.description;

            description = description.replace("#y", "");
            description = description.replace("#b", "");
            description = description.replace("NL", "");

            description = description.replace("thevacant:", "");

            if (BaseMod.hasModID("HermitState:")) {
                description = description.replace(HermitMod.getModID() + ":", "");
            }

            relicDescriptionMap.put(key, relic.name + " : " + description);
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

    private static ArrayList<AbstractRelic> getAllRelics() {
        ArrayList<AbstractRelic> relics = new ArrayList<>();
        @SuppressWarnings("unchecked")
        HashMap<String, AbstractRelic> sharedRelics = ReflectionHacks
                .getPrivateStatic(RelicLibrary.class, "sharedRelics");

        relics.addAll(sharedRelics.values());
        relics.addAll(RelicLibrary.redList);
        relics.addAll(RelicLibrary.greenList);
        relics.addAll(RelicLibrary.blueList);
        relics.addAll(RelicLibrary.whiteList);
        relics.addAll(BaseMod.getAllCustomRelics().values().stream()
                             .flatMap(characterRelicMap -> characterRelicMap.values().stream())
                             .collect(Collectors.toCollection(ArrayList::new)));

        Collections.sort(relics);
        return relics;
    }
}
