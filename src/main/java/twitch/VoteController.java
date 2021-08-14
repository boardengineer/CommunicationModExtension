package twitch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class VoteController {
    abstract void setUpChoices();

    abstract void render(SpriteBatch spriteBatch);

    abstract void endVote();
}
