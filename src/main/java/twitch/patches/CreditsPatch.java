package twitch.patches;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.credits.CreditLine;
import com.megacrit.cardcrawl.credits.CreditsScreen;
import com.megacrit.cardcrawl.helpers.FontHelper;
import twitch.TwitchController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

public class CreditsPatch {
    @SpirePatch(clz = CreditsScreen.class, method = SpirePatch.CONSTRUCTOR)
    public static class ChangeCreditsPatch {
        @SpirePostfixPatch
        public static void editCreditLines(CreditsScreen creditsScreen) {
            ArrayList<CreditLine> lines = ReflectionHacks
                    .getPrivate(creditsScreen, CreditsScreen.class, "lines");

            ListIterator<CreditLine> linesIterator = lines.listIterator();
            while (linesIterator.hasNext()) {
                CreditLine line = linesIterator.next();
                BitmapFont font = ReflectionHacks.getPrivate(line, CreditLine.class, "font");

                // Remove all references to myself
                if (line.text.equals("Board Engineer")) {
                    linesIterator.remove();

                    while (linesIterator.hasPrevious()) {
                        CreditLine toRemove = linesIterator.previous();

                        BitmapFont tempFont = ReflectionHacks
                                .getPrivate(toRemove, CreditLine.class, "font");
                        boolean isHeading = tempFont == FontHelper.tipBodyFont;

                        linesIterator.remove();
                        if (isHeading) {
                            break;
                        }
                    }
                }
            }

            linesIterator = lines.listIterator();
            boolean addedMainSection = false;
            boolean addedExtension = false;
            boolean addedModState = false;

            while (linesIterator.hasNext()) {
                CreditLine line = linesIterator.next();

                if (!addedMainSection) {
                    if (line.text.equals("Communication Mod")) {
                        addedMainSection = true;
                        linesIterator.previous();
                        linesIterator
                                .add(new CreditLine("Battle Ai Suite (State Save Mod, Battle Ai Mod, Ludicrous Speed Mod)", 0F, true));
                        linesIterator.add(new CreditLine("Board Engineer", 0F, false));


                        try {
                            ArrayList<String> channelSubs = TwitchController.apiController
                                    .queryChannelSubscribers();

                            linesIterator
                                    .add(new CreditLine("Thanks to All Channel Subscribers", 0F, true));

                            Iterator<String> subIterator = channelSubs.iterator();
                            while (subIterator.hasNext()) {
                                String subsLine = subIterator.next();
                                for (int i = 0; i < 2 && subIterator.hasNext(); i++) {
                                    subsLine += " " + subIterator.next();
                                }
                                linesIterator.add(new CreditLine(subsLine, 0F, false));
                            }

                        } catch (IOException | NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (!addedExtension) {
                    if (line.text.equals("StSLib")) {
                        addedExtension = true;
                        linesIterator.previous();
                        linesIterator
                                .add(new CreditLine("CommunicationModExtension", 0F, true));
                        linesIterator.add(new CreditLine("Board Engineer", 0F, false));
                    }
                }

                if (!addedModState) {
                    if (line.text.equals("Music & Sounds")) {
                        addedModState = true;
                        linesIterator.previous();
                        linesIterator
                                .add(new CreditLine("State Extensions (Marisa, Vacant, Hermit, Cursed)", 0F, true));
                        linesIterator.add(new CreditLine("Board Engineer", 0F, false));

                        linesIterator
                                .add(new CreditLine("Twitch Slays Spire Relics", 0F, true));
                        linesIterator.add(new CreditLine("Board Engineer", 0F, false));
                    }
                }

            }

            reAlignCredits(lines);
        }
    }

    public static void reAlignCredits(ArrayList<CreditLine> lines) {
        float offset = -400F;

        for (CreditLine line : lines) {
            BitmapFont font = ReflectionHacks.getPrivate(line, CreditLine.class, "font");
            boolean isHeading = font == FontHelper.tipBodyFont;


            if (isHeading) {
                offset -= 150.0F;
            } else {
                offset -= 45.0F;
            }

            float y = offset * Settings.scale;

            ReflectionHacks.setPrivate(line, CreditLine.class, "y", y);
        }
    }
}
