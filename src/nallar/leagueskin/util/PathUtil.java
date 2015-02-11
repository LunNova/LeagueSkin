package nallar.leagueskin.util;

import nallar.leagueskin.Log;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtil {
    public static Path dataDir() {
        return Paths.get("test/");
    }

    public static Path configDir() {
        return dataDir().resolve("config");
    }

    public static Path backupDir() {
        return dataDir().resolve("backups");
    }

    public static Path leagueDirectory() {
        return Paths.get("C:/Riot Games/League of Legends");
    }

    public static Path gameDirectory() {
        return leagueDirectory().resolve("RADS/projects/lol_game_client/");
    }

    public static Path filearchivesDirectory() {
        return gameDirectory().resolve("filearchives");
    }

    public static Path releasesDirectory() {
        return gameDirectory().resolve("releases");
    }

    public static Path airDirectory() {
        return leagueDirectory().resolve("RADS/projects/lol_air_client");
    }

    public static Path airReleasesDirectory() {
        return airDirectory().resolve("releases");
    }

    public static Path airDeployDirectory() {
        return releaseDirectory(airReleasesDirectory()).resolve("deploy");
    }

    public static Path releaseDirectory(Path releases) {
        //List<Path> candidates = new ArrayList<>();
        int maximum = -1;
        Path maximumCandidate = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(releases)) {
            for (Path path : stream) {
                String lastPart = path.getFileName().toString();
                String[] split = lastPart.split("\\.");
                try {
                    int count = 0;
                    for (int i = 0; i < split.length; i++) {
                        int part = Integer.valueOf(split[i]);
                        if (part > 255) {
                            Log.warn("part: " + part + " in " + path + " higher than 255, ignoring.");
                        }
                        count += part << (8 * (split.length - (i + 1)));
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

    public static String canonical(String path) {
        path = path.replace('\\', '/');
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }
}
