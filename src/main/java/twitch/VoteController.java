package twitch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.Optional;

public abstract class VoteController {
    public abstract void setUpChoices();

    public abstract void render(SpriteBatch spriteBatch);

    public Optional<String> getTipString() {
        return Optional.empty();
    }

    public void endVote(TwitchController.Choice result) {
        endVote();
    }

    /**
     * Doesn't get called if the override with a result gets re-implemented
     */
    public void endVote() {
    }
}
