package makamys.mclib.updatecheck;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL11;

import makamys.mclib.updatecheck.UpdateCheckLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;

public class Util {
	
	public static void openURLInBrowser(String url) {
		try {
			Class oclass = Class.forName("java.awt.Desktop");
			Object object = oclass.getMethod("getDesktop", new Class[0]).invoke((Object) null, new Object[0]);
			oclass.getMethod("browse", new Class[] { URI.class }).invoke(object, new Object[] { new URI(url) });
		} catch (Throwable throwable) {
			UpdateCheckLib.LOGGER.error("Couldn\'t open link", throwable);
		}
	}
	
	/** A simple alternative to Minecraft's tooltip drawing function in case it can't be used. */
	public static void drawSimpleTooltip(int mouseX, int mouseY, int z, List<String> lines) {
		GuiScreen current = Minecraft.getMinecraft().currentScreen;
		
		FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
		
		int margin = 2;
        
        int width = lines.stream().mapToInt(s -> fr.getStringWidth(s)).max().getAsInt();
        int height = lines.size() * 10;
        
		int startX = mouseX - width / 2;
		int startY = mouseY - height - 5;
        
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(0, 0, 0, 0.7f);
        int p_73733_3_ = startX - margin;
        int p_73733_1_ = startX + width + margin * 2;
        int p_73733_4_ = startY - margin;
        int p_73733_2_ = startY + height + margin * 2;
        tessellator.addVertex((double)p_73733_3_, (double)p_73733_2_, (double)z);
        tessellator.addVertex((double)p_73733_1_, (double)p_73733_2_, (double)z);
        tessellator.addVertex((double)p_73733_1_, (double)p_73733_4_, (double)z);
        tessellator.addVertex((double)p_73733_3_, (double)p_73733_4_, (double)z);
        tessellator.draw();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        
        int y = 0;
        for(String s : lines) {
        	current.drawString(Minecraft.getMinecraft().fontRenderer, s, startX + margin, startY + margin + y, 0xFFFFFF);
        	y += 10;
        }
	}
	
}
