package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The type roles (U8) and the weight axis (U6). Pure: no device, no Context.
 */
public class UiTypeTest {

    @Test
    public void theFiveRolesUseTheFiveSizes() {
        assertEquals(UiTokens.TEXT_DISPLAY, UiType.Role.DISPLAY.sizeSp, 0.001f);
        assertEquals(UiTokens.TEXT_TITLE_ROLE, UiType.Role.TITLE.sizeSp, 0.001f);
        assertEquals(UiTokens.TEXT_BODY, UiType.Role.BODY.sizeSp, 0.001f);
        assertEquals(UiTokens.TEXT_LABEL, UiType.Role.LABEL.sizeSp, 0.001f);
        assertEquals(UiTokens.TEXT_CAPTION, UiType.Role.CAPTION.sizeSp, 0.001f);
    }

    @Test
    public void theRolesAreInDescendingSizeOrder() {
        UiType.Role[] roles = UiType.Role.values();
        for (int i = 1; i < roles.length; i++) {
            assertTrue(roles[i] + " must be smaller than " + roles[i - 1],
                    roles[i].sizeSp < roles[i - 1].sizeSp);
        }
    }

    @Test
    public void theRolesUseTheTokensWeights() {
        assertEquals(UiTokens.DISPLAY_WEIGHT, UiType.Role.DISPLAY.weight);
        assertEquals(UiTokens.TITLE_WEIGHT, UiType.Role.TITLE.weight);
        assertEquals(UiTokens.BODY_WEIGHT, UiType.Role.BODY.weight);
        assertEquals(UiTokens.ROW_TITLE_WEIGHT, UiType.Role.LABEL.weight);
        assertEquals(UiTokens.CAPTION_WEIGHT, UiType.Role.CAPTION.weight);
    }

    @Test
    public void theWeightAxisIsThePlatformsFormat() {
        assertEquals("'wght' 600", UiType.weightAxis(600));
        assertEquals("'wght' 500", UiType.weightAxis(500));
        // Out-of-range values clamp rather than producing a setting the font cannot parse.
        assertEquals("'wght' 1000", UiType.weightAxis(5000));
        assertEquals("'wght' 1", UiType.weightAxis(-4));
    }

    @Test
    public void theClosestRoleIsTheNearestSize() {
        assertEquals(UiType.Role.BODY, UiType.closestRole(14f));
        assertEquals(UiType.Role.CAPTION, UiType.closestRole(11.4f));
        assertEquals(UiType.Role.DISPLAY, UiType.closestRole(100f));
    }
}
