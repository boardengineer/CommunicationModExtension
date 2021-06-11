package twitch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;

public class RenderHelpers {
    static void renderTextBelowHitbox(SpriteBatch spriteBatch, String text, Hitbox hitbox) {
        BitmapFont font = FontHelper.buttonLabelFont;
        Color color = Color.RED;
        float textWidth = FontHelper.getWidth(font, text, 1f);
        float messageX = hitbox.x + (hitbox.width - textWidth) / 2;

        FontHelper.renderFont(spriteBatch, font, text, messageX, hitbox.y, color);
    }

    static void renderTextAboveHitbox(SpriteBatch spriteBatch, String text, Hitbox hitbox) {
        BitmapFont font = FontHelper.buttonLabelFont;
        Color color = Color.RED;
        float textWidth = FontHelper.getWidth(font, text, 1f);
        float messageX = hitbox.x + (hitbox.width - textWidth) / 2;

        float messageY = hitbox.y + hitbox.height + FontHelper.getHeight(font);

        FontHelper.renderFont(spriteBatch, font, text, messageX, messageY, color);
    }
}
