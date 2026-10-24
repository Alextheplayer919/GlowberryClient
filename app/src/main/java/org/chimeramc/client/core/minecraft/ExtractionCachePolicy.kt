package org.chimeramc.client.core.minecraft

/**
 * The rule that decides whether the extracted native-library cache can be reused.
 *
 * <p>Pure and Android-free on purpose: the logic lives inside `GamePackageManager`, whose class
 * initialisation loads a native library, so a JVM test that touched it would fail before it could
 * assert anything. Extracting the rule here is what makes the "wipe and re-extract on update"
 * behaviour testable — the same shape used for `VersionCodeNormalizer`.
 *
 * <p>The cache is keyed by two independent values, and both must match before a cache hit is
 * trusted:
 *  * the **manifest** — the exact set of APK files the libraries were extracted from, so a version
 *    whose package changed is re-extracted even if the launcher did not change; and
 *  * the **extraction version** — the extractor pipeline plus the app's own version code, so a
 *    launcher update always wipes libraries an older extractor produced. Stale extracted libraries
 *    are the reported cause of `dlopen` failures that only appear after an update.
 */
object ExtractionCachePolicy {

    /** Manual bump of the extraction pipeline. Raise when the extraction logic itself changes. */
    const val EXTRACTOR_VERSION: Int = 4

    /**
     * The value written beside the extracted libraries.
     *
     * <p>The manual extractor version in the high bits and the app version code in the low bits, so
     * either a pipeline change or an ordinary launcher update changes the value and invalidates the
     * cache. `appVersionCode` is masked to 16 bits because it only has to differ between updates,
     * not to be a faithful copy of it.
     */
    @JvmStatic
    fun extractionVersion(extractorVersion: Int, appVersionCode: Int): Int =
        (extractorVersion shl 16) or (appVersionCode and 0xffff)

    /** True when a marker read from disk names [expected], on any extractor version. */
    @JvmStatic
    fun markerMatches(stored: String?, expected: Int): Boolean =
        stored != null && stored.trim() == expected.toString()

    /**
     * True when a cache written by an older extractor must be wiped before reuse.
     *
     * <p>Only when the manifest still matches: a manifest mismatch already forces a full
     * re-extraction, so there is nothing to wipe first.
     */
    @JvmStatic
    fun shouldWipeStaleLibraries(manifestMatches: Boolean, extractionVersionMatches: Boolean): Boolean =
        manifestMatches && !extractionVersionMatches

    /**
     * True when every library the launch needs is present and non-empty.
     *
     * <p>A zero-length file is treated as absent: a library truncated by an interrupted extraction
     * is not loadable, and the cache-hit path must not accept it.
     */
    @JvmStatic
    fun allLibrariesPresent(required: Collection<String>, exists: (String) -> Boolean): Boolean =
        required.all { exists(it) }

    /**
     * The single decision the extraction uses: reuse the cache, or wipe and re-extract.
     *
     * <p>Reuse requires all four: a matching manifest, a matching extraction version, and every
     * required library present. Anything else is a miss, which re-extracts — the safe direction,
     * because a wrongly-trusted cache fails at `dlopen` with an error the user cannot act on.
     */
    @JvmStatic
    fun isCacheUsable(
        manifestMatches: Boolean,
        extractionVersionMatches: Boolean,
        requiredLibrariesPresent: Boolean
    ): Boolean = manifestMatches && extractionVersionMatches && requiredLibrariesPresent
}
