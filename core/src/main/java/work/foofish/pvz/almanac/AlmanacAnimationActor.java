package work.foofish.pvz.almanac;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * 图鉴专用的动画展示Actor，独立于GameScreen
 * 仅用于播放动画，不包含任何游戏逻辑
 */
public class AlmanacAnimationActor extends Actor {
    private final Animation<TextureRegion> animation;
    private float stateTime;
    private float scale;
    private boolean flipX;

    public AlmanacAnimationActor(Animation<TextureRegion> animation) {
        this(animation, 1.0f);
    }

    public AlmanacAnimationActor(Animation<TextureRegion> animation, float scale) {
        this.animation = animation;
        this.scale = scale;
        this.stateTime = 0f;
        this.flipX = false;
        if (animation != null) {
            TextureRegion frame = animation.getKeyFrame(0);
            setSize(frame.getRegionWidth() * scale, frame.getRegionHeight() * scale);
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (animation == null) return;

        TextureRegion frame = animation.getKeyFrame(stateTime, true);
        batch.setColor(getColor().r, getColor().g, getColor().b, getColor().a * parentAlpha);

        float drawWidth = frame.getRegionWidth() * scale;
        float drawHeight = frame.getRegionHeight() * scale;

        if (flipX) {
            batch.draw(frame, getX() + drawWidth, getY(), -drawWidth, drawHeight);
        } else {
            batch.draw(frame, getX(), getY(), drawWidth, drawHeight);
        }
    }

    public void setScale(float scale) {
        this.scale = scale;
        if (animation != null) {
            TextureRegion frame = animation.getKeyFrame(0);
            setSize(frame.getRegionWidth() * scale, frame.getRegionHeight() * scale);
        }
    }

    public float getAnimationScale() {
        return scale;
    }

    public void setFlipX(boolean flipX) {
        this.flipX = flipX;
    }

    public boolean isFlipX() {
        return flipX;
    }

    public void resetStateTime() {
        this.stateTime = 0f;
    }

    public Animation<TextureRegion> getAnimation() {
        return animation;
    }
}
