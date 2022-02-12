package twitch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.Optional;

public abstract class VoteController {
    abstract void setUpChoices();

    abstract void render(SpriteBatch spriteBatch);

    Optional<String> getTipString() {
        return Optional.empty();
    }

    void endVote(TwitchController.Choice result) {
        endVote();
    }

    /**
     * Doesn't get called if the override with a result gets re-implemented
     */
    void endVote() {
    }
}
