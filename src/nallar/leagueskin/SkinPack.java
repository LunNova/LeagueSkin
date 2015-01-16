package nallar.leagueskin;

import nallar.leagueskin.models.Obj;
import nallar.leagueskin.models.Skn;
import nallar.leagueskin.util.Throw;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SkinPack {
    final String match;

    private List<Replacement> replacements = new ArrayList<>();

    public SkinPack(Path folder) {
        this(folder, null);
    }

    public SkinPack(Path folder, String match) {
        this.match = match;
        recursiveSearch(folder);
    }

    private static String shortNameFromPath(Path p) {
        return p.getFileName().toString().toLowerCase().replace("\\", "/");
    }

    private void recursiveSearch(Path path) {
        path = path.toAbsolutePath();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                String forwardSlashName = entry.toString().replace('\\', '/');
                String name = shortNameFromPath(entry);
                if (Files.isDirectory(entry)) {
                    recursiveSearch(entry);
                } else if (!name.startsWith("!") && !forwardSlashName.contains("/!")) {
                    if (match == null || name.contains(match)) {
                        boolean discardsPrevious = true;
                        ReplacementGenerator replacementGenerator = previous -> Files.readAllBytes(entry);
                        if (name.endsWith(".obj")) {
                            discardsPrevious = false;
                            name = name.replace(".obj", ".skn");
                            Obj replacement = new Obj();
                            replacement.load(entry);
                            final String finalName = name;
                            replacementGenerator = previous -> {
                                Skn skn = new Skn(finalName, ByteBuffer.wrap(previous));
                                if (replacement.getVertexes().length != skn.getVertexes().length) {
                                    System.out.println("Mismatched vertex counts replacing " + entry);
                                    return previous;
                                }
                                skn.setVertexes(replacement.getVertexes());
                                skn.setIndices(replacement.getIndices());
                                return skn.update();
                            };
                        }
                        replacements.add(new Replacement(name, replacementGenerator, discardsPrevious, path));
                    }
                }
            }
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
    }

    public List<Replacement> getReplacements() {
        return replacements;
    }

    public static class Replacement {
        public final String name;
        public final ReplacementGenerator generator;
        public final boolean discardsPrevious;
        public final Path path;

        public Replacement(String name, ReplacementGenerator generator, boolean discardsPrevious, Path path) {
            this.name = name;
            this.generator = generator;
            this.discardsPrevious = discardsPrevious;
            this.path = path;
        }
    }
}
