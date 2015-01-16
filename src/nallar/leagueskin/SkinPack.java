package nallar.leagueskin;

import nallar.leagueskin.models.Obj;
import nallar.leagueskin.models.Skn;
import nallar.leagueskin.util.Throw;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SkinPack {
    private static final Set<String> claimed = new HashSet<>();
    public final Map<String, ReplacementGenerator> replacements = new HashMap<>();
    final String match;
    private final RafManager manager;

    public SkinPack(Path folder, RafManager manager) {
        this(folder, manager, null);
    }

    public SkinPack(Path folder, RafManager manager, String match) {
        this.match = match;
        this.manager = manager;
        recursiveSearch(folder, replacements);
    }

    private static String shortNameFromPath(Path p) {
        return p.getFileName().toString().toLowerCase().replace("\\", "/");
    }

    private void recursiveSearch(Path path, Map<String, ReplacementGenerator> replacements) {
        path = path.toAbsolutePath();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                String forwardSlashName = entry.toString().replace('\\', '/');
                String name = shortNameFromPath(entry);
                if (Files.isDirectory(entry)) {
                    recursiveSearch(entry, replacements);
                } else if (!name.startsWith("!") && !forwardSlashName.contains("/!")) {
                    if ((match == null || name.contains(match)) && claimed.add(name)) {
                        ReplacementGenerator replacementGenerator = previous -> Files.readAllBytes(entry);
                        if (name.endsWith(".obj")) {
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
                        try {
                            List<String> names = manager.getFullNames(name, entry);
                            for (String fullName : names) {
                                // TODO: Chaining?
                                replacements.put(fullName, replacementGenerator);
                            }
                        } catch (Exception e) {e.printStackTrace();}
                    }
                }
            }
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
    }
}
