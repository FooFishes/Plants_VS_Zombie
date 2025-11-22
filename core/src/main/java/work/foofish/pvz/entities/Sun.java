package work.foofish.pvz.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.utils.AssetPaths;

public class Sun {
    private static final float LIFETIME = 10f; // Sun disappears after 10 seconds
    private static final float FALL_SPEED = 50f; // Pixels per second
    private static final float TARGET_Y = 100f; // Stop falling at this Y

    private final GameScreen screen;
    private final Vector2 position;
    private final Rectangle bounds;
    private final TextureRegion region;
    private float stateTime;
    private boolean active;
    private boolean isFalling;

    public Sun(GameScreen screen, float x, float y) {
        this.screen = screen;
        this.position = new Vector2(x, y);
        this.active = true;
        this.stateTime = 0f;
        this.isFalling = false; // Default to not falling (produced by sunflower)

        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.UI_ATLAS);
        this.region = atlas.findRegion(AssetPaths.REGION_SUN);

        if (this.region != null) {
            this.bounds = new Rectangle(x, y, region.getRegionWidth(), region.getRegionHeight());
        } else {
            this.bounds = new Rectangle(x, y, 50, 50); // Fallback size
        }
    }

    public void update(float delta) {
        stateTime += delta;
        if (stateTime >= LIFETIME) {
            active = false;
        }

        if (isFalling && position.y > TARGET_Y) {
            position.y -= FALL_SPEED * delta;
            bounds.setPosition(position);
        }
    }

    public void draw(SpriteBatch batch) {
        if (active && region != null) {
            batch.draw(region, position.x, position.y);
        }
    }

    public void collect() {
        active = false;
        // TODO: Add logic to increase player's sun currency
        System.out.println("Sun collected!");
    }

    public boolean isActive() {
        return active;
    }

    public Rectangle getBounds() {
        return bounds;
    }
}
