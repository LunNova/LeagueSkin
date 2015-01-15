package nallar.leagueskin.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RiotUtil {
    public static Path leagueDirectory() {
        return Paths.get("C:/Riot Games/League of Legends");
    }

    public static Path gameclientDirectory() {
        return leagueDirectory().resolve("RADS/projects/lol_game_client/");
    }

    public static Path filearchivesDirectory() {
        return gameclientDirectory().resolve("filearchives");
    }

    public static Path releasesDirectory() {
        return gameclientDirectory().resolve("releases");
    }

    public static Path releaseDirectory() {
        //List<Path> candidates = new ArrayList<>();
        int maximum = -1;
        Path maximumCandidate = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(releasesDirectory())) {
            for (Path path : stream) {
                String lastPart = path.getFileName().toString();
                String[] split = lastPart.split("\\.");
                try {
                    int count = 0;
                    for (int i = 0; i < split.length; i++) {
                        count += Integer.valueOf(split[i]) << (8 * (split.length - (i + 1)));
                    }
                    if (count > maximum) {
                        if (Files.exists(path.resolve("releasemanifest"))) {
                            maximum = count;
                            maximumCandidate = path;
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
        if (maximumCandidate == null) {
            throw new RuntimeException("No candidate paths");
        }
        return maximumCandidate;
    }
}
