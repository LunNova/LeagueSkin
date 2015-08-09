package nallar.leagueskin.riotfiles;

public interface FileEntry {
	default String getFileName() {
		return getPath().substring(getPath().lastIndexOf('/') + 1);
	}

	String getPath();

	byte[] getRawBytes();

	byte[] getDecompressedBytes();

	int getSizeOnDisk();
}
