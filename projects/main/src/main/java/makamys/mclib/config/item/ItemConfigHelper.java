package makamys.mclib.config.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;

public class ItemConfigHelper {
    
    public static List<Item> resolveItemList(List<String> list){
        List<Item> items = new ArrayList<>();
        for(String itemStr : list) {
            Object itemObj = Item.itemRegistry.getObject(itemStr);
            if(itemObj != null) {
                items.add((Item)itemObj);
            }
        }
        
        return items;
    }
    
}
