package work.foofish.pvz.entities.zombies;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
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
    private final Animation<TextureRegion> boomDieAnimation;
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
        this.walkAnimation = ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_WALK,
            0.09f,
            Animation.PlayMode.LOOP,
            null,
            referenceFrame
        );
        this.attackAnimation = ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ATTACK,
            0.09f,
            Animation.PlayMode.LOOP,
            walkAnimation,
            referenceFrame
        );
        this.lostHeadWalkAnimation = ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_LOST_HEAD,
            0.09f,
            Animation.PlayMode.LOOP,
            walkAnimation,
            referenceFrame
        );
        this.lostHeadAttackAnimation = ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_LOST_HEAD_ATTACK,
            0.09f,
            Animation.PlayMode.NORMAL,
            attackAnimation,
            referenceFrame
        );
        this.dieAnimation = ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_DIE,
            0.08f,
            Animation.PlayMode.NORMAL,
            walkAnimation,
            referenceFrame
        );
        this.headAnimation = ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_HEAD,
            0.08f,
            Animation.PlayMode.NORMAL,
            null,
            null
        );
        this.boomDieAnimation = ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_BOOM_DIE,
            0.08f,
            Animation.PlayMode.NORMAL,
            dieAnimation,
            referenceFrame
        );
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
            case BOOM_DIE:
                return boomDieAnimation != null ? boomDieAnimation : dieAnimation;
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

    private static Animation<TextureRegion> ensureAnimation (TextureAtlas atlas,
                                                             String regionName,
                                                             float frameDuration,
                                                             Animation.PlayMode playMode,
                                                             Animation<TextureRegion> fallbackAnimation,
                                                             TextureRegion fallbackFrame) {
        Animation<TextureRegion> animation = createAnimation(atlas, regionName, frameDuration, playMode);
        if (animation != null) {
            return animation;
        }
        if (fallbackAnimation != null) {
            return fallbackAnimation;
        }
        if (fallbackFrame != null) {
            Animation<TextureRegion> single = new Animation<>(frameDuration, fallbackFrame);
            single.setPlayMode(playMode);
            return single;
        }
        return null;
    }

    private static Animation<TextureRegion> createAnimation (TextureAtlas atlas,
                                                             String regionName,
                                                             float frameDuration,
                                                             Animation.PlayMode playMode) {
        if (atlas == null) {
            return null;
        }
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(regionName);
        if (regions != null && regions.size > 0) {
            return new Animation<>(frameDuration, regions, playMode);
        }
        TextureAtlas.AtlasRegion singleRegion = atlas.findRegion(regionName);
        if (singleRegion != null) {
            Animation<TextureRegion> animation = new Animation<>(frameDuration, singleRegion);
            animation.setPlayMode(playMode);
            return animation;
        }
        return null;
    }

    private static float getReferenceWidth (TextureRegion referenceFrame) {
        return referenceFrame != null ? referenceFrame.getRegionWidth() : 70f;
    }

    private static float getReferenceHeight (TextureRegion referenceFrame) {
        return referenceFrame != null ? referenceFrame.getRegionHeight() : 90f;
    }
}
