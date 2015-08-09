package nallar.leagueskin.riotfiles;

import nallar.leagueskin.Backups;
import nallar.leagueskin.Log;
import nallar.leagueskin.ReplacementGeneratorWrapper;
import nallar.leagueskin.util.Throw;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Parses Riot Archive Format (.RAF) files
 * NOT THREADSAFE
 */
public class Raf implements FileSource {
	static {
		testRafHash();
	}

	private static void testRafHash(String s, int expected) {
		int result = rafHash(s);
		if (result != expected) {
			throw new Error("Unexpected rafHash result. Used " + s + ", expected: " + expected + ", got: " + result);
		}
	}

	private static void testRafHash() {
		testRafHash("DATA/Characters/Corki/Corki.inibin", 118575870);
	}

	private static final boolean DEBUG_PARSE = Boolean.getBoolean("leagueskin.debug.parse");
	private static final boolean DEBUG_DUMP = true; //Boolean.getBoolean("leagueskin.debug.dump");
	private static final byte[] inflateBuffer = new byte[1024 * 1024];
	private static final byte[] deflateBuffer = new byte[1024 * 1024];
	private final Path location;
	private final String name;
	private final MappedByteBuffer buffer;
	private List<RafEntry> rafEntryList = new ArrayList<>();
	private Set<String> fileNames = new HashSet<>();

	public Raf(Path location) {
		this.location = location;
		String name = location.toString().replace("\\", "/");
		this.name = name.substring(name.lastIndexOf('/', name.lastIndexOf('/') - 1) + 1);
		MappedByteBuffer b;
		try (RandomAccessFile file = new RandomAccessFile(location.toFile(), "rw")) {
			b = file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, file.length());
		} catch (IOException e) {
			throw new RuntimeException("Failed to open RAF file " + location, e);
		}
		b.order(ByteOrder.LITTLE_ENDIAN);
		this.buffer = b;
		parse();
		int fixed = fixManifest();
		if (fixed != 0) {
			Log.info("Corrected " + fixed + " manifest entries.");
		}
	}

	private static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	/**
	 * Returns RAF Hash for given name - Do not use for anything else, terrible hash function. So many collisions!
	 *
	 * @param name
	 * @return
	 */
	private static int rafHash(String name) {
		if (name.startsWith("/")) {
			name = name.substring(1); // Strip first slash, expected format is DATA/Characters/Corki/Corki.inibin
		}
		name = name.toLowerCase();
		int temp;
		int hash = 0;
		for (int i = 0; i < name.length(); i++) {
			hash = (hash << 4) + name.charAt(i);
			temp = hash & 0xF0000000;
			if (temp != 0) {
				hash = hash ^ (temp >>> 24);
				hash = hash ^ temp;
			}
		}
		return hash;
	}

	private static byte[] decompress(byte[] input) {
		Inflater inflater = new Inflater();
		inflater.setInput(input);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (!inflater.finished()) {
			int n;
			try {
				n = inflater.inflate(inflateBuffer);
				if (n == 0) {
					Log.warn("Needs input: " + inflater.needsInput());
					Log.warn("Needs dictionary: " + inflater.needsInput());
					throw new RuntimeException("Failed to decompress, bad input");
				}
			} catch (DataFormatException e) {
				throw Throw.sneaky(e);
			}
			baos.write(inflateBuffer, 0, n);
		}
		return baos.toByteArray();
	}

	private static byte[] compress(byte[] input) {
		Deflater deflater = new Deflater();
		deflater.setInput(input);
		deflater.finish();
		byte[] compressed;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			while (!deflater.finished()) {
				int n = deflater.deflate(deflateBuffer);
				baos.write(deflateBuffer, 0, n);
			}
			compressed = baos.toByteArray();
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}
		deflater.end();
		if (!Arrays.equals(decompress(compressed), input)) {
			throw new RuntimeException("Mismatch!");
		}
		return compressed;
	}

	@Override
	public Collection<FileEntry> getEntries() {
		return new ArrayList<>(rafEntryList);
	}

	private int fixManifest() {
		int[] size = {0};
		rafEntryList.forEach((x) -> {
			if (ReleaseManifest.INSTANCE.setSize(x)) {
				size[0]++;
			}
		});
		return size[0];
	}

	@Override
	public void update(Map<String, ReplacementGeneratorWrapper> replacements) {
		if (Collections.disjoint(fileNames, replacements.keySet())) {
			return;
		}
		//dump();
		//debug("In common for " + this);
		List<RafEntry> sortedRafList = new ArrayList<>(rafEntryList);
		Collections.sort(sortedRafList, (s1, s2) -> Integer.compareUnsigned(s1.rafDatOffset, s2.rafDatOffset));

		// Rename .raf.dat to .raf.dat.bak
		// Open for reading, memory mapped.
		Path rafDat = Paths.get(location.toString() + ".dat");
		Path rafDatBak = Paths.get(location.toString() + ".dat.bak");
		try {
			Files.move(rafDat, rafDatBak);
		} catch (IOException e1) {
			if (Files.exists(rafDatBak)) {
				try {
					Files.delete(rafDat);
				} catch (IOException e2) {
					throw Throw.sneaky(e2);
				}
			} else {
				throw Throw.sneaky(e1);
			}
		}
		try (
			RandomAccessFile old = new RandomAccessFile(rafDatBak.toFile(), "r");
			RandomAccessFile created = new RandomAccessFile(rafDat.toFile(), "rw")
		) {
			for (RafEntry entry : sortedRafList) {
				int expectedSize = entry.size;
				int offset = (int) created.getFilePointer();
				int decompressedSize = 0;
				ReplacementGeneratorWrapper replacement = replacements.get(entry.getPath());
				if (old.getFilePointer() != entry.rafDatOffset) {
					Log.warn("FP should already be at correct offset in old data. Should be " + entry.rafDatOffset + ", got " + old.getFilePointer());
					old.seek(entry.rafDatOffset);
				}
				byte[] oldData = new byte[entry.size];
				old.readFully(oldData);
				if (replacement == null) {
					// Copy old
					created.write(oldData);
				} else {
					boolean compressed = false;
					if (entry.size >= 2) {
						int magic = ((oldData[0] & 0xff) << 8) | (oldData[1] & 0xff);
						compressed = (magic == 0x7801 || magic == 0x789c);
					}
					oldData = compressed ? decompress(oldData) : oldData;
					Backups.INSTANCE.setBytes(entry.getPath(), oldData);
					byte[] replacementData = replacement.apply(oldData);
					decompressedSize = replacementData.length;
					if (compressed) {
						replacementData = compress(replacementData);
					}
					expectedSize = replacementData.length;
					entry.expectedRawBytes = replacementData;
					created.write(replacementData);
				}
				entry.rafDatOffset = offset;
				entry.size = (int) created.getFilePointer() - offset;
				if (replacement != null) {
					ReleaseManifest.INSTANCE.setSize(entry.getPath(), entry.size, decompressedSize);
				}
				if (entry.size != expectedSize) {
					throw new RuntimeException("Mismatched sizes! Expected " + expectedSize + ", got " + entry.size);
				}
			}
		} catch (IOException e) {
			if (Files.exists(rafDatBak)) {
				if (Files.exists(rafDat)) {
					try {
						Files.delete(rafDat);
					} catch (IOException e1) {
						e.printStackTrace();
						throw Throw.sneaky(e1);
					}
				}
				try {
					Files.move(rafDatBak, rafDat);
				} catch (IOException e1) {
					e.printStackTrace();
					throw Throw.sneaky(e1);
				}
			}
			throw new RuntimeException("Failed to open RAF.dat file " + rafDatBak, e);
		}

		try {
			Files.delete(rafDatBak);
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}

		for (RafEntry entry : rafEntryList) {
			buffer.position(entry.rafOffset + 4); // skip hash
			buffer.putInt(entry.rafDatOffset);
			buffer.putInt(entry.size);
		}
		buffer.force();

		sanityCheck();
	}

	private void sanityCheck() {
		rafEntryList.forEach(RafEntry::checkExpectedBytes);

		List<FileEntry> oldList = new ArrayList<>(rafEntryList);
		rafEntryList.clear();
		parse();
		for (int i = 0; i < rafEntryList.size(); i++) {
			FileEntry old = oldList.get(i);
			RafEntry now = rafEntryList.get(i);
			if (!old.toString().equals(now.toString())) {
				Log.warn("Mismatch before/after reparse before: " + old + ", now: " + now);
			}
			if (now.hash != rafHash(now.getPath())) {
				Log.warn("Incorrect RAF hash for " + now + " got: " + now.hash + " expected " + rafHash(now.getFileName()));
			}
		}
	}

	private void parse() {
		buffer.position(0);

		// Header
		int magic = buffer.getInt();
		int version = buffer.getInt();
		int riotIndex = buffer.getInt();
		int fileListOffset = buffer.getInt();
		int stringTableOffset = buffer.getInt();

		if (DEBUG_PARSE) {
			Log.trace("Magic is " + Integer.toHexString(magic));
			Log.trace("Version is " + Integer.toHexString(version));
			Log.trace("Riot index is " + Integer.toHexString(riotIndex));
			Log.trace("File list offset is " + Integer.toHexString(fileListOffset));
			Log.trace("String table offset is " + Integer.toHexString(stringTableOffset));
		}

		// File list
		buffer.position(fileListOffset);
		int count = buffer.getInt();
		if (DEBUG_PARSE) {
			Log.trace("Entries in file list: " + count);
		}

		for (int i = 0; i < count; i++) {
			int rafOffset = buffer.position();
			int hash = buffer.getInt();
			int rafDatOffset = buffer.getInt();
			int size = buffer.getInt();
			int stringTableIndex = buffer.getInt();
			if (DEBUG_PARSE) {
				Log.trace("Hash is " + hash);
				Log.trace("Offset is " + rafDatOffset);
				Log.trace("Size is " + size);
				Log.trace("String table index is " + stringTableIndex);
			}
			rafEntryList.add(new RafEntry(rafOffset, rafDatOffset, size, stringTableIndex, hash));
		}

		// String table
		buffer.position(stringTableOffset);
		int stringTableSize = buffer.getInt();
		if (count != buffer.getInt()) {
			throw new RuntimeException("Disagreeing counts - string table count does not match file count");
		}

		for (int i = 0; i < count; i++) {
			int offset = buffer.getInt();

			int length = buffer.getInt();
			byte[] bytes = new byte[length - 1]; // -1 to remove null char, ignored.
			int oldPos = buffer.position();
			buffer.position(offset + stringTableOffset);
			buffer.get(bytes);
			String name;
			try {
				name = '/' + new String(bytes, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw Throw.sneaky(e);
			}
			for (RafEntry rafEntry : rafEntryList) {
				if (rafEntry.stringTableIndex == i) {
					rafEntry.path = name; // For consistency with ReleaseManifest names
					break;
				}
			}
			fileNames.add(name);
			if (DEBUG_PARSE) {
				Log.trace("String " + i + " offset " + offset);
				Log.trace("String " + i + " length " + length);
				Log.trace("String " + i + " is \"" + name + '"');
			}
			buffer.position(oldPos);
		}
	}

	public void dump() {
		int entries = rafEntryList.size();
		int size = 0;
		for (RafEntry rafEntry : rafEntryList) {
			if (DEBUG_DUMP) {
				Log.trace(rafEntry + " in " + name);
			}
			size += rafEntry.size;
		}
		Log.trace(entries + " entries in " + name + " RAF of total size " + humanReadableByteCount(size, false));
	}

	public String toString() {
		return name + " RAF with " + rafEntryList.size() + " entries";
	}

	public class RafEntry implements FileEntry {
		private String path;
		private int rafOffset;
		private int rafDatOffset;
		private int size;
		private int stringTableIndex;
		private int hash;
		private byte[] expectedRawBytes;

		public RafEntry(int rafOffset, int rafDatOffset, int size, int stringTableIndex, int hash) {
			this.rafOffset = rafOffset;
			this.rafDatOffset = rafDatOffset;
			this.size = size;
			this.stringTableIndex = stringTableIndex;
			this.hash = hash;
		}

		public String toString() {
			return path + " is of size " + humanReadableByteCount(size, false) + " at offset " + rafDatOffset;
		}

		@Override
		public String getPath() {
			return path;
		}

		public void checkExpectedBytes() {
			if (expectedRawBytes != null && !Arrays.equals(expectedRawBytes, getRawBytes())) {
				throw new RuntimeException("Mismatch for expected contents of " + this);
			}
		}

		@Override
		public byte[] getRawBytes() {
			Path rafDat = Paths.get(location.toString() + ".dat");
			byte[] data;
			try (RandomAccessFile raf = new RandomAccessFile(rafDat.toFile(), "r")) {
				data = new byte[size];
				raf.seek(rafDatOffset);
				raf.readFully(data);
			} catch (IOException e) {
				throw Throw.sneaky(e);
			}
			return data;
		}

		@Override
		public byte[] getDecompressedBytes() {
			byte[] data = getRawBytes();
			if (size >= 2) {
				short magic = (short) ((((data[0] & 0xff) << 8)) | (data[1] & 0xff));
				if (magic == 0x7801 || magic == 0x789c) {
					try {
						data = decompress(data);
					} catch (RuntimeException e) {
						throw new RuntimeException("Failed to decompress " + toString(), e);
					}
				}
			}
			return data;
		}

		@Override
		public int getSizeOnDisk() {
			return size;
		}
	}
}
