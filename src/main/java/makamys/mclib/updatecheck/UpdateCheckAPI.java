package makamys.mclib.updatecheck;

import static makamys.mclib.updatecheck.UpdateCheckLib.*;

import java.util.concurrent.CompletableFuture;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import makamys.mclib.updatecheck.UpdateCheckLib.UpdateCategory;

public class UpdateCheckAPI {
    
    public static String MODS_CATEGORY_ID = "mods";
    public static String RESOURCE_PACKS_CATEGORY_ID = "resource_packs";
    
    /** Convenience method for submitting a Forge mod update check task. The version will be retrieved from the mod's @Mod annotation.
     * 
     * @see #submitTask */
    public void submitModTask(String modid, String updateJSONUrl) {
        if(!isEnabled()) return;
        submitModTask(modid, null, updateJSONUrl);
    }
    
    /** Convenience method for submitting a Forge mod update check task. Use this if you want to use a different version from the one in the mod's @Mod annotation.
     * 
     * @see #submitTask */
    public void submitModTask(String modid, String currentVersion, String updateJSONUrl) {
        if(!isEnabled()) return;
        ModContainer mc = Loader.instance().getIndexedModList().get(modid);
        if(mc == null) {
            LOGGER.warn("Tried to register update check for non-existent modid: " + modid);
            return;
        }
        submitTask(mc.getName(), currentVersion != null ? currentVersion : mc.getVersion(), MODS_CATEGORY_ID, updateJSONUrl);
    }
    
    /** Submit an update check task for a component (usually a mod, but can be something else like a resource pack). This should be called before the Forge post-init stage.
     * 
     * @param name The display name that will be used in the update check results UI.
     * @param currentVersion The currently installed version of the component.
     * @param categoryID The ID of the category this component belongs to. Pre-defined ones are {@link #MODS_CATEGORY_ID} and {@link #RESOURCE_PACKS_CATEGORY_ID}, but new ones can be defined using {@link #registerCategory}.
     * @param updateJSONUrl The URL of the mod's update JSON. Has to follow Forge's format (see https://mcforge.readthedocs.io/en/latest/gettingstarted/autoupdate/). However, only the "promos" and "homepage" fields are required. */
    public void submitTask(String name, String currentVersion, String categoryID, String updateJSONUrl) {
        if(!isEnabled()) return;
        if(!categories.containsKey(categoryID)) {
            LOGGER.warn("Tried to register a non-existent category for mod " + name + ": " + categoryID);
        }
        futures.add(CompletableFuture.supplyAsync(new UpdateCheckTask(name, currentVersion, categories.get(categoryID), updateJSONUrl), executor));
    }
    
    /** Register a category for your components. Use this if your component is something other than a mod or a resource pack.
     * @param id The unique identifier used to refer to this category. If a category is already defined with this ID, this method will not do anything.
     * @param version The version of the component's dependency. For a mod or resource pack, this will be the Minecraft version.
     * @param backwardsCompatible Can the component run if our version of its dependency is higher than what it requires? <br>
     *        For example, this is false for Forge mods (we can't run Forge mods made for a lower version of Minecraft!), but true for MAtmos soundpacks (packs made for lower versions of MAtmos will still work).
     * @param displayName The display name of the category that will be shown in the UI. Should be capitalized and singular.
     */
    public void registerCategory(String id, String version, String displayName, boolean backwardsCompatible) {
        if(!isEnabled()) return;
        if(!categories.containsKey(id)) {
            categories.put(id, new UpdateCategory(id, version, displayName, backwardsCompatible));
        }
    }
    
}
