package twitch;

import com.google.gson.JsonObject;

public interface GameController {
    int getAscension();

    void reportGameOver(JsonObject gameState);
}
