package work.foofish.pvz.entities.plants;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import work.foofish.pvz.screens.GameScreen;

public abstract class BasePlant {
    protected final GameScreen screen;
    protected final Vector2 position;
    protected final Rectangle bounds;
    protected final Animation<TextureRegion> animation;

    protected float stateTime;
    protected int health;
    protected final int cost;
    protected boolean alive;
    protected final float cooldown;

    protected BasePlant (GameScreen screen, float x, float y, Rectangle bounds, Animation<TextureRegion> animation, int cost, float cooldown) {
        this.screen = screen;
        this.cooldown = cooldown;
        this.position = new Vector2(x, y);
        this.animation = animation;
        this.cost = cost;
        this.stateTime = 0f;
        TextureRegion firstFrame = animation.getKeyFrame(0);
        this.bounds = new Rectangle(x, y, firstFrame.getRegionWidth(), firstFrame.getRegionHeight());
    }

    public void update (float delta) {
        stateTime += delta;
    }

    public void draw (SpriteBatch batch) {
        batch.draw(animation.getKeyFrame(stateTime, true), position.x, position.y);
    }

    public void takeDamage (int damage) {
        this.health -= damage;
        if (health <= 0) {
            alive = false;
        }
    }

    public boolean isAlive () {
        return alive;
    }

    public Vector2 getPosition () {
        return position;
    }

    public Rectangle getBounds () {
        return bounds;
    }

    public int cost () {
        return cost;
    }
}
