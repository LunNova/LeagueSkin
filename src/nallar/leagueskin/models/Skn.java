package nallar.leagueskin.models;

import com.google.common.base.Charsets;
import nallar.leagueskin.Log;
import nallar.leagueskin.util.Throw;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

/**
 * Parses Riot Skin .SKN files
 */
public class Skn implements Model {
	private static final boolean DEBUG_PARSE = Boolean.getBoolean("leagueskin.debug.parse");
	private static final boolean DEBUG_DUMP = Boolean.getBoolean("leagueskin.debug.dump");
	private static final int MAGIC = 0x00112233;
	private final String name;
	private final ByteBuffer buffer;
	private int[][] indices;
	private Vertex[] vertexes;
	short version = 4;
	private int numberOfMaterials = 0;
	private short numMesh; // Not really sure what this is
	private List<Material> materials = new ArrayList<>();
	private int unknownIntV4;
	private byte[] unknownBytesV4 = new byte[48];

	public Skn(String name, ByteBuffer buffer) {
		this.name = name;
		this.buffer = buffer;
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		parse();
	}

	public Skn(Path location) {
		String name = location.toString().replace("\\", "/");
		this.name = name.substring(name.lastIndexOf('/', name.lastIndexOf('/') - 1) + 1);
		MappedByteBuffer b;
		try (RandomAccessFile file = new RandomAccessFile(location.toFile(), "rw")) {
			b = file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, file.length());
		} catch (IOException e) {
			throw new RuntimeException("Failed to open SKN file " + location, e);
		}
		b.order(ByteOrder.LITTLE_ENDIAN);
		this.buffer = b;
		parse();
		sanityCheck(true);
	}

	private void sanityCheck(boolean initial) {
		if (initial && !Arrays.equals(asBytes(), asBytesFromOld())) {
			throw new IllegalStateException("Parsed SKN result does not match input bytes.");
		}
		for (Vertex vertex : vertexes) {
			vertex.sanityCheck();
		}
	}

	private void parse() {
		//debug("SKN is " + name);

		buffer.position(0);

		if (buffer.remaining() < 4) {
			Log.error("Not loading invalid SKN: empty!");
			return;
		}

		// Header
		int magic = buffer.getInt();
		if (magic != MAGIC) {
			throw new RuntimeException(name + " is not a valid SKN file - wrong magic value, got " + Integer.toHexString(magic) + ", expected 0x00112233");
		}

		version = buffer.getShort(); // Seems to be a version indicator?
		numMesh = buffer.getShort();
		//debug("numMesh: " + numMesh);

		if (version != 0) {
			materials.clear();
			numberOfMaterials = buffer.getInt();

			//debug("Number of materials is " + numberOfMaterials);

			for (int i = 0; i < numberOfMaterials; i++) {
				byte[] nameBytes = new byte[64];
				buffer.get(nameBytes);
				String nullName;
				try {
					nullName = new String(nameBytes, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw Throw.sneaky(e);
				}
				String name = nullName.substring(0, nullName.indexOf('\0'));
				//debug(name);
				int startVertex = buffer.getInt();
				int numVertexes = buffer.getInt();
				int startIndex = buffer.getInt();
				int numIndexes = buffer.getInt();
				materials.add(new Material(name, startVertex, numVertexes, startIndex, numIndexes));
			}
		}

		if (version == 4) {
			unknownIntV4 = buffer.getInt(); // skip 1 int - what is it?
		}

		int numIndices = buffer.getInt();
		int numVertexes = buffer.getInt();

		if (version == 4) {
			buffer.get(unknownBytesV4);
		}

		//debug("Indices " + numIndices + ", verts: " + numVertexes);

		int[][] indices = new int[numIndices / 3][3];
		for (int i = 0; i < (numIndices / 3); i++) {
			for (int j = 0; j < 3; j++) {
				indices[i][j] = buffer.getShort();
			}
		}

		this.indices = indices;

		vertexes = new Vertex[numVertexes];

		for (int i = 0; i < numVertexes; i++) {
			Vertex vertex = vertexes[i] = new Vertex();
			vertex.read(buffer);
		}

		buffer.position(buffer.position() + 12); //Skip 12 null bytes

		if (calculateSize() != buffer.capacity()) {
			Log.info("Buffer size mismatch. expected: " + calculateSize() + " real: " + buffer.capacity() + " our position: " + buffer.position());
			int remaining = buffer.remaining();
			byte[] r = new byte[buffer.remaining()];
			buffer.get(remaining);
			Log.info(Arrays.toString(r));
		}
	}

	private int calculateSize() {
		int size = 0;
		size += 4 + 2 + 2; // magic + version + numMesh
		if (version != 0) {
			size += 4; // Number of materials field
			size += numberOfMaterials * (64 + 4 * 4); // (64 byte string + 4 ints) per material
			if (version == 4) {
				size += 4 + 48; // unknown int + 48 unknown bytes of data
			}
		}
		size += 2 * 4; // 2 ints for number of indices and vertexes
		size += indices.length * 3 * 2; // Indice entry = 3 * short
		size += vertexes.length * Vertex.BYTES_PER_ENTRY;
		size += 12; // 12 null bytes
		return size;
	}

	public byte[] asBytes() {
		sanityCheck(false);
		ByteBuffer buffer = ByteBuffer.allocate(calculateSize());
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt(MAGIC);
		buffer.putShort(version);
		buffer.putShort(numMesh);

		if (version != 0) {
			buffer.putInt(numberOfMaterials);

			//debug("Number of materials is " + numberOfMaterials);

			for (Material m : materials) {
				byte[] nameBytes = new byte[64];
				System.arraycopy(m.name.getBytes(Charsets.ISO_8859_1), 0, nameBytes, 0, m.name.length());
				buffer.put(nameBytes);
				buffer.putInt(m.startVertex);
				buffer.putInt(m.numVertexes);
				buffer.putInt(m.startIndex);
				buffer.putInt(m.numIndexes);
			}
		}

		if (version == 4) {
			buffer.putInt(unknownIntV4);
		}

		buffer.putInt(indices.length * 3);
		buffer.putInt(vertexes.length);

		if (version == 4) {
			buffer.put(unknownBytesV4);
		}

		//debug("Indices " + numIndices + ", verts: " + numVertexes);

		for (int[] indice : indices) {
			for (int j = 0; j < 3; j++) {
				buffer.putShort((short) indice[j]);
			}
		}

		for (Vertex v : vertexes) {
			v.write(buffer);
		}

		buffer.put(new byte[12]);//12 null bytes

		if (buffer.position() != buffer.capacity()) {
			throw new RuntimeException("Mismatch");
		}
		return buffer.array().clone();
	}

	public byte[] asBytesFromOld() {
		ByteBuffer buffer = ByteBuffer.allocate(this.buffer.capacity());
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		this.buffer.position(0);
		buffer.put(this.buffer);
		buffer.position(0);

		// Header
		int magic = buffer.getInt();
		if (magic != MAGIC) {
			throw new RuntimeException(name + " is not a valid SKN file - wrong magic value, got " + magic + ", expected 0x00112233");
		}

		short versionThingy = buffer.getShort();
		//debug("hasMat: " + version);
		short numMesh = buffer.getShort();
		//debug("numMesh: " + numMesh);

		if (versionThingy != 0) {
			int numberOfMaterials = buffer.getInt();

			//debug("Number of materials is " + numberOfMaterials);

			for (int i = 0; i < numberOfMaterials; i++) {
				byte[] nameBytes = new byte[64];
				buffer.get(nameBytes);
				String nullName;
				try {
					nullName = new String(nameBytes, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw Throw.sneaky(e);
				}
				String name = nullName.substring(0, nullName.indexOf('\0'));
				int startVertex = buffer.getInt();
				int numVertexes = buffer.getInt();
				int startIndex = buffer.getInt();
				int numIndexes = buffer.getInt();
			}
		}

		if (versionThingy == 4) {
			buffer.putInt(unknownIntV4);
		}

		int numIndices = buffer.getInt();
		int numVertexes = buffer.getInt();

		if (versionThingy == 4) {
			buffer.position(buffer.position() + 48);
		}

		//debug("upd Indices " + numIndices + ", verts: " + numVertexes);

		int[][] indices = new int[numIndices / 3][3];
		for (int i = 0; i < (numIndices / 3); i++) {
			for (int j = 0; j < 3; j++) {
				indices[i][j] = buffer.getShort();
			}
		}

		//this.indices = indices;

		//vertexes = new float[numVertexes][3];

		for (int i = 0; i < numVertexes; i++) {
			vertexes[i].write(buffer);
		}
		return buffer.array().clone();
	}


	public String toString() {
		return name + " SKN";
	}

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

	private static class Material {
		private final String name;
		private final int startVertex;
		private final int numVertexes;
		private final int startIndex;
		private final int numIndexes;

		public Material(String name, int startVertex, int numVertexes, int startIndex, int numIndexes) {
			this.name = name;
			this.startVertex = startVertex;
			this.numVertexes = numVertexes;
			this.startIndex = startIndex;
			this.numIndexes = numIndexes;
		}
	}
}
