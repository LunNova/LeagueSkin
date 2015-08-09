package nallar.leagueskin.util;

import java.io.*;

public class FriendlyException extends RuntimeException {
	public FriendlyException(String message) {
		super(message);
	}

	@Override
	public StackTraceElement[] getStackTrace() {
		return new StackTraceElement[0];
	}

	@Override
	public void printStackTrace(PrintStream s) {
		s.println(this);
	}

	@Override
	public String toString() {
		return getMessage();
	}
}
