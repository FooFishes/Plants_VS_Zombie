package work.foofish.pvz.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import work.foofish.pvz.entities.zombies.BaseZombie;

import java.util.List;

/**
 * 简易小推车（割草机）实现：
 * - 默认停在对应行最左侧，等待僵尸触发
 * - 被触发后向右高速行驶，接触到的僵尸将被碾碎
 * - 超出地图范围后即失效
 */
public class LawnMower {
    private static final float DEFAULT_WIDTH = 60f;
    private static final float DEFAULT_HEIGHT = 60f;
    private static final float TRAVEL_SPEED = 360f;

    private final TextureRegion region;
    private final Vector2 position;
    private final Rectangle bounds;
    private final int row;
    private final float worldWidth;

    private boolean triggered;
    private boolean spent;

    public LawnMower (TextureRegion region, float x, float y, int row, float worldWidth) {
        this.region = region;
        this.row = row;
        this.worldWidth = worldWidth;
        float width = region != null ? region.getRegionWidth() : DEFAULT_WIDTH;
        float height = region != null ? region.getRegionHeight() : DEFAULT_HEIGHT;
        this.position = new Vector2(x, y);
        this.bounds = new Rectangle(x, y, width, height);
    }

    public void update (float delta, List<BaseZombie> zombies) {
        if (spent) {
            return;
        }

        if (!triggered && checkAndCrushZombies(zombies)) {
            triggered = true;
        }

        if (triggered) {
            position.x += TRAVEL_SPEED * delta;
            bounds.setPosition(position.x, position.y);
            checkAndCrushZombies(zombies);
            if (position.x > worldWidth) {
                spent = true;
            }
        }
    }

    private boolean checkAndCrushZombies (List<BaseZombie> zombies) {
        boolean hit = false;
        if (zombies == null) {
            return false;
        }
        for (BaseZombie zombie : zombies) {
            if (zombie == null || !zombie.isAlive() || zombie.getRow() != row) {
                continue;
            }
            if (bounds.overlaps(zombie.getCollisionBounds())) {
                zombie.takeDamage(Integer.MAX_VALUE);
                hit = true;
            }
        }
        return hit;
    }

    public void draw (SpriteBatch batch) {
        if (spent || region == null) {
            return;
        }
        batch.draw(region, position.x, position.y, bounds.width, bounds.height);
    }

    public boolean isSpent () {
        return spent;
    }
}
