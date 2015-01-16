package nallar.leagueskin;

import nallar.leagueskin.util.FriendlyException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReplacementGeneratorWrapper {
    boolean discardsPrevious = false;
    private List<ReplacementGenerator> replacementGenerators = new ArrayList<>();
    private List<Path> paths = new ArrayList<>();

    public void addGenerator(ReplacementGenerator generator, boolean discardsPrevious, Path path) {
        paths.add(path);
        if (discardsPrevious) {
            if (this.discardsPrevious) {
                throw new FriendlyException("Two sources for the same file which discard previous data in " + this);
            }
            this.discardsPrevious = true;
            replacementGenerators.add(0, generator);
        } else {
            replacementGenerators.add(generator);
        }
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

    public String toString() {
        return paths.toString();
    }
}
