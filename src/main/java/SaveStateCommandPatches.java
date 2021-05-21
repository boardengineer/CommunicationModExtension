import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import communicationmod.CommandExecutor;
import communicationmod.InvalidCommandException;
import savestate.SaveState;

import java.util.HashMap;

import static communicationmod.CommandExecutor.getAvailableCommands;

public class SaveStateCommandPatches {
    public static HashMap<String, SaveState> savedStates = new HashMap<>();

    @SpirePatch(
            clz = CommandExecutor.class,
            method = "executeCommand"
    )
    public static class AlsoExecuteSaveAndLoadState {
        @SpirePrefixPatch
        public static SpireReturn doMoreActions(String command) {
            command = command.toLowerCase();
            String[] tokens = command.split("\\s+");
            if (tokens.length == 0) {
                return SpireReturn.Continue();
            }

            if (getAvailableCommands().contains("play")) {
                try {
                    switch (tokens[0]) {
                        case "savestate":
                            executeSaveStateCommand(tokens);
                            return SpireReturn.Return(true);
                        case "loadstate":
                            executeLoadStateCommand(tokens);
                            return SpireReturn.Return(true);
                        default:
                            return SpireReturn.Continue();
                    }
                } catch (InvalidCommandException e) {
                    // we'll let the normal behavior handle it
                }
            }
            return SpireReturn.Continue();
        }
    }

    public static void executeSaveStateCommand(String[] tokens) throws InvalidCommandException {
        if (tokens.length != 2) {
            throw new InvalidCommandException("Please specify a statename");
        }

        savedStates.put(tokens[1], new SaveState());
        System.err.println("state saved");
    }

    public static void executeLoadStateCommand(String[] tokens) throws InvalidCommandException {
        if (tokens.length != 2) {
            throw new InvalidCommandException("Please specify a statename");
        }

        if (savedStates.containsKey(tokens[1])) {
            savedStates.get(tokens[1]).loadState();
            System.err.println("state loaded");
        }
    }
}
