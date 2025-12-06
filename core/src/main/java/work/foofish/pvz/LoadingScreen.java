package work.foofish.pvz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import work.foofish.pvz.screens.HomeScreen;
import work.foofish.pvz.utils.AssetPaths;

public class LoadingScreen implements Screen {
    private final PvzGame game;
    private final AssetService assets;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(800, 600, camera);

    private float elapsed;

    public LoadingScreen (PvzGame game) {
        this.game = game;
        this.assets = game.getAssets();
        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);
    }

    @Override
    public void show () {
        assets.enqueueFromManifest("manifest.json");
    }

    @Override
    public void render (float delta) {
        elapsed = elapsed + delta;

        Gdx.gl.glClearColor(0.2f, 0.5f, 0.8f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        String progressText = "Loading... " + (assets.progress() * 100) + "%";
        game.debugFont.draw(game.batch, progressText, 320, 310);
        game.batch.end();

        if (assets.update() && elapsed > 1f) {
            // 资源加载完成后，从 AssetManager 中获取中文 UI 字体
            try {
                BitmapFont cnFont = assets.get(AssetPaths.FONT_CN_DEFAULT, BitmapFont.class);
                game.uiFont = cnFont != null ? cnFont : game.debugFont;
            } catch (Exception e) {
                // 如果字体加载失败，则继续使用调试字体，避免阻塞游戏
                game.uiFont = game.debugFont;
            }
            game.setScreen(new HomeScreen(game));
        }
    }

    @Override
    public void resize (int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause () {

    }

    @Override
    public void resume () {

    }

    @Override
    public void hide () {

    }


    @Override
    public void dispose () {

    }
}
