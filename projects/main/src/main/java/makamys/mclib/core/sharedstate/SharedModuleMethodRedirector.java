package makamys.mclib.core.sharedstate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import makamys.mclib.core.MCLib;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

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
