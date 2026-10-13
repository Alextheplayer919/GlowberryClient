package org.chimeramc.client.core.cosmetics.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * The authored-pet rescale. Authored Blockbench exports are in their own model units (a dragon is
 * ~23 tall) while the preview draws on the ~14-pixel body-plan scale, so an un-normalised mesh was
 * several times too large and off-screen — the "some pets don't appear" symptom.
 */
public class PreviewMeshModelScaleTest {

    private static File petDir() {
        for (File c : new File[]{new File("../resources/cosmetics/models/pets"),
                new File("resources/cosmetics/models/pets")}) {
            if (c.isDirectory()) return c;
        }
        return new File("../resources/cosmetics/models/pets");
    }

    private static float height(List<PreviewMeshModel.Box> boxes) {
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (PreviewMeshModel.Box b : boxes) {
            for (float[] c : b.corners) {
                minY = Math.min(minY, c[1]);
                maxY = Math.max(maxY, c[1]);
            }
        }
        return maxY - minY;
    }

    @Test
    public void everyShippedAuthoredPetNormalizesToTheTargetHeight() throws Exception {
        int checked = 0;
        for (File f : petDir().listFiles((d, n) -> n.endsWith(".geo.json"))) {
            String json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            String retargeted = org.chimeramc.client.core.cosmetics.PetBoneRetarget.apply(json);
            BedrockGeometry g = BedrockGeometryParser.parse(retargeted);
            if (g == null) continue;
            List<PreviewMeshModel.Box> boxes = PreviewMeshModel.build(g);
            if (boxes.isEmpty()) continue;
            PreviewMeshModel.normalizeToHeight(boxes, 14f);
            assertEquals(f.getName() + " height normalised", 14f, height(boxes), 0.01f);
            // Grounded: the lowest corner sits at y=0.
            float minY = Float.MAX_VALUE;
            for (PreviewMeshModel.Box b : boxes) {
                for (float[] c : b.corners) minY = Math.min(minY, c[1]);
            }
            assertEquals(f.getName() + " grounded at y=0", 0f, minY, 0.01f);
            checked++;
        }
        assertTrue("checked at least one authored pet", checked > 0);
    }

    @Test
    public void normalizationIsCentredOnXAndZ() {
        BedrockGeometry g = BedrockGeometryParser.parse(
                "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":"
                        + "{\"identifier\":\"g\",\"texture_width\":64,\"texture_height\":64},"
                        + "\"bones\":[{\"name\":\"pet\",\"pivot\":[0,0,0],\"cubes\":["
                        + "{\"origin\":[10,0,10],\"size\":[4,8,4],\"uv\":[0,0]}]}]}]}");
        List<PreviewMeshModel.Box> boxes = PreviewMeshModel.build(g);
        PreviewMeshModel.normalizeToHeight(boxes, 8f);
        // Centre of x/z must be 0 after normalisation.
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (PreviewMeshModel.Box b : boxes) {
            for (float[] c : b.corners) {
                minX = Math.min(minX, c[0]);
                maxX = Math.max(maxX, c[0]);
                minZ = Math.min(minZ, c[2]);
                maxZ = Math.max(maxZ, c[2]);
            }
        }
        assertEquals(0f, (minX + maxX) * 0.5f, 0.001f);
        assertEquals(0f, (minZ + maxZ) * 0.5f, 0.001f);
    }
}
