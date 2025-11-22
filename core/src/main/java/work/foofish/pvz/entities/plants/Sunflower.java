package work.foofish.pvz.entities.plants;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import work.foofish.pvz.entities.Sun;
import work.foofish.pvz.screens.GameScreen;
import work.foofish.pvz.utils.AssetPaths;

public class Sunflower extends BasePlant {
    private static final int COST = 50;
    private static final int HEALTH = 300;
    private static final float COOLDOWN = 7.5f;
    private static final float PRODUCTION_INTERVAL = 2f;

    private float productionTimer;

    public Sunflower(GameScreen screen, float x, float y) {
        super(screen, x, y,
              createAnimation(screen),
              COST,
              HEALTH,
              COOLDOWN);
        this.productionTimer = 0f;
    }

    private static Animation<TextureRegion> createAnimation(GameScreen screen) {
        TextureAtlas atlas = screen.getAssets().getAtlas(AssetPaths.PLANTS_ATLAS);
        return new Animation<>(0.1f, atlas.findRegions(AssetPaths.REGION_SUNFLOWER_NORMAL), Animation.PlayMode.LOOP);
    }

    @Override
    public void action(float delta) {
        productionTimer += delta;
        if (productionTimer >= PRODUCTION_INTERVAL) {
            produceSun();
            productionTimer = 0f;
        }
    }

    private void produceSun() {
        // Spawn sun slightly above the plant
        float sunX = position.x + 10; // Offset slightly
        float sunY = position.y + 40;
        Sun sun = new Sun(screen, sunX, sunY);
        screen.addSun(sun);
        System.out.println("Sunflower produced sun at " + position);
    }
}
