package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Pins the account skin lookup's pure half: the skin-service response parsing and the on-disk cache.
 *
 * <p>The network and the bitmap decode are Android-side and need a device, but the two things that
 * actually break a lookup — mis-reading the service payload and losing the cached atlas — are pure
 * and pinned here. The payload shapes are literal copies of what the service returns, so a change to
 * its schema fails this test rather than silently disabling the feature.
 */
public class AccountSkinServiceTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void aTextureIdIsReadDirectlyWhenPresent() {
        String body = "{\"hash\":\"abc\",\"is_steve\":false,"
                + "\"signature\":\"sig\",\"texture_id\":\"a90790c57e181ed13aded14c47ee2f7c8d\","
                + "\"value\":\"\"}";
        assertEquals("a90790c57e181ed13aded14c47ee2f7c8d",
                AccountSkinService.parseTextureId(body));
    }

    @Test
    public void aTextureIdIsExtractedFromTheBase64ValueWhenThereIsNoTextureId() {
        String inner = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/"
                + "deadbeef00112233445566778899aabb\"}}}";
        String value = Base64.getEncoder().encodeToString(
                inner.getBytes(StandardCharsets.UTF_8));
        String body = "{\"hash\":\"h\",\"is_steve\":false,\"value\":\"" + value + "\"}";
        assertEquals("deadbeef00112233445566778899aabb",
                AccountSkinService.parseTextureId(body));
    }

    @Test
    public void anUnknownPlayerIsNotASkin() {
        // The service returns an empty object when it has never seen the xuid.
        assertNull(AccountSkinService.parseTextureId("{}"));
        assertNull(AccountSkinService.parseTextureId(""));
        assertNull(AccountSkinService.parseTextureId(null));
        assertNull(AccountSkinService.parseTextureId("not json"));
    }

    @Test
    public void aMalformedBase64ValueIsNotASkin() {
        assertNull(AccountSkinService.textureIdFromValue("!!!not base64!!!"));
        String inner = "{\"textures\":{}}";
        String value = Base64.getEncoder().encodeToString(
                inner.getBytes(StandardCharsets.UTF_8));
        assertNull("a payload with no SKIN entry must not resolve",
                AccountSkinService.textureIdFromValue(value));
    }

    @Test
    public void theTextureUrlUsesTheMojangHost() {
        String inner = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/"
                + "00112233445566778899aabbccddeeff\"}}}";
        String value = Base64.getEncoder().encodeToString(
                inner.getBytes(StandardCharsets.UTF_8));
        String body = "{\"value\":\"" + value + "\"}";
        String id = AccountSkinService.parseTextureId(body);
        assertNotNull(id);
        assertEquals(AccountSkinService.TEXTURE_HOST + "00112233445566778899aabbccddeeff",
                AccountSkinService.TEXTURE_HOST + id);
    }

    // --- Cache ------------------------------------------------------------------------------

    @Test
    public void theAccountCacheRoundTripsAndKeepsTheAccountKey() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        File root = tmp.newFolder();
        assertTrue(AccountSkinCache.write(root, new AccountSkinCache.Entry(png, "account:12345")));
        AccountSkinCache.Entry read = AccountSkinCache.read(root);
        assertNotNull(read);
        assertEquals(png.length, read.png.length);
        assertEquals("account:12345", read.accountKey);
    }

    @Test
    public void anEmptyCacheReadsAsNull() throws Exception {
        assertNull(AccountSkinCache.read(tmp.newFolder()));
    }

    @Test
    public void clearingRemovesTheCachedSkin() throws Exception {
        File root = tmp.newFolder();
        AccountSkinCache.write(root, new AccountSkinCache.Entry(new byte[] {1, 2, 3}, "account:1"));
        assertNotNull(AccountSkinCache.read(root));
        assertTrue(AccountSkinCache.clear(root));
        assertNull(AccountSkinCache.read(root));
    }

    @Test
    public void theCacheUsesItsOwnFileSoTheGameCaptureCannotClobberIt() {
        // The account PNG must live in a different directory/file than the game-skin cache, or a
        // capture would erase the account skin (and the reverse).
        assertTrue(!"skins".equals(AccountSkinCache.DIR));
        assertTrue(!"local_skin.bin".equals(AccountSkinCache.PNG_NAME));
    }
}
