package work.foofish.pvz.almanac;

import work.foofish.pvz.utils.AssetPaths;

/**
 * 图鉴中的植物类型枚举
 */
public enum AlmanacPlantType {
    PEASHOOTER("almanac/plant/peashooter.json", AssetPaths.REGION_PEASHOOTER, AssetPaths.REGION_CARD_PEASHOOTER),
    SUNFLOWER("almanac/plant/sunflower.json", AssetPaths.REGION_SUNFLOWER_NORMAL, AssetPaths.REGION_CARD_SUNFLOWER),
    SNOW_PEA("almanac/plant/snowpea.json", AssetPaths.REGION_SNOWPEA, AssetPaths.REGION_CARD_SNOWPEA),
    REPEATER_PEA("almanac/plant/repeaterpea.json", AssetPaths.REGION_REPEATERPEA, AssetPaths.REGION_CARD_REPEATERPEA),
    WALLNUT("almanac/plant/wallnut.json", AssetPaths.REGION_WALLNUT_NORMAL, AssetPaths.REGION_CARD_WALLNUT),
    CHERRY_BOMB("almanac/plant/cherrybomb.json", AssetPaths.REGION_CHERRY_BOMB, AssetPaths.REGION_CARD_CHERRY_BOMB);

    private final String jsonPath;
    private final String animationRegion;
    private final String cardRegion;

    AlmanacPlantType(String jsonPath, String animationRegion, String cardRegion) {
        this.jsonPath = jsonPath;
        this.animationRegion = animationRegion;
        this.cardRegion = cardRegion;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public String getAnimationRegion() {
        return animationRegion;
    }

    public String getCardRegion() {
        return cardRegion;
    }
}
