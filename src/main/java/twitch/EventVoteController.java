package twitch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import communicationmod.ChoiceScreenUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class EventVoteController implements VoteController {
    // Event rendering references
    private final HashMap<String, LargeDialogOptionButton> voteStringToEventButtonMap;
    private final HashMap<String, String> voteStringToOriginalEventButtonMessageMap;

    private final TwitchController twitchController;

    public EventVoteController(TwitchController twitchController) {
        voteStringToEventButtonMap = new HashMap<>();
        ArrayList<LargeDialogOptionButton> eventButtons = ChoiceScreenUtils
                .getEventButtons();
        voteStringToOriginalEventButtonMessageMap = new HashMap<>();

        for (int i = 0; i < eventButtons.size(); i++) {
            LargeDialogOptionButton button = eventButtons.get(i);

            String voteString = Integer.toString(i + 1);

            voteStringToOriginalEventButtonMessageMap.put(voteString, button.msg);
            voteStringToEventButtonMap.put(voteString, button);
        }

        this.twitchController = twitchController;
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
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
            } else {
                System.err.println("no event button for " + choice.choiceName);
            }
        }
    }
}
