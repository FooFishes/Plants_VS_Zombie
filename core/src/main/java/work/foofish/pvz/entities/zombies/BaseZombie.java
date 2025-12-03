package work.foofish.pvz.entities.zombies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;
import work.foofish.pvz.entities.plants.BasePlant;
import work.foofish.pvz.screens.GameScreen;

public abstract class BaseZombie {
    protected final GameScreen screen;
    protected final Vector2 position;
    protected final Rectangle bounds;
    protected final int row;
    protected final int maxHealth;
    protected int health;
    protected ZombieState state = ZombieState.WALK;
    protected boolean lostHead;
    protected BasePlant targetPlant;
    protected float stateTime;
    protected float attackTimer;
    private boolean alive = true;
    private final Rectangle collisionBounds;
    private static final float CENTER_COLLISION_WIDTH = 10f;
    private static final float LOST_HEAD_DURATION = 1.0f; // 掉头后存活1秒
    private float lostHeadTimer = 0f; // 掉头后的计时器
    private static final Color SLOW_OVERLAY_COLOR = new Color(0.1f, 0.45f, 0.95f, 0.7f);
    private float speedMultiplier = 1f;
    private float slowTimer = 0f;
    private float slowDuration = 0f;

    protected BaseZombie (GameScreen screen, float x, float y, int row, int maxHealth, float width, float height) {
        this.screen = screen;
        this.position = new Vector2(x, y);
        this.row = row;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.bounds = new Rectangle(position.x, position.y, width, height);
        this.collisionBounds = new Rectangle(bounds);
    }

    public void update (float delta) {
        if (!alive) return;
        stateTime += delta;
        bounds.setPosition(position.x, position.y);
        updateCollisionBounds();

        // 检查掉头后的生存时间
        if (lostHead) {
            lostHeadTimer += delta;
            if (lostHeadTimer >= LOST_HEAD_DURATION) {
                // 掉头1秒后自动死亡
                if (state != ZombieState.DIE && state != ZombieState.DEAD) {
                    health = 0;
                    switchTo(ZombieState.DIE);
                }
            }
        }

        updateSlowState(delta);

        switch (state) {
            case WALK:
            case LOST_HEAD:
                moveForward(delta);
                break;
            case ATTACK:
            case LOST_HEAD_ATTACK:
                attack(delta);
                break;
            case DIE:
                if (isCurrentAnimationFinished()) {
                    alive = false;
                    state = ZombieState.DEAD;
                }
                break;
            case DEAD:
                alive = false;
                break;
        }

        if (position.x + bounds.width < 0f) {
            alive = false;
        }
    }

    private void moveForward (float delta) {
        position.x -= getWalkSpeed() * getMovementMultiplier() * delta;
        BasePlant plant = screen.findPlantInRow(row, collisionBounds);
        if (plant != null) {
            targetPlant = plant;
            switchTo(lostHead ? ZombieState.LOST_HEAD_ATTACK : ZombieState.ATTACK);
        }
    }

    private void attack (float delta) {
        attackTimer += delta;
        if (targetPlant == null || !targetPlant.isAlive() || !targetPlant.getBounds().overlaps(collisionBounds)) {
            targetPlant = null;
            switchTo(lostHead ? ZombieState.LOST_HEAD : ZombieState.WALK);
            return;
        }

        float effectiveInterval = getAttackInterval();
        if (speedMultiplier < 1f) {
            effectiveInterval /= Math.max(speedMultiplier, 0.01f);
        }
        if (attackTimer >= effectiveInterval) {
            attackTimer = 0f;
            applyAttack(targetPlant);
        }
    }

    protected void applyAttack (BasePlant target) {
        target.takeDamage(getBiteDamage());
    }

    public void takeDamage (int damage) {
        if (!alive || state == ZombieState.DEAD) return;
        health -= damage;
        if (health <= 0) {
            health = 0;
            switchTo(ZombieState.DIE);
        } else if (!lostHead && health <= getLostHeadThreshold()) {
            lostHead = true;
            if (state == ZombieState.WALK) {
                switchTo(ZombieState.LOST_HEAD);
            } else if (state == ZombieState.ATTACK) {
                switchTo(ZombieState.LOST_HEAD_ATTACK);
            }
        }
    }

    protected int getLostHeadThreshold () {
        return maxHealth / 2;
    }

    protected void switchTo (ZombieState newState) {
        if (state == newState) return;
        state = newState;
        stateTime = 0f;
        onStateChanged(newState);
        updateCollisionBounds();
    }

    protected void onStateChanged (ZombieState newState) {
        // 留给子类扩展特殊效果
    }

    public void draw (SpriteBatch batch) {
        if (!alive && state != ZombieState.DIE) return;
        if (state == ZombieState.DEAD) return;
        Animation<TextureRegion> animation = getAnimation(state);
        TextureRegion frame = animation.getKeyFrame(stateTime, isLooping(state));
        float scale = getDrawScale();
        float width = frame.getRegionWidth() * scale;
        float height = frame.getRegionHeight() * scale;
        float drawX = position.x + getDrawOffsetX();
        batch.draw(frame, drawX, position.y, width, height);
        if (isSlowed()) {
            Color currentColor = batch.getColor();
            float originalR = currentColor.r;
            float originalG = currentColor.g;
            float originalB = currentColor.b;
            float originalA = currentColor.a;
            batch.setColor(SLOW_OVERLAY_COLOR);
            batch.draw(frame, drawX, position.y, width, height);
            batch.setColor(originalR, originalG, originalB, originalA);
        }
    }

    /**
     * 获取绘制时的X轴偏移量，子类可以重写此方法来修正不同状态下的视觉位置
     *
     * @return X轴偏移量（像素）
     */
    protected float getDrawOffsetX () {
        return 0f;
    }

    /**
     * @return 绘制缩放系数，默认1表示原始大小
     */
    protected float getDrawScale () {
        return 1f;
    }

    private boolean isLooping (ZombieState currentState) {
        return currentState != ZombieState.DIE && currentState != ZombieState.LOST_HEAD_ATTACK;
    }

    private boolean isCurrentAnimationFinished () {
        Animation<TextureRegion> animation = getAnimation(state);
        if (animation.getPlayMode() == Animation.PlayMode.LOOP || animation.getPlayMode() == Animation.PlayMode.LOOP_PINGPONG) {
            return false;
        }
        return animation.isAnimationFinished(stateTime);
    }

    public Rectangle getBounds () {
        return bounds;
    }

    public Rectangle getCollisionBounds () {
        return collisionBounds;
    }

    public int getRow () {
        return row;
    }

    public boolean isAlive () {
        return alive;
    }

    public void applySlow (float multiplier, float duration) {
        if (duration <= 0f) return;
        multiplier = MathUtils.clamp(multiplier, 0.1f, 1f);
        if (speedMultiplier == 1f || multiplier < speedMultiplier || slowTimer <= 0f) {
            speedMultiplier = multiplier;
            slowDuration = duration;
        } else if (multiplier == speedMultiplier) {
            slowDuration = Math.max(slowDuration, duration);
        }
        slowTimer = Math.max(slowTimer, duration);
    }

    private boolean isSlowed () {
        return slowTimer > 0f && speedMultiplier < 1f;
    }

    private float getMovementMultiplier () {
        return speedMultiplier;
    }

    private void updateSlowState (float delta) {
        if (slowTimer <= 0f) {
            slowTimer = 0f;
            if (speedMultiplier != 1f) {
                speedMultiplier = 1f;
                slowDuration = 0f;
            }
            return;
        }
        slowTimer -= delta;
        if (slowTimer <= 0f) {
            slowTimer = 0f;
            speedMultiplier = 1f;
            slowDuration = 0f;
        }
    }

    protected abstract float getWalkSpeed ();

    protected abstract int getBiteDamage ();

    protected abstract float getAttackInterval ();

    protected abstract Animation<TextureRegion> getAnimation (ZombieState state);

    private void updateCollisionBounds () {
        if (requiresCenterDetection()) {
            float centerX = bounds.x + bounds.width / 2f;
            float width = Math.min(bounds.width, CENTER_COLLISION_WIDTH);
            collisionBounds.set(centerX - width / 2f, bounds.y, width, bounds.height);
        } else {
            collisionBounds.set(bounds);
        }
    }

    private boolean requiresCenterDetection () {
        return state == ZombieState.WALK || state == ZombieState.ATTACK
            || state == ZombieState.LOST_HEAD || state == ZombieState.LOST_HEAD_ATTACK;
    }

    public enum ZombieState {
        WALK,
        ATTACK,
        LOST_HEAD,
        LOST_HEAD_ATTACK,
        DIE,
        DEAD
    }
}
