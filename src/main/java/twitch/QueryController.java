package twitch;

import basemod.BaseMod;
import basemod.ReflectionHacks;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DescriptionLine;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.stats.CharStat;
import com.megacrit.cardcrawl.shop.ShopScreen;
import hermit.HermitMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class QueryController {
    private static final long DECK_DISPLAY_TIMEOUT = 60_000;
    private static final long RELIC_DISPLAY_TIMEOUT = 60_000;
    private static final long BOSS_DISPLAY_TIMEOUT = 30_000;

    private final TwitchController twitchController;

    private final HashMap<String, String> cardsToDescriptionMap;
    private final HashMap<String, String> cardNamesToIdMap;

    private final HashMap<String, String> keywordDescriptionMap;
    private final HashMap<String, String> relicDescriptionMap;

    private static long lastDeckDisplayTimestamp = 0L;
    public static long lastBossDisplayTimestamp = 0L;
    public static long lastRelicDisplayTimestamp = 0L;

    public QueryController(TwitchController twitchController) {
        this.twitchController = twitchController;

        cardNamesToIdMap = new HashMap<>();
        cardsToDescriptionMap = new HashMap<>();
        populateCardMaps();

        keywordDescriptionMap = new HashMap<>();
        populateKeywordMap();

        relicDescriptionMap = new HashMap<>();
        populateRelicMap();
    }

    public void maybeRunQuery(String[] tokens) {
        if (tokens.length == 0) {
            return;
        }

        try {
            switch (tokens[0]) {
                case "!deck":
                    runDeckQuery();
                    break;
                case "!boss":
                    runBossQuery();
                    break;
                case "!relics":
                    queryAllRelics();
                    break;
                case "!card":
                    queryCard(tokens);
                    break;
                case "!relic":
                    queryRelic(tokens);
                    break;
                case "!info":
                    queryKeyword(tokens);
                    break;
                case "!status":
                    runStatusQuery();
                    break;
                case "!potions":
                    runPotionQuery();
                    break;
                case "!purgecost":
                    runPurgeCostQuery();
                    break;
                case "!runstats":
                    runStatsQuery();
                    break;
                case "!notecard":
                    runNotecardQuery();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                                     .format("(%s) %s [%d]: %s, %s: %s",
                                             card.color.name().toLowerCase(), card.name, card.cost,
                                             card.rarity.name().toLowerCase(),
                                             card.type.name().toLowerCase(), description);
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
                                         .format("(%s) %s [%d]: %s, %s: %s",
                                                 card.color.name()
                                                           .toLowerCase(), card.name, card.cost,
                                                 card.rarity.name().toLowerCase(),
                                                 card.type.name()
                                                          .toLowerCase(), upgradedDescription);
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

    private void runDeckQuery() {
        long now = System.currentTimeMillis();
        if (now > lastDeckDisplayTimestamp + DECK_DISPLAY_TIMEOUT) {
            lastDeckDisplayTimestamp = now;
            HashMap<String, Integer> cards = new HashMap<>();
            AbstractDungeon.player.masterDeck.group
                    .forEach(c -> cards.merge(c.name, 1, Integer::sum));

            StringBuilder sb = new StringBuilder("[BOT] Deck: ");
            for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                if (cards.containsKey(c.name)) {
                    sb.append(c.name);
                    int amt = cards.get(c.name);
                    if (amt > 1) {
                        sb.append(" x").append(amt);
                    }
                    sb.append(";");
                    cards.remove(c.name);
                }
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }

            TwitchController.twirk.channelMessage(sb.toString());
        }
    }

    private void runBossQuery() {
        long now = System.currentTimeMillis();
        if (now > lastBossDisplayTimestamp + BOSS_DISPLAY_TIMEOUT) {
            lastBossDisplayTimestamp = now;
            TwitchController.twirk
                    .channelMessage("[BOT] Upcoming Boss: " + AbstractDungeon.bossKey);
        }
    }

    private void runStatusQuery() {
        try {
            String message = String
                    .format("[BOT] HP: %d | Floor: %d | Gold: %d", AbstractDungeon.player.currentHealth, AbstractDungeon.floorNum, AbstractDungeon.player.gold);

            TwitchController.twirk.channelMessage(message);
        } catch (NullPointerException | IllegalArgumentException e) {

        }
    }

    private void runPurgeCostQuery() {
        try {
            String message = String
                    .format("[BOT] purge cost: %d", ShopScreen.actualPurgeCost);

            TwitchController.twirk.channelMessage(message);
        } catch (NullPointerException | IllegalArgumentException e) {

        }
    }

    private void runStatsQuery() {
        try {
            String statsString = CardCrawlGame.characterManager.getAllCharacters().stream()
                                                               .map(character -> {
                                                                   CharStat stats = character
                                                                           .getCharStat();
                                                                   return character.chosenClass
                                                                           .name() + " " + stats
                                                                           .getVictoryCount() + "-" + stats
                                                                           .getDeathCount();
                                                               }).collect(Collectors.joining(";"));

            String message = String
                    .format("[BOT] Run History Stats--- %s", statsString);

            TwitchController.twirk.channelMessage(message);
        } catch (NullPointerException | IllegalArgumentException e) {

        }
    }

    private void runNotecardQuery() {
        try {
            String message = String
                    .format("[BOT] Card in the wall - %s", CardLibrary
                            .getCard(CardCrawlGame.playerPref
                                    .getString("NOTE_CARD", "Iron Wave")).name);

            TwitchController.twirk.channelMessage(message);
        } catch (NullPointerException | IllegalArgumentException e) {

        }
    }


    private void runPotionQuery() {
        try {
            ArrayList<AbstractPotion> potions = AbstractDungeon.player.potions;

            String potionsString = potions.stream()
                                          .filter(potion -> !(potion instanceof PotionSlot))
                                          .map(potion -> potion.name)
                                          .collect(Collectors.joining(" : "));

            String message = String
                    .format("[BOT] potions: %s", potionsString);

            TwitchController.twirk.channelMessage(message);
        } catch (NullPointerException | IllegalArgumentException e) {

        }
    }

    private void queryAllRelics() {
        long now = System.currentTimeMillis();
        if (now > lastRelicDisplayTimestamp + RELIC_DISPLAY_TIMEOUT) {
            lastRelicDisplayTimestamp = now;

            String relics = AbstractDungeon.player.relics.stream()
                                                         .map(relic -> relic.relicId)
                                                         .collect(Collectors
                                                                 .joining(";"));

            TwitchController.twirk.channelMessage("[BOT] Relic List: " + relics);
        }
    }

    private void queryCard(String[] tokens) {
        String queryString = "";
        for (int i = 1; i < tokens.length; i++) {
            queryString += tokens[i].toLowerCase();
        }
        Optional<String> queryResult = getDescriptionForCard(queryString);

        if (queryResult.isPresent()) {
            TwitchController.twirk.channelMessage("[BOT] " + queryResult.get());
        }
    }

    private void queryRelic(String[] tokens) {
        String queryString = "";
        for (int i = 1; i < tokens.length; i++) {
            queryString += tokens[i].toLowerCase();
        }
        Optional<String> queryResult = getDescriptionForRelic(queryString);

        if (queryResult.isPresent()) {
            TwitchController.twirk.channelMessage("[BOT] " + queryResult.get());
        }
    }

    private void queryKeyword(String[] tokens) {
        String queryString = "";
        for (int i = 1; i < tokens.length; i++) {
            queryString += tokens[i].toLowerCase();
        }
        Optional<String> queryResult = getDefinitionForKeyword(queryString);

        if (queryResult.isPresent()) {
            TwitchController.twirk.channelMessage("[BOT] " + queryResult.get());
        }
    }
}
