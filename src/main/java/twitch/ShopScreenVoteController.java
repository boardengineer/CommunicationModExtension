package twitch;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

import java.util.ArrayList;
import java.util.HashMap;

public class ShopScreenVoteController implements VoteController {
    private final HashMap<String, Object> messageToShopItemMap;
    private final TwitchController twitchController;

    ShopScreenVoteController(TwitchController twitchController) {
        this.twitchController = twitchController;
        messageToShopItemMap = new HashMap<>();
        ArrayList<Object> shopItems = ReflectionHacks
                .privateStaticMethod(ChoiceScreenUtils.class, "getAvailableShopItems")
                .invoke();
        boolean hasSozu = AbstractDungeon.player.hasRelic(Sozu.ID);
        boolean hasPotionSlot = AbstractDungeon.player.potions.stream().anyMatch(potion -> potion instanceof PotionSlot);

        for (Object item : shopItems) {
            if(!(item instanceof StorePotion) || !hasSozu && hasPotionSlot) {
                messageToShopItemMap.put(getShopItemString(item).toLowerCase(), item);
            }
        }
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

            String message = choice.choiceName;
            if (message.equals("leave")) {
                String leaveMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                RenderHelpers
                        .renderTextBelowHitbox(spriteBatch, leaveMessage, AbstractDungeon.overlayMenu.cancelButton.hb);
            } else if (message.equals("purge")) {
                String purgeMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                RenderHelpers
                        .renderTextBelowHitbox(spriteBatch, purgeMessage, addGoldHitbox(getShopPurgeHitbox(), 1));
            } else if (messageToShopItemMap.containsKey(message)) {
                Hitbox shopItemHitbox = getShopItemHitbox(messageToShopItemMap
                        .get(message));


                if (shopItemHitbox != null) {
                    String shopMessage = String.format("[vote %s] (%s)",
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

                    RenderHelpers.renderTextBelowHitbox(spriteBatch, shopMessage, shopItemHitbox);
                } else {
                    System.err.println("no hitbox for" + choice.choiceName);
                }
            } else {
                System.err.println("no shop button for " + choice.choiceName);
            }
        }
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
        }

        System.err.println("no string can be made for " + item);

        return null;
    }
}
