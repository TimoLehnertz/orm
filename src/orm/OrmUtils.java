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
import annotations.NotNull;
import annotations.OneToMany;
import annotations.OneToOne;
import annotations.SqlLontext;
import annotations.SqlText;
import annotations.SqlVarchar;
import annotations.Table;
import sqlMagic.DbConnector;
import sqlMagic.Delete;
import sqlMagic.SqlParams;
import sqlMagic.SupportedTypes;


public class OrmUtils {

	protected static int state = Orm.READ_WRITE;
	
	protected static final String NULLPTR = null;
	
	/**
	 * Simplified Database access
	 */
	private static DbConnector db = DbConnector.getInstance();
	
	/**
	 * Simplified logging
	 */
	private static final Logger log = Orm.logger;
	
	/**
	 * register manager for registering Entities and avoiding multiple savings of the exact same entity
	 */
	static RegisterManager manager = RegisterManager.getInstance();
	
	/**
	 * Name of the primary key field in class Entity
	 * This String can only be change id if the primary Key changes
	 */
	public static final String ENTITY_PK_FIELDNAME = "id_entity";
	
	/**
	 * Method wich should be invoked first before any interactions can be made with the orm
	 * required to define dependet Tables
	 * After it creates missing tables
	 * @param entities Tables to initialize
	 * @return true for success
	 */
	protected static boolean initTables(Class<? extends Entity<?>>[] entities) {
		log.info("Initializing tables..");
		log.incrementTab();
		if(!db.isOperatable()) {
			log.error("Database is not operatable! aboarding");
			log.decrementTab();
			return false;
		}
		
		initForeignKeys(entities);
		
		sortRelations(); // sort fkRelations
		
		boolean succsess = crateTables();//crate tables
		
		log.decrementTab();
		log.info("Done");
		return succsess;
	}

	/**
	 * This method first checks if the given Entity is valid to be saved
	 * and if so it will be saved into the database if state is equal to READ_WRITE
	 * @param entity to be saved
	 * @param pointer optional pointer for reference. used when this method gets invoked by another saving entity to reference the others one to many relation
	 * @param pointId id to point to
	 * @return
	 */
	protected static int saveEntity(Entity<?> entity, FK pointer, long pointId) {
		log.incrementTab("Saving Entity...", Logger.INFO);
		log.info(pointer != null ? "received pointer " + pointer + " with id: " + pointId : "");
		if(!isEntityValidForSave(entity)) {
			log.decrementTab("Cant save Entity! Didnt pass validation checks", Logger.WARN);
			return -1;
		}
		if(!entity.doesTableExists()) {
			log.warn("You saved an entity without its Table! did you invoked Orm.initTables() before?");
			return -1;
		}
		boolean isRegistered = manager.isEntityRegistered(entity);
		if(isRegistered) {//is already in the database -> UPDATE
			int id = updateEntity(entity);
			if(id > -1) {
				log.decrementTab("Done", Logger.INFO);
			} else {
				log.decrementTab("Could not save Entity " + entity.getClass().getSimpleName() + "! UPDATE failed", Logger.WARN);
			}
			return id;
		} else {// not in the database yet -> INSERT
			int id = insertEntity(entity, pointer, pointId);
			if(id > -1) {
				manager.registerEntity(entity, id);
				log.decrementTab("Done", Logger.INFO);
			} else {
				log.decrementTab("Could not save Entity! SQL INSERT failed", Logger.WARN);
			}
			return id;
		}
	}
	
	protected static int insertEntity(Entity<?> entity, FK pointer, long pointId) {
		log.incrementTab("inserting Entity " + getTableName(entity.getClass()) + "...", Logger.INFO);
		int id = -1;
		SqlParams insert = getInsertSql(entity, pointer, pointId);
		if(insert != null) {
			id = db.executeInsert(insert);
			if(id >= 0) {
				log.info("Succsessfully insertet Entity");
				log.info("Registering Entity in registerManager");
				manager.registerEntity(entity, id);
				
				boolean success = saveEntitiesManyRelations(entity);
				log.decrementTab(success ? "Done" : "Done, but some Many Fields may not be saved", Logger.INFO);
			} else {
				log.decrementTab("ERROR while inserting into the database", Logger.ERROR);
				return id;
			}
		} else {
			log.decrementTab();// No message needed as getInsertSql(entity) will inform about errors
			return id;
		}
		return id;
	}
	
	protected static int updateEntity(Entity<?> entity) {
		log.incrementTab("Updating Entity " + entity.getTableName() + "...", Logger.INFO);
		SqlParams update = getUpdateSql(entity);
		if(update != null) {
			boolean succsess = db.execute(update);
			if(succsess) {
				succsess = saveEntitiesManyRelations(entity);
				log.decrementTab(succsess ? "DONE" : "Done with failed saveEntitiesManyRelations", Logger.INFO);
				return manager.getIdFromEntity(entity);
			} else {
				log.decrementTab("Could not Update Entity: error during execution in database", Logger.ERROR);
				return -1;
			}
		} else {
			log.decrementTab("Could not Update Entity: getUpdateSql() failed", Logger.ERROR);
			return -1;
		}
	}
	
	/**
	 * Gererates the update sql for the given entity
	 * 
	 * Only updates the entities primitive / String fields!
	 * 
	 * @param entity
	 * @return SQL
	 */
	protected static SqlParams getUpdateSql(Entity<?> entity) {
		long id = manager.getIdFromEntity(entity);
		log.incrementTab("Generating Update SQL for Entity(id:" + id + ") " + entity.getTableName(), Logger.INFO);
		if(id == -1) {
			log.decrementTab("Can not update Entity: not inserted yet", Logger.ERROR);
			return null;
		}
		SqlParams update = new SqlParams("UPDATE `" + entity.getTableName() + "` ");
		String delimiter = " SET";
		for (Field field : getPrimitiveFields(entity.getClass())) {// primitive fields
			field.setAccessible(true);
			update.sql += delimiter + "`" + getColumnNameFromPrimitiveField(field) + "` = ?";
			delimiter = ", ";
			try {
				update.add(field.get(entity));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				if(field.isAnnotationPresent(NotNull.class)) {
					log.decrementTab("Field::get Error message -> " + e.getMessage(), Logger.ERROR);
					return null;
				} else {
					log.warn("Field::get Error message -> " + e.getMessage() + " | Updating with NULL instead");
					update.add(NULLPTR);
				}
			}
		}
		update.sql += " WHERE `" + ENTITY_PK_FIELDNAME + "` = ?;";
		update.add(id);
		log.decrementTab("Done", Logger.INFO);
		return update;
	}
	
	/**
	 * iterate trough existing One To Many foreign keys and save their lists
	 * when saving the method will pass the corresponding foreign key and the own id so the saved Entities can reference the given entity
	 * 
	 * @ToDo
	 * 
	 * @param entity
	 * @return succsess
	 */
	protected static boolean saveEntitiesManyRelations(Entity<?> entity) {
		int id = manager.getIdFromEntity(entity);
		log.incrementTab("Saving " + entity.getClass() + "(ID=" + id + ") Many Relations...", Logger.INFO);
		if(id == -1) {
			log.decrementTab("Entity is not in the database yet aboarding", Logger.WARN);
			return false;
		}
		int failed = 0;
		int succeeded = 0;
		/**
		 * one to many
		 * 1. Save all contents
		 * 2. delete (all) wich are not contained
		 */
		List<FK> oneToManys = manager.getOneToManyFksPointingTo(entity.getClass());
		log.info("found " + oneToManys.size() + " ONE_TO_MANY relations: " + oneToManys);
		for (FK fk : oneToManys) {
			List<Entity<?>> content = fk.getListContentFor(entity);
			List<Integer> contentIds = new ArrayList<>();
			if(content == null && fk.getOwnField().isAnnotationPresent(NotNull.class)) {// is null and NotNullAnotated
				log.decrementTab("ERROR fetching List content from Entity", Logger.WARN);
				return false;
			} else if(content != null) {
				for (Entity<?> listEntity : content) {
					int contentId = saveEntity(listEntity, fk, id);
					if(contentId != -1) {//succsess
						contentIds.add(contentId);
						succeeded++;
					}else {//error
						failed++;
					}
				}
			} else {
				//Null but valid all contents will be deleted from the database
			}
			/**
			 * delete all wich are not contained in contebnt ids
			 */
			log.info("Deleting dead one to many relations");
			Delete delete = new Delete(fk.getOwnTable());
			boolean succsess = delete.where.pkNotIn(contentIds).and().columnEquals(fk.getColumnname(), id).execute();
			if(succsess) {
				succeeded++;
			} else {
				failed++;
			}
		}
		/**
		 * many to many
		 */
		List<LinkTable> links = getLinkTablesFrom(entity.getClass());
		log.info("found " + links.size() + " MANY_TO_MANY relations: " + links);
		for (LinkTable linkTable : links) {
			log.info("handling linktable " + linkTable);
			List<Entity<?>> contentList = linkTable.getLinkAlistContent(entity);
			if(contentList == null) {
				if(linkTable.getLinkAReferenceField().isAnnotationPresent(NotNull.class)) {
					log.warn("Link tbale list field was null! skipped");
					failed++;
				} else {
					succeeded++;
					continue;//skip as null is valid
				}
			}
			log.info("Content size: " + contentList.size());
			List<Integer> savedIds = new ArrayList<>();
			for (Entity<?> content : contentList) {
				int savedId = content.save();
				boolean succsess = db.execute(linkTable.getInsertSql(entity, content));
				if(succsess) {
					savedIds.add(savedId);
					succeeded++;
				} else {
					failed++;
				}
			}
			log.info("Deleting many one to many relations");
			List<Integer> deletedIds = linkTable.deleteDeadLinks(id, savedIds);
			Delete delete = new Delete(linkTable.linkB);
			boolean succsess = delete.where.pkIn(deletedIds).execute();
			if(succsess) {
				succeeded++;
			} else {
				failed++;
			}
		}
		log.info(succeeded + " succseeded operations, " + failed + " failed operations from " + (succeeded + failed) + " in total");
		log.decrementTab("Done", Logger.INFO);
		return true;
	}
	
	/**
	 * check if a tyble has been initiated for this type
	 * @param type
	 * @return
	 */
	protected static boolean isIitiated(Class<?> type){
		for (Entry<Class<?>, List<FK>> entry : manager.fkRelations.entrySet()) {
			if(entry.getKey() == type) {
				return true;
			}
		}
		return false;
	}
	
	private static List<LinkTable> getLinkTablesFrom(Class<?> from){
		List<LinkTable> out = new ArrayList<>();
		for (LinkTable linkTable : manager.linkTables) {
			if(linkTable.getLinkA() == from) {
				out.add(linkTable);
			}
		}
		return out;
	}
	
	/**
	 * get the SQL INSERT COMMAND as String
	 * saves Primitive and One To One fields
	 * this method doesnt implement good error handling so isEntityValidForSave should be invoked for save usage
	 * If a pointer is given it will be used as first field to inert to
	 * @param entity
	 * @param pointer optional to reference a One to Many relation
	 * @param pointId id for fk
	 * @return SQL INSERT or null incase of failure
	 */
	protected static SqlParams getInsertSql(Entity<?> entity, FK pointer, long pointId) {
		log.incrementTab("Generating insert SQL Entity...", Logger.INFO);
		SqlParams insert = new SqlParams("INSERT INTO " + entity.getTableName() + " (");
		String delimiter = "";
		if(pointer != null) {
			insert.sql += delimiter + "`" + pointer.getColumnname() + "`";
			insert.add(pointId);
			delimiter = ", ";
		}
		insert.sql += delimiter + getInsertColumnNameString(entity) + ") VALUES (";
		delimiter = "";
		if(pointer != null) {
			insert.sql += delimiter + "?";
			delimiter = ", ";
		}
		for (Field field : getPrimitiveFields(entity.getClass())) {// Primitive fields
			field.setAccessible(true);
			try {
				insert.add(field.get(entity));
				insert.sql += delimiter + "?";
				delimiter = ", ";
			} catch (IllegalArgumentException | IllegalAccessException e) {
				if(field.getAnnotation(NotNull.class) == null) {
					log.warn("Reflection Error while trying to fetch fields content :( inserting NULL instead");
					insert.sql += delimiter + "NULL";
					delimiter = ", ";
				} else {
					log.decrementTab("Reflection Error while trying to fetch fields content :( aboarding", Logger.ERROR);
					return null;
				}
			}
		}
		for (FK fk : manager.getOneToOneFksFrom(entity.getClass())) {//ONE TO ONE relations
			Entity<?> content = fk.getEntityContentFor(entity);
			if(content == null && fk.isNotNull()) {
				log.decrementTab("Reflection Error while trying to fetch fields content :( aboarding", Logger.ERROR);
				return null;
			} else if(content == null && !fk.isNotNull()){
				insert.sql += delimiter + "?";
				insert.add(NULLPTR);
				delimiter = ", ";
			} else {
				long referenceId = saveEntity(content, null, 0);
				insert.sql += delimiter + "?";
				insert.add(referenceId);
				delimiter = ", ";
			}
		}
		insert.sql += ");";
		log.decrementTab("Done", Logger.INFO);
		return insert;
	}
	
	/**
	 * returns the entities Column names in its table in a specific order to be user for INSERT
	 * ORDER:
	 * -	Primitive Fields / String Fields in the order from getDeclaredFields()
	 * -	Foreign Key fields in the order from getDeclaredFields() (Only for OneToOne relations)
	 * @param entity
	 * @return
	 */
	protected static String getInsertColumnNameString(Entity<?> entity) {
		String out = "";
		String delimiter = "";
		for (Field field : getPrimitiveFields(entity.getClass())) {//Primitive Fields
			out += delimiter + "`" + getColumnNameFromPrimitiveField(field) + "`";
			delimiter = ", ";
		}
		for (FK fk : manager.getOneToOneFksFrom(entity.getClass())) {// ONE TO ONE FOREIGN KEYS
			out += delimiter + "`" + fk.getColumnname() + "`";
			delimiter = ", ";
		}
		return out;
	}
	
	/**
	 * Get all fields of an Entity which are no Annotated with @NoOrm and belong to the List of Supported Types
	 * 
	 * returned List is kept in the order, that getDeclaredFields provides(important for SQL INSERT)
	 * 
	 * @param entity
	 * @return List of Fields
	 */
	public static List<Field> getPrimitiveFields(Class<?> entity){
		List<Field> out = new ArrayList<>();
		for (Field field : entity.getDeclaredFields()) {
			if(isFieldNoOrm(field)) {
				continue;//Skip NoOrm Fields
			}
			if(SupportedTypes.isTypeSupported(field.getType())) {
				out.add(field);
			}
		}
		return out;
	}
	
	/**
	 * search through all existing fks with type one to many and returm a list of all pointing from a reference
	 * @param reference
	 * @return List of FKs
	 */
	public static List<FK> getOneToManyFksPointingFrom(Class<?> reference){
		List<FK> out = new ArrayList<>();
		for (Entry<Class<?>, List<FK>> entry : manager.fkRelations.entrySet()) {
			for (FK fk : entry.getValue()) {//iterating trough all fks
				if(fk.getType() == FK.ONE_TO_MANY && fk.getOwnTable() == reference) {
					out.add(fk);
				}
			}
		}
		return out;
	}
	
	/**
	 * get The Column Name representation of a Field
	 * Not useful yet but may be useful when this behaivior is meant to change in the future#
	 * @param f Field
	 * @return String name
	 */
	protected static String getColumnNameFromPrimitiveField(Field f) {
		return f.getName();
	}
	
	/**
	 * check if all field names of a type exept of thos Annotated to be @NoOrm have valid names
	 * @param type
	 * @return isValid
	 */
	protected static boolean checkClassFieldNames(Class<?> type) {
		List<String> fieldNamesLowerCase = new ArrayList<>();
		for (Field field : type.getDeclaredFields()) {
			if(isFieldNoOrm(field)) {
				continue;
			}
			if(field.getName().toLowerCase() == ENTITY_PK_FIELDNAME.toLowerCase() || fieldNamesLowerCase.contains(field.getName().toLowerCase())) {
				log.warn(type + " is NOT VALID! reason -> field " + field + " has an invalid name please chaneg it");
				return false;
			}
			fieldNamesLowerCase.add(field.getName().toLowerCase());
		}
		return true;
	}
	
	/**
	 * Checks if aan Entity is valid for saving
	 * Does not check for correct use of Annotations -> expects Annotaions to be used correctly.
	 * May lead to unexpected behaivior if annotations are used incorrect
	 * Does stop checking the entity if one bad value is found
	 * -	Checks if all @NotNull annotated Fields are infact not null
	 * -	Checks if all @SqlVarchar annotated Fields dont exeed their length
	 * -	Checks if all @SqlText annotated Fields dont exeed their length
	 * -	Checks if all @SSqlLongText annotated Fields dont exeed their length
	 * @param e Entity to be checked
	 * @return is Entity valid
	 */
	protected static boolean isEntityValidForSave(Entity<?> e) {
		log.disable();
		log.incrementTab("Checking if " + e.getClass() + " is valid for save...", Logger.DEBUG);
		if(!isIitiated(e.getClass())) {
			log.decrementTab("Done! " + e.getClass() + " is NOT VALID! reason -> Field Table is not initiated yet", Logger.WARN);
			return false;
		}
		for (Field field : e.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			Object content;
			try {
				content = field.get(e);	
			} catch (IllegalArgumentException | IllegalAccessException e1) {
				log.decrementTab("Done! " + e.getClass() + " is NOT VALID! reason -> " + e1.getMessage(), Logger.WARN);
				return false;
			}
			SqlVarchar varchar = field.getAnnotation(SqlVarchar.class);
			SqlText text = field.getAnnotation(SqlText.class);
			SqlLontext longText = field.getAnnotation(SqlLontext.class);
			NotNull nn = field.getAnnotation(NotNull.class);
			if(nn != null) {
				if(content == null) {
					log.decrementTab("Done! " + e.getClass() + " is NOT VALID! reason -> Field " + field + " is notated @NotNull but was null", Logger.WARN);
					return false;
				}
			}
			if(content instanceof String && content != null) { //Skipping null content as null is perfectly valid at this position
				int length = ((String) content).length();
				if(varchar != null) {
					if(length > varchar.size()) {
						log.decrementTab("Done! " + e.getClass() + " is NOT VALID! reason -> Field " + field + " of Type String exxeeds the maximum varchar size of " + varchar.size() + " chars. You can modify the lenth via Annotation", Logger.WARN);
						return false;
					}
				} else if(text != null) {
					if(length > SupportedTypes.MAX_TEXT_SIZE) {
						log.decrementTab("Done! " + e.getClass() + " is NOT VALID! reason -> Field " + field + " of Type String exxeeds the maximum varchar size of " + SupportedTypes.MAX_TEXT_SIZE + " chars", Logger.WARN);
						return false;
					}
				} else if(longText != null) {
					if(length > SupportedTypes.MAX_LONGTEXT_SIZE) {
						log.decrementTab("Done! " + e.getClass() + " is NOT VALID! reason -> Field " + field + " of Type String exxeeds the maximum varchar size of " + SupportedTypes.MAX_LONGTEXT_SIZE + " chars", Logger.WARN);
						return false;
					}
				} else if(length > SupportedTypes.MAX_TEXT_SIZE){
					log.decrementTab("Done! " + e.getClass() + " is NOT VALID! reason -> Field " + field + " of Type String exxeeds the maximum varchar size of " + SupportedTypes.MAX_TEXT_SIZE + " chars", Logger.WARN);
					return false;
				}
			}
			if(content != null) {
				if(isValidListField(field)) {//List field
					if(content instanceof List<?>) {
						@SuppressWarnings("unchecked")//should not happen might be good to fix @ToDo
						List<? extends Entity<?>> contentList = (List<? extends Entity<?>>) content;
						for (Entity<?> entity : contentList) {
							if(!isEntityValidForSave(entity)) {
								log.decrementTab("Done! " + e.getClass() + " is NOT VALID! reason -> Entity " + entity + " from List Field " + field + " is not valid", Logger.WARN);
								return false;
							}
						}
					} else {
						log.decrementTab("Done! " + e.getClass() + " is NOT VALID! reason -> Field " + field + " class casting error :(", Logger.WARN);
						return false;
					}
				} else if(isOrmFieldvalid(field, false)){// as no list, sub type
					if(content instanceof Entity<?>) {
						Entity<?> entity = (Entity<?>) content;
						if(!isEntityValidForSave(entity)) {
							log.decrementTab("Done! " + e.getClass() + " is NOT VALID! reason -> Entity " + entity + " from List Field " + field + " is not valid", Logger.WARN);
							return false;
						}
					} else {
						log.decrementTab("Done! " + e.getClass() + " is NOT VALID! reason -> Field " + field + " class casting error :(", Logger.WARN);
						return false;
					}
				}
			}
		}
		log.decrementTab("Done! " + e.getClass() + " is valid for save :)", Logger.DEBUG);
		log.enable();
		return true;
	}

	/**
	 * gets invoked at start by initTables()
	 * searches for dependencies betwween Tables and saves them into relations
	 * @param entities Pool
	 */
	protected static void initForeignKeys(Class<?>[] entities) {
		log.info("Initializing table-dependencies..");
		log.incrementTab();
		for (Class<?> type : entities) {
			if(!isOrmTypeValid(type, true)) {
				continue;
			
			} else if(checkClassFieldNames(type)){
				if(!manager.fkRelations.containsKey(type)) {
					manager.fkRelations.put(type, new ArrayList<FK>());
				}
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
						log.debug("Initiating One To many relation from \"" + field + "\" To \"" + otm.referenceTable() + "\"");
						Class<? extends Entity<?>> relationtable = otm.referenceTable();
						if(!manager.fkRelations.containsKey(relationtable)) {// No table registered yet
							manager.fkRelations.put(relationtable, new ArrayList<FK>());//register table
						}
						manager.fkRelations.get(relationtable).add(new FK(relationtable, type, field, ENTITY_PK_FIELDNAME, FK.ONE_TO_MANY, true));//add dependency for "many" table to own table with cascade set
					}
					/**
					 * One To One
					 * "type" has one ___
					 * => "type" is dependent on ___
					 */
					if(oto != null && otm == null) {
						Class<? extends Entity<?>> relationtable = oto.referenceTable();
						manager.fkRelations.get(type).add(new FK(type, relationtable, field, ENTITY_PK_FIELDNAME, FK.ONE_TO_ONE, false /*No cascade*/));
					}
					
					if(m2m != null && oto == null && otm == null) {
						log.info("Initiated link table from: " + type + "." + field.getName() + " to table " + getTableName(m2m.referenceTable()));
						final LinkTable link = new LinkTable(type, m2m.referenceTable(), field);
						manager.linkTables.add(link);
					}
				}
			}
		}
		log.info("Dependencies: " + manager.fkRelations);
		log.decrementTab();
		log.info("Done!");
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
		log.info("Sorting creation Order..");
		log.incrementTab();
		Map<Class<?>, List<FK>> pseudoCreated = new LinkedHashMap<>();//List of pseudo created Classes
		int done = manager.fkRelations.size();
		for (int i = 0; i < manager.fkRelations.size(); i++) {
			if(pseudoCreated.size() == manager.fkRelations.size()) {
				done = i;
				break;
			}
			for (Entry<Class<?>, List<FK>> entry : manager.fkRelations.entrySet()) {
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
		if(done == manager.fkRelations.size()) {//no sucsess
			log.warn("Could not define Creation order of tables! Do you have loops in your table scheme?");
		} else { // succsess
			log.info("Created creation order in " + done + " / " + manager.fkRelations.size() + " iterations :)");
			log.info("order: " + pseudoCreated);
		}
		log.decrementTab();
		log.info("Done!");
		manager.fkRelations = pseudoCreated;//applying sort
	}
	
	/**
	 * loop through the given table classes and crate them
	 * @param tables
	 * @return succsess
	 */
	private static boolean crateTables() {
		log.info("Creating tables...");
		log.incrementTab();
		int skipped = 0;
		for (Entry<Class<?>, List<FK>> entry : manager.fkRelations.entrySet()) {
			if(!createTable(entry.getKey(), entry.getValue())) {
				skipped++;
			}
		}
		if(skipped > 0) {
			log.info(skipped + " Tables skipped");
		}
		createLinkTables();
		log.decrementTab();
		log.info("Done!");
		return true;
	}
	
	/**
	 * Create link tables based on the linkTables array
	 * @return
	 */
	private static boolean createLinkTables() {
		log.info("Creating link tables");
		log.incrementTab();
		boolean succsess = true;
		for (LinkTable linkTable : manager.linkTables) {
			if(!linkTable.isCreated()) {
				log.info("Creating Link table \"" + linkTable.getTableName() + "\"");
				log.debug(linkTable.getCreateSql());
				boolean tmp = true;
				if(state == Orm.READ_WRITE) {
					db.execute(linkTable.getCreateSql());
				} else {
					log.info("Skipped creation of Link table due to READ_ONLY");
				}
				if(!tmp) {
					log.warn("Failed creating Link table \"" + linkTable.getTableName() + "\"");
				}
				succsess &= tmp;
			} else {
				log.debug("Skipped " +  linkTable.getTableName() + "(Already exists)");
			}
		}
		log.decrementTab();
		log.info("Done");
		return succsess;
	}
	
	private static boolean createTable(Class<?> type, List<FK> foreignKeys) {
		if(!isOrmTypeValid(type, false) || db.doesTableExist(getTableName(type))) {
			return false;
		}
		Table table = type.getAnnotation(Table.class);
		if(table == null) {
			return false;
		}
		SqlParams createParam = new SqlParams("CREATE TABLE `" + table.name() + "`(" + getSqlFieldDeclaraionString(type, foreignKeys) + ")");
		createParam.sql += "CHARACTER SET = " + table.charset();
		createParam.sql += " ENGINE =  " + table.engine() + ";";
		if(state == Orm.READ_WRITE) {
			return db.execute(createParam);
		} else {
			log.info("Skipped creation of table due to READ_ONLY");
			return true;
		}
	}
	
	private static String getSqlFieldDeclaraionString(Class<?> type, List<FK> foreignKeys) {
		String out = "`" + ENTITY_PK_FIELDNAME + "` INT PRIMARY KEY AUTO_INCREMENT";
		for (Field field : type.getDeclaredFields()) {
			if(isFieldNoOrm(field) || doesFieldHasRelations(field)) {
				continue;//skip @NoOrm and relational Fields for now
			} else
//			Primitive Fields
			if(SupportedTypes.isTypeSupported(field.getType())) {
				out += ", `" + getColumnNameFromPrimitiveField(field) + "` " + SupportedTypes.javaFieldToMysqlType(field);
			}
		}
		for (FK fk : foreignKeys) {//foreign Key fields
			out += ", " + fk.getCreateSql();
		}
		return out;
	}
	
	private static boolean doesFieldHasRelations(Field field) {
		OneToOne oto = field.getAnnotation(OneToOne.class);
		OneToMany otm = field.getAnnotation(OneToMany.class);
		ManyToMany m2m = field.getAnnotation(ManyToMany.class);
		return oto != null || otm != null || m2m != null;
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
	 * Checks if a Field of type List<? extends Entity> or ? extends Entity is valid for use in orm
	 * returns false for primitive / String fields
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
	
	protected static boolean isValidListField(Field field) {
		if(isListClass(field.getType())){
			return isOrmTypeValid(getGenericClassFromListField(field), false);
		}
		return false;
	}
	
	/**
	 * Checks if a Class type is compatible with Orm 
	 * @param type
	 * @param log
	 * @return
	 */
	protected static boolean isOrmTypeValid(Class<?> type, boolean logging) {
		if(!isTypeSubTypeFromEntity(type)) {
			if(logging) {
				log.warn("Class \"" + type + "\"! is no subclass from Entity");
			}
			return false;
		}
		Table table = type.getAnnotation(Table.class);
		if(table == null && logging) {
			log.warn("You did not provide a Table Anotation for class \"" + type + "\"! Please do so by adding @Table(name = \"<tableName>\"");
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
		SqlVarchar varchar = field.getAnnotation(SqlVarchar.class);
		SqlText text = field.getAnnotation(SqlText.class);
		SqlLontext longText = field.getAnnotation(SqlLontext.class);
		OneToOne oneToOne = field.getAnnotation(OneToOne.class);
		OneToMany oneToMany = field.getAnnotation(OneToMany.class);
		ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
		if(field.getType() != String.class) {
			if(varchar != null || text != null || longText != null) {
				log.warn("Field \"" + field + "\" is not compatible with Annotations for String Fields");
			}
		}
		if(isTypeSubTypeFromEntity(field.getType())){//subtype
			if(manyToMany != null) {
				log.warn("Field \"" + field + "\" not compatible with Annotation ManyToMany");
			} if(oneToMany != null) {
				log.warn("Field \"" + field + "\" not compatible with Annotation OneToMany");
			}else if(oneToOne == null) {
				log.warn("Field \"" + field + "\" is compatible but No Annotation was found SKIPPED | Add @NoOrm to surpress this warning");
			}
		} else if(isListClass(field.getType())) {//list Field
			if(isOrmTypeValid(getGenericClassFromListField(field), false)) {
				if(oneToOne != null) {
					log.warn("Field \"" + field + "\" not compatible with Annotation OneToOne");
				} else if(manyToMany == null && oneToMany == null) {
					log.warn("List Field \"" + field + "\" is compatible but No Annotation was found SKIPPED add @NoOrm to surpress this warning");
				} else if(manyToMany != null && oneToMany != null){
					log.warn("List Field \"" + field + "\" is compatible not compatible with manyToMany AND oneToMany");
				}
			}
		} else if(SupportedTypes.isTypeSupported(field.getType())) {//"primitive" Field
			if(oneToOne != null || oneToMany != null || manyToMany != null) {
				log.warn("Field \"" + field + "\" is not compatible width relation Annotations");
			}
			if(varchar != null && text != null) {
				log.warn("Field \"" + field + "\" is not compatible width both @SqlVarchar and @SqlText choose one of them");
			}
			if(varchar != null) {
				if(varchar.size() < 1) {
					log.warn("Field \"" + field + "\" has varchar smaller 1! one will be used instead");
				}
			}
			if(varchar != null || text != null || longText != null) {
				if(field.getType() != String.class) {
					log.warn("Field \"" + field + "\" is not compatible width @SqlVarchar / @SqlText / @SqlLongText use them for String fields only");
				}
			}
			if(field.getType() == String.class) {
				if(varchar == null && text == null && longText == null) {
					log.warn("Field \"" + field + "\" of type String is not Anotated SqlType text will be choosen. Annotate the field with @SqlText / @SqlText or @NoOrm to surpress this warning");
				}
			}
		} else {
			log.warn("Field \"" + field + "\" is not supported :( use @NoOrm to surpress this warning or ask for support :)");
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
	public static String getTableName(Class<?> type) {
		Table table = type.getAnnotation(Table.class);
		if(table == null) {
			log.warn("You did not provide a Table Anotation for class \"" + type + "\"! Please do so by adding @Table(name = \"<tableName>\"");
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
	 * @return n times the string glued together to one
	 */
	public static String getNStrings(String s, int n) {
		String out = "";
		for (int i = 0; i < n; i++) {
			out += s;
		}
		return out;
	}
	
	/**
	 * @param n
	 * @return n tabs as String
	 */
	public static String getnTabs(int n) {
		return getNStrings("\t", n);
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
		
		String out = "\"" + o.getClass().getSimpleName() + "\": { " + getnTabs(tabs);
		Field[] fields = o.getClass().getDeclaredFields();
		String delimiter = "";
		for (Field field : fields) {
			try {
				field.setAccessible(true);
				Object fieldContent = field.get(o);
				if(fieldContent != null) {
					out += delimiter + "\n" + getnTabs(tabs) +  " \"" + field.getName() + "\": " + stringifyObject(fieldContent, tabs);
				} else {
					out += delimiter + "\n" + getnTabs(tabs) +  " \"" + field.getName() + "\": NULL" ;
				}
				delimiter = ",";
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return out + "\n" + getnTabs(tabs - 1) + "}";
	}
}