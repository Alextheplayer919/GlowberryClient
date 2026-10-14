package org.chimeramc.client.core.cosmetics;

import java.util.ArrayList;
import java.util.List;

/**
 * Formats the live native-render values into a short on-screen list, so a device can watch the
 * draw path while in third person.
 *
 * <p>Pure and Android-free: it takes the raw readings and returns plain strings, so the wording is
 * unit-tested without a device. An unknown reading renders as "unknown", never as a real value —
 * a "0 draws" would read as a real measurement.
 */
public final class RenderDebugLines {

    private RenderDebugLines() {
    }

    /** The raw readings, as gathered from {@code CosmeticsDiagnosticsReport.Inputs}. */
    public static final class Values {
        public boolean nativeLoaded;
        public int renderSlot = -1;
        public boolean hookInstalled;
        public int framesSeen = -1;
        public boolean rendererDrawable;
        public int glBackend;
        public boolean eglContextCurrent;
        public int drawsThisSession;
        public int lastError;
        public int geometryBytes = -1;
        public int pieceAtlasMask = -1;
    }

    /** Builds the short list of lines to draw. */
    public static List<String> lines(Values v) {
        Values val = v == null ? new Values() : v;
        List<String> out = new ArrayList<>();
        out.add("native: " + yesNo(val.nativeLoaded));
        out.add("slot: " + (val.renderSlot < 0 ? "unknown" : String.valueOf(val.renderSlot)));
        out.add("hook: " + yesNo(val.hookInstalled));
        out.add("frames: " + count(val.framesSeen));
        out.add("drawable: " + yesNo(val.rendererDrawable));
        out.add("GL: " + backend(val.glBackend));
        out.add("EGL: " + yesNo(val.eglContextCurrent));
        out.add("draws: " + val.drawsThisSession);
        out.add("GL error: " + (val.lastError <= 0 ? "none" : "0x" + Integer.toHexString(val.lastError)));
        out.add("geometry: " + count(val.geometryBytes) + " B");
        out.add("atlases: " + mask(val.pieceAtlasMask));
        out.add("3rd person: required");
        return out;
    }

    private static String yesNo(boolean v) {
        return v ? "yes" : "no";
    }

    private static String count(int v) {
        return v < 0 ? "unknown" : String.valueOf(v);
    }

    private static String backend(int v) {
        return RenderBackend.label(RenderBackend.fromNativeCode(v));
    }

    private static String mask(int v) {
        if (v < 0) return "unknown";
        StringBuilder b = new StringBuilder();
        if ((v & 1) != 0) b.append("cape ");
        if ((v & 2) != 0) b.append("hat ");
        if ((v & 4) != 0) b.append("pet ");
        return b.length() == 0 ? "none" : b.toString().trim();
    }
}
