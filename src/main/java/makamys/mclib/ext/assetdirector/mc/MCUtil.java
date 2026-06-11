package makamys.mclib.ext.assetdirector.mc;

import java.io.File;

import cpw.mods.fml.common.ProgressManager;
import cpw.mods.fml.common.versioning.ComparableVersion;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.launchwrapper.Launch;

/** Provides abstractions to avoid direct references to Minecraft in AssetDirector's code. */
public class MCUtil {
    
    public static File getMCAssetsDir() {
        return (File)ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "fileAssets", "field_110446_Y");
    }
    
    public static class ProgressBar {
        private final cpw.mods.fml.common.ProgressManager.ProgressBar internal;
        
        private ProgressBar(cpw.mods.fml.common.ProgressManager.ProgressBar bar) {
            this.internal = bar;
        }
        
        public static ProgressBar push(String title, int steps) {
            return new ProgressBar(ProgressManager.push(title, steps));
        }

        public void step(String message) {
            internal.step(message);
        }
        
        public void pop() {
            ProgressManager.pop(internal);
        }
    }
    
    public static class Version {
        private final ComparableVersion internal;
        
        public Version(String version) {
            internal = new ComparableVersion(version);
        }
        
        public int compareTo(Version other) {
            return internal.compareTo(other.internal);
        }
    }

    public static File getInstanceDir() {
        return Launch.minecraftHome;
    }
    
}
