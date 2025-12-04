package work.foofish.pvz.entities.plants;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import work.foofish.pvz.entities.bullets.PeaBullet;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.utils.AssetPaths;

/**
 * 双发射手：和豌豆射手类似，但每轮快速发射两颗豌豆。
 */
public class RepeaterPea extends BasePlant {
    private static final int COST = 200;
    private static final int HEALTH = 300;
    private static final float COOLDOWN = 7.5f;
    private static final float SHOOT_INTERVAL = MathUtils.random(1.36f, 1.5f);
    private static final float DOUBLE_SHOT_DELAY = 0.18f;
    private static final float BULLET_SPEED = 280f;
    private static final int BULLET_DAMAGE = 20;

    private float shootTimer;
    private float volleyTimer;
    private int pendingShots;

    public RepeaterPea (GameScreen screen, float x, float y, int row, int col) {
        super(screen, x, y, row, col,
            createAnimation(screen),
            COST,
            HEALTH,
            COOLDOWN);
    }

    private static Animation<TextureRegion> createAnimation (GameScreen screen) {
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.PLANTS_ATLAS);
        return new Animation<>(0.08f, atlas.findRegions(AssetPaths.REGION_REPEATERPEA), Animation.PlayMode.LOOP);
    }

    @Override
    public void action (float delta) {
        shootTimer += delta;

        // 处理第二发延迟豌豆
        if (pendingShots > 0) {
            volleyTimer += delta;
            if (volleyTimer >= DOUBLE_SHOT_DELAY) {
                volleyTimer = 0f;
                pendingShots--;
                spawnBullet();
            }
        }

        if (!screen.hasVisibleZombieInRow(row)) {
            shootTimer = Math.min(shootTimer, SHOOT_INTERVAL);
            return;
        }

        if (pendingShots == 0 && shootTimer >= SHOOT_INTERVAL) {
            shootTimer = 0f;
            spawnBullet();
            pendingShots = 1; // 还需补发一颗
            volleyTimer = 0f;
        }
    }

    private void spawnBullet () {
        float bulletX = position.x + bounds.width - 40f;
        float bulletY = position.y + bounds.height * 0.6f;
        PeaBullet bullet = new PeaBullet(screen, bulletX, bulletY, row, BULLET_SPEED, BULLET_DAMAGE);
        screen.addBullet(bullet);
    }
}
