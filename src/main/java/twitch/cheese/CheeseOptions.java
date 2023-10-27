package twitch.cheese;

import basemod.BaseMod;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.blue.Amplify;
import com.megacrit.cardcrawl.cards.blue.CreativeAI;
import com.megacrit.cardcrawl.cards.blue.MeteorStrike;
import com.megacrit.cardcrawl.cards.colorless.*;
import com.megacrit.cardcrawl.cards.curses.Pride;
import com.megacrit.cardcrawl.cards.green.Burst;
import com.megacrit.cardcrawl.cards.green.Nightmare;
import com.megacrit.cardcrawl.cards.purple.MasterReality;
import com.megacrit.cardcrawl.cards.red.DoubleTap;
import com.megacrit.cardcrawl.cards.red.DualWield;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.*;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import theVacant.cards.Skills.Expand;
import tssrelics.relics.*;
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

        put("tingting", new CheeseController.CheeseConfig("tingting", () ->
                addRelic(new Tingsha().makeCopy()), true));

        put("uglystick", new CheeseController.CheeseConfig("uglystick", () ->
                addRelic(new DeadBranch().makeCopy()), true));

        if (BaseMod.hasModID("TSSRelics:")) {
            put("alltiedup", new CheeseController.CheeseConfig("alltiedup", () ->
                    addRelic(new JadeMysticKnot().makeCopy()), true));
            put("serenity", new CheeseController.CheeseConfig("serenity", () ->
                    addRelic(new FestivuePole().makeCopy()), true));
            put("riskandreward", new CheeseController.CheeseConfig("riskandreward", () ->
                    addRelic(new SneckoCharm().makeCopy()), false));
        }

        put("likeasensei", new CheeseController.CheeseConfig("likeasensei", () ->
                addRelic(new UnceasingTop().makeCopy()), true));

        put("weallscream", new CheeseController.CheeseConfig("weallscream", () ->
                addRelic(new IceCream().makeCopy()), true));

        put("powerpuff", new CheeseController.CheeseConfig("powerpuff", () ->
                addRelic(new ChemicalX().makeCopy()), true));

        put("cawcawcaw", new CheeseController.CheeseConfig("cawcawcaw", () -> {
            addRelic(new CultistMask().makeCopy());
            addRelic(new CultistMask().makeCopy());
            addRelic(new CultistMask().makeCopy());
        }, true));

        put("knifeyspooney", new CheeseController.CheeseConfig("knifeyspooney", () -> {
            addRelic(new StrangeSpoon().makeCopy());
            addCard(new RitualDagger().makeCopy());
        }, true));


        if (BaseMod.hasModID("VacantState:")) {
            put("eminentdomain", new CheeseController.CheeseConfig("eminentdomain", () -> {
                addRelic(new MeatOnTheBone().makeCopy());
                addCard(new Expand().makeCopy());
                addCard(new Expand().makeCopy());
            }, true));
        }

        put("clownswilleatme", new CheeseController.CheeseConfig("clownswilleatme", () -> {
            addCard(new Nightmare().makeCopy());
        }, true));

        put("dualwield", new CheeseController.CheeseConfig("dualwield", () -> {
            addCard(new DualWield().makeCopy());
        }, true));

        put("iknowkungfu", new CheeseController.CheeseConfig("iknowkungfu", () -> {
            removeStartRelic();
            addRelic(new PandorasBox().makeCopy());
        }, true));

        put("makesthewholeworldblind", new CheeseController.CheeseConfig("makesthewholeworldblind", () -> {
            removeStartRelic();
            addRelic(new SneckoEye().makeCopy());
        }, true));

        put("ineedamedic", new CheeseController.CheeseConfig("ineedamedic", () ->
                addRelic(new ToughBandages().makeCopy()), true));

        put("esuna", new CheeseController.CheeseConfig("esuna", () ->
                addRelic(new MedicalKit().makeCopy()), true));

        put("cursedbreakfast", new CheeseController.CheeseConfig("cursedbreakfast", () -> {
            removeStartRelic();

            removeRelic(new ToxicEgg2().makeCopy());
            removeRelic(new MoltenEgg2().makeCopy());
            removeRelic(new FrozenEgg2().makeCopy());

            AbstractDungeon.rareRelicPool.add(0, ToxicEgg2.ID);
            AbstractDungeon.uncommonRelicPool.add(0, MoltenEgg2.ID);
            AbstractDungeon.commonRelicPool.add(0, FrozenEgg2.ID);

            addRelic(new CallingBell().makeCopy());

        }, true));

        put("choasmode", new CheeseController.CheeseConfig("choasmode", () -> {
            addRelic(new PrismaticBranch().makeCopy());
        }, true));

        put("ihatecards", new CheeseController.CheeseConfig("ihatecards", () -> {
            removeStartRelic();
            addRelic(new BustedCrown().makeCopy());
            addRelic(new SmilingMask().makeCopy());
            addRelic(new SingingBowl().makeCopy());
            addRelic(new BusinessContract().makeCopy());
        }, true));

        put("homeontherange", new CheeseController.CheeseConfig("homeontherange", () -> {
            removeStartRelic();

            addRelic(new HappyFlowerBed().makeCopy());
            addRelic(new TinyHouse().makeCopy());
        }, true));

        put("myfriendthemerchant", new CheeseController.CheeseConfig("myfriendthemerchant", () -> {
            addRelic(new WingBoots().makeCopy());
            addRelic(new Courier().makeCopy());
            addRelic(new MembershipCard().makeCopy() );
        }, true));

        put("sinsinprogress", new CheeseController.CheeseConfig("sinsinprogress", () -> {
            addCard(new Pride().makeCopy());
            addCard(new HandOfGreed().makeCopy());
            addCard(new Pride().makeCopy());
            addCard(new HandOfGreed().makeCopy());
        }, true));

        put("toofattofail", new CheeseController.CheeseConfig("toofattofail", () -> {
            removeStartRelic();

            addRelic(new CoffeeDripper().makeCopy());

            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());
            addRelic(new Mango().makeCopy());

            addRelic(new MarkOfTheBloom().makeCopy());
        }, true));

        put("giftsfromabove", new CheeseController.CheeseConfig("giftsfromabove", () -> {
            removeStartRelic();

            addRelic(new HornOfPlenty().makeCopy());
            addCard(new Pride().makeCopy());
            addCard(new MeteorStrike().makeCopy());
            addRelic(new PrismaticShard().makeCopy());
        }, true));

        put("toilandtrouble", new CheeseController.CheeseConfig("toilandtrouble", () -> {
            addCard(new DoubleTap().makeCopy());
            addCard(new Amplify().makeCopy());
            addCard(new Burst().makeCopy());
        }, true));

        put("powerfulpowers", new CheeseController.CheeseConfig("powerfulpowers", () -> {
            addCard(new MasterReality().makeCopy());
            addCard(new CreativeAI().makeCopy());
            addCard(new Amplify().makeCopy());
        }, true));

        put("bedbugs", new CheeseController.CheeseConfig("bedbugs", () -> {
            addCard(new Madness().makeCopy());
            addCard(new Madness().makeCopy());
            addCard(new Nightmare().makeCopy());
        }, true));

        put("monsterhunter", new CheeseController.CheeseConfig("monsterhunter", () -> {
            removeStartRelic();

            addRelic(new BlackStar().makeCopy());
            addRelic(new WingBoots().makeCopy());

            addRelic(new PreservedAmber().makeCopy());
            addRelic(new PreservedInsect().makeCopy());
        }, true));

        put("kafkaesque", new CheeseController.CheeseConfig("kafkaesque", () -> {
            addCard(new Metamorphosis().makeCopy());
            addCard(new Chrysalis().makeCopy());
        }, true));
    }};

    private static void addRelic(AbstractRelic relic) {
        AbstractDungeon.getCurrRoom()
                       .spawnRelicAndObtain((float) (Settings.WIDTH / 2), (float) (Settings.HEIGHT / 2), relic);
        removeRelic(relic);
    }

    private static void removeRelic(AbstractRelic relic) {
        try {
            TIER_TO_LIST_MAP.get(relic.tier).call().remove(relic.relicId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addCard(AbstractCard card) {
        AbstractDungeon.topLevelEffects
                .add(new ShowCardAndObtainEffect(card, (float) Settings.WIDTH / 2.0F - AbstractCard.IMG_WIDTH / 2.0F - 30.0F * Settings.scale, (float) Settings.HEIGHT / 2.0F));
    }

    private static void removeStartRelic() {
        AbstractDungeon.player
                .loseRelic(AbstractDungeon.player.relics.get(0).relicId);
    }
}
