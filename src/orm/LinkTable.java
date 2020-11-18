package orm;

import java.lang.reflect.Field;

/**
 * Class for storing and managing A link table between two Many To Many Tables
 * @author timo.lehnertz
 *
 */

class LinkTable {

	public Class<?> linkA;
	public Class<?> linkB;
	
	public Field linkAReferenceField;

	public LinkTable(Class<?> linkA, Class<?> linkB, Field linkAReferenceField) {
		super();
		this.linkA = linkA;
		this.linkB = linkB;
		this.linkAReferenceField = linkAReferenceField;
	}
	
	public String getCreateSql() {
		String s = "CREATE TABLE " + getTableName() + "(";
		s += OrmUtils.getTableName(linkA) + " INT NOT NULL";
		s += ", " + OrmUtils.getTableName(linkB) + " INT NOT NULL";
		s += ", PRIMARY KEY(" + OrmUtils.getTableName(linkA) + ", " + OrmUtils.getTableName(linkB) + ")";//Primary key
		s += ", FOREIGN KEY(" + OrmUtils.getTableName(linkA) + ") REFERENCES " + OrmUtils.getTableName(linkA) + "(" + OrmUtils.ENTITY_PK_FIELDNAME + ")";//foreign key
		s += ", FOREIGN KEY(" + OrmUtils.getTableName(linkA) + ") REFERENCES " + OrmUtils.getTableName(linkA) + "(" + OrmUtils.ENTITY_PK_FIELDNAME + ")";//foreign key
		return s + ");";
	}
	
	
	
	public String getTableName() {
		return OrmUtils.getTableName(linkA) + "X" + OrmUtils.getTableName(linkB);
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
}