package orm;

import annotations.Table;

/**
 * Class for easy access to Orm features
 * Meant to be extended by any class to gain Orm features
 * 
 * @author Timo Lehnertz
 *
 */

public class Entity<T extends Entity<?>> {
	
	/**
	 * Checks if this entity is valid and save to be saved to the database
	 * logggs all results and reasonds why it isnt valid to the logger.
	 * Use at least Loggger.WARN loglevel to see why an Entity might not be valid
	 * @return am I valid?
	 */
	public boolean isValid() {
		return Orm.isEntityValidForSafe(this);
	}
	
	public boolean save() {
		return Orm.save(this) >= 0;
	}
	
	public boolean doesTableExists() {
		return DbConnector.getInstance().doesTableExist(getTableName());
	}
	
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
//	@Override
//	public String toString() {
//		return OrmUtils.stringifyObject(this);
//	}
}