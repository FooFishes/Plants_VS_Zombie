package work.foofish.pvz.entities.zombies;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.utils.AssetPaths;

public class ConeheadZombie extends BaseZombie {
    private static final int BODY_HEALTH = 270;
    private static final int CONE_HEALTH = 370;
    private static final int TOTAL_HEALTH = BODY_HEALTH + CONE_HEALTH;
    private static final float WALK_SPEED = 19f;
    private static final int BITE_DAMAGE = 4;
    private static final float ATTACK_INTERVAL = .04f;
    private static final float DRAW_SCALE = 0.9f;
    private static final float EXTRA_LOST_HEAD_OFFSET = 25f;
    private static final float DEFAULT_REFERENCE_WIDTH = 75f;
    private static final float DEFAULT_REFERENCE_HEIGHT = 100f;

    private final Animation<TextureRegion> coneWalkAnimation;
    private final Animation<TextureRegion> coneAttackAnimation;
    private final Animation<TextureRegion> normalWalkAnimation;
    private final Animation<TextureRegion> normalAttackAnimation;
    private final Animation<TextureRegion> lostHeadWalkAnimation;
    private final Animation<TextureRegion> lostHeadAttackAnimation;
    private final Animation<TextureRegion> dieAnimation;
    private final Animation<TextureRegion> headAnimation;
    private final Animation<TextureRegion> boomDieAnimation;

    private final float referenceWidth;
    private final float coneWalkOffset;
    private final float coneAttackOffset;
    private final float normalWalkOffset;
    private final float normalAttackOffset;
    private final float lostHeadWalkOffset;
    private final float lostHeadAttackOffset;

    private boolean coneIntact = true;
    private boolean headAnimationActive;
    private float headAnimationTime;
    private final Vector2 headPosition = new Vector2();

    public ConeheadZombie (GameScreen screen, float x, float y, int row) {
        this(screen, x, y, row, fetchReferenceFrame(screen));
    }

    private ConeheadZombie (GameScreen screen, float x, float y, int row, TextureRegion referenceFrame) {
        super(screen, x, y, row, TOTAL_HEALTH,
            ZombieAnimationHelper.getReferenceWidth(referenceFrame, DEFAULT_REFERENCE_WIDTH) * DRAW_SCALE,
            ZombieAnimationHelper.getReferenceHeight(referenceFrame, DEFAULT_REFERENCE_HEIGHT) * DRAW_SCALE);
        this.referenceWidth = ZombieAnimationHelper.getReferenceWidth(referenceFrame, DEFAULT_REFERENCE_WIDTH) * DRAW_SCALE;
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.ZOMBIES_ATLAS);

        this.coneWalkAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_CONEHEAD_ZOMBIE_WALK,
            0.09f,
            Animation.PlayMode.LOOP,
            null,
            referenceFrame
        );
        this.coneAttackAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_CONEHEAD_ZOMBIE_ATTACK,
            0.09f,
            Animation.PlayMode.LOOP,
            coneWalkAnimation,
            referenceFrame
        );
        TextureRegion normalReference = fetchNormalReferenceFrame(atlas);
        this.normalWalkAnimation = ZombieAnimationHelper.ensureAnimation(
            atlas,
            AssetPaths.REGION_NORMAL_ZOMBIE_WALK,
            0.09f,
            Animation.PlayMode.LOOP,
            coneWalkAnimation,
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

        this.coneWalkOffset = ZombieAnimationHelper.computeCenteringOffset(referenceWidth, coneWalkAnimation, DRAW_SCALE);
        this.coneAttackOffset = ZombieAnimationHelper.computeCenteringOffset(referenceWidth, coneAttackAnimation, DRAW_SCALE);
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
        Array<TextureAtlas.AtlasRegion> coneFrames = atlas.findRegions(AssetPaths.REGION_CONEHEAD_ZOMBIE_WALK);
        if (coneFrames != null && coneFrames.size > 0) {
            return coneFrames.first();
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
        checkConeState(previousHealth);
    }

    @Override
    public void takeExplosionDamage (int damage) {
        int previousHealth = health;
        super.takeExplosionDamage(damage);
        checkConeState(previousHealth);
    }

    private void checkConeState (int previousHealth) {
        if (!coneIntact) {
            return;
        }
        if (previousHealth > BODY_HEALTH && health <= BODY_HEALTH) {
            coneIntact = false;
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
            return coneIntact ? coneAttackOffset : normalAttackOffset;
        }
        return coneIntact ? coneWalkOffset : normalWalkOffset;
    }

    @Override
    protected float getDrawScale () {
        return DRAW_SCALE;
    }

    @Override
    protected Animation<TextureRegion> getAnimation (ZombieState state) {
        switch (state) {
            case ATTACK:
                return coneIntact ? pickAnimation(coneAttackAnimation, normalAttackAnimation) : normalAttackAnimation;
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
                return coneIntact ? pickAnimation(coneWalkAnimation, normalWalkAnimation) : normalWalkAnimation;
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
