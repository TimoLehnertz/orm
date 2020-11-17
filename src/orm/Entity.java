package orm;

import annotations.AutoIncrement;
import annotations.PrimaryKey;

/**
 * Class for easy access to Orm features
 * Meant to be extended by any class to gain AutoDb features
 * 
 * @author Timo Lehnertz
 *
 */

public class Entity<T extends Entity<?>> {
	
	@PrimaryKey
	@AutoIncrement
	protected long id_entity;
	
	/**
	 * Saves/updates this instance and all its content to the database
	 * @return
	 */
//	public long save() {
		
//		return OrmLogic.save(this);
//	}
	
	/**
	 * Deletes this object and its contents from the database
	 * @return true if succsessful
	 */
//	public boolean delete() {
//		return OrmLogic.delete(this);
//	}
	
	/**
	 * @return List of all Entity instances ever beeing saved to the database
	 */
//	public List<T> getAll() {
//		return (List<T>) OrmLogic.getAllFromType(this.getClass(), null);
//	}
	
	/**
	 * Overriden to String functionality to easier see relations between objects and its content
	 */
	
	@Override
	public String toString() {
		return OrmUtils.stringifyObject(this);
	}
}