package work.foofish.pvz.entities.plants;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import work.foofish.pvz.entities.zombies.BaseZombie;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.ui.Boom;
import work.foofish.pvz.utils.AssetPaths;

/**
 * 樱桃炸弹：放置后短暂蓄力，随后对周围区域造成高额伤害。
 */
public class CherryBomb extends BasePlant {
    private static final int COST = 150;
    private static final int HEALTH = 999;
    private static final float COOLDOWN = 30f;
    private static final float FUSE_TIME = 1.0f;
    private static final float EXPLOSION_RADIUS = 150f;
    private static final int EXPLOSION_DAMAGE = 1800;
    private static final float BOOM_SCALE = 1.5f;

    private float fuseTimer;
    private boolean exploded;
    private final Rectangle explosionArea = new Rectangle();

    public CherryBomb (GameScreen screen, float x, float y, int row, int col) {
        super(screen, x, y, row, col,
            createAnimation(screen),
            COST,
            HEALTH,
            COOLDOWN);
    }

    private static Animation<TextureRegion> createAnimation (GameScreen screen) {
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.PLANTS_ATLAS);
        Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(AssetPaths.REGION_CHERRY_BOMB);
        if (frames == null || frames.size == 0) {
            throw new IllegalStateException("Cherry Bomb frames missing from plants atlas");
        }
        return new Animation<>(0.08f, frames, Animation.PlayMode.NORMAL);
    }

    @Override
    public void action (float delta) {
        if (exploded) {
            return;
        }
        fuseTimer += delta;
        if (fuseTimer >= FUSE_TIME) {
            explode();
        }
    }

    private void explode () {
        if (exploded) {
            return;
        }
        exploded = true;
        float centerX = position.x + bounds.width / 2f;
        float centerY = position.y + bounds.height / 2f;
        float diameter = EXPLOSION_RADIUS * 2f;
        explosionArea.set(centerX - EXPLOSION_RADIUS, centerY - EXPLOSION_RADIUS, diameter, diameter);

        for (BaseZombie zombie : screen.getZombies()) {
            if (!zombie.isAlive()) {
                continue;
            }
            if (zombie.getCollisionBounds().overlaps(explosionArea)) {
                zombie.takeExplosionDamage(EXPLOSION_DAMAGE);
            }
        }

        TextureAtlas uiAtlas = screen.getAssets().getAtlas(AssetPaths.UI_ATLAS);
        if (uiAtlas != null) {
            screen.addBoom(new Boom(uiAtlas, centerX, centerY, BOOM_SCALE));
        }

        alive = false;
    }

    @Override
    public void draw (SpriteBatch batch) {
        if (!alive) {
            return;
        }
        TextureRegion frame = animation.getKeyFrame(stateTime, false);
        batch.draw(frame, position.x, position.y);
    }
}
