package twitch.patches;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.AsyncSaver;
import com.megacrit.cardcrawl.helpers.File;
import com.megacrit.cardcrawl.helpers.SeedHelper;

import java.util.concurrent.BlockingQueue;

public class SavePatches {
    @SpirePatch(clz = AsyncSaver.class, method = "save")
    public static class BackUpAllSavesPatch {
        @SpirePostfixPatch
        public static void backUpSave(String filePath, String data) {
            BlockingQueue<File> saveQueue = ReflectionHacks
                    .getPrivateStatic(AsyncSaver.class, "saveQueue");


            try {
                String fileName = String.format("startstates\\%s\\%02d\\%s", SeedHelper
                        .getString(Settings.seed), AbstractDungeon.floorNum, filePath);


                System.err.println("(before) writing to " + fileName);
                if (fileName.contains("saves\\")) {
                    fileName = fileName.replace("saves\\", "");
                } else {
                    return;
                }

                java.io.File saveFile = new java.io.File(fileName);
                boolean endFileExists = saveFile.exists() && !saveFile.isDirectory();

                System.err.println("(after) writing to " + fileName);
                if (!endFileExists) {
                    saveQueue.add(new File(fileName, data));
                } else {
                    System.err.println("not overwriting");
                }
            } catch (NullPointerException e) {
            }
        }
    }
}
