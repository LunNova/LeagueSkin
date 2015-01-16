package nallar.leagueskin.models;

import nallar.leagueskin.Log;
import nallar.leagueskin.util.Throw;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Parses Riot Skin .SKN files
 */
public class Skn implements TriModel, VertexModel {
    private static final boolean DEBUG_PARSE = Boolean.getBoolean("leagueskin.debug.parse");
    private static final boolean DEBUG_DUMP = Boolean.getBoolean("leagueskin.debug.dump");
    private final String name;
    private final ByteBuffer buffer;
    private int[][] indices;
    private float[][] vertexes;

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
        sanityCheck();
    }

    private void sanityCheck() {
        for (float[] vertex : vertexes) {
            for (float part : vertex) {
                if (part > 1000 || part < -1000) {
                    throw new RuntimeException("Failed sanity check - extreme vertex coordinate " + part);
                }
            }
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
        if (magic != 0x00112233) {
            throw new RuntimeException(name + " is not a valid SKN file - wrong magic value, got " + Integer.toHexString(magic) + ", expected 0x00112233");
        }

        short versionThingy = buffer.getShort();
        //debug("hasMat: " + versionThingy);
        short numMesh = buffer.getShort();
        //debug("numMesh: " + numMesh);

        if (versionThingy == 0) {
            //?
        } else {
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
                //debug(name);
                int startVertex = buffer.getInt();
                int numVertexes = buffer.getInt();
                int startIndex = buffer.getInt();
                int numIndexes = buffer.getInt();
            }
        }

        if (versionThingy == 4) {
            buffer.getInt(); // skip 1 int - what is it?
        }

        int numIndices = buffer.getInt();
        int numVertexes = buffer.getInt();

        if (versionThingy == 4) {
            buffer.position(buffer.position() + 48);
        }

        //debug("Indices " + numIndices + ", verts: " + numVertexes);

        int[][] indices = new int[numIndices / 3][3];
        for (int i = 0; i < (numIndices / 3); i++) {
            for (int j = 0; j < 3; j++) {
                indices[i][j] = buffer.getShort();
            }
        }

        this.indices = indices;

        vertexes = new float[numVertexes][3];

        for (int i = 0; i < numVertexes; i++) {
            float xPos = buffer.getFloat();
            float yPos = buffer.getFloat();
            float zPos = buffer.getFloat();
            vertexes[i][0] = xPos;
            vertexes[i][1] = yPos;
            vertexes[i][2] = zPos;

            final int BONE_INDEX_SIZE = 4;

            for (int j = 0; j < BONE_INDEX_SIZE; j++) {
                byte boneIndex = buffer.get();
            }

            for (int j = 0; j < BONE_INDEX_SIZE; j++) {
                float boneWeight = buffer.getFloat();
            }


            float xNor = buffer.getFloat();
            float yNor = buffer.getFloat();
            float ZNor = buffer.getFloat();

            float xTex = buffer.getFloat();
            float yTex = buffer.getFloat();

            //debug("x " + xPos + ", y "+ yPos + " z " + zPos);
        }

        // Make array of indexes - uint16 * numIndices
        // Make array of verts - riot vertex thingy * numVertexes
//        OBJ test = new OBJ();
//        test.setIndices(indices);
//        test.setVertexes(vertexes);
//        test.save(Paths.get("./test/soraka.obj"));

        //throw null;
    }

    public byte[] update() {
        sanityCheck();

        buffer.position(0);

        // Header
        int magic = buffer.getInt();
        if (magic != 0x00112233) {
            throw new RuntimeException(name + " is not a valid SKN file - wrong magic value, got " + magic + ", expected 0x00112233");
        }

        short versionThingy = buffer.getShort();
        //debug("hasMat: " + versionThingy);
        short numMesh = buffer.getShort();
        //debug("numMesh: " + numMesh);

        if (versionThingy == 0) {
            //?
        } else {
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
            buffer.getInt(); // skip 1 int - what is it?
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
            buffer.putFloat(vertexes[i][0]);
            buffer.putFloat(vertexes[i][1]);
            buffer.putFloat(vertexes[i][2]);

            final int BONE_INDEX_SIZE = 4;

            for (int j = 0; j < BONE_INDEX_SIZE; j++) {
                byte boneIndex = buffer.get();
            }

            for (int j = 0; j < BONE_INDEX_SIZE; j++) {
                float boneWeight = buffer.getFloat();
            }


            float xNor = buffer.getFloat();
            float yNor = buffer.getFloat();
            float ZNor = buffer.getFloat();

            float xTex = buffer.getFloat();
            float yTex = buffer.getFloat();

            //debug("x " + xPos + ", y "+ yPos + " z " + zPos);
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
    public float[][] getVertexes() {
        return vertexes;
    }

    @Override
    public void setVertexes(float[][] vertexes) {
        this.vertexes = vertexes;
    }
}
