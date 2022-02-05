import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.gikk.twirk.types.AbstractTwitchUserFields;
import com.gikk.twirk.types.TagMap;
import com.gikk.twirk.types.emote.EmoteParser;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

public class SilenceTwirkPatches {
    @SpirePatch(clz = EmoteParser.class, method = "parseEmotes")
    public static class SilentEmoteThing {
        @SpirePrefixPatch
        public static SpireReturn doNothing(String content, String tag) {
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch(clz = AbstractTwitchUserFields.class, method = "parseUserProperties")
    public static class SilentOtherEmoteThing {
        @SpirePrefixPatch
        public static SpireReturn noEmoteParsing(AbstractTwitchUserFields abstractTwitchUserFields, TwitchMessage message) {
            String channelOwner = message.getTarget().substring(1);
            TagMap r = message.getTagMap();
            String temp = message.getPrefix();
            String testLogin = r.getAsString("login");
            if (testLogin.isEmpty()) {
                abstractTwitchUserFields.userName = temp.contains("!") ? temp
                        .substring(1, temp.indexOf("!")) : "";
            } else {
                abstractTwitchUserFields.userName = testLogin;
            }

            temp = r.getAsString("display-name");
            abstractTwitchUserFields.displayName = temp.isEmpty() ? Character
                    .toUpperCase(abstractTwitchUserFields.userName
                            .charAt(0)) + abstractTwitchUserFields.userName.substring(1) : temp;
            temp = r.getAsString("badges");
            abstractTwitchUserFields.badges = temp.isEmpty() ? new String[0] : temp.split(",");
            abstractTwitchUserFields.isMod = r.getAsBoolean("mod");
            abstractTwitchUserFields.isSub = r.getAsBoolean("subscriber");
            abstractTwitchUserFields.isTurbo = r.getAsBoolean("turbo");
            abstractTwitchUserFields.userID = r.getAsLong("user-id");

            return SpireReturn.Return(null);
        }
    }
}
