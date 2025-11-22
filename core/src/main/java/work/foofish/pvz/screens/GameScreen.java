package work.foofish.pvz.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import work.foofish.pvz.AssetService;
import work.foofish.pvz.PvzGame;
import work.foofish.pvz.entities.Sun;
import work.foofish.pvz.entities.plants.BasePlant;
import work.foofish.pvz.entities.plants.Sunflower;
import work.foofish.pvz.utils.AssetPaths;

import java.util.List;

public class GameScreen implements Screen, InputProcessor {
    private static final float MAP_WIDTH = 1400f;
    private static final float MAP_HEIGHT = 600f;
    private static final float VIEW_WIDTH = 900f;
    private static final float VIEW_HEIGHT = 600f;
    private static final float INTRO_DURATION = 1.75f;
    private static final float OUTRO_DURATION = 1.25f;

    // Grid configuration
    private static final float CELL_WIDTH = 80f;
    private static final float CELL_HEIGHT = 96f;
    private static final float GRID_OFFSET_X = 260f; // Distance from Map Left to Grid Left
    private static final float GRID_OFFSET_Y = 85f;  // Distance from Map Top to Grid Top

    private final PvzGame game;
    private final AssetService assets;

    public AssetService getAssets() {
        return assets;
    }
    private final OrthographicCamera worldCamera = new OrthographicCamera();
    private final FitViewport worldViewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, worldCamera);
    private final Stage uiStage;
    private final TextureAtlas uiAtlas;
    private final TextureRegion mapBackground;
    private final Rectangle[][] grid = new Rectangle[5][9];
    private final com.badlogic.gdx.graphics.glutils.ShapeRenderer shapeRenderer;

    private final List<BasePlant> plants = new java.util.ArrayList<>();
    private final List<Sun> suns = new java.util.ArrayList<>();

    private InputMultiplexer inputMultiplexer;

    private CameraAnchor currentAnchor = CameraAnchor.RIGHT;
    private CameraAnchor targetAnchor = CameraAnchor.RIGHT;
    private CameraState cameraState = CameraState.INTRO;
    private boolean cameraTransitionActive;
    private float transitionStartX;
    private float transitionTargetX;
    private float transitionTimer;
    private float transitionDuration = INTRO_DURATION;

    public GameScreen (PvzGame game) {
        this.game = game;
        this.assets = game.getAssets();
        this.uiStage = new Stage(new ScreenViewport());
        this.shapeRenderer = new com.badlogic.gdx.graphics.glutils.ShapeRenderer();

        this.uiAtlas = this.assets.getAtlas(AssetPaths.MAP_ATLAS);
        TextureRegion bgRegion = null;
        if (uiAtlas != null) {
            bgRegion = uiAtlas.findRegion(AssetPaths.REGION_SIMPLE_DAY);
        }
        this.mapBackground = bgRegion;

        initGrid();
        for (int i = 0; i < 5; i++) {
            // Add default Sunflower
            addPlant(new Sunflower(this, grid[i][i].x, grid[i][i].y));
        }

        snapCameraTo(currentAnchor);
    }

    public void addPlant(BasePlant plant) {
        plants.add(plant);
    }

    public void addSun(Sun sun) {
        suns.add(sun);
    }

    @Override
    public void show () {
        inputMultiplexer = new InputMultiplexer(uiStage, this);
        Gdx.input.setInputProcessor(inputMultiplexer);
        beginIntroSlide();
    }

    @Override
    public void render (float delta) {
        updateCamera(delta);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 使用世界相机渲染地图背景
        SpriteBatch batch = game.batch;
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        if (mapBackground != null) {
            // 把整张 1400x600 的地图画在世界坐标系中，由相机裁剪出 900x600 的可见区域
            batch.draw(mapBackground, 0f, 0f, MAP_WIDTH, MAP_HEIGHT);
        }

        // Update and draw plants
        for (int i = plants.size() - 1; i >= 0; i--) {
            BasePlant plant = plants.get(i);
            plant.update(delta);
            plant.draw(batch);
            if (!plant.isAlive()) {
                plants.remove(i);
            }
        }

        // Update and draw suns
        for (int i = suns.size() - 1; i >= 0; i--) {
            Sun sun = suns.get(i);
            sun.update(delta);
            sun.draw(batch);
            if (!sun.isActive()) {
                suns.remove(i);
            }
        }

        batch.end();

        // Draw grid borders
        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 0, 0, 1); // Red color
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                Rectangle rect = grid[row][col];
                shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
            }
        }
        shapeRenderer.end();

        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void resize (int width, int height) {
        worldViewport.update(width, height);
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void pause () {
    }

    @Override
    public void resume () {
    }

    @Override
    public void hide () {
        if (Gdx.input.getInputProcessor() == inputMultiplexer) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose () {
        uiStage.dispose();
        shapeRenderer.dispose();
    }

    public void beginIntroSlide () {
        cameraState = CameraState.INTRO;
        snapCameraTo(CameraAnchor.RIGHT);
        startTransition(CameraAnchor.CENTER, INTRO_DURATION);
    }

    public void beginOutroSlide () {
        if (cameraState == CameraState.OUTRO || (cameraTransitionActive && targetAnchor == CameraAnchor.LEFT)) {
            return;
        }
        cameraState = CameraState.OUTRO;
        startTransition(CameraAnchor.LEFT, OUTRO_DURATION);
    }

    private void startTransition (CameraAnchor anchor, float duration) {
        targetAnchor = anchor;
        transitionStartX = worldCamera.position.x;
        transitionTargetX = anchor.centerX;
        transitionTimer = 0f;
        transitionDuration = Math.max(0.01f, duration);
        cameraTransitionActive = true;
    }

    private void snapCameraTo (CameraAnchor anchor) {
        worldCamera.position.set(anchor.centerX, MAP_HEIGHT / 2f, 0f);
        clampCameraX();
        worldCamera.update();
        currentAnchor = anchor;
    }

    private void updateCamera (float delta) {
        if (cameraTransitionActive) {
            transitionTimer = Math.min(transitionTimer + delta, transitionDuration);
            float progress = transitionTimer / transitionDuration;
            float eased = Interpolation.sine.apply(progress);
            worldCamera.position.x = MathUtils.lerp(transitionStartX, transitionTargetX, eased);
            if (transitionTimer >= transitionDuration) {
                cameraTransitionActive = false;
                currentAnchor = targetAnchor;
                cameraState = currentAnchor == CameraAnchor.CENTER ? CameraState.PLAY : cameraState;
            }
        }
        worldCamera.position.y = MAP_HEIGHT / 2f;
        clampCameraX();
        worldCamera.update();
    }

    private void clampCameraX () {
        float minX = VIEW_WIDTH / 2f;
        float maxX = MAP_WIDTH - VIEW_WIDTH / 2f;
        worldCamera.position.x = MathUtils.clamp(worldCamera.position.x, minX, maxX);
    }

    /**
     * 初始化 5x9 的格子坐标，用于种植植物的地图网格。
     * 这里使用简单的占位布局，后续可以根据实际美术调整。
     */
    private void initGrid () {
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                float x = GRID_OFFSET_X + col * CELL_WIDTH;
                // Calculate y relative to top-left:
                // Map Height - Top Offset - (Row Index + 1) * Cell Height
                // This makes grid[0][0] the top-left cell
                float y = MAP_HEIGHT - GRID_OFFSET_Y - (row + 1) * CELL_HEIGHT;
                grid[row][col] = new Rectangle(x, y, CELL_WIDTH, CELL_HEIGHT);
            }
        }
    }

    @Override
    public boolean keyDown (int keycode) {
        return false;
    }

    @Override
    public boolean keyUp (int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped (char character) {
        return false;
    }

    @Override
    public boolean touchDown (int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp (int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled (int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged (int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved (int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled (float amountX, float amountY) {
        return false;
    }

    private enum CameraAnchor {
        LEFT(VIEW_WIDTH / 2f),
        CENTER(200f + VIEW_WIDTH / 2f),
        RIGHT(MAP_WIDTH - VIEW_WIDTH / 2f);

        private final float centerX;

        CameraAnchor (float centerX) {
            this.centerX = centerX;
        }
    }

    private enum CameraState {
        INTRO,
        PLAY,
        OUTRO
    }
}
