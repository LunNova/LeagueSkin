package nallar.leagueskin;

import java.io.IOException;

public interface ReplacementGenerator {
    byte[] generateReplacement(byte[] previous) throws IOException;
}
