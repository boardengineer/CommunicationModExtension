package twitch.votecontrollers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gikk.twirk.Twirk;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.relics.WingBoots;
import com.megacrit.cardcrawl.rooms.*;
import communicationmod.ChoiceScreenUtils;
import twitch.*;

import java.util.*;
import java.util.stream.Collectors;

public class MapVoteController extends VoteController {
    private static final String MAP_LONG_KEY = "map_long";
    private static final String MAP_SHORT_KEY = "map_short";
    private static final String MAP_SKIP_KEY = "map_skip";

    private static final int DEFAULT_LONG_VOTE_TIME_MILLIS = 30_000;
    private static final int DEFAULT_SHORT_VOTE_TIME_MILLIS = 15_000;
    private static final int DEFAULT_SKIP_VOTE_TIME_MILLS = 1_000;

    private static final HashSet<Integer> FIRST_FLOOR_NUMS = new HashSet<Integer>() {{
        add(0);
        add(17);
        add(34);
    }};

    private static final HashSet<Integer> NO_OPT_REST_SITE_FLOORS = new HashSet<Integer>() {{
        add(14);
        add(31);
        add(48);
    }};

    private static final HashMap<Class, String> ROOM_DISPLAY_STRINGS = new HashMap<Class, String>() {{
        put(MonsterRoom.class, "Monster Room");
        put(MonsterRoomElite.class, "Elite Monster Room");
        put(EventRoom.class, "Event Room");
        put(RestRoom.class, "Campfire Room");
        put(TreasureRoom.class, "Treasure Room");
    }};

    private final TwitchController twitchController;
    private final HashMap<String, MapRoomNode> messageToRoomNodeMap;
    private final JsonObject stateJson;

    public MapVoteController(TwitchController twitchController, JsonObject stateJson) {
        super(twitchController);
        messageToRoomNodeMap = new HashMap<>();
        ArrayList<MapRoomNode> mapChoice = ChoiceScreenUtils.getMapScreenNodeChoices();
        for (MapRoomNode node : mapChoice) {
            messageToRoomNodeMap
                    .put(String.format("x=%d", node.x).toLowerCase(), node);
        }

        this.twitchController = twitchController;
        this.stateJson = stateJson;

        HashMap<String, Integer> optionsMap = TwitchController.optionsMap;

        optionsMap.putIfAbsent(MAP_LONG_KEY, DEFAULT_LONG_VOTE_TIME_MILLIS);
        optionsMap.putIfAbsent(MAP_SHORT_KEY, DEFAULT_SHORT_VOTE_TIME_MILLIS);
        optionsMap.putIfAbsent(MAP_SKIP_KEY, DEFAULT_SKIP_VOTE_TIME_MILLS);
    }

    @Override
    public void setUpChoices() {
        twitchController.setUpDefaultVoteOptions(stateJson);
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        Set<String> winningResults = twitchController.getBestVoteResultKeys();

        for (int i = 0; i < TwitchController.viableChoices.size(); i++) {
            if (TwitchController.viableChoices.get(i) instanceof CommandChoice) {
                CommandChoice choice = (CommandChoice) TwitchController.viableChoices.get(i);

                Color messageColor = winningResults
                        .contains(choice.voteString) ? new Color(1.f, 1.f, 0, 1.f) : new Color(1.f, 0, 0, 1.f);


                String message = choice.choiceName;
                if (messageToRoomNodeMap.containsKey(message)) {
                    MapRoomNode mapRoomNode = messageToRoomNodeMap.get(message);
                    Hitbox roomHitbox = mapRoomNode.hb;

                    String mapMessage = String.format("[vote %s] (%s)",
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

                    // Alternate having the vote above and below so that the messages don't
                    // run into each other
                    if (i % 2 == 0) {
                        RenderHelpers
                                .renderTextBelowHitbox(spriteBatch, mapMessage, roomHitbox, messageColor);
                    } else {
                        RenderHelpers
                                .renderTextAboveHitbox(spriteBatch, mapMessage, roomHitbox, messageColor);
                    }
                } else {
                    System.err.println("no room button for " + choice.choiceName);
                }
            }
        }
    }

    @Override
    public void endVote() {

    }

    @Override
    public void sendVoteMessage() {
        List<Command> viableChoices = TwitchController.viableChoices;
        Twirk twirk = TwitchController.twirk;

        if (viableChoices.size() > 1) {
            String messageString = viableChoices.stream()
                                                .map(choice -> toMessageString(choice))
                                                .collect(Collectors.joining(" "));
            twirk.channelMessage("[BOT] Vote: " + messageString);
        }
    }

    private String toMessageString(Command command) {
        if (command instanceof ExtendTimerCommand) {
            return String
                    .format("[ Extend Timer | %s]", command.getVoteString());
        }
        if (!(command instanceof CommandChoice)) {
            return "";
        }
        CommandChoice choice = (CommandChoice) command;
        Class roomClass = messageToRoomNodeMap.get(choice.choiceName).getRoom().getClass();

        String roomDisplayName = ROOM_DISPLAY_STRINGS.containsKey(roomClass) ? ROOM_DISPLAY_STRINGS
                .get(roomClass) : roomClass.getSimpleName().toLowerCase();

        return String
                .format("[ %s | %s | %s ]", choice.voteString, choice.choiceName, roomDisplayName);
    }

    @Override
    public long getVoteTimerMillis() {
        if (FIRST_FLOOR_NUMS.contains(AbstractDungeon.floorNum)) {
            return TwitchController.optionsMap.get(MAP_LONG_KEY);
        } else if (NO_OPT_REST_SITE_FLOORS
                .contains(AbstractDungeon.floorNum) && !AbstractDungeon.player.relics
                .stream()
                .anyMatch(relic -> relic instanceof WingBoots && relic.counter > 0)) {
            return TwitchController.optionsMap.get(MAP_SKIP_KEY);
        } else {
            return TwitchController.optionsMap.get(MAP_SHORT_KEY);
        }
    }
}
