package makamys.mclib.core.sharedstate;

import java.util.function.Supplier;

import net.minecraft.launchwrapper.Launch;

/** Helper for sharing state across multiple instances of the same shaded library. */
public class SharedReference<T> {
    
    @SuppressWarnings("unchecked")
    private static <T> T get(String namespace, String name, Class<?> clazz, Supplier<T> constructor) {
        String id = "mclib.sharedstate." + namespace + "." + name;
        Object ref = Launch.blackboard.get(id);
        if(ref == null) {
            if(constructor != null) {
                ref = constructor.get();
            } else {
                try {
                    ref = clazz.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        Launch.blackboard.put(id, ref);
        return (T)ref;
    }
    
    public static <T> T get(String namespace, String name, Class<?> clazz) {
        return get(namespace, name, clazz, null);
    }
    
    public static <T> T get(String namespace, String name, Supplier<T> constructor) {
        return get(namespace, name, null, constructor);
    }
    
}
