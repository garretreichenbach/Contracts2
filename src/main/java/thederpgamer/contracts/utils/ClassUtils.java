package thederpgamer.contracts.utils;

import thederpgamer.contracts.Contracts;

import java.lang.reflect.Field;

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
}
