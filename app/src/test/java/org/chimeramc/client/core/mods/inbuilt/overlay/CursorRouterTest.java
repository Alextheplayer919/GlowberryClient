package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the routing rule: while a menu is open the controller input is consumed and never reaches
 * the game; while it is closed the game handles it normally. A leak here is the "camera spun while
 * the wheel was open" bug.
 */
public class CursorRouterTest {

    private static final class FakeSink implements CursorRouter.GameSink {
        int clicks;
        float lastX;
        float lastY;

        @Override
        public void click(float x, float y) {
            clicks++;
            lastX = x;
            lastY = y;
        }
    }

    @Test
    public void aClosedMenuDoesNotConsumeStickInput() {
        FakeSink sink = new FakeSink();
        CursorRouter router = new CursorRouter(new CursorController(1000, 600, 1f), sink);
        router.setMenuOpen(false);
        assertFalse("the game must handle the stick when no menu is open",
                router.offerStick(1f, 0f, 0.1f, false, false));
    }

    @Test
    public void anOpenMenuConsumesStickInputAndNeverLeaksIt() {
        FakeSink sink = new FakeSink();
        CursorRouter router = new CursorRouter(new CursorController(1000, 600, 1f), sink);
        router.setMenuOpen(true);
        assertTrue("an open menu must consume the stick", router.offerStick(1f, 0f, 0.1f, false, false));
        // The stick moved the cursor, and no game click happened.
        assertTrue(router.cursor().x() > 500f);
        assertEquals("no input may reach the game while the menu is open", 0, sink.clicks);
    }

    @Test
    public void buttonsAreConsumedOnlyWhileOpen() {
        CursorRouter router = new CursorRouter(new CursorController(1000, 600, 1f), new FakeSink());
        router.setMenuOpen(false);
        assertFalse(router.offerButton(CursorRouter.Button.SELECT));
        router.setMenuOpen(true);
        assertTrue(router.offerButton(CursorRouter.Button.SELECT));
        assertFalse("NONE is never consumed", router.offerButton(CursorRouter.Button.NONE));
    }

    @Test
    public void clickThroughIsRefusedWhileTheMenuIsOpen() {
        FakeSink sink = new FakeSink();
        CursorRouter router = new CursorRouter(new CursorController(1000, 600, 1f), sink);
        router.setMenuOpen(true);
        router.clickThrough();
        assertEquals("an open menu must not click the game", 0, sink.clicks);

        router.setMenuOpen(false);
        router.clickThrough();
        assertEquals(1, sink.clicks);
    }

    @Test
    public void clickThroughForwardsTheCursorPoint() {
        FakeSink sink = new FakeSink();
        CursorController cursor = new CursorController(1000, 600, 1f);
        cursor.setPosition(120f, 340f);
        CursorRouter router = new CursorRouter(cursor, sink);
        router.clickThrough();
        assertEquals(120f, sink.lastX, 1e-3f);
        assertEquals(340f, sink.lastY, 1e-3f);
    }

    @Test
    public void aNullSinkDoesNotThrow() {
        CursorRouter router = new CursorRouter(new CursorController(1000, 600, 1f), null);
        router.clickThrough();  // must not throw
        assertTrue(true);
    }
}
