package twitch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;

public class RenderHelpers {
    public static void renderTextBelowHitbox(SpriteBatch spriteBatch, String text, Hitbox hitbox) {
        renderTextBelowHitbox(spriteBatch, text, hitbox, null);
    }

    public static void renderTextBelowHitbox(SpriteBatch spriteBatch, String text, Hitbox hitbox, Color color) {
        BitmapFont font = FontHelper.buttonLabelFont;
        Color actualColor = color == null ? Color.RED : color;
        float textWidth = FontHelper.getWidth(font, text, 1f);
        float messageX = hitbox.x + (hitbox.width - textWidth) / 2;

        FontHelper.renderFont(spriteBatch, font, text, messageX, hitbox.y, actualColor);
    }

    public static void renderTextAboveHitbox(SpriteBatch spriteBatch, String text, Hitbox hitbox) {
        renderTextAboveHitbox(spriteBatch, text, hitbox, null);
    }

    public static void renderTextAboveHitbox(SpriteBatch spriteBatch, String text, Hitbox hitbox, Color color) {
        BitmapFont font = FontHelper.buttonLabelFont;
        Color actualColor = color == null ? Color.RED : color;
        float textWidth = FontHelper.getWidth(font, text, 1f);
        float messageX = hitbox.x + (hitbox.width - textWidth) / 2;

        float messageY = hitbox.y + hitbox.height + FontHelper.getHeight(font);

        FontHelper.renderFont(spriteBatch, font, text, messageX, messageY, actualColor);
    }
}
