package makamys.mclib.core.sharedstate;

import net.minecraft.launchwrapper.Launch;

/** Helper for sharing state across multiple instances of the same shaded library. */
public class SharedReference<T> {
    
    @SuppressWarnings("unchecked")
    public static <T> T get(String namespace, String name, Class<?> clazz) {
        String id = "makamys.sharedstate." + namespace + "." + name;
        Object ref = Launch.blackboard.get(id);
        if(ref == null) {
            try {
                ref = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return (T)ref;
    }
    
}
