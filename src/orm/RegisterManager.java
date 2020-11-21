package orm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author Timo Lehnertz
 *
 */

public class RegisterManager {

	/**
	 * relations -> Map wich should be initiated at the start via Orm.initTables()
	 * after initialization this Map should contain every table as Key and
	 * correspondingly a List of all Tables wich it is dependent on
	 * 
	 * later this will be needed for defining the order of table creation
	 * 
	 * Tables are represented by Entity extending class types
	 */
	public Map<Class<?>, List<FK>> fkRelations = new LinkedHashMap<>();
	
	/**
	 * Link tables for many to many relations
	 */
	public List<LinkTable> linkTables = new ArrayList<>();
	
	private static RegisterManager instance;
	
	private Map<Entity<?>, Integer> registeredEntities = new HashMap<>();
	
	private RegisterManager() {
		super();
	}
	
	public static RegisterManager getInstance() {
		if(instance == null) {
			instance = new RegisterManager();
		}
		return instance;
	}
	
	/**
	 * search through all existing fks with type one to many and returm a list of all pointing to reference
	 * @param reference
	 * @return List of FKs
	 */
	public List<FK> getOneToManyFksPointingTo(Class<?> reference){
		List<FK> out = new ArrayList<>();
		for (Entry<Class<?>, List<FK>> entry : fkRelations.entrySet()) {
			for (FK fk : entry.getValue()) {//iterating trough all fks
				if(fk.getType() == FK.ONE_TO_MANY && fk.getReferenceTable() == reference) {
					out.add(fk);
				}
			}
		}
		return out;
	}
	
	/**
	 * search ONE to one fkRelations for
	 * @param entity to search for
	 * @return List of matching foreign keys
	 */
	public List<FK> getOneToOneFksFrom(Class<?> type){
		List<FK> out = new ArrayList<>();
		for (FK fk : fkRelations.get(type)) {
			if(fk.getType() == FK.ONE_TO_ONE) {
				out.add(fk);
			}
		}
		return out;
	}
	
	public List<FK> getOneToOnePointingTo(Class<?> type){
		List<FK> out = new ArrayList<>();
		for (Entry<Class<?>, List<FK>> entry : fkRelations.entrySet()) {
			for ( FK fk : entry.getValue()) {
				if(fk.getReferenceTable() == type && fk.type == FK.ONE_TO_ONE) {
					out.add(fk);
				}
			}
		}
		return out;
	}
	
	public List<LinkTable> getLinkTablesFromLinkA(Class<?> linkA){
		List<LinkTable> out = new ArrayList<>();
		for (LinkTable linkTable : linkTables) {
			if(linkTable.getLinkA() == linkA) {
				out.add(linkTable);
			}
		}
		return out;
	}
	
	public boolean registerEntity(Entity<?> e, int id) {
		if(!registeredEntities.containsKey(e)) {
			registeredEntities.put(e, id);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isEntityRegistered(Entity<?> e) {
		return registeredEntities.containsKey(e);
	}
	
	public int getIdFromEntity(Entity<?> e) {
		if(registeredEntities.containsKey(e)) {
			return registeredEntities.get(e);
		} else {
			return -1;
		}
	}
	
	public boolean deleteEntity(Entity<?> e) {
		if(registeredEntities.containsKey(e)) {
			registeredEntities.remove(e);
			return true;
		} else {
			return false;
		}
	}
}
