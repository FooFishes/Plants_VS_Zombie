package work.foofish.pvz;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class HomeScreen implements Screen {
    private final PvzGame game;
    private final Stage stage;
    private final TextureAtlas atlas;
    private final Animation<TextureRegion> sunflowerNormalAnimation;
    private final Animation<TextureRegion> sunflowerActiveAnimation;

    private float animTime;
    private float produceIntervalTimer;
    private float activeTimer;
    private boolean producingSun;

    private static final float PRODUCE_INTERVAL = 2f;      // 每 2 秒产一次阳光
    private static final float ACTIVE_DURATION = 0.5f;     // 激活态持续时间

    public HomeScreen (PvzGame game) {
        this.game = game;
        // 从资源管理器中获取 Plants 图集
        this.atlas = game.getAssetService().getAtlas("atlases/Plants.atlas");
        this.stage = new Stage(new FitViewport(900, 600), game.batch);
        // 普通、激活两套动画帧
        this.sunflowerNormalAnimation = new Animation<>(0.1f, atlas.findRegions("sunflower_normal"), Animation.PlayMode.LOOP);
        this.sunflowerActiveAnimation = new Animation<>(0.1f, atlas.findRegions("sunflower_active"), Animation.PlayMode.LOOP);

        this.animTime = 0f;
        this.produceIntervalTimer = 0f;
        this.activeTimer = 0f;
        this.producingSun = false;
    }

    /**
     * Called when this screen becomes the current screen for a {@link Game}.
     */
    @Override
    public void show () {
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Called when the screen should render itself.
     *
     * @param delta The time in seconds since the last render.
     */
    @Override
    public void render (float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        animTime += delta;
        produceIntervalTimer += delta;

        // 每隔 PRODUCE_INTERVAL 进入一次激活态，产阳光
        if (!producingSun && produceIntervalTimer >= PRODUCE_INTERVAL) {
            producingSun = true;
            activeTimer = 0f;
            produceIntervalTimer = 0f;
            Gdx.app.log("Sunflower", "Produce sun!");
        }

        // 激活态只持续 ACTIVE_DURATION，然后回到普通态
        if (producingSun) {
            activeTimer += delta;
            if (activeTimer >= ACTIVE_DURATION) {
                producingSun = false;
            }
        }

        stage.act(delta);
        stage.getViewport().apply();

        // 使用批处理在固定位置绘制向日葵帧
        SpriteBatch batch = game.batch;
        batch.setProjectionMatrix(stage.getCamera().combined);
        batch.begin();

        Animation<TextureRegion> currentAnim = producingSun ? sunflowerActiveAnimation : sunflowerNormalAnimation;
        TextureRegion currentFrame = currentAnim.getKeyFrame(animTime, true);
        batch.draw(currentFrame, 50, 50);

        batch.end();

        stage.draw();
    }

    /**
     * @param width
     * @param height
     * @see ApplicationListener#resize(int, int)
     */
    @Override
    public void resize (int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    /**
     * @see ApplicationListener#pause()
     */
    @Override
    public void pause () {

    }

    /**
     * @see ApplicationListener#resume()
     */
    @Override
    public void resume () {

    }

    /**
     * Called when this screen is no longer the current screen for a {@link Game}.
     */
    @Override
    public void hide () {

    }

    /**
     * Called when this screen should release all resources.
     */
    @Override
    public void dispose () {
        stage.dispose();
    }
}
