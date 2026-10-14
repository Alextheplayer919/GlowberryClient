package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The seam through which a controller drives an in-game menu: a stick moves a cursor, a button
 * activates the element under it, and — while the menu is open — <b>no</b> stick or button input
 * reaches the game.
 *
 * <p>The game-side effect is an interface so the routing rule ("the menu consumes the input; the
 * game sees nothing") is unit-tested with a fake sink instead of a device. A real implementation
 * forwards a synthesised tap to the game only when the menu is closed.
 */
public final class CursorRouter {

    /** The game-side action a menu can trigger (a synthesised click on a screen point). */
    public interface GameSink {
        /** Called with a screen point when the menu chooses to click through to the game. */
        void click(float x, float y);
    }

    private final CursorController cursor;
    private final GameSink gameSink;

    /** True while a menu is open and therefore consuming controller input. */
    private boolean menuOpen;

    public CursorRouter(CursorController cursor, GameSink gameSink) {
        this.cursor = cursor;
        this.gameSink = gameSink;
    }

    /** Opens/closes the menu. While open, controller input is consumed and never reaches the game. */
    public void setMenuOpen(boolean open) {
        this.menuOpen = open;
    }

    public boolean isMenuOpen() {
        return menuOpen;
    }

    public CursorController cursor() {
        return cursor;
    }

    /**
     * Offers one frame of controller input to the menu.
     *
     * @return true when the menu consumed it (so the caller must NOT forward it to the game);
     *         false when the menu is closed and the game should handle the input normally.
     */
    public boolean offerStick(float stickX, float stickY, float dtSeconds, boolean precision,
                              boolean fast) {
        if (!menuOpen) return false;
        cursor.update(stickX, stickY, dtSeconds, precision, fast, CursorController.DEFAULT_MAX_SPEED_DP);
        return true;
    }

    /**
     * Offers a menu button press. Returns true when the menu consumed it.
     *
     * @param button one of {@link Button}
     */
    public boolean offerButton(Button button) {
        return menuOpen && button != Button.NONE;
    }

    /**
     * Asks the menu to click the game at the cursor. Only ever forwards when the menu is closed, so
     * an open menu can never leak a click to the game.
     */
    public void clickThrough() {
        if (menuOpen) return;
        if (gameSink != null) gameSink.click(cursor.x(), cursor.y());
    }

    /** The menu buttons the cursor understands. */
    public enum Button {
        NONE, SELECT, BACK, PIN, DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT, CYCLE_NEXT, CYCLE_PREV
    }
}
