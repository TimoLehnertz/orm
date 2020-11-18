package orm;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import annotations.ManyToMany;
import annotations.NoOrm;
import annotations.OneToMany;
import annotations.OneToOne;
import annotations.Table;
import annotations.Varchar;


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
	private static Map<Class<?>, List<FK>> fkRelations = new LinkedHashMap<>();
	
	/**
	 * Link tables for many to many relations
	 */
	private static List<LinkTable> linkTables = new ArrayList<>();
	
	/**
	 * Simplified Database access
	 */
	private static DbConnector db = DbConnector.getInstance();
	
	/**
	 * Name of the primary key field in class Entity
	 * This String can only be change id if the primary Key changes
	 */
	public static final String ENTITY_PK_FIELDNAME = "id_entity";
	
	/**
	 * tabs for debugging
	 */
	int tabs = 0;
	
	/**
	 * Method wich should be invoked first before any interactions can be made with the orm
	 * required to define dependet Tables
	 * After it creates missing tables
	 * @param entities Tables to initialize
	 * @return true for success
	 */
	protected static boolean initTables(Class<? extends Entity<?>>[] entities) {
		Orm.logger.notice("Initializing tables..");
		if(!db.isOperatable()) {
			Orm.logger.error("Database is not operatable! aboarding");
			return false;
		}
		Orm.logger.incrementTab();
		
		initForeignKeys(entities);
		
		sortRelations(); // sort fkRelations
		
		boolean succsess = crateTables();//crate tables
		
		Orm.logger.decrementTab();
		Orm.logger.notice("Done");
		return succsess;
	}

	/**
	 * gets invoked at start by initTables()
	 * searches for dependencies betwween Tables and saves them into relations
	 * @param entities Pool
	 */
	protected static void initForeignKeys(Class<? extends Entity<?>>[] entities) {
		Orm.logger.notice("Initializing table-dependencies..");
		Orm.logger.incrementTab();
		for (Class<? extends Entity<?>> type : entities) {
			if(!isOrmTypeValid(type, true)) {
				continue;
			} else {
				fkRelations.put(type, new ArrayList<FK>());
				for (Field field : type.getDeclaredFields()) {
					
					warnOrmField(field);//look for suspicius use of annotations and warn the user
					
					if(!isOrmFieldvalid(field, false)) {
						continue;//Skip no table fields
					}
//					Check dependencies
					
					/**
					 * One to Many => field is dependent on type
					 */
					OneToOne oto = field.getAnnotation(OneToOne.class);
					OneToMany otm = field.getAnnotation(OneToMany.class);
					ManyToMany m2m = field.getAnnotation(ManyToMany.class);
					if(otm != null && oto == null && m2m == null) {
						Class<? extends Entity<?>> relationtable = otm.referenceTable();
						if(!fkRelations.containsKey(relationtable)) {// No table registered yet
							fkRelations.put(relationtable, new ArrayList<FK>());//register table
						}
						fkRelations.get(relationtable).add(new FK(type, field.getName()));//add dependency for "many" table to own table
					}
					/**
					 * One To One
					 * "type" has one ___
					 * => "type" is dependent on ___
					 */
					if(oto != null && otm == null) {
						Class<? extends Entity<?>> relationtable = oto.referenceTable();
						fkRelations.get(type).add(new FK(relationtable, ENTITY_PK_FIELDNAME));
					}
					
					if(m2m != null && oto == null && otm == null) {
						Orm.logger.notice("Initiated link table from: " + type + "." + field.getName() + " to table " + getTableName(m2m.referenceTable()));
						final LinkTable link = new LinkTable(type, m2m.referenceTable(), field);
						linkTables.add(link);
					}
				}
			}
		}
		Orm.logger.notice("Dependencies: " + fkRelations);
		Orm.logger.decrementTab();
		Orm.logger.notice("Done!");
	}
	
	/**
	 * look through the ralation Map and figure out the order in wich tables should be created
	 * Warns if the relations dont allow for table creation
	 * 
	 * Also figures out the foreign Keys(Auto generyted and manually added)
	 * 
	 * @return the order needed to create tables and their foreign keys
	 */
	private static void sortRelations(){
		Orm.logger.notice("Sorting creation Order..");
		Orm.logger.incrementTab();
		Map<Class<?>, List<FK>> pseudoCreated = new LinkedHashMap<>();//List of pseudo created Classes
		int done = fkRelations.size();
		for (int i = 0; i < fkRelations.size(); i++) {
			if(pseudoCreated.size() == fkRelations.size()) {
				done = i;
				break;
			}
			for (Entry<Class<?>, List<FK>> entry : fkRelations.entrySet()) {
				if(pseudoCreated.containsKey(entry.getKey())){//check if already "created"
					continue;
				}
				boolean satisfied = true;
				for (FK fk : entry.getValue()) {// check if all dependencies are "created"
					if(!pseudoCreated.containsKey(fk.getReferenceTable())) {
						satisfied = false;
					}
				}
				if(satisfied) {
//					System.out.println(pseudoCreated);
//					System.out.println("contains all dependencies for: " + entry);
					pseudoCreated.put(entry.getKey(), entry.getValue());//"create"
				}
			}
		}
		if(done == fkRelations.size()) {//no sucsess
			Orm.logger.warn("Could not define Creation order of tables! Do you have loops in your logic?");
		} else { // succsess
			Orm.logger.notice("Created creation order in " + done + " / " + fkRelations.size() + " iterations :)");
			Orm.logger.notice("order: " + pseudoCreated);
		}
		Orm.logger.decrementTab();
		Orm.logger.notice("Done!");
		fkRelations = pseudoCreated;//applying sort
	}
	
	/**
	 * loop through the given table classes and crate them
	 * @param tables
	 * @return succsess
	 */
	private static boolean crateTables() {
		Orm.logger.notice("Creating tables...");
		Orm.logger.incrementTab();
		int skipped = 0;
		for (Entry<Class<?>, List<FK>> entry : fkRelations.entrySet()) {
			if(!createTable(entry.getKey(), entry.getValue())) {
				skipped++;
			}
		}
		if(skipped > 0) {
			Orm.logger.notice("->" + skipped + " Tables skipped");
		}
		
		createLinkTables();
		
		Orm.logger.decrementTab();
		Orm.logger.notice("Done!");
		return false;
	}
	
	/**
	 * Create link tables based on the linkTables array
	 * @return
	 */
	private static boolean createLinkTables() {
		Orm.logger.notice("Creating link tables");
		Orm.logger.incrementTab();
		boolean succsess = true;
		for (LinkTable linkTable : linkTables) {
			if(!linkTable.isCreated()) {
				Orm.logger.notice("Creating Link table \"" + linkTable.getTableName() + "\"");
				Orm.logger.debug(linkTable.getCreateSql());
				boolean tmp = db.execute(linkTable.getCreateSql());
				if(!tmp) {
					Orm.logger.warn("Failed creating Link table \"" + linkTable.getTableName() + "\"");
				}
				succsess &= tmp;
			} else {
				Orm.logger.debug("Skipped " +  linkTable.getTableName() + "(Already exists)");
			}
		}
		Orm.logger.decrementTab();
		Orm.logger.notice("Done");
		return succsess;
	}
	
	private static boolean createTable(Class<?> type, List<FK> foreignKeys) {
		if(!isOrmTypeValid(type, false) || DbConnector.getInstance().doesTableExist(getTableName(type))) {
			return false;
		}
		Table table = type.getAnnotation(Table.class);
		if(table == null) {
			return false;
		}
		SqlParams createParam = new SqlParams("CREATE TABLE " + table.name() + "(" + getSqlFieldDeclaraionString(type, foreignKeys) + ")");
		createParam.sql += "CHARACTER SET = " + table.charset();
		createParam.sql += " ENGINE =  " + table.engine();
		createParam.sql += ";";
//		return db.execute(createParam);
		return false;
	}
	
	private static String getSqlFieldDeclaraionString(Class<?> type, List<FK> foreignKeys) {
		String out = ENTITY_PK_FIELDNAME + " INT PRIMARY KEY";
		for (FK fk : foreignKeys) {
			out += getTableName(fk.getReferenceTable()) + " INT";
		}
		return out;
	}
	
	/**
	 * Get the primary key field which is declare in entity
	 * @return
	 */
	private static Field getPrimaryKeyField(){
		try {
			return Entity.class.getDeclaredField(ENTITY_PK_FIELDNAME);
		} catch (NoSuchFieldException | SecurityException e) {
			Orm.logger.error("Primary key in ENTITY changed please change its name back to \"" + ENTITY_PK_FIELDNAME + "\"");
			e.printStackTrace();
			return null;
		}
	}
	
//	private static Field getForeignKeyReferenceField(Field fkField, ForeignKey fk, boolean superClass) {
//		Field reference = null;
//		try {
//			if(superClass) {
//				reference = fk.referenceTable().getSuperclass().getDeclaredField(fk.field());
//			} else {
//				reference = fk.referenceTable().getDeclaredField(fk.field());
//			}
//			if(SupportedTypes.isTypeValidForkey(reference.getType())) {
//				if(fkField.getType() == reference.getType()) {
//					if(reference.getAnnotation(PrimaryKey.class) == null) {
//						Orm.logger.warn("The Foreign key at \"" + fk.referenceTable() + "\", \"" + fk.field() + "\" is pointing to a non Primary key Field");
//					}
//					return reference;
//				} else {
//					Orm.logger.warn("Foreign And referenced Primary keys have different types! ON: \"" + fk.referenceTable() + "\", \"" + fk.field() + "\"");
//				}
//			} else {
//				Orm.logger.warn("Foreign Key has a wrong type! Only numeric foreign Keys are supported. ON: \"" + fk.referenceTable() + "\", \"" + fk.field() + "\"");
//			}
//		} catch (NoSuchFieldException | SecurityException e) {
//			if(!superClass) {
//				reference = getForeignKeyReferenceField(fkField, fk, true);
//				if(reference == null) {
//					Orm.logger.warn("Foreign Key is incorrectly formed given: \"" + fk.referenceTable() + "\", \"" + fk.field() + "\", message: " + e.getMessage());
//				} else {
//					if(reference.getAnnotation(PrimaryKey.class) == null) {
//						Orm.logger.warn("The Foreign key at \"" + fk.referenceTable() + "\", \"" + fk.field() + "\" is pointing to a non Primary key");
//					}
//					return reference;
//				}
//			}
//		}
//		return null;
//	}
	
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
		OneToOne oneToOne = field.getAnnotation(OneToOne.class);
		OneToMany oneToMany = field.getAnnotation(OneToMany.class);
		ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
		if(isTypeSubTypeFromEntity(field.getType())){//subtype
			if(manyToMany != null) {
				Orm.logger.warn("Field \"" + field + "\" not compatible with Annotation ManyToMany");
			} if(oneToMany != null) {
				Orm.logger.warn("Field \"" + field + "\" not compatible with Annotation OneToMany");
			}else if(oneToOne == null) {
				Orm.logger.warn("Field \"" + field + "\" is compatible but No Annotation was found SKIPPED | Add @NoOrm to surpress this warning");
			}
		} else if(isListClass(field.getType())) {//list Field
			if(isOrmTypeValid(getGenericClassFromListField(field), false)) {
				if(oneToOne != null) {
					Orm.logger.warn("Field \"" + field + "\" not compatible with Annotation OneToOne");
				} else if(manyToMany == null && oneToMany == null) {
					Orm.logger.warn("List Field \"" + field + "\" is compatible but No Annotation was found SKIPPED add @NoOrm to surpress this warning");
				} else if(manyToMany != null && oneToMany != null){
					Orm.logger.warn("List Field \"" + field + "\" is compatible not compatible with manyToMany AND oneToMany");
				}
			}
		} else if(SupportedTypes.isTypeSupported(field.getType())) {//"primitive" Field
			Varchar varchar = field.getAnnotation(Varchar.class);
			if(varchar != null) {
				if(field.getType() != String.class) {
					Orm.logger.warn("List Field \"" + field + "\" is compatible width @Varchar use varchar for String fields");
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
	protected static String getTableName(Class<?> type) {
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
	public static String getnTabs(int n) {
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
