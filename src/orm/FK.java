package orm;

import java.lang.reflect.Field;
import java.util.List;

public class FK {

	public static final int ONE_TO_ONE = 0;
	public static final int ONE_TO_MANY = 1;
	
	/**
	 * table the foreign key is pointing to
	 */
	private Class<?> referenceTable;
	
	/**
	 * table in which the foreign key is located
	 */
	private Class<?> ownTable;
	/**
	 * Reference to th field representing this relationship
	 * always the Annotated field with One to one or One to may
	 */
	private Field ownField;
	
	/**
	 * name of the column in the database this fk is pointing to
	 */
	private String referenceColumn;
	
	private boolean notNull = false;
	private boolean unique = false;
	
	boolean cascade;
	int type;
	
	public FK(Class<?> ownTable, Class<?> referenceTable,  Field ownField, String referenceColumn, int type, boolean cascade) {
		super();
		this.referenceTable = referenceTable;
		this.ownTable = ownTable;
		this.ownField = ownField;
		this.referenceColumn = referenceColumn;
		this.type = type;
		ownField.setAccessible(true);
	}
	
	public FK(Class<?> ownTable, Class<?> referenceTable, Field ownField, String referenceColumn, int type, boolean notNull, boolean cascade, boolean unique) {
		super();
		this.referenceTable = referenceTable;
		this.ownTable = ownTable;
		this.ownField = ownField;
		this.referenceColumn = referenceColumn;
		this.type = type;
		this.notNull = notNull;
		this.unique = unique;
		ownField.setAccessible(true);
	}
	
	public String getCreateSql() {
		String out = "`" + getColumnname() + "` INT" + (notNull ? " NOT NULL" : "") + (unique ? "UNIQUE" : "");//declaration
		out += ", FOREIGN KEY(`" + getColumnname() + "`) REFERENCES `" + OrmUtils.getTableName(referenceTable) + "`(`" + referenceColumn + "`)";//reference
		if(cascade) {
			out += " ON DELETE CASCADE ON UPDATE CASCADE";
		}
		return out;
	}
	
	/**
	 * Get the Entity content of the pointing Field
	 * Only allowed for Type ONE TO ONE
	 * @param instance to get for
	 * @return ENTITY or null in case of error or wrong usage
	 */
	public Entity<?> getEntityContentFor(Entity<?> instance){
		if(type == ONE_TO_ONE) {
			Object content = getContentFor(instance);
			if(content instanceof Entity) {
				return (Entity<?>) content;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Get the List<Entity> content of the pointing Field
	 * Only allowed for Type ONE TO MANY
	 * @param instance to get for
	 * @return ENTITY or null in case of error or wrong usage
	 */
	@SuppressWarnings("unchecked")// impossible to check. I guess.. I'll have some trust
	public List<Entity<?>> getListContentFor(Entity<?> instance){
		if(type == ONE_TO_MANY) {
			Object content = getContentFor(instance);
			if(OrmUtils.isListClass(content.getClass())) {
				return (List<Entity<?>>) content;
			} else {
				return null;
			}
		} else {
			Orm.logger.warn("incorrect usage of getListContentFor(): used on a ONE_TO_ONE FK");
			return null;
		}
	}
	
	private Object getContentFor(Entity<?> instance) {
		try {
			return ownField.get(instance);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			Orm.logger.error("Foreign key incorrectly formed please report this error message: " + e.getMessage());
			return null;// cant handle errors different at this point
		}
	}
	
	public boolean isCascade() {
		return cascade;
	}

	public String getColumnname() {
		return OrmUtils.getTableName(referenceTable) + "_" + referenceColumn;
	}

	public Class<?> getReferenceTable() {
		return referenceTable;
	}
	public void setReferenceTable(Class<?> referenceTable) {
		this.referenceTable = referenceTable;
	}
	
	protected String getReferenceColumn() {
		return referenceColumn;
	}

	public boolean isNotNull() {
		return notNull;
	}

	public boolean isUnique() {
		return unique;
	}

	protected int getType() {
		return type;
	}

	protected Field getOwnField() {
		return ownField;
	}

	protected Class<?> getOwnTable() {
		return ownTable;
	}

	@Override
	public String toString() {
		if(type == ONE_TO_MANY) {
			return "from <Some table>" + getColumnname() + " to " + OrmUtils.getTableName(referenceTable) + "." + referenceColumn + "(mapped to: " + ownField + ")";
		} else if(type == ONE_TO_ONE){
			return "from " + getColumnname() + " to " + referenceTable.getSimpleName() + "." + referenceColumn;
		} else {
			return super.toString();
		}
	}
}