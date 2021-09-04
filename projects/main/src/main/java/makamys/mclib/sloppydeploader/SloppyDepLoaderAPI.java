package makamys.mclib.sloppydeploader;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import makamys.mclib.core.sharedstate.SharedReference;
import static makamys.mclib.sloppydeploader.SloppyDepLoader.NS;

public class SloppyDepLoaderAPI {
	
	static Map<String, String> modDeps = SharedReference.get(NS, "modDeps", HashMap.class);
	
	/** Enqueues dependency downloading for a mod. Can be called anytime before pre-init. */
    public static void addDependenciesForMod(String modid, SloppyDependency... sloppyDependencies) {
        modDeps.put(modid, String.join(";", Arrays.stream(sloppyDependencies).map(d -> d.serializeToString()).toArray(String[]::new)));
    }
	
}
