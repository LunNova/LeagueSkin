package nallar.leagueskin;

import nallar.leagueskin.riotfiles.ReleaseManifest;

import java.util.Scanner;
import java.util.regex.Pattern;

public class DevUtil {
    public static void main(String[] args) {
        String matcher = new Scanner(System.in).nextLine();
        Pattern p = matcher.isEmpty() ? null : Pattern.compile(matcher, Pattern.CASE_INSENSITIVE);
        ReleaseManifest.INSTANCE.getFileNames().stream().filter(s -> p == null || p.matcher(s).find()).forEach(System.out::println);
    }
}
