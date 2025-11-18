package work.foofish.pvz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public final class AssetService {
    private static final AssetService INSTANCE = new AssetService();
    private final AssetManager manager;

    private AssetService () {
        manager = new AssetManager();
        Texture.setAssetManager(manager);
        // 加载 FreeType 字体
        FileHandleResolver resolver = new InternalFileHandleResolver();
        manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
    }

    public static AssetService getInstance () {
        return INSTANCE;
    }

    public void enqueueFromManifest (String manifestPath) {
        JsonValue root = new JsonReader().parse(Gdx.files.internal(manifestPath));
        JsonValue atlases = root.get("atlases");
        if (atlases != null) {
            for (JsonValue atlas : atlases) {
                manager.load(atlas.asString(), TextureAtlas.class);
            }
        }

        JsonValue musics = root.get("musics");
        if (musics != null) {
            for (JsonValue music : musics) {
                manager.load(music.asString(), Music.class);
            }
        }

        JsonValue sounds = root.get("sounds");
        if (sounds != null) {
            for (JsonValue sound : sounds) {
                manager.load(sound.asString(), Sound.class);
            }
        }

        JsonValue fonts = root.get("fonts");
        if (fonts != null) {
            for (JsonValue font : fonts) {
                FreetypeFontLoader.FreeTypeFontLoaderParameter parameter = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
                parameter.fontFileName = font.getString("file");
                parameter.fontParameters.size = font.getInt("size");
                manager.load(font.getString("key") + ".ttf", BitmapFont.class, parameter);
            }
        }
    }

    public boolean update () {
        return manager.update();
    }

    public float progress () {
        return manager.getProgress();
    }

    public <T> T get (String path, Class<T> type) {
        return manager.get(path, type);
    }

    public TextureAtlas getAtlas (String path) {
        return get(path, TextureAtlas.class);
    }

    public void dispose () {
        manager.dispose();
    }
}
