package twitch;

public class ExtendTimerCommand implements Command {
    String voteString;

    public ExtendTimerCommand(String voteString) {
        this.voteString = voteString;
    }

    @Override
    public void execute() {
        TwitchController.voteEndTimeMillis += 10_000;
    }

    @Override
    public String getVoteString() {
        return voteString;
    }
}
