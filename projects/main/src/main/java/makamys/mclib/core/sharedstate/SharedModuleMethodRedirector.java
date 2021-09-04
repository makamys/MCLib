package makamys.mclib.core.sharedstate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import makamys.mclib.core.MCLib;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/** Redirects method invocations of a field's object to the equivalent field inside the newest version of the library present.
 * <br>
 * (Equivalence here means the fields have the same name, and the enclosing class has the same canonical class name relative to mclib package.)
 */
public class SharedModuleMethodRedirector implements MethodInterceptor {

	private Field field;
	private Object original;
	
	public SharedModuleMethodRedirector(Field field) {
		this.field = field;
	}

	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
		if(SharedLibHelper.isNewestLib(MCLib.instance)) {
			if(original == null) {
				try {
					original = field.getType().getConstructor().newInstance();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			return proxy.invoke(original, args);
		} else {
			Class<?> otherModulesClass = SharedLibHelper.findNewestLibClass(field.getDeclaringClass());
			Field otherField = otherModulesClass.getField(field.getName());
			Object other = otherField.get(null);
			Method otherMethod = otherField.getType().getMethod(method.getName(), method.getParameterTypes());
			return otherMethod.invoke(other, args);
		}
	}

}
