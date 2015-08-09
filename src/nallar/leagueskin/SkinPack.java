package nallar.leagueskin;

import nallar.leagueskin.models.ModelTransfer;
import nallar.leagueskin.models.Obj;
import nallar.leagueskin.models.Skn;
import nallar.leagueskin.util.Throw;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

public class SkinPack {
	final String match;

	private List<Replacement> replacements = new ArrayList<>();

	public SkinPack(Path folder) {
		this(folder, null);
	}

	public SkinPack(Path folder, String match) {
		this.match = match;
		preSearch(folder);
		//if (true) { throw null; }
		recursiveSearch(folder);
	}

	private static String shortNameFromPath(Path p) {
		return p.getFileName().toString().toLowerCase().replace("\\", "/");
	}

	private static void preSearch(Path path) {
		Path workingDir = Paths.get("").toAbsolutePath();
		path = path.toAbsolutePath();
		boolean autoDDS = false;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entryFull : stream) {
				Path entry = workingDir.relativize(entryFull);
				String name = shortNameFromPath(entry);
				if (name.equalsIgnoreCase("!autoDDS")) {
					autoDDS = true;
					Files.delete(entry);
				}
				if (name.startsWith("!")) {
					continue;
				}
				if (Files.isDirectory(entry)) {
					preSearch(entry);
					continue;
				}
				if (autoDDS && (name.endsWith(".jpg") || name.endsWith(".png"))) {
					if (name.contains("_splash")) {
						continue;
					}
					String extension = name.substring(name.lastIndexOf('.') + 1);
					String mainPart = name.substring(0, name.contains("^.") ? name.indexOf('^', 1) : name.lastIndexOf('_'));
					if (name.endsWith("_0." + extension) && name.contains("_square")) {
						name = mainPart + "^." + extension;
						Path oldEntry = entry;
						entry = entry.getParent().resolve(name);
						Files.move(oldEntry, entry);
					}
					String nameLower = name.toLowerCase();
					String champ = name.substring(0, name.contains("_") ? name.indexOf('_') : name.indexOf('^', 1));
					if (nameLower.contains("_square^.")) {
						Path out = entry.getParent().resolve(champ + "_square^.dds");
						DDSConverter.convert(entry, out);
						Path circle = entry.getParent().resolve(champ + "_circle^.dds");
						if (!Files.exists(circle)) {
							Files.copy(out, circle);
						}
					} else {
						Path out = entry.getParent().resolve(champ + "loadscreen^.dds");
						DDSConverter.convert(entry, out);
					}
				}
			}
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}
	}

	private void recursiveSearch(Path path) {
		Path workingDir = Paths.get("").toAbsolutePath();
		path = path.toAbsolutePath();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entryFull : stream) {
				Path entry = workingDir.relativize(entryFull);
				String name = shortNameFromPath(entry);
				if (name.startsWith("!")) {
					continue;
				}
				if (Files.isDirectory(entry)) {
					recursiveSearch(entry);
					continue;
				}
				if (match == null || name.contains(match)) {
					boolean discardsPrevious = true;
					ReplacementGenerator replacementGenerator = previous -> Files.readAllBytes(entry);
					if (name.endsWith(".obj")) {
						discardsPrevious = false;
						name = name.replace(".obj", ".skn");
						Obj replacement = new Obj();
						try {
							replacement.load(entry);
						} catch (Throwable t) {
							throw new RuntimeException("Failed to load " + entry, t);
						}
						final String finalName = name;
						replacementGenerator = previous -> {
							Skn skn = new Skn(finalName, ByteBuffer.wrap(previous));
							try {
								return ModelTransfer.transfer(skn, replacement);
							} catch (Exception e) {
								Log.error("Error replacing " + entry, e);
								return previous;
							}
						};
					}
					replacements.add(new Replacement(name, replacementGenerator, discardsPrevious, entry));
				}
			}
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}
	}

	public List<Replacement> getReplacements() {
		return replacements;
	}

	public static class Replacement {
		public final String name;
		public final ReplacementGenerator generator;
		public final boolean discardsPrevious;
		public final Path path;

		public Replacement(String name, ReplacementGenerator generator, boolean discardsPrevious, Path path) {
			this.name = name;
			this.generator = generator;
			this.discardsPrevious = discardsPrevious;
			this.path = path;
		}

		@Override
		public String toString() {
			return "Replacement{" +
				"name='" + name + '\'' +
				", generator=" + generator +
				", discardsPrevious=" + discardsPrevious +
				", path=" + path +
				'}';
		}
	}
}
