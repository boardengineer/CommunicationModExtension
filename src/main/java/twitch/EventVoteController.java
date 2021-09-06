package twitch;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import communicationmod.ChoiceScreenUtils;
import mintySpire.patches.map.MiniMapDisplay;

import java.util.ArrayList;
import java.util.HashMap;

public class EventVoteController extends VoteController {
    private static final float SCALE = 3.3f;

    public static final OrthographicCamera CAMERA = new OrthographicCamera(MiniMapDisplay.FRAME_BUFFER
            .getWidth() * SCALE, MiniMapDisplay.FRAME_BUFFER
            .getHeight() * SCALE);

    // Event rendering references
    private final HashMap<String, LargeDialogOptionButton> voteStringToEventButtonMap;
    private final HashMap<String, String> voteStringToOriginalEventButtonMessageMap;

    private final TwitchController twitchController;
    private final JsonObject stateJson;

    public EventVoteController(TwitchController twitchController, JsonObject stateJson) {
        voteStringToEventButtonMap = new HashMap<>();
        ArrayList<LargeDialogOptionButton> eventButtons = ChoiceScreenUtils
                .getActiveEventButtons();
        voteStringToOriginalEventButtonMessageMap = new HashMap<>();

        for (int i = 0; i < eventButtons.size(); i++) {
            LargeDialogOptionButton button = eventButtons.get(i);

            String voteString = Integer.toString(i + 1);

            voteStringToOriginalEventButtonMessageMap.put(voteString, button.msg);
            voteStringToEventButtonMap.put(voteString, button);
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
        if (AbstractDungeon.getCurrRoom().event instanceof NeowEvent) {
            MiniMapDisplay.renderMinimap(spriteBatch, Settings.WIDTH / 8.f, 0, CAMERA);
        }

        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            TwitchController.Choice choice = twitchController.viableChoices.get(i);
            String message = choice.voteString;
            if (voteStringToEventButtonMap.containsKey(message)) {
                voteStringToEventButtonMap.get(message).msg = String
                        .format("%s [vote %s] (%s)",
                                voteStringToOriginalEventButtonMessageMap.get(message),
                                choice.voteString,
                                voteFrequencies.getOrDefault(choice.voteString, 0));
            }
        }
    }

    @Override
    public void endVote() {

    }
}
