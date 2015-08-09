package nallar.leagueskin.riotfiles;

import nallar.leagueskin.ReplacementGeneratorWrapper;

import java.util.*;

public interface FileSource {
	Collection<? extends FileEntry> getEntries();

	void update(Map<String, ReplacementGeneratorWrapper> replacements);
}
