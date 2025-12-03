package work.foofish.pvz.entities.bullets;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
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

    private float stateTime;
    private boolean exploding;
    private boolean alive = true;

    public PeaBullet (GameScreen screen, float x, float y, int row, float speed, int damage) {
        this.screen = screen;
        this.position = new Vector2(x, y);
        this.row = row;
        this.speed = speed;
        this.damage = damage;

        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.BULLETS_ATLAS);
        this.flyAnimation = new Animation<>(0.05f, atlas.findRegions(AssetPaths.REGION_PEA_NORMAL), Animation.PlayMode.LOOP);
        this.explodeAnimation = new Animation<>(0.04f, atlas.findRegions(AssetPaths.REGION_PEA_NORMAL_EXPLODE), Animation.PlayMode.NORMAL);
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
        batch.draw(frame, position.x, position.y);
    }

    public boolean isAlive () {
        return alive;
    }
}
