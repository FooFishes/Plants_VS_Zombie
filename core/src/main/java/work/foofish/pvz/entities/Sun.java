package work.foofish.pvz.entities;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.utils.AssetPaths;

public class Sun {
    private static final float LIFETIME = 10f; // 阳光在10秒后消失
    private static final float FALL_SPEED = 50f; // 每秒下落像素数
    private static final float TARGET_Y = 100f; // 停止下落的Y坐标

    private final GameScreen screen;
    private final Vector2 position;
    private final Rectangle bounds;
    private final Animation<TextureRegion> animation;
    private float stateTime;
    private boolean active;
    private boolean isFalling;

    public Sun(GameScreen screen, float x, float y) {
        this.screen = screen;
        this.position = new Vector2(x, y);
        this.active = true;
        this.stateTime = 0f;
        this.isFalling = false; // 默认不下落（由向日葵产生）

        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.UI_ATLAS);
        this.animation = new Animation<>(0.1f, atlas.findRegions(AssetPaths.REGION_SUN), Animation.PlayMode.LOOP);

        TextureRegion firstFrame = animation.getKeyFrame(0);
        if (firstFrame != null) {
            this.bounds = new Rectangle(x, y, firstFrame.getRegionWidth(), firstFrame.getRegionHeight());
        } else {
            this.bounds = new Rectangle(x, y, 50, 50); // 备用尺寸
        }
    }

    public void update(float delta) {
        stateTime += delta;
        if (stateTime >= LIFETIME) {
            active = false;
        }

        if (isFalling && position.y > TARGET_Y) {
            position.y -= FALL_SPEED * delta;
            bounds.setPosition(position);
        }
    }

    public void draw(SpriteBatch batch) {
        if (active && animation != null) {
            TextureRegion currentFrame = animation.getKeyFrame(stateTime, true);
            batch.draw(currentFrame, position.x, position.y);
        }
    }

    public void collect() {
        active = false;
        // TODO: 添加增加玩家阳光货币的逻辑
        System.out.println("Sun collected!");
    }

    public boolean isActive() {
        return active;
    }

    public Rectangle getBounds() {
        return bounds;
    }
}
