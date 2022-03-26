package twitch.cheese;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.neow.NeowReward;
import twitch.CheeseController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class NeowPatches {
    @SpirePatch(clz = NeowReward.class, method = "activate")
    public static class AddNirlysPatch {
        @SpirePrefixPatch
        public static SpireReturn addNilrys(NeowReward neowReward) {
            if (CheeseController.requestedCheeseConfig.isPresent()) {
                CheeseController.CheeseConfig config = CheeseController.requestedCheeseConfig.get();

                config.cheeseEffect.run();

                CheeseController.requestedCheeseConfig = Optional.empty();

                // clear the cheese config
                new Thread(() -> {
                    CheeseController.cheeseConfig.remove("cheese_id");
                    try {
                        CheeseController.cheeseConfig.save();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                return config.replaceNeow ? SpireReturn.Return(null) : SpireReturn.Continue();
            }

            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = NeowEvent.class, method = "blessing")
    @SpirePatch(clz = NeowEvent.class, method = "miniBlessing")
    public static class CustomBlessingPatch {
        @SpirePrefixPatch
        public static SpireReturn replaceBlessings(NeowEvent neowEvent) {
            if (CheeseController.requestedCheeseConfig
                    .isPresent() && CheeseController.requestedCheeseConfig.get().replaceNeow) {
                ArrayList<NeowReward> rewards = ReflectionHacks
                        .getPrivate(neowEvent, NeowEvent.class, "rewards");

                NeowReward cheeseReward = new NeowReward(false);
                cheeseReward.optionLabel = "It's Cheese Time!!!";
                rewards.add(cheeseReward);

                ReflectionHacks.setPrivate(neowEvent, NeowEvent.class, "screenNum", 3);

                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }
    }
}
