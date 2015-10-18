package nallar.leagueskin.models;

import com.google.common.base.Charsets;
import nallar.leagueskin.util.Throw;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Obj implements Model {
	private int[][] indices;
	private Vertex[] vertexes;

	@Override
	public int[][] getIndices() {
		return indices;
	}

	@Override
	public void setIndices(int[][] indices) {
		this.indices = indices;
	}

	@Override
	public Vertex[] getVertexes() {
		return vertexes;
	}

	@Override
	public void setVertexes(Vertex[] vertexes) {
		this.vertexes = vertexes;
	}

	public void load(Path p) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(p.toFile()), "UTF-8"))) {
			List<float[]> vertexList = new ArrayList<>();
			List<float[]> textureList = new ArrayList<>();
			List<float[]> normalList = new ArrayList<>();
			List<int[]> indexList = new ArrayList<>();
			List<int[]> normalIndexList = new ArrayList<>();
			List<int[]> textureIndexList = new ArrayList<>();
			br.lines().forEach((String line) -> {
				line = line.trim();
				if (line.isEmpty()) {
					return;
				}
				if (line.startsWith("f ")) {
					// face - tri
					String[] parts = line.substring(2).split(" ");
					if (parts.length != 3) {
						throw new RuntimeException("Face (f) isn't triangle - not 3 parts");
					}
					int[] indexes = new int[3];
					int[] normalIndexes = new int[]{-1, -1, -1};
					int[] textureIndexes = new int[]{-1, -1, -1};
					for (int i = 0; i < 3; i++) {
						String[] part = parts[i].split("/");
						for (int j = 0; j < part.length; j++) {
							if (part[j].isEmpty()) {
								continue;
							}
							int value = Integer.valueOf(part[j]) - 1;
							switch (j) {
								case 0:
									indexes[i] = value;
									break;
								case 1:
									textureIndexes[i] = value;
									break;
								case 2:
									normalIndexes[i] = value;
									break;
							}
						}
					}
					indexList.add(indexes);
					normalIndexList.add(normalIndexes);
					textureIndexList.add(textureIndexes);
				}
				if (line.startsWith("v ")) {
					// vertex
					String[] parts = line.substring(2).split(" ");
					if (parts.length != 3) {
						throw new RuntimeException("Vertex (v) must have 3 parts");
					}
					float[] vertexes = new float[3];
					for (int i = 0; i < 3; i++) {
						vertexes[i] = Float.valueOf(parts[i]);
					}
					vertexList.add(vertexes);
				}
				if (line.startsWith("vt ")) {
					// vertex texture
					String[] parts = line.substring(3).split(" ");
					if (parts.length != 2) {
						throw new RuntimeException("Vertex texture (vt) must have 2 parts");
					}
					float[] textures = new float[2];
					for (int i = 0; i < 2; i++) {
						textures[i] = Float.valueOf(parts[i]);
					}
					textureList.add(textures);
				}
				if (line.startsWith("vn ")) {
					// vertex normal
					String[] parts = line.substring(3).split(" ");
					if (parts.length != 3) {
						throw new RuntimeException("Vertex texture (vt) must have 2 parts");
					}
					float[] normals = new float[3];
					for (int i = 0; i < 3; i++) {
						normals[i] = Float.valueOf(parts[i]);
					}
					normalList.add(normals);
				}
			});
			if (!indexList.isEmpty()) {
				indices = indexList.toArray(new int[indexList.size()][]);
			}
			if (!vertexList.isEmpty()) {
				vertexes = new Vertex[vertexList.size()];
				for (int i = 0; i < vertexes.length; i++) {
					Vertex v = vertexes[i] = new Vertex();
					float[] parts = vertexList.get(i);
					v.xPos = parts[0];
					v.yPos = parts[1];
					v.zPos = parts[2];
				}
				if (indices != null) {
					for (int i = 0; i < indices.length; i++) {
						int[] indexes = indices[i];
						int[] normalIndexes = normalIndexList.get(i);
						int[] textureIndexes = textureIndexList.get(i);
						for (int j = 0; j < indexes.length; j++) {
							Vertex v = vertexes[indexes[j]];
							if (normalIndexes[j] != -1) {
								float[] normals = normalList.get(normalIndexes[j]);
								v.xNor = normals[0];
								v.yNor = normals[1];
								v.zNor = normals[2];
							}
							if (textureIndexes[j] != -1) {
								float[] textures = textureList.get(textureIndexes[j]);
								v.xTex = textures[0];
								v.yTex = textures[1];
							}
						}
					}
				}
				save(Paths.get("C:/Riot Games/test.obj"));
			}
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}
	}

	public String asObjString() {
		StringBuilder sb = new StringBuilder();
		if (vertexes != null) {
			for (Vertex vertex : vertexes) {
				sb.append('v');
				for (float part : vertex.getPos()) {
					sb.append(' ').append(part);
				}
				sb.append('\n');
			}
			for (Vertex vertex : vertexes) {
				if (!Float.isNaN(vertex.xTex)) {
					sb.append("vt");
					for (float part : vertex.getTex()) {
						sb.append(' ').append(part);
					}
					sb.append('\n');
				}
			}
			for (Vertex vertex : vertexes) {
				sb.append("vn");
				for (float part : vertex.getNor()) {
					sb.append(' ').append(part);
				}
				sb.append('\n');
			}
		}
		if (indices != null) {
//            for (int i = indices.length - 1; i >= 0; i--) {
//                int[] index = indices[i];
////            for (int[] index : indices) {
//                // Add one, as OBJ is 1-indexed.
//                sb.append('f');
////                for (int part : index) {
////                    sb.append(' ').append(part + 1).append('/').append(part + 1).append('/').append(part + 1);
////                }
////                sb.append("\nf");
//                for (int j = index.length - 1; j >= 0; j--) {
//                    int part = index[j];
//                    sb.append(' ').append(part + 1).append('/').append(part + 1).append('/').append(part + 1);
//                }
//                sb.append('\n');
//            }
			for (int[] index : indices) {
				// Add one, as OBJ is 1-indexed.
				sb.append('f');
				for (int part : index) {
					sb.append(' ').append(part + 1).append('/').append(part + 1).append('/').append(part + 1);
				}
				sb.append('\n');
			}
		}
		return sb.toString();
	}

	public byte[] asBytes() {
		return asObjString().getBytes(Charsets.ISO_8859_1);
	}

	public void save(Path p) {
		try (PrintWriter printWriter = new PrintWriter(p.toFile())) {
			printWriter.write(asObjString());
		} catch (FileNotFoundException e) {
			throw Throw.sneaky(e);
		}
	}
}
