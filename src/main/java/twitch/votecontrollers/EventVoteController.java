package twitch.votecontrollers;

import ThMod.event.Mushrooms_MRS;
import basemod.BaseMod;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.beyond.MysteriousSphere;
import com.megacrit.cardcrawl.events.city.MaskedBandits;
import com.megacrit.cardcrawl.events.exordium.DeadAdventurer;
import com.megacrit.cardcrawl.events.exordium.Mushrooms;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import communicationmod.ChoiceScreenUtils;
import mintySpire.patches.map.MiniMapDisplay;
import twitch.CommandChoice;
import twitch.TwitchController;
import twitch.VoteController;

import java.util.ArrayList;
import java.util.HashMap;

public class EventVoteController extends VoteController {
    private static final float SCALE = 3.3f;
    public static OrthographicCamera camera = null;

    // Event rendering references
    private final HashMap<String, LargeDialogOptionButton> voteStringToEventButtonMap;
    private final HashMap<String, String> voteStringToOriginalEventButtonMessageMap;

    private final JsonObject stateJson;

    public EventVoteController(TwitchController twitchController, JsonObject stateJson) {
        super(twitchController);

        if (BaseMod.hasModID("mintyspire:")) {
            if (camera == null) {
                camera = new OrthographicCamera(MiniMapDisplay.FRAME_BUFFER
                        .getWidth() * SCALE, MiniMapDisplay.FRAME_BUFFER
                        .getHeight() * SCALE);
            }
        }

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

        this.stateJson = stateJson;
    }

    @Override
    public void setUpChoices() {
        twitchController.setUpDefaultVoteOptions(stateJson);
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        if (AbstractDungeon.getCurrRoom().event instanceof NeowEvent) {
            if (BaseMod.hasModID("mintyspire:")) {
                MiniMapDisplay.renderMinimap(spriteBatch, Settings.WIDTH / 8.f, 0, camera);
            }
        } else {
            AbstractEvent event = AbstractDungeon.getCurrRoom().event;

            boolean disableMapForCustomEvent = false;
            if (BaseMod.hasModID("TS05_Marisa:")) {
                if (event instanceof Mushrooms_MRS) {
                    disableMapForCustomEvent = true;
                }
            }

            if (!(event instanceof Mushrooms) &&
                    !(event instanceof MaskedBandits) &&
                    !(event instanceof MysteriousSphere) &&
                    !(event instanceof DeadAdventurer) &&
                    !disableMapForCustomEvent) {

                // Don't show the map for the events with the event text on the left and battle on the
                // right.
                if (BaseMod.hasModID("mintyspire:")) {
                    MiniMapDisplay
                            .renderMinimap(spriteBatch, Settings.WIDTH / 8 * 3 * -1, 0, camera);
                }
            }
        }

        HashMap<String, Integer> voteFrequencies = TwitchController.getVoteFrequencies();
        for (int i = 0; i < TwitchController.viableChoices.size(); i++) {
            if (TwitchController.viableChoices.get(i) instanceof CommandChoice) {
                CommandChoice choice = (CommandChoice) TwitchController.viableChoices.get(i);
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
    }

    @Override
    public void endVote() {
        // Reset The event button text in case those buttons are going to be used for the next
        // phase.
        for (int i = 0; i < TwitchController.viableChoices.size(); i++) {
            if (TwitchController.viableChoices.get(i) instanceof CommandChoice) {
                CommandChoice choice = (CommandChoice) TwitchController.viableChoices.get(i);
                String message = choice.voteString;
                if (voteStringToEventButtonMap.containsKey(message)) {
                    voteStringToEventButtonMap
                            .get(message).msg = voteStringToOriginalEventButtonMessageMap
                            .get(message);
                }
            }
        }
    }
}
