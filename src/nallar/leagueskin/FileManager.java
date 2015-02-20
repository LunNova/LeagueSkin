package nallar.leagueskin;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import nallar.leagueskin.riotfiles.*;
import nallar.leagueskin.util.Throw;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileManager {
    private final List<FileSource> fileSourceList = new ArrayList<>();
    private final ArrayListMultimap<String, String> shortNamesToLong = ArrayListMultimap.create();
    private final Map<String, FileEntry> entries = Maps.newHashMap();
    private final FileStatusManager fileStatusManager = new FileStatusManager();

    private static LoadingCache<String, ReplacementGeneratorWrapper> newReplacementsCache() {
        return CacheBuilder.newBuilder().build(new CacheLoader<String, ReplacementGeneratorWrapper>() {
            @Override
            public ReplacementGeneratorWrapper load(String key) throws Exception {
                return new ReplacementGeneratorWrapper();
            }
        });
    }

    public FileManager(Path rafDirectory, Path airDirectory) {
        recursiveSearch(rafDirectory, 0);
        fileSourceList.add(new AirFileSource(airDirectory));

        for (FileSource fileSource : fileSourceList) {
            for (FileEntry entry : fileSource.getEntries()) {
                shortNamesToLong.put(entry.getFileName().toLowerCase(), entry.getPath());
                entries.put(entry.getPath(), entry);
            }
        }
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
            StringBuilder sb = new StringBuilder();
            sb.append("Updating ").append(efficientReplacements.size()).append(" files:");
            efficientReplacements.asMap().forEach((name, replacement) -> sb.append('\n').append(name.startsWith("/DATA/") ? name.substring(6) : name));
            Log.info(sb.toString());
            fileSourceList.forEach((raf) -> raf.update(efficientReplacements.asMap()));
        }

        fileStatusManager.saveStatus();
        Backups.INSTANCE.finish();
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
            match = matchPart.substring(indexDollar + 2, matchPart.lastIndexOf('/'));
        }
        // TODO: Refactor to return list of names, have * select all instead of requiring single match
        List<String> names;
        if (shortName.contains("^")) {
            names = new ArrayList<>();
            String extension = shortName.substring(shortName.indexOf('.'));
            String start = shortName.substring(0, shortName.indexOf('^'));
            for (String name : shortNamesToLong.keySet()) {
                if (name.startsWith(start) && name.endsWith(extension)) {
                    int c = name.charAt(start.length());
                    boolean matched = c == '.';
                    if (!matched && c == '_') {
                        c = name.charAt(start.length() + 1);
                        if (c >= '0' && c <= '9') {
                            matched = true;
                        }
                    }
                    if (matched) {
                        names.addAll(shortNamesToLong.get(name));
                    }
                }
            }
        } else {
            names = shortNamesToLong.get(shortName);
            if (names.size() > 1) {
                if (match == null) {
                    for (String name : names) {
                        if (!name.startsWith("AIR/") && !name.endsWith(".dds")) {
                            throw new RuntimeException("Multiple possible full names for " + shortName + "(path: " + realPath + "), please specify the full name.  Got " + names);
                        }
                    }
                    Log.warn("Multiple possible full names for " + shortName + "(path: " + realPath + "), please specify the full name.  Got " + names);
                } else if (!match.isEmpty()) {
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
                    fileSourceList.add(new Raf(entry));
                }
            }
        } catch (IOException e) {
            throw Throw.sneaky(e);
        }
    }

    public FileEntry getEntry(String match) {
        return entries.get(match);
    }
}
