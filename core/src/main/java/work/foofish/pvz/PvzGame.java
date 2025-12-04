package work.foofish.pvz;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class PvzGame extends Game {

    // 供所有 Screen 共享的资源
    public SpriteBatch batch;
    public BitmapFont debugFont;
    // 专门用于游戏内中文 UI 文本的字体，由 AssetService 通过 manifest.json 加载
    public BitmapFont uiFont;
    private final AssetService assetService = AssetService.getInstance();

    @Override
    public void create () {
        batch = new SpriteBatch();
        // 默认调试字体，仅用于加载界面等英文调试文本
        debugFont = new BitmapFont();
        // 在资源加载完成前先用调试字体作为占位，避免空指针
        uiFont = debugFont;
        setScreen(new LoadingScreen(this));
    }

    public AssetService getAssets () {
        return assetService;
    }

    @Override
    public void dispose () {
        if (screen != null) {
            screen.dispose();
        }
        batch.dispose();
        // debugFont 由游戏手动创建，需要手动释放
        if (debugFont != null) {
            debugFont.dispose();
        }
        // uiFont 由 AssetManager 管理，不要单独 dispose，交给 AssetService 统一释放
        assetService.dispose();
    }
}
