package makamys.mclib.ccl.inventory;

import codechicken.lib.inventory.InventoryRange;
import static codechicken.lib.inventory.InventoryUtils.*;
import net.minecraft.item.ItemStack;

public class InventoryUtils2 {
    
    /**
     * Same as InventoryUtils.insertItem, but lets you run just one pass at a time.
     * @param simulate If set to true, no items will actually be inserted
     * @return The number of items unable to be inserted
     */
    public static int insertItem(InventoryRange inv, ItemStack stack, boolean simulate, int pass) {
        stack = stack.copy();
        for (int slot : inv.slots) {
            ItemStack base = inv.inv.getStackInSlot(slot);
            if((pass == 0) == (base == null))
                continue;
            int fit = fitStackInSlot(inv, slot, stack);
            if (fit == 0)
                continue;

            if (base != null) {
                stack.stackSize -= fit;
                if (!simulate) {
                    base.stackSize += fit;
                    inv.inv.setInventorySlotContents(slot, base);
                }
            } else {
                if (!simulate)
                    inv.inv.setInventorySlotContents(slot, copyStack(stack, fit));
                stack.stackSize -= fit;
            }
            if (stack.stackSize == 0)
                return 0;
        }
        return stack.stackSize;
    }
    
}
