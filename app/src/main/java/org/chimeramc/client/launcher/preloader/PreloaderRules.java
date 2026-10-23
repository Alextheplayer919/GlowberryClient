package org.chimeramc.client.preloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, Android-free parsing and selection for the preloader signature-rules JSON.
 *
 * <p>This exists because two runtime decisions used to be wrong and were untestable inline in
 * {@link PreloaderSignatureRulesManager} (which touches {@code SharedPreferences}, files and the
 * network):
 *
 * <ul>
 *   <li><b>Remote overwrite.</b> A remote file whose revision was merely equal to (or newer than)
 *       the local copy replaced it even when it was missing fields the bundled copy carries, so a
 *       locally built APK could run with older, field-less rules and the hook silently failed to
 *       install. {@link #coversBundledRules} is the guard.</li>
 *   <li><b>First-match field loss.</b> Selecting the <em>first</em> rule whose version range matches
 *       drops any field that rule omits; on 1.26.33.1 the matching rule has no
 *       {@code playerRenderVtableIndex}, so the render slot fell through to a default.
 *       {@link #mergeRuleForVersion} resolves each field from the first matching rule that
 *       <em>has</em> it, mirroring the native merge in {@code GameHookRules.cpp}.</li>
 * </ul>
 *
 * <p>The version comparison deliberately drops a leading {@code 1.} (see
 * {@code pl/runtime/VersionCompare.hpp}) so the launcher's {@code 26.33} and the rules'
 * {@code 1.26.33.00} compare in the same space. Keeping that rule here means the Java diagnostics
 * report and the native selector agree on which rule a running version uses.
 */
public final class PreloaderRules {

    /** Every per-rule key (outside {@code sigs}) that changes runtime behaviour. */
    private static final String[] RULE_KEYS = {
            "isShowingMenuVtableIndex",
            "playerRenderVtableIndex",
            "skinCapeVtableIndex",
            "packetReadVtableIndex",
            "particleEngineVtableIndex",
    };

    /** Signature keys the menu/HUD hooks require for a rule to be usable at all. */
    private static final String[] REQUIRED_SIG_KEYS = {
            "pauseMenuDtorSig", "pauseMenuOpenSig", "hudScreenDtorSig", "hudScreenOpenSig",
    };

    private PreloaderRules() {
    }

    /** Parses the document root, or null when it is not a JSON object. */
    public static JsonObject parse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(content);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** The document's declared {@code revision}, or 0 when absent/unparseable. */
    public static int revision(String content) {
        JsonObject root = parse(content);
        if (root == null || !root.has("revision") || !root.get("revision").isJsonPrimitive()) {
            return 0;
        }
        try {
            return Math.max(0, root.get("revision").getAsInt());
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** The rules array, or an empty list when the document has none. */
    public static List<JsonObject> rules(String content) {
        List<JsonObject> out = new ArrayList<>();
        JsonObject root = parse(content);
        if (root == null || !root.has("rules") || !root.get("rules").isJsonArray()) {
            return out;
        }
        JsonArray array = root.getAsJsonArray("rules");
        for (JsonElement element : array) {
            if (element != null && element.isJsonObject()) {
                out.add(element.getAsJsonObject());
            }
        }
        return out;
    }

    /**
     * True when the document holds at least one rule with the full menu/HUD signature set (or a
     * menu vtable index) — the same bar the manager used before, so an invalid fetch is rejected.
     */
    public static boolean hasValidRules(String content) {
        for (JsonObject rule : rules(content)) {
            if (hasCompleteSignatures(rule)) {
                return true;
            }
        }
        return false;
    }

    /** The sigs object of a rule, or the rule itself when sigs are declared inline. */
    public static JsonObject signatures(JsonObject rule) {
        if (rule == null) {
            return null;
        }
        JsonElement sigs = rule.get("sigs");
        return sigs != null && sigs.isJsonObject() ? sigs.getAsJsonObject() : rule;
    }

    private static boolean hasCompleteSignatures(JsonObject rule) {
        JsonObject sigs = signatures(rule);
        if (sigs == null) {
            return false;
        }
        for (String key : REQUIRED_SIG_KEYS) {
            if (!hasNonBlankString(sigs, key)) {
                return false;
            }
        }
        return hasNonBlankString(sigs, "isShowingMenuSig") || hasIndex(rule, "isShowingMenuVtableIndex");
    }

    private static boolean hasNonBlankString(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString()
                && !value.getAsString().trim().isEmpty();
    }

    private static boolean hasIndex(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return false;
        }
        try {
            return value.getAsInt() >= 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Numeric version parts, ignoring separators (mirrors {@code ParseVersionParts}). */
    public static int[] versionParts(String value) {
        List<Integer> parts = new ArrayList<>();
        long current = 0;
        boolean inNumber = false;
        if (value != null) {
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                if (ch >= '0' && ch <= '9') {
                    current = Math.min(current * 10 + (ch - '0'), Integer.MAX_VALUE);
                    inNumber = true;
                } else if (inNumber) {
                    parts.add((int) current);
                    current = 0;
                    inNumber = false;
                }
            }
        }
        if (inNumber) {
            parts.add((int) current);
        }
        int[] out = new int[parts.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = parts.get(i);
        }
        return out;
    }

    /** Version parts with a leading {@code 1.} dropped, so {@code 26.33} matches {@code 1.26.33.00}. */
    public static int[] normalizeVersionParts(String value) {
        int[] parts = versionParts(value);
        if (parts.length > 0 && parts[0] == 1) {
            int[] out = new int[parts.length - 1];
            System.arraycopy(parts, 1, out, 0, out.length);
            return out;
        }
        return parts;
    }

    /** Component-wise comparison with missing components as zero (mirrors {@code CompareVersions}). */
    public static int compareVersions(String left, String right) {
        int[] a = normalizeVersionParts(left);
        int[] b = normalizeVersionParts(right);
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return x < y ? -1 : 1;
            }
        }
        return 0;
    }

    /** True when a rule's {@code min}/{@code max} range contains {@code version}. */
    public static boolean ruleMatches(JsonObject rule, String version) {
        String min = optionalString(rule, "min");
        String max = optionalString(rule, "max");
        if (version == null || version.trim().isEmpty()) {
            return min.isEmpty() && max.isEmpty();
        }
        if (!min.isEmpty() && compareVersions(version, min) < 0) {
            return false;
        }
        return max.isEmpty() || compareVersions(version, max) <= 0;
    }

    private static String optionalString(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return "";
        }
        return value.getAsString().trim();
    }

    /**
     * Resolves the effective rule for {@code version} by merging fields from every matching rule.
     *
     * <p>Each field is taken from the <em>first</em> matching rule (in file order) that declares
     * it, so a specific rule's signatures win while a field only a broader rule carries is still
     * inherited. This mirrors {@code LoadConfiguredGameHookSignatures} in the native preloader; the
     * two must stay in step or the diagnostics report will describe a rule the hook did not use.
     *
     * @return the merged rule, or null when no rule matches the version.
     */
    public static JsonObject mergeRuleForVersion(String content, String version) {
        List<JsonObject> matching = new ArrayList<>();
        for (JsonObject rule : rules(content)) {
            if (ruleMatches(rule, version)) {
                matching.add(rule);
            }
        }
        if (matching.isEmpty()) {
            return null;
        }

        JsonObject merged = new JsonObject();
        JsonObject mergedSigs = new JsonObject();

        // Signatures: first matching rule that declares each key wins.
        for (JsonObject rule : matching) {
            JsonObject sigs = signatures(rule);
            if (sigs == null) {
                continue;
            }
            for (String key : sigKeysOf()) {
                if (!mergedSigs.has(key) && hasNonBlankString(sigs, key)) {
                    mergedSigs.add(key, sigs.get(key));
                }
            }
        }
        merged.add("sigs", mergedSigs);

        // Slot indices and other scalar keys.
        for (JsonObject rule : matching) {
            for (String key : RULE_KEYS) {
                if (!merged.has(key) && hasIndex(rule, key)) {
                    merged.add(key, rule.get(key));
                }
            }
        }
        // Record which rule range was used, for the diagnostics summary.
        merged.addProperty("min", optionalString(matching.get(0), "min"));
        if (!optionalString(matching.get(0), "max").isEmpty()) {
            merged.addProperty("max", optionalString(matching.get(0), "max"));
        }
        return merged;
    }

    private static List<String> sigKeysOf() {
        List<String> keys = new ArrayList<>();
        for (String key : REQUIRED_SIG_KEYS) {
            keys.add(key);
        }
        keys.add("isShowingMenuSig");
        keys.add("imageLoaderSig");
        return keys;
    }

    /**
     * True when [content]'s rule DB contains at least one rule covering [version].
     *
     * <p>This is the fail-closed coverage test (item G5): if the DB has no entry for the running
     * Bedrock build, the preloader's byte-pattern hook discovery cannot succeed, so the launcher
     * refuses to hand the rules over instead of logging scan-and-hope misses at runtime. It uses
     * the same rule-selection path the native merge uses, so the Java refusal and the native
     * install agree on which builds are covered.
     */
    public static boolean coversVersion(String content, String version) {
        return mergeRuleForVersion(content, version) != null;
    }

    /** True when [merged] carries a usable {@code playerRenderVtableIndex} (skin/cosmetic slot). */
    public static boolean hasPlayerRenderIndex(JsonObject merged) {
        return hasIndex(merged, "playerRenderVtableIndex");
    }

    /** The signature keys a merged/loaded rule actually carries, for the report summary. */
    public static String describeRule(JsonObject merged) {
        if (merged == null) {
            return "no matching rule";
        }
        StringBuilder b = new StringBuilder();
        String min = optionalString(merged, "min");
        String max = optionalString(merged, "max");
        b.append("min=").append(min.isEmpty() ? "-" : min);
        b.append(" max=").append(max.isEmpty() ? "-" : max);
        b.append(" keys=[");
        JsonObject sigs = signatures(merged);
        JsonObject source = sigs == null ? merged : sigs;
        boolean first = true;
        for (String key : sigKeysOf()) {
            if (hasNonBlankString(source, key)) {
                if (!first) b.append(',');
                b.append(key);
                first = false;
            }
        }
        for (String key : RULE_KEYS) {
            if (hasIndex(merged, key)) {
                if (!first) b.append(',');
                b.append(key).append('=').append(merged.get(key).getAsInt());
                first = false;
            }
        }
        b.append(']');
        return b.toString();
    }

    /**
     * True when {@code remote} is a safe replacement for {@code bundled}: for every rule in the
     * bundled copy, the remote carries a merged rule over the same version range that includes every
     * key the bundled rule declares.
     *
     * <p>This is the guard against a remote file with a newer revision that has silently dropped a
     * field (e.g. {@code playerRenderVtableIndex}); without it the hook installs without a slot and
     * fails on the running build.
     */
    public static boolean coversBundledRules(String remote, String bundled) {
        List<JsonObject> bundledRules = rules(bundled);
        if (bundledRules.isEmpty()) {
            return true; // nothing to protect
        }
        if (rules(remote).isEmpty()) {
            return false;
        }
        for (JsonObject bundledRule : bundledRules) {
            String probe = optionalString(bundledRule, "min");
            if (probe.isEmpty()) {
                probe = optionalString(bundledRule, "max");
            }
            if (probe.isEmpty()) {
                continue;
            }
            JsonObject merged = mergeRuleForVersion(remote, probe);
            if (merged == null || !coversKeys(bundledRule, merged)) {
                return false;
            }
        }
        return true;
    }

    private static boolean coversKeys(JsonObject required, JsonObject candidate) {
        JsonObject requiredSigs = signatures(required);
        JsonObject candidateSigs = signatures(candidate);
        if (requiredSigs != null) {
            for (String key : sigKeysOf()) {
                if (hasNonBlankString(requiredSigs, key)
                        && (candidateSigs == null || !hasNonBlankString(candidateSigs, key))) {
                    return false;
                }
            }
        }
        for (String key : RULE_KEYS) {
            if (hasIndex(required, key) && !hasIndex(candidate, key)) {
                return false;
            }
        }
        return true;
    }
}
