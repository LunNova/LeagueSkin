import nallar.leagueskin.RafManager;
import nallar.leagueskin.SkinPack;
import nallar.leagueskin.riotfiles.ReleaseManifest;
import nallar.leagueskin.util.PathUtil;

public class Main {
    public static void main(String[] args) {
        try {
            //noinspection ResultOfMethodCallIgnored
            ReleaseManifest.INSTANCE.hashCode(); //Force instance to be instantiated by loading class.
            new RafManager(PathUtil.filearchivesDirectory()).installSkinPack(new SkinPack(PathUtil.dataDir().resolve("Skins")));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
