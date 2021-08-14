package twitch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.map.MapRoomNode;
import communicationmod.ChoiceScreenUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class MapVoteController extends VoteController {
    private final TwitchController twitchController;
    private final HashMap<String, MapRoomNode> messageToRoomNodeMap;
    private final JsonObject stateJson;

    public MapVoteController(TwitchController twitchController, JsonObject stateJson) {
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
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);

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
                    RenderHelpers.renderTextBelowHitbox(spriteBatch, mapMessage, roomHitbox);
                } else {
                    RenderHelpers.renderTextAboveHitbox(spriteBatch, mapMessage, roomHitbox);
                }
            } else {
                System.err.println("no room button for " + choice.choiceName);
            }
        }
    }

    @Override
    public void endVote() {

    }
}
