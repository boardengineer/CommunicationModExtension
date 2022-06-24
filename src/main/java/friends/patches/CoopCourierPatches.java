package friends.patches;

import basemod.ReflectionHacks;
import chronoMods.TogetherManager;
import chronoMods.coop.*;
import chronoMods.network.NetworkHelper;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.TheEnding;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.map.DungeonMap;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.ui.buttons.ProceedButton;
import communicationmod.ChoiceScreenUtils;
import communicationmod.CommunicationMod;
import communicationmod.patches.DungeonMapPatch;
import communicationmod.patches.MapRoomNodeHoverPatch;
import twitch.Command;
import twitch.CommandChoice;
import twitch.ExtendTimerCommand;
import twitch.TwitchController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class CoopCourierPatches {
    @SpireEnum
    static ChoiceScreenUtils.ChoiceType COURIER_ROOM;
    @SpireEnum
    static ChoiceScreenUtils.ChoiceType COURIER_SCREEN;
    @SpireEnum
    static ChoiceScreenUtils.ChoiceType TEAMRELICS_SCREEN;

    @SpirePatch(clz = ChoiceScreenUtils.class, method = "getCurrentChoiceType", optional = true, requiredModId = "chronoMods")
    public static class GetCourierChoiceTypePatch {
        @SpirePrefixPatch
        public static SpireReturn addCourierTypes() {
            if (!AbstractDungeon.isScreenUp) {
                if (AbstractDungeon.getCurrRoom() instanceof CoopCourierRoom) {
                    return SpireReturn.Return(COURIER_ROOM);
                }
            } else {
                if (AbstractDungeon.screen == CoopCourierScreen.Enum.COURIER) {
                    return SpireReturn.Return(COURIER_SCREEN);
                } else if (AbstractDungeon.screen == CoopBossRelicSelectScreen.Enum.TEAMRELIC) {
                    return SpireReturn.Return(TEAMRELICS_SCREEN);
                }
            }

            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = ChoiceScreenUtils.class, method = "isConfirmButtonAvailable", paramtypez = {ChoiceScreenUtils.ChoiceType.class}, optional = true, requiredModId = "chronoMods")
    public static class AvailableConfirmButtonPatch {
        @SpirePrefixPatch
        public static SpireReturn checkCourChoiceType(ChoiceScreenUtils.ChoiceType choiceType) {
            if (choiceType == COURIER_ROOM) {
                return SpireReturn.Return(true);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = CoopNeowChoice.class, method = "registerButtonSelection", optional = true, requiredModId = "chronoMods")
    public static class RestartCoopEventVote {
        @SpirePostfixPatch
        public static void maybeRestartChoice(CoopNeowChoice choice) {
            // Restart the vote every time a new selection is made
            if (!choice.playerInfo.isUser(TogetherManager.currentUser)) {
                TwitchController.disableVote();
                CommunicationMod.mustSendGameState = true;
            }
        }
    }

    // Leave the Shop room (not screen) proceed <=> confirm
    @SpirePatch(clz = TwitchController.class, method = "setUpDefaultVoteOptions", optional = true, requiredModId = "chronoMods")
    public static class LeaveCourierForTwitchPatch {
        @SpirePostfixPatch
        public static void checkCourChoiceType(TwitchController twitchController) {
            if (AbstractDungeon.isScreenUp && AbstractDungeon.screen == CoopCourierScreen.Enum.COURIER) {
                TwitchController.viableChoices
                        .add(new CommandChoice("leave", "0", "leave", "proceed"));
            }
        }
    }

    // Add a a wait option and
    @SpirePatch(clz = TwitchController.class, method = "startVote", paramtypez = {long.class, boolean.class}, optional = true, requiredModId = "chronoMods")
    public static class AddExtendTimePatch {
        @SpirePrefixPatch
        public static void checkCourChoiceType(TwitchController twitchController) {
            if (TwitchController.viableChoices.size() > 1) {
                TwitchController.viableChoices
                        .add(new ExtendTimerCommand(Integer
                                .toString(TwitchController.viableChoices.size() + 1)));

                TwitchController.choicesMap = new HashMap<>();
                for (Command choice : TwitchController.viableChoices) {
                    TwitchController.choicesMap.put(choice.getVoteString(), choice);
                }
            }
        }
    }

    @SpirePatch(clz = ChoiceScreenUtils.class, method = "getConfirmButtonText", paramtypez = {ChoiceScreenUtils.ChoiceType.class}, optional = true, requiredModId = "chronoMods")
    public static class ConfirmButtonTextPatch {
        @SpirePrefixPatch
        public static SpireReturn checkCourChoiceType(ChoiceScreenUtils.ChoiceType choiceType) {
            if (choiceType == COURIER_ROOM) {
                return SpireReturn.Return("proceed");
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = ChoiceScreenUtils.class, method = "pressConfirmButton", paramtypez = {ChoiceScreenUtils.ChoiceType.class}, optional = true, requiredModId = "chronoMods")
    public static class ConfirmButtonPressPatch {
        @SpirePrefixPatch
        public static SpireReturn checkCourChoiceType(ChoiceScreenUtils.ChoiceType choiceType) {
            if (choiceType == COURIER_ROOM) {
                AbstractDungeon.overlayMenu.proceedButton.show();
                Hitbox hb = ReflectionHacks
                        .getPrivate(AbstractDungeon.overlayMenu.proceedButton, ProceedButton.class, "hb");
                hb.clicked = true;
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    // Leave the Shop screen (not room) leave <=> cancel
    @SpirePatch(clz = ChoiceScreenUtils.class, method = "isCancelButtonAvailable", paramtypez = {ChoiceScreenUtils.ChoiceType.class}, optional = true, requiredModId = "chronoMods")
    public static class AvailableCancelButtonPatch {
        @SpirePrefixPatch
        public static SpireReturn checkCourChoiceType(ChoiceScreenUtils.ChoiceType choiceType) {
            if (choiceType == COURIER_SCREEN) {
                return SpireReturn.Return(true);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = ChoiceScreenUtils.class, method = "getCancelButtonText", paramtypez = {ChoiceScreenUtils.ChoiceType.class}, optional = true, requiredModId = "chronoMods")
    public static class CancelButtonTextPatch {
        @SpirePrefixPatch
        public static SpireReturn checkCourChoiceType(ChoiceScreenUtils.ChoiceType choiceType) {
            if (choiceType == COURIER_SCREEN) {
                return SpireReturn.Return("leave");
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = ChoiceScreenUtils.class, method = "bossNodeAvailable", optional = true, requiredModId = "chronoMods")
    public static class AdjustBossPosition {
        @SpirePrefixPatch
        public static SpireReturn checkAdjustedBossPosition() {
            MapRoomNode currMapNode = AbstractDungeon.getCurrMapNode();
            return SpireReturn.Return(currMapNode.y == 15 || (AbstractDungeon.id
                    .equals(TheEnding.ID) && currMapNode.y == 2));
        }
    }

    @SpirePatch(clz = ChoiceScreenUtils.class, method = "makeMapChoice", optional = true, requiredModId = "chronoMods")
    public static class AdjustBossPositionMakeChoicePatch {
        @SpirePrefixPatch
        public static SpireReturn AdjustMakeBossPositionPatch(int choice) {
            System.err.println("makeMapChoice patch triggering");
            MapRoomNode currMapNode = AbstractDungeon.getCurrMapNode();
            if (currMapNode.y == 15 || (AbstractDungeon.id
                    .equals(TheEnding.ID) && currMapNode.y == 2)) {
                if (choice == 0) {
                    CoopBossPatches.activateBossNode();
                    return SpireReturn.Return(null);
                } else {
                    throw new IndexOutOfBoundsException("Only a boss node can be chosen here.");
                }
            }
            ArrayList<MapRoomNode> nodeChoices = ChoiceScreenUtils.getMapScreenNodeChoices();
            MapRoomNodeHoverPatch.hoverNode = nodeChoices.get(choice);
            MapRoomNodeHoverPatch.doHover = true;
            AbstractDungeon.dungeonMapScreen.clicked = true;
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch(clz = CoopBossPatches.DungeonMapIsShitty.class, method = "Prefix", optional = true, requiredModId = "chronoMods")
    public static class DungeonPatchPatch {
        @SpirePostfixPatch
        public static void AdjustMakeBossPositionPatch(DungeonMap dungeonMap) {
            if (DungeonMapPatch.doBossHover) {
                dungeonMap.bossHb.hovered = true;
                InputHelper.justClickedLeft = true;
                DungeonMapPatch.doBossHover = false;
            }
        }
    }

    @SpirePatch(clz = ChoiceScreenUtils.class, method = "pressCancelButton", paramtypez = {ChoiceScreenUtils.ChoiceType.class}, optional = true, requiredModId = "chronoMods")
    public static class CancelButtonPressPatch {
        @SpirePrefixPatch
        public static SpireReturn checkCourChoiceType(ChoiceScreenUtils.ChoiceType choiceType) {
            if (choiceType == COURIER_SCREEN) {
                AbstractDungeon.overlayMenu.cancelButton.hb.clicked = true;
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = ChoiceScreenUtils.class, method = "getCurrentChoiceList", optional = true, requiredModId = "chronoMods")
    public static class AddCourierChoiceOptions {
        @SpirePrefixPatch
        public static SpireReturn addCourierTypes() {
            ChoiceScreenUtils.ChoiceType choiceType = ChoiceScreenUtils.getCurrentChoiceType();

            if (choiceType == COURIER_ROOM) {
                return SpireReturn.Return(getCourierRoomChoices());
            } else if (choiceType == COURIER_SCREEN) {
                return SpireReturn.Return(getCourierScreenItems());
            } else if (choiceType == TEAMRELICS_SCREEN) {
                return SpireReturn.Return(getTeamRelics());
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = ChoiceScreenUtils.class, method = "executeChoice", optional = true, requiredModId = "chronoMods")
    public static class ExecuteCourierOptionsPatch {
        @SpirePrefixPatch
        public static SpireReturn executeCourierTypes(int choiceIndex) {
            ChoiceScreenUtils.ChoiceType choiceType = ChoiceScreenUtils.getCurrentChoiceType();

            if (choiceType == COURIER_ROOM) {
                makeCourierRoomChoice();
                return SpireReturn.Return(null);
            } else if (choiceType == COURIER_SCREEN) {
                getCourierScreenChoices().get(choiceIndex).select();
                CommunicationMod.mustSendGameState = true;
                return SpireReturn.Return(null);
            } else if (choiceType == TEAMRELICS_SCREEN) {
                selectTeamRelic(choiceIndex);
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    public static void selectTeamRelic(int choiceIndex) {
        CoopBossRelicSelectScreen teamRelicScreen = TogetherManager.teamRelicScreen;

        if (choiceIndex != teamRelicScreen.selectedIndex) {
            teamRelicScreen.selected.get(choiceIndex).add(TogetherManager.currentUser);

            if (teamRelicScreen.selectedIndex != -1) {
                teamRelicScreen.selected.get(teamRelicScreen.selectedIndex)
                                        .remove(TogetherManager.currentUser);
            }

            teamRelicScreen.selectedIndex = choiceIndex;
        }

        // Share your choice with the world
        NetworkHelper.sendData(NetworkHelper.dataType.ChooseTeamRelic);

        if (teamRelicScreen.selected.get(choiceIndex).size() == TogetherManager.players.size()) {
            AbstractBlight blight = teamRelicScreen.blights.get(choiceIndex);
            blight.obtain();
            blight.onEquip();
            blight.isObtained = true;
            TogetherManager.teamRelicScreen.blightChoiceComplete();
        }

    }

    public static void makeCourierRoomChoice() {
        TogetherManager.courierScreen.open();
    }

    public static ArrayList<String> getCourierRoomChoices() {
        ArrayList<String> choices = new ArrayList<>();
        choices.add("shop");
        return choices;
    }

    public static ArrayList<String> getTeamRelics() {
        return TogetherManager.teamRelicScreen.blights.stream().map(blight -> blight.name)
                                                      .collect(Collectors
                                                              .toCollection(ArrayList::new));
    }

    public static ArrayList<String> getCourierScreenItems() {
        return getCourierScreenChoices().stream().map(CourChoice::getDisplayString)
                                        .collect(Collectors
                                                .toCollection(ArrayList::new));
    }

    private static ArrayList<CourChoice> getCourierScreenChoices() {
        ArrayList<CourChoice> choices = new ArrayList<>();
        CoopCourierScreen screen = TogetherManager.courierScreen;
        boolean hasRecipient = false;

        if (!TogetherManager.getCurrentUser().packages.isEmpty()) {
            choices.add(new RewardCourChoice());
        }

        for (CoopCourierRecipient player : screen.players) {
            if (!player.selected) {
                PlayerCourChoice playerChoice = new PlayerCourChoice();
                playerChoice.recipient = player;
                choices.add(playerChoice);
            } else {
                hasRecipient = true;
            }
        }

        if (hasRecipient) {
            for (CoopCourierRelic relic : screen.relics) {
                if (relic.price <= AbstractDungeon.player.gold) {
                    RelicCourChoice relicChoice = new RelicCourChoice();
                    relicChoice.relic = relic;
                    choices.add(relicChoice);
                }
            }

            for (int cardIndex = 0; cardIndex < 3; cardIndex++) {
                if (screen.cards[cardIndex] != null) {
                    AbstractCard card = screen.cards[cardIndex];
                    if (card.price <= AbstractDungeon.player.gold) {
                        CardCourChoice cardCourChoice = new CardCourChoice();
                        cardCourChoice.card = card;
                        choices.add(cardCourChoice);
                    }
                } else {
                    if (screen.boosterActive[cardIndex]) {
                        int price = 15 * (cardIndex + 1);
                        if (cardIndex == 2) {
                            price = 75;
                        }

                        if (AbstractDungeon.ascensionLevel >= 16) {
                            price = (int) ((float) price * 1.1F);
                        }

                        if (AbstractDungeon.player.hasRelic("The Courier")) {
                            price = (int) ((float) price * 0.8F);
                        }

                        if (AbstractDungeon.player.hasRelic("Membership Card")) {
                            price = (int) ((float) price * 0.5F);
                        }

                        if (AbstractDungeon.player.hasRelic("Ectoplasm")) {
                            price = (int) ((float) price * 0.5F);
                        }

                        if (price <= AbstractDungeon.player.gold) {
                            BoosterCourChoice boosterChoice = new BoosterCourChoice();
                            boosterChoice.boosterindex = cardIndex;
                            choices.add(boosterChoice);
                        }
                    }
                }
            }

            for (CoopCourierPotion potion : screen.potions) {
                if (potion.price <= AbstractDungeon.player.gold) {
                    PotionCourChoice potionChoice = new PotionCourChoice();
                    potionChoice.potion = potion;
                    choices.add(potionChoice);
                }
            }
        }

        return choices;
    }

    interface CourChoice {
        void select();

        String getDisplayString();
    }

    static class RelicCourChoice implements CourChoice {
        CoopCourierRelic relic;

        @Override
        public void select() {
            relic.purchaseRelic();
        }

        @Override
        public String getDisplayString() {
            return relic.relic.name;
        }
    }

    static class CardCourChoice implements CourChoice {
        int cardIndex;
        AbstractCard card;

        @Override
        public void select() {
            TogetherManager.courierScreen.purchaseCard(card, cardIndex);
        }

        @Override
        public String getDisplayString() {
            return card.name;
        }
    }

    static class BoosterCourChoice implements CourChoice {
        int boosterindex;

        @Override
        public void select() {
            TogetherManager.courierScreen.purchaseBooster(boosterindex);
        }

        @Override
        public String getDisplayString() {
            return "booster";
        }
    }

    static class PotionCourChoice implements CourChoice {
        CoopCourierPotion potion;

        @Override
        public void select() {
            potion.purchasePotion();
        }

        @Override
        public String getDisplayString() {
            return potion.potion.name;
        }
    }

    static class PlayerCourChoice implements CourChoice {
        CoopCourierRecipient recipient;

        @Override
        public void select() {
            recipient.parent.deselect();
            recipient.selected = true;
            recipient.hb.clicked = false;
        }

        @Override
        public String getDisplayString() {
            return recipient.player.userName;
        }
    }

    static class RewardCourChoice implements CourChoice {
        @Override
        public void select() {
            AbstractDungeon.getCurrRoom().rewards.clear();
            AbstractDungeon.getCurrRoom().rewards = new ArrayList(TogetherManager
                    .getCurrentUser().packages);
            AbstractDungeon.combatRewardScreen.open(CoopCourierScreen.TALK[22]);
            AbstractDungeon.combatRewardScreen.rewards
                    .remove(AbstractDungeon.combatRewardScreen.rewards.size() - 1);
            AbstractDungeon.getCurrRoom().rewardPopOutTimer = 0.0F;
            TogetherManager.getCurrentUser().packages.clear();
        }

        @Override
        public String getDisplayString() {
            return "reward box";
        }
    }
}
