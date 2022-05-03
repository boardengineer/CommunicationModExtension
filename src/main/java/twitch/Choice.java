package twitch;

import java.util.ArrayList;
import java.util.Optional;

public class Choice {
    public String choiceName;
    public String voteString;
    public Optional<RewardInfo> rewardInfo = Optional.empty();
    public final ArrayList<String> resultCommands;

    public Choice(String choiceName, String voteString, String... resultCommands) {
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
}
