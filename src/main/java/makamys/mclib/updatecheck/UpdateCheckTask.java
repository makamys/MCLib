package makamys.mclib.updatecheck;

import static makamys.mclib.updatecheck.UpdateCheckLib.LOGGER;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
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
import makamys.mclib.updatecheck.UpdateCheckTask;

class UpdateCheckTask implements Supplier<UpdateCheckTask.Result> {
        
    String name;
    ComparableVersion currentVersion;
    UpdateCategory category;
    String updateJSONUrl;
    List<Hyperlink> homepages;
    
    public UpdateCheckTask(String name, String currentVersion, UpdateCategory category, String updateJSONUrl) {
        this.name = name;
        this.currentVersion = new ComparableVersion(currentVersion);
        this.category = category;
        this.updateJSONUrl = updateJSONUrl;
        this.homepages = new ArrayList<>();
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
        
        String jsonString;
        
        if(MockHelper.isTestMode() && MockHelper.isMockUrl(updateJSONUrl)) {
            jsonString = MockHelper.downloadMockText(updateJSONUrl);
        } else {
            URL url = new URL(updateJSONUrl);
            InputStream contents = url.openStream();

            jsonString = IOUtils.toString(contents, "UTF-8");
        }

        JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
        
        JsonElement homepageElem = json.get("homepage");
        JsonElement homepagesElem = json.get("homepages");
        if(homepageElem instanceof JsonPrimitive && homepagesElem == null) {
            homepages.add(new Hyperlink(homepageElem.getAsString()));
        } else if(homepagesElem instanceof JsonObject && homepageElem == null) {
            JsonObject homepagesObj = (JsonObject)homepagesElem;
            
            for(Entry<String, JsonElement> e : homepagesObj.entrySet()) {
                JsonElement value = e.getValue();
                if(value instanceof JsonPrimitive) {
                    homepages.add(new Hyperlink(value.getAsString(), e.getKey()));
                } else {
                    LOGGER.log(getErrorLevel(), "Invalid value for homepage " + e.getKey() + " in " + updateJSONUrl);
                }
            }
        } else {
            LOGGER.log(getErrorLevel(), "Failed to locate homepage(s) in " + updateJSONUrl);
        }
        
        String channel = ConfigUCL.promoChannel;
        
        ComparableVersion categoryVersion = new ComparableVersion(category.version);
        
        JsonElement promos = json.get("promos");
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
    
    public static class Hyperlink {
        final String url;
        final String display;
        
        public Hyperlink(String url, String display) {
            this.url = url;
            this.display = display;
        }
        
        public Hyperlink(String url) {
            this(url, url);
        }
    }
}
