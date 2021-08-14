package twitch.patches;

import ThMod.event.OrinTheCat;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import java.util.ArrayList;

public class EventsPatches {
    @SpirePatch(clz = AbstractDungeon.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {String.class, String.class, AbstractPlayer.class, ArrayList.class})
    public static class DisableEventsPatch {
        @SpirePostfixPatch
        public static void RemoveBadEvents(AbstractDungeon dungeon, String name, String levelId, AbstractPlayer p, ArrayList<String> newSpecialOneTimeEventList) {
            // We don't yet have a stream-friendly implementation of Match and keep
            AbstractDungeon.shrineList.remove("Match and Keep!");

            // The Orin the cat Monster doesn't save/load state correctly
            AbstractDungeon.eventList.remove(OrinTheCat.ID);
        }
    }
}
