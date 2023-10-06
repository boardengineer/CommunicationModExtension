package twitch.votecontrollers;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.relics.Sozu;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import communicationmod.ChoiceScreenUtils;
import tssrelics.relics.DiceOfFate;
import twitch.CommandChoice;
import twitch.RenderHelpers;
import twitch.TwitchController;
import twitch.VoteController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class ShopScreenVoteController extends VoteController {
    private final HashMap<String, Object> voteStringToShopItemMap;
    private final JsonObject stateJson;

    public ShopScreenVoteController(TwitchController twitchController, JsonObject stateJson) {
        super(twitchController);

        voteStringToShopItemMap = new HashMap<>();
        ArrayList<Object> shopItems = ReflectionHacks
                .privateStaticMethod(ChoiceScreenUtils.class, "getAvailableShopItems")
                .invoke();

        for (int i = 0; i < shopItems.size(); i++) {
            String voteString = Integer.toString(i + 1);

            voteStringToShopItemMap.put(voteString, shopItems.get(i));
        }

        this.stateJson = stateJson;
    }

    @Override
    public void setUpChoices() {
        twitchController.setUpDefaultVoteOptions(stateJson);

        boolean hasSozu = AbstractDungeon.player.hasRelic(Sozu.ID);

        boolean hasPotionSlot = AbstractDungeon.player.potions.stream()
                                                              .anyMatch(potion -> potion instanceof PotionSlot);
        boolean canTakePotion = hasPotionSlot && !hasSozu;

        TwitchController.viableChoices = TwitchController.viableChoices.stream()
                                                                       .filter(choice -> choice instanceof CommandChoice)
                                                                       .filter(choice -> (canTakePotion) || !isPotionChoice((CommandChoice) choice))
                                                                       .collect(Collectors
                                                                               .toCollection(ArrayList::new));

        TwitchController.viableChoices
                .add(new CommandChoice("leave", "0", "leave", "proceed"));
    }

    @Override
    public JsonArray getVoteChoicesJson() {

        float rugY = ReflectionHacks
                .getPrivate(AbstractDungeon.shopScreen, ShopScreen.class, "rugY");
        if (Math.abs(rugY) > .25) {
            System.err.println("animating rug, not starting bote yet");
            return new JsonArray();
        }


        JsonArray result = new JsonArray();
        for (int i = 0; i < TwitchController.viableChoices.size(); i++) {
            if (TwitchController.viableChoices.get(i) instanceof CommandChoice) {
                JsonObject optionJson = new JsonObject();

                CommandChoice choice = (CommandChoice) TwitchController.viableChoices.get(i);

                String message = choice.choiceName;
                String voteString = choice.voteString;

                if (message.equals("leave")) {
                    Hitbox hitbox = AbstractDungeon.overlayMenu.cancelButton.hb;

                    optionJson.addProperty("value", choice.voteString);
                    optionJson.addProperty("x_pos", hitbox.x);
                    optionJson.addProperty("y_pos", hitbox.y);
                    optionJson.addProperty("height", hitbox.height);
                    optionJson.addProperty("width", hitbox.width);
                } else if (message.equals("purge")) {
                    Hitbox hitbox = getShopPurgeHitbox();

                    optionJson.addProperty("value", choice.voteString);
                    optionJson.addProperty("x_pos", hitbox.x);
                    optionJson.addProperty("y_pos", hitbox.y);
                    optionJson.addProperty("height", hitbox.height);
                    optionJson.addProperty("width", hitbox.width);
                } else if (voteStringToShopItemMap.containsKey(voteString)) {
                    Hitbox shopItemHitbox = getJsonShopItemHitbox(voteStringToShopItemMap
                            .get(voteString));

                    if (shopItemHitbox != null) {
                        optionJson.addProperty("value", choice.voteString);
                        optionJson.addProperty("x_pos", shopItemHitbox.x);
                        optionJson.addProperty("y_pos", shopItemHitbox.y);
                        optionJson.addProperty("height", shopItemHitbox.height);
                        optionJson.addProperty("width", shopItemHitbox.width);
                    } else {
                        System.err.println("no hitbox for" + choice.choiceName);
                    }
                } else {
                    System.err.println("no shop button for " + choice.choiceName);
                }

                result.add(optionJson);
            }
        }

        return result;
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = TwitchController.getVoteFrequencies();
        Set<String> winningResults = TwitchController.getBestVoteResultKeys();

        for (int i = 0; i < TwitchController.viableChoices.size(); i++) {
            if (TwitchController.viableChoices.get(i) instanceof CommandChoice) {
                CommandChoice choice = (CommandChoice) TwitchController.viableChoices.get(i);

                Color messageColor = winningResults
                        .contains(choice.voteString) ? new Color(1.f, 1.f, 0, 1.f) : new Color(1.f, 0, 0, 1.f);

                String message = choice.choiceName;
                String voteString = choice.voteString;

                if (message.equals("leave")) {
                    String leaveMessage = String.format("[vote %s] (%s)",
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

                    RenderHelpers
                            .renderTextBelowHitbox(spriteBatch, leaveMessage, AbstractDungeon.overlayMenu.cancelButton.hb, messageColor);
                } else if (message.equals("purge")) {
                    String purgeMessage = String.format("[vote %s] (%s)",
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

                    RenderHelpers
                            .renderTextBelowHitbox(spriteBatch, purgeMessage, addGoldHitbox(getShopPurgeHitbox(), 1), messageColor);
                } else if (voteStringToShopItemMap.containsKey(voteString)) {
                    Hitbox shopItemHitbox = getShopItemHitbox(voteStringToShopItemMap
                            .get(voteString));


                    if (shopItemHitbox != null) {
                        String shopMessage = String.format("[vote %s] (%s)",
                                choice.voteString,
                                voteFrequencies.getOrDefault(choice.voteString, 0));

                        RenderHelpers
                                .renderTextBelowHitbox(spriteBatch, shopMessage, shopItemHitbox, messageColor);
                    } else {
                        System.err.println("no hitbox for" + choice.choiceName);
                    }
                } else {
                    System.err.println("no shop button for " + choice.choiceName);
                }
            }
        }
    }

    @Override
    public void endVote() {

    }

    private static String getShopItemString(Object item) {
        if (item instanceof String) {
            return (String) item;
        } else if (item instanceof AbstractCard) {
            return ((AbstractCard) item).name.toLowerCase();
        } else if (item instanceof StoreRelic) {
            return ((StoreRelic) item).relic.name;
        } else if (item instanceof StorePotion) {
            return ((StorePotion) item).potion.name;
        } else if (item instanceof DiceOfFate.RerollStoreChoice) {
            return "Reroll";
        }

        System.err.println("no string can be made for " + item);

        return "";
    }

    private static Hitbox addGoldHitbox(Hitbox hitbox, int slot) {
        return new Hitbox(hitbox.x + (slot - 1) * 50, hitbox.y - 62.0F * Settings.scale, hitbox.width, hitbox.height + 62.F * Settings.scale);
    }

    private Hitbox getShopPurgeHitbox() {
        ShopScreen screen = AbstractDungeon.shopScreen;
        float CARD_W = 110.0F * Settings.scale;
        float CARD_H = 150.0F * Settings.scale;
        float purgeCardX = ReflectionHacks.getPrivate(screen, ShopScreen.class, "purgeCardX");
        float purgeCardY = ReflectionHacks.getPrivate(screen, ShopScreen.class, "purgeCardY");

        float height = 2 * CARD_H;
        float width = 2 * CARD_W;

        return new Hitbox(purgeCardX - CARD_W, purgeCardY - CARD_H, width, height);
    }

    private static Hitbox getShopItemHitbox(Object item) {
        if (item instanceof String) {
            System.err.println("no hitbox for string " + item);
            return null;
        } else if (item instanceof AbstractCard) {
            return addGoldHitbox(((AbstractCard) item).hb, 1);
        } else if (item instanceof StoreRelic) {
            StoreRelic storeRelic = (StoreRelic) item;
            return addGoldHitbox(storeRelic.relic.hb, ReflectionHacks
                    .getPrivate(storeRelic, StoreRelic.class, "slot"));
        } else if (item instanceof StorePotion) {
            StorePotion storePotion = (StorePotion) item;
            return addGoldHitbox(storePotion.potion.hb, ReflectionHacks
                    .getPrivate(storePotion, StorePotion.class, "slot"));
        } else if (item instanceof DiceOfFate.RerollStoreChoice) {
            return DiceOfFate.rerollChoice.hb;
        }

        System.err.println("no string can be made for " + item);

        return null;
    }

    private static Hitbox getJsonShopItemHitbox(Object item) {
        if (item instanceof String) {
            System.err.println("no hitbox for string " + item);
            return null;
        } else if (item instanceof AbstractCard) {
            return ((AbstractCard) item).hb;
        } else if (item instanceof StoreRelic) {
            StoreRelic storeRelic = (StoreRelic) item;
            return storeRelic.relic.hb;
        } else if (item instanceof StorePotion) {
            StorePotion storePotion = (StorePotion) item;
            return storePotion.potion.hb;
        } else if (item instanceof DiceOfFate.RerollStoreChoice) {
            return DiceOfFate.rerollChoice.hb;
        }

        return null;
    }

    private static boolean isPotionChoice(CommandChoice choice) {
        if (choice.choiceName.equals("Fire Potion")) {
            return true;
        }

        return CombatRewardVoteController.POTION_NAMES
                .contains(choice.choiceName.toLowerCase()) || choice.choiceName
                .toLowerCase().contains("potion");
    }
}
