package nallar.leagueskin;

import java.nio.file.*;
import java.util.concurrent.*;

public class DDSConverter {
	public static void convert(Path in, Path out) {
		// TODO don't hardcode
		if (Files.exists(out)) {
			return;
		}
		ProcessBuilder processBuilder = new ProcessBuilder("C:/Program Files/ImageMagick-6.9.0-Q16/convert.exe", in.toAbsolutePath().toString(), "-define", "dds:compression=dxt5", "-define", "dds:mipmaps=8", out.toAbsolutePath().toString());
		processBuilder.redirectErrorStream(true);
		try {
			Process p = processBuilder.start();
			if (!p.waitFor(10, TimeUnit.SECONDS)) {
				Log.warn("DDS conversion of " + in + " to " + out + " taking more than 10 seconds, given up.");
				p.destroyForcibly();
				Files.delete(out);
			}
		} catch (Throwable e) {
			Log.error("Failed to convert " + in + " to DDS", e);
		}
	}

	private static String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}
}
