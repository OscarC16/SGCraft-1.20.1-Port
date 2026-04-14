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

    public static class Face {
        public int texture;
        public double[][] vertices; // Each vertex: [x, y, z, nx, ny, nz, u, v]
        public int[][] triangles;   // Each triangle: [i0, i1, i2]
    }

    private static final Gson GSON = new Gson();

    public static SmegModel fromResource(ResourceLocation location) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                InputStreamReader reader = new InputStreamReader(resource.get().open());
                SmegModel model = GSON.fromJson(reader, SmegModel.class);
                reader.close();
                return model;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load SMEG model: " + location, e);
        }
        throw new RuntimeException("SMEG model not found: " + location);
    }
}
