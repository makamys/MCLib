package makamys.mclib.core.sharedstate;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableObject;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.versioning.ComparableVersion;
import makamys.mclib.core.MCLib;
import net.sf.cglib.proxy.Enhancer;

public class SharedLibHelper {

	private static List<String> existingLibPackages = SharedReference.get("mclib", "existingLibPackages", ArrayList.class);
	private static MutableObject<String> newestLibPackage = SharedReference.get("mclib", "newestLibPackage", MutableObject.class);
	
	private static String thisLibPackage;
	
	public static void register(MCLib mcLib) {
		thisLibPackage = getClassNameParent(mcLib.getClass().getCanonicalName(), 2);
		MCLib.LOGGER.trace("Registering package " + thisLibPackage);
		existingLibPackages.add(thisLibPackage);
	}
	
	private static String getClassNameParent(String name, int count) {
		String[] canonicalNameParts = name.split("\\.");
		canonicalNameParts = Arrays.copyOf(canonicalNameParts, canonicalNameParts.length - count);
		return String.join(".", canonicalNameParts);
	}

	public static void shareifyClass(Class<?> clazz) {
		for(Field f : clazz.getFields()) {
			if(f.isAnnotationPresent(SharedField.class)) {
				int mod = f.getModifiers();
				if(Modifier.isStatic(mod) && Modifier.isPublic(mod)) {
					try {
						Enhancer e = new Enhancer();
						e.setSuperclass(f.getType());
						e.setCallback(new SharedModuleMethodRedirector(f));
						Object obj = e.create();
						f.set(null, obj);
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else {
					throw new IllegalArgumentException("Field " + f.getName() + " in class " + clazz.getCanonicalName() + " is not public and static, this is not supported.");
				}
			}
		}
	}
	
	public static boolean isNewestLib(MCLib mcLib) {
		return getNewestLibPackage().equals(thisLibPackage);
	}
	
	private static String getNewestLibPackage() {
		if(!Loader.instance().hasReachedState(LoaderState.PREINITIALIZATION)) {
			throw new IllegalStateException("Shared module was called before mod construction phase ended, this is not allowed.");
		}
		
		if(newestLibPackage.getValue() == null) {	
			String newestPkg = null;
			ComparableVersion newestVersion = null;
			for(String pkg : existingLibPackages) {
				try {
					Class<?> otherMcLibClass = Class.forName(pkg + "." + toRelativeClassName(MCLib.class.getCanonicalName()));
					ComparableVersion otherVersion = new ComparableVersion((String)otherMcLibClass.getField("VERSION").get(null));
					if(newestVersion == null || newestVersion.compareTo(otherVersion) < 0) {
						newestPkg = pkg;
						newestVersion = otherVersion;
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			MCLib.LOGGER.debug("Latest version of MCLib present: " + newestVersion + " @ " + newestPkg);
			
			newestLibPackage.setValue(newestPkg);
		}
		
		return newestLibPackage.getValue();
	}
	
	public static String toRelativeClassName(String className) {
		if(className.startsWith(thisLibPackage)) {
			return className.substring(thisLibPackage.length() + 1);
		} else {
			throw new IllegalArgumentException(className + " does not start with " + thisLibPackage);
		}
	}

	public static Class<?> findNewestLibClass(Class<?> clazz) {
		try {
			return Class.forName(getNewestLibPackage() + "." + toRelativeClassName(clazz.getCanonicalName()));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}


}
