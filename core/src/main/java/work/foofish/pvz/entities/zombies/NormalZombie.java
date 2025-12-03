package work.foofish.pvz.entities.zombies;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.utils.AssetPaths;

public class NormalZombie extends BaseZombie {
    private static final int HEALTH = 270;
    private static final float WALK_SPEED = 19f;
    private static final int BITE_DAMAGE = 20;
    private static final float ATTACK_INTERVAL = 1.0f;
    private static final float DRAW_SCALE = 0.9f;

    private final Animation<TextureRegion> walkAnimation;
    private final Animation<TextureRegion> attackAnimation;
    private final Animation<TextureRegion> lostHeadWalkAnimation;
    private final Animation<TextureRegion> lostHeadAttackAnimation;
    private final Animation<TextureRegion> dieAnimation;
    private final Animation<TextureRegion> headAnimation;
    private boolean headAnimationActive;
    private float headAnimationTime;
    private final Vector2 headPosition = new Vector2();
    private final float referenceWidth;
    private final float lostHeadWalkOffset;
    private final float lostHeadAttackOffset;
    private static final float EXTRA_LOST_HEAD_OFFSET = 25f;

    public NormalZombie (GameScreen screen, float x, float y, int row) {
        this(screen, x, y, row, fetchReferenceFrame(screen));
    }

    private NormalZombie (GameScreen screen, float x, float y, int row, TextureRegion referenceFrame) {
        super(screen, x, y, row, HEALTH,
            getReferenceWidth(referenceFrame) * DRAW_SCALE,
            getReferenceHeight(referenceFrame) * DRAW_SCALE);
        this.referenceWidth = getReferenceWidth(referenceFrame) * DRAW_SCALE;
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.ZOMBIES_ATLAS);
        this.walkAnimation = new Animation<>(0.09f, atlas.findRegions(AssetPaths.REGION_NORMAL_ZOMBIE_WALK), Animation.PlayMode.LOOP);
        this.attackAnimation = new Animation<>(0.09f, atlas.findRegions(AssetPaths.REGION_NORMAL_ATTACK), Animation.PlayMode.LOOP);
        this.lostHeadWalkAnimation = new Animation<>(0.09f, atlas.findRegions(AssetPaths.REGION_NORMAL_ZOMBIE_LOST_HEAD), Animation.PlayMode.LOOP);
        this.lostHeadAttackAnimation = new Animation<>(0.09f, atlas.findRegions(AssetPaths.REGION_NORMAL_ZOMBIE_LOST_HEAD_ATTACK), Animation.PlayMode.NORMAL);
        this.dieAnimation = new Animation<>(0.08f, atlas.findRegions(AssetPaths.REGION_NORMAL_ZOMBIE_DIE), Animation.PlayMode.NORMAL);
        this.headAnimation = new Animation<>(0.08f, atlas.findRegions(AssetPaths.REGION_NORMAL_ZOMBIE_HEAD), Animation.PlayMode.NORMAL);
        this.lostHeadWalkOffset = computeCenteringOffset(referenceWidth, lostHeadWalkAnimation, DRAW_SCALE);
        this.lostHeadAttackOffset = computeCenteringOffset(referenceWidth, lostHeadAttackAnimation, DRAW_SCALE);
    }

    private static TextureRegion fetchReferenceFrame (GameScreen screen) {
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.ZOMBIES_ATLAS);
        if (atlas == null) {
            return null;
        }
        return atlas.findRegions(AssetPaths.REGION_NORMAL_ZOMBIE_WALK).first();
    }

    @Override
    protected float getWalkSpeed () {
        return WALK_SPEED;
    }

    @Override
    protected int getBiteDamage () {
        return BITE_DAMAGE;
    }

    @Override
    protected float getAttackInterval () {
        return ATTACK_INTERVAL;
    }

    @Override
    protected Animation<TextureRegion> getAnimation (ZombieState state) {
        switch (state) {
            case ATTACK:
                return attackAnimation;
            case LOST_HEAD:
                return lostHeadWalkAnimation;
            case LOST_HEAD_ATTACK:
                return lostHeadAttackAnimation;
            case DIE:
                return dieAnimation;
            case WALK:
            default:
                return walkAnimation;
        }
    }

    @Override
    public void update (float delta) {
        super.update(delta);
        if (headAnimationActive && headAnimation != null) {
            headAnimationTime += delta;
            if (headAnimation.isAnimationFinished(headAnimationTime)) {
                headAnimationActive = false;
            }
        }
    }

    @Override
    protected float getDrawOffsetX () {
        if (state == ZombieState.LOST_HEAD) {
            return lostHeadWalkOffset + EXTRA_LOST_HEAD_OFFSET;
        }
        if (state == ZombieState.LOST_HEAD_ATTACK) {
            return lostHeadAttackOffset + EXTRA_LOST_HEAD_OFFSET;
        }
        return 0f;
    }

    @Override
    protected float getDrawScale () {
        return DRAW_SCALE;
    }

    @Override
    public void draw (SpriteBatch batch) {
        super.draw(batch);
        if (headAnimationActive && headAnimation != null) {
            TextureRegion frame = headAnimation.getKeyFrame(headAnimationTime, false);
            float width = frame.getRegionWidth() * DRAW_SCALE;
            float height = frame.getRegionHeight() * DRAW_SCALE;
            batch.draw(frame, headPosition.x, headPosition.y, width, height);
        }
    }

    @Override
    protected void onStateChanged (ZombieState newState) {
        super.onStateChanged(newState);
        // 在僵尸刚掉头时播放头部掉落动画
        if (newState == ZombieState.LOST_HEAD || newState == ZombieState.LOST_HEAD_ATTACK) {
            startHeadAnimation();
        }
    }

    private void startHeadAnimation () {
        if (headAnimation == null) {
            return;
        }
        headAnimationActive = true;
        headAnimationTime = 0f;
        TextureRegion frame = headAnimation.getKeyFrame(0f, false);
        float scaledWidth = frame.getRegionWidth() * DRAW_SCALE;
        float offsetX = (bounds.width - scaledWidth) / 2f;
        headPosition.set(position.x + offsetX, position.y);
    }

    private static float computeCenteringOffset (float referenceWidth, Animation<TextureRegion> animation, float scale) {
        if (animation == null || animation.getKeyFrames().length == 0) {
            return 0f;
        }
        TextureRegion firstFrame = animation.getKeyFrames()[0];
        float scaledFrameWidth = firstFrame.getRegionWidth() * scale;
        return (referenceWidth - scaledFrameWidth) / 2f;
    }

    private static float getReferenceWidth (TextureRegion referenceFrame) {
        return referenceFrame != null ? referenceFrame.getRegionWidth() : 70f;
    }

    private static float getReferenceHeight (TextureRegion referenceFrame) {
        return referenceFrame != null ? referenceFrame.getRegionHeight() : 90f;
    }
}
