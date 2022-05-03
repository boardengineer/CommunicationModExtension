package twitch;

import com.google.gson.JsonObject;

public class RewardInfo {
    public final String rewardType;
    public String potionName = null;
    public String relicName = null;

    public RewardInfo(JsonObject rewardJson) {
        rewardType = rewardJson.get("reward_type").getAsString();
        if (rewardType.equals("POTION")) {
            potionName = rewardJson.get("potion").getAsJsonObject().get("name").getAsString();
        } else if (rewardType.equals("RELIC")) {
            relicName = rewardJson.get("relic").getAsJsonObject().get("name").getAsString();
        }
    }
}
