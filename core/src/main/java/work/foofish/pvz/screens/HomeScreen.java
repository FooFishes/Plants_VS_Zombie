package work.foofish.pvz.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import work.foofish.pvz.PvzGame;
import work.foofish.pvz.utils.AssetPaths;

public class HomeScreen implements Screen {
    private final PvzGame game;
    private final Stage stage;
    private final TextureAtlas atlas;
    private final Skin skin;

    public HomeScreen (PvzGame game) {
        this.game = game;
        this.stage = new Stage(new FitViewport(900, 600), game.batch);
        this.atlas = game.getAssets().getAtlas(AssetPaths.UI_ATLAS);
        this.skin = new Skin();
        this.skin.addRegions(atlas);
        buildUI();
    }

    private void buildUI () {
        TextureRegion bgRegion = atlas.findRegion(AssetPaths.REGION_MAIN_MENU);
        Image background = new Image(bgRegion);
        background.setFillParent(true);
        stage.addActor(background);

        Button.ButtonStyle btnStyle = new Button.ButtonStyle();
        btnStyle.up = skin.newDrawable("adventure_up");
        btnStyle.down = skin.newDrawable("adventure_down");
        skin.add("adventure_button", btnStyle);

        Table table = new Table();
        table.setFillParent(true);

        Button adventure = new Button(skin, "adventure_button");
        adventure.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game));
            }
        });

        table.add(adventure).width(360).height(150).pad(70, 0, 0, 75);
        table.align(Align.topRight);
        stage.addActor(table);
        table.toFront();

        // 图鉴按钮
        TextureRegion almanacUp = atlas.findRegion(AssetPaths.REGION_ALMANAC_BTN_UP);
        TextureRegion almanacDown = atlas.findRegion(AssetPaths.REGION_ALMANAC_BTN_DOWN);
        if (almanacUp != null && almanacDown != null) {
            Button.ButtonStyle almanacStyle = new Button.ButtonStyle();
            almanacStyle.up = skin.newDrawable(AssetPaths.REGION_ALMANAC_BTN_UP);
            almanacStyle.down = skin.newDrawable(AssetPaths.REGION_ALMANAC_BTN_DOWN);
            skin.add("almanac_button", almanacStyle);

            Button almanac = new Button(skin, "almanac_button");
            almanac.setPosition(370, 60);
            almanac.addListener(new ChangeListener() {
                @Override
                public void changed (ChangeEvent event, Actor actor) {
                    game.setScreen(new AlmanacScreen(game, HomeScreen.this));
                }
            });
            stage.addActor(almanac);
        }
    }

    @Override
    public void show () {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render (float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize (int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause () {

    }

    @Override
    public void resume () {

    }

    @Override
    public void hide () {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose () {
        stage.dispose();
        skin.dispose();
    }
}
