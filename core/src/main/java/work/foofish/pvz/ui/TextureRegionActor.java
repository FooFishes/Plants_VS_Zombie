package work.foofish.pvz.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * 简单的纹理渲染Actor，用于在Scene2D中显示TextureRegion
 */
public class TextureRegionActor extends Actor {
    private TextureRegion region;

    public TextureRegionActor(TextureRegion region) {
        this.region = region;
        if (region != null) {
            setSize(region.getRegionWidth(), region.getRegionHeight());
        }
    }

    public TextureRegion getRegion() {
        return region;
    }

    public void setRegion(TextureRegion region) {
        this.region = region;
        if (region != null) {
            setSize(region.getRegionWidth(), region.getRegionHeight());
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (region == null) return;
        batch.setColor(getColor().r, getColor().g, getColor().b, getColor().a * parentAlpha);
        batch.draw(region, getX(), getY(), getOriginX(), getOriginY(),
                getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation());
    }
}
