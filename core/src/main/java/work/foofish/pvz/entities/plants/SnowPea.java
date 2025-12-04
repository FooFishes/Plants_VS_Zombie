package work.foofish.pvz.entities.plants;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import work.foofish.pvz.entities.bullets.PeaBullet;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.utils.AssetPaths;

/**
 * 寒冰射手：发射带有减速效果的豌豆。
 */
public class SnowPea extends BasePlant {
    private static final int COST = 175;
    private static final int HEALTH = 300;
    private static final float COOLDOWN = 7.5f;
    private static final float SHOOT_INTERVAL = MathUtils.random(1.36f, 1.5f);
    private static final float BULLET_SPEED = 280f;
    private static final int BULLET_DAMAGE = 20;

    private float shootTimer;

    public SnowPea (GameScreen screen, float x, float y, int row, int col) {
        super(screen, x, y, row, col,
            createAnimation(screen),
            COST,
            HEALTH,
            COOLDOWN);
    }

    private static Animation<TextureRegion> createAnimation (GameScreen screen) {
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.PLANTS_ATLAS);
        Array<TextureAtlas.AtlasRegion> frames = atlas != null ? atlas.findRegions(AssetPaths.REGION_SNOWPEA) : null;
        if (frames == null || frames.size == 0) {
            frames = atlas != null ? atlas.findRegions(AssetPaths.REGION_PEASHOOTER) : null;
        }
        if (frames == null || frames.size == 0) {
            throw new IllegalStateException("No frames found for Snow Pea animation.");
        }
        return new Animation<>(0.1f, frames, Animation.PlayMode.LOOP);
    }

    @Override
    public void action (float delta) {
        shootTimer += delta;
        if (!screen.hasVisibleZombieInRow(row)) {
            shootTimer = Math.min(shootTimer, SHOOT_INTERVAL);
            return;
        }

        if (shootTimer >= SHOOT_INTERVAL) {
            shootTimer = 0f;
            fire();
        }
    }

    private void fire () {
        float bulletX = position.x + bounds.width - 40f;
        float bulletY = position.y + bounds.height * 0.6f;
        PeaBullet bullet = new PeaBullet(screen, bulletX, bulletY, row, BULLET_SPEED, BULLET_DAMAGE, PeaBullet.Type.SNOW);
        screen.addBullet(bullet);
    }
}

