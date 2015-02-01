package nallar.leagueskin;

import nallar.leagueskin.models.Obj;
import nallar.leagueskin.models.Skn;
import nallar.leagueskin.riotfiles.Raf;
import nallar.leagueskin.riotfiles.ReleaseManifest;
import nallar.leagueskin.util.PathUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DevUtil {
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new Scanner(System.in).nextLine().split(" ");
        }
        String matcher = args[0];
        System.out.println(Arrays.toString(args));
        boolean extractMatches = false;
        boolean extractObjMatches = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("extract")) {
                extractMatches = true;
            }
            if (arg.equalsIgnoreCase("extractObj")) {
                extractObjMatches = true;
            }
        }
        Pattern p = matcher.isEmpty() ? null : Pattern.compile(matcher, Pattern.CASE_INSENSITIVE);
        Stream<String> matches = ReleaseManifest.INSTANCE.getFileNames().stream().filter(s -> p == null || p.matcher(s).find());
        RafManager rafManager = new RafManager(PathUtil.filearchivesDirectory());

        try {
            Files.createDirectories(PathUtil.dataDir().resolve("extract/"));
            Files.createDirectories(PathUtil.dataDir().resolve("skinify/"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        final boolean finalExtractObjMatches = extractObjMatches;
        final boolean finalExtractMatches = extractMatches;
        matches.forEach((match) -> {
            Log.info(match + (Backups.INSTANCE.has(match) ? " - replaced with custom skin" : ""));
            Raf.RAFEntry entry = rafManager.getEntry(match);
            if (finalExtractMatches) {
                try {
                    Files.write(PathUtil.dataDir().resolve("extract/" + entry.getShortName()), entry.getBytes());
                } catch (Exception e) {
                    throw new RuntimeException("Error extracting " + entry.getShortName(), e);
                }
            }
            if (finalExtractObjMatches && match.endsWith(".skn")) {
                Skn made;
                try {
                    made = new Skn(entry.name, ByteBuffer.wrap(entry.getBytes()));
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                Obj obj = new Obj();
                obj.setIndices(made.getIndices());
                obj.setVertexes(made.getVertexes());
                obj.save(PathUtil.dataDir().resolve("skinify/" + entry.getShortName() + ".obj"));
            }
        });
    }
}
