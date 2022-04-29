package makamys.mclib.config.item;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.item.Item;

public class ItemConfigHelper {
    
    public static List<Item> resolveItemList(List<String> list){
        List<Item> items = new ArrayList<>();
        for(String itemStr : list) {
            items.addAll(resolveItem(itemStr));
        }
        
        return items;
    }
    
    public static List<Item> resolveItem(String itemStr){
        itemStr = ensureNamespaced(itemStr);
        List<Item> results = new ArrayList<>();
        Pattern pattern = Pattern.compile(itemStr.replace(".", "\\.").replace("*", ".*"));
        for(Object itemNameObj : Item.itemRegistry.getKeys()) {
            if(pattern.matcher((String)itemNameObj).matches()) {
                results.add((Item)Item.itemRegistry.getObject(itemNameObj));
            }
        }
        return results;
    }
    
    public static String ensureNamespaced(String name) {
        return name.indexOf(':') == -1 ? "minecraft:" + name : name;
    }
    
}
