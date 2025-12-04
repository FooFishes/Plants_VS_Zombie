package work.foofish.pvz.almanac;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import work.foofish.pvz.AssetService;
import work.foofish.pvz.ui.TextureRegionActor;
import work.foofish.pvz.utils.AssetPaths;

/**
 * 图鉴植物卡片组件
 */
public class AlmanacPlantCard extends Group {
    private final AlmanacPlantType plantType;
    private final TextureRegionActor cardActor;
    private final TextureRegionActor highlightActor;
    private boolean selected;

    public AlmanacPlantCard(AlmanacPlantType plantType, AssetService assets) {
        this.plantType = plantType;

        TextureAtlas cardAtlas = assets.getAtlas(AssetPaths.CARD_ATLAS);
        TextureRegion cardRegion = cardAtlas.findRegion(plantType.getCardRegion());

        this.cardActor = new TextureRegionActor(cardRegion);
        this.cardActor.setPosition(0, 0);
        addActor(cardActor);

        // 高亮效果（如果有的话）
        TextureAtlas almanacAtlas = assets.getAtlas(AssetPaths.ALMANAC_ATLAS);
        TextureRegion highlightRegion = almanacAtlas.findRegion(AssetPaths.REGION_SEED_PACKET_FLASH);
        if (highlightRegion != null) {
            this.highlightActor = new TextureRegionActor(highlightRegion);
            this.highlightActor.setPosition(0, 0);
            this.highlightActor.setVisible(false);
            addActor(highlightActor);
        } else {
            this.highlightActor = null;
        }

        setSize(cardActor.getWidth(), cardActor.getHeight());

        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (highlightActor != null) {
                    highlightActor.setVisible(true);
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (highlightActor != null && !selected) {
                    highlightActor.setVisible(false);
                }
            }
        });
    }

    public AlmanacPlantType getPlantType() {
        return plantType;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (highlightActor != null) {
            highlightActor.setVisible(selected);
        }
    }

    public boolean isSelected() {
        return selected;
    }
}
