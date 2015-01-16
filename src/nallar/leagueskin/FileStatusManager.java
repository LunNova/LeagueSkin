package nallar.leagueskin;

import nallar.leagueskin.util.Throw;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileStatusManager {
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
