package orm;

import annotations.Table;
import sqlMagic.DbConnector;
import sqlMagic.Delete;
import sqlMagic.Select;

/**
 * Class for easy access to Orm features
 * Meant to be extended by any class to gain Orm features
 * 
 * @author Timo Lehnertz
 *
 */

public class Entity<T extends Entity<?>> {
	
	/**
	 * easy syntactical access to sqlObjetcs
	 */
	public Delete delete = new Delete(this.getClass());
	public Select<T> select = new Select(getClass());
	
	
	/**
	 * Checks if this entity is valid and save to be saved to the database
	 * logggs all results and reasonds why it isnt valid to the logger.
	 * Use at least Loggger.WARN loglevel to see why an Entity might not be valid
	 * @return am I valid?
	 */
	public boolean isValid() {
		return Orm.isEntityValidForSafe(this);
	}
	
	/**
	 * saves this entity and all its contents to the database
	 * @return
	 */
	public int save() {
		return Orm.save(this);
	}
	
	/**
	 * Check if the table for this Entity type has been created already
	 * @return 
	 */
	public boolean doesTableExists() {
		return DbConnector.getInstance().doesTableExist(getTableName());
	}
	
	/**
	 * Deletes this single instance from the database
	 * all contents get deleted with this entity
	 * @return succsess
	 */ 
	public boolean delete() {
		if(getIdInDatabase() > -1) {
			Delete del = new Delete(this.getClass());
			del.where.pkIn((int) getIdInDatabase());
			return del.execute();
		} else {
			Orm.logger.warn("Cann not delete entity " + getClass().getSimpleName() + " Reason -> Not saved in the database yet");
			return false;
		}
	}
	
	/**
	 * Deletes all instances with their contents from this class type from the database
	 * @return succsess
	 */
	public boolean deleteAll() {
		Delete del = new Delete(this.getClass());
		return del.execute();
	}
	
	/**
	 * get the id of this instance in the database
	 * @return id or -1 if not in the db yet
	 */
	public long getIdInDatabase() {
		return RegisterManager.getInstance().getIdFromEntity(this);
	}
	
	/**
	 * @return the given Table Name
	 */
	public String getTableName() {
		Table table = getClass().getAnnotation(Table.class);
		if(table == null) {
			Orm.logger.warn("You did not provide a Table Anotation for class \"" + getClass() + "\"! Please do so by adding @Table(name = \"<tableName>\"");
			return null;
		}
		return table.name();
	}
	
	/**
	 * JSon like to String
	 */
	@Override
	public String toString() {
		return OrmUtils.stringifyObject(this);
	}
}