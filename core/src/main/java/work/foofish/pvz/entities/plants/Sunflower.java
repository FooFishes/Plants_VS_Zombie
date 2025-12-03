package work.foofish.pvz.entities.plants;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import work.foofish.pvz.entities.Sun;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.utils.AssetPaths;

public class Sunflower extends BasePlant {
    private static final int COST = 50;
    private static final int HEALTH = 300;
    private static final float COOLDOWN = 7.5f;
    private static final float PRODUCTION_INTERVAL = 5f;
    private static final float ACTIVE_DURATION = 1.0f;

    private enum State {
        IDLE,       // 正常状态，等待产生阳光
        PRODUCING   // 激活状态，显示发光效果
    }

    private final Animation<TextureRegion> normalAnimation;
    private final Animation<TextureRegion> activeAnimation;

    private State currentState;
    private float productionTimer;
    private float stateTimer;

    public Sunflower (GameScreen screen, float x, float y, int row, int col) {
        super(screen, x, y, row, col,
            createNormalAnimation(screen),
            COST,
            HEALTH,
            COOLDOWN);

        this.normalAnimation = this.animation;
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.PLANTS_ATLAS);
        this.activeAnimation = new Animation<>(0.1f, atlas.findRegions(AssetPaths.REGION_SUNFLOWER_ACTIVE), Animation.PlayMode.LOOP);

        this.currentState = State.IDLE;
        this.productionTimer = 0f;
        this.stateTimer = 0f;
    }

    private static Animation<TextureRegion> createNormalAnimation (GameScreen screen) {
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.PLANTS_ATLAS);
        return new Animation<>(0.1f, atlas.findRegions(AssetPaths.REGION_SUNFLOWER_NORMAL), Animation.PlayMode.LOOP);
    }

    @Override
    public void action (float delta) {
        productionTimer += delta;

        switch (currentState) {
            case IDLE:
                updateIdleState(delta);
                break;
            case PRODUCING:
                updateProducingState(delta);
                break;
        }
    }

    private void updateIdleState (float delta) {
        if (productionTimer >= PRODUCTION_INTERVAL) {
            transitionToProducing();
        }
    }

    private void updateProducingState (float delta) {
        stateTimer += delta;
        if (stateTimer >= ACTIVE_DURATION) {
            transitionToIdle();
        }
    }

    private void transitionToProducing () {
        currentState = State.PRODUCING;
        stateTimer = 0f;
        productionTimer = 0f;
        produceSun();
    }

    private void transitionToIdle () {
        currentState = State.IDLE;
        stateTimer = 0f;
    }

    @Override
    public void draw (SpriteBatch batch) {
        if (!alive) return;

        // 绘制正常帧（始终可见作为基础）
        TextureRegion normalFrame = normalAnimation.getKeyFrame(stateTime, true);
        batch.draw(normalFrame, position.x, position.y);

        if (currentState == State.PRODUCING) {
            // 在顶部绘制激活帧，使用透明度混合
            TextureRegion activeFrame = activeAnimation.getKeyFrame(stateTime, true);

            // 计算交叉淡入淡出的透明度（正弦波：0 -> 1 -> 0）
            float progress = stateTimer / ACTIVE_DURATION;
            float alpha = MathUtils.sin(progress * MathUtils.PI);

            Color c = batch.getColor();
            float oldAlpha = c.a;

            batch.setColor(c.r, c.g, c.b, alpha * oldAlpha);
            batch.draw(activeFrame, position.x, position.y);
            batch.setColor(c.r, c.g, c.b, oldAlpha); // 恢复透明度
        }
    }

    private void produceSun () {
        // 在植物上方稍微偏移的位置生成阳光
        float sunX = position.x + 25;
        float sunY = position.y - 10;
        Sun sun = new Sun(screen, sunX, sunY);
        screen.addSun(sun);
    }
}
