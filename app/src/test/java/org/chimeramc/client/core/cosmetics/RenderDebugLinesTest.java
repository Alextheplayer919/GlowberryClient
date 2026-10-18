package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * Pins the live render debug wording: unknown readings must render as "unknown", never as a real
 * value, and the third-person requirement is always stated (in first person the game does not
 * render the local player model, so the draw path never runs).
 */
public class RenderDebugLinesTest {

    @Test
    public void unknownReadingsRenderAsUnknown() {
        RenderDebugLines.Values v = new RenderDebugLines.Values();
        v.nativeLoaded = false;
        v.renderSlot = -1;
        v.framesSeen = -1;
        v.geometryBytes = -1;
        v.pieceAtlasMask = -1;
        List<String> lines = RenderDebugLines.lines(v);
        String joined = String.join("\n", lines);
        assertTrue(joined.contains("slot: unknown"));
        assertTrue(joined.contains("frames: unknown"));
        assertTrue(joined.contains("geometry: unknown B"));
        assertTrue(joined.contains("atlases: unknown"));
        assertTrue(joined.contains("GL: unknown"));
    }

    @Test
    public void aLiveReadingIsShown() {
        RenderDebugLines.Values v = new RenderDebugLines.Values();
        v.nativeLoaded = true;
        v.renderSlot = 17;
        v.hookInstalled = true;
        v.framesSeen = 120;
        v.capeEngineRouteLive = true;
        v.glBackend = 1;
        v.eglContextCurrent = true;
        v.pieceAtlasMask = 0b011;
        List<String> lines = RenderDebugLines.lines(v);
        String joined = String.join("\n", lines);
        assertTrue(joined.contains("native: yes"));
        assertTrue(joined.contains("slot: 17"));
        assertTrue(joined.contains("frames: 120"));
        assertTrue(joined.contains("cape route: yes"));
        assertTrue(joined.contains("GL: GLES"));
        assertTrue(joined.contains("atlases: cape hat"));
    }

    @Test
    public void theThirdPersonRequirementIsAlwaysStated() {
        List<String> lines = RenderDebugLines.lines(new RenderDebugLines.Values());
        assertTrue(String.join("\n", lines).toLowerCase().contains("3rd person"));
    }

    @Test
    public void theLinesAreNeverEmpty() {
        List<String> lines = RenderDebugLines.lines(null);
        assertFalse(lines.isEmpty());
        for (String line : lines) {
            assertFalse(line.trim().isEmpty());
        }
    }
}
