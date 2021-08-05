package makamys.mclib.sloppydeploader;

import java.util.List;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

/* Adapted from GuiConfirmOpenLink */

@SideOnly(Side.CLIENT)
public class GuiRestartNotification extends GuiYesNo
{
    private List<String> deps;

    private GuiScreen parent;
    
    public GuiRestartNotification(GuiScreen parent, List<String> deps)
    {
        super(null, "", "", -1);
        this.parent = parent;
        this.confirmButtonText = I18n.format("Quit game");
        this.cancelButtonText = I18n.format("Keep playing");
        this.deps = deps;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui()
    {
        this.buttonList.add(new GuiOptionButton(0, this.width / 2 - 155, this.height - 32, this.confirmButtonText));
        this.buttonList.add(new GuiOptionButton(1, this.width / 2 - 155 + 160, this.height - 32, this.cancelButtonText));
    }

    protected void actionPerformed(GuiButton p_146284_1_)
    {
        switch(p_146284_1_.id) {
        case 0:
            FMLCommonHandler.instance().exitJava(0, false);
            break;
        case 1:
            Minecraft.getMinecraft().displayGuiScreen(parent);
            break;
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int p_73863_1_, int p_73863_2_, float p_73863_3_)
    {
        super.drawScreen(p_73863_1_, p_73863_2_, p_73863_3_);
        
        int y = 30;
        
        this.drawCenteredString(this.fontRendererObj, "The following optional dependencies have been downloaded:", this.width / 2, y, 0xFFFFFF);
        y += 18;
        
        for(String dep : deps) {
            this.drawCenteredString(this.fontRendererObj, EnumChatFormatting.YELLOW + dep, this.width / 2, y, 0xFFFFFF);
            y += 12;
        }
        y += 6;
        
        this.drawCenteredString(this.fontRendererObj, "A game restart is needed to activate them.", this.width / 2, y, 0xFFFFFF);
        y += 12;
        this.drawCenteredString(this.fontRendererObj, "Do you want to restart the game now?", this.width / 2, y, 0xFFFFFF);
    }
}

