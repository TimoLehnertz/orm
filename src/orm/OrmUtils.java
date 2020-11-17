package orm;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import annotations.ForeignKey;
import annotations.ManyToMany;
import annotations.NoOrm;
import annotations.OneToMany;
import annotations.OneToOne;
import annotations.PrimaryKey;
import annotations.Table;


public class OrmUtils {

	/**
	 * relations -> Map wich should be initiated at the start via Orm.initTables()
	 * after initialization this Map should contain every table as Key and
	 * correspondingly a List of all Tables wich it is dependent on
	 * 
	 * later this will be needed for defining the order of table creation
	 * 
	 * Tables are represented by Entity extending class types
	 */
	private static Map<Class<? extends Entity<?>>, List<Class<? extends Entity<?>>>> relations = new HashMap<>();
	
	/**
	 * Method wich should be invoked first before any interactions can be made with the orm
	 * required to define dependet Tables
	 * After it creates missing tables
	 * @param entities Tables to initialize
	 * @return true for success
	 */
	protected static boolean initTables(Class<? extends Entity<?>>[] entities) {
		Orm.logger.notice("Initializing tables..");
		initRelations(entities);
		return false;
	}
	
	/**
	 * gets invoked at start by initTables()
	 * searches for dependencies betwween Tables and saves them into relations
	 * @param entities Pool
	 */
	protected static void initRelations(Class<? extends Entity<?>>[] entities) {
		Orm.logger.notice("	Initializing table-dependencies");
		for (Class<? extends Entity<?>> type : entities) {
			if(!isOrmTypeValid(type, true)) {
				continue;
			} else {
				relations.put(type, new ArrayList<Class<? extends Entity<?>>>());
				for (Field field : type.getDeclaredFields()) {
					warnOrmField(field);
					if(!isOrmFieldvalid(field, false)) {
						continue;//Skip no table fields
					}
//					Check dependencies
					
					/**
					 * One to Many => field is dependent on type
					 */
					OneToMany otm = field.getAnnotation(OneToMany.class);
					if(otm != null) {
						Class<? extends Entity<?>> relationtable = otm.referenceTable();
//						System.out.println(relationtable);
						if(!relations.containsKey(relationtable)) {// No table registered yet
							relations.put(relationtable, new ArrayList<Class<? extends Entity<?>>>());//register table
							relations.get(relationtable).add(type);//add dependency to own table
						} else {
							relations.get(relationtable).add(type);//add dependency to own table
						}
					}
					/**
					 * One To One
					 * "type" has one ___
					 * => "type" is dependent on ___
					 */
					OneToOne oto = field.getAnnotation(OneToOne.class);
					if(oto != null) {
						Class<? extends Entity<?>> relationtable = oto.referenceTable();
						relations.get(type).add(relationtable);
					}
					
					/**
					 * Foreign Keys
					 * foreign Key is deoendet on its reference
					 */
					
					ForeignKey fk = field.getAnnotation(ForeignKey.class);// do nothing just trigger warning
					if(fk != null) {
						getForeignKeyReferenceField(field, fk);// I add dependency anyways even in case of failure so you dont get more errors by fixing the foreign key
						Class<? extends Entity<?>> relationtable = fk.referenceTable();
						relations.get(type).add(relationtable);
					}
				}
			}
		}
		Orm.logger.notice("		Dependencies: " + relations);
		Orm.logger.notice("	Done!");
	}
	
	private static Field getForeignKeyReferenceField(Field fkField, ForeignKey fk) {
		return getForeignKeyReferenceField(fkField, fk, false);
	}
	
	private static Field getForeignKeyReferenceField(Field fkField, ForeignKey fk, boolean superClass) {
		Field reference = null;
		try {
			if(superClass) {
				reference = fk.referenceTable().getSuperclass().getDeclaredField(fk.field());
			} else {
				reference = fk.referenceTable().getDeclaredField(fk.field());
			}
			if(SupportedTypes.isTypeValidForkey(reference.getType())) {
				if(fkField.getType() == reference.getType()) {
					if(reference.getAnnotation(PrimaryKey.class) == null) {
						Orm.logger.warn("The Foreign key at \"" + fk.referenceTable() + "\", \"" + fk.field() + "\" is pointing to a non Primary key");
					}
					return reference;
				} else {
					Orm.logger.warn("Foreign And referenced Primary keys have different types! ON: \"" + fk.referenceTable() + "\", \"" + fk.field() + "\"");
				}
			} else {
				Orm.logger.warn("Foreign Key has a wrong type! Only numeric foreign Keys are supported. ON: \"" + fk.referenceTable() + "\", \"" + fk.field() + "\"");
			}
		} catch (NoSuchFieldException | SecurityException e) {
			if(!superClass) {
				reference = getForeignKeyReferenceField(fkField, fk, true);
				if(reference == null) {
					Orm.logger.warn("Foreign Key is incorrectly formed given: \"" + fk.referenceTable() + "\", \"" + fk.field() + "\", message: " + e.getMessage());
				} else {
					if(reference.getAnnotation(PrimaryKey.class) == null) {
						Orm.logger.warn("The Foreign key at \"" + fk.referenceTable() + "\", \"" + fk.field() + "\" is pointing to a non Primary key");
					}
					return reference;
				}
			}
		}
		return null;
	}
	
	/**
	 * Checks wether a class type extends Entity but is not an entity itself
	 * @param type
	 * @return check
	 */
	private static boolean isTypeSubTypeFromEntity(Class<?> type) {
		return isTypeSubType(type, Entity.class);
	}
	
	/**
	 * checks if a given subtype is inherited from a given supertype but not the supertype itself
	 * @param subType
	 * @param superType
	 * @return
	 */
	private static boolean isTypeSubType(Class<?> subType, Class<?> superType) {
		return superType.isAssignableFrom(subType) && subType != superType;
	}
	
	/**
	 * checks if a given subtype is inherited from a given supertype
	 * @param subType
	 * @param superType
	 * @return
	 */
	private static boolean isTypeSubTypeOrSame(Class<?> subType, Class<?> superType) {
		return superType.isAssignableFrom(subType);
	}
	
	/**
	 * Checks if a field has the @NoOrm Annotation
	 * @param field to check
	 * @return has or has Not
	 */
	private static boolean isFieldNoOrm(Field field) {
		NoOrm noOrm = field.getAnnotation(NoOrm.class);
		return noOrm != null;
	}
	
	/**
	 * Checks if a Field is valid for use in orm
	 * only checks types and annotations
	 * doesnt look for potentially missing dependencies
	 * 
	 * @param type
	 * @return validation
	 */
	protected static boolean isOrmFieldvalid(Field field, boolean log) {
		if(isFieldNoOrm(field)) {
			return false;
		}
		if(isListClass(field.getType())){ //list
			return isOrmTypeValid(getGenericClassFromListField(field), log);
		} else if(isTypeSubTypeFromEntity(field.getType())){ // subtype
			return isOrmTypeValid(field.getType(), log);
		} else if(field.getAnnotation(ForeignKey.class) != null) {// foreign KEy
			return SupportedTypes.isTypeValidForkey(field.getType());
		}
		return false;//no matches
	}
	
	/**
	 * Checks if a Class type is compatible with Orm 
	 * @param type
	 * @param log
	 * @return
	 */
	protected static boolean isOrmTypeValid(Class<?> type, boolean log) {
		if(!isTypeSubTypeFromEntity(type)) {
			if(log) {
				Orm.logger.warn("Class \"" + type + "\"! is no subclass from Entity");
			}
			return false;
		}
		Table table = type.getAnnotation(Table.class);
		if(table == null && log) {
			Orm.logger.warn("You did not provide a Table Anotation for class \"" + type + "\"! Please do so by adding @Table(name = \"<tableName>\"");
		}
		return table != null;
	}
	
	/**
	 * Looks up a field and warns if the field has potential for missuse of annotations
	 * Warnings produced here usually dont have a negative impact on th orm
	 * @param field
	 */
	protected static void warnOrmField(Field field) {
		if(isFieldNoOrm(field)) {
			return;
		}
		if(isTypeSubTypeFromEntity(field.getType())){//subtype
			OneToOne oneToOne = field.getAnnotation(OneToOne.class);
			OneToMany oneToMany = field.getAnnotation(OneToMany.class);
			ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
			if(manyToMany != null) {
				Orm.logger.warn("Field \"" + field + "\" not compatible with Annotation ManyToMany");
			} if(oneToMany != null) {
				Orm.logger.warn("Field \"" + field + "\" not compatible with Annotation OneToMany");
			}else if(oneToOne == null) {
				Orm.logger.warn("Field \"" + field + "\" is compatible but No Annotation was found SKIPPED add @NoOrm to surpress this warning");
			}
		} else if(isListClass(field.getType())) {//list Field
			if(isOrmTypeValid(getGenericClassFromListField(field), false)) {
				ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
				OneToMany oneToMany = field.getAnnotation(OneToMany.class);
				OneToOne oneToOne = field.getAnnotation(OneToOne.class);
				if(oneToOne != null) {
					Orm.logger.warn("Field \"" + field + "\" not compatible with Annotation OneToOne");
				} else if(manyToMany == null && oneToMany == null) {
					Orm.logger.warn("List Field \"" + field + "\" is compatible but No Annotation was found SKIPPED add @NoOrm to surpress this warning");
				}
			}
		}
	}
	
	/**
	 * is a given class a List
	 * @param type
	 * @return
	 */
	protected static boolean isListClass(Class<?> type) {
		return isTypeSubTypeOrSame(type, List.class);
	}
	
	/**
	 * get the table Name from a entity extending cclass
	 * @param type
	 * @return table name or null in case of error
	 */
	protected static String getTableName(Class<? extends Entity<?>> type) {
		Table table = type.getAnnotation(Table.class);
		if(table == null) {
			Orm.logger.warn("You did not provide a Table Anotation for class \"" + type + "\"! Please do so by adding @Table(name = \"<tableName>\"");
			return null;
		}
		return table.name();
	}
	
	/**
	 * gets the first generic parameter from list field
	 * used to find out what type a list ist of
	 * @param listField to get from
	 * @return Class type of generic content
	 */
	private static Class<?> getGenericClassFromListField(Field listField){
		if(listField.getType() != List.class) {
			return null;
		}
		Type genericType = listField.getGenericType();
		if(genericType instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) genericType;
			Type[] types = pt.getActualTypeArguments();
            if(types[0] instanceof Class) {
            	 return (Class<?>) pt.getActualTypeArguments()[0];
            } else {
            	System.err.println("Only Lists with primitive or Entities are valid! Issued field: " + listField);
            	return null;
            }
		} else{
			System.err.println("not parameterized: " + genericType);
			return null;
		}
	}
	
	/**
	 * Stringifies an object to somewhat of a json format
	 * Used for improved toString() functionality
	 * 
	 * @param o Object to be strinified
	 * @return String representation
	 */
	public static String stringifyObject(Object o) {
		return stringifyObject(o, 0);
	}
	
	/**
	 * @param n
	 * @return n tabs as String
	 */
	private static String getnTabs(int n) {
		String out = "";
		for (int i = 0; i < n; i++) {
			out += "\t";
		}
		return out;
	}
	
	/**
	 * Stringifies an object to somewhat of a json format
	 * Used for improved toString() functionality
	 * @param o Object
	 * @param tabs for recursive calls
	 * @return String
	 */
	private static String stringifyObject(Object o, int tabs) {
		tabs++;
		if(SupportedTypes.isTypeSupported(o.getClass())) {
			if(o instanceof String) {
				return "\"" + o.toString() + "\"";
			} else {
				return o.toString();
			}
		}
		if(o instanceof List) {
			List<?> list = (List<?>) o;
			String out = "[";
			boolean first = true;
			for (Object elem : list) {
				out += (first ? "" : ", ") + stringifyObject(elem, tabs);
				first = false;
			}
			return out + "]";
		}
		
		String out = "\"" + o.getClass().getSimpleName() + "\": {\n " + getnTabs(tabs);
		Field[] fields = o.getClass().getDeclaredFields();
		for (Field field : fields) {
			try {
				field.setAccessible(true);
				Object fieldContent = field.get(o);
				if(fieldContent != null) {
					out +=",\n" + getnTabs(tabs) +  " \"" + field.getName() + "\": " + stringifyObject(fieldContent, tabs);
				} else {
					out +=",\n" + getnTabs(tabs) +  " \"" + field.getName() + "\": NULL" ;
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return out + "\n" + getnTabs(tabs - 1) + "}";
	}
}