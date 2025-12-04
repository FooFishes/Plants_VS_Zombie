package work.foofish.pvz.entities.plants;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.utils.AssetPaths;

public class Wallnut extends BasePlant {
    private static final int COST = 50;
    private static final int HEALTH = 4000;
    private static final float COOLDOWN = 30f;

    private enum DamageStage {
        NORMAL,
        CRACKED1,
        CRACKED2
    }

    private final Animation<TextureRegion> normalAnimation;
    private final Animation<TextureRegion> crackedStageOneAnimation;
    private final Animation<TextureRegion> crackedStageTwoAnimation;
    private Animation<TextureRegion> activeAnimation;
    private DamageStage stage = DamageStage.NORMAL;

    public Wallnut (GameScreen screen, float x, float y, int row, int col) {
        super(screen, x, y, row, col,
            createNormalAnimation(screen),
            COST,
            HEALTH,
            COOLDOWN);

        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.PLANTS_ATLAS);
        this.normalAnimation = this.animation;
        this.crackedStageOneAnimation = createStageAnimation(atlas, AssetPaths.REGION_WALLNUT_CRACKED1);
        this.crackedStageTwoAnimation = createStageAnimation(atlas, AssetPaths.REGION_WALLNUT_CRACKED2);
        this.activeAnimation = normalAnimation;
        updateBoundsToCurrentAnimation();
    }

    private static Animation<TextureRegion> createNormalAnimation (GameScreen screen) {
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.PLANTS_ATLAS);
        return new Animation<>(0.08f, atlas.findRegions(AssetPaths.REGION_WALLNUT_NORMAL), Animation.PlayMode.LOOP);
    }

    private static Animation<TextureRegion> createStageAnimation (TextureAtlas atlas, String regionName) {
        return new Animation<>(0.08f, atlas.findRegions(regionName), Animation.PlayMode.LOOP);
    }

    @Override
    public void action (float delta) {
        // Wallnut is purely defensive; no additional action needed per frame.
    }

    @Override
    public void draw (SpriteBatch batch) {
        if (!alive) {
            return;
        }
        if (activeAnimation == null) {
            return;
        }
        TextureRegion frame = activeAnimation.getKeyFrame(stateTime, true);
        batch.draw(frame, position.x, position.y);
    }

    @Override
    public void takeDamage (int damage) {
        if (!alive) {
            return;
        }
        super.takeDamage(damage);
        if (alive) {
            updateStage();
        }
    }

    private void updateStage () {
        float healthRatio = Math.max(0f, (float) health / HEALTH);
        DamageStage newStage;
        if (healthRatio > 2f / 3f) {
            newStage = DamageStage.NORMAL;
        } else if (healthRatio > 1f / 3f) {
            newStage = DamageStage.CRACKED1;
        } else {
            newStage = DamageStage.CRACKED2;
        }

        if (newStage == stage) {
            return;
        }

        stage = newStage;
        switch (stage) {
            case NORMAL:
                activeAnimation = normalAnimation;
                break;
            case CRACKED1:
                activeAnimation = crackedStageOneAnimation != null ? crackedStageOneAnimation : normalAnimation;
                break;
            case CRACKED2:
                activeAnimation = crackedStageTwoAnimation != null ? crackedStageTwoAnimation : crackedStageOneAnimation;
                if (activeAnimation == null) {
                    activeAnimation = normalAnimation;
                }
                break;
        }
        updateBoundsToCurrentAnimation();
    }

    private void updateBoundsToCurrentAnimation () {
        if (activeAnimation == null) {
            return;
        }
        TextureRegion frame = activeAnimation.getKeyFrame(0f);
        bounds.setSize(frame.getRegionWidth(), frame.getRegionHeight());
    }
}
