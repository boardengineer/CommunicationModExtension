package friends.patches;

import chronoMods.TogetherManager;
import chronoMods.coop.CoopCourierScreen;
import chronoMods.coop.CoopDeathNotification;
import chronoMods.network.NetworkHelper;
import chronoMods.network.RemotePlayer;
import chronoMods.ui.deathScreen.EndScreenCoopLoss;
import chronoMods.ui.deathScreen.NewDeathScreenPatches;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import communicationmod.CommunicationMod;
import twitch.TwitchController;

import java.nio.ByteBuffer;
import java.util.HashSet;

public class NetworkingPatches {
    private static final HashSet<NetworkHelper.dataType> TRANSFER_TYPES = new HashSet<NetworkHelper.dataType>() {{
        add(NetworkHelper.dataType.TransferPotion);
        add(NetworkHelper.dataType.TransferCard);
        add(NetworkHelper.dataType.TransferBooster);
        add(NetworkHelper.dataType.TransferRelic);
    }};

    @SpirePatch(clz = NetworkHelper.class, method = "parseData", optional = true, requiredModId = "chronoMods")
    public static class DelayLifeLossPatch {
        @SpirePrefixPatch
        public static SpireReturn delayLifeLoss(ByteBuffer data, RemotePlayer playerInfo) {
            data.mark();
            SpireReturn result = delayLifeLossHelper(data, playerInfo);
            if (result == SpireReturn.Continue()) {
                data.reset();
            }
            return result;
        }

        private static SpireReturn delayLifeLossHelper(ByteBuffer data, RemotePlayer playerInfo) {
            int enumIndex = data.getInt();

            if (enumIndex > NetworkHelper.dataType.values().length || enumIndex < 0) {
                return SpireReturn.Return(null);
            }

            NetworkHelper.dataType type = NetworkHelper.dataType.values()[enumIndex];
            if (type == NetworkHelper.dataType.LoseLife && TwitchController.inBattle) {
                if (playerInfo.isUser(TogetherManager.currentUser)) {
                    int counter = data.getInt(4);
                    if (counter >= 0) {
                        AbstractDungeon.effectList.add(new CoopDeathNotification(playerInfo));
                        if (AbstractDungeon.player.hasBlight("StringOfFate")) {
                            if (AbstractDungeon.player.getBlight("StringOfFate").increment > 0) {
                                AbstractDungeon.player.getBlight("StringOfFate").increment = 0;
                                SpireReturn.Return(null);
                            }

                            AbstractDungeon.player.getBlight("StringOfFate").counter = counter;
                            AbstractDungeon.player
                                    .decreaseMaxHealth(AbstractDungeon.player.maxHealth / 4);
                            if (AbstractDungeon.player.currentHealth > AbstractDungeon.player.maxHealth) {
                                AbstractDungeon.player.currentHealth = AbstractDungeon.player.maxHealth;
                            }
                        }
                    } else {
                        AbstractDungeon.player.currentHealth = 0;
                        AbstractDungeon.player.isDead = true;
                        NewDeathScreenPatches.EndScreenBase = new EndScreenCoopLoss(AbstractDungeon
                                .getCurrRoom().monsters);
                        AbstractDungeon.screen = NewDeathScreenPatches.Enum.RACEEND;
                    }

                    return SpireReturn.Return(null);
                } else {
                    int counter = data.getInt(4);
                    AbstractDungeon.effectList.add(new CoopDeathNotification(playerInfo));
                    TwitchController.messageQueue.offer(new LifeLossMessage(counter));

                    return SpireReturn.Return(null);
                }
            } else if (TRANSFER_TYPES.contains(type)) {
                // Restart the vote of someone sent us something
                if (AbstractDungeon.isScreenUp &&
                        AbstractDungeon.screen == CoopCourierScreen.Enum.COURIER) {
                    long steamId = data.getLong(4);
                    if (TogetherManager.currentUser.isUser(steamId)) {
                        TwitchController.disableVote();
                        CommunicationMod.mustSendGameState = true;
                    }
                }
            }

            return SpireReturn.Continue();
        }
    }

    public interface DelayedMessage {
        void execute();
    }

    public static class LifeLossMessage implements DelayedMessage {
        private final int counter;

        public LifeLossMessage(int counter) {
            this.counter = counter;
        }

        @Override
        public void execute() {
            if (counter >= 0) {
                if (AbstractDungeon.player.hasBlight("StringOfFate")) {
                    if (AbstractDungeon.player.getBlight("StringOfFate").increment > 0) {
                        AbstractDungeon.player.getBlight("StringOfFate").increment = 0;
                        return;
                    }

                    AbstractDungeon.player.getBlight("StringOfFate").counter = counter;
                    AbstractDungeon.player
                            .decreaseMaxHealth(AbstractDungeon.player.maxHealth / 4);
                    if (AbstractDungeon.player.currentHealth > AbstractDungeon.player.maxHealth) {
                        AbstractDungeon.player.currentHealth = AbstractDungeon.player.maxHealth;
                    }
                }
            } else {
                AbstractDungeon.player.currentHealth = 0;
                AbstractDungeon.player.isDead = true;
                NewDeathScreenPatches.EndScreenBase = new EndScreenCoopLoss(AbstractDungeon
                        .getCurrRoom().monsters);
                AbstractDungeon.screen = NewDeathScreenPatches.Enum.RACEEND;
            }
        }
    }
}
