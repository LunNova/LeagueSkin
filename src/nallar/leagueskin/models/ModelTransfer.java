package nallar.leagueskin.models;

public class ModelTransfer {
	public static byte[] transfer(Model orig, Model replacement) {
		if (orig.getVertexes() != null && replacement.getVertexes().length != orig.getVertexes().length) {
			throw new RuntimeException("Mismatched vertex count");
		}
		Vertex[] temp = replacement.getVertexes();
		if (temp != null) {
			Vertex[] origVerts = orig.getVertexes();
			if (origVerts != null && temp.length == origVerts.length) {
				Vertex[] verts = new Vertex[temp.length];
				for (int i = 0; i < temp.length; i++) {
					Vertex o = origVerts[i];
					Vertex v = temp[i];
					verts[i] = v;
					if (Float.isNaN(v.xTex) || Float.isNaN(v.yTex)) {
						v.xTex = o.xTex;
						v.yTex = o.yTex;
					}
					if (Float.isNaN(v.xNor) || Float.isNaN(v.yNor) || Float.isNaN(v.yPos)) {
						v.xNor = o.xNor;
						v.yNor = o.yNor;
						v.zNor = o.zNor;
					}
					// TODO don't just overwrite
					v.boneWeight = o.boneWeight;
					v.boneIndex = o.boneIndex;
				}
				temp = verts;
			}
			orig.setVertexes(temp);
		}
		int[][] indices = replacement.getIndices();
		if (indices != null) {
			orig.setIndices(indices);
		}
		return orig.asBytes();
	}
}
