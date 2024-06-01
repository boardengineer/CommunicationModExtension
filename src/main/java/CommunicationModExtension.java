import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import communicationmod.CommandExecutor;
import communicationmod.CommunicationMod;
import communicationmod.GameStateConverter;
import communicationmod.InvalidCommandException;
import ludicrousspeed.Controller;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@SpireInitializer
public class CommunicationModExtension {
    public static CommunicationMethod communicationMethod = CommunicationMethod.SOCKET;
    private static final int PORT = 8080;

    enum CommunicationMethod {
        SOCKET,
        EXTERNAL_PROCESS
    }

    public static final HashMap<String, String> relicNameToIdmap = new HashMap<>();

    @SpirePatch(clz = CommunicationMod.class, method = "startExternalProcess", paramtypez = {})
    public static class NetworkCommunicationPatch {
        @SpirePrefixPatch
        public static SpireReturn startNetworkCommunications(CommunicationMod communicationMod) {
            switch (communicationMethod) {
                case SOCKET:
                    setSocketThreads();
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
                                String stateString = GameStateConverter.getCommunicationState();
                                JsonObject state =
                                        new JsonParser().parse(stateString).getAsJsonObject();
                                if(state.has("available_commands")) {
                                    System.err.println("State has available commands \n \n \n");
                                    JsonArray commands =
                                            state.get("available_commands").getAsJsonArray();

                                    for(JsonElement command : commands) {
                                        if(command.getAsString().equals("play")) {
                                            System.err.println("play command available \n \n \n");
                                        }
                                    }

                                    System.err.println(commands);
                                }

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
