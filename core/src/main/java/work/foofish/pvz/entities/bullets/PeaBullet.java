package work.foofish.pvz.entities.bullets;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import work.foofish.pvz.entities.zombies.BaseZombie;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.utils.AssetPaths;

public class PeaBullet {
    private final GameScreen screen;
    private final Vector2 position;
    private final Rectangle bounds;
    private final Animation<TextureRegion> flyAnimation;
    private final Animation<TextureRegion> explodeAnimation;
    private final int row;
    private final float speed;
    private final int damage;
    private final Type type;
    private final boolean appliesSlow;
    private final float slowMultiplier;
    private final float slowDuration;
    private final float explodeScale;

    private float stateTime;
    private boolean exploding;
    private boolean alive = true;

    public PeaBullet (GameScreen screen, float x, float y, int row, float speed, int damage) {
        this(screen, x, y, row, speed, damage, Type.NORMAL);
    }

    public PeaBullet (GameScreen screen, float x, float y, int row, float speed, int damage, Type bulletType) {
        this.screen = screen;
        this.position = new Vector2(x, y);
        this.row = row;
        this.speed = speed;
        this.damage = damage;
        this.type = bulletType != null ? bulletType : Type.NORMAL;

        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.BULLETS_ATLAS);
        Array<TextureAtlas.AtlasRegion> flyRegions = atlas.findRegions(this.type.flyRegion);
        if (flyRegions == null || flyRegions.size == 0) {
            flyRegions = atlas.findRegions(AssetPaths.REGION_PEA_NORMAL);
        }
        Array<TextureAtlas.AtlasRegion> explodeRegions = atlas.findRegions(this.type.explodeRegion);
        if (explodeRegions == null || explodeRegions.size == 0) {
            explodeRegions = atlas.findRegions(AssetPaths.REGION_PEA_NORMAL_EXPLODE);
        }
        this.flyAnimation = new Animation<>(0.05f, flyRegions, Animation.PlayMode.LOOP);
        this.explodeAnimation = new Animation<>(0.04f, explodeRegions, Animation.PlayMode.NORMAL);
        this.appliesSlow = this.type.appliesSlow;
        this.slowMultiplier = this.type.slowMultiplier;
        this.slowDuration = this.type.slowDuration;
        this.explodeScale = this.type.explodeScale;
        TextureRegion firstFrame = flyAnimation.getKeyFrame(0);
        this.bounds = new Rectangle(x, y, firstFrame.getRegionWidth(), firstFrame.getRegionHeight());
    }

    public void update (float delta) {
        stateTime += delta;
        if (!exploding) {
            position.x += speed * delta;
            bounds.setPosition(position.x, position.y);
            if (position.x > screen.getWorldWidth()) {
                alive = false;
                return;
            }
            checkCollision();
        } else if (explodeAnimation.isAnimationFinished(stateTime)) {
            alive = false;
        }
    }

    private void checkCollision () {
        for (BaseZombie zombie : screen.getZombies()) {
            if (!zombie.isAlive() || zombie.getRow() != row) {
                continue;
            }
            if (bounds.overlaps(zombie.getCollisionBounds())) {
                zombie.takeDamage(damage);
                if (appliesSlow) {
                    zombie.applySlow(slowMultiplier, slowDuration);
                }
                explode();
                break;
            }
        }
    }

    private void explode () {
        if (exploding) return;
        exploding = true;
        stateTime = 0f;
    }

    public void draw (SpriteBatch batch) {
        if (!alive) return;
        Animation<TextureRegion> animation = exploding ? explodeAnimation : flyAnimation;
        TextureRegion frame = animation.getKeyFrame(stateTime, !exploding);
        float width = frame.getRegionWidth();
        float height = frame.getRegionHeight();
        float drawWidth = width;
        float drawHeight = height;
        float drawX = position.x;
        float drawY = position.y;
        if (exploding && explodeScale != 1f) {
            drawWidth = width * explodeScale;
            drawHeight = height * explodeScale;
            drawX += (width - drawWidth) / 2f;
            drawY += (height - drawHeight) / 2f;
        }
        batch.draw(frame, drawX, drawY, drawWidth, drawHeight);
    }

    public boolean isAlive () {
        return alive;
    }

    public enum Type {
        NORMAL(AssetPaths.REGION_PEA_NORMAL, AssetPaths.REGION_PEA_NORMAL_EXPLODE, false, 1f, 0f, 1f),
        SNOW(AssetPaths.REGION_PEA_SNOW, AssetPaths.REGION_PEA_SNOW_EXPLODE, true, 0.5f, 3.5f, 0.55f);

        final String flyRegion;
        final String explodeRegion;
        final boolean appliesSlow;
        final float slowMultiplier;
        final float slowDuration;
        final float explodeScale;

        Type (String flyRegion, String explodeRegion, boolean appliesSlow, float slowMultiplier, float slowDuration, float explodeScale) {
            this.flyRegion = flyRegion;
            this.explodeRegion = explodeRegion;
            this.appliesSlow = appliesSlow;
            this.slowMultiplier = slowMultiplier;
            this.slowDuration = slowDuration;
            this.explodeScale = explodeScale;
        }
    }
}
