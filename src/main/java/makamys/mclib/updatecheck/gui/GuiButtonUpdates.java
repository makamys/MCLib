package makamys.mclib.updatecheck.gui;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.input.Keyboard;

import makamys.mclib.updatecheck.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import static net.minecraft.util.EnumChatFormatting.*;

public class GuiButtonUpdates extends GuiButtonGeneric {

    int updateCount;
    String url;
    
    public GuiButtonUpdates(int id, int posX, int posY, int width, int height, int updateCount, String url) {
        super(id, posX, posY, width, height, "");
        setUpdateCount(updateCount);
        this.url = url;
    }
    
    public void setUpdateCount(int newCount) {
        updateCount = newCount;
        if(updateCount >= 0) {
            displayString = GREEN + "+" + updateCount;
        }
        visible = updateCount != 0;
        enabled = updateCount > 0;
    }
    
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if(updateCount == -1) {
            displayString = new String[] {
                    GRAY + "." + DARK_GRAY +"..",
                    DARK_GRAY + "." + GRAY + "." + DARK_GRAY + ".",
                    DARK_GRAY + ".." + GRAY + "."
                }[(int)((System.nanoTime() / 1000000000) % 3)];
        }
        
        super.drawButton(mc, mouseX, mouseY);
    }
    
    @Override
    public List<String> getTooltipStrings() {
        if(updateCount >= 0) {
            String plural = updateCount != 1 ? "s" : "";
            return Arrays.asList(
                    "" + GREEN + updateCount + RESET + " update" + plural + " available.",
                    "Click to open list in browser.",
                    GRAY + "(Shift click to copy URL.)");
        } else {
            return Arrays.asList(
                    GRAY + "Checking for updates...");
        }
    }
    
    @Override
    public void onClicked() {
        super.onClicked();
        if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            GuiScreen.setClipboardString(url);
        } else {
            Util.openURLInBrowser(url);
        }
    }

}
