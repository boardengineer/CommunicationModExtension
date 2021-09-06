package twitch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.Optional;

public abstract class VoteController {
    abstract void setUpChoices();

    abstract void render(SpriteBatch spriteBatch);

    Optional<String> getTipString() {
        return Optional.empty();
    }

    abstract void endVote();
}
