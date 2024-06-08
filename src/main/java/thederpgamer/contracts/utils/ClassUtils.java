package thederpgamer.contracts.utils;

import thederpgamer.contracts.Contracts;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class ClassUtils {

	public static Object getField(Object object, String fieldName) {
		try {
			Field field = object.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(object);
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while getting private field " + fieldName + " from object " + object, exception);
			return null;
		}
	}

	public static void setField(Object object, String fieldName, Object value) {
		try {
			Field field = object.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(object, value);
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while setting private field " + fieldName + " from object " + object, exception);
		}
	}

	public static Object invokeMethod(Object object, String methodName, Object... args) {
		try {
			Class<?>[] argTypes = new Class<?>[args.length];
			for(int i = 0; i < args.length; i++) argTypes[i] = args[i].getClass();
			Method method = object.getClass().getDeclaredMethod(methodName, argTypes);
			method.setAccessible(true);
			return method.invoke(object, args);
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while invoking method " + methodName + " from object " + object, exception);
			return null;
		}
	}
}
