import basemod.BaseMod;
import basemod.ReflectionHacks;
import basemod.interfaces.PostInitializeSubscriber;
import battleaimod.BattleAiMod;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import communicationmod.CommandExecutor;
import communicationmod.CommunicationMod;
import communicationmod.GameStateConverter;
import communicationmod.InvalidCommandException;
import de.robojumper.ststwitch.TwitchConfig;
import ludicrousspeed.Controller;
import twitch.TwitchController;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@SpireInitializer
public class CommunicationModExtension implements PostInitializeSubscriber {
    public static CommunicationMethod communicationMethod = CommunicationMethod.TWITCH_CHAT;
    private static final int PORT = 8080;

    enum CommunicationMethod {
        SOCKET,
        TWITCH_CHAT,
        EXTERNAL_PROCESS
    }

    public static void initialize() {
        BaseMod.subscribe(new CommunicationModExtension());
    }


    @SpirePatch(clz = CommunicationMod.class, method = "startExternalProcess", paramtypez = {})
    public static class NetworkCommunicationPatch {
        @SpirePrefixPatch
        public static SpireReturn startNetworkCommunications(CommunicationMod communicationMod) {
            switch (communicationMethod) {
                case SOCKET:
                    setSocketThreads();
                    return SpireReturn.Return(true);
                case TWITCH_CHAT:
                    setTwitchThreads();
                    return SpireReturn.Return(true);
                case EXTERNAL_PROCESS:
                default:
                    return SpireReturn.Continue();
            }
        }
    }

    private static void setSocketThreads() {
        Thread starterThread = new Thread(() -> {
            try {
                // start stuff then start read thread and write thread
                ServerSocket serverSocket = new ServerSocket(PORT);

                Socket socket = serverSocket.accept();

                Thread writeThread = new Thread(() -> {
                    try {
                        DataOutputStream out = new DataOutputStream(socket
                                .getOutputStream());

                        CommunicationMod.subscribe(() -> {
                            try {
                                out.writeUTF(GameStateConverter.getCommunicationState());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                writeThread.start();

                Thread readThread = new Thread(() -> {
                    try {
                        DataInputStream in = new DataInputStream(new BufferedInputStream(socket
                                .getInputStream()));

                        while (true) {
                            CommunicationMod.queueCommand(in.readUTF());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                readThread.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        starterThread.start();
    }

    public static void setTwitchThreads() {
        Optional<TwitchConfig> twitchConfigOptional = TwitchConfig.readConfig();
        if (twitchConfigOptional.isPresent()) {
            TwitchConfig twitchConfig = twitchConfigOptional.get();

            String channel = ReflectionHacks
                    .getPrivate(twitchConfig, TwitchConfig.class, "channel");
            String username = ReflectionHacks
                    .getPrivate(twitchConfig, TwitchConfig.class, "username");
            String token = ReflectionHacks.getPrivate(twitchConfig, TwitchConfig.class, "token");

            try {
                Twirk twirk = new TwirkBuilder(channel, username, token).setSSL(true).build();

                TwitchController controller = new TwitchController(twirk);
                BaseMod.subscribe(controller);

                twirk.addIrcListener(new TwirkListener() {
                    @Override
                    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
                        controller.receiveMessage(sender, message.getContent());
                    }
                });

                CommunicationMod.subscribe(() -> controller
                        .startVote(GameStateConverter.getCommunicationState()));

                twirk.connect();
                System.err.println("connected as " + username);

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ColonelSanders implements Controller {
        private boolean shouldSend = true;

        @Override
        public void step() {
            LinkedBlockingQueue<String> writeQueue =
                    ReflectionHacks
                            .getPrivateStatic(CommunicationMod.class, "writeQueue");

            if (writeQueue != null && shouldSend) {
                shouldSend = false;
                writeQueue.add(GameStateConverter.getCommunicationState());
            }


            BlockingQueue<String> readQueue = ReflectionHacks
                    .getPrivateStatic(CommunicationMod.class, "readQueue");
            if (readQueue != null && !readQueue.isEmpty()) {
                try {
                    CommandExecutor.executeCommand(readQueue.poll());
                    shouldSend = true;
                } catch (InvalidCommandException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean isDone() {
            return false;
        }
    }

    @Override
    public void receivePostInitialize() {
        if (BaseMod
                .hasModID("BattleAiMod:") && communicationMethod == CommunicationMethod.TWITCH_CHAT) {
            if (BattleAiMod.isClient) {
                setTwitchThreads();
                sendSuccessToController();
            }
        }
    }

    private static final String HOST_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5123;

    private static void sendSuccessToController() {
        new Thread(() -> {
            try {
                Thread.sleep(5_000);
                Socket socket;
                socket = new Socket();
                socket.connect(new InetSocketAddress(HOST_IP, SERVER_PORT));
                new DataOutputStream(socket.getOutputStream()).writeUTF("SUCCESS");
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket
                        .getInputStream()));

                while (true) {
                    String controllerLine = in.readUTF();
                    if (controllerLine.equals("kill")) {
                        int monsterCount = AbstractDungeon.getCurrRoom().monsters.monsters.size();
                        int[] multiDamage = new int[monsterCount];

                        for (int i = 0; i < monsterCount; ++i) {
                            multiDamage[i] = 999;
                        }

                        AbstractDungeon.actionManager
                                .addToTop(new DamageAllEnemiesAction(AbstractDungeon.player, multiDamage, DamageInfo.DamageType.HP_LOSS, AbstractGameAction.AttackEffect.NONE));
                    } else if (controllerLine.equals("start")) {
                        CommunicationMod.queueCommand("start ironclad");
                    } else if (controllerLine.equals("enable")) {
                        System.out.println("received enable signal");
                        TwitchController.enable();
                    } else if (controllerLine.equals("state")) {
                        CommunicationMod.mustSendGameState = true;
                    } else if (controllerLine.equals("battlerestart")) {
                        TwitchController.battleRestart();
                    } else if (controllerLine.equals("advancegame")) {
                        CommunicationMod.mustSendGameState = true;
                        TwitchController.inBattle = false;
                    } else if (controllerLine.equals("losebattle")) {
                        AbstractDungeon.actionManager
                                .addToTop(new LoseHPAction(AbstractDungeon.player, AbstractDungeon.player, 999));
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
