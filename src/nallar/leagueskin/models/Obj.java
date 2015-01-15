package nallar.leagueskin.models;

import nallar.leagueskin.util.Throw;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Obj implements TriModel, VertexModel, TexturedVertexModel {
    private int[][] indices;
    private float[][] vertexes;

    @Override
    public int[][] getIndices() {
        return indices;
    }

    @Override
    public void setIndices(int[][] indices) {
        this.indices = indices;
    }

    @Override
    public float[][] getVertexes() {
        return vertexes;
    }

    @Override
    public void setVertexes(float[][] vertexes) {
        this.vertexes = vertexes;
    }

    public void load(Path p) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(p.toFile()), "UTF-8"))) {
            List<float[]> vertexList = new ArrayList<>();
            List<int[]> indexList = new ArrayList<>();
            br.lines().forEach((String line) -> {
                line = line.trim();
                if (line.isEmpty()) {
                    return;
                }
                if (line.startsWith("f ")) {
                    // face - tri
                    String[] parts = line.substring(2).split(" ");
                    if (parts.length != 3) {
                        throw new RuntimeException("Face isn't triangle - not 3 parts");
                    }
                    int[] indexes = new int[3];
                    for (int i = 0; i < 3; i++) {
                        String part = parts[i];
                        int indexSlash = part.indexOf('/');
                        if (indexSlash != -1) {
                            part = part.substring(0, indexSlash);
                        }
                        indexes[i] = Integer.valueOf(part);
                    }
                    indexList.add(indexes);
                }
                if (line.startsWith("v ")) {
                    // face - tri
                    String[] parts = line.substring(2).split(" ");
                    if (parts.length != 3) {
                        throw new RuntimeException("Vertex must have 3 parts");
                    }
                    float[] vertexes = new float[3];
                    for (int i = 0; i < 3; i++) {
                        vertexes[i] = Float.valueOf(parts[i]);
                    }
                    vertexList.add(vertexes);
                }
            });
            if (!indexList.isEmpty()) {
                indices = indexList.toArray(new int[indexList.size()][]);
            }
            if (!vertexList.isEmpty()) {
                vertexes = vertexList.toArray(new float[vertexList.size()][]);
            }
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
    }

    public String asObjString() {
        StringBuilder sb = new StringBuilder();
        if (vertexes != null) {
            for (float[] vertex : vertexes) {
                sb.append('v');
                for (float part : vertex) {
                    sb.append(' ').append(part);
                }
                sb.append('\n');
            }
        }
        if (indices != null) {
            for (int[] index : indices) {
                // Add one, as OBJ is 1-indexed.
                sb.append("f ").append(index[0] + 1).append(' ').append(index[1] + 1).append(' ').append(index[2] + 1).append('\n');
            }
        }
        return sb.toString();
    }

    public void save(Path p) {
        try (PrintWriter printWriter = new PrintWriter(p.toFile())) {
            printWriter.write(asObjString());
        } catch (FileNotFoundException e) {
            throw Throw.sneaky(e);
        }
    }
}
