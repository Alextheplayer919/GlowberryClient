package org.chimeramc.client.core.antegg;

import java.util.Locale;

/**
 * The rules for importing an {@code .AntEgg} from a share sheet or a pasted link (F1).
 *
 * <p>Pure and Android-free so the decisions are unit-testable without a device. The importer in
 * {@code FileHandler} only performs the IO; every "is this something we accept, and what do we call
 * it" question is answered here.
 *
 * <p>Three rules are load-bearing and each exists because the obvious alternative is wrong:
 *
 * <ul>
 *   <li><b>Only {@code http} and {@code https} are fetched.</b> A pasted {@code file://},
 *       {@code content://} or {@code javascript:} string must not become a network request, and a
 *       {@code javascript:} URL handed to a downloader is the classic way a paste field turns into
 *       a scripting vector. Anything else is refused rather than guessed at.</li>
 *   <li><b>A link must name the package.</b> The URL's last path segment has to end in
 *       {@code .antegg}, because a link that names nothing importable cannot be validated before
 *       the download. A generic "download any URL and hope" path is what turns a share-sheet entry
 *       into a remote-code-loading feature: the whole point of {@code .AntEgg} validation is that
 *       the archive is inspected before it is extracted, and that only holds if the thing being
 *       fetched is the package.</li>
 *   <li><b>Shared text is scraped, not trusted.</b> A share sheet often hands over a whole message
 *       ("try this mod https://example.com/foo.antegg it's great") rather than a bare URL, so the
 *       first {@code http(s)} token is extracted and the surrounding prose is discarded.</li>
 * </ul>
 *
 * <p>This class never performs IO and never throws: a malformed input returns {@code null} or
 * {@code false}, so a bad paste is a message rather than a crash.
 */
public final class AntEggLink {

    /** The extension a link must name, matched case-insensitively. */
    public static final String EXTENSION = AntEggPackage.EXTENSION;

    /** A bound on the pasted text we will scan, so a pathological paste cannot be walked. */
    public static final int MAX_TEXT_LENGTH = 4096;

    private AntEggLink() {}

    /**
     * Whether a URL may be fetched for an import.
     *
     * <p>Scheme allow-list only. A non-{@code http(s)} scheme is refused even if it happens to end
     * in {@code .antegg}, because the downloader speaks HTTP and a file or content scheme reaching
     * it is a bug at the caller, not something to silently accommodate.
     */
    public static boolean isFetchable(String url) {
        String scheme = schemeOf(url);
        return "http".equals(scheme) || "https".equals(scheme);
    }

    /** The lower-cased scheme of a URL, or an empty string when there is none. */
    public static String schemeOf(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        int colon = trimmed.indexOf(':');
        if (colon <= 0) return "";
        String scheme = trimmed.substring(0, colon).toLowerCase(Locale.ROOT);
        for (int i = 0; i < scheme.length(); i++) {
            char c = scheme.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '+' || c == '-' || c == '.';
            if (!ok) return "";
        }
        return scheme;
    }

    /**
     * The file name a URL implies, or {@code null} when it does not name an {@code .antegg}.
     *
     * <p>The query string and fragment are stripped first: a link is commonly shared with
     * {@code ?utm_source=...} or {@code #download} appended, and the extension check must look at
     * the path, not at whatever follows it.
     */
    public static String fileNameOf(String url) {
        if (url == null) return null;
        String path = pathOf(url.trim());
        if (path.isEmpty()) return null;
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        if (name.isEmpty()) return null;
        // A name with no extension is not a package name; do not invent one.
        if (!AntEggPackage.looksLikeAntEgg(name)) return null;
        return name;
    }

    /** Whether a URL names an {@code .antegg} package this app may fetch and import. */
    public static boolean isImportable(String url) {
        return isFetchable(url) && fileNameOf(url) != null;
    }

    /**
     * The first importable {@code http(s)} URL inside a piece of shared text, or {@code null}.
     *
     * <p>Scans for each {@code http://} / {@code https://} occurrence and takes the first that
     * yields a package name, so prose before or after the link is ignored. A URL that names
     * something other than an {@code .antegg} is skipped rather than returned, so a message that
     * mentions two links resolves to the one that can actually be imported.
     */
    public static String firstImportableUrl(String text) {
        if (text == null) return null;
        String haystack = text.length() > MAX_TEXT_LENGTH
                ? text.substring(0, MAX_TEXT_LENGTH)
                : text;
        int index = 0;
        while (index < haystack.length()) {
            int http = haystack.indexOf("http://", index);
            int https = haystack.indexOf("https://", index);
            int start;
            if (http < 0) {
                start = https;
            } else if (https < 0) {
                start = http;
            } else {
                start = Math.min(http, https);
            }
            if (start < 0) return null;

            String candidate = tokenAt(haystack, start);
            if (isImportable(candidate)) return candidate;
            index = start + 4;
        }
        return null;
    }

    /**
     * Whether a shared text contains anything this importer would accept.
     *
     * <p>Used to decide whether an incoming {@code ACTION_SEND} is ours at all, so a share of
     * unrelated text is left to whatever else handles it rather than being swallowed.
     */
    public static boolean looksLikeImportText(String text) {
        return firstImportableUrl(text) != null;
    }

    /** The path component of a URL, without the query string or fragment. */
    private static String pathOf(String url) {
        String rest = url;
        int scheme = rest.indexOf("://");
        if (scheme >= 0) {
            rest = rest.substring(scheme + 3);
            int slash = rest.indexOf('/');
            if (slash < 0) return "";
            rest = rest.substring(slash);
        }
        int cut = rest.length();
        int query = rest.indexOf('?');
        if (query >= 0 && query < cut) cut = query;
        int fragment = rest.indexOf('#');
        if (fragment >= 0 && fragment < cut) cut = fragment;
        return rest.substring(0, cut);
    }

    /**
     * The URL token starting at {@code start}, ending at the first character that cannot appear in
     * a URL in a shared message.
     *
     * <p>Trailing punctuation is stripped because a link at the end of a sentence arrives with a
     * period, comma or closing bracket attached, and including it would break the extension check.
     */
    private static String tokenAt(String text, int start) {
        int end = start;
        while (end < text.length()) {
            char c = text.charAt(end);
            if (c <= ' ' || c == '"' || c == '<' || c == '>' || c == '\\'
                    || c == ')' || c == ']' || c == '}' || c == '\'' || c == '`'
                    || c == '*' || c == '|') {
                break;
            }
            end++;
        }
        String token = text.substring(start, end);
        // A sentence-ending period or comma rides along with the link; a period inside the name
        // does not, so only trailing separators are removed.
        while (token.length() > 0) {
            char last = token.charAt(token.length() - 1);
            if (last == '.' || last == ',' || last == ';' || last == ':' || last == '!'
                    || last == '?') {
                token = token.substring(0, token.length() - 1);
            } else {
                break;
            }
        }
        return token;
    }
}
