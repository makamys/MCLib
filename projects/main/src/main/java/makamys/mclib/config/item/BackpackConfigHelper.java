package makamys.mclib.config.item;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Helper for config of backpacks or similar portable storage items */
public class BackpackConfigHelper {
    
    private static final String TAPED_STORAGE_DRAWERS = "mclib:taped_storage_drawers";

    public static final String CONFIG_DESCRIPTION_SUFFIX = "\n" + TAPED_STORAGE_DRAWERS + " is a special item name that corresponds to taped Storage Drawers drawers\n";
    
    public static String[] NON_NESTABLE_BACKPACK_BLACKLIST = {TAPED_STORAGE_DRAWERS, "JABBA:moverDiamond", "JABBA:mover", "ExtraUtilities:bedrockiumIngot", "ExtraUtilities:block_bedrockium"};
    
    private boolean enableDrawers;
    private Set<Item> blacklistedItems;
    
    public BackpackConfigHelper(List<String> blacklist) {
        if(blacklist.contains(TAPED_STORAGE_DRAWERS)) {
            blacklist.remove(TAPED_STORAGE_DRAWERS);
            enableDrawers = true;
        }
        
        blacklistedItems = ItemConfigHelper.resolveItemList(blacklist).stream().collect(Collectors.toSet());
    }
    
    public boolean isAllowed(ItemStack is) {
        return is == null || !blacklistedItems.contains(is.getItem()); // TODO storage drawers
    }
    
}
