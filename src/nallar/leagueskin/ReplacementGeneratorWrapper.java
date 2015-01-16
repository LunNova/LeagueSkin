package nallar.leagueskin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReplacementGeneratorWrapper {
    boolean discardsPrevious = false;
    private List<ReplacementGenerator> replacementGenerators = new ArrayList<>();

    public void addGenerator(ReplacementGenerator generator, boolean discardsPrevious) {
        if (discardsPrevious) {
            if (this.discardsPrevious) {
                throw new RuntimeException("Already discarding previous, can't add " + generator + " in " + this);
            }
            this.discardsPrevious = true;
            replacementGenerators.add(0, generator);
        }
        replacementGenerators.add(generator);
    }

    public byte[] apply(byte[] previous) throws IOException {
        for (ReplacementGenerator generator : replacementGenerators) {
            previous = generator.generateReplacement(previous);
        }
        return previous;
    }
}
