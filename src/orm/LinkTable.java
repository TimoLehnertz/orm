package orm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for storing and managing A link table between two Many To Many Tables
 * @author timo.lehnertz
 *
 */

class LinkTable {

	public Class<?> linkA;
	public Class<?> linkB;
	
	public Field linkAReferenceField;
	
	/**
	 * LinkB reference field is the primary key defined in Entity in all cases
	 */

	public LinkTable(Class<?> linkA, Class<?> linkB, Field linkAReferenceField) {
		super();
		this.linkA = linkA;
		this.linkB = linkB;
		this.linkAReferenceField = linkAReferenceField;
		linkAReferenceField.setAccessible(true);
	}
	
	public String getCreateSql() {
		String s = "CREATE TABLE `" + getTableName() + "`(";
		s += "`" + OrmUtils.getTableName(linkA) + "` INT NOT NULL";
		s += ", `" + OrmUtils.getTableName(linkB) + "` INT NOT NULL";
		s += ", PRIMARY KEY(`" + OrmUtils.getTableName(linkA) + "`, `" + OrmUtils.getTableName(linkB) + "`)";//Primary key
		s += ", FOREIGN KEY(`" + OrmUtils.getTableName(linkA) + "`) REFERENCES `" + OrmUtils.getTableName(linkA) + "`(`" + OrmUtils.ENTITY_PK_FIELDNAME + "`) ON UPDATE CASCADE ON DELETE CASCADE";//foreign key
		s += ", FOREIGN KEY(`" + OrmUtils.getTableName(linkB) + "`) REFERENCES `" + OrmUtils.getTableName(linkB) + "`(`" + OrmUtils.ENTITY_PK_FIELDNAME + "`) ON UPDATE CASCADE ON DELETE CASCADE";//foreign key
		return s + ");";
	}
	
	public SqlParams getInsertSql(Entity<?> a, Entity<?> b) {
		long idA = RegisterManager.getInstance().getIdFromEntity(a);
		long idB = RegisterManager.getInstance().getIdFromEntity(b);
		if(Math.min(idA, idB) == -1) {
			Orm.logger.warn("getInsertSql Error: at least on entity was not in the database Yet");
			return null;
		}
		SqlParams sql = new SqlParams("INSERT IGNORE INTO " + getTableName() + "(`" + OrmUtils.getTableName(linkA) + "`, `" + OrmUtils.getTableName(linkB) + "`) VALUES(?, ?);");
		sql.add(idA, idB);
		return sql;
	}
	
	@SuppressWarnings("unchecked")//@TODo solution...
	public List<Entity<?>> getLinkAlistContent(Entity<?> reference){
		List<Entity<?>> out = new ArrayList<>();
		try {
			out = (List<Entity<?>>) linkAReferenceField.get(reference);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			Orm.logger.warn("getLinkAlistContent failed. message: " + e.getMessage());
		}
		return out;
	}
	
	public String getTableName() {
		return OrmUtils.getTableName(linkA) + "x" + OrmUtils.getTableName(linkB);
	}
	
	public boolean hasLink(Class<?> link) {
		return linkA == link || linkB == link;
	}

	public Class<?> getLinkA() {
		return linkA;
	}

	public void setLinkA(Class<?> linkA) {
		this.linkA = linkA;
	}

	public Class<?> getLinkB() {
		return linkB;
	}

	public void setLinkB(Class<?> linkB) {
		this.linkB = linkB;
	}

	public Field getLinkAReferenceField() {
		return linkAReferenceField;
	}

	public void setLinkAReferenceField(Field linkAReferenceField) {
		this.linkAReferenceField = linkAReferenceField;
	}

	public boolean isCreated() {
		return DbConnector.getInstance().doesTableExist(getTableName());
	}
	
	@Override
	public String toString() {
		return "M2M -> LinkA: " + OrmUtils.getTableName(linkA) + "." + linkAReferenceField.getName() + ", LinkB: " + OrmUtils.getTableName(linkB);
	}
}