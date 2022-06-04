package twitch;

import com.megacrit.cardcrawl.core.CardCrawlGame;

public class GameOverWithCreditsCommand implements Command{
    @Override
    public void execute() {
        CardCrawlGame.startOverButShowCredits();
    }

    @Override
    public String getVoteString() {
        return "gameoverwithcredits";
    }
}
