import basemod.BaseMod;
import basemod.ReflectionHacks;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import battleaimod.BattleAiMod;
import battleaimod.networking.AiClient;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import communicationmod.CommandExecutor;
import communicationmod.CommunicationMod;
import communicationmod.GameStateConverter;
import communicationmod.InvalidCommandException;
import ludicrousspeed.Controller;
import ludicrousspeed.LudicrousSpeedMod;
import savestate.patches.SavesPatches;

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
public class CommunicationModExtension implements PostUpdateSubscriber, PostInitializeSubscriber {
    public static CommunicationMethod communicationMethod = CommunicationMethod.SOCKET;
    private static final int PORT = 8080;

    static boolean inBattle = false;
    static boolean shouldStartClientOnUpdate = false;


    public static void initialize() {
        BaseMod.subscribe(new CommunicationModExtension());
    }

    @Override
    public void receivePostUpdate() {
        if (shouldStartClientOnUpdate) {
            shouldStartClientOnUpdate = false;
            inBattle = true;
            System.err.println("requesting start ai client");
            startAiClient();
        }

        // The Ai Client has stopped simulation.  Hand control back to the Twitch interface.
        if (BattleAiMod.rerunController != null || LudicrousSpeedMod.mustRestart) {
            if (BattleAiMod.rerunController.isDone || LudicrousSpeedMod.mustRestart) {
                LudicrousSpeedMod.controller = BattleAiMod.rerunController = null;
                inBattle = false;

                if (LudicrousSpeedMod.mustRestart) {
                    System.err.println("Desync detected, rerunning simluation");
                    LudicrousSpeedMod.mustRestart = false;
//                    startAiClient();
                }
            }
        }
    }

    private void startAiClient() {
        if (BattleAiMod.aiClient == null) {
            try {
                BattleAiMod.aiClient = new AiClient();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (BattleAiMod.aiClient != null) {
            BattleAiMod.aiClient.sendState();
        }
    }

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
                                if (state.has("available_commands")) {
                                    JsonArray commands =
                                            state.get("available_commands").getAsJsonArray();

                                    boolean addAutoplay = false;
                                    for (JsonElement command : commands) {
                                        if (command.getAsString().equals("play") && !inBattle) {
                                            System.err.println("play command available \n \n \n");
                                            addAutoplay = true;
                                            break;
                                        }
                                    }

                                    if (addAutoplay) {
                                        commands.add("autoplay");
                                    }
                                    commands.add("load");

                                    state.add("available_commands", commands);
                                }

                                if (!inBattle) {
                                    out.writeUTF(state.toString());
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, "output_thread");
                writeThread.start();

                Thread readThread = new Thread(() -> {
                    try {
                        DataInputStream in = new DataInputStream(new BufferedInputStream(socket
                                .getInputStream()));

                        while (true) {
                            String command = in.readUTF();

                            if (command.equals("autoplay")) {
                                shouldStartClientOnUpdate = true;
                            } else if (command.contains("load")) {
                                System.err.println("loading game...?");
                                String[] tokens = command.split("\\s+");
                                if (tokens.length >= 2) {
                                    SavesPatches.load(tokens[1], AbstractPlayer.PlayerClass.IRONCLAD);
                                }
                            } else {
                                CommunicationMod.queueCommand(command);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, "input_thread");
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

    @Override
    public void receivePostInitialize() {
        String connectOnStartupFlag = System.getProperty("connectOnStartup");
        if (connectOnStartupFlag != null) {
            if(Boolean.parseBoolean(connectOnStartupFlag)) {
                setSocketThreads();
            }
        }
    }
}
