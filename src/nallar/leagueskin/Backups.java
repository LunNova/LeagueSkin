package nallar.leagueskin;

import nallar.leagueskin.util.PathUtil;
import nallar.leagueskin.util.Throw;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Backups {
    public static final Backups INSTANCE = new Backups();
    private List<Path> deletions = new ArrayList<Path>();

    // Backup names, as the full name of the file in the backup. EG, /Data/BananaBanana/Soraka.skn
    final Set<String> backupNames = new HashSet<>();
    final Path location = PathUtil.backupDir();

    public Backups() {
        try {
            Files.createDirectories(location);
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
        recursiveSearch(location);
    }

    private Path pathFromString(String path) {
        return location.resolve(PathUtil.canonical(path));
    }

    public byte[] getBytes(String path) {
        path = PathUtil.canonical(path);
        if (!backupNames.contains(path)) {
            throw new RuntimeException("No backup for " + path);
        }
        try {
            return Files.readAllBytes(pathFromString(path));
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
    }

    public void setBytes(String path, byte[] bytes) {
        path = PathUtil.canonical(path);
        if (!backupNames.add(path)) {
            Log.trace("Not backing up " + path + ", already saved");
            return;
        }
        try {
            Path target = pathFromString(path);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
    }

    public void delete(String path) {
        path = PathUtil.canonical(path);
        if (!backupNames.remove(path)) {
            throw new RuntimeException("No backup for " + path);
        }
        deletions.add(pathFromString(path));
    }

    private void recursiveSearch(Path path) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    recursiveSearch(entry);
                    continue;
                }
                String rafPath = PathUtil.canonical(location.relativize(entry).toString());
                backupNames.add(rafPath);
            }
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
    }

    public ReplacementGenerator getReplacementGenerator(String path_) {
        String path = PathUtil.canonical(path_);
        if (!backupNames.contains(path)) {
            throw new RuntimeException("No backup for " + path);
        }
        return bytes -> {
            bytes = getBytes(path);
            delete(path);
            return bytes;
        };
    }

    public void finish() {
        for (Path p : deletions) {
            if (!Files.exists(p)) {
                throw new RuntimeException(p + " should exist to delete");
            }
            try {
                Files.delete(p);
            } catch (IOException e) {
                throw Throw.sneaky(e);
            }
        }
        deletions.clear();
    }

    public boolean has(String match) {
        return backupNames.contains(PathUtil.canonical(match));
    }
}
