package work.foofish.pvz.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

/**
 * 可按压的按钮组件，按下时有位移效果
 */
public class PressableGroup extends Group {
    private static final float PRESS_OFFSET = 2f;
    private Runnable clickListener;
    private boolean isPressed;

    public PressableGroup() {
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                isPressed = true;
                moveBy(PRESS_OFFSET, -PRESS_OFFSET);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (isPressed) {
                    isPressed = false;
                    moveBy(-PRESS_OFFSET, PRESS_OFFSET);
                    if (clickListener != null && hit(x, y, true) != null) {
                        clickListener.run();
                    }
                }
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                boolean inside = hit(x, y, true) != null;
                if (isPressed && !inside) {
                    isPressed = false;
                    moveBy(-PRESS_OFFSET, PRESS_OFFSET);
                } else if (!isPressed && inside) {
                    isPressed = true;
                    moveBy(PRESS_OFFSET, -PRESS_OFFSET);
                }
            }
        });
    }

    public void setClickListener(Runnable listener) {
        this.clickListener = listener;
    }

    public Runnable getClickListener() {
        return clickListener;
    }
}
