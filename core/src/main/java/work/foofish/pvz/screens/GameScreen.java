package work.foofish.pvz.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import work.foofish.pvz.AssetService;
import work.foofish.pvz.PvzGame;
import work.foofish.pvz.entities.Sun;
import work.foofish.pvz.entities.plants.BasePlant;
import work.foofish.pvz.entities.plants.Sunflower;
import work.foofish.pvz.utils.AssetPaths;

import java.util.ArrayList;
import java.util.List;

public class GameScreen implements Screen, InputProcessor {
    private static final float MAP_WIDTH = 1400f;
    private static final float MAP_HEIGHT = 600f;
    private static final float VIEW_WIDTH = 900f;
    private static final float VIEW_HEIGHT = 600f;
    private static final float INTRO_DURATION = 1.75f;
    private static final float OUTRO_DURATION = 1.25f;

    // 网格配置
    private static final float CELL_WIDTH = 80f;
    private static final float CELL_HEIGHT = 95f;
    private static final float GRID_OFFSET_X = 260f; // 从地图左侧到网格左侧的距离
    private static final float GRID_OFFSET_Y = 85f;  // 从地图顶部到网格顶部的距离

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
    private final TextureRegion chooserBackground;
    private final Rectangle[][] grid = new Rectangle[5][9];
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private GlyphLayout layout;

    // 阳光货币系统
    private int sunCount = 50;

    private final List<BasePlant> plants = new ArrayList<>();
    private final List<Sun> suns = new ArrayList<>();
    private final List<FlyingSun> flyingSuns = new ArrayList<>();

    // Plant Selection
    private TextureAtlas cardAtlas;
    private TextureRegion ghostPlantRegion;
    private final List<SeedCard> seedCards = new ArrayList<>();
    private SeedCard selectedSeedCard = null;

    private InputMultiplexer inputMultiplexer;

    private CameraAnchor currentAnchor = CameraAnchor.RIGHT;
    private CameraAnchor targetAnchor = CameraAnchor.RIGHT;
    private CameraState cameraState = CameraState.INTRO;
    private boolean cameraTransitionActive;
    private float transitionStartX;
    private float transitionTargetX;
    private float transitionTimer;
    private float transitionDuration = INTRO_DURATION;

    // 天空掉落阳光计时器
    private float sunSpawnTimer = 0f;
    private float nextSunSpawnTime = 5f; // 初始5秒后掉落第一个阳光

    public GameScreen (PvzGame game) {
        this.game = game;
        this.assets = game.getAssets();
        // 使用 FitViewport 保持与游戏世界一致的缩放
        this.uiStage = new Stage(new FitViewport(VIEW_WIDTH, VIEW_HEIGHT));
        this.shapeRenderer = new ShapeRenderer();

        this.uiAtlas = this.assets.getAtlas(AssetPaths.MAP_ATLAS);
        TextureRegion bgRegion = null;
        if (uiAtlas != null) {
            bgRegion = uiAtlas.findRegion(AssetPaths.REGION_SIMPLE_DAY);
        }
        this.mapBackground = bgRegion;

        // 加载UI图集中的chooser背景
        TextureAtlas uiAtlasForChooser = this.assets.getAtlas(AssetPaths.UI_ATLAS);
        this.chooserBackground = uiAtlasForChooser.findRegion(AssetPaths.REGION_CHOOSER);

        // Load Card Atlas and Plant Atlas for Ghost
        this.cardAtlas = this.assets.getAtlas(AssetPaths.CARD_ATLAS);
        TextureAtlas plantsAtlas = this.assets.getAtlas(AssetPaths.PLANTS_ATLAS);
        if (plantsAtlas != null) {
            this.ghostPlantRegion = plantsAtlas.findRegion(AssetPaths.REGION_SUNFLOWER_NORMAL);
        }

        // Initialize Seed Cards
        if (cardAtlas != null) {
            // Sunflower Card
            seedCards.add(new SeedCard(
                cardAtlas.findRegion(AssetPaths.REGION_CARD_SUNFLOWER),
                50,
                "Sunflower",
                Sunflower.class
            ));
        }

        // 创建字体用于显示阳光数量
        this.font = new BitmapFont(); // 使用默认字体
        this.font.setColor(Color.BLACK);
        this.font.getData().setScale(1.2f); // 放大字体

        initGrid();
        // Removed default sunflower adding to allow player to plant
        // for (int i = 0; i < 5; i++) {
        //     addPlant(new Sunflower(this, grid[i][i].x, grid[i][i].y));
        // }

        snapCameraTo(currentAnchor);
    }

    public void addPlant(BasePlant plant) {
        plants.add(plant);
    }

    public void addSun(Sun sun) {
        suns.add(sun);
    }

    /**
     * 增加玩家的阳光货币
     * @param amount 增加的数量
     */
    public void addSun(int amount) {
        sunCount += amount;
        System.out.println("Sun collected! Total: " + sunCount);
    }

    private void spawnSkySun() {
        // 随机X坐标：在网格范围内 (260 到 260+720)
        // 减去阳光宽度(约50)以防超出
        float minX = GRID_OFFSET_X;
        float maxX = GRID_OFFSET_X + 9 * CELL_WIDTH - 50f;
        float x = MathUtils.random(minX, maxX);

        // 起始Y坐标：屏幕上方
        float startY = MAP_HEIGHT + 50f;

        Sun sun = new Sun(this, x, startY);
        sun.setFalling(true);
        // 随机目标高度：40 到 450 之间 (Y=0是底部, Y=600是顶部)
        // 之前用户提到的 100-700 范围中，大于 600 的值会导致阳光停在屏幕外或顶部
        sun.setTargetY(MathUtils.random(40f, 450f));

        addSun(sun);
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

        // 更新天空掉落阳光的计时器
        if (cameraState == CameraState.PLAY) {
            sunSpawnTimer += delta;
            if (sunSpawnTimer >= nextSunSpawnTime) {
                spawnSkySun();
                sunSpawnTimer = 0f;
                nextSunSpawnTime = MathUtils.random(2f, 3f); // 随机间隔 8-15 秒
            }

            // Update Seed Cards Cooldown
            for (SeedCard card : seedCards) {
                card.update(delta);
            }
        }

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

        // 更新并绘制植物
        for (int i = plants.size() - 1; i >= 0; i--) {
            BasePlant plant = plants.get(i);
            plant.update(delta);
            plant.draw(batch);
            if (!plant.isAlive()) {
                plants.remove(i);
            }
        }

        // 更新并绘制阳光
        for (int i = suns.size() - 1; i >= 0; i--) {
            Sun sun = suns.get(i);
            sun.update(delta);
            sun.draw(batch);
            if (!sun.isActive()) {
                suns.remove(i);
            }
        }

        batch.end();

        // 使用UI相机渲染顶部植物选择栏和阳光计数
        batch.setProjectionMatrix(uiStage.getCamera().combined);
        batch.begin();



        // 计算 chooser 的位置
        // 游戏网格左侧的世界坐标 X = 260
        // 游戏开始时（Play状态），相机左侧的世界坐标 X = 200 (CENTER - VIEW_WIDTH/2 = 650 - 450 = 200)
        // 所以网格相对于屏幕左侧的偏移是 260 - 200 = 60
        float chooserX = 30f;

        float originalHeight = chooserBackground != null ? chooserBackground.getRegionHeight() : 87f;
        float originalWidth = chooserBackground != null ? chooserBackground.getRegionWidth() : 450f;

        float newHeight = originalHeight + 10f;
        float scale = newHeight / originalHeight;
        float newWidth = originalWidth * scale;

        float chooserY = VIEW_HEIGHT - newHeight; // 顶部对齐

        // 绘制chooser背景在屏幕顶部
        if (chooserBackground != null) {
            batch.draw(chooserBackground, chooserX, chooserY, newWidth, newHeight);
        }

        // Draw Seed Cards
        // User specified: Card size is 54*68
        float cardWidth = 54f * scale;
        float cardHeight = 68f * scale;
        // Slot spacing based on debug renderer (64 * scale)
        float slotSpacing = 64f * scale;
        // First card starts at 80px from chooser left edge
        float firstCardOffsetX = 78f * scale;
        // Vertical offset: Center in the chooser height
        // newHeight is the chooser height.
        float slotOffsetY = (newHeight - cardHeight) / 2f;

        for (int i = 0; i < seedCards.size(); i++) {
            SeedCard card = seedCards.get(i);
            float slotX = chooserX + firstCardOffsetX + i * slotSpacing;
            float slotY = chooserY + slotOffsetY;

            // Update bounds for click detection
            card.setBounds(slotX, slotY, cardWidth, cardHeight);

            // Tint if your sun cannot afford
            if (sunCount < card.cost) {
                batch.setColor(0.5f, 0.5f, 0.5f, 1f);
            } else {
                batch.setColor(Color.WHITE);
            }

            if (card.region != null) {
                batch.draw(card.region, slotX, slotY, cardWidth, cardHeight);
            }

            // Reset color
            batch.setColor(Color.WHITE);
        }

        // 更新并绘制飞行的阳光 (在 Chooser 之上)
        for (int i = flyingSuns.size() - 1; i >= 0; i--) {
            FlyingSun fs = flyingSuns.get(i);
            fs.time += delta;
            float progress = Math.min(1f, fs.time / fs.duration);
            // 使用平滑插值
            float alpha = Interpolation.pow2Out.apply(progress);

            float currentX = MathUtils.lerp(fs.startX, fs.targetX, alpha);
            float currentY = MathUtils.lerp(fs.startY, fs.targetY, alpha);

            fs.sun.getPosition().set(currentX, currentY);
            fs.sun.update(delta); // 更新动画帧

            // 动画最后 30% 的时间进行透明度渐变
            float opacity = 1f;
            if (progress > 0.7f) {
                opacity = 1f - (progress - 0.7f) / 0.3f;
            }

            // 设置透明度
            batch.setColor(1f, 1f, 1f, opacity);
            fs.sun.draw(batch);
            // 恢复不透明
            batch.setColor(Color.WHITE);

            if (progress >= 1f) {
                addSun(25);
                flyingSuns.remove(i);
            }
        }

        // 显示阳光数量
        if (font != null) {
            String sunString = String.valueOf(sunCount);
            // 使用 GlyphLayout 计算文本宽高
            if (layout == null) layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
            layout.setText(font, sunString);

            // 用户提供的原始像素坐标 (相对于原始图片左上角)
            float origBoxX = 10f;
            float origBoxY = 60f;
            float origBoxW = 55f;
            float origBoxH = 20f;

            // 转换为屏幕坐标
            // chooserX, chooserY 是 chooser 在屏幕上的左下角坐标
            float boxScreenX = chooserX + origBoxX * scale;
            // LibGDX y轴向上，原始图片y轴向下
            // 盒子底部相对于图片底部的距离 = originalHeight - (origBoxY + origBoxH)
            float boxScreenY = chooserY + (originalHeight - origBoxY - origBoxH) * scale;
            float boxScreenWidth = origBoxW * scale;
            float boxScreenHeight = origBoxH * scale;

            // 居中计算
            float textX = boxScreenX + (boxScreenWidth - layout.width) / 2f;
            // 字体绘制的Y是基线，通常加上一半高度可以垂直居中
            // 向下移动 2px
            float textY = boxScreenY + (boxScreenHeight + layout.height) / 2f - 2f;

            font.draw(batch, sunString, textX, textY);
        }

        // Draw Ghost Plant if planting
        // Draw Ghost Plant if planting
        if (selectedSeedCard != null && ghostPlantRegion != null) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.input.getY();

            // Convert mouse screen coordinates to world coordinates for grid checking
            Vector3 worldMouse = worldViewport.unproject(new Vector3(mouseX, mouseY, 0));

            float drawX = -9999; // Default to off-screen if logic fails
            float drawY = -9999;
            boolean snapped = false;

            // Check if mouse is over any grid cell
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 9; col++) {
                    Rectangle cell = grid[row][col];
                    if (cell.contains(worldMouse.x, worldMouse.y)) {
                        drawX = cell.x;
                        drawY = cell.y;
                        snapped = true;
                        break;
                    }
                }
                if (snapped) break;
            }

            // If not snapped to grid, follow the mouse (convert screen to world for drawing)
            if (!snapped) {
                drawX = worldMouse.x - ghostPlantRegion.getRegionWidth() / 2f;
                drawY = worldMouse.y - ghostPlantRegion.getRegionHeight() / 2f;
            }

            // Draw the ghost plant
            // Note: We are inside a batch block using uiStage camera currently (lines 235-378)
            // BUT the grid coordinates are in World space.
            // We should probably draw the ghost plant in World space if it's snapped to the grid,
            // or convert World coords to UI coords.
            // Given the structure, it's easier to end the UI batch, begin a World batch, draw ghost, end World batch, restart UI batch.
            // OR just project the world coordinates to UI coordinates.

            batch.end(); // End UI batch

            batch.setProjectionMatrix(worldCamera.combined);
            batch.begin(); // Begin World batch

            batch.setColor(1f, 1f, 1f, 0.5f); // Semi-transparent
            batch.draw(ghostPlantRegion, drawX, drawY);
            batch.setColor(Color.WHITE);

            batch.end(); // End World batch

            batch.setProjectionMatrix(uiStage.getCamera().combined);
            batch.begin(); // Resume UI batch for remaining UI elements (if any)
        }

        batch.end();

        // 绘制UI网格边框 (chooser slots)
        shapeRenderer.setProjectionMatrix(uiStage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.5f, 0.2f, 0f, 1f); // 深棕色

        float slotWidth = 64 * scale; // 缩放 slot 宽度
        float slotHeight = newHeight;

        for (int i = 0; i < 8; i++) {
            float x = chooserX + i * slotWidth;
            float y = chooserY;
            shapeRenderer.rect(x, y, slotWidth, slotHeight);
        }
        shapeRenderer.end();

        // Draw Cooldown Overlay (using ShapeRenderer)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiStage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        for (SeedCard card : seedCards) {
            if (card.cooldownTimer > 0) {
                float ratio = card.cooldownTimer / card.cooldownMax;
                shapeRenderer.rect(card.x, card.y, card.width, card.height * ratio);
            }
        }
        shapeRenderer.end();


        // 绘制网格边框 - 使用轻微的内缩以清晰显示每个单元格的边界
        // 这样可以避免相邻单元格的边框重叠，使视觉边界与点击检测边界一致
        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 0, 0, 1); // 红色
        float borderInset = 0.5f; // 边框内缩0.5像素，使边界更清晰
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                Rectangle rect = grid[row][col];
                // 绘制稍微内缩的边框，这样可以清楚地看到每个格子的实际范围
                shapeRenderer.rect(
                    rect.x + borderInset,
                    rect.y + borderInset,
                    rect.width - 2 * borderInset,
                    rect.height - 2 * borderInset
                );
            }
        }
        shapeRenderer.end();

        // 绘制网格索引标签用于调试
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        if (font != null) {
            font.setColor(Color.YELLOW); // 黄色标签
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 9; col++) {
                    Rectangle rect = grid[row][col];
                    String label = row + "," + col;
                    GlyphLayout labelLayout = new GlyphLayout(font, label);
                    // 在每个格子中心绘制标签
                    font.draw(batch, label,
                        rect.x + (rect.width - labelLayout.width) / 2f,
                        rect.y + (rect.height + labelLayout.height) / 2f);
                }
            }
            font.setColor(Color.BLACK); // 恢复原来的颜色
        }
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
        shapeRenderer.dispose();
        if (font != null) {
            font.dispose();
        }
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
     */
    private void initGrid () {
        System.out.println("[DEBUG] Initializing grid...");
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                float x = GRID_OFFSET_X + col * CELL_WIDTH;
                // 相对于左上角计算 y 坐标：
                // 地图高度 - 顶部偏移 - (行索引 + 1) * 单元格高度
                // 这使得 grid[0][0] 成为左上角的单元格
                float y = MAP_HEIGHT - GRID_OFFSET_Y - (row + 1) * CELL_HEIGHT;
                grid[row][col] = new Rectangle(x, y, CELL_WIDTH, CELL_HEIGHT);
                if (row == 0 && col < 3) { // Only print first few cells to avoid spam
                    System.out.printf("[DEBUG] grid[%d][%d]: x=%.2f-%.2f, y=%.2f-%.2f%n",
                        row, col, x, x + CELL_WIDTH, y, y + CELL_HEIGHT);
                }
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
        // 将屏幕坐标转换为世界坐标
        // 使用 viewport.unproject 而不是 camera.unproject，以正确处理视口偏移和缩放
        Vector3 worldCoords = worldViewport.unproject(new Vector3(screenX, screenY, 0));

        // 检查是否点击了任何阳光
        for (int i = suns.size() - 1; i >= 0; i--) {
            Sun sun = suns.get(i);
            if (sun.isActive() && sun.getBounds().contains(worldCoords.x, worldCoords.y)) {
                sun.setCollected(true); // 标记为已收集，防止消失或下落

                // 计算屏幕坐标 (UI Stage Coordinates)
                Vector3 pos = new Vector3(sun.getPosition().x, sun.getPosition().y, 0);
                worldViewport.project(pos); // 转换为屏幕像素坐标 (Y向上)
                pos.y = Gdx.graphics.getHeight() - pos.y; // 翻转Y轴以匹配 unproject 的输入要求 (Y向下)
                uiStage.getViewport().unproject(pos); // 转换为UI视口坐标

                // 计算目标位置 (Chooser 阳光图标中心)
                float chooserX = 30f;
                float originalHeight = chooserBackground != null ? chooserBackground.getRegionHeight() : 87f;
                float newHeight = originalHeight + 10f;
                float scale = newHeight / originalHeight;
                float chooserY = VIEW_HEIGHT - newHeight;

                // 目标中心点 (相对于 Chooser 左上角 39, 33)
                float targetCenterX = chooserX + 39f * scale;
                float targetCenterY = (chooserY + newHeight) - 33f * scale;

                // 阳光绘制坐标 (中心对齐)
                float sunW = sun.getBounds().width;
                float sunH = sun.getBounds().height;
                float targetX = targetCenterX - sunW / 2f;
                float targetY = targetCenterY - sunH / 2f;

                flyingSuns.add(new FlyingSun(sun, pos.x, pos.y, targetX, targetY));
                suns.remove(i); // 从世界列表中移除

                return true; // 消耗此事件
            }
        }

        // Check Card Clicks (UI Coordinates)
        Vector3 uiCoords = new Vector3(screenX, screenY, 0);
        uiStage.getViewport().unproject(uiCoords);

        for (SeedCard card : seedCards) {
            if (card.contains(uiCoords.x, uiCoords.y)) {
                // If clicking the already selected card, deselect it
                if (selectedSeedCard == card) {
                    selectedSeedCard = null;
                    return true;
                }
                // Otherwise select the new card if affordable and ready
                if (sunCount >= card.cost && card.cooldownTimer <= 0) {
                    selectedSeedCard = card;
                    return true;
                }
            }
        }

        // Handle Planting (World Coordinates)
        if (selectedSeedCard != null) {
            // Cancel if right click
            if (button == 1) { // Right mouse button
                selectedSeedCard = null;
                return true;
            }

            // Check if clicked on a valid grid cell
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 9; col++) {
                    Rectangle cell = grid[row][col];
                    if (cell.contains(worldCoords.x, worldCoords.y)) {
                        // Check if cell is empty (simple check: no plant center in cell)
                        boolean occupied = false;
                        for (BasePlant p : plants) {
                            // Simple collision check using bounds
                            Rectangle pBounds = p.getBounds();
                            float pCenterX = pBounds.x + pBounds.width / 2f;
                            float pCenterY = pBounds.y + pBounds.height / 2f;
                            if (cell.contains(pCenterX, pCenterY)) {
                                occupied = true;
                                break;
                            }
                        }

                        if (!occupied) {
                            // Plant it!
                            if (selectedSeedCard.plantName.equals("Sunflower")) {
                                addPlant(new Sunflower(this, cell.x, cell.y));
                            }
                            // Deduct sun and start cooldown
                            sunCount -= selectedSeedCard.cost;
                            selectedSeedCard.cooldownTimer = selectedSeedCard.cooldownMax;
                            selectedSeedCard = null; // Deselect after planting
                            return true;
                        }
                    }
                }
            }

            // If clicked elsewhere, maybe cancel? Or keep selected?
            // Usually in PvZ, right click cancels, left click elsewhere does nothing or cancels.
            // Let's keep it selected unless right click.
        }

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

    private static class FlyingSun {
        Sun sun;
        float startX, startY;
        float targetX, targetY;
        float time;
        float duration = 0.7f; // 飞行时间

        public FlyingSun(Sun sun, float startX, float startY, float targetX, float targetY) {
            this.sun = sun;
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.time = 0;
        }
    }

    private enum CameraState {
        INTRO,
        PLAY,
        OUTRO
    }
    private static class SeedCard {
        TextureRegion region;
        float x, y, width, height;
        int cost;
        float cooldownMax = 7.5f;
        float cooldownTimer = 0f;
        String plantName;
        Class<? extends BasePlant> plantType;

        public SeedCard(TextureRegion region, int cost, String plantName, Class<? extends BasePlant> plantType) {
            this.region = region;
            this.cost = cost;
            this.plantName = plantName;
            this.plantType = plantType;
        }

        public void update(float delta) {
            if (cooldownTimer > 0) {
                cooldownTimer -= delta;
                if (cooldownTimer < 0) cooldownTimer = 0;
            }
        }

        public void setBounds(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean contains(float x, float y) {
            return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
        }
    }
}
