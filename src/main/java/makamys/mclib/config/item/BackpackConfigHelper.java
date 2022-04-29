package makamys.mclib.config.item;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import makamys.mclib.core.MCLib;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Helper for config of backpacks or similar portable storage items */
public class BackpackConfigHelper {
    
    public static final String CONFIG_DESCRIPTION_SUFFIX = "\nThe '*' character can be used as a wildcard to match one or more characters.\n";
    
    public static String[] NON_NESTABLE_BACKPACK_BLACKLIST = {"JABBA:mover*", "ExtraUtilities:*bedrockium*"};
    
    private Set<Item> blacklistedItems;
    
    public BackpackConfigHelper(List<String> blacklist) {
        blacklistedItems = ItemConfigHelper.resolveItemList(blacklist).stream().collect(Collectors.toSet());
        
        MCLib.LOGGER.debug("Resolved backpack blacklist to " + blacklistedItems);
    }
    
    public boolean isAllowed(ItemStack is) {
        return is == null || !blacklistedItems.contains(is.getItem());
    }
    
}
