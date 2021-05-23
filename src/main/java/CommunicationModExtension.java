import basemod.BaseMod;
import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import communicationmod.CommandExecutor;
import communicationmod.CommunicationMod;
import communicationmod.GameStateConverter;
import communicationmod.InvalidCommandException;
import de.robojumper.ststwitch.TwitchConfig;
import ludicrousspeed.Controller;
import ludicrousspeed.LudicrousSpeedMod;
import savestate.SaveStateMod;
import twitch.TwitchController;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CommunicationModExtension {
    public static CommunicationMethod communicationMethod = CommunicationMethod.TWITCH_CHAT;
    private static final int PORT = 8080;

    enum CommunicationMethod {
        SOCKET,
        TWITCH_CHAT,
        EXTERNAL_PROCESS
    }

    @SpirePatch(clz = CommunicationMod.class, method = "startExternalProcess", paramtypez = {})
    public static class NetworkCommunicationPatch {
        @SpirePrefixPatch
        public static SpireReturn startNetworkCommunications(CommunicationMod communicationMod) {
            switch (communicationMethod) {
                case SOCKET:
                    // THIS IS EXPERIMENTAL CODE
                    BaseMod.subscribe(new LudicrousSpeedMod());
                    Settings.MASTER_VOLUME = 0;
                    CardCrawlGame.sound.update();
                    Settings.isDemo = true;
                    SaveStateMod.shouldGoFast = true;
                    LudicrousSpeedMod.plaidMode = true;

                    Settings.ACTION_DUR_XFAST = 0.001F;
                    Settings.ACTION_DUR_FASTER = 0.002F;
                    Settings.ACTION_DUR_FAST = 0.0025F;
                    Settings.ACTION_DUR_MED = 0.005F;
                    Settings.ACTION_DUR_LONG = .01F;
                    Settings.ACTION_DUR_XLONG = .015F;

                    LudicrousSpeedMod.controller = new ColonelSanders();

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
                        LinkedBlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();

                        ReflectionHacks
                                .setPrivateStatic(CommunicationMod.class, "writeQueue", writeQueue);

                        while (true) {
                            if (!writeQueue.isEmpty()) {
                                out.writeUTF(writeQueue.remove());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                writeThread.start();

                ReflectionHacks
                        .setPrivateStatic(CommunicationMod.class, "writeThread", writeThread);

                Thread readThread = new Thread(() -> {
                    try {
                        DataInputStream in = new DataInputStream(new BufferedInputStream(socket
                                .getInputStream()));
                        LinkedBlockingQueue<String> readQueue = new LinkedBlockingQueue<>();

                        ReflectionHacks
                                .setPrivateStatic(CommunicationMod.class, "readQueue", readQueue);

                        while (true) {
                            readQueue.add(in.readUTF());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                readThread.start();

                ReflectionHacks
                        .setPrivateStatic(CommunicationMod.class, "readThread", readThread);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        starterThread.start();
    }

    private static void setTwitchThreads() {
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
                LinkedBlockingQueue<String> readQueue = new LinkedBlockingQueue<>();
                ReflectionHacks
                        .setPrivateStatic(CommunicationMod.class, "readQueue", readQueue);
                TwitchController controller = new TwitchController(readQueue);
                BaseMod.subscribe(controller);

                twirk.addIrcListener(new TwirkListener() {
                    @Override
                    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
                        controller.receiveMessage(sender.getDisplayName(), message.getContent());
                    }
                });

                twirk.connect();
                System.err.println("connected as " + username);


                Thread writeThread = new Thread(() -> {
                    LinkedBlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
                    ReflectionHacks
                            .setPrivateStatic(CommunicationMod.class, "writeQueue", writeQueue);

                    while (true) {
                        if (!writeQueue.isEmpty()) {
                            System.err.println("should be sending message");
                            String stateMessage = writeQueue.poll();
                            controller.startVote(stateMessage);
                        }
                    }
                });
                writeThread.start();

                ReflectionHacks
                        .setPrivateStatic(CommunicationMod.class, "writeThread", writeThread);


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
}
