package twitch.votecontrollers;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.potions.*;
import com.megacrit.cardcrawl.relics.*;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.ui.buttons.CancelButton;
import com.megacrit.cardcrawl.ui.buttons.ProceedButton;
import communicationmod.CommandExecutor;
import tssrelics.relics.SneckoCharm;
import tssrelics.relics.SneckoSkinBoots;
import twitch.Choice;
import twitch.RewardInfo;
import twitch.TwitchController;
import twitch.VoteController;

import java.util.*;
import java.util.stream.Collectors;

import static twitch.RenderHelpers.renderTextBelowHitbox;

public class CombatRewardVoteController extends VoteController {
    private final HashMap<String, RewardItem> voteStringToCombatRewardItem;
    private final HashMap<String, String> voteStringToOriginalRewardTextMap;
    private final JsonObject stateJson;

    public CombatRewardVoteController(TwitchController twitchController, JsonObject stateJson) {
        super(twitchController);
        this.stateJson = stateJson;

        JsonObject gameState = stateJson.get("game_state").getAsJsonObject();
        JsonArray choicesJson = gameState.get("choice_list").getAsJsonArray();

        voteStringToCombatRewardItem = new HashMap<>();
        voteStringToOriginalRewardTextMap = new HashMap<>();

        for (int i = 0; i < choicesJson.size(); i++) {
            String voteString = Integer.toString(i + 1);

            RewardItem reward = AbstractDungeon.combatRewardScreen.rewards.get(i);

            voteStringToCombatRewardItem.put(voteString, reward);
            voteStringToOriginalRewardTextMap.put(voteString, reward.text);
        }
    }

    @Override
    public void setUpChoices() {
        JsonObject gameState = stateJson.get("game_state").getAsJsonObject();

        JsonArray rewardsArray = gameState.get("screen_state").getAsJsonObject()
                                          .get("rewards").getAsJsonArray();
        JsonArray choicesJson = gameState.get("choice_list").getAsJsonArray();

        if (choicesJson.size() == rewardsArray.size()) {
            twitchController.choices = new ArrayList<>();
            for (int i = 0; i < choicesJson.size(); i++) {
                String choiceString = choicesJson.get(i).getAsString();
                JsonObject rewardJson = rewardsArray.get(i).getAsJsonObject();
                String choiceCommand = String.format("choose %s", twitchController.choices.size());

                // the voteString will start at 1
                String voteString = Integer.toString(twitchController.choices.size() + 1);

                Choice toAdd = new Choice(choiceString, voteString, choiceCommand);
                toAdd.rewardInfo = Optional.of(new RewardInfo(rewardJson));
                if (toAdd.rewardInfo.isPresent() && toAdd.rewardInfo.get().relicName != null) {
                    toAdd.choiceName = toAdd.rewardInfo.get().relicName;
                }
                twitchController.choices.add(toAdd);
            }
        } else {
            System.err.println("What are you doing susan???????");
        }

        // "True" Choices code
        twitchController.skipAfterCard = true;
        boolean shouldAllowLeave = false;

        ArrayList<Choice> result = new ArrayList<>();

        twitchController.choices.stream()
                                .forEach(choice -> result.add(choice));


        // If anything from the auto choice list is in the reward list, product a list with it
        // as the only choice
        for (String choiceName : AUTO_CHOICE_NAMES) {
            Optional<Choice> autoChoice = result.stream()
                                                .filter(choice -> choice.choiceName
                                                                         .equals(choiceName))
                                                .findAny();

            if (autoChoice.isPresent()) {
                ArrayList<Choice> onlyOption = new ArrayList<>();
                onlyOption.add(autoChoice.get());
                twitchController.viableChoices = onlyOption;
                return;
            }
        }

        Collection<Choice> potionChoices = result.stream()
                                                 .filter(choice -> choice.choiceName
                                                                          .equals("potion"))
                                                 .collect(Collectors
                                                                          .toCollection(ArrayList::new));

        boolean hasSozu = AbstractDungeon.player.hasRelic(Sozu.ID);

        boolean hasPotionSlot = AbstractDungeon.player.potions.stream()
                                                              .anyMatch(potion -> potion instanceof PotionSlot);

        boolean shouldEvaluatePotions = !hasPotionSlot && !hasSozu;

        for (Choice potionChoice : potionChoices) {
            boolean skipPotion = false;

            // Can pick up a potion but has no slots.
            if (shouldEvaluatePotions) {
                AbstractPotion lowWeightPotion = null;
                int weightDifferential = 0;
                int choiceWeight = POTION_WEIGHTS
                        .getOrDefault(potionChoice.rewardInfo.get().potionName, 5);

                // Discard the lowest value potion
                for (AbstractPotion p : AbstractDungeon.player.potions) {
                    int w = POTION_WEIGHTS.getOrDefault(p.name, 5);
                    if (choiceWeight > w) {
                        if (lowWeightPotion != null) {
                            int newWDiff = w - choiceWeight;
                            if (newWDiff < weightDifferential) {
                                weightDifferential = newWDiff;
                                lowWeightPotion = p;
                            }
                        } else {
                            weightDifferential = w - choiceWeight;
                            lowWeightPotion = p;
                        }
                    }
                }

                if (lowWeightPotion != null) {
                    AbstractDungeon.player.removePotion(lowWeightPotion);
                } else {
                    // The incoming potion is of the lowest value
                    skipPotion = true;
                }
            }

            if (!skipPotion) {
                ArrayList<Choice> onlyPotion = new ArrayList<>();
                onlyPotion.add(potionChoice);

                // Then the potion
                twitchController.viableChoices = onlyPotion;
                return;
            } else {
                result.remove(potionChoice);
            }
        }

        Optional<Choice> relicChoice = result.stream()
                                             .filter(choice -> choice.rewardInfo
                                                                      .isPresent() && choice.rewardInfo
                                                                      .get().relicName != null)
                                             .findAny();

        Optional<Choice> sapphireKeyChoice = result.stream()
                                                   .filter(choice -> choice.choiceName
                                                                            .equals("sapphire_key"))
                                                   .findAny();

        // Automatically take relics unless there's a sapphire key attached.
        // TODO: Relics not attached to sapphire keys should be taken before the sapphire key vote.
        if (relicChoice.isPresent() && !sapphireKeyChoice.isPresent()) {
            ArrayList<Choice> onlyRelic = new ArrayList<>();
            onlyRelic.add(relicChoice.get());

            if (OPTIONAL_RELICS
                    .contains(relicChoice.get().rewardInfo.get().relicName)) {
                shouldAllowLeave = true;
            } else {
                twitchController.viableChoices = onlyRelic;
                return;
            }
        }

        if (result.size() > 1) {
            twitchController.skipAfterCard = false;
            shouldAllowLeave = true;
        }
        if (shouldAllowLeave) {
            if (CommandExecutor.isCancelCommandAvailable()) {
                result.add(new Choice("cancel", "0", "cancel"));
            } else {
                result.add(new Choice("leave", "0", "leave", "proceed"));
            }
        }

        twitchController.viableChoices = result;
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        HashMap<String, Integer> voteFrequencies = twitchController.getVoteFrequencies();
        Set<String> winningResults = twitchController.getBestVoteResultKeys();

        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            Choice choice = twitchController.viableChoices.get(i);

            Color messageColor = winningResults
                    .contains(choice.voteString) ? new Color(1.f, 1.f, 0, 1.f) : new Color(1.f, 0, 0, 1.f);

            String message = choice.choiceName;
            if (message.equals("leave")) {
                String leaveMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                renderTextBelowHitbox(spriteBatch, leaveMessage, ReflectionHacks
                        .getPrivate(AbstractDungeon.overlayMenu.proceedButton, ProceedButton.class, "hb"), messageColor);
            } else if (message.equals("cancel")) {
                String leaveMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                renderTextBelowHitbox(spriteBatch, leaveMessage, ReflectionHacks
                        .getPrivate(AbstractDungeon.overlayMenu.cancelButton, CancelButton.class, "hb"), messageColor);
            } else if (voteStringToCombatRewardItem.containsKey(choice.voteString)) {
                RewardItem rewardItem = voteStringToCombatRewardItem.get(choice.voteString);
                String rewardItemMessage = String.format("[vote %s] (%s)",
                        choice.voteString,
                        voteFrequencies.getOrDefault(choice.voteString, 0));

                rewardItem.text = voteStringToOriginalRewardTextMap
                        .get(choice.voteString) + rewardItemMessage;
            } else {
                System.err.println("no card button for " + choice.choiceName);
            }
        }
    }

    @Override
    public void endVote() {
        // Reset the text to avoid duped vote strings
        for (int i = 0; i < twitchController.viableChoices.size(); i++) {
            Choice choice = twitchController.viableChoices.get(i);
            if (voteStringToCombatRewardItem.containsKey(choice.voteString)) {
                RewardItem rewardItem = voteStringToCombatRewardItem.get(choice.voteString);
                rewardItem.text = voteStringToOriginalRewardTextMap.get(choice.voteString);
            }
        }
    }

    @Override
    public Optional<String> getTipString() {
        if (!twitchController.skipAfterCard) {
            String resultString = "Card Reward Listing: ";
            for (Map.Entry<String, RewardItem> entry : voteStringToCombatRewardItem.entrySet()) {
                RewardItem reward = entry.getValue();
                if (reward.type == RewardItem.RewardType.CARD) {
                    resultString += String.format("[ %s | %s ]", entry.getKey(),
                            reward.cards.stream().map(card -> card.name)
                                        .collect(Collectors.joining(" , ")));
                }
            }

            return Optional.of(resultString);
        }
        return super.getTipString();
    }

    public static HashSet<String> POTION_NAMES = new HashSet<String>();

    public static HashMap<String, Integer> POTION_WEIGHTS = new HashMap<String, Integer>() {{
        //General weighting philosophy: Immediate effects outweigh build up potions in effectiveness because the bot tends to spam potions to end a combat.
        // Potions that give the bot more choice or mitigate damage are preferable.
        // Targetable potions are probably bad.
        //Debuff
        put(PotionHelper.getPotion(WeakenPotion.POTION_ID).name, 3);
        put(PotionHelper.getPotion(FearPotion.POTION_ID).name, 3);
        //Resource
        //Energy
        put(PotionHelper.getPotion(BottledMiracle.POTION_ID).name, 7);
        put(PotionHelper.getPotion(EnergyPotion.POTION_ID).name, 6);

        //Draw
        put(PotionHelper.getPotion(SwiftPotion.POTION_ID).name, 5);
        put(PotionHelper.getPotion(SneckoOil.POTION_ID).name, 8);
        put(PotionHelper.getPotion(GamblersBrew.POTION_ID).name, 0);

        //BlockPotion
        put(PotionHelper.getPotion(BlockPotion.POTION_ID).name, 7);
        put(PotionHelper.getPotion(EssenceOfSteel.POTION_ID).name, 5);
        put(PotionHelper.getPotion(HeartOfIron.POTION_ID).name, 6);
        put(PotionHelper.getPotion(GhostInAJar.POTION_ID).name, 8);

        //HP
        put(PotionHelper.getPotion(FruitJuice.POTION_ID).name, 10);
        put(PotionHelper.getPotion(BloodPotion.POTION_ID).name, 10);
        put(PotionHelper.getPotion(RegenPotion.POTION_ID).name, 8);
        put(PotionHelper.getPotion(FairyPotion.POTION_ID).name, 11);

        put(PotionHelper.getPotion(EntropicBrew.POTION_ID).name, 9);
        put(PotionHelper.getPotion(Ambrosia.POTION_ID).name, 7);

        //Stat
        put(PotionHelper.getPotion(StrengthPotion.POTION_ID).name, 4);
        put(PotionHelper.getPotion(CultistPotion.POTION_ID).name, 6);
        put(PotionHelper.getPotion(DexterityPotion.POTION_ID).name, 5);
        put(PotionHelper.getPotion(FocusPotion.POTION_ID).name, 6);
        put(PotionHelper.getPotion(PotionOfCapacity.POTION_ID).name, 4);
        put(PotionHelper.getPotion(AncientPotion.POTION_ID).name, 3);

        //Temp stat
        put(PotionHelper.getPotion(SpeedPotion.POTION_ID).name, 5);
        put(PotionHelper.getPotion(SteroidPotion.POTION_ID).name, 4);

        //Card choice
        put(PotionHelper.getPotion(AttackPotion.POTION_ID).name, 4);
        put(PotionHelper.getPotion(SkillPotion.POTION_ID).name, 4);
        put(PotionHelper.getPotion(PowerPotion.POTION_ID).name, 4);
        put(PotionHelper.getPotion(ColorlessPotion.POTION_ID).name, 4);

        put(PotionHelper.getPotion(LiquidMemories.POTION_ID).name, 5);

        //Damage
        //Direct
        put(PotionHelper.getPotion(FirePotion.POTION_ID).name, 6);
        put(PotionHelper.getPotion(ExplosivePotion.POTION_ID).name, 6);
        put(PotionHelper.getPotion(PoisonPotion.POTION_ID).name, 5);
        put(PotionHelper.getPotion(CunningPotion.POTION_ID).name, 5);

        //Indirect
        put(PotionHelper.getPotion(EssenceOfDarkness.POTION_ID).name, 6);
        put(PotionHelper.getPotion(LiquidBronze.POTION_ID).name, 4);

        //Misc
        put(PotionHelper.getPotion(SmokeBomb.POTION_ID).name, 5);
        put(PotionHelper.getPotion(StancePotion.POTION_ID).name, 0);
        //Cards

        put(PotionHelper.getPotion(BlessingOfTheForge.POTION_ID).name, 2);
        put(PotionHelper.getPotion(DuplicationPotion.POTION_ID).name, 5);
        put(PotionHelper.getPotion(DistilledChaosPotion.POTION_ID).name, 5);
        put(PotionHelper.getPotion(Elixir.POTION_ID).name, 4);

        keySet().forEach(key -> POTION_NAMES.add(key.toLowerCase()));
    }};

    public static HashSet<String> OPTIONAL_RELICS = new HashSet<String>() {{
        add(new BottledFlame().name);
        add(new BottledLightning().name);
        add(new BottledTornado().name);
        add(new DeadBranch().name);
        add(new Omamori().name);
        add(new TinyChest().name);
        add(new WarPaint().name);
        add(new Whetstone().name);
        add(new NinjaScroll().name);
        add(new BlueCandle().name);

        add(SneckoSkinBoots.ID);
        add(SneckoCharm.ID);
    }};

    public static HashSet<String> AUTO_CHOICE_NAMES = new HashSet<String>() {{
        add("gold");
        add("hermit_bounty");
        add("stolen_gold");
        add("emerald_key");
    }};
}
