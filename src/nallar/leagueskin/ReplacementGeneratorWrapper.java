package nallar.leagueskin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReplacementGeneratorWrapper {
    boolean discardsPrevious = false;
    private List<ReplacementGenerator> replacementGenerators = new ArrayList<>();
    private List<Path> paths = new ArrayList<>();

    public void addGenerator(ReplacementGenerator generator, boolean discardsPrevious, Path path) {
        if (discardsPrevious) {
            if (this.discardsPrevious) {
                throw new RuntimeException("Already discarding previous, can't add " + generator + " in " + this);
            }
            this.discardsPrevious = true;
            replacementGenerators.add(0, generator);
        } else {
            replacementGenerators.add(generator);
        }
        paths.add(path);
    }

    public List<Path> getPaths() {
        return paths;
    }

    public byte[] apply(byte[] previous) throws IOException {
        for (ReplacementGenerator generator : replacementGenerators) {
            previous = generator.generateReplacement(previous);
        }
        return previous;
    }
}
