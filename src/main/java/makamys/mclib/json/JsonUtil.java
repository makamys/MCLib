package makamys.mclib.json;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonUtil {

    public static JsonObject getOrCreateObject(JsonObject obj, String memberName) {
        return (JsonObject)JsonUtil.getOrCreateElement(obj, memberName, () -> new JsonObject());
    }

    public static JsonArray getOrCreateArray(JsonObject obj, String memberName) {
        return (JsonArray)JsonUtil.getOrCreateElement(obj, memberName, () -> new JsonArray());
    }

    public static JsonElement getOrCreateElement(JsonObject obj, String memberName, Supplier<JsonElement> memberConstructor) {
        if(obj.has(memberName)) {
            return obj.get(memberName);
        } else {
            JsonElement elem = memberConstructor.get();
            obj.add(memberName, elem);
            return elem;
        }
    }

}
