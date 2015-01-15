package nallar.leagueskin;

import nallar.leagueskin.models.Obj;
import nallar.leagueskin.models.Skn;
import nallar.leagueskin.riotfiles.Raf;
import nallar.leagueskin.util.Throw;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RafManager {
    private static final List<String> testSKN = new ArrayList<>();

    static {
        //testSKN.add(".skn");
    }

    private static final List<String> extractObj = new ArrayList<>();

    static {
        //extractObj.add(".skn");
    }

    private static final List<String> testExtract = new ArrayList<>();

    static {
        //testExtract.add(".ini");
    }

    private final List<Raf> rafList = new ArrayList<>();

    public RafManager(Path directory) {
        recursiveSearch(directory, 0);

        SkinPack sknRep = new SkinPack(Paths.get("./test/Skins"), ".skn");
        System.out.println(sknRep.replacements);
        rafList.forEach((raf) -> {
            raf.update(sknRep.replacements);
            raf.fixManifest();
        });
        List<String> generatedExtract = new ArrayList<>(sknRep.replacements.keySet());

        rafList.forEach((raf) -> {
            for (Raf.RAFEntry entry : raf.getEntries()) {
                boolean extract = false;
                for (String test : generatedExtract) {
                    if (entry.name.toLowerCase().endsWith(test)) {
                        extract = true;
                    }
                }
                if (false && extract) {
                    Skn made = new Skn(entry.name, ByteBuffer.wrap(entry.getBytes()));
                    Obj obj = new Obj();
                    obj.setIndices(made.getIndices());
                    obj.setVertexes(made.getVertexes());
                    obj.save(Paths.get("./test/generated/" + entry.getShortName() + ".obj"));
                }

                extract = false;
                for (String test : testExtract) {
                    if (entry.name.toLowerCase().endsWith(test)) {
                        extract = true;
                    }
                }
                if (extract) {
                    try {
                        Files.write(Paths.get("./test/extract/" + entry.getShortName()), entry.getBytes());
                    } catch (Exception e) {
                        throw new RuntimeException("Error extracting " + entry.getShortName(), e);
                    }
                }
            }
        });

        SkinPack testSkinPack = new SkinPack(Paths.get("./test/Skins/"));
        System.out.println(testSkinPack.replacements);
        rafList.forEach((raf) -> raf.update(testSkinPack.replacements));

        //rafList.forEach(nallar.leagueskin.riotfiles.RAF::dump);
        rafList.forEach((raf) -> {
            for (Raf.RAFEntry entry : raf.getEntries()) {
                boolean makeSkn = false;
                for (String test : testSKN) {
                    if (entry.name.toLowerCase().endsWith(test)) {
                        makeSkn = true;
                    }
                }
                if (makeSkn) {
                    Skn made;
                    try {
                        made = new Skn(entry.name, ByteBuffer.wrap(entry.getBytes()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    boolean objSkin = false;
                    for (String test : extractObj) {
                        if (entry.name.toLowerCase().endsWith(test)) {
                            objSkin = true;
                        }
                    }
                    if (objSkin) {
                        Obj obj = new Obj();
                        obj.setIndices(made.getIndices());
                        obj.setVertexes(made.getVertexes());
                        obj.save(Paths.get("./test/skinify/" + entry.getShortName() + ".obj"));
                    }
                }
            }
        });
    }

    private void recursiveSearch(Path path, int depth) {
        if (depth > 1) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    recursiveSearch(entry, depth + 1);
                } else if (entry.toString().endsWith(".raf")) {
                    rafList.add(new Raf(entry));
                }
            }
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
    }
}
