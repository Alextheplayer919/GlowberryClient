package org.chimeramc.client.preloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;

import org.junit.Test;

/**
 * Pins the two rules-protection fixes and the per-field rule merge.
 *
 * <p>These are pure JVM tests: the manager itself touches {@code SharedPreferences}, files and the
 * network, so the decisions that were wrong (a remote overwrite dropping fields, and first-match
 * rule selection losing {@code playerRenderVtableIndex}) live in {@link PreloaderRules} where they
 * can be pinned without a device. "Verified by test" for the selection/overwrite logic; the hook
 * install itself remains verified only by code reading plus the device checklist.
 */
public class PreloaderRulesTest {

    /** A rule over a version range with the required menu/HUD signatures and full field set. */
    private static String rule(String min, String max, Integer renderSlot, boolean withMenuIndex) {
        StringBuilder b = new StringBuilder();
        b.append("{");
        if (min != null) b.append("\"min\":\"").append(min).append("\",");
        if (max != null) b.append("\"max\":\"").append(max).append("\",");
        if (withMenuIndex) b.append("\"isShowingMenuVtableIndex\":151,");
        if (renderSlot != null) b.append("\"playerRenderVtableIndex\":").append(renderSlot).append(",");
        b.append("\"sigs\":{")
                .append("\"pauseMenuDtorSig\":\"AABB\",")
                .append("\"pauseMenuOpenSig\":\"CCDD\",")
                .append("\"hudScreenDtorSig\":\"EEFF\",")
                .append("\"hudScreenOpenSig\":\"1122\",")
                .append("\"isShowingMenuSig\":\"3344\"")
                .append("}}");
        return b.toString();
    }

    private static String doc(int revision, String... rules) {
        return "{\"revision\":" + revision + ",\"rules\":[" + String.join(",", rules) + "]}";
    }

    /** The shape of the shipped file: the narrow 1.21.132..1.26.39.99 rule is written LAST. */
    private static String shippedShape() {
        return doc(5,
                rule("1.26.50.00", null, 17, true),
                rule("1.26.40.00", null, 17, true),
                rule("1.26.30.00", null, null, false),           // no render slot, no menu index
                rule("1.21.132", "1.26.39.99", 17, false));      // has the render slot
    }

    @Test
    public void aVersionWithNoDirectRuleInheritsTheSlotFromABroaderRule() {
        JsonObject merged = PreloaderRules.mergeRuleForVersion(shippedShape(), "1.26.33.1");
        assertNotNull("1.26.33.1 must match a rule", merged);
        // The first matching rule (1.26.30.00, unbounded) omits the slot; the 1.21.132..1.26.39.99
        // rule carries it. First-match selection would have passed value_or(0); the merge must not.
        assertTrue("render slot must be inherited from the narrower rule",
                PreloaderRules.hasPlayerRenderIndex(merged));
        assertEquals(17, merged.get("playerRenderVtableIndex").getAsInt());
        // In the shipped file no rule covering 1.26.33.1 declares isShowingMenuVtableIndex (they use
        // the isShowingMenuSig string instead), so the merge must not invent an index.
        assertFalse("no menu index invented when no matching rule declares one",
                merged.has("isShowingMenuVtableIndex"));
    }

    @Test
    public void theMergedSignaturesComeFromTheFirstMatchingRuleThatHasThem() {
        JsonObject merged = PreloaderRules.mergeRuleForVersion(shippedShape(), "1.26.33.1");
        JsonObject sigs = PreloaderRules.signatures(merged);
        assertNotNull(sigs);
        assertEquals("AABB", sigs.get("pauseMenuDtorSig").getAsString());
        assertEquals("3344", sigs.get("isShowingMenuSig").getAsString());
    }

    @Test
    public void selectionIsCorrectAtEveryBoundaryTheRulesWereWrittenFor() {
        String json = shippedShape();
        // 1.26.39.99 is the top of the narrow rule and the bottom of the unbounded 1.26.30 rule.
        JsonObject at3999 = PreloaderRules.mergeRuleForVersion(json, "1.26.39.99");
        assertNotNull(at3999);
        assertEquals(17, at3999.get("playerRenderVtableIndex").getAsInt());
        // 1.26.40.00 selects the 1.26.40 rule (first in file order among matching).
        JsonObject at4000 = PreloaderRules.mergeRuleForVersion(json, "1.26.40.00");
        assertNotNull(at4000);
        assertEquals(17, at4000.get("playerRenderVtableIndex").getAsInt());
        // 26.52 (the APK-derived form) must compare in the same space as 1.26.50.00.
        JsonObject at2652 = PreloaderRules.mergeRuleForVersion(json, "26.52");
        assertNotNull("26.52 must match the 1.26.50.00 rule", at2652);
        assertEquals(17, at2652.get("playerRenderVtableIndex").getAsInt());
    }

    @Test
    public void noRuleMeansNoMergedRule() {
        assertNull(PreloaderRules.mergeRuleForVersion(shippedShape(), "1.20.0"));
    }

    @Test
    public void aRemoteFileThatDropsAFieldIsRefused() {
        String bundled = shippedShape();
        // Same revision, but the narrow rule lost its render slot — the exact regression that made
        // the hook install without a slot.
        String remoteBad = doc(5,
                rule("1.26.50.00", null, 17, true),
                rule("1.26.40.00", null, 17, true),
                rule("1.26.30.00", null, null, false),
                rule("1.21.132", "1.26.39.99", null, false));
        assertFalse("a remote file missing playerRenderVtableIndex must not replace the bundled one",
                PreloaderRules.coversBundledRules(remoteBad, bundled));
    }

    @Test
    public void aRemoteFileCoveringEveryBundledFieldIsAccepted() {
        String bundled = shippedShape();
        // A superset: the same ranges, each with every key the bundled rule had.
        String remoteGood = doc(6,
                rule("1.26.60.00", null, 20, true),
                rule("1.26.50.00", null, 17, true),
                rule("1.26.40.00", null, 17, true),
                rule("1.26.30.00", null, 17, false),
                rule("1.21.132", "1.26.39.99", 17, false));
        assertTrue(PreloaderRules.coversBundledRules(remoteGood, bundled));
    }

    @Test
    public void revisionParsing() {
        assertEquals(5, PreloaderRules.revision(shippedShape()));
        assertEquals(0, PreloaderRules.revision("{}"));
        assertEquals(0, PreloaderRules.revision("not json"));
        assertEquals(0, PreloaderRules.revision(null));
    }

    @Test
    public void validityRequiresTheMenuHudSignatures() {
        assertTrue(PreloaderRules.hasValidRules(shippedShape()));
        assertFalse(PreloaderRules.hasValidRules("{\"rules\":[]}"));
        assertFalse(PreloaderRules.hasValidRules("{\"rules\":[{\"min\":\"1.0\"}]}"));
    }

    @Test
    public void versionComparisonDropsTheLeadingOne() {
        assertEquals(0, PreloaderRules.compareVersions("26.33", "1.26.33.00"));
        assertEquals(0, PreloaderRules.compareVersions("1.26.33.1", "26.33.1"));
        assertTrue(PreloaderRules.compareVersions("26.52", "1.26.50.00") > 0);
        assertTrue(PreloaderRules.compareVersions("26.33", "1.26.40.00") < 0);
    }

    @Test
    public void describeRuleNamesTheResolvedFields() {
        JsonObject merged = PreloaderRules.mergeRuleForVersion(shippedShape(), "1.26.33.1");
        String described = PreloaderRules.describeRule(merged);
        assertTrue(described, described.contains("playerRenderVtableIndex=17"));
    }

    @Test
    public void theRealBundledFileResolvesTheRenderSlotFor126331() throws Exception {
        // The synthetic tests above pin the rule; this one pins the shipped file, so a real edit
        // that drops the slot (or reorders the ranges) fails here rather than on a device.
        java.nio.file.Path path = realRulesPath();
        org.junit.Assume.assumeTrue("bundled rules not found at " + path,
                java.nio.file.Files.exists(path));
        String bundled = new String(java.nio.file.Files.readAllBytes(path),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("bundled rules must be valid", PreloaderRules.hasValidRules(bundled));
        assertTrue("revision must be at least 6", PreloaderRules.revision(bundled) >= 6);

        JsonObject merged = PreloaderRules.mergeRuleForVersion(bundled, "1.26.33.1");
        assertNotNull("1.26.33.1 must match a rule in the bundled file", merged);
        assertTrue("the bundled file must give 1.26.33.1 a render slot",
                PreloaderRules.hasPlayerRenderIndex(merged));
        // The bundled copy must also be self-consistent: a remote file replacing it must cover
        // every field it declares.
        assertTrue("the bundled file covers itself",
                PreloaderRules.coversBundledRules(bundled, bundled));
    }

    /**
     * Phase 0.1: a four-component patch version must match the rule written for its release line.
     *
     * <p>The diagnostics report from a 1.26.33.1 device showed no hooks armed. The cause was the
     * rule's exact range ({@code min == max == 1.26.33}), which a patch version ({@code 1.26.33.1})
     * exceeds — so the build fell through to the broad 1.21.132..1.26.39.99 rule and used the wrong
     * menu/HUD signatures. The rule now covers the whole 1.26.33.x line.
     */
    @Test
    public void theBundledFileCoversTheWhole12633PatchLine() throws Exception {
        java.nio.file.Path path = realRulesPath();
        org.junit.Assume.assumeTrue("bundled rules not found at " + path,
                java.nio.file.Files.exists(path));
        String bundled = new String(java.nio.file.Files.readAllBytes(path),
                java.nio.charset.StandardCharsets.UTF_8);

        for (String version : new String[] {"1.26.33", "1.26.33.0", "1.26.33.1", "1.26.33.99"}) {
            JsonObject merged = PreloaderRules.mergeRuleForVersion(bundled, version);
            assertNotNull("the bundled file must match " + version, merged);
            assertTrue(version + " must get a render slot",
                    PreloaderRules.hasPlayerRenderIndex(merged));
            assertEquals(version + " must get the menu index its release line pins",
                    153, merged.get("isShowingMenuVtableIndex").getAsInt());
        }
        // 1.26.34 is a different line and must NOT be caught by the widened 1.26.33 rule.
        JsonObject nextLine = PreloaderRules.mergeRuleForVersion(bundled, "1.26.34");
        assertNotNull("1.26.34 still matches the broad rule", nextLine);
        assertFalse("the widened rule must not swallow the next line",
                nextLine.has("isShowingMenuVtableIndex"));
    }

    /**
     * Phase 0.2: the rules the app packages must be the rules the source declares.
     *
     * <p>The device report showed "revision 0" for a source file at revision 7. One way that can
     * happen is the APK carrying a stale or different asset than the source tree, so the revision
     * the native side reads is not the one a developer sees in {@code resources/}. Gradle copies
     * {@code resources/} into the merged assets verbatim, so this test asserts the merged asset (when
     * a build has produced one) is byte-identical to the source, and that the revision is the one the
     * source declares.
     */
    @Test
    public void thePackagedRulesMatchTheSourceRules() throws Exception {
        java.nio.file.Path source = realRulesPath();
        org.junit.Assume.assumeTrue("bundled rules not found at " + source,
                java.nio.file.Files.exists(source));
        String sourceRules = new String(java.nio.file.Files.readAllBytes(source),
                java.nio.charset.StandardCharsets.UTF_8);
        int sourceRevision = PreloaderRules.revision(sourceRules);
        assertTrue("source rules must declare a revision >= 7", sourceRevision >= 7);

        java.nio.file.Path merged = java.nio.file.Paths.get(
                "build/intermediates/assets/debug/mergeDebugAssets/preloader/preloader_signature_rules_source.json");
        if (!java.nio.file.Files.exists(merged)) {
            merged = java.nio.file.Paths.get(
                    "../build/intermediates/assets/debug/mergeDebugAssets/preloader/preloader_signature_rules_source.json");
        }
        org.junit.Assume.assumeTrue("no merged asset yet (clean tree)", java.nio.file.Files.exists(merged));
        String packaged = new String(java.nio.file.Files.readAllBytes(merged),
                java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("the packaged rules revision must equal the source revision",
                sourceRevision, PreloaderRules.revision(packaged));
        assertEquals("the packaged rules must be byte-identical to the source",
                sourceRules, packaged);
    }

    private static java.nio.file.Path realRulesPath() {
        // Gradle runs unit tests with the module dir as the working directory, so the file is at
        // ../resources/preloader/...; fall back to a repo-relative path for other runners.
        String[] candidates = {
                "../resources/preloader/preloader_signature_rules_source.json",
                "resources/preloader/preloader_signature_rules_source.json",
        };
        for (String candidate : candidates) {
            java.nio.file.Path p = java.nio.file.Paths.get(candidate).toAbsolutePath().normalize();
            if (java.nio.file.Files.exists(p)) {
                return p;
            }
        }
        return java.nio.file.Paths.get(candidates[0]).toAbsolutePath().normalize();
    }
}
