package twitch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gikk.twirk.Twirk;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class VoteController {
    public final TwitchController twitchController;

    public abstract void setUpChoices();

    public abstract void render(SpriteBatch spriteBatch);

    protected VoteController(TwitchController twitchController) {
        this.twitchController = twitchController;
    }

    public Optional<String> getTipString() {
        return Optional.empty();
    }

    public void endVote(Command result) {
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
        List<Command> viableChoices = TwitchController.viableChoices;
        Twirk twirk = TwitchController.twirk;

        if (TwitchController.viableChoices.size() > 1) {
            int appendedSize = 0;
            ArrayList<Command> toSend = new ArrayList<>();
            for (int i = 0; i < viableChoices.size(); i++) {
                toSend.add(viableChoices.get(i));
                appendedSize++;

                if (appendedSize % 20 == 0) {
                    String messageString = toSend.stream()
                                                 .map(command -> messageForCommand(command))
                                                 .collect(Collectors.joining(" "));

                    twirk.channelMessage("[BOT] Vote: " + messageString);

                    toSend = new ArrayList<>();
                    appendedSize = 0;
                }
            }

            if (!toSend.isEmpty()) {
                String messageString = toSend.stream()
                                             .map(command -> messageForCommand(command))
                                             .collect(Collectors.joining(" "));

                twirk.channelMessage("[BOT] Vote: " + messageString);
            }
        }
    }

    static String messageForCommand(Command command) {
        if (command instanceof CommandChoice) {
            CommandChoice choice = (CommandChoice) command;
            return String
                    .format("[%s| %s]", choice.voteString, choice.choiceName);
        } else if (command instanceof ExtendTimerCommand) {
            return String
                    .format("[%s| Extend Vote Timer]", command.getVoteString());
        }
        return "";
    }
}
