package twitch.votecontrollers;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;
import twitch.CommandChoice;
import twitch.RenderHelpers;
import twitch.TwitchController;
import twitch.VoteController;

import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

public class BossRewardVoteController extends VoteController {
    private final HashMap<String, AbstractRelic> messageToBossRelicMap;
    private final JsonObject stateJson;

    public BossRewardVoteController(TwitchController twitchController, JsonObject stateJson) {
        super(twitchController);

        messageToBossRelicMap = new HashMap<>();

        for (AbstractRelic relic : AbstractDungeon.bossRelicScreen.relics) {
            messageToBossRelicMap.put(relic.name.toLowerCase(), relic);
        }

        this.stateJson = stateJson;
    }

    @Override
    public void setUpChoices() {
        twitchController.setUpDefaultVoteOptions(stateJson);

        // TODO: fix this an re-enable
//        twitchController.viableChoices
//                .add(new TwitchController.Choice("Skip", "0", "skip", "proceed"));
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = TwitchController.getVoteFrequencies();
        Set<String> winningResults = TwitchController.getBestVoteResultKeys();

        for (int i = 0; i < TwitchController.viableChoices.size(); i++) {
            if (TwitchController.viableChoices.get(i) instanceof CommandChoice) {
                CommandChoice choice = (CommandChoice) TwitchController.viableChoices.get(i);

                Color messageColor = winningResults
                        .contains(choice.voteString) ? new Color(1.f, 1.f, 0, 1.f) : new Color(1.f, 0, 0, 1.f);

                String message = choice.choiceName.toLowerCase(Locale.ROOT);
                if (messageToBossRelicMap.containsKey(message)) {
                    AbstractRelic rewardItem = messageToBossRelicMap.get(message);
                    Hitbox rewardItemHitbox = rewardItem.hb;
                    String rewardItemMessage = String.format("[vote %s] (%s)",
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

                    RenderHelpers
                            .renderTextBelowHitbox(spriteBatch, rewardItemMessage, rewardItemHitbox, messageColor);
                } else if (message.equalsIgnoreCase("skip")) {
                    String skipMessage = String.format("[vote %s] (%s)",
                            choice.voteString,
                            voteFrequencies.getOrDefault(choice.voteString, 0));

                    MenuCancelButton cancelButton = ReflectionHacks
                            .getPrivate(AbstractDungeon.bossRelicScreen, BossRelicSelectScreen.class, "cancelButton");

                    RenderHelpers
                            .renderTextBelowHitbox(spriteBatch, skipMessage, cancelButton.hb, messageColor);
                } else {
                    System.err.println("no boss relic button for " + choice.choiceName);
                }
            }
        }
    }
}
