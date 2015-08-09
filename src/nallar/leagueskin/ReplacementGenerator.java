package nallar.leagueskin;

import java.io.*;

public interface ReplacementGenerator {
	byte[] generateReplacement(byte[] previous) throws IOException;
}
