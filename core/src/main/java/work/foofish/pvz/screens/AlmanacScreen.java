package work.foofish.pvz.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import work.foofish.pvz.AssetService;
import work.foofish.pvz.PvzGame;
import work.foofish.pvz.almanac.*;
import work.foofish.pvz.ui.PressableGroup;
import work.foofish.pvz.ui.TextureRegionActor;
import work.foofish.pvz.utils.AssetPaths;

/**
 * 图鉴界面
 */
public class AlmanacScreen implements Screen {

    private enum State {
        INDEX,
        PLANT_DETAILS,
        ZOMBIE_DETAILS
    }

    private static final float WORLD_WIDTH = 900f;
    private static final float WORLD_HEIGHT = 600f;

    private final PvzGame game;
    private final Screen previousScreen;
    private final AssetService assets;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final Stage stage;
    private final BitmapFont font;

    private State state;

    // 索引页
    private AlmanacAnimationActor indexSunflower;
    private AlmanacAnimationActor indexZombie;
    private PressableGroup plantDetailBtn;
    private PressableGroup zombieDetailBtn;
    private PressableGroup closeBtn;
    private PressableGroup indexBtn;

    // 植物详情页
    private boolean plantDetailsInitialized = false;
    private final Array<AlmanacPlantCard> plantCards = new Array<>();
    private AlmanacPlantCard selectedPlantCard;
    private AlmanacAnimationActor selectedPlantActor;
    private Label plantNameLabel;
    private Table plantDetailTable;
    private Label plantCostLabel;
    private Label plantRechargeLabel;
    private TextureRegionActor plantGroundActor;

    // 僵尸详情页
    private boolean zombieDetailsInitialized = false;
    private final Array<AlmanacZombieCard> zombieCards = new Array<>();
    private AlmanacZombieCard selectedZombieCard;
    private AlmanacAnimationActor selectedZombieActor;
    private Label zombieNameLabel;
    private Table zombieDetailTable;

    public AlmanacScreen(PvzGame game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        this.assets = game.getAssets();
        this.state = State.INDEX;

        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.update();

        this.stage = new Stage(viewport, game.batch);
        // 所有图鉴中的中文文本统一使用 PvzGame.uiFont，如果为空则回退到 debugFont
        this.font = game.uiFont != null ? game.uiFont : game.debugFont;

        initIndexPage();
    }

    private void initIndexPage() {
        // 创建索引页的向日葵动画
        TextureAtlas plantsAtlas = assets.getAtlas(AssetPaths.PLANTS_ATLAS);
        Array<TextureAtlas.AtlasRegion> sunflowerFrames = plantsAtlas.findRegions(AssetPaths.REGION_SUNFLOWER_NORMAL);
        Animation<TextureRegion> sunflowerAnim = new Animation<>(0.1f, sunflowerFrames, Animation.PlayMode.LOOP);
        indexSunflower = new AlmanacAnimationActor(sunflowerAnim, 1.5f);
        indexSunflower.setPosition(190, 290);

        // 创建索引页的僵尸动画
        TextureAtlas zombiesAtlas = assets.getAtlas(AssetPaths.ZOMBIES_ATLAS);
        Array<TextureAtlas.AtlasRegion> zombieFrames = zombiesAtlas.findRegions(AssetPaths.REGION_NORMAL_ZOMBIE_WALK);
        Animation<TextureRegion> zombieAnim = new Animation<>(0.09f, zombieFrames, Animation.PlayMode.LOOP);
        indexZombie = new AlmanacAnimationActor(zombieAnim, 1.2f);
        indexZombie.setFlipX(true);
        indexZombie.setPosition(580, 245);

        TextureAtlas almanacAtlas = assets.getAtlas(AssetPaths.ALMANAC_ATLAS);

        // 查看植物按钮
        plantDetailBtn = createButton(
                almanacAtlas.findRegion(AssetPaths.REGION_SEED_CHOOSER_BTN),
                almanacAtlas.findRegion(AssetPaths.REGION_SEED_CHOOSER_BTN_GLOW),
                "查看植物"
        );
        plantDetailBtn.setPosition(155, 220);
        plantDetailBtn.setClickListener(() -> {
            state = State.PLANT_DETAILS;
            if (!plantDetailsInitialized) {
                plantDetailsInitialized = true;
                initPlantDetailsPage();
            }
            rebuildStageForPlantDetails();
        });

        // 查看僵尸按钮
        zombieDetailBtn = createButton(
                almanacAtlas.findRegion(AssetPaths.REGION_GRAVE_BTN),
                almanacAtlas.findRegion(AssetPaths.REGION_GRAVE_BTN_GLOW),
                "查看僵尸"
        );
        zombieDetailBtn.setPosition(560, 220);
        zombieDetailBtn.setClickListener(() -> {
            state = State.ZOMBIE_DETAILS;
            if (!zombieDetailsInitialized) {
                zombieDetailsInitialized = true;
                initZombieDetailsPage();
            }
            rebuildStageForZombieDetails();
        });

        // 关闭按钮
        closeBtn = createButton(
                almanacAtlas.findRegion(AssetPaths.REGION_ALMANAC_CLOSE_BTN),
                almanacAtlas.findRegion(AssetPaths.REGION_ALMANAC_CLOSE_BTN_HL),
                "关闭"
        );
        closeBtn.setPosition(780, 10);
        closeBtn.setClickListener(() -> game.setScreen(previousScreen));

        // 索引按钮（用于从详情页返回）
        indexBtn = createButton(
                almanacAtlas.findRegion(AssetPaths.REGION_ALMANAC_INDEX_BTN),
                almanacAtlas.findRegion(AssetPaths.REGION_ALMANAC_INDEX_BTN_HL),
                "索引"
        );
        indexBtn.setPosition(20, 10);
        indexBtn.setClickListener(() -> {
            state = State.INDEX;
            rebuildStageForIndex();
        });

        rebuildStageForIndex();
    }

    private PressableGroup createButton(TextureRegion normalRegion, TextureRegion highlightRegion, String text) {
        PressableGroup btn = new PressableGroup();

        TextureRegionActor normalActor = new TextureRegionActor(normalRegion);
        normalActor.setPosition(0, 0);
        btn.addActor(normalActor);

        TextureRegionActor highlightActor = null;
        if (highlightRegion != null) {
            highlightActor = new TextureRegionActor(highlightRegion);
            highlightActor.setPosition(0, 0);
            highlightActor.setVisible(false);
            btn.addActor(highlightActor);
        }

        Label label = new Label(text, new Label.LabelStyle(font, Color.WHITE));
        label.pack();
        label.setPosition(
                (normalActor.getWidth() - label.getWidth()) / 2f,
                (normalActor.getHeight() - label.getHeight()) / 2f
        );
        btn.addActor(label);

        btn.setSize(normalActor.getWidth(), normalActor.getHeight());

        final TextureRegionActor finalHighlightActor = highlightActor;
        btn.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (finalHighlightActor != null) {
                    finalHighlightActor.setVisible(true);
                    normalActor.setVisible(false);
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (finalHighlightActor != null) {
                    finalHighlightActor.setVisible(false);
                    normalActor.setVisible(true);
                }
            }
        });

        return btn;
    }

    private void initPlantDetailsPage() {
        TextureAtlas almanacAtlas = assets.getAtlas(AssetPaths.ALMANAC_ATLAS);

        // 草地背景
        TextureRegion groundRegion = almanacAtlas.findRegion(AssetPaths.REGION_ALMANAC_GROUND_DAY);
        if (groundRegion != null) {
            plantGroundActor = new TextureRegionActor(groundRegion);
            plantGroundActor.setPosition(595, 370);
            plantGroundActor.setScale(0.93f,0.70f);
        }

        // 植物名称标签
        plantNameLabel = new Label("", new Label.LabelStyle(font, Color.YELLOW));
        plantNameLabel.setPosition(615, 343);
        plantNameLabel.setWidth(150f);
        plantNameLabel.setAlignment(Align.center);

        // 详情表格
        plantDetailTable = new Table();
        plantDetailTable.setPosition(560, 310);
        plantDetailTable.setWidth(260f);
        plantDetailTable.top();

        // 花费和充能标签
        plantCostLabel = new Label("", new Label.LabelStyle(font, Color.WHITE));
        plantCostLabel.setPosition(560, 90);

        plantRechargeLabel = new Label("", new Label.LabelStyle(font, Color.WHITE));
        plantRechargeLabel.setPosition(750, 90);

        // 创建植物卡片
        double gapX = 58.3;
        int startX = 34;
        int startY = 440;

        for (AlmanacPlantType plantType : AlmanacPlantType.values()) {
            AlmanacPlantCard card = new AlmanacPlantCard(plantType, assets);
            card.setPosition(startX, startY);
            card.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showPlantDetails(card);
                }
            });
            plantCards.add(card);
            startX += gapX;
        }

        // 默认选中第一个
        if (plantCards.size > 0) {
            showPlantDetails(plantCards.get(0));
        }
    }

    private void showPlantDetails(AlmanacPlantCard card) {
        if (selectedPlantCard == card) return;

        if (selectedPlantCard != null) {
            selectedPlantCard.setSelected(false);
        }
        selectedPlantCard = card;
        card.setSelected(true);

        // 创建植物动画
        TextureAtlas plantsAtlas = assets.getAtlas(AssetPaths.PLANTS_ATLAS);
        Array<TextureAtlas.AtlasRegion> frames = plantsAtlas.findRegions(card.getPlantType().getAnimationRegion());
        Animation<TextureRegion> anim = new Animation<>(0.1f, frames, Animation.PlayMode.LOOP);
        selectedPlantActor = new AlmanacAnimationActor(anim, 1.5f);
        selectedPlantActor.setPosition(640, 383);

        // 读取 JSON 配置
        String jsonPath = card.getPlantType().getJsonPath();
        try {
            String jsonContent = Gdx.files.internal(jsonPath).readString();
            JsonReader reader = new JsonReader();
            JsonValue value = reader.parse(jsonContent);

            plantNameLabel.setText(value.getString("name"));

            plantDetailTable.clear();
            Label descLabel = new Label(value.getString("description"), new Label.LabelStyle(font, new Color(0f, 0f, 0.55f, 1f)));
            descLabel.setWrap(true);
            descLabel.setWidth(plantDetailTable.getWidth());
            plantDetailTable.add(descLabel).width(plantDetailTable.getWidth()).padBottom(8f).row();

            JsonValue tips = value.get("tips");
            if (tips != null) {
                for (JsonValue field = tips.child; field != null; field = field.next) {
                    Label tipLabel = new Label(field.name + ": " + field.asString(), new Label.LabelStyle(font, Color.BROWN));
                    tipLabel.setWrap(true);
                    tipLabel.setWidth(plantDetailTable.getWidth());
                    plantDetailTable.add(tipLabel).width(plantDetailTable.getWidth()).padBottom(2f).row();
                }
            }

            Label storyLabel = new Label(value.getString("story"), new Label.LabelStyle(font, new Color(0.65f, 0.16f, 0.16f, 1f)));
            storyLabel.setWrap(true);
            storyLabel.setWidth(plantDetailTable.getWidth());
            plantDetailTable.add(storyLabel).width(plantDetailTable.getWidth()).padTop(8f).row();

            plantCostLabel.setText("花费: " + value.getString("cost"));
            plantRechargeLabel.setText("充能: " + value.getString("recharge"));
        } catch (Exception e) {
            Gdx.app.error("AlmanacScreen", "Failed to load plant JSON: " + jsonPath, e);
        }

        rebuildStageForPlantDetails();
    }

    private void initZombieDetailsPage() {
        // 僵尸名称标签
        zombieNameLabel = new Label("", new Label.LabelStyle(font, Color.GREEN));
        zombieNameLabel.setPosition(615, 275);
        zombieNameLabel.setWidth(150f);
        zombieNameLabel.setAlignment(Align.center);

        // 详情表格
        zombieDetailTable = new Table();
        zombieDetailTable.setPosition(560, 250);
        zombieDetailTable.setWidth(265f);
        zombieDetailTable.top();

        // 创建僵尸卡片
        int gapX = 85;
        int startX = 25;
        int startY = 440;

        for (AlmanacZombieType zombieType : AlmanacZombieType.values()) {
            AlmanacZombieCard card = new AlmanacZombieCard(zombieType, assets);
            card.setPosition(startX, startY);
            card.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showZombieDetails(card);
                }
            });
            zombieCards.add(card);
            startX += gapX;
        }

        // 默认选中第一个
        if (zombieCards.size > 0) {
            showZombieDetails(zombieCards.get(0));
        }
    }

    private void showZombieDetails(AlmanacZombieCard card) {
        if (selectedZombieCard == card) return;

        if (selectedZombieCard != null) {
            selectedZombieCard.setSelected(false);
        }
        selectedZombieCard = card;
        card.setSelected(true);

        // 创建僵尸动画
        TextureAtlas zombiesAtlas = assets.getAtlas(AssetPaths.ZOMBIES_ATLAS);
        Array<TextureAtlas.AtlasRegion> frames = zombiesAtlas.findRegions(card.getZombieType().getAnimationRegion());
        Animation<TextureRegion> anim = new Animation<>(0.09f, frames, Animation.PlayMode.LOOP);
        selectedZombieActor = new AlmanacAnimationActor(anim, card.getZombieType().getScale());
        selectedZombieActor.setFlipX(true);
        selectedZombieActor.setPosition(645, 350);

        // 读取 JSON 配置
        String jsonPath = card.getZombieType().getJsonPath();
        try {
            String jsonContent = Gdx.files.internal(jsonPath).readString();
            JsonReader reader = new JsonReader();
            JsonValue value = reader.parse(jsonContent);

            zombieNameLabel.setText(value.getString("name"));

            zombieDetailTable.clear();
            Label descLabel = new Label(value.getString("description"), new Label.LabelStyle(font, new Color(0.16f, 0.2f, 0.35f, 1f)));
            descLabel.setWrap(true);
            descLabel.setWidth(zombieDetailTable.getWidth());
            zombieDetailTable.add(descLabel).width(zombieDetailTable.getWidth()).padBottom(8f).row();

            JsonValue tips = value.get("tips");
            if (tips != null) {
                for (JsonValue field = tips.child; field != null; field = field.next) {
                    Label tipLabel = new Label(field.name + ": " + field.asString(), new Label.LabelStyle(font, Color.BROWN));
                    tipLabel.setWrap(true);
                    tipLabel.setWidth(zombieDetailTable.getWidth());
                    zombieDetailTable.add(tipLabel).width(zombieDetailTable.getWidth()).padBottom(2f).row();
                }
            }

            Label storyLabel = new Label(value.getString("story"), new Label.LabelStyle(font, new Color(0.56f, 0.26f, 0.11f, 1f)));
            storyLabel.setWrap(true);
            storyLabel.setWidth(zombieDetailTable.getWidth());
            zombieDetailTable.add(storyLabel).width(zombieDetailTable.getWidth()).padTop(8f).row();
        } catch (Exception e) {
            Gdx.app.error("AlmanacScreen", "Failed to load zombie JSON: " + jsonPath, e);
        }

        rebuildStageForZombieDetails();
    }

    private void rebuildStageForIndex() {
        stage.clear();
        stage.addActor(indexSunflower);
        stage.addActor(indexZombie);
        stage.addActor(plantDetailBtn);
        stage.addActor(zombieDetailBtn);
        stage.addActor(closeBtn);
    }

    private void rebuildStageForPlantDetails() {
        stage.clear();
        if (plantGroundActor != null) {
            stage.addActor(plantGroundActor);
        }
        for (AlmanacPlantCard card : plantCards) {
            stage.addActor(card);
        }
        if (selectedPlantActor != null) {
            stage.addActor(selectedPlantActor);
        }
        stage.addActor(plantNameLabel);
        stage.addActor(plantDetailTable);
        stage.addActor(plantCostLabel);
        stage.addActor(plantRechargeLabel);
        stage.addActor(indexBtn);
        stage.addActor(closeBtn);
    }

    private void rebuildStageForZombieDetails() {
        stage.clear();
        for (AlmanacZombieCard card : zombieCards) {
            stage.addActor(card);
        }
        if (selectedZombieActor != null) {
            stage.addActor(selectedZombieActor);
        }
        stage.addActor(zombieNameLabel);
        stage.addActor(zombieDetailTable);
        stage.addActor(indexBtn);
        stage.addActor(closeBtn);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.3f, 0.7f, 0.8f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        TextureAtlas almanacAtlas = assets.getAtlas(AssetPaths.ALMANAC_ATLAS);

        game.batch.begin();
        // 绘制背景
        TextureRegion bgRegion = null;
        switch (state) {
            case INDEX:
                bgRegion = almanacAtlas.findRegion(AssetPaths.REGION_ALMANAC_INDEX_BG);
                break;
            case PLANT_DETAILS:
                bgRegion = almanacAtlas.findRegion(AssetPaths.REGION_ALMANAC_PLANT_BG);
                break;
            case ZOMBIE_DETAILS:
                bgRegion = almanacAtlas.findRegion(AssetPaths.REGION_ALMANAC_ZOMBIE_BG);
                break;
        }
        if (bgRegion != null) {
            game.batch.draw(bgRegion, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        }

        // 绘制详情页的信息卡背景
        if (state == State.PLANT_DETAILS) {
            TextureRegion plantCardBg = almanacAtlas.findRegion(AssetPaths.REGION_ALMANAC_PLANT_CARD);
            if (plantCardBg != null) {
                game.batch.draw(plantCardBg, 530, 50);
            }
        } else if (state == State.ZOMBIE_DETAILS) {
            TextureRegion zombieCardBg = almanacAtlas.findRegion(AssetPaths.REGION_ALMANAC_ZOMBIE_CARD);
            if (zombieCardBg != null) {
                game.batch.draw(zombieCardBg, 530, 50);
            }
            TextureRegion groundRegion = almanacAtlas.findRegion(AssetPaths.REGION_ALMANAC_GROUND_DAY);
            if (groundRegion != null) {
                game.batch.draw(groundRegion, 590, 300);
            }
        }
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
