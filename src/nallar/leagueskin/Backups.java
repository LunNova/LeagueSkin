package nallar.leagueskin;

import nallar.leagueskin.util.PathUtil;
import nallar.leagueskin.util.Throw;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class Backups {
    public static final Backups INSTANCE = new Backups();

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

    public byte[] getRawBytes(String path) {
        if (!backupNames.contains(path)) {
            throw new RuntimeException("No backup for " + path);
        }
        try {
            return Files.readAllBytes(location.resolve(path));
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
    }

    public void setRawBytes(String path, byte[] rawBytes) {
        if (!backupNames.add(path)) {
            System.out.println("Not backing up " + path + ", already saved");
            return;
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try {
            Path target = location.resolve(path);
            Files.createDirectories(target.getParent());
            Files.write(target, rawBytes);
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
    }

    public void delete(String path) {
        if (!backupNames.remove(path)) {
            throw new RuntimeException("No backup for " + path);
        }
    }

    private void recursiveSearch(Path path) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    recursiveSearch(entry);
                    continue;
                }
                String rafPath = location.relativize(path).toString();
                if (!rafPath.startsWith("/")) {
                    rafPath = '/' + rafPath;
                }
                backupNames.add(rafPath);
            }
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
    }
}
