package nallar.leagueskin;

import nallar.leagueskin.models.Obj;
import nallar.leagueskin.util.Throw;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SkinPack {
    private static final Set<String> claimed = new HashSet<>();
    public final Map<String, Object> replacements = new HashMap<>();
    private final Map<Path, FileStatus> fileStatusMap = new HashMap<>();
    final String match;

    public SkinPack(Path folder) {
        this(folder, null);
    }

    public SkinPack(Path folder, String match) {
        this.match = match;
        recursiveSearch(folder, replacements);
    }

    private void recursiveSearch(Path path, Map<String, Object> replacements) {
        path = path.toAbsolutePath();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    recursiveSearch(entry, replacements);
                } else {
                    String name = shortNameFromPath(entry);
                    String longName = entry.toString().toLowerCase().replace("\\", "/");

                    if (!name.startsWith("!") && !longName.contains("/!") && (match == null || name.contains(match)) && claimed.add(name)) {
                        updateFileStatus(path);
                        if (name.endsWith(".obj")) {
                            name = name.replace(".obj", ".skn");
                            Obj replacement = new Obj();
                            replacement.load(entry);
                            replacements.put(name, replacement);
                        } else {
                            replacements.put(name, entry);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
        for (FileStatus fileStatus : fileStatusMap.values()) {
            if (!fileStatus.needsUpdate) {
                replacements.remove(shortNameFromPath(fileStatus.path));
            }
            if (!fileStatus.exists) {
                // TODO: Check backups and add restore action? Then remove from fileStatus map
            }
        }
    }

    private static String shortNameFromPath(Path p) {
        return p.getFileName().toString().toLowerCase().replace("\\", "/");
    }

    private FileStatus updateFileStatus(Path p) {
        FileStatus s = fileStatusMap.get(p);
        if (s == null) {
            s = new FileStatus(p);
            fileStatusMap.put(p, s);
        }
        s.update();
        return s;
    }

    private static class FileStatus {
        final Path path;
        long lastSize = 0;
        long lastDate = 0;
        boolean needsUpdate = false;
        boolean exists = false;

        private FileStatus(Path path) {
            this.path = path;
        }

        void update() {
            if (!Files.exists(path)) {
                throw new RuntimeException("Shouldn't retrieve status for file which is not present");
            }
            exists = true;
            try {
                long size = Files.size(path);
                long date = Files.getLastModifiedTime(path).toMillis();

                if (size != lastSize || date != lastDate) {
                    needsUpdate = true;
                }

                lastSize = size;
                lastDate = date;
            } catch (IOException e) {
                throw Throw.sneaky(e);
            }
        }
    }
}
