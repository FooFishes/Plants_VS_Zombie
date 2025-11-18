package work.foofish.pvz;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class PvzGame extends Game {

    // 供所有 Screen 共享的资源
    public SpriteBatch batch;
    public BitmapFont debugFont;
    private final AssetService assetService = AssetService.getInstance();

    @Override
    public void create () {
        batch = new SpriteBatch();
        debugFont = new BitmapFont();
        setScreen(new LoadingScreen(this));
    }

    public AssetService getAssetService() {
        return assetService;
    }

    @Override
    public void dispose () {
        if (screen != null) {
            screen.dispose();
        }
        batch.dispose();
        debugFont.dispose();
        assetService.dispose();
    }
}
