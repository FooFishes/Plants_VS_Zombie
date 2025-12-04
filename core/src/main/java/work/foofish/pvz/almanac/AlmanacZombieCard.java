package work.foofish.pvz.almanac;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Array;
import work.foofish.pvz.AssetService;
import work.foofish.pvz.ui.TextureRegionActor;
import work.foofish.pvz.utils.AssetPaths;

/**
 * 图鉴僵尸卡片组件
 */
public class AlmanacZombieCard extends Group {
    private final AlmanacZombieType zombieType;
    private final TextureRegionActor borderActor;
    private final TextureRegionActor backgroundActor;
    private final AlmanacAnimationActor zombieActor;
    private boolean selected;
    private float stateTime;

    public AlmanacZombieCard(AlmanacZombieType zombieType, AssetService assets) {
        this.zombieType = zombieType;
        this.stateTime = 0f;

        TextureAtlas almanacAtlas = assets.getAtlas(AssetPaths.ALMANAC_ATLAS);

        // 背景
        TextureRegion bgRegion = almanacAtlas.findRegion(AssetPaths.REGION_ZOMBIE_WINDOW_BG);
        this.backgroundActor = new TextureRegionActor(bgRegion);
        this.backgroundActor.setPosition(0, 0);
        addActor(backgroundActor);

        // 僵尸动画
        TextureAtlas zombieAtlas = assets.getAtlas(AssetPaths.ZOMBIES_ATLAS);
        Array<TextureAtlas.AtlasRegion> frames = zombieAtlas.findRegions(zombieType.getAnimationRegion());
        Animation<TextureRegion> animation = new Animation<>(0.09f, frames, Animation.PlayMode.LOOP);
        this.zombieActor = new AlmanacAnimationActor(animation, zombieType.getScale() * 0.6f);
        this.zombieActor.setFlipX(true);

        // 根据背景大小居中放置僵尸
        float zx = (backgroundActor.getWidth() - zombieActor.getWidth()) / 2f;
        float zy = (backgroundActor.getHeight() - zombieActor.getHeight()) / 2f;
        zombieActor.setPosition(zx, zy);
        zombieActor.setTouchable(Touchable.disabled);
        addActor(zombieActor);

        // 边框
        TextureRegion borderRegion = almanacAtlas.findRegion(AssetPaths.REGION_ZOMBIE_WINDOW_BORDER);
        this.borderActor = new TextureRegionActor(borderRegion);
        this.borderActor.setPosition(0, 0);
        addActor(borderActor);

        setSize(borderActor.getWidth(), borderActor.getHeight());

        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                // 悬停效果
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                // 移除悬停效果
            }
        });
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
    }

    @Override
    protected void drawChildren(Batch batch, float parentAlpha) {
        // 裁剪区域，防止僵尸超出边框
        if (clipBegin(3, 3, getWidth() - 6, getHeight() - 6)) {
            super.drawChildren(batch, parentAlpha);
            clipEnd();
        } else {
            super.drawChildren(batch, parentAlpha);
        }
    }

    public AlmanacZombieType getZombieType() {
        return zombieType;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }
}
