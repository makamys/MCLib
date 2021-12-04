package makamys.mclib.ext.assetdirector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import makamys.mclib.json.JsonUtil;

public class ADJsonHelper {

    public static void addObject(JsonObject json, String version, String path) {
        JsonObject assets = JsonUtil.getOrCreateObject(json, "assets");
        JsonObject verObj = JsonUtil.getOrCreateObject(assets, version);
        JsonArray objects = JsonUtil.getOrCreateArray(verObj, "objects");
        objects.add(new JsonPrimitive(path));
    }
    
    public static void addSoundEvent(JsonObject json, String version, String name) {
        addSoundEvent(json, version, name, null);
    }
    
    public static void addSoundEvent(JsonObject json, String version, String name, String category) {
        JsonObject assets = JsonUtil.getOrCreateObject(json, "assets");
        JsonObject verObj = JsonUtil.getOrCreateObject(assets, version);
        JsonArray soundEvents = JsonUtil.getOrCreateArray(verObj, "soundEvents");
        JsonElement sound = null;
        
        if(category != null) {
            JsonObject soundObj = new JsonObject();
            soundObj.add("name", new JsonPrimitive(name));
            soundObj.add("category", new JsonPrimitive(category));
            sound = soundObj;
        } else {
            sound = new JsonPrimitive(name);
        }
        soundEvents.add(sound);
    }
    
    public static void addJar(JsonObject json, String version) {
        JsonObject assets = JsonUtil.getOrCreateObject(json, "assets");
        JsonObject verObj = JsonUtil.getOrCreateObject(assets, version);
        verObj.addProperty("jar", true);
    }
    
}
