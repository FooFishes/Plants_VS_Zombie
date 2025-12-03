package work.foofish.pvz.ui;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import work.foofish.pvz.utils.AssetPaths;

/**
 * 樱桃炸弹爆炸时的简易特效，负责渲染 UI 图集中的 Boom 帧序列。
 */
public class Boom {
    private static final float FRAME_DURATION = 0.06f;
    private static final float DEFAULT_SCALE = 0.7f;

    private final Vector2 center = new Vector2();
    private final Animation<TextureRegion> animation;
    private final float scale;
    private float stateTime;
    private boolean finished;

    public Boom (TextureAtlas uiAtlas, float centerX, float centerY) {
        this(uiAtlas, centerX, centerY, DEFAULT_SCALE);
    }

    public Boom (TextureAtlas uiAtlas, float centerX, float centerY, float scale) {
        if (uiAtlas == null) {
            throw new IllegalArgumentException("UI atlas is required for Boom animation");
        }
        Array<TextureAtlas.AtlasRegion> frames = fetchFrames(uiAtlas);
        this.animation = new Animation<>(FRAME_DURATION, frames, Animation.PlayMode.NORMAL);
        this.center.set(centerX, centerY);
        this.scale = scale;
    }

    private static Array<TextureAtlas.AtlasRegion> fetchFrames (TextureAtlas atlas) {
        Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(AssetPaths.REGION_BOOM);
        if (frames == null || frames.size == 0) {
            TextureAtlas.AtlasRegion single = atlas.findRegion(AssetPaths.REGION_BOOM);
            if (single != null) {
                frames = new Array<>(1);
                frames.add(single);
            }
        }
        if (frames == null || frames.size == 0) {
            TextureAtlas.AtlasRegion fallback = atlas.findRegion(AssetPaths.REGION_SUN);
            if (fallback != null) {
                frames = new Array<>(1);
                frames.add(fallback);
            }
        }
        if (frames == null || frames.size == 0) {
            throw new IllegalStateException("No frames available for Boom animation");
        }
        return frames;
    }

    public void update (float delta) {
        if (finished) {
            return;
        }
        stateTime += delta;
        if (animation.isAnimationFinished(stateTime)) {
            finished = true;
        }
    }

    public void draw (SpriteBatch batch) {
        if (finished) {
            return;
        }
        TextureRegion frame = animation.getKeyFrame(stateTime, animation.getPlayMode() == Animation.PlayMode.LOOP);
        float width = frame.getRegionWidth() * scale;
        float height = frame.getRegionHeight() * scale;
        float drawX = center.x - width / 2f;
        float drawY = center.y - height / 2f;
        batch.draw(frame, drawX, drawY, width, height);
    }

    public boolean isFinished () {
        return finished;
    }
}
