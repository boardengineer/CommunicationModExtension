package twitch.votecontrollers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gikk.twirk.Twirk;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.rooms.RestRoom;
import communicationmod.ChoiceScreenUtils;
import twitch.RenderHelpers;
import twitch.TwitchController;
import twitch.VoteController;

import java.util.*;
import java.util.stream.Collectors;

public class MapVoteController extends VoteController {
    public static final HashMap<Class, String> ROOM_DISPLAY_STRINGS = new HashMap<Class, String>() {{
        put(MonsterRoom.class, "Monster Room");
        put(MonsterRoomElite.class, "Elite Monster Room");
        put(EventRoom.class, "Event Room");
        put(RestRoom.class, "Campfire Room");
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
    }

    @Override
    public void setUpChoices() {
        twitchController.setUpDefaultVoteOptions(stateJson);
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        Set<String> winningResults = twitchController.getBestVoteResultKeys();

        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

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

    @Override
    public void endVote() {

    }

    @Override
    public void sendVoteMessage() {
        List<TwitchController.Choice> viableChoices = twitchController.viableChoices;
        Twirk twirk = TwitchController.twirk;

        if (viableChoices.size() > 1) {
            String messageString = viableChoices.stream().map(this::toMessageString)
                                                .collect(Collectors.joining(" "));
            twirk.channelMessage("[BOT] Vote: " + messageString);
        }
    }

    private String toMessageString(TwitchController.Choice choice) {
        Class roomClass = messageToRoomNodeMap.get(choice.choiceName).getRoom().getClass();

        String roomDisplayName = ROOM_DISPLAY_STRINGS.containsKey(roomClass) ? ROOM_DISPLAY_STRINGS
                .get(roomClass) : roomClass.getSimpleName().toLowerCase();

        return String
                .format("[ %s | %s | %s ]", choice.voteString, choice.choiceName, roomDisplayName);
    }
}
