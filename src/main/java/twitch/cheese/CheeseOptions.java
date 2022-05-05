package twitch.cheese;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.colorless.RitualDagger;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.*;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import tssrelics.relics.FestivuePole;
import tssrelics.relics.JadeMysticKnot;
import twitch.CheeseController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;

public class CheeseOptions {
    private static final HashMap<AbstractRelic.RelicTier, Callable<ArrayList<String>>> TIER_TO_LIST_MAP = new HashMap<AbstractRelic.RelicTier, Callable<ArrayList<String>>>() {{
        put(AbstractRelic.RelicTier.COMMON, () -> AbstractDungeon.commonRelicPool);
        put(AbstractRelic.RelicTier.UNCOMMON, () -> AbstractDungeon.uncommonRelicPool);
        put(AbstractRelic.RelicTier.RARE, () -> AbstractDungeon.rareRelicPool);
        put(AbstractRelic.RelicTier.BOSS, () -> AbstractDungeon.bossRelicPool);
        put(AbstractRelic.RelicTier.SHOP, () -> AbstractDungeon.shopRelicPool);
    }};

    public static final HashMap<String, CheeseController.CheeseConfig> AVAILABLE_CHEESES = new HashMap<String, CheeseController.CheeseConfig>() {{
        put("rainbow", new CheeseController.CheeseConfig("rainbow", () ->
                addRelic(new PrismaticShard().makeCopy()), false));

        put("serenity", new CheeseController.CheeseConfig("serenity", () ->
                addRelic(new FestivuePole().makeCopy()), true));

        put("tingting", new CheeseController.CheeseConfig("tingting", () ->
                addRelic(new Tingsha().makeCopy()), true));

        put("uglystick", new CheeseController.CheeseConfig("uglystick", () ->
                addRelic(new DeadBranch().makeCopy()), true));

        put("alltiedup", new CheeseController.CheeseConfig("alltiedup", () ->
                addRelic(new JadeMysticKnot().makeCopy()), true));

        put("knifeyspooney", new CheeseController.CheeseConfig("knifeyspooney", () -> {
            addRelic(new StrangeSpoon().makeCopy());

            AbstractDungeon.topLevelEffects
                    .add(new ShowCardAndObtainEffect(new RitualDagger()
                            .makeCopy(), (float) Settings.WIDTH / 2.0F - AbstractCard.IMG_WIDTH / 2.0F - 30.0F * Settings.scale, (float) Settings.HEIGHT / 2.0F));
        }, true));
    }};

    private static void addRelic(AbstractRelic relic) {
        AbstractDungeon.getCurrRoom()
                       .spawnRelicAndObtain((float) (Settings.WIDTH / 2), (float) (Settings.HEIGHT / 2), relic);

        try {
            TIER_TO_LIST_MAP.get(relic.tier).call().remove(relic.relicId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
