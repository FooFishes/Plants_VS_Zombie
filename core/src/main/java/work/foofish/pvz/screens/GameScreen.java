package work.foofish.pvz.screens;

import com.badlogic.gdx.*;
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
import work.foofish.pvz.entities.bullets.PeaBullet;
import work.foofish.pvz.entities.plants.BasePlant;
import work.foofish.pvz.entities.plants.Peashooter;
import work.foofish.pvz.entities.plants.RepeaterPea;
import work.foofish.pvz.entities.plants.Sunflower;
import work.foofish.pvz.entities.zombies.BaseZombie;
import work.foofish.pvz.entities.zombies.NormalZombie;
import work.foofish.pvz.utils.AssetPaths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameScreen implements Screen, InputProcessor {
    private static final String TAG = GameScreen.class.getSimpleName();
    private static final float MAP_WIDTH = 1400f;
    private static final float MAP_HEIGHT = 600f;
    private static final float VIEW_WIDTH = 900f;
    private static final float VIEW_HEIGHT = 600f;
    private static final float INTRO_DURATION = 1.75f;
    private static final float OUTRO_DURATION = 1.25f;
    private static final int GRID_ROWS = 5;
    private static final int GRID_COLS = 9;
    private static final float SKY_SUN_INTERVAL_MIN = 8f;
    private static final float SKY_SUN_INTERVAL_MAX = 15f;

    // 网格配置
    private static final float CELL_WIDTH = 80f;
    private static final float CELL_HEIGHT = 95f;
    private static final float GRID_OFFSET_X = 260f; // 从地图左侧到网格左侧的距离
    private static final float GRID_OFFSET_Y = 85f;  // 从地图顶部到网格顶部的距离

    private final PvzGame game;
    private final AssetService assets;

    public AssetService getAssets () {
        return assets;
    }

    private final OrthographicCamera worldCamera = new OrthographicCamera();
    private final FitViewport worldViewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, worldCamera);
    private final Stage uiStage;
    private final TextureAtlas uiAtlas;
    private final TextureRegion mapBackground;
    private final TextureRegion chooserBackground;
    private final Rectangle[][] grid = new Rectangle[GRID_ROWS][GRID_COLS];
    private final Rectangle cameraBoundsCache = new Rectangle();
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private GlyphLayout layout;

    // 阳光货币系统
    private int sunCount = 50;

    private final List<BasePlant> plants = new ArrayList<>();
    private final List<Sun> suns = new ArrayList<>();
    private final List<FlyingSun> flyingSuns = new ArrayList<>();
    private final List<PeaBullet> bullets = new ArrayList<>();
    private final List<BaseZombie> zombies = new ArrayList<>();
    private final List<BaseZombie> zombiesView = Collections.unmodifiableList(zombies);

    // Plant Selection
    private final TextureAtlas cardAtlas;
    private final List<SeedCard> seedCards = new ArrayList<>();
    private SeedCard selectedSeedCard = null;
    private final GhostPlacement ghostPlacement = new GhostPlacement();

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
    private final ZombieSpawner zombieSpawner = new ZombieSpawner();
    private boolean debugOverlayEnabled;
    private final Vector3 worldTouch = new Vector3();
    private final Vector3 uiTouch = new Vector3();
    private final Vector3 tmpVec = new Vector3();

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
        TextureRegion sunflowerGhost = null;
        TextureRegion peashooterGhost = null;
        TextureRegion repeaterGhost = null;
        if (plantsAtlas != null) {
            sunflowerGhost = plantsAtlas.findRegion(AssetPaths.REGION_SUNFLOWER_NORMAL);
            peashooterGhost = plantsAtlas.findRegion(AssetPaths.REGION_PEASHOOTER);
            repeaterGhost = plantsAtlas.findRegion(AssetPaths.REGION_REPEATERPEA);
        }

        // Initialize Seed Cards
        if (cardAtlas != null) {
            seedCards.add(new SeedCard(
                cardAtlas.findRegion(AssetPaths.REGION_CARD_SUNFLOWER),
                sunflowerGhost,
                50,
                7.5f,
                0f,
                "Sunflower",
                (screen, cell, row, col) -> new Sunflower(screen, cell.x, cell.y, row, col)
            ));
            seedCards.add(new SeedCard(
                cardAtlas.findRegion(AssetPaths.REGION_CARD_PEASHOOTER),
                peashooterGhost,
                100,
                7f,
                -3f,
                "Peashooter",
                (screen, cell, row, col) -> new Peashooter(screen, cell.x, cell.y, row, col)
            ));
            seedCards.add(new SeedCard(
                cardAtlas.findRegion(AssetPaths.REGION_CARD_REPEATERPEA),
                repeaterGhost,
                200,
                7.5f,
                -3f,
                "RepeaterPea",
                (screen, cell, row, col) -> new RepeaterPea(screen, cell.x, cell.y, row, col)
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

    public void addPlant (BasePlant plant) {
        plants.add(plant);
    }

    public void addSun (Sun sun) {
        suns.add(sun);
    }

    public void addBullet (PeaBullet bullet) {
        if (bullet != null) {
            bullets.add(bullet);
        }
    }

    public void addZombie (BaseZombie zombie) {
        if (zombie != null) {
            zombies.add(zombie);
        }
    }

    /**
     * 增加玩家的阳光货币
     *
     * @param amount 增加的数量
     */
    public void addSun (int amount) {
        sunCount += amount;
        if (Gdx.app != null) {
            Gdx.app.log(TAG, "Sun collected, total=" + sunCount);
        }
    }

    private void spawnSkySun () {
        // 随机X坐标：在网格范围内 (260 到 260+720)
        // 减去阳光宽度(约50)以防超出
        float minX = GRID_OFFSET_X;
        float maxX = GRID_OFFSET_X + GRID_COLS * CELL_WIDTH - 50f;
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

    public boolean hasZombieInRow (int rowIndex) {
        for (BaseZombie zombie : zombies) {
            if (zombie.isAlive() && zombie.getRow() == rowIndex) {
                return true;
            }
        }
        return false;
    }

    public boolean hasVisibleZombieInRow (int rowIndex) {
        Rectangle cameraBounds = getCameraBounds();
        for (BaseZombie zombie : zombies) {
            if (!zombie.isAlive() || zombie.getRow() != rowIndex) {
                continue;
            }
            Rectangle detectionArea = zombie.getCollisionBounds();
            if (cameraBounds.contains(detectionArea)) {
                return true;
            }
        }
        return false;
    }

    private Rectangle getCameraBounds () {
        float left = worldCamera.position.x - VIEW_WIDTH / 2f;
        float bottom = worldCamera.position.y - VIEW_HEIGHT / 2f;
        cameraBoundsCache.set(left, bottom, VIEW_WIDTH, VIEW_HEIGHT);
        return cameraBoundsCache;
    }

    public BasePlant findPlantInRow (int rowIndex, Rectangle area) {
        for (BasePlant plant : plants) {
            if (!plant.isAlive() || plant.getRow() != rowIndex) {
                continue;
            }
            if (plant.getBounds().overlaps(area)) {
                return plant;
            }
        }
        return null;
    }

    private boolean isCellOccupied (int row, int col) {
        Rectangle cell = grid[row][col];
        for (BasePlant plant : plants) {
            if (!plant.isAlive()) {
                continue;
            }
            Rectangle pBounds = plant.getBounds();
            float centerX = pBounds.x + pBounds.width / 2f;
            float centerY = pBounds.y + pBounds.height / 2f;
            if (cell.contains(centerX, centerY)) {
                return true;
            }
        }
        return false;
    }

    public List<BaseZombie> getZombies () {
        return zombiesView;
    }

    public float getWorldWidth () {
        return MAP_WIDTH;
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
                nextSunSpawnTime = MathUtils.random(SKY_SUN_INTERVAL_MIN, SKY_SUN_INTERVAL_MAX); // 随机间隔 8-15 秒
            }
            zombieSpawner.update(delta);

            // Update Seed Cards Cooldown
            for (SeedCard card : seedCards) {
                card.update(delta);
            }
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 使用世界相机渲染地图背景
        // 先更新世界实体，确保逻辑顺序为僵尸 -> 植物 -> 子弹 -> 阳光
        for (int i = zombies.size() - 1; i >= 0; i--) {
            BaseZombie zombie = zombies.get(i);
            zombie.update(delta);
            if (!zombie.isAlive()) {
                zombies.remove(i);
            }
        }

        for (int i = plants.size() - 1; i >= 0; i--) {
            BasePlant plant = plants.get(i);
            plant.update(delta);
            if (!plant.isAlive()) {
                plants.remove(i);
            }
        }

        for (int i = bullets.size() - 1; i >= 0; i--) {
            PeaBullet bullet = bullets.get(i);
            bullet.update(delta);
            if (!bullet.isAlive()) {
                bullets.remove(i);
            }
        }

        for (int i = suns.size() - 1; i >= 0; i--) {
            Sun sun = suns.get(i);
            sun.update(delta);
            if (!sun.isActive()) {
                suns.remove(i);
            }
        }

        SpriteBatch batch = game.batch;
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        if (mapBackground != null) {
            batch.draw(mapBackground, 0f, 0f, MAP_WIDTH, MAP_HEIGHT);
        }

        for (BasePlant plant : plants) {
            plant.draw(batch);
        }

        for (BaseZombie zombie : zombies) {
            zombie.draw(batch);
        }

        for (PeaBullet bullet : bullets) {
            bullet.draw(batch);
        }

        for (Sun sun : suns) {
            sun.draw(batch);
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
        float chooserScale = 0.85f;

        float newHeight = originalHeight * chooserScale;
        float scale = chooserScale;
        float newWidth = originalWidth * chooserScale;

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
            float drawX = slotX + card.offsetX;

            // Update bounds for click detection
            card.setBounds(drawX, slotY, cardWidth, cardHeight);

            // Tint if your sun cannot afford
            if (!card.isSelectable(sunCount)) {
                batch.setColor(0.5f, 0.5f, 0.5f, 1f);
            } else {
                batch.setColor(Color.WHITE);
            }

            if (card.region != null) {
                batch.draw(card.region, drawX, slotY, cardWidth, cardHeight);
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
                addSun(fs.reward);
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

        drawGhostPlant(batch);

        batch.end();

        float slotWidth = 64 * scale; // 缩放 slot 宽度
        float slotHeight = newHeight;

        if (debugOverlayEnabled) {
            shapeRenderer.setProjectionMatrix(uiStage.getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0.5f, 0.2f, 0f, 1f); // 深棕色
            for (int i = 0; i < 8; i++) {
                float x = chooserX + i * slotWidth;
                float y = chooserY;
                shapeRenderer.rect(x, y, slotWidth, slotHeight);
            }
            shapeRenderer.end();
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiStage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (SeedCard card : seedCards) {
            if (card == selectedSeedCard) {
                shapeRenderer.setColor(1f, 1f, 1f, 0.2f);
                shapeRenderer.rect(card.x - 2f, card.y - 2f, card.width + 4f, card.height + 4f);
            }
            if (card.cooldownTimer > 0f) {
                float ratio = card.cooldownTimer / card.cooldownMax;
                float overlayHeight = card.height * ratio;
                float overlayY = card.y + card.height - overlayHeight;
                shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
                shapeRenderer.rect(card.x, overlayY, card.width, overlayHeight);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);


        if (debugOverlayEnabled) {
            // 绘制网格边框 - 使用轻微的内缩以清晰显示每个单元格的边界
            shapeRenderer.setProjectionMatrix(worldCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1, 0, 0, 1); // 红色
            float borderInset = 0.5f; // 边框内缩0.5像素，使边界更清晰
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    Rectangle rect = grid[row][col];
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
                for (int row = 0; row < GRID_ROWS; row++) {
                    for (int col = 0; col < GRID_COLS; col++) {
                        Rectangle rect = grid[row][col];
                        String label = row + "," + col;
                        GlyphLayout labelLayout = new GlyphLayout(font, label);
                        font.draw(batch, label,
                            rect.x + (rect.width - labelLayout.width) / 2f,
                            rect.y + (rect.height + labelLayout.height) / 2f);
                    }
                }
                font.setColor(Color.BLACK); // 恢复原来的颜色
            }
            batch.end();
        }

        uiStage.act(delta);
        uiStage.draw();
    }

    private void drawGhostPlant (SpriteBatch batch) {
        if (selectedSeedCard == null) {
            return;
        }
        TextureRegion ghostRegion = selectedSeedCard.getGhostRegion();
        if (ghostRegion == null) {
            return;
        }

        GhostPlacement placement = calculateGhostPlacement(ghostRegion);

        batch.end();
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        Color current = batch.getColor();
        float oldR = current.r;
        float oldG = current.g;
        float oldB = current.b;
        float oldA = current.a;

        if (placement.validPlacement) {
            batch.setColor(1f, 1f, 1f, 0.55f);
        } else {
            batch.setColor(1f, 0.4f, 0.4f, 0.4f);
        }
        batch.draw(ghostRegion, placement.drawX, placement.drawY);
        batch.setColor(oldR, oldG, oldB, oldA);

        batch.end();
        batch.setProjectionMatrix(uiStage.getCamera().combined);
        batch.begin();
    }

    private GhostPlacement calculateGhostPlacement (TextureRegion ghostRegion) {
        ghostPlacement.reset();
        worldTouch.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        worldViewport.unproject(worldTouch);

        ghostPlacement.drawX = worldTouch.x - ghostRegion.getRegionWidth() / 2f;
        ghostPlacement.drawY = worldTouch.y - ghostRegion.getRegionHeight() / 2f;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                Rectangle cell = grid[row][col];
                if (cell.contains(worldTouch.x, worldTouch.y)) {
                    ghostPlacement.drawX = cell.x;
                    ghostPlacement.drawY = cell.y;
                    ghostPlacement.snapped = true;
                    ghostPlacement.row = row;
                    ghostPlacement.col = col;
                    ghostPlacement.validPlacement = !isCellOccupied(row, col);
                    return ghostPlacement;
                }
            }
        }

        return ghostPlacement;
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
        if (Gdx.app != null) {
            Gdx.app.debug(TAG, "Initializing grid");
        }
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                float x = GRID_OFFSET_X + col * CELL_WIDTH;
                // 相对于左上角计算 y 坐标：
                // 地图高度 - 顶部偏移 - (行索引 + 1) * 单元格高度
                // 这使得 grid[0][0] 成为左上角的单元格
                float y = MAP_HEIGHT - GRID_OFFSET_Y - (row + 1) * CELL_HEIGHT;
                grid[row][col] = new Rectangle(x, y, CELL_WIDTH, CELL_HEIGHT);
                if (row == 0 && col < 3 && Gdx.app != null) {
                    Gdx.app.debug(TAG, String.format("grid[%d][%d]: x=%.2f-%.2f, y=%.2f-%.2f",
                        row, col, x, x + CELL_WIDTH, y, y + CELL_HEIGHT));
                }
            }
        }
    }

    @Override
    public boolean keyDown (int keycode) {
        if (keycode == Input.Keys.F1) {
            debugOverlayEnabled = !debugOverlayEnabled;
            if (Gdx.app != null) {
                Gdx.app.log(TAG, "Debug overlay " + (debugOverlayEnabled ? "enabled" : "disabled"));
            }
            return true;
        }
        if (keycode == Input.Keys.ESCAPE) {
            selectedSeedCard = null;
            return true;
        }
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
        worldTouch.set(screenX, screenY, 0f);
        worldViewport.unproject(worldTouch);

        // 检查是否点击了任何阳光
        for (int i = suns.size() - 1; i >= 0; i--) {
            Sun sun = suns.get(i);
            if (!sun.canBeCollected()) {
                continue;
            }
            if (sun.getBounds().contains(worldTouch.x, worldTouch.y)) {
                int reward = sun.collect();
                if (reward <= 0) {
                    continue;
                }

                // 计算屏幕坐标 (UI Stage Coordinates)
                tmpVec.set(sun.getPosition().x, sun.getPosition().y, 0f);
                worldViewport.project(tmpVec); // 转换为屏幕像素坐标 (Y向上)
                tmpVec.y = Gdx.graphics.getHeight() - tmpVec.y; // 翻转Y轴
                uiStage.getViewport().unproject(tmpVec); // 转换为UI视口坐标

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

                flyingSuns.add(new FlyingSun(sun, tmpVec.x, tmpVec.y, targetX, targetY, reward));
                suns.remove(i); // 从世界列表中移除

                return true; // 消耗此事件
            }
        }

        // Check Card Clicks (UI Coordinates)
        uiTouch.set(screenX, screenY, 0f);
        uiStage.getViewport().unproject(uiTouch);

        for (SeedCard card : seedCards) {
            if (!card.contains(uiTouch.x, uiTouch.y)) {
                continue;
            }
            if (selectedSeedCard == card) {
                selectedSeedCard = null;
                return true;
            }
            if (card.isSelectable(sunCount)) {
                selectedSeedCard = card;
                return true;
            }
            return true;
        }

        // Handle Planting (World Coordinates)
        if (selectedSeedCard != null) {
            // Cancel if right click
            if (button == 1) { // Right mouse button
                selectedSeedCard = null;
                return true;
            }

            // Check if clicked on a valid grid cell
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    Rectangle cell = grid[row][col];
                    if (cell.contains(worldTouch.x, worldTouch.y)) {
                        if (!isCellOccupied(row, col)) {
                            BasePlant plant = selectedSeedCard.createPlant(this, cell, row, col);
                            if (plant != null) {
                                addPlant(plant);
                                sunCount -= selectedSeedCard.cost;
                                selectedSeedCard.triggerCooldown();
                                selectedSeedCard = null;
                                return true;
                            }
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

    private static class GhostPlacement {
        float drawX;
        float drawY;
        boolean snapped;
        boolean validPlacement;
        int row = -1;
        int col = -1;

        void reset () {
            drawX = -9999f;
            drawY = -9999f;
            snapped = false;
            validPlacement = false;
            row = -1;
            col = -1;
        }
    }

    private static class FlyingSun {
        Sun sun;
        float startX, startY;
        float targetX, targetY;
        float time;
        float duration = 0.7f; // 飞行时间
        int reward;

        public FlyingSun (Sun sun, float startX, float startY, float targetX, float targetY, int reward) {
            this.sun = sun;
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.time = 0;
            this.reward = reward;
        }
    }

    private enum CameraState {
        INTRO,
        PLAY,
        OUTRO
    }

    private class ZombieSpawner {
        private float spawnTimer;
        private float nextSpawnTime = 5f;

        void update (float delta) {
            spawnTimer += delta;
            if (spawnTimer >= nextSpawnTime) {
                spawnTimer = 0f;
                spawnZombie();
                nextSpawnTime = MathUtils.random(4f, 7f);
            }
        }

        private void spawnZombie () {
            int row = MathUtils.random(0, grid.length - 1);
            Rectangle cell = grid[row][grid[row].length - 1];
            float spawnY = cell.y;
            float spawnX = MAP_WIDTH - 120f;
            addZombie(new NormalZombie(GameScreen.this, spawnX, spawnY, row));
        }
    }

    private static class SeedCard {
        TextureRegion region;
        TextureRegion ghostRegion;
        float x, y, width, height;
        int cost;
        float cooldownMax;
        float cooldownTimer = 0f;
        String plantName;
        PlantFactory plantFactory;
        final float offsetX;

        public SeedCard (TextureRegion region, TextureRegion ghostRegion, int cost, float cooldownMax, float offsetX, String plantName, PlantFactory plantFactory) {
            this.region = region;
            this.ghostRegion = ghostRegion;
            this.cost = cost;
            this.cooldownMax = cooldownMax;
            this.offsetX = offsetX;
            this.plantName = plantName;
            this.plantFactory = plantFactory;
        }

        public void update (float delta) {
            if (cooldownTimer > 0) {
                cooldownTimer -= delta;
                if (cooldownTimer < 0) cooldownTimer = 0;
            }
        }

        public void triggerCooldown () {
            cooldownTimer = cooldownMax;
        }

        public void setBounds (float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean contains (float x, float y) {
            return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
        }

        public boolean isSelectable (int currentSun) {
            return currentSun >= cost && cooldownTimer <= 0f;
        }

        public TextureRegion getGhostRegion () {
            return ghostRegion;
        }

        public BasePlant createPlant (GameScreen screen, Rectangle cell, int row, int col) {
            if (plantFactory == null) return null;
            return plantFactory.create(screen, cell, row, col);
        }
    }

    @FunctionalInterface
    private interface PlantFactory {
        BasePlant create (GameScreen screen, Rectangle cell, int row, int col);
    }
}
