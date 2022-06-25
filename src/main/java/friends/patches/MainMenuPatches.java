package friends.patches;

import basemod.ReflectionHacks;
import chronoMods.TogetherManager;
import chronoMods.network.NetworkHelper;
import chronoMods.network.steam.SteamCallbacks;
import chronoMods.ui.lobby.MainLobbyScreen;
import chronoMods.ui.lobby.NewGameScreen;
import chronoMods.ui.mainMenu.NewMenuButtons;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuButton;
import communicationmod.CommandExecutor;
import communicationmod.CommunicationMod;
import savestate.SaveStateMod;

import java.util.ArrayList;

public class MainMenuPatches {
    public static NetworkHelper.dataType toSend = null;
    public static boolean inLobby = false;

    @SpirePatch(clz = TogetherManager.class, method = "receivePostDungeonInitialize", optional = true, requiredModId = "chronoMods")
    public static class SpyOnPostDunInit {
        @SpirePrefixPatch
        public static SpireReturn spyBefore() {
            if (CardCrawlGame.mainMenuScreen == null) {
                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }

    }

    @SpirePatch(clz = CommandExecutor.class, method = "getAvailableCommands", optional = true, requiredModId = "chronoMods")
    public static class AddSpireWithFriendsMainMenuPatch {
        public static boolean hijack = true;

        @SpirePrefixPatch
        public static SpireReturn<ArrayList<String>> addCommands() {
            if (SaveStateMod.shouldGoFast) {
                // Menu BS is throwing NPEs, if we want to add these options in fast mode we'll have
                // to properly fix this.
                return SpireReturn.Continue();
            }

            if (hijack) {
                // Run the original getAvailableCommands method and add to it
                hijack = false;
                ArrayList<String> result = CommandExecutor.getAvailableCommands();
                hijack = true;

                if (isCoopAvailable()) {
                    result.add("coop");
                }

                if (isBingoAvailable()) {
                    result.add("bingo");
                }

                if (isVersusAvailable()) {
                    result.add("versus");
                }

                if (inMainLobby()) {
                    result.add("create");
                }

                if (inHostLobby()) {
                    result.add("launch");
                }

                // Spire with friends disabled vanilla games
                result.remove("start");

                return SpireReturn.Return(result);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = SteamCallbacks.class, method = "onLobbyCreated", optional = true, requiredModId = "chronoMods")
    public static class SpyOnLobbyCreatePatch {
        @SpirePostfixPatch
        public static void sendMessage() {
            inLobby = true;
        }
    }

    @SpirePatch(clz = NewGameScreen.class, method = "update", optional = true, requiredModId = "chronoMods")
    public static class SendMessageAtUpdate {
        public static long messageSendTime = 0L;

        @SpirePostfixPatch
        public static void sendMessage(NewGameScreen newGameScreen) {
            if (inLobby && toSend != null && System.currentTimeMillis() > messageSendTime) {
                newGameScreen.playerList.toggleReadyState();
                NetworkHelper.dataType tempSend = toSend;
                NetworkHelper.sendData(tempSend);

                newGameScreen.neowToggle.ticked = true;
                NetworkHelper.sendData(NetworkHelper.dataType.Rules);
                NetworkHelper.sendData(NetworkHelper.dataType.Start);
                toSend = null;
                inLobby = false;
            }
        }
    }

    @SpirePatch(clz = MainLobbyScreen.class, method = "update", optional = true, requiredModId = "chronoMods")
    public static class OpenNewGameFromLobbyPatch {
        public static boolean createNewGameLobbyonOpen = false;

        @SpirePostfixPatch
        public static void sendMessage() {
            if (createNewGameLobbyonOpen) {
                NewMenuButtons.openNewGame();
                createNewGameLobbyonOpen = false;
            }

        }
    }

    @SpirePatch(
            clz = CommandExecutor.class, method = "executeCommand", optional = true, requiredModId = "chronoMods")
    public static class SpireWithFriendsInMainMenuPatch {
        @SpirePrefixPatch
        public static SpireReturn doMoreActions(String command) {
            command = command.toLowerCase();
            String[] tokens = command.split("\\s+");
            if (tokens.length == 0) {
                return SpireReturn.Continue();
            }

            switch (tokens[0]) {
                case "coop":
                    TogetherManager.gameMode = TogetherManager.mode.Coop;
                    OpenNewGameFromLobbyPatch.createNewGameLobbyonOpen = true;
                    NewMenuButtons.openLobby();

                    SendMessageAtUpdate.messageSendTime = System.currentTimeMillis() + 15_000;
                    toSend = NetworkHelper.dataType.Ready;

                    CommunicationMod.mustSendGameState = true;
                    return SpireReturn.Return(true);
                case "bingo":
                    TogetherManager.gameMode = TogetherManager.mode.Bingo;
                    NewMenuButtons.openLobby();
                    CommunicationMod.mustSendGameState = true;
                    return SpireReturn.Return(true);
                case "versus":
                    TogetherManager.gameMode = TogetherManager.mode.Versus;
                    NewMenuButtons.openLobby();
                    CommunicationMod.mustSendGameState = true;
                    return SpireReturn.Return(true);
                case "create":
                    NewMenuButtons.openNewGame();
                    CommunicationMod.mustSendGameState = true;
                    return SpireReturn.Return(true);
                case "launch":
                    if (TogetherManager.gameMode == TogetherManager.mode.Bingo) {
                        NetworkHelper.sendData(NetworkHelper.dataType.BingoRules);
                    }

                    NetworkHelper.sendData(NetworkHelper.dataType.Rules);
                    NetworkHelper.sendData(NetworkHelper.dataType.Start);
                    CommunicationMod.mustSendGameState = true;
                    return SpireReturn.Return(true);
            }

            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = CommandExecutor.class, method = "isStartCommandAvailable", optional = true, requiredModId = "chronoMods")
    public static class NoStartFromLobbyPatch {
        @SpirePrefixPatch
        public static SpireReturn<Boolean> noStart() {
            return SpireReturn.Return(isInMainMenu());
        }
    }

    private static boolean isCoopAvailable() {
        return isInMainMenu() && isButtonAvailable(getNonPublicEnumNamed("COOP"));
    }

    private static boolean isBingoAvailable() {
        return isInMainMenu() && isButtonAvailable(getNonPublicEnumNamed("BINGO"));
    }

    private static boolean isVersusAvailable() {
        return isInMainMenu() && isButtonAvailable(getNonPublicEnumNamed("VERSUS"));
    }

    private static boolean inMainLobby() {
        return CardCrawlGame.mainMenuScreen.screen == MainLobbyScreen.Enum.MAIN_LOBBY;
    }

    private static boolean inHostLobby() {
        return CardCrawlGame.mainMenuScreen.screen == NewGameScreen.Enum.CREATEMULTIPLAYERGAME;
    }

    private static boolean isInMainMenu() {
        return !CommandExecutor
                .isInDungeon() && CardCrawlGame.mainMenuScreen != null && CardCrawlGame.mainMenuScreen.screen == MainMenuScreen.CurScreen.MAIN_MENU;
    }

    private static boolean isButtonAvailable(MenuButton.ClickResult clickResult) {
        return CardCrawlGame.mainMenuScreen.buttons.stream()
                                                   .anyMatch(button -> button.result == clickResult);
    }

    private static MenuButton.ClickResult getNonPublicEnumNamed(String name) {
        return ReflectionHacks.getPrivateStatic(NewMenuButtons.class, name);
    }
}
