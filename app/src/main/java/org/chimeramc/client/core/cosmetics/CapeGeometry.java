package org.chimeramc.client.core.cosmetics;

/**
 * The cape geometry id, and an empty mesh for it.
 *
 * <p>The cape used to be a 16-bone chain that the native renderer drew as geometry. That route only
 * works where the raw GLES renderer can run, and on a Vulkan RenderDragon build it cannot, so the
 * cape now reaches the game as a texture swap at the engine's image loader instead (see
 * {@link NativeCosmeticsBridge#publishCapeTexture}). There is therefore no cape mesh to build.
 *
 * <p>The identifier survives because the geometry blob is a fixed set of named entries; publishing
 * the cape id with an empty mesh keeps the blob's shape stable so a native parser that expects the
 * entry still finds one that resolves and draws nothing.
 */
public final class CapeGeometry {

    /**
     * The cape geometry's identifier.
     *
     * <p>It used to live on the deleted resource-pack builder; the geometry blob is the only
     * consumer now, so the id belongs with the (empty) geometry it names.
     */
    public static final String GEOMETRY_ID = "geometry.chimera_cape";

    private CapeGeometry() {
    }

    /**
     * A cape geometry that resolves but draws nothing.
     *
     * <p>An empty bone list keeps the identifier valid without drawing anything, which is what the
     * blob needs now that the cape is a texture swap rather than geometry.
     */
    public static String emptyGeometryJson() {
        return "{\n"
                + "  \"format_version\": \"1.12.0\",\n"
                + "  \"minecraft:geometry\": [\n"
                + "    {\n"
                + "      \"description\": {\n"
                + "        \"identifier\": \"" + GEOMETRY_ID + "\",\n"
                + "        \"texture_width\": 64,\n"
                + "        \"texture_height\": 32\n"
                + "      },\n"
                + "      \"bones\": [\n"
                + "        {\n"
                + "          \"name\": \"body\",\n"
                + "          \"pivot\": [0.0, 24.0, 0.0],\n"
                + "          \"cubes\": []\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  ]\n"
                + "}\n";
    }
}
