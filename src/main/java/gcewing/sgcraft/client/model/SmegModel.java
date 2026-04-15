package gcewing.sgcraft.client.model;

import com.google.gson.Gson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.client.Minecraft;

import java.io.InputStreamReader;
import java.util.Optional;

/**
 * SMEG Model format parser - replicates the original SGCraft model loading.
 * Each face has a texture index, vertices (x,y,z, nx,ny,nz, u,v) and triangle indices.
 */
public class SmegModel {

    public double[] bounds;
    public Face[] faces;

    // Optimized runtime data
    public Face[][] groupedFaces;

    public static class Face {
        public int texture;
        public double[][] vertices; // Each vertex: [x, y, z, nx, ny, nz, u, v]
        public int[][] triangles;   // Each triangle: [i0, i1, i2]

        // Precomputed shading color
        public int r, g, b;
    }

    private static final Gson GSON = new Gson();

    public static SmegModel fromResource(ResourceLocation location) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                InputStreamReader reader = new InputStreamReader(resource.get().open());
                SmegModel model = GSON.fromJson(reader, SmegModel.class);
                reader.close();
                if (model != null) {
                    model.precompute();
                }
                return model;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load SMEG model: " + location, e);
        }
        throw new RuntimeException("SMEG model not found: " + location);
    }

    /**
     * Groups faces by their texture index and precomputes their shading color.
     * This avoids expensive filtering and math in the rendering loop.
     */
    public void precompute() {
        if (faces == null) return;

        // Grouping: find max texture index
        int maxTex = 0;
        for (Face face : faces) {
            if (face.texture > maxTex) maxTex = face.texture;
            
            // Precompute shading for this face
            // Use shading logic from the original mod's renderers
            double[] firstVert = face.vertices[face.triangles[0][0]];
            float fnx = (float) firstVert[3];
            float fny = (float) firstVert[4];
            float fnz = (float) firstVert[5];
            float shade = (float)(0.6 * fnx * fnx + 0.8 * fnz * fnz + (fny > 0 ? 1.0 : 0.5) * fny * fny);
            shade = Math.max(shade, 0.4f);
            face.r = (int)(255 * shade);
            face.g = (int)(255 * shade);
            face.b = (int)(255 * shade);
        }

        groupedFaces = new Face[maxTex + 1][];
        for (int t = 0; t <= maxTex; t++) {
            final int tex = t;
            java.util.List<Face> list = new java.util.ArrayList<>();
            for (Face f : faces) {
                if (f.texture == tex) list.add(f);
            }
            groupedFaces[t] = list.toArray(new Face[0]);
        }
    }
}
