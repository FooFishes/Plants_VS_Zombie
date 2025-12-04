package work.foofish.pvz.entities.zombies;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.utils.AssetPaths;

/**
 * 铁桶僵尸，除去铁桶后会以普通僵尸状态继续行动。
 */
public class BucketheadZombie extends BaseZombie {
    private static final int BODY_HEALTH = 270;
    private static final int BUCKET_HEALTH = 1100;
    private static final int TOTAL_HEALTH = BODY_HEALTH + BUCKET_HEALTH;
    private static final float WALK_SPEED = 19f;
    private static final int BITE_DAMAGE = 4;
    private static final float ATTACK_INTERVAL = .04f;
    private static final float DRAW_SCALE = 0.9f;
    private static final float EXTRA_LOST_HEAD_OFFSET = 25f;
    private static final float DEFAULT_REFERENCE_WIDTH = 75f;
    private static final float DEFAULT_REFERENCE_HEIGHT = 100f;

    private final Animation<TextureRegion> bucketWalkAnimation;
    private final Animation<TextureRegion> bucketAttackAnimation;
    private final Animation<TextureRegion> normalWalkAnimation;
    private final Animation<TextureRegion> normalAttackAnimation;
    private final Animation<TextureRegion> lostHeadWalkAnimation;
    private final Animation<TextureRegion> lostHeadAttackAnimation;
    private final Animation<TextureRegion> dieAnimation;
    private final Animation<TextureRegion> headAnimation;
    private final Animation<TextureRegion> boomDieAnimation;

    private final float referenceWidth;
    private final float bucketWalkOffset;
    private final float bucketAttackOffset;
    private final float normalWalkOffset;
    private final float normalAttackOffset;
    private final float lostHeadWalkOffset;
    private final float lostHeadAttackOffset;

    private boolean bucketIntact = true;
    private boolean headAnimationActive;
    private float headAnimationTime;
    private final Vector2 headPosition = new Vector2();

    public BucketheadZombie (GameScreen screen, float x, float y, int row) {
        this(screen, x, y, row, fetchReferenceFrame(screen));
    }

    private BucketheadZombie (GameScreen screen, float x, float y, int row, TextureRegion referenceFrame) {
        super(screen, x, y, row, TOTAL_HEALTH,
            ZombieAnimationHelper.getReferenceWidth(referenceFrame, DEFAULT_REFERENCE_WIDTH) * DRAW_SCALE,
            ZombieAnimationHelper.getReferenceHeight(referenceFrame, DEFAULT_REFERENCE_HEIGHT) * DRAW_SCALE);
        this.referenceWidth = ZombieAnimationHelper.getReferenceWidth(referenceFrame, DEFAULT_REFERENCE_WIDTH) * DRAW_SCALE;
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.ZOMBIES_ATLAS);

        this.bucketWalkAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_BUCKETHEAD_ZOMBIE_WALK,
            0.09f,
            Animation.PlayMode.LOOP,
            null,
            referenceFrame
        );
        this.bucketAttackAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_BUCKETHEAD_ZOMBIE_ATTACK,
            0.09f,
            Animation.PlayMode.LOOP,
            bucketWalkAnimation,
            referenceFrame
        );
        TextureRegion normalReference = fetchNormalReferenceFrame(atlas);
        this.normalWalkAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_WALK,
            0.09f,
            Animation.PlayMode.LOOP,
            bucketWalkAnimation,
            normalReference
        );
        this.normalAttackAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ATTACK,
            0.09f,
            Animation.PlayMode.LOOP,
            normalWalkAnimation,
            normalReference
        );
        this.lostHeadWalkAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_LOST_HEAD,
            0.09f,
            Animation.PlayMode.LOOP,
            normalWalkAnimation,
            normalReference
        );
        this.lostHeadAttackAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_LOST_HEAD_ATTACK,
            0.09f,
            Animation.PlayMode.NORMAL,
            lostHeadWalkAnimation,
            normalReference
        );
        this.dieAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_DIE,
            0.08f,
            Animation.PlayMode.NORMAL,
            normalWalkAnimation,
            normalReference
        );
        this.headAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_HEAD,
            0.08f,
            Animation.PlayMode.NORMAL,
            null,
            null
        );
        this.boomDieAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_BOOM_DIE,
            0.08f,
            Animation.PlayMode.NORMAL,
            dieAnimation,
            normalReference
        );

        this.bucketWalkOffset = ZombieAnimationHelper.computeCenteringOffset(referenceWidth, bucketWalkAnimation, DRAW_SCALE);
        this.bucketAttackOffset = ZombieAnimationHelper.computeCenteringOffset(referenceWidth, bucketAttackAnimation, DRAW_SCALE);
        this.normalWalkOffset = ZombieAnimationHelper.computeCenteringOffset(referenceWidth, normalWalkAnimation, DRAW_SCALE);
        this.normalAttackOffset = ZombieAnimationHelper.computeCenteringOffset(referenceWidth, normalAttackAnimation, DRAW_SCALE);
        this.lostHeadWalkOffset = ZombieAnimationHelper.computeCenteringOffset(referenceWidth, lostHeadWalkAnimation, DRAW_SCALE);
        this.lostHeadAttackOffset = ZombieAnimationHelper.computeCenteringOffset(referenceWidth, lostHeadAttackAnimation, DRAW_SCALE);
    }

    private static TextureRegion fetchReferenceFrame (GameScreen screen) {
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.ZOMBIES_ATLAS);
        if (atlas == null) {
            return null;
        }
        Array<TextureAtlas.AtlasRegion> bucketFrames = atlas.findRegions(AssetPaths.REGION_BUCKETHEAD_ZOMBIE_WALK);
        if (bucketFrames != null && bucketFrames.size > 0) {
            return bucketFrames.first();
        }
        return fetchNormalReferenceFrame(atlas);
    }

    private static TextureRegion fetchNormalReferenceFrame (TextureAtlas atlas) {
        if (atlas == null) {
            return null;
        }
        Array<TextureAtlas.AtlasRegion> normalFrames = atlas.findRegions(AssetPaths.REGION_NORMAL_ZOMBIE_WALK);
        if (normalFrames != null && normalFrames.size > 0) {
            return normalFrames.first();
        }
        return atlas.findRegion(AssetPaths.REGION_NORMAL_ZOMBIE_WALK);
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
    protected int getLostHeadThreshold () {
        return BODY_HEALTH / 2;
    }

    @Override
    public void takeDamage (int damage) {
        int previousHealth = health;
        super.takeDamage(damage);
        checkBucketState(previousHealth);
    }

    @Override
    public void takeExplosionDamage (int damage) {
        int previousHealth = health;
        super.takeExplosionDamage(damage);
        checkBucketState(previousHealth);
    }

    private void checkBucketState (int previousHealth) {
        if (!bucketIntact) {
            return;
        }
        if (previousHealth > BODY_HEALTH && health <= BODY_HEALTH) {
            bucketIntact = false;
            if (state == ZombieState.WALK || state == ZombieState.ATTACK) {
                stateTime = 0f;
            }
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
        if (newState == ZombieState.LOST_HEAD || newState == ZombieState.LOST_HEAD_ATTACK) {
            startHeadAnimation();
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
        if (state == ZombieState.ATTACK) {
            return bucketIntact ? bucketAttackOffset : normalAttackOffset;
        }
        return bucketIntact ? bucketWalkOffset : normalWalkOffset;
    }

    @Override
    protected float getDrawScale () {
        return DRAW_SCALE;
    }

    @Override
    protected Animation<TextureRegion> getAnimation (ZombieState state) {
        switch (state) {
            case ATTACK:
                return bucketIntact ? pickAnimation(bucketAttackAnimation, normalAttackAnimation) : normalAttackAnimation;
            case LOST_HEAD:
                return lostHeadWalkAnimation;
            case LOST_HEAD_ATTACK:
                return lostHeadAttackAnimation;
            case DIE:
                return dieAnimation;
            case BOOM_DIE:
                return boomDieAnimation != null ? boomDieAnimation : dieAnimation;
            case WALK:
            default:
                return bucketIntact ? pickAnimation(bucketWalkAnimation, normalWalkAnimation) : normalWalkAnimation;
        }
    }

    private Animation<TextureRegion> pickAnimation (Animation<TextureRegion> primary, Animation<TextureRegion> fallback) {
        if (primary != null) {
            return primary;
        }
        return fallback;
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
}
