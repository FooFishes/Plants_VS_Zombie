# 僵尸与豌豆射手实现计划

## 1. 目标与范围
- 在现有 `GameScreen` 架构下，实现完整的豌豆射手链路（植物实体、子弹、UI 种子卡、放置反馈、冷却/花费逻辑）。
- 引入僵尸系统：包括可扩展的僵尸基类、普通僵尸行为、与植物/子弹的交互，以及生成/波次控制的骨架。
- 预留对其他僵尸（如路障、铁桶等）和更多植物的扩展能力，避免后续大面积重构。
- 所有资源引用均使用 `work.foofish.pvz.utils.AssetPaths` 中已经定义的路径常量。

## 2. 资源清单
- 植物图集：`AssetPaths.PLANTS_ATLAS`，豌豆射手区域 `AssetPaths.REGION_PEASHOOTER`。
- 子弹图集：`AssetPaths.BULLETS_ATLAS`，普通豌豆 `AssetPaths.REGION_PEA_NORMAL` / 爆炸帧 `AssetPaths.REGION_PEA_NORMAL_EXPLODE`。
- 僵尸图集：`AssetPaths.ZOMBIES_ATLAS`，普通僵尸走路/攻击/掉头/死亡相关区域（`REGION_NORMAL_ZOMBIE_*`）均已在常量中列出。
- 卡片图集：`AssetPaths.CARD_ATLAS`，需新增豌豆射手卡片纹理常量并在 `GameScreen` 中引用。

## 3. 豌豆射手实现规划
### 3.1 类设计
- 新增 `Peashooter extends BasePlant`，与 `Sunflower` 一致放在 `work.foofish.pvz.entities.plants` 包内。
- 关键属性：攻击间隔（如 1.5s）、单发伤害、射程（按行检测最右侧僵尸即可）、是否在僵尸进入射程后才开火等。
- `action(float delta)` 中维护计时器，检查当前行是否存在僵尸：
  - `GameScreen` 需提供按行查询僵尸的能力（例如 `getZombiesInRow(int row)`）。
  - 当检测到行内僵尸并满足冷却时，生成 `PeaBullet`。

### 3.2 动画与渲染
- 使用 `AssetPaths.REGION_PEASHOOTER` 创建 `Animation<TextureRegion>`，帧率与 `Sunflower` 类似（0.1f）。
- 可根据需要添加发射瞬间帧序列或轻微缩放效果，初期可只用单套帧循环。

### 3.3 子弹系统
- 新建 `work.foofish.pvz.entities.bullets.PeaBullet`：
  - 属性：位置、速度（常量水平速度）、伤害值、所属行、存活标记、碰撞 `Rectangle`、动画（飞行/爆炸）。
  - `update(float delta)`：移动 -> 检查超出地图 -> 检查与僵尸碰撞（通过 `GameScreen` 提供的僵尸列表或行索引结构）。
  - 命中流程：触发爆炸动画（`REGION_PEA_NORMAL_EXPLODE`），播放一次并在结束后标记移除；命中瞬间对目标僵尸调用 `takeDamage(int)` 并停止穿透。
  - `draw(SpriteBatch batch)`：根据状态选择飞行或爆炸帧。
- `GameScreen` 中维护 `List<PeaBullet>`，在渲染循环里统一更新/绘制/回收。

### 3.4 UI & 交互
- 在 `AssetPaths` 添加 `REGION_CARD_PEASHOOTER`（若实际图集命名不同需确认），并在 `GameScreen` 初始化 `SeedCard` 列表时加入豌豆射手卡片。
- 设定费用（100 阳光）与冷却（7s，可配置），卡片根据 `sunCount` 和 `cooldownTimer` 自动变灰。
- 幽灵预览：在 `selectedSeedCard` 为豌豆射手时，使用 `REGION_PEASHOOTER` 区域做半透明贴图。

## 4. 僵尸系统实现规划
### 4.1 抽象层
- 新建 `work.foofish.pvz.entities.zombies.BaseZombie`：
  - 属性：血量、当前行、移动速度、攻击力、状态（Walk、Bite、LostHead、Die）、动画映射、`Rectangle bounds`。
  - `update(float delta)`：根据状态处理移动、攻击、动画时间，以及与植物的接触检测。
  - `takeDamage(int damage)`：处理掉头动画、死亡过渡，支持触发头掉落特效（可先 TODO）。
  - 针对扩展的开放点：`protected void onStateChanged(State newState)`、`protected float getAttackInterval()`、`protected void applyAttack(BasePlant target)` 等，让子类可覆写不同数值/特性。

### 4.2 普通僵尸
- `NormalZombie extends BaseZombie`：
  - 初始化动画：`REGION_NORMAL_ZOMBIE_WALK`、`REGION_NORMAL_ATTACK`、`REGION_NORMAL_ZOMBIE_DIE`、`REGION_NORMAL_ZOMBIE_LOST_HEAD`、`REGION_NORMAL_ZOMBIE_LOST_HEAD_ATTACK`、`REGION_NORMAL_ZOMBIE_HEAD`、`REGION_NORMAL_ZOMBIE_BOOM_DIE`。
  - 行走速度（约 0.5~0.7 单位/秒）、攻击伤害（每口 50）等常量。
  - 状态切换：血量低于阈值（如 100）时切入掉头状态并更换动画；血量 <= 0 时进入死亡动画并在播放结束后从列表移除。

### 4.3 行为与碰撞
- 行内移动：根据所在行的 `grid` Y 值，保持与植物对齐。
- 攻击触发：当 `bounds` 与植物 `Rectangle` 重叠一定阈值时转入 ATTACK 状态，并定时对目标植物扣血。
- 死亡处理：播放死亡动画后删除；可在后续加入掉落阳光或特效。

### 4.4 扩展预留
- 在 `BaseZombie` 中抽象以下可覆盖项：
  1. `getMaxHealth()` / `getWalkSpeed()` / `getBiteDamage()`。
  2. `getAnimation(State state)`：允许不同僵尸提供不同帧。
  3. `protected void handleSpecialDeath()`：方便路障/铁桶掉落装备或爆炸等特殊逻辑。
- `GameScreen` 持有 `List<BaseZombie>`，并提供按行索引的视图（例如 `Map<Integer, Array<BaseZombie>> zombiesByRow` 或过滤方法），供植物和子弹快速查询。
- 僵尸生成器：可添加 `ZombieSpawner`（内部类或独立类）负责定时向随机行生成 `NormalZombie`，并保留 `spawnNormalZombie(int row, float delay)` 等方法供关卡脚本调用。

## 5. 系统整合改动
- `GameScreen`：
  1. 新增 `List<BaseZombie> zombies` 与 `List<PeaBullet>` 成员，在 `render` 中分别更新/绘制，并在迭代过程中清理死亡实体。
  2. 在 `touchDown` 放置植物时记录行索引，便于豌豆射手发射子弹时知道自己所在行。
  3. 提供 `boolean hasZombieInRow(int row)`、`BaseZombie getFirstZombieInRow(int row)` 等方法供植物/子弹调用。
  4. 在世界更新阶段先更新僵尸，再更新植物/子弹，保证攻击判定顺序一致。
  5. UI：扩充 `SeedCard` 结构（如加入 `TextureRegion ghostRegion`），让后续植物只需提供素材常量即可完成卡片/幽灵绑定。
- 碰撞顺序：子弹更新 -> 对僵尸做 `bounds.overlaps` 检查 -> 命中后立刻减血；僵尸更新 -> 发现与植物碰撞 -> 切换状态/扣血；植物只在 `action` 中做自身逻辑。
- 调试：可暂时复用 `ShapeRenderer` 显示僵尸/子弹包围盒以便调试，完成后关闭。

## 6. 里程碑与实施步骤
1. **基础结构搭建**：创建 `BaseZombie`、`NormalZombie`、`PeaBullet`、`Peashooter` 类，并把 `GameScreen` 中的集合、渲染/更新管线搭好（可先用占位贴图）。
2. **资源联调**：接入真实动画帧，校准坐标与缩放；添加豌豆射手卡片/幽灵。
3. **交互实现**：
   - 豌豆射手检测同一行僵尸并发射子弹。
   - 子弹命中僵尸后播放爆炸并造成伤害。
   - 僵尸碰撞植物触发攻击，植物死亡后僵尸继续前进。
4. **生成与节奏**：实现基础僵尸生成逻辑（固定时间/波次），并与相机状态配合。
5. **扩展接口验证**：以注释或 stub 形式演示如何新增 `BucketheadZombie`，验证 `BaseZombie` 的可扩展点。
6. **测试与调优**：覆盖以下场景——多行战斗、植物死亡、子弹与僵尸大量存在时的性能、卡片冷却/费用处理。

## 7. 待确认/可选项
- 是否需要实现命中硬直（僵尸被豌豆击中短暂停顿），暂按即时扣血处理，可后续追加。
- 僵尸头部掉落、爆炸死亡等特效是否必须在首版完成，如素材缺失可先 TODO 占位。
- 豌豆射手射程计划为无限直线（与原作一致），若需限制请提前说明。

如需调整范围或数值，请告知，我会据此修正计划。
