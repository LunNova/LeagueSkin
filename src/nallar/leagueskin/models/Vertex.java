package nallar.leagueskin.models;

import java.nio.ByteBuffer;

public class Vertex {
    public static final int BONE_INDEX_SIZE = 4;
    public static final int BYTES_PER_ENTRY = 4 * (8 + BONE_INDEX_SIZE) + BONE_INDEX_SIZE;

    public float xPos = Float.NaN;
    public float yPos = Float.NaN;
    public float zPos = Float.NaN;

    public byte[] boneIndex = new byte[BONE_INDEX_SIZE];
    public float[] boneWeight = new float[BONE_INDEX_SIZE];

    public float xNor = Float.NaN;
    public float yNor = Float.NaN;
    public float zNor = Float.NaN;

    public float xTex = Float.NaN;
    public float yTex = Float.NaN;

    public void sanityCheck() {
        float[] parts = getParts();
        for (int i = 0; i < parts.length; i++) {
            float part = parts[i];
            if (Float.isNaN(part) || part > 1000 || part < -1000) {
                throw new RuntimeException("Failed sanity check - extreme vertex coordinate " + i + " -> " + part);
            }
        }
    }

    public float[] getParts() {
        return new float[]{xPos, yPos, zPos, xNor, yNor, zNor, xTex, yTex};
    }

    public void read(ByteBuffer buffer) {
        xPos = buffer.getFloat();
        yPos = buffer.getFloat();
        zPos = buffer.getFloat();

        for (int j = 0; j < BONE_INDEX_SIZE; j++) {
            boneIndex[j] = buffer.get();
        }

        for (int j = 0; j < BONE_INDEX_SIZE; j++) {
            boneWeight[j] = buffer.getFloat();
        }

        xNor = buffer.getFloat();
        yNor = buffer.getFloat();
        zNor = buffer.getFloat();

        xTex = buffer.getFloat();
        yTex = 1 - buffer.getFloat(); // standard format is inverse of what Skn uses.
    }

    public void write(ByteBuffer buffer) {
        buffer.putFloat(xPos);
        buffer.putFloat(yPos);
        buffer.putFloat(zPos);

        for (int j = 0; j < BONE_INDEX_SIZE; j++) {
            buffer.put(boneIndex[j]);
        }

        for (int j = 0; j < BONE_INDEX_SIZE; j++) {
            buffer.putFloat(boneWeight[j]);
        }

        buffer.putFloat(xNor);
        buffer.putFloat(yNor);
        buffer.putFloat(zNor);

        buffer.putFloat(xTex);
        buffer.putFloat(1 - yTex); // standard format is inverse of what Skn uses.
    }

    public float[] getPos() {
        return new float[]{xPos, yPos, zPos};
    }

    public float[] getTex() {
        return new float[]{xTex, yTex};
    }

    public float[] getNor() {
        return new float[]{xNor, yNor, zNor};
    }
}
