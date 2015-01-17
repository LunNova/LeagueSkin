package nallar.leagueskin;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import nallar.leagueskin.riotfiles.Raf;
import nallar.leagueskin.riotfiles.ReleaseManifest;
import nallar.leagueskin.util.Throw;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RafManager {
//    private static final List<String> testSKN = new ArrayList<>();
//
//    static {
//        //testSKN.add(".skn");
//    }
//
//    private static final List<String> extractObj = new ArrayList<>();
//
//    static {
//        //extractObj.add(".skn");
//    }
//
//    private static final List<String> testExtract = new ArrayList<>();
//
//    static {
//        //testExtract.add(".ini");
//    }

    private final List<Raf> rafList = new ArrayList<>();
    private final ArrayListMultimap<String, String> shortNamesToLong = ArrayListMultimap.create();
    private final FileStatusManager fileStatusManager = new FileStatusManager();

    private static LoadingCache<String, ReplacementGeneratorWrapper> newReplacementsCache() {
        return CacheBuilder.newBuilder().build(new CacheLoader<String, ReplacementGeneratorWrapper>() {
            @Override
            public ReplacementGeneratorWrapper load(String key) throws Exception {
                return new ReplacementGeneratorWrapper();
            }
        });
    }

    public RafManager(Path directory) {
        recursiveSearch(directory, 0);

        for (Raf raf : rafList) {
            for (Raf.RAFEntry entry : raf.getEntries()) {
                shortNamesToLong.put(entry.getShortName().toLowerCase(), entry.name);
            }
        }

        rafList.forEach(Raf::fixManifest);
        ReleaseManifest.INSTANCE.sanityCheck();
    }

    public void installSkinPack(SkinPack skinPack) {
        LoadingCache<String, ReplacementGeneratorWrapper> replacements = newReplacementsCache();
        skinPack.getReplacements().forEach(replacement -> {
            String name = replacement.name;
            List<String> names = getFullNames(name, replacement.path);
            for (String fullName : names) {
                replacements.getUnchecked(fullName).addGenerator(replacement.generator, replacement.discardsPrevious, replacement.path);
            }
        });

        LoadingCache<String, ReplacementGeneratorWrapper> efficientReplacements = newReplacementsCache();
        fileStatusManager.findChangedStatus(replacements, efficientReplacements);

        if (efficientReplacements.size() == 0) {
            Log.info("No files need updated");
        } else {
            Log.info("Updating " + efficientReplacements.size() + " files");
            rafList.forEach((raf) -> raf.update(efficientReplacements.asMap()));
        }

        fileStatusManager.saveStatus();
        Backups.INSTANCE.finish();

//        List<String> generatedExtract = new ArrayList<>(); // TODO: fix, broken after refactoring
//
//        rafList.forEach((raf) -> {
//            for (Raf.RAFEntry entry : raf.getEntries()) {
//                boolean extract = false;
//                for (String test : generatedExtract) {
//                    if (entry.name.toLowerCase().endsWith(test)) {
//                        extract = true;
//                    }
//                }
//                if (false && extract) {
//                    Skn made = new Skn(entry.name, ByteBuffer.wrap(entry.getBytes()));
//                    Obj obj = new Obj();
//                    obj.setIndices(made.getIndices());
//                    obj.setVertexes(made.getVertexes());
//                    obj.save(Paths.get("./test/generated/" + entry.getShortName() + ".obj"));
//                }
//
//                extract = false;
//                for (String test : testExtract) {
//                    if (entry.name.toLowerCase().endsWith(test)) {
//                        extract = true;
//                    }
//                }
//                if (extract) {
//                    try {
//                        Files.write(Paths.get("./test/extract/" + entry.getShortName()), entry.getBytes());
//                    } catch (Exception e) {
//                        throw new RuntimeException("Error extracting " + entry.getShortName(), e);
//                    }
//                }
//            }
//        });
//
//        //rafList.forEach(nallar.leagueskin.riotfiles.RAF::dump);
//        rafList.forEach((raf) -> {
//            for (Raf.RAFEntry entry : raf.getEntries()) {
//                boolean makeSkn = false;
//                for (String test : testSKN) {
//                    if (entry.name.toLowerCase().endsWith(test)) {
//                        makeSkn = true;
//                    }
//                }
//                if (makeSkn) {
//                    Skn made;
//                    try {
//                        made = new Skn(entry.name, ByteBuffer.wrap(entry.getBytes()));
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        continue;
//                    }
//                    boolean objSkin = false;
//                    for (String test : extractObj) {
//                        if (entry.name.toLowerCase().endsWith(test)) {
//                            objSkin = true;
//                        }
//                    }
//                    if (objSkin) {
//                        Obj obj = new Obj();
//                        obj.setIndices(made.getIndices());
//                        obj.setVertexes(made.getVertexes());
//                        obj.save(Paths.get("./test/skinify/" + entry.getShortName() + ".obj"));
//                    }
//                }
//            }
//        });
    }

    public List<String> getFullNames(String shortName, Path realPath) {
        int index = shortName.lastIndexOf('.');
        if (index == -1) {
            throw new RuntimeException("Should have filetype");
        }
        index = shortName.lastIndexOf('.', index - 1);
        String match = null;
        if (index != -1) {
            match = shortName.substring(0, index);
            shortName = shortName.substring(index + 1);
        }
        if (realPath.toString().contains("$$")) {
            if (match != null) {
                throw new RuntimeException("Can't use match " + match + " and $$ notation.");
            }
            String matchPart = realPath.toString().replace('\\', '/');
            int indexDollar = matchPart.indexOf("$$");
            match = matchPart.substring(indexDollar + 2, matchPart.indexOf('/', indexDollar));
        }
        // TODO: Refactor to return list of names, have * select all instead of requiring single match
        List<String> names = shortNamesToLong.get(shortName);
        if (names.size() > 1) {
            if (match == null) {
                throw new RuntimeException("Multiple possible full names for " + shortName + "(path: " + realPath + "), please specify the full name.  Got " + names);
            }
            if (!match.isEmpty()) {
                boolean immediateEnding = !match.endsWith("$");
                if (!immediateEnding) {
                    match = match.substring(0, match.length() - 1);
                }
                match = match.replace('.', '/').toLowerCase() + (immediateEnding ? '/' + shortName : "");
                List<String> newNames = new ArrayList<>();
                for (String name : names) {
                    if (name.toLowerCase().contains(match)) {
                        newNames.add(name);
                    }
                }
                names = newNames;
            }
        }

        if (names.size() == 0) {
            Log.warn("File " + shortName + " (match: " + match + ", path: " + realPath + ") does not exist in RAFs.");
            if (match != null) {
                throw new RuntimeException("File " + shortName + " (match: " + match + ", path: " + realPath + ") does not exist in RAFs.");
            }
        }
        return names;
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
