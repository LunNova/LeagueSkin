package nallar.leagueskin.riotfiles;

import nallar.leagueskin.ReplacementGeneratorWrapper;

import java.util.Collection;
import java.util.Map;

public interface FileSource {
    Collection<? extends FileEntry> getEntries();

    void update(Map<String, ReplacementGeneratorWrapper> replacements);
}
