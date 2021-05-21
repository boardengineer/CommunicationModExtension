import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import communicationmod.CommunicationMod;
import de.robojumper.ststwitch.TwitchConfig;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

public class CommunicationModExtension {
    public static CommunicationMethod communicationMethod = CommunicationMethod.EXTERNAL_PROCESS;
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
                    setSocketCommunicationThreads();
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

    private static void setSocketCommunicationThreads() {
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
                final LinkedBlockingQueue<String> readQueue = new LinkedBlockingQueue<>();

                ReflectionHacks
                        .setPrivateStatic(CommunicationMod.class, "readQueue", readQueue);

                twirk.addIrcListener(new TwirkListener() {
                    @Override
                    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
                        if(sender.getDisplayName().equals(username)) {
                            System.err.println("should be receiving message");
                            readQueue.add(message.getContent());
                        }
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
                            twirk.channelMessage(writeQueue.poll());
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
}
