package twitch;

import com.megacrit.cardcrawl.helpers.SeedHelper;
import communicationmod.CommunicationMod;
import twitch.votecontrollers.CharacterVoteController;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import static twitch.TwitchController.gameController;
import static twitch.TwitchController.voteController;

public class CommandChoice implements Command {
    public String choiceName;
    public String voteString;
    public Optional<RewardInfo> rewardInfo = Optional.empty();
    public final ArrayList<String> resultCommands;

    public CommandChoice(String choiceName, String voteString, String... resultCommands) {
        this.choiceName = choiceName;
        this.voteString = voteString;

        this.resultCommands = new ArrayList<>();
        for (String resultCommand : resultCommands) {
            this.resultCommands.add(resultCommand);
        }
    }

    @Override
    public String toString() {
        return "Choice{" +
                "choiceName='" + choiceName + '\'' +
                ", voteString='" + voteString + '\'' +
                ", resultCommands=" + resultCommands +
                '}';
    }

    @Override
    public void execute() {
        for (String command : resultCommands) {
            boolean isCharacterVote = voteController != null &&
                    voteController instanceof CharacterVoteController;

            if (isCharacterVote && resultCommands.size() == 1) {
                int ascension = gameController.getAscension();

                if (ascension > 0) {
                    String seedString = SeedHelper.getString(new Random().nextLong());

                    command += String
                            .format(" %d %s", gameController.getAscension(), seedString);
                }
            }
            CommunicationMod.queueCommand(command);
        }
    }

    @Override
    public String getVoteString() {
        return voteString;
    }
}
