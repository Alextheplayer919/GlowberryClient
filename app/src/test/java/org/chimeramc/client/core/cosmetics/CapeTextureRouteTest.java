package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The Vulkan-safe cape route, and its fail-closed behaviour.
 *
 * <p>The route replaces the cape texture the engine uploads, keyed on the captured vanilla cape, so
 * it works even when GLES is unavailable (a Vulkan RenderDragon build). The JVM test classpath has
 * no {@code libpreloader}, so every native call must degrade to a safe no-op: no capture, no
 * substitution, and no crash. That is exactly the path a build without the native library takes.
 */
public class CapeTextureRouteTest {

    @Test
    public void withoutTheNativeLibraryThereIsNoCapture() {
        assertFalse(NativeCosmeticsBridge.hasCapturedCape());
        assertTrue(NativeCosmeticsBridge.capeSubstitutionCount() == 0);
        // Diagnostics are null when the native library is absent, which is the honest "unknown".
        assertNull(NativeCosmeticsBridge.capeCaptureDiagnostics());
    }

    @Test
    public void publishingACapeFailsClosedWithNoEngine() {
        // Registering a cape must not throw even with no engine; it simply cannot take effect.
        CosmeticCatalog.Cape cape = CosmeticCatalog.capes().get(0);
        NativeCosmeticsBridge.publishCapeTexture(cape);
        // Clearing is likewise safe.
        NativeCosmeticsBridge.clearCapeTexture();
        NativeCosmeticsBridge.publishCapeTexture(null);
    }

    @Test
    public void theCapeTextureIsATwoToOnePanel() {
        // The engine recognises a cape by its 2:1 shape, so the painted texture must be 2:1 or the
        // substitution would never key on the local cape.
        assertTrue(CapeTexturePainter.TEXTURE_WIDTH == 2 * CapeTexturePainter.TEXTURE_HEIGHT);
    }
}
