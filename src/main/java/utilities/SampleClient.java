package utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SampleClient {
    private static final String HOST_IP = "127.0.0.1";
    private static final int PORT = 8080;
    private static boolean commandReady = true;
    private static boolean doAutoPlay = false;
    private static boolean showState = false;

    private static final long PANIC_TIME_MS = 3_000L;
    private static long timeUntilPanic = PANIC_TIME_MS;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.err.println("we are here");

        List<String> commands = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("data/runs/3ESSK05Q3UNKS.run"))) {
            String line = br.readLine();

            while (line != null) {
                commands.add(line);
                line = br.readLine();
            }
        }

        Iterator<String> commandIterator = commands.iterator();

        Socket socket = new Socket();
        try {
            System.err.println("connecting...");

            socket.connect(new InetSocketAddress(HOST_IP, PORT));

            System.err.println("connected");

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    DataInputStream inStream = new DataInputStream(new BufferedInputStream(socket
                            .getInputStream()));

                    while (true) {
                        String stateJson = inStream.readUTF();
                        JsonArray commandsArray = new JsonParser().parse(stateJson)
                                                                  .getAsJsonObject()
                                                                  .get("available_commands")
                                                                  .getAsJsonArray();

                        if (showState) {
                            System.err.println(stateJson);
                        }

                        for (int i = 0; i < commandsArray.size(); i++) {
                            if (commandsArray.get(i).getAsString().equals("autoplay")) {
                                System.err.println("has autoplay");
//                                doAutoPlay = true;
                            }
                        }

                        commandReady = true;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            List<BenchMarkElement> results = new ArrayList<>();
            long latestCommandTime = System.currentTimeMillis();
            while (commandIterator.hasNext()) {
                if (commandReady) {
                    timeUntilPanic = PANIC_TIME_MS;
                    String command = commandIterator.next();
                    if (doAutoPlay) {
                        command = "autoplay";
                        doAutoPlay = false;
                    }

                    System.err.println("executing command " + command);


                    BenchMarkElement toAdd = new BenchMarkElement();
                    toAdd.command = command;
                    long newTime = System.currentTimeMillis();
                    toAdd.elapsed = newTime - latestCommandTime;
                    latestCommandTime = newTime;
                    results.add(toAdd);
                    out.writeUTF(command);
                    commandReady = false;
                } /*else if (timeUntilPanic <= 0) {
                    timeUntilPanic = PANIC_TIME_MS;
                    out.writeUTF("state");
                    showState = true;
                }*/
                Thread.sleep(1);
                timeUntilPanic--;
            }

            System.out.println("completed recall");

            for (BenchMarkElement element : results) {
                System.out.printf("%s,%s,%d\n", element.command, element.command.split(" ")[0], element.elapsed);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class BenchMarkElement {
        String command;
        long elapsed;
    }
}
