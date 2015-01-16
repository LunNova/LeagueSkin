package nallar.leagueskin;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import nallar.leagueskin.util.PathUtil;
import nallar.leagueskin.util.Throw;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileStatusManager {
    private static final ArrayListMultimap<String, FileStatus> currentStatus = ArrayListMultimap.create();
    private static ArrayListMultimap<String, FileStatus> lastStatus = ArrayListMultimap.create();
    private final Path fileStatusLocation;

    public FileStatusManager() {
        fileStatusLocation = PathUtil.configDir().resolve("fileStatus.config");
        if (Files.exists(fileStatusLocation)) {
            try (ObjectInputStream o = new ObjectInputStream(new FileInputStream(fileStatusLocation.toFile()))) {
                lastStatus = (ArrayListMultimap<String, FileStatus>) o.readObject();
            } catch (Throwable t) {
                System.out.println("Failed to read last file status");
                t.printStackTrace();
            }
        }
    }

    public void saveStatus() {
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(fileStatusLocation.toFile()))) {
            o.writeObject(currentStatus);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void findChangedStatus(LoadingCache<String, ReplacementGeneratorWrapper> allReplacements, LoadingCache<String, ReplacementGeneratorWrapper> changedReplacements) {
        for (Map.Entry<String, ReplacementGeneratorWrapper> e : allReplacements.asMap().entrySet()) {
            String key = e.getKey();
            ReplacementGeneratorWrapper replacement = e.getValue();
            for (Path p : replacement.getPaths()) {
                currentStatus.put(key, new FileStatus(p));
            }
        }
        Set<String> combinedKeys = new HashSet<>();
        combinedKeys.addAll(currentStatus.keySet());
        combinedKeys.addAll(lastStatus.keySet());
        for (String key : combinedKeys) {
            List<FileStatus> current = currentStatus.get(key);
            List<FileStatus> old = lastStatus.get(key);

            if (current.size() == 0) {
                assert old.size() != 0;
                // TODO: Add action to replace from backup
                changedReplacements.getUnchecked(key).addGenerator(Backups.INSTANCE.getReplacementGenerator(key), true, null);
            }
            if (!old.equals(current)) {
                changedReplacements.put(key, allReplacements.getIfPresent(key));
            }
        }
    }

    private static class FileStatus {
        long size = 0;
        long date = 0;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FileStatus)) return false;

            FileStatus that = (FileStatus) o;

            return date == that.date && size == that.size;

        }

        @Override
        public int hashCode() {
            int result = (int) (size ^ (size >>> 32));
            result = 31 * result + (int) (date ^ (date >>> 32));
            return result;
        }

        private FileStatus(Path path) {
            if (!Files.exists(path)) {
                throw new RuntimeException("Shouldn't retrieve status for file which is not present");
            }
            try {
                size = Files.size(path);
                date = Files.getLastModifiedTime(path).toMillis();
            } catch (IOException e) {
                throw Throw.sneaky(e);
            }
        }
    }
}
