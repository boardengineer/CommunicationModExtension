package twitch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gikk.twirk.Twirk;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class VoteController {
    protected final TwitchController twitchController;

    public abstract void setUpChoices();

    public abstract void render(SpriteBatch spriteBatch);

    protected VoteController(TwitchController twitchController) {
        this.twitchController = twitchController;
    }

    public Optional<String> getTipString() {
        return Optional.empty();
    }

    public void endVote(Choice result) {
        endVote();
    }

    /**
     * Doesn't get called if the override with a result gets re-implemented
     */
    public void endVote() {
    }

    public long getVoteTimerMillis() {
        return TwitchController.optionsMap.get("other");
    }

    public void sendVoteMessage() {
        sendDefaultVoteMessage();
    }

    public static void sendDefaultVoteMessage() {
        List<Choice> viableChoices = TwitchController.viableChoices;
        Twirk twirk = TwitchController.twirk;

        if (TwitchController.viableChoices.size() > 1) {
            int appendedSize = 0;
            ArrayList<Choice> toSend = new ArrayList<>();
            for (int i = 0; i < viableChoices.size(); i++) {
                toSend.add(viableChoices.get(i));
                appendedSize++;

                if (appendedSize % 20 == 0) {
                    // TODO kill print
                    String messageString = toSend.stream().peek(choice -> System.err
                            .println(choice.rewardInfo.isPresent() ? choice.rewardInfo
                                    .get().relicName : " ")).map(choice -> String
                            .format("[%s| %s]", choice.voteString, choice.choiceName))
                                                 .collect(Collectors.joining(" "));

                    twirk.channelMessage("[BOT] Vote: " + messageString);

                    toSend = new ArrayList<>();
                    appendedSize = 0;
                }
            }

            if (!toSend.isEmpty()) {
                String messageString = toSend.stream().peek(choice -> System.err
                        .println(choice.rewardInfo.isPresent() ? choice.rewardInfo
                                .get().relicName : " ")).map(choice -> String
                        .format("[%s| %s]", choice.voteString, choice.choiceName))
                                             .collect(Collectors.joining(" "));

                twirk.channelMessage("[BOT] Vote: " + messageString);
            }
        }
    }
}
