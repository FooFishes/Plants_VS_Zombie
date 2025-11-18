package work.foofish.pvz;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class LoadingScreen implements Screen {
    private final PvzGame game;
    private final AssetService assetService;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(800, 600, camera);

    private float elapsed;

    public LoadingScreen(PvzGame game) {
        this.game = game;
        this.assetService = game.getAssetService();
        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);
    }

    /**
     * Called when this screen becomes the current screen for a {@link Game}.
     */
    @Override
    public void show () {
        assetService.enqueueFromManifest("manifest.json");
    }

    /**
     * Called when the screen should render itself.
     *
     * @param delta The time in seconds since the last render.
     */
    @Override
    public void render (float delta) {
        elapsed = elapsed + delta;

        Gdx.gl.glClearColor(0.2f, 0.5f, 0.8f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        String progressText = "Loading... " + (assetService.progress() * 100) + "%";
        game.debugFont.draw(game.batch, progressText, 320, 310);
        game.batch.end();

        if (assetService.update() && elapsed > 1f) {
            game.setScreen(new HomeScreen(game));
        }
    }

    /**
     * @param width
     * @param height
     * @see ApplicationListener#resize(int, int)
     */
    @Override
    public void resize (int width, int height) {
        viewport.update(width, height, true);
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

    }
}
