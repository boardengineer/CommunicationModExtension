package twitch.patches;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.AsyncSaver;
import com.megacrit.cardcrawl.helpers.File;

import java.util.concurrent.BlockingQueue;

public class SavePatches {
    @SpirePatch(clz = AsyncSaver.class, method = "save")
    public static class BackUpAllSavesPatch {
        @SpirePostfixPatch
        public static void backUpSave(String filePath, String data) {
            BlockingQueue<File> saveQueue = ReflectionHacks
                    .getPrivateStatic(AsyncSaver.class, "saveQueue");

            String backupFilePath = String
                    .format("savealls\\%s_%02d_%s", filePath, AbstractDungeon.floorNum, Settings.seed);

            saveQueue.add(new File(backupFilePath, data));
        }
    }
}
