package work.foofish.pvz.almanac;

import work.foofish.pvz.utils.AssetPaths;

/**
 * 图鉴中的僵尸类型枚举
 */
public enum AlmanacZombieType {
    NORMAL_ZOMBIE("almanac/zombie/normal_zombie.json", AssetPaths.REGION_NORMAL_ZOMBIE_WALK, 0.9f, 0.5f, 0.5f),
    CONEHEAD_ZOMBIE("almanac/zombie/conehead_zombie.json", AssetPaths.REGION_CONEHEAD_ZOMBIE_WALK, 0.9f, 0.5f, 0.55f),
    BUCKETHEAD_ZOMBIE("almanac/zombie/buckethead_zombie.json", AssetPaths.REGION_BUCKETHEAD_ZOMBIE_WALK, 0.9f, 0.5f, 0.55f);

    private final String jsonPath;
    private final String animationRegion;
    private final float scale;
    private final float xCenterPercent;
    private final float yCenterPercent;

    AlmanacZombieType(String jsonPath, String animationRegion, float scale, float xCenterPercent, float yCenterPercent) {
        this.jsonPath = jsonPath;
        this.animationRegion = animationRegion;
        this.scale = scale;
        this.xCenterPercent = xCenterPercent;
        this.yCenterPercent = yCenterPercent;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public String getAnimationRegion() {
        return animationRegion;
    }

    public float getScale() {
        return scale;
    }

    public float getXCenterPercent() {
        return xCenterPercent;
    }

    public float getYCenterPercent() {
        return yCenterPercent;
    }
}
