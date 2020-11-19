package orm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author Timo Lehnertz
 *
 */

public class RegisterManager {

	private RegisterManager() {
		super();
	}
	
	private static RegisterManager instance;
	
	private Map<Entity<?>, Long> registeredEntities = new HashMap<>();
	
	public static RegisterManager getInstance() {
		if(instance == null) {
			instance = new RegisterManager();
		}
		return instance;
	}
	
	public boolean registerEntity(Entity<?> e, long id) {
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
	
	public long getIdFromEntity(Entity<?> e) {
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
