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

public class GameScreen implements Screen, InputProcessor {
    private static final float MAP_WIDTH = 1400f;
    private static final float MAP_HEIGHT = 600f;
    private static final float VIEW_WIDTH = 900f;
    private static final float VIEW_HEIGHT = 600f;
    private static final float INTRO_DURATION = 1.75f;
    private static final float OUTRO_DURATION = 1.25f;

    private final PvzGame game;
    private final AssetService assets;
    private final OrthographicCamera worldCamera = new OrthographicCamera();
    private final FitViewport worldViewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, worldCamera);
    private final Stage uiStage;
    private final TextureAtlas uiAtlas;
    private final TextureRegion mapBackground;
    private final Rectangle[][] grid = new Rectangle[5][9];

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

        // 使用 ui.atlas 中的一张图做地图背景占位
        this.uiAtlas = this.assets.getAtlas("atlases/map.atlas");
        TextureRegion bgRegion = null;
        if (uiAtlas != null) {
            bgRegion = uiAtlas.findRegion("simple_day");
        }
        this.mapBackground = bgRegion;

        initGrid();
        snapCameraTo(currentAnchor);
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

        // TODO: 在此处绘制地图网格、植物、僵尸等世界元素（使用 grid）

        batch.end();

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
        float cellWidth = 80f;
        float cellHeight = 100f;
        float startX = 200f;
        float startY = 50f;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                float x = startX + col * cellWidth;
                float y = startY + row * cellHeight;
                grid[row][col] = new Rectangle(x, y, cellWidth, cellHeight);
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
