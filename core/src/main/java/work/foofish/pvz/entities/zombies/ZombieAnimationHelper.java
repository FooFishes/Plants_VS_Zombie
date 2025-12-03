package work.foofish.pvz.entities.zombies;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

final class ZombieAnimationHelper {
    private ZombieAnimationHelper () {
    }

    static Animation<TextureRegion> ensureAnimation (TextureAtlas atlas,
                                                     String regionName,
                                                     float frameDuration,
                                                     Animation.PlayMode playMode,
                                                     Animation<TextureRegion> fallbackAnimation,
                                                     TextureRegion fallbackFrame) {
        Animation<TextureRegion> animation = createAnimation(atlas, regionName, frameDuration, playMode);
        if (animation != null) {
            return animation;
        }
        if (fallbackAnimation != null) {
            return fallbackAnimation;
        }
        if (fallbackFrame != null) {
            Animation<TextureRegion> single = new Animation<>(frameDuration, fallbackFrame);
            single.setPlayMode(playMode);
            return single;
        }
        return null;
    }

    static Animation<TextureRegion> createAnimation (TextureAtlas atlas,
                                                     String regionName,
                                                     float frameDuration,
                                                     Animation.PlayMode playMode) {
        if (atlas == null) {
            return null;
        }
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(regionName);
        if (regions != null && regions.size > 0) {
            return new Animation<>(frameDuration, regions, playMode);
        }
        TextureAtlas.AtlasRegion singleRegion = atlas.findRegion(regionName);
        if (singleRegion != null) {
            Animation<TextureRegion> animation = new Animation<>(frameDuration, singleRegion);
            animation.setPlayMode(playMode);
            return animation;
        }
        return null;
    }

    static float computeCenteringOffset (float referenceWidth, Animation<TextureRegion> animation, float scale) {
        if (animation == null || animation.getKeyFrames().length == 0) {
            return 0f;
        }
        TextureRegion firstFrame = animation.getKeyFrames()[0];
        float scaledFrameWidth = firstFrame.getRegionWidth() * scale;
        return (referenceWidth - scaledFrameWidth) / 2f;
    }

    static float getReferenceWidth (TextureRegion referenceFrame, float fallbackWidth) {
        if (referenceFrame != null) {
            return referenceFrame.getRegionWidth();
        }
        return fallbackWidth;
    }

    static float getReferenceHeight (TextureRegion referenceFrame, float fallbackHeight) {
        if (referenceFrame != null) {
            return referenceFrame.getRegionHeight();
        }
        return fallbackHeight;
    }
}
