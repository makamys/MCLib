package makamys.mclib.updatecheck;

import static makamys.mclib.updatecheck.UpdateCheckLib.LOGGER;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import cpw.mods.fml.common.versioning.ComparableVersion;
import makamys.mclib.updatecheck.UpdateCheckLib.UpdateCategory;
import makamys.mclib.updatecheck.ConfigUCL;
import makamys.mclib.updatecheck.UpdateCheckTask;

class UpdateCheckTask implements Supplier<UpdateCheckTask.Result> {
    	
	String name;
	ComparableVersion currentVersion;
	UpdateCategory category;
	String updateJSONUrl;
	String homepage = "";
	
	public static final boolean TEST_MODE = Boolean.parseBoolean(System.getProperty("updateCheckLib.test", "false"));
	private static final String MOCK_PREFIX = "mock://";
	
	public UpdateCheckTask(String name, String currentVersion, UpdateCategory category, String updateJSONUrl) {
		this.name = name;
		this.currentVersion = new ComparableVersion(currentVersion);
		this.category = category;
		this.updateJSONUrl = updateJSONUrl;
	}
	
	@Override
	public UpdateCheckTask.Result get() {
        LOGGER.debug("Checking " + name + " for updates");

        ComparableVersion current = currentVersion;
        ComparableVersion solved = null;
        try {
        	solved = solveVersion();
        } catch(Exception e) {
        	LOGGER.log(getErrorLevel(), "Failed to retrieve update JSON for " + name + ": " + e.getMessage());
        }
        if (solved == null)
            return new UpdateCheckTask.Result(this);
        
        LOGGER.debug("Update version found for " + name + ": " + solved + " (running " + current + ")");

        return new UpdateCheckTask.Result(this, solved);
    }
	
	private Level getErrorLevel() {
		return !ConfigUCL.hideErrored ? Level.ERROR : Level.DEBUG;
	}
	
	private ComparableVersion solveVersion() throws Exception {
		if(category == null) return null;
		if(TEST_MODE && updateJSONUrl.startsWith(MOCK_PREFIX)) return mockSolveVersion();
		
        URL url = new URL(updateJSONUrl);
        InputStream contents = url.openStream();

        String jasonString = IOUtils.toString(contents, "UTF-8");

        JsonObject jason = new JsonParser().parse(jasonString).getAsJsonObject();
        
        JsonElement homepageElem = jason.get("homepage");
        if(homepageElem instanceof JsonPrimitive) {
        	homepage = homepageElem.getAsString();
        } else {
        	LOGGER.log(getErrorLevel(), "Failed to locate 'homepage' element in " + updateJSONUrl);
        }
        
        String channel = ConfigUCL.promoChannel;
        
        ComparableVersion categoryVersion = new ComparableVersion(category.version);
        
        JsonElement promos = jason.get("promos");
        if(promos instanceof JsonObject) {
        	try {
	        	ComparableVersion newestLowerCategoryVersion = Collections.max(((JsonObject)promos).entrySet().stream().map(e -> new ComparableVersion(e.getKey().split("-")[0])).filter(v -> v.compareTo(categoryVersion) <= 0).collect(Collectors.toList()));
	        	if(newestLowerCategoryVersion.compareTo(categoryVersion) == 0 || category.backwardsCompatible) {
	        		String promoKey = newestLowerCategoryVersion + "-" + channel;
	        		JsonElement promoVersion = ((JsonObject)promos).get(promoKey);
		        	
		        	if(promoVersion != null) {
		        		return new ComparableVersion(promoVersion.getAsString());
		        	} else {
		        		LOGGER.log(getErrorLevel(), "No promo named " + promoKey + " found in " + updateJSONUrl);
		        	}
	        	} else {
	        		LOGGER.log(getErrorLevel(), "No promo found for non-backwards compatible category of version " + categoryVersion + " in " + updateJSONUrl);
	        	}
        	} catch(NoSuchElementException e) {
        		LOGGER.log(getErrorLevel(), "No promo found for category version lower than " + category.version + " in " + updateJSONUrl);
        	}
        } else {
        	LOGGER.log(getErrorLevel(), "Failed to locate promos in " + updateJSONUrl);
        }
        return null;
    }
	
	private ComparableVersion mockSolveVersion() {
		return new ComparableVersion(updateJSONUrl.substring(MOCK_PREFIX.length()));
	}
	
	public static class Result {
		UpdateCheckTask task;
		public ComparableVersion newVersion;
		
		public Result(UpdateCheckTask task, ComparableVersion newVersion) {
			this.task = task;
			this.newVersion = newVersion;
		}
		
		public Result(UpdateCheckTask task) {
			this(task, null);
		}
		
		public boolean foundUpdate() {
			return newVersion != null && newVersion.compareTo(task.currentVersion) > 0;
		}
		
		public boolean isInteresting() {
			return (!ConfigUCL.hideErrored && newVersion == null) || foundUpdate();
		}
	}
}
