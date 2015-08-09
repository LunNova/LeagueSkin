package nallar.leagueskin.models;

public interface Model {
	int[][] getIndices();

	void setIndices(int[][] indices);

	public Vertex[] getVertexes();

	public void setVertexes(Vertex[] vertexes);

	byte[] asBytes();
}
