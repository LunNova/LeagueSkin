package nallar.leagueskin.riotfiles;

import nallar.leagueskin.Backups;
import nallar.leagueskin.Log;
import nallar.leagueskin.ReplacementGeneratorWrapper;
import nallar.leagueskin.util.PathUtil;
import nallar.leagueskin.util.Throw;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AirFileSource implements FileSource {
	private Map<String, AirFileEntry> fileEntryMap = new HashMap<>();

	public AirFileSource(Path airDirectory) {
		recursiveSearch(airDirectory.resolve("assets"), airDirectory);
		recursiveSearch(airDirectory.resolve("mod"), airDirectory);
	}

	private void recursiveSearch(Path airDirectory, Path parent) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(airDirectory)) {
			for (Path entry : stream) {
				if (Files.isDirectory(entry)) {
					recursiveSearch(entry, parent);
					continue;
				}
				String rafPath = "AIR/" + PathUtil.canonical(parent.relativize(entry).toString());
				AirFileEntry f = new AirFileEntry(entry, rafPath, Files.size(entry));
				fileEntryMap.put(rafPath, f);
			}
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}
	}

	@Override
	public Collection<? extends FileEntry> getEntries() {
		return fileEntryMap.values();
	}

	@Override
	public void update(Map<String, ReplacementGeneratorWrapper> replacements) {
		if (Collections.disjoint(fileEntryMap.keySet(), replacements.keySet())) {
			return;
		}
		for (Map.Entry<String, ReplacementGeneratorWrapper> stringReplacementGeneratorWrapperEntry : replacements.entrySet()) {
			AirFileEntry entry = fileEntryMap.get(stringReplacementGeneratorWrapperEntry.getKey());
			if (entry == null) {
				continue;
			}
			ReplacementGeneratorWrapper replacement = stringReplacementGeneratorWrapperEntry.getValue();
			Log.info("Replacing " + entry + " with " + replacement);
			try {
				byte[] oldBytes = Files.readAllBytes(entry.entry);
				Backups.INSTANCE.setBytes(entry.getPath(), oldBytes);
				Files.write(entry.entry, replacement.apply(oldBytes));
			} catch (IOException e) {
				throw Throw.sneaky(e);
			}
		}
	}

	private static class AirFileEntry implements FileEntry {
		private final String path;
		private final Path entry;
		private final int size;

		public AirFileEntry(Path entry, String rafPath, long size) {
			path = rafPath;
			this.entry = entry;
			this.size = (int) size;
		}

		@Override
		public String toString() {
			return path + " of size " + size;
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public byte[] getRawBytes() {
			try {
				return Files.readAllBytes(entry);
			} catch (IOException e) {
				throw Throw.sneaky(e);
			}
		}

		@Override
		public byte[] getDecompressedBytes() {
			return getRawBytes();
		}

		@Override
		public int getSizeOnDisk() {
			return size;
		}
	}
}
