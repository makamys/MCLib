package makamys.mclib.config.item;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import makamys.mclib.core.MCLib;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Helper for config of backpacks or similar portable storage items */
public class BackpackConfigHelper {
    
    private static final String TAPED_STORAGE_DRAWERS = "mclib:taped_storage_drawers";

    public static final String CONFIG_DESCRIPTION_SUFFIX = "\n" + TAPED_STORAGE_DRAWERS + " is a special item name that corresponds to taped Storage Drawers drawers\nThe '*' character can be used as a wildcard to match one or more characters.";
    
    public static String[] NON_NESTABLE_BACKPACK_BLACKLIST = {TAPED_STORAGE_DRAWERS, "JABBA:mover*", "ExtraUtilities:*bedrockium*"};
    
    private boolean blacklistDrawers;
    private Set<Item> blacklistedItems;
    
    public BackpackConfigHelper(List<String> blacklist) {
        if(blacklist.contains(TAPED_STORAGE_DRAWERS)) {
            blacklist.remove(TAPED_STORAGE_DRAWERS);
            blacklistDrawers = true;
        }
        
        blacklistedItems = ItemConfigHelper.resolveItemList(blacklist).stream().collect(Collectors.toSet());
        
        MCLib.LOGGER.debug("Resolved backpack blacklist to " + blacklistedItems);
    }
    
    public boolean isAllowed(ItemStack is) {
        return is == null || !blacklistedItems.contains(is.getItem()); // TODO storage drawers
    }
    
}
