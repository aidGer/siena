/*
 * Copyright 2009 Alberto Gimeno <gimenete at gmail.com>
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package siena;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassInfo {
	
	private static Map<Class<?>, ClassInfo> infoClasses = new HashMap<Class<?>, ClassInfo>();
	
	public String tableName;

	public List<Field> keys = new ArrayList<Field>();
	public List<Field> insertFields = new ArrayList<Field>();
	public List<Field> updateFields = new ArrayList<Field>();
	public List<Field> generatedKeys = new ArrayList<Field>();
	public List<Field> allFields = new ArrayList<Field>();

	private ClassInfo(Class<?> clazz) {
		tableName = getTableName(clazz);

		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			
			Class<?> type = field.getType();
			if(type == Class.class || type == Query.class ||
					(field.getModifiers() & Modifier.TRANSIENT) == Modifier.TRANSIENT ||
					(field.getModifiers() & Modifier.STATIC) == Modifier.STATIC)
				continue;
			
			Id id = field.getAnnotation(Id.class);
			if(id != null) {
				if(id.value() == Generator.AUTO_INCREMENT) {
					generatedKeys.add(field);
				} else {
					insertFields.add(field);
				}
				keys.add(field);
			} else {
				updateFields.add(field);
				insertFields.add(field);
			}
			allFields.add(field);
		}
	}

	private String getTableName(Class<?> clazz) {
		Table t = clazz.getAnnotation(Table.class);
		if(t == null) return clazz.getSimpleName();
		return t.value();
	}

	public static String[] getColumnNames(Field field) {
		Column c = field.getAnnotation(Column.class);
		if(c != null && c.value().length > 0) return c.value();
		
		// default mapping: field names
		if(isModel(field.getType())) {
			ClassInfo ci = getClassInfo(field.getType());
			List<String> keys = new ArrayList<String>();
			for (Field key : ci.keys) {
				keys.addAll(Arrays.asList(getColumnNames(key)));
			}
			return keys.toArray(new String[keys.size()]);
		}
		return new String[]{ field.getName() };
	}

	public static boolean isModel(Class<?> type) {
		if(type.getSuperclass() == Model.class)
			return true;
		// TODO: this needs to be tested
		if(type.getName().startsWith("java.")) return false;
		if(type == Json.class) return false;
		return !ClassInfo.getClassInfo(type).keys.isEmpty();
	}

	public static boolean isId(Field field) {
		return field.getAnnotation(Id.class) != null;
	}
	
	/**
	 * Useful for those PersistenceManagers that only support one @Id
	 * @param clazz
	 * @return
	 */
	public static Field getIdField(Class<?> clazz) {
		List<Field> keys = ClassInfo.getClassInfo(clazz).keys;
		if(keys.isEmpty())
			throw new SienaException("No valid @Id defined in class "+clazz.getName());
		if(keys.size() > 1)
			throw new SienaException("Multiple @Id defined in class "+clazz.getName());
		return keys.get(0);
	}

	public static ClassInfo getClassInfo(Class<?> clazz) {
		ClassInfo ci = infoClasses.get(clazz);
		if(ci == null) {
			ci = new ClassInfo(clazz);
			infoClasses.put(clazz, ci);
		}
		return ci;
	}

}