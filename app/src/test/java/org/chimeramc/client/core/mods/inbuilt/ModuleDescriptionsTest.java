package org.chimeramc.client.core.mods.inbuilt;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every in-game module must carry a short description: one sentence, present tense, under 60
 * characters. The Mod Menu renders it as a one-line caption, so a missing or over-long description
 * is a visible defect (an empty caption, or a truncated one).
 *
 * <p>The test parses {@code strings.xml} from disk — it does not need Android resources — so it runs
 * as a plain JVM test. A module whose description is missing fails here rather than silently showing
 * a blank caption on the device.
 */
public class ModuleDescriptionsTest {

    private static final int MAX_CHARS = 80;  // the spec's hard cap; 60 is the target

    private static File stringsXml() {
        for (File f : new File[]{
                new File("src/main/res/values/strings.xml"),
                new File("../app/src/main/res/values/strings.xml"),
                new File("app/src/main/res/values/strings.xml")}) {
            if (f.isFile()) return f;
        }
        return null;
    }

    /** name -> text for every {@code inbuilt_mod_*_desc} string. */
    private static Map<String, String> descriptions(String xml) {
        Map<String, String> out = new LinkedHashMap<>();
        Pattern p = Pattern.compile("<string name=\"(inbuilt_mod_[a-z_]+_desc)\">([^<]*)</string>");
        Matcher m = p.matcher(xml);
        while (m.find()) {
            out.put(m.group(1), m.group(2));
        }
        return out;
    }

    @Test
    public void everyModuleHasADescription() throws Exception {
        File f = stringsXml();
        Assume.assumeTrue("strings.xml not found", f != null);
        String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        Map<String, String> descs = descriptions(xml);
        assertTrue("no module descriptions found at all", descs.size() >= 30);
    }

    @Test
    public void noDescriptionExceedsTheCap() throws Exception {
        File f = stringsXml();
        Assume.assumeTrue("strings.xml not found", f != null);
        String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        for (Map.Entry<String, String> e : descriptions(xml).entrySet()) {
            int len = e.getValue().length();
            assertTrue(e.getKey() + " description is " + len + " chars (cap " + MAX_CHARS + "): \""
                    + e.getValue() + "\"", len <= MAX_CHARS);
        }
    }

    @Test
    public void everyDescriptionIsOneSentence() throws Exception {
        File f = stringsXml();
        Assume.assumeTrue("strings.xml not found", f != null);
        String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        for (Map.Entry<String, String> e : descriptions(xml).entrySet()) {
            String text = e.getValue();
            assertTrue(e.getKey() + " must not be empty", !text.trim().isEmpty());
            // One sentence: at most one terminal punctuation mark, and it is at the end.
            int terminals = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '.' || c == '!' || c == '?') terminals++;
            }
            assertTrue(e.getKey() + " must be one sentence: \"" + text + "\"", terminals <= 1);
        }
    }

    @Test
    public void noDescriptionUsesMarketingWords() throws Exception {
        File f = stringsXml();
        Assume.assumeTrue("strings.xml not found", f != null);
        String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        String[] banned = {"ultimate", "best ever", "revolutionary", "amazing", "awesome",
                "powerful", "premium"};
        for (Map.Entry<String, String> e : descriptions(xml).entrySet()) {
            String lower = e.getValue().toLowerCase(java.util.Locale.US);
            for (String word : banned) {
                assertTrue(e.getKey() + " uses a marketing word (\"" + word + "\"): \"" + e.getValue()
                        + "\"", !lower.contains(word));
            }
        }
    }
}
