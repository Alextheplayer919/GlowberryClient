package org.chimeramc.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The API-28 guard: pins the helper rules, then scans the real source tree for the banned calls.
 *
 * <p>This exists because a call to a Java library method newer than the device's runtime is not a
 * compile error. The project compiles against the API 36 SDK, so {@code InputStream.readAllBytes()}
 * and {@code String.isBlank()} build cleanly and then throw {@link NoSuchMethodError} on Android
 * 9-12. That is an {@link Error}, so it is not contained by the {@code catch (Exception)} that
 * wrapped the call, and the launch died with "Launch failed" before any Minecraft code ran.
 *
 * <p>The scan half is the CI check: it walks every shipped Java/Kotlin source and fails on a banned
 * call. The hand-written half pins the classifier so a broken pattern cannot pass vacuously.
 */
public class Api28CompatTest {

    // --- The helper rules --------------------------------------------------------------------

    @Test
    public void blankMatchesTheJavaElevenDefinition() {
        assertTrue(Api28Compat.isBlank(null));
        assertTrue(Api28Compat.isBlank(""));
        assertTrue(Api28Compat.isBlank("   "));
        assertTrue(Api28Compat.isBlank("\t\n\r"));
        // Character.isWhitespace is the definition String.isBlank uses; a space/tab-only check
        // would get these wrong.
        assertTrue(Api28Compat.isBlank("\u000B\u000C"));
        assertFalse(Api28Compat.isBlank(" a "));
        assertFalse(Api28Compat.isBlank("a"));
        assertFalse(Api28Compat.isBlank("\u00A0"));
    }

    @Test
    public void isNotBlankIsTheExactNegation() {
        for (String s : new String[] {null, "", " ", "\t", "a", " a ", "\u00A0"}) {
            assertEquals(Api28Compat.isBlank(s), !Api28Compat.isNotBlank(s));
        }
    }

    @Test
    public void listOfKeepsOrderAndRejectsMutation() {
        List<String> list = Api28Compat.listOf("a", "b", "c");
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("c", list.get(2));
        try {
            list.add("d");
            throw new AssertionError("expected the list to be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // The API 33 method is unmodifiable too, so a call site cannot tell them apart.
        }
    }

    @Test
    public void listOfRejectsANullElementLikeTheJavaMethodDoes() {
        try {
            Api28Compat.listOf("a", null);
            throw new AssertionError("expected NullPointerException");
        } catch (NullPointerException expected) {
            // Matches List.of.
        }
    }

    @Test
    public void setOfRejectsMutationAndDuplicates() {
        java.util.Set<String> set = Api28Compat.setOf("a", "b", "a");
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        try {
            set.add("c");
            throw new AssertionError("expected the set to be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // Matches Set.of.
        }
    }

    @Test
    public void setOfRejectsANullElement() {
        try {
            Api28Compat.setOf("a", null);
            throw new AssertionError("expected NullPointerException");
        } catch (NullPointerException expected) {
            // Matches Set.of.
        }
    }

    @Test
    public void urlEncodeMatchesThePlatformEncoding() throws Exception {
        // The helper must produce exactly what the API 33 one-argument overload would, or a redirect
        // URI built with it would not match what the auth server expects.
        assertEquals(java.net.URLEncoder.encode("a b/c?d=e&f", "UTF-8"),
                Api28Compat.urlEncode("a b/c?d=e&f"));
        // A redirect-URI component is the real call site: dots and letters pass through.
        assertEquals("com.microsoft.identity.client.SignActivity",
                Api28Compat.urlEncode("com.microsoft.identity.client.SignActivity"));
    }

    @Test
    public void urlEncodeIsNullSafe() {
        // A null topic is a programming error, not a reason to throw inside a sync timer.
        assertNull(Api28Compat.urlEncode(null));
    }

    // --- The classifier ----------------------------------------------------------------------

    private static final class Rule {
        final String label;
        final Pattern pattern;

        Rule(String label, String regex) {
            this.label = label;
            this.pattern = Pattern.compile(regex);
        }
    }

    /**
     * Calls that do not exist below API 33 (plus the API 34/35 ones) and must never appear in a
     * shipped source file. Each pattern is precise: {@code Files.readAllBytes} is API 26 and is
     * deliberately allowed, so the receiver is checked rather than the bare method name, and
     * {@code .reversed()} on a Comparator (API 1) is distinguished from the API 35 collection one.
     */
    private static final List<Rule> BANNED = Arrays.asList(
            // Files.readAllBytes(Path) is API 26 and is the replacement; any other receiver is
            // an InputStream/FileInputStream call, which is API 33.
            new Rule("InputStream.readAllBytes (API 33)",
                    "(?<!Files)\\.readAllBytes\\s*\\(\\s*\\)"),
            new Rule("InputStream.readNBytes (API 33)", "\\.readNBytes\\s*\\(\\s*\\)"),
            new Rule("InputStream.transferTo (API 33)", "\\.transferTo\\s*\\("),
            new Rule("String.isBlank (API 33)", "\\.isBlank\\s*\\(\\s*\\)"),
            new Rule("String.strip/stripLeading/stripTrailing (API 33)",
                    "\\.strip(?:Leading|Trailing)?\\s*\\("),
            new Rule("String.repeat (API 33)", "\\.repeat\\s*\\(\\s*\\d"),
            new Rule("List/Set/Map.of (API 33)",
                    "\\b(?:java\\.util\\.)?(?:List|Set|Map)\\.of\\s*\\("),
            new Rule("List/Set/Map.copyOf (API 33)",
                    "\\b(?:java\\.util\\.)?(?:List|Set|Map)\\.copyOf\\s*\\("),
            new Rule("Collectors.toUnmodifiable* (API 33)",
                    "toUnmodifiable(?:List|Set|Map)\\s*\\("),
            new Rule("Optional.orElseThrow() (API 33)", "\\.orElseThrow\\s*\\(\\s*\\)"),
            new Rule("Optional.stream (API 33)", "\\bOptional\\.stream\\s*\\("),
            new Rule("Files.mismatch/readString/writeString (API 33)",
                    "\\bFiles\\.(?:mismatch|readString|writeString)\\s*\\("),
            new Rule("Arrays.compare/mismatch (API 33)",
                    "\\bArrays\\.(?:compare|mismatch)\\s*\\("),
            new Rule("Objects.requireNonNullElse (API 33)",
                    "\\bObjects\\.requireNonNullElse\\s*\\("),
            new Rule("Map.entry (API 33)", "\\bMap\\.entry\\s*\\("),
            // Collectors.toList() is API 24; Stream.toList() (API 34) has no receiver chain.
            new Rule("Stream.toList (API 34)",
                    "(?<!Collectors)\\.toList\\s*\\(\\s*\\)"),
            new Rule("SequencedCollection (API 35)",
                    "\\.(?:getFirst|getLast)\\s*\\(\\s*\\)|\\bSequencedCollection\\b"),
            // URLEncoder.encode(String, Charset) is API 33; the pre-33 signature takes a String
            // encoding name. Api28Compat.urlEncode wraps it. This one was a live sign-in crash.
            new Rule("URLEncoder.encode(String, Charset) (API 33)",
                    "\\bURLEncoder\\.encode\\s*\\([^)]*StandardCharsets"),
            // StateListDrawable.getStateCount()/getStateDrawable() are API 29. They throw on API 28,
            // and because they throw an Error a catch (Exception) does not contain them.
            new Rule("StateListDrawable.getStateCount/getStateDrawable (API 29)",
                    "\\.(?:getStateCount|getStateDrawable)\\s*\\("));

    /** Every banned call found in [text], as {@code "line N: label"}. Comments are not code. */
    private static List<String> scan(String text) {
        List<String> out = new ArrayList<>();
        String[] lines = stripComments(text).split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            for (Rule rule : BANNED) {
                if (rule.pattern.matcher(lines[i]).find()) {
                    out.add("line " + (i + 1) + ": " + rule.label + " -> " + lines[i].trim());
                    break;
                }
            }
        }
        return out;
    }

    /** Blanks out block and line comments so prose about a banned call is not a violation. */
    private static String stripComments(String text) {
        String noBlock = text.replaceAll("(?s)/\\*.*?\\*/", "");
        StringBuilder out = new StringBuilder(noBlock.length());
        for (String line : noBlock.split("\n", -1)) {
            int slash = line.indexOf("//");
            out.append(slash >= 0 ? line.substring(0, slash) : line).append('\n');
        }
        return out.toString();
    }

    @Test
    public void theClassifierFlagsAKnownBadLine() {
        assertTrue(scan("    byte[] b = in.readAllBytes();").stream()
                .anyMatch(v -> v.contains("InputStream.readAllBytes")));
        assertTrue(scan("if (!s.isBlank()) {}").stream()
                .anyMatch(v -> v.contains("String.isBlank")));
        assertTrue(scan("Set<String> s = Set.of(\"a\");").stream()
                .anyMatch(v -> v.contains("List/Set/Map.of")));
        assertTrue(scan("String r = \"x\".repeat(3);").stream()
                .anyMatch(v -> v.contains("String.repeat")));
    }

    @Test
    public void theClassifierDoesNotFlagTheApi26AndApi1CallsThisProjectReliesOn() {
        // Files.readAllBytes(Path) is API 26 and is what replaced the crash.
        assertTrue(scan("byte[] b = Files.readAllBytes(p);").isEmpty());
        assertTrue(scan("byte[] b = java.nio.file.Files.readAllBytes(f.toPath());").isEmpty());
        // Comparator.reversed() is API 1, not the API 35 SequencedCollection.reversed().
        assertTrue(scan("list.sort(Comparator.comparingLong(File::lastModified).reversed());")
                .isEmpty());
        // Collectors.toList is API 24.
        assertTrue(scan("list.stream().collect(Collectors.toList());").isEmpty());
        // String.join is API 26.
        assertTrue(scan("String.join(\", \", parts);").isEmpty());
        // URLEncoder: the one-arg Charset overload is API 33, the String-encoding one is API 1.
        assertTrue(scan("URLEncoder.encode(v, StandardCharsets.UTF_8);").stream()
                .anyMatch(m -> m.contains("URLEncoder")));
        assertTrue(scan("URLEncoder.encode(v, \"UTF-8\");").isEmpty());
        assertTrue(scan("Api28Compat.urlEncode(v);").isEmpty());
        // StateListDrawable: the accessors are API 29, the constant state is API 1.
        assertTrue(scan("sld.getStateCount();").stream()
                .anyMatch(m -> m.contains("StateListDrawable")));
        assertTrue(scan("sld.getStateDrawable(0);").stream()
                .anyMatch(m -> m.contains("StateListDrawable")));
        assertTrue(scan("container.getChildCount();").isEmpty());
    }

    @Test
    public void aCommentMentioningABannedCallIsNotAViolation() {
        assertTrue(scan("// do not use InputStream.readAllBytes here").isEmpty());
        // A JavaDoc block naming the call is prose; the delimiters are part of real source.
        assertTrue(scan("/**\n * {@code in.readAllBytes()} is API 33, use Files.\n */").isEmpty());
        assertTrue(scan("/* Set.of() is API 33. */").isEmpty());
    }

    // --- The repository scan (the CI check) ---------------------------------------------------

    @Test
    public void noShippedSourceCallsAnApiNewerThanTheMinimumSdk() throws IOException {
        Path repo = findRepoRoot();
        List<String> violations = new ArrayList<>();
        int scanned = 0;
        for (String relative : Arrays.asList(
                "app/src/main/java", "minecraft/src/main/java", "pojav_controls/src/main/java")) {
            Path root = repo.resolve(relative);
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path file : (Iterable<Path>) walk.filter(Api28CompatTest::isSource)::iterator) {
                    scanned++;
                    String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                    for (String violation : scan(text)) {
                        violations.add(repo.relativize(file) + ": " + violation);
                    }
                }
            }
        }
        assertTrue("scan found too few source files, the guard would pass vacuously: " + scanned,
                scanned > 200);
        assertTrue("API newer than 28 in shipped sources:\n" + String.join("\n  ", violations),
                violations.isEmpty());
    }

    private static boolean isSource(Path p) {
        String name = p.toString();
        return name.endsWith(".java") || name.endsWith(".kt");
    }

    private static Path findRepoRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("settings.gradle"))
                    && Files.isDirectory(dir.resolve("app/src/main/java"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("repo root not found from "
                + Paths.get("").toAbsolutePath());
    }
}
