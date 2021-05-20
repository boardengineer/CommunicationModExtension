import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import communicationmod.CommunicationMod;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class CommunicationModExtension {
    public static boolean startSocketCommunication = true;
    private static final int PORT = 8080;

    @SpirePatch(clz = CommunicationMod.class, method = "startExternalProcess", paramtypez = {})
    public static class NetworkCommunicationPatch {
        @SpirePrefixPatch
        public static SpireReturn startNetworkCommunications(CommunicationMod communicationMod) {
            if (startSocketCommunication) {
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
                return SpireReturn.Return(true);
            }
            return SpireReturn.Continue();
        }
    }
}
