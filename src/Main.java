import nallar.leagueskin.RafManager;
import nallar.leagueskin.riotfiles.ReleaseManifest;
import nallar.leagueskin.util.RiotUtil;

public class Main {
    public static void main(String[] args) {
        //noinspection ResultOfMethodCallIgnored
        ReleaseManifest.INSTANCE.hashCode(); //Force instance to be instantiated by loading class.
        new RafManager(RiotUtil.filearchivesDirectory());
    }
}
